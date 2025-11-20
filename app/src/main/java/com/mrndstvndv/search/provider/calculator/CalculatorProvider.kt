package com.mrndstvndv.search.provider.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.util.CalculatorEngine
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CalculatorProvider(
    private val context: Context,
    private val settingsRepository: ProviderSettingsRepository
) : Provider {

    override val id: String = "calculator"
    override val displayName: String = "Calculator"

    override fun canHandle(query: Query): Boolean {
        if (settingsRepository.providerSettings.value.firstOrNull { it.id == id }?.isEnabled == false) {
            return false
        }
        return CalculatorEngine.isExpression(query.trimmedText)
    }

    override suspend fun query(query: Query): List<ProviderResult> {
        val expression = query.trimmedText
        val result = CalculatorEngine.compute(expression) ?: return emptyList()

        val action: suspend () -> Unit = {
            withContext(Dispatchers.Main) {
                copyToClipboard(result)
            }
        }

        return listOf(
            ProviderResult(
                id = "$id:$expression",
                title = "= $result",
                subtitle = expression,
                providerId = id,
                onSelect = action
            )
        )
    }

    private fun copyToClipboard(value: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("calculator", value)
        clipboard?.setPrimaryClip(clip)
    }
}
