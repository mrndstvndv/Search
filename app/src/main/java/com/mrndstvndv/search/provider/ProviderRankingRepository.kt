package com.mrndstvndv.search.provider

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONException

/**
 * Manages provider display order/ranking.
 * Ensures results are always sorted by user-defined provider order, not by load speed.
 */
class ProviderRankingRepository(context: Context) {
    companion object {
        private const val PREF_NAME = "provider_rankings"
        private const val KEY_PROVIDER_ORDER = "provider_order"

        // Default provider order (used if not yet customized by user)
        private val DEFAULT_PROVIDER_ORDER = listOf(
            "app-list",
            "calculator",
            "text-utilities",
            "file-search",
            "web-search",
            "debug-long-operation"
        )

        @Volatile
        private var INSTANCE: ProviderRankingRepository? = null

        fun getInstance(context: Context): ProviderRankingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProviderRankingRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _providerOrder = MutableStateFlow(loadProviderOrder())
    val providerOrder: StateFlow<List<String>> = _providerOrder

    /**
     * Get the rank/position of a provider (lower = higher priority)
     */
    fun getRank(providerId: String): Int {
        return _providerOrder.value.indexOf(providerId).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    /**
     * Update the provider order
     */
    fun setProviderOrder(order: List<String>) {
        _providerOrder.value = order
        saveProviderOrder(order)
    }

    /**
     * Move provider up in ranking (higher priority)
     */
    fun moveUp(providerId: String) {
        val current = _providerOrder.value.toMutableList()
        val index = current.indexOf(providerId)
        if (index > 0) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            setProviderOrder(current)
        }
    }

    /**
     * Move provider down in ranking (lower priority)
     */
    fun moveDown(providerId: String) {
        val current = _providerOrder.value.toMutableList()
        val index = current.indexOf(providerId)
        if (index < current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            setProviderOrder(current)
        }
    }

    private fun loadProviderOrder(): List<String> {
        return try {
            val json = preferences.getString(KEY_PROVIDER_ORDER, null)
            if (json != null) {
                val array = JSONArray(json)
                (0 until array.length()).map { array.getString(it) }
            } else {
                DEFAULT_PROVIDER_ORDER
            }
        } catch (e: JSONException) {
            DEFAULT_PROVIDER_ORDER
        }
    }

    private fun saveProviderOrder(order: List<String>) {
        preferences.edit {
            val array = JSONArray(order)
            putString(KEY_PROVIDER_ORDER, array.toString())
        }
    }
}
