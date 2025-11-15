package com.mrndstvndv.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.ui.settings.GeneralSettingsScreen
import com.mrndstvndv.search.ui.theme.SearchTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val aliasRepository = remember { AliasRepository(this@SettingsActivity) }
            val settingsRepository = remember { ProviderSettingsRepository(this@SettingsActivity) }
            SearchTheme {
                GeneralSettingsScreen(
                    aliasRepository = aliasRepository,
                    settingsRepository = settingsRepository,
                    onClose = { finish() }
                )
            }
        }
    }
}
