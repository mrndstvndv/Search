package com.mrndstvndv.search.provider

import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query

interface Provider {

    /** Unique provider identifier (e.g., "app-list"). */
    val id: String

    /** Human readable name shown in settings or logs. */
    val displayName: String

    /** Optional initialization hook for heavy setup. */
    fun initialize() {}

    /** Returns true when this provider is interested in the current query. */
    fun canHandle(query: Query): Boolean

    /** Resolves zero or more results for the current query. */
    suspend fun query(query: Query): List<ProviderResult>

    /** Cleanup hook for disposing resources, if needed. */
    fun dispose() {}
}
