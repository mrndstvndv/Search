package com.mrndstvndv.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.settings.AssistantRoleManager
import com.mrndstvndv.search.ui.settings.GeneralSettingsScreen
import com.mrndstvndv.search.ui.theme.SearchTheme

class SettingsActivity : ComponentActivity() {
    private val assistantRoleManager by lazy { AssistantRoleManager(this) }
    private val defaultAssistantState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshDefaultAssistantState()

        setContent {
            val aliasRepository = remember { AliasRepository(this@SettingsActivity) }
            val settingsRepository = remember { ProviderSettingsRepository(this@SettingsActivity) }
            val isDefaultAssistant by defaultAssistantState
            val appName = getString(R.string.app_name)
            SearchTheme {
                GeneralSettingsScreen(
                    aliasRepository = aliasRepository,
                    settingsRepository = settingsRepository,
                    appName = appName,
                    isDefaultAssistant = isDefaultAssistant,
                    onRequestSetDefaultAssistant = { assistantRoleManager.launchDefaultAssistantSettings() },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultAssistantState()
    }

    private fun refreshDefaultAssistantState() {
        defaultAssistantState.value = assistantRoleManager.isDefaultAssistant()
    }
}
