package com.mrndstvndv.search.provider.model

data class Query(
    val text: String,
    val source: QuerySource = QuerySource.USER_INPUT
) {
    val trimmedText: String
        get() = text.trim()

    val isBlank: Boolean
        get() = trimmedText.isEmpty()
}

enum class QuerySource {
    USER_INPUT,
    SHORTCUT,
    PROGRAMMATIC
}
