package com.mrndstvndv.search

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.provider.files.FileSearchRepository
import com.mrndstvndv.search.provider.files.FileThumbnailRepository
import com.mrndstvndv.search.provider.ProviderRankingRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.settings.AssistantRoleManager
import com.mrndstvndv.search.ui.settings.GeneralSettingsScreen
import com.mrndstvndv.search.ui.settings.ProviderSettingsScreen
import com.mrndstvndv.search.ui.settings.WebSearchSettingsScreen
import com.mrndstvndv.search.ui.theme.SearchTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Bundle
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {
    private val assistantRoleManager by lazy { AssistantRoleManager(this) }
    private val defaultAssistantState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            val aliasRepository = remember { AliasRepository(this@SettingsActivity) }
            val settingsRepository = remember { ProviderSettingsRepository(this@SettingsActivity) }
            val fileSearchRepository = remember { FileSearchRepository.getInstance(this@SettingsActivity) }
            val rankingRepository = remember { ProviderRankingRepository.getInstance(this@SettingsActivity) }
            val fileThumbnailRepository = remember { FileThumbnailRepository.getInstance(this@SettingsActivity) }
            val isDefaultAssistant by defaultAssistantState
            val motionPreferences by settingsRepository.motionPreferences.collectAsState()
            val webSearchSettings by settingsRepository.webSearchSettings.collectAsState()
            val appName = getString(R.string.app_name)
            val navController = rememberNavController()
            val providers = remember {
                com.mrndstvndv.search.provider.Providers(
                    context = this@SettingsActivity,
                    settingsRepository = settingsRepository,
                    fileSearchRepository = fileSearchRepository,
                    fileThumbnailRepository = fileThumbnailRepository
                ).get()
            }

            SearchTheme(motionPreferences = motionPreferences) {
                LaunchedEffect(Unit) {
                    refreshDefaultAssistantState()
                }

                NavHost(navController = navController, startDestination = "general") {
                    composable("general") {
                        GeneralSettingsScreen(
                            aliasRepository = aliasRepository,
                            settingsRepository = settingsRepository,
                            fileSearchRepository = fileSearchRepository,
                            rankingRepository = rankingRepository,
                            appName = appName,
                            isDefaultAssistant = isDefaultAssistant,
                            onRequestSetDefaultAssistant = { assistantRoleManager.launchDefaultAssistantSettings() },
                            onOpenWebSearchSettings = { navController.navigate("web_search") },
                            onOpenProviderSettings = { navController.navigate("provider_settings") },
                            onClose = { finish() }
                        )
                    }
                    composable("web_search") {
                        WebSearchSettingsScreen(
                            initialSettings = webSearchSettings,
                            onBack = { navController.popBackStack() },
                            onSave = { newSettings ->
                                settingsRepository.saveWebSearchSettings(newSettings)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("provider_settings") {
                        ProviderSettingsScreen(
                            settingsRepository = settingsRepository,
                            providers = providers,
                            onNavigateToWebSearchSettings = { navController.navigate("web_search") }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultAssistantState()
    }

    private fun refreshDefaultAssistantState() {
        lifecycleScope.launch(Dispatchers.Default) {
            val isDefault = assistantRoleManager.isDefaultAssistant()
            withContext(Dispatchers.Main) {
                defaultAssistantState.value = isDefault
            }
        }
    }
}
