package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mrndstvndv.search.provider.settings.WebSearchSettings
import com.mrndstvndv.search.provider.settings.WebSearchSite
import com.mrndstvndv.search.ui.components.ScrimDialog

@Composable
fun WebSearchSettingsScreen(
    initialSettings: WebSearchSettings,
    onBack: () -> Unit,
    onSave: (WebSearchSettings) -> Unit
) {
    var sites by remember { mutableStateOf(initialSettings.sites) }
    var defaultSiteId by remember { mutableStateOf(initialSettings.defaultSiteId) }
    var editingSite by remember { mutableStateOf<Pair<Int, WebSearchSite>?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    val placeholder = WebSearchSettings.QUERY_PLACEHOLDER

    LaunchedEffect(initialSettings) {
        sites = initialSettings.sites
        defaultSiteId = initialSettings.defaultSiteId
    }

    val allTemplatesValid = sites.all { it.urlTemplate.contains(placeholder) }

    fun saveSettings() {
        val resolvedDefault = sites.firstOrNull { it.id == defaultSiteId }?.id
            ?: sites.firstOrNull()?.id
            ?: ""
        if (resolvedDefault.isNotBlank() && allTemplatesValid) {
            onSave(WebSearchSettings(resolvedDefault, sites))
        }
    }

    fun updateSite(index: Int, updater: (WebSearchSite) -> WebSearchSite) {
        val mutable = sites.toMutableList()
        mutable[index] = updater(mutable[index])
        sites = mutable
        saveSettings()
    }

    fun removeSite(index: Int) {
        if (sites.size <= 1) return
        val removed = sites[index]
        val mutable = sites.toMutableList().also { it.removeAt(index) }
        sites = mutable
        if (defaultSiteId == removed.id) {
            defaultSiteId = mutable.firstOrNull()?.id.orEmpty()
        }
        saveSettings()
    }

    fun addCustomSite(name: String, template: String, onError: (String) -> Unit): Boolean {
        val trimmedName = name.trim()
        val trimmedTemplate = template.trim()
        if (trimmedName.isBlank() || trimmedTemplate.isBlank()) {
            onError("Name and template are required")
            return false
        }
        if (!trimmedTemplate.contains(placeholder)) {
            onError("Template must include $placeholder")
            return false
        }
        val candidateId = trimmedName.lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { trimmedName.lowercase() }
        if (sites.any { it.id == candidateId }) {
            onError("A site with that name already exists")
            return false
        }
        val newSite = WebSearchSite(id = candidateId, displayName = trimmedName, urlTemplate = trimmedTemplate)
        sites = sites + newSite
        saveSettings()
        return true
    }

    // Edit dialog
    editingSite?.let { (index, site) ->
        WebSearchSiteEditDialog(
            site = site,
            canRemove = sites.size > 1,
            onDismiss = { editingSite = null },
            onSave = { updatedSite ->
                updateSite(index) { updatedSite }
                editingSite = null
            },
            onRemove = {
                removeSite(index)
                editingSite = null
            }
        )
    }

    // Add dialog
    if (isAddDialogOpen) {
        WebSearchSiteAddDialog(
            placeholder = placeholder,
            onDismiss = { isAddDialogOpen = false },
            onAdd = { name, template, onError ->
                if (addCustomSite(name, template, onError)) {
                    isAddDialogOpen = false
                }
            }
        )
    }

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
                WebSearchHeader(onBack = onBack)
            }

            item {
                SettingsSection(
                    title = "Search Engines",
                    subtitle = "Manage your search providers. Use $placeholder as the query placeholder."
                ) {
                    SettingsCardGroup {
                        sites.forEachIndexed { index, site ->
                            WebSearchSiteRow(
                                site = site,
                                isDefault = defaultSiteId == site.id,
                                onSetDefault = {
                                    defaultSiteId = site.id
                                    saveSettings()
                                },
                                onEdit = { editingSite = index to site }
                            )
                            if (index < sites.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        TextButton(
                            onClick = { isAddDialogOpen = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Add Search Engine")
                        }
                    }
                }
            }

            if (!allTemplatesValid) {
                item {
                    Text(
                        text = "Every template needs $placeholder",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSearchHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Web Search",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure search providers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun WebSearchSiteRow(
    site: WebSearchSite,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tap area with display name and URL template
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = site.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = site.urlTemplate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Vertical divider
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // RadioButton for setting default
        RadioButton(
            selected = isDefault,
            onClick = onSetDefault,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).padding(end = 12.dp)
        )
    }
}

@Composable
private fun WebSearchSiteEditDialog(
    site: WebSearchSite,
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onSave: (WebSearchSite) -> Unit,
    onRemove: () -> Unit
) {
    var displayName by remember { mutableStateOf(site.displayName) }
    var urlTemplate by remember { mutableStateOf(site.urlTemplate) }
    val placeholder = WebSearchSettings.QUERY_PLACEHOLDER
    val isValid = displayName.isNotBlank() && urlTemplate.contains(placeholder)

    ScrimDialog(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Edit Search Engine",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = urlTemplate,
                onValueChange = { urlTemplate = it },
                label = { Text("URL template") },
                supportingText = { Text(text = "Include $placeholder") },
                isError = !urlTemplate.contains(placeholder),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            if (!urlTemplate.contains(placeholder)) {
                Text(
                    text = "Missing $placeholder",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canRemove) {
                    TextButton(
                        onClick = onRemove
                    ) {
                        Text(
                            text = "Remove",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(site.copy(displayName = displayName.trim(), urlTemplate = urlTemplate.trim()))
                        },
                        enabled = isValid
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}


@Composable
private fun WebSearchSiteAddDialog(
    placeholder: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, template: String, onError: (String) -> Unit) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var urlTemplate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isValid = displayName.isNotBlank() && urlTemplate.contains(placeholder)

    ScrimDialog(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Add Search Engine",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = urlTemplate,
                onValueChange = { urlTemplate = it },
                label = { Text("URL template") },
                supportingText = { Text(text = "Include $placeholder") },
                isError = urlTemplate.isNotBlank() && !urlTemplate.contains(placeholder),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            if (urlTemplate.isNotBlank() && !urlTemplate.contains(placeholder)) {
                Text(
                    text = "Missing $placeholder",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onAdd(displayName, urlTemplate) { error ->
                            errorMessage = error
                        }
                    },
                    enabled = isValid
                ) {
                    Text(text = "Add")
                }
            }
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
