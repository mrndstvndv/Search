package com.mrndstvndv.search.provider.model

import android.graphics.Bitmap

data class ProviderResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: Bitmap? = null,
    val providerId: String,
    val score: Float = 0f,
    val extras: Map<String, Any?> = emptyMap(),
    val onSelect: (() -> Unit)? = null
)
