package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import kotlin.math.roundToInt

@Composable
fun GeneralSettingsScreen(
    aliasRepository: AliasRepository,
    settingsRepository: ProviderSettingsRepository,
    onClose: () -> Unit
) {
    val aliasEntries by aliasRepository.aliases.collectAsState()
    val webSearchSettings by settingsRepository.webSearchSettings.collectAsState()
    val translucentResultsEnabled by settingsRepository.translucentResultsEnabled.collectAsState()
    val backgroundOpacity by settingsRepository.backgroundOpacity.collectAsState()
    val backgroundBlurStrength by settingsRepository.backgroundBlurStrength.collectAsState()
    var showWebSearchDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 50.dp)
            .padding(horizontal = 16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(onClick = onClose) {
                    Text(text = "Close")
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = "Web search",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Manage the search engines that appear in the main view.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Button(onClick = { showWebSearchDialog = true }) {
                Text(text = "Edit web search sites")
            }

            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Control how the search results list is rendered.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Translucent results",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Make the list items slightly see-through.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = translucentResultsEnabled,
                    onCheckedChange = { checked ->
                        settingsRepository.setTranslucentResultsEnabled(checked)
                    }
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background opacity",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Adjust how strong the dimmed background appears behind the search UI.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "${(backgroundOpacity * 100).roundToInt()}%"
                    )
                }
                Slider(
                    value = backgroundOpacity,
                    onValueChange = { settingsRepository.setBackgroundOpacity(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.padding(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background blur strength",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Adjust how strong the wallpaper blur looks behind the app.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "${(backgroundBlurStrength * 100).roundToInt()}%"
                    )
                }
                Slider(
                    value = backgroundBlurStrength,
                    onValueChange = { settingsRepository.setBackgroundBlurStrength(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                text = "Aliases",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Long press a result in the main search to add or update an alias.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.padding(8.dp))

            if (aliasEntries.isEmpty()) {
                Text(
                    text = "No aliases created yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(aliasEntries) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = entry.alias,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = entry.target.summary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { aliasRepository.removeAlias(entry.alias) }) {
                                Text(text = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWebSearchDialog) {
        WebSearchProviderSettingsDialog(
            initialSettings = webSearchSettings,
            onDismiss = { showWebSearchDialog = false },
            onSave = { newSettings ->
                settingsRepository.saveWebSearchSettings(newSettings)
                showWebSearchDialog = false
            }
        )
    }
}
