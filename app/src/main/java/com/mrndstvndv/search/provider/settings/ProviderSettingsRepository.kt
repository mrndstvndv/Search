package com.mrndstvndv.search.provider.settings

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.core.content.edit

class ProviderSettingsRepository(context: Context) {
    companion object {
        private const val PREF_NAME = "provider_settings"
        private const val KEY_WEB_SEARCH = "web_search"
        private const val KEY_TRANSLUCENT_RESULTS = "translucent_results"
        private const val KEY_BACKGROUND_OPACITY = "background_opacity"
        private const val DEFAULT_BACKGROUND_OPACITY = 0.35f
    }

    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _webSearchSettings = MutableStateFlow(loadWebSearchSettings())
    val webSearchSettings: StateFlow<WebSearchSettings> = _webSearchSettings

    private val _translucentResultsEnabled = MutableStateFlow(loadTranslucentResultsEnabled())
    val translucentResultsEnabled: StateFlow<Boolean> = _translucentResultsEnabled

    private val _backgroundOpacity = MutableStateFlow(loadBackgroundOpacity())
    val backgroundOpacity: StateFlow<Float> = _backgroundOpacity

    fun saveWebSearchSettings(settings: WebSearchSettings) {
        preferences.edit { putString(KEY_WEB_SEARCH, settings.toJsonString()) }
        _webSearchSettings.value = settings
    }

    fun setTranslucentResultsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_TRANSLUCENT_RESULTS, enabled) }
        _translucentResultsEnabled.value = enabled
    }

    fun setBackgroundOpacity(alpha: Float) {
        val coercedAlpha = alpha.coerceIn(0f, 1f)
        preferences.edit { putFloat(KEY_BACKGROUND_OPACITY, coercedAlpha) }
        _backgroundOpacity.value = coercedAlpha
    }

    private fun loadWebSearchSettings(): WebSearchSettings {
        val json = preferences.getString(KEY_WEB_SEARCH, null) ?: return WebSearchSettings.default()
        return try {
            WebSearchSettings.fromJson(JSONObject(json)) ?: WebSearchSettings.default()
        } catch (ignored: JSONException) {
            WebSearchSettings.default()
        }
    }

    private fun loadTranslucentResultsEnabled(): Boolean {
        return preferences.getBoolean(KEY_TRANSLUCENT_RESULTS, false)
    }

    private fun loadBackgroundOpacity(): Float {
        return preferences.getFloat(KEY_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY)
    }
}

data class WebSearchSettings(
    val defaultSiteId: String,
    val sites: List<WebSearchSite>
) {
    companion object {
        private val DEFAULT_SITES = listOf(
            WebSearchSite(
                id = "bing",
                displayName = "Bing",
                urlTemplate = "https://www.bing.com/search?q={query}&form=QBLH"
            ),
            WebSearchSite(
                id = "duckduckgo",
                displayName = "DuckDuckGo",
                urlTemplate = "https://duckduckgo.com/?q={query}"
            ),
            WebSearchSite(
                id = "google",
                displayName = "Google",
                urlTemplate = "https://www.google.com/search?q={query}"
            ),
            WebSearchSite(
                id = "youtube",
                displayName = "YouTube",
                urlTemplate = "https://m.youtube.com/results?search_query={query}"
            ),
            WebSearchSite(
                id = "twitter",
                displayName = "Twitter",
                urlTemplate = "https://x.com/search?q={query}"
            )
        )
        const val QUERY_PLACEHOLDER = "{query}"

        fun default(): WebSearchSettings {
            return WebSearchSettings(
                defaultSiteId = DEFAULT_SITES.first().id,
                sites = DEFAULT_SITES
            )
        }

        fun fromJson(json: JSONObject?): WebSearchSettings? {
            if (json == null) return null
            val array = json.optJSONArray("sites") ?: return null
            val sites = mutableListOf<WebSearchSite>()
            for (i in 0 until array.length()) {
                WebSearchSite.fromJson(array.optJSONObject(i))?.let { sites.add(it) }
            }
            if (sites.isEmpty()) return null
            val candidate = json.optString("defaultSiteId", sites.first().id)
            val defaultId = sites.firstOrNull { it.id == candidate }?.id ?: sites.first().id
            return WebSearchSettings(defaultSiteId = defaultId, sites = sites)
        }
    }

    fun siteForId(id: String?): WebSearchSite? {
        return sites.firstOrNull { it.id == id }
    }

    fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("defaultSiteId", defaultSiteId)
        val array = JSONArray()
        sites.forEach { array.put(it.toJson()) }
        root.put("sites", array)
        return root
    }

    fun toJsonString(): String = toJson().toString()
}

data class WebSearchSite(
    val id: String,
    val displayName: String,
    val urlTemplate: String
) {
    fun buildUrl(query: String): String {
        val template = normalizedTemplate()
        val encoded = Uri.encode(query)
        return template.replace(WebSearchSettings.QUERY_PLACEHOLDER, encoded)
    }

    private fun normalizedTemplate(): String {
        return if (urlTemplate.contains(WebSearchSettings.QUERY_PLACEHOLDER)) {
            urlTemplate
        } else if (urlTemplate.contains("?")) {
            "${urlTemplate}&q=${WebSearchSettings.QUERY_PLACEHOLDER}"
        } else {
            "${urlTemplate}?q=${WebSearchSettings.QUERY_PLACEHOLDER}"
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("displayName", displayName)
            put("urlTemplate", urlTemplate)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): WebSearchSite? {
            if (json == null) return null
            val id = json.opt("id") as? String ?: return null
            val name = json.opt("displayName") as? String ?: return null
            val template = json.opt("urlTemplate") as? String ?: return null
            return WebSearchSite(id = id, displayName = name, urlTemplate = template)
        }
    }
}
