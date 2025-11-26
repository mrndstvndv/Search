package com.mrndstvndv.search.provider.files

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mrndstvndv.search.provider.files.index.FileSearchDatabase
import com.mrndstvndv.search.provider.files.index.IndexedDocumentDao
import com.mrndstvndv.search.provider.files.index.IndexedDocumentEntity
import com.mrndstvndv.search.provider.files.indexing.FileSearchIndexWorker
import com.mrndstvndv.search.provider.files.indexing.IncrementalFileSyncWorker
import com.mrndstvndv.search.provider.settings.FileSearchRoot
import com.mrndstvndv.search.provider.settings.FileSearchSortMode
import java.util.concurrent.TimeUnit
import kotlin.math.min

class FileSearchRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database: FileSearchDatabase = FileSearchDatabase.get(appContext)
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun search(
        queryText: String,
        rootIds: List<String>,
        sortMode: FileSearchSortMode,
        sortAscending: Boolean,
        limit: Int = MAX_RESULTS
    ): List<FileSearchMatch> {
        val cleaned = queryText.trim()
        if (cleaned.isEmpty() || rootIds.isEmpty()) return emptyList()
        val normalized = "%${escapeLikeWildcards(cleaned)}%"
        val dao = database.indexedDocumentDao()
        val matches = dao.search(rootIds, normalized, min(limit, MAX_RESULTS))
        val mapped = matches.map { entity ->
            FileSearchMatch(
                documentUri = entity.documentUri,
                displayName = entity.displayName,
                relativePath = entity.relativePath,
                rootDisplayName = entity.rootDisplayName,
                mimeType = entity.mimeType,
                isDirectory = entity.isDirectory,
                lastModified = entity.lastModified,
                sizeBytes = entity.sizeBytes
            )
        }
        return sortMatches(mapped, sortMode, sortAscending).take(min(limit, MAX_RESULTS))
    }

    suspend fun deleteRootEntries(rootId: String) {
        database.indexedDocumentDao().deleteForRoot(rootId)
    }

    fun scheduleFullIndex(root: FileSearchRoot) {
        val request = buildIndexRequest(root)
        workManager.enqueueUniqueWork(uniqueWorkName(root.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun buildIndexRequest(root: FileSearchRoot): OneTimeWorkRequest {
        val input = workDataOf(
            FileSearchIndexWorker.KEY_ROOT_ID to root.id,
            FileSearchIndexWorker.KEY_ROOT_URI to root.uri.toString(),
            FileSearchIndexWorker.KEY_ROOT_DISPLAY_NAME to root.displayName
        )
        return OneTimeWorkRequestBuilder<FileSearchIndexWorker>()
            .setInputData(input)
            .addTag(indexTag(root.id))
            .build()
    }

    private fun uniqueWorkName(rootId: String): String = "file-search-index-$rootId"

    private fun indexTag(rootId: String): String = "file-search-index-tag-$rootId"

    private fun sortMatches(
        matches: List<FileSearchMatch>,
        sortMode: FileSearchSortMode,
        sortAscending: Boolean
    ): List<FileSearchMatch> {
        val comparator = when (sortMode) {
            FileSearchSortMode.DATE -> compareBy<FileSearchMatch> { it.lastModified }
            FileSearchSortMode.NAME -> compareBy<FileSearchMatch> { it.displayName.lowercase() }
        }
        val effectiveComparator = if (sortAscending) comparator else comparator.reversed()
        val directories = matches.filter { it.isDirectory }.sortedWith(effectiveComparator)
        val files = matches.filterNot { it.isDirectory }.sortedWith(effectiveComparator)
        return directories + files
    }

    private fun escapeLikeWildcards(input: String): String {
        return buildString(input.length) {
            input.forEach { char ->
                when (char) {
                    '%', '_', '\\' -> {
                        append('\\')
                        append(char)
                    }
                    else -> append(char)
                }
            }
        }
    }

    /**
     * Schedules periodic background sync for file changes.
     * @param intervalMinutes Sync interval in minutes. Pass 0 to disable.
     */
    fun schedulePeriodicSync(intervalMinutes: Int) {
        if (intervalMinutes <= 0) {
            cancelPeriodicSync()
            return
        }

        val request = PeriodicWorkRequestBuilder<IncrementalFileSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Cancels the periodic sync worker.
     */
    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }

    /**
     * Triggers an immediate incremental sync.
     * Uses expedited work when possible for faster execution.
     */
    fun triggerImmediateSync() {
        val request = OneTimeWorkRequestBuilder<IncrementalFileSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(request)
    }

    companion object {
        private const val MAX_RESULTS = 40
        private const val PERIODIC_SYNC_WORK_NAME = "file-search-periodic-sync"

        @Volatile
        private var INSTANCE: FileSearchRepository? = null

        fun getInstance(context: Context): FileSearchRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FileSearchRepository(context).also { INSTANCE = it }
            }
        }
    }
}

data class FileSearchMatch(
    val documentUri: String,
    val displayName: String,
    val relativePath: String,
    val rootDisplayName: String,
    val mimeType: String?,
    val isDirectory: Boolean,
    val lastModified: Long,
    val sizeBytes: Long
)
