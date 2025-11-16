package com.mrndstvndv.search.provider.debug

import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Debug-only provider that surfaces an entry to intentionally block on selection.
 * Useful for verifying loading indicators or pending-action UI flows.
 */
class DebugLongOperationProvider(
    private val activity: ComponentActivity,
    private val defaultDurationMs: Long = DEFAULT_DURATION_MS
) : Provider {

    override val id: String = "debug-long-operation"
    override val displayName: String = "Debug • Long Operation"

    override fun canHandle(query: Query): Boolean {
        val normalized = query.trimmedText.lowercase()
        if (normalized.isEmpty()) return false
        return normalized.startsWith("debug") || normalized.startsWith("long test")
    }

    override suspend fun query(query: Query): List<ProviderResult> {
        if (!canHandle(query)) return emptyList()
        val normalized = query.trimmedText.lowercase()
        val durationMs = extractDuration(normalized) ?: defaultDurationMs
        val seconds = durationMs / 1000.0
        val title = "Long operation (${seconds}s)"
        val subtitle = "Blocks for ${durationMs}ms to test loading overlay"

        val action: suspend () -> Unit = {
            delay(durationMs)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Debug wait finished (${seconds}s)", Toast.LENGTH_SHORT).show()
            }
        }

        return listOf(
            ProviderResult(
                id = "$id:$durationMs",
                title = title,
                subtitle = subtitle,
                providerId = id,
                onSelect = action
            )
        )
    }

    private fun extractDuration(text: String): Long? {
        val number = NUMBER_REGEX.find(text)?.value?.toLongOrNull() ?: return null
        val clampedSeconds = number.coerceIn(1, 30)
        return clampedSeconds * 1000L
    }

    private companion object {
        const val DEFAULT_DURATION_MS = 4000L
        val NUMBER_REGEX = Regex("(\\d+)")
    }
}
