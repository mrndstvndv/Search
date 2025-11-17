package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import kotlin.math.roundToInt

@Composable
fun GeneralSettingsScreen(
    aliasRepository: AliasRepository,
    settingsRepository: ProviderSettingsRepository,
    appName: String,
    isDefaultAssistant: Boolean,
    onRequestSetDefaultAssistant: () -> Unit,
    onClose: () -> Unit
) {
    val aliasEntries by aliasRepository.aliases.collectAsState()
    val webSearchSettings by settingsRepository.webSearchSettings.collectAsState()
    val translucentResultsEnabled by settingsRepository.translucentResultsEnabled.collectAsState()
    val backgroundOpacity by settingsRepository.backgroundOpacity.collectAsState()
    val backgroundBlurStrength by settingsRepository.backgroundBlurStrength.collectAsState()
    val activityIndicatorDelayMs by settingsRepository.activityIndicatorDelayMs.collectAsState()
    val animationsEnabled by settingsRepository.animationsEnabled.collectAsState()
    var showWebSearchDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                SettingsHeader(onClose = onClose)
            }

            if (!isDefaultAssistant) {
                item {
                    DefaultAssistantCard(
                        appName = appName,
                        onRequestSetDefaultAssistant = onRequestSetDefaultAssistant
                    )
                }
            }

            item {
                SettingsSection(
                    title = "Web search",
                    subtitle = "Manage the search engines that appear in the main view."
                ) {
                    SettingsCardGroup {
                        SettingsActionRow(
                            title = "Search providers",
                            subtitle = "Choose which engines appear on the sheet.",
                            onClick = { showWebSearchDialog = true }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = "Appearance",
                    subtitle = "Control how the search results list is rendered."
                ) {
                    SettingsCardGroup {
                        SettingsToggleRow(
                            title = "Translucent results",
                            subtitle = "Make the list items slightly see-through.",
                            checked = translucentResultsEnabled,
                            onCheckedChange = { settingsRepository.setTranslucentResultsEnabled(it) }
                        )
                        SettingsDivider()
                        SettingsSliderRow(
                            title = "Background opacity",
                            subtitle = "Dim the wallpaper behind the search UI.",
                            valueText = "${(backgroundOpacity * 100).roundToInt()}%",
                            value = backgroundOpacity,
                            onValueChange = { settingsRepository.setBackgroundOpacity(it) }
                        )
                        SettingsDivider()
                        SettingsSliderRow(
                            title = "Background blur strength",
                            subtitle = "Adjust how strong the wallpaper blur looks behind the app.",
                            valueText = "${(backgroundBlurStrength * 100).roundToInt()}%",
                            value = backgroundBlurStrength,
                            onValueChange = { settingsRepository.setBackgroundBlurStrength(it) }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = "Behavior",
                    subtitle = "Adjust how quickly Search reacts to your actions."
                ) {
                    SettingsCardGroup {
                        SettingsToggleRow(
                            title = "Enable animations",
                            subtitle = "Turn off to remove most in-app transitions.",
                            checked = animationsEnabled,
                            onCheckedChange = { settingsRepository.setAnimationsEnabled(it) }
                        )
                        SettingsDivider()
                        SettingsSliderRow(
                            title = "Activity indicator delay",
                            subtitle = "Wait time before showing the loading spinner.",
                            valueText = "${activityIndicatorDelayMs} ms",
                            value = activityIndicatorDelayMs.toFloat(),
                            onValueChange = { settingsRepository.setActivityIndicatorDelayMs(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            steps = 19
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = "Aliases",
                    subtitle = "Long press a result in the main search to add or update an alias."
                ) {
                    SettingsCardGroup {
                        if (aliasEntries.isEmpty()) {
                            Text(
                                modifier = Modifier.padding(20.dp),
                                text = "No aliases created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column {
                                aliasEntries.forEachIndexed { index, entry ->
                                    AliasRow(
                                        alias = entry.alias,
                                        summary = entry.target.summary,
                                        onRemove = { aliasRepository.removeAlias(entry.alias) }
                                    )
                                    if (index < aliasEntries.lastIndex) {
                                        SettingsDivider()
                                    }
                                }
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

@Composable
private fun SettingsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tune how Search behaves on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsCardGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit section"
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    subtitle: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 9
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun AliasRow(
    alias: String,
    summary: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alias,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onRemove) {
            Text(text = "Remove")
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun DefaultAssistantCard(
    appName: String,
    onRequestSetDefaultAssistant: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Set $appName as the default assistant",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$appName can open instantly from the system assistant gesture.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestSetDefaultAssistant) {
                Text(text = "Set as default")
            }
        }
    }
}
