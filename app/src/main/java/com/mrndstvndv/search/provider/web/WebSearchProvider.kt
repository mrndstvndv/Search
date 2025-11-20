package com.mrndstvndv.search.provider.web

import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.core.net.toUri
import com.mrndstvndv.search.alias.WebSearchAliasTarget
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// TODO: the query should be empty if a whitespace between the trigger and query is not yet made. the behavior right now is that the searchitem searches for the trigger if a query is not yet made
class WebSearchProvider(
    private val context: Context,
    private val settingsRepository: ProviderSettingsRepository
) : Provider {

    override val id: String = "web-search"
    override val displayName: String = "Web Search"

    override fun canHandle(query: Query): Boolean {
        if (settingsRepository.providerSettings.value.firstOrNull { it.id == id }?.isEnabled == false) {
            return false
        }
        val cleaned = query.trimmedText
        if (cleaned.isBlank()) return false
        return !Patterns.WEB_URL.matcher(cleaned).matches()
    }

    override suspend fun query(query: Query): List<ProviderResult> {
        val cleaned = query.trimmedText
        if (cleaned.isBlank()) return emptyList()

        val settings = settingsRepository.webSearchSettings.value
        val sites = settings.sites.filter { it.isEnabled } // Filter out disabled sites
        if (sites.isEmpty()) return emptyList()
        val defaultSite = settings.siteForId(settings.defaultSiteId) ?: sites.first()
        val triggerToken = query.originalText.trimStart().takeWhile { char ->
            !char.isWhitespace() && char != ':'
        }
        val triggerMatches = if (triggerToken.isBlank()) {
            emptyList()
        } else {
            sites.filter { it.id != defaultSite.id }
                .filter { site -> site.displayName.contains(triggerToken, ignoreCase = true) }
        }
        val searchTerms = dropTriggerToken(cleaned, triggerToken).ifBlank { cleaned }.trim()
        val visibleSites = if (triggerMatches.isEmpty()) {
            listOf(defaultSite)
        } else {
            triggerMatches + defaultSite
        }
        return visibleSites.map { site ->
            val actualQuery = if (site.id == defaultSite.id) cleaned else searchTerms
            val searchUrl = site.buildUrl(actualQuery)
            val action: suspend () -> Unit = {
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri())
                    context.startActivity(intent)
                }
            }
            ProviderResult(
                id = "$id:${site.id}:${actualQuery.hashCode()}",
                title = "Search \"$actualQuery\"",
                subtitle = if (site.id == defaultSite.id) {
                    "${site.displayName} (default)"
                } else {
                    site.displayName
                },
                providerId = id,
                onSelect = action,
                aliasTarget = WebSearchAliasTarget(site.id, site.displayName),
                keepOverlayUntilExit = true
            )
        }
    }

    private fun dropTriggerToken(queryText: String, triggerToken: String): String {
        if (triggerToken.isBlank()) return queryText.trimStart()
        val trimmed = queryText.trimStart()
        val lowerTrimmed = trimmed.lowercase()
        val lowerToken = triggerToken.lowercase()
        if (!lowerTrimmed.startsWith(lowerToken)) return trimmed
        if (trimmed.length > triggerToken.length) {
            val boundary = trimmed[triggerToken.length]
            if (!boundary.isWhitespace()) return trimmed
        }
        return trimmed.substring(triggerToken.length).trimStart()
    }
}
