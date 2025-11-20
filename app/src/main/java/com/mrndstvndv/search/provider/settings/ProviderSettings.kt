package com.mrndstvndv.search.provider.settings

import org.json.JSONObject

data class ProviderSettings(
    val id: String,
    val isEnabled: Boolean
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): ProviderSettings? {
            if (json == null) return null
            val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
            val isEnabled = json.optBoolean("isEnabled", true)
            return ProviderSettings(
                id = id,
                isEnabled = isEnabled
            )
        }
    }
}
