package com.mrndstvndv.search.provider.web

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.ComponentActivity
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import androidx.core.net.toUri

class WebSearchProvider(
    private val activity: ComponentActivity,
    private val settingsRepository: ProviderSettingsRepository
) : Provider {

    override val id: String = "web-search"
    override val displayName: String = "Web Search"

    override fun canHandle(query: Query): Boolean {
        val cleaned = query.trimmedText
        if (cleaned.isBlank()) return false
        return !Patterns.WEB_URL.matcher(cleaned).matches()
    }

    override suspend fun query(query: Query): List<ProviderResult> {
        val cleaned = query.trimmedText
        if (cleaned.isBlank()) return emptyList()

        val settings = settingsRepository.webSearchSettings.value
        val sites = settings.sites
        if (sites.isEmpty()) return emptyList()
        val orderedSites = settings.siteForId(settings.defaultSiteId)?.let { defaultSite ->
            listOf(defaultSite) + sites.filter { it.id != defaultSite.id }
        } ?: sites
        return orderedSites.map { site ->
            val searchUrl = site.buildUrl(cleaned)
            val action = {
                val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri())
                activity.startActivity(intent)
                activity.finish()
            }
            ProviderResult(
                id = "$id:${site.id}:${cleaned.hashCode()}",
                title = "Search \"$cleaned\"",
                subtitle = if (site.id == settings.defaultSiteId) {
                    "${site.displayName} (default)"
                } else {
                    site.displayName
                },
                providerId = id,
                onSelect = action
            )
        }
    }
}
