package com.mrndstvndv.search.alias

import org.json.JSONObject

sealed interface AliasTarget {
    val providerId: String
    val summary: String
    fun toJson(): JSONObject

    companion object {
        private const val KEY_TYPE = "type"
        private const val TYPE_WEB_SEARCH = "web-search"
        private const val TYPE_APP_LAUNCH = "app-launch"

        fun fromJson(json: JSONObject?): AliasTarget? {
            if (json == null) return null
            val type = json.optString(KEY_TYPE, null) ?: return null
            return when (type) {
                TYPE_WEB_SEARCH -> WebSearchAliasTarget.fromJson(json)
                TYPE_APP_LAUNCH -> AppLaunchAliasTarget.fromJson(json)
                else -> null
            }
        }
    }
}

data class AliasEntry(
    val alias: String,
    val target: AliasTarget,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private const val KEY_ALIAS = "alias"
        private const val KEY_TARGET = "target"
        private const val KEY_CREATED_AT = "createdAt"

        fun fromJson(json: JSONObject?): AliasEntry? {
            if (json == null) return null
            val alias = json.optString(KEY_ALIAS, null) ?: return null
            val target = AliasTarget.fromJson(json.optJSONObject(KEY_TARGET)) ?: return null
            val createdAt = json.optLong(KEY_CREATED_AT, System.currentTimeMillis())
            return AliasEntry(alias, target, createdAt)
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ALIAS, alias)
            put(KEY_TARGET, target.toJson())
            put(KEY_CREATED_AT, createdAt)
        }
    }
}

data class AliasMatch(
    val entry: AliasEntry,
    val remainingQuery: String
)

data class AliasCreationCandidate(
    val target: AliasTarget,
    val suggestion: String,
    val description: String
)

data class WebSearchAliasTarget(
    val siteId: String,
    val displayName: String
) : AliasTarget {
    override val providerId: String = "web-search"
    override val summary: String
        get() = displayName

    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", TYPE)
            put("siteId", siteId)
            put("displayName", displayName)
        }
    }

    companion object {
        private const val TYPE = "web-search"

        fun fromJson(json: JSONObject?): WebSearchAliasTarget? {
            if (json == null) return null
            val siteId = json.optString("siteId", null) ?: return null
            val displayName = json.optString("displayName", null) ?: return null
            return WebSearchAliasTarget(siteId, displayName)
        }
    }
}

data class AppLaunchAliasTarget(
    val packageName: String,
    val label: String
) : AliasTarget {
    override val providerId: String = "app-list"
    override val summary: String
        get() = label

    override fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", TYPE)
            put("packageName", packageName)
            put("label", label)
        }
    }

    companion object {
        private const val TYPE = "app-launch"

        fun fromJson(json: JSONObject?): AppLaunchAliasTarget? {
            if (json == null) return null
            val packageName = json.optString("packageName", null) ?: return null
            val label = json.optString("label", null) ?: return null
            return AppLaunchAliasTarget(packageName, label)
        }
    }
}
