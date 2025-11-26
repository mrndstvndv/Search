package com.mrndstvndv.search.provider.apps

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.util.loadAppIconBitmap
import com.mrndstvndv.search.alias.AppLaunchAliasTarget
import com.mrndstvndv.search.util.FuzzyMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListProvider(
    private val activity: ComponentActivity,
    private val settingsRepository: ProviderSettingsRepository,
    private val iconSize: Int
) : Provider {

    override val id: String = "app-list"
    override val displayName: String = "Applications"

    private val packageManager = activity.packageManager
    private val iconCache = mutableMapOf<String, Bitmap?>()
    private val applications by lazy { loadApplications() }

    override fun canHandle(query: Query): Boolean = true

    override suspend fun query(query: Query): List<ProviderResult> {
        val normalized = query.trimmedText
        val settings = settingsRepository.appSearchSettings.value
        val includePackageName = settings.includePackageName

        val matches: List<ScoredApp> = if (normalized.isBlank()) {
            // No query - return all apps with zero score
            applications.map { ScoredApp(it, 0, emptyList()) }
        } else {
            // Apply fuzzy matching and scoring
            applications.mapNotNull { app ->
                val labelMatch = FuzzyMatcher.match(normalized, app.label)
                val packageMatch = if (includePackageName) {
                    FuzzyMatcher.match(normalized, app.packageName)
                } else null

                // Determine if label match is the best (with package name penalty applied)
                val packageScoreWithPenalty = (packageMatch?.score ?: Int.MIN_VALUE) - PACKAGE_NAME_PENALTY
                val labelIsBest = labelMatch != null && labelMatch.score >= packageScoreWithPenalty

                // Calculate effective score and matched indices
                when {
                    labelIsBest -> ScoredApp(
                        app = app,
                        score = labelMatch!!.score,
                        matchedIndices = labelMatch.matchedIndices
                    )
                    packageMatch != null -> ScoredApp(
                        app = app,
                        score = packageScoreWithPenalty,
                        matchedIndices = emptyList() // Package name matches don't highlight title
                    )
                    else -> null
                }
            }.sortedByDescending { it.score }
        }

        val limited = matches.take(MAX_RESULTS)
        val results = mutableListOf<ProviderResult>()

        for (scoredApp in limited) {
            val entry = scoredApp.app
            val action: suspend () -> Unit = {
                withContext(Dispatchers.Main) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(entry.packageName)
                    if (launchIntent != null) {
                        activity.startActivity(launchIntent)
                        activity.finish()
                    }
                }
            }
            results += ProviderResult(
                id = "$id:${entry.packageName}",
                title = entry.label,
                subtitle = entry.packageName,
                icon = null,
                defaultVectorIcon = Icons.Outlined.Android,
                iconLoader = { loadIcon(entry.packageName) },
                providerId = id,
                extras = mapOf(EXTRA_PACKAGE_NAME to entry.packageName),
                onSelect = action,
                aliasTarget = AppLaunchAliasTarget(entry.packageName, entry.label),
                keepOverlayUntilExit = true,
                matchedTitleIndices = scoredApp.matchedIndices
            )
        }
        return results
    }

    private fun loadApplications(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager).toString().takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                AppEntry(packageName, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private suspend fun loadIcon(packageName: String): Bitmap? {
        if (iconCache.containsKey(packageName)) return iconCache[packageName]
        val icon = withContext(Dispatchers.IO) {
            loadAppIconBitmap(packageManager, packageName, iconSize)
        }
        iconCache[packageName] = icon
        return icon
    }

    private data class AppEntry(val packageName: String, val label: String)

    private data class ScoredApp(
        val app: AppEntry,
        val score: Int,
        val matchedIndices: List<Int>
    )

    private companion object {
        const val MAX_RESULTS = 40
        private const val EXTRA_PACKAGE_NAME = "packageName"
        /** Penalty applied to package name matches so label matches rank higher */
        private const val PACKAGE_NAME_PENALTY = 10
    }
}
