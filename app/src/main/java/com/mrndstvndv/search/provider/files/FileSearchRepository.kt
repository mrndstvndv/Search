package com.mrndstvndv.search.provider.files

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mrndstvndv.search.provider.files.index.FileSearchDatabase
import com.mrndstvndv.search.provider.files.index.IndexedDocumentDao
import com.mrndstvndv.search.provider.files.index.IndexedDocumentEntity
import com.mrndstvndv.search.provider.files.indexing.FileSearchIndexWorker
import com.mrndstvndv.search.provider.settings.FileSearchRoot
import kotlin.math.min

class FileSearchRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database: FileSearchDatabase = FileSearchDatabase.get(appContext)
    private val workManager = WorkManager.getInstance(appContext)

    suspend fun search(
        queryText: String,
        rootIds: List<String>,
        limit: Int = MAX_RESULTS
    ): List<FileSearchMatch> {
        val cleaned = queryText.trim()
        if (cleaned.isEmpty() || rootIds.isEmpty()) return emptyList()
        val normalized = "%${escapeLikeWildcards(cleaned)}%"
        val dao = database.indexedDocumentDao()
        val matches = dao.search(rootIds, normalized, min(limit, MAX_RESULTS))
        return matches.map { entity ->
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

    companion object {
        private const val MAX_RESULTS = 40

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
