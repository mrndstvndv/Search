package com.mrndstvndv.search.provider.web

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.ComponentActivity
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query

class WebSearchProvider(
    private val activity: ComponentActivity
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

        val searchUrl = buildSearchUrl(cleaned)
        val action = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            activity.startActivity(intent)
            activity.finish()
        }

        return listOf(
            ProviderResult(
                id = "$id:${cleaned.hashCode()}",
                title = "Search \"$cleaned\"",
                subtitle = "Bing",
                providerId = id,
                onSelect = action
            )
        )
    }

    private fun buildSearchUrl(text: String): String {
        val encoded = Uri.encode(text)
        return "https://www.bing.com/search?q=$encoded&form=QBLH"
    }
}
