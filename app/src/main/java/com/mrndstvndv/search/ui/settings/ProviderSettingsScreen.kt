package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.provider.Provider
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository

@Composable
fun ProviderSettingsScreen(
    settingsRepository: ProviderSettingsRepository,
    providers: List<Provider>,
    onNavigateToWebSearchSettings: () -> Unit
) {
    val providerSettings by settingsRepository.providerSettings.collectAsState()

    Scaffold(
        topBar = {
            Text(
                text = "Provider Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(providers) { provider ->
                val isEnabled = providerSettings.find { it.id == provider.id }?.isEnabled ?: true
                ProviderRow(
                    provider = provider,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        settingsRepository.setProviderEnabled(provider.id, enabled)
                    },
                    onClick = {
                        if (provider.id == "web-search") {
                            onNavigateToWebSearchSettings()
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: Provider,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = provider.displayName, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}
