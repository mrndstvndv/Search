package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrndstvndv.search.provider.settings.WebSearchSettings
import com.mrndstvndv.search.provider.settings.WebSearchSite
import com.mrndstvndv.search.ui.settings.SettingsToggleRow

@Composable
fun WebSearchSettingsScreen(
    initialSettings: WebSearchSettings,
    onBack: () -> Unit,
    onSave: (WebSearchSettings) -> Unit
) {
    var sites by remember { mutableStateOf(initialSettings.sites) }
    var defaultSiteId by remember { mutableStateOf(initialSettings.defaultSiteId) }
    var newSiteName by remember { mutableStateOf("") }
    var newSiteTemplate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val placeholder = WebSearchSettings.QUERY_PLACEHOLDER

    LaunchedEffect(initialSettings) {
        sites = initialSettings.sites
        defaultSiteId = initialSettings.defaultSiteId
    }

    val allTemplatesValid = sites.all { it.urlTemplate.contains(placeholder) }

    fun updateSite(index: Int, updater: (WebSearchSite) -> WebSearchSite) {
        val mutable = sites.toMutableList()
        mutable[index] = updater(mutable[index])
        sites = mutable
    }

    fun removeSite(index: Int) {
        if (sites.size <= 1) return
        val removed = sites[index]
        val mutable = sites.toMutableList().also { it.removeAt(index) }
        sites = mutable
        if (defaultSiteId == removed.id) {
            defaultSiteId = mutable.firstOrNull()?.id.orEmpty()
        }
    }

    fun addCustomSite() {
        val name = newSiteName.trim()
        val template = newSiteTemplate.trim()
        if (name.isBlank() || template.isBlank()) {
            errorMessage = "Name and template are required"
            return
        }
        if (!template.contains(placeholder)) {
            errorMessage = "Template must include $placeholder"
            return
        }
        val candidateId = name.lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { name.lowercase() }
        if (sites.any { it.id == candidateId }) {
            errorMessage = "A site with that name already exists"
            return
        }
        val newSite = WebSearchSite(id = candidateId, displayName = name, urlTemplate = template, isEnabled = true)
        sites = sites + newSite
        defaultSiteId = candidateId
        newSiteName = ""
        newSiteTemplate = ""
        errorMessage = null
    }

    val isSaveEnabled = sites.isNotEmpty() && allTemplatesValid && defaultSiteId.isNotBlank()

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
                                canRemove = sites.size > 1,
                                onSetDefault = { defaultSiteId = site.id },
                                onUpdate = { updater -> updateSite(index, updater) },
                                onRemove = { removeSite(index) }
                            )
                            if (index < sites.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    title = "Add Custom Site",
                    subtitle = "Add a new search engine to the list."
                ) {
                    SettingsCardGroup {
                        Column(modifier = Modifier.padding(20.dp)) {
                            TextField(
                                value = newSiteName,
                                onValueChange = { newSiteName = it },
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
                                value = newSiteTemplate,
                                onValueChange = { newSiteTemplate = it },
                                label = { Text("URL template") },
                                supportingText = { Text(text = "Include $placeholder") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = ::addCustomSite) {
                                    Text(text = "Add site")
                                }
                            }
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
            
            // Spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Save Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val resolvedDefault = sites.firstOrNull { it.id == defaultSiteId }?.id
                            ?: sites.firstOrNull()?.id
                            ?: ""
                        if (resolvedDefault.isNotBlank()) {
                            onSave(WebSearchSettings(resolvedDefault, sites))
                        }
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save Changes")
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
    canRemove: Boolean,
    onSetDefault: () -> Unit,
    onUpdate: ((WebSearchSite) -> WebSearchSite) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSetDefault() }
            ) {
                RadioButton(
                    selected = isDefault,
                    onClick = onSetDefault
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDefault) "Default" else "Set as default",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove site",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        SettingsToggleRow(
            title = "Enabled",
            subtitle = "Allow this site to appear in search results.",
            checked = site.isEnabled,
            onCheckedChange = { newValue ->
                onUpdate { it.copy(isEnabled = newValue) }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = site.displayName,
            onValueChange = { newValue ->
                onUpdate { it.copy(displayName = newValue) }
            },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = site.urlTemplate,
            onValueChange = { newValue ->
                onUpdate { it.copy(urlTemplate = newValue) }
            },
            label = { Text("URL template") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        
        if (!site.urlTemplate.contains(WebSearchSettings.QUERY_PLACEHOLDER)) {
            Text(
                text = "Missing ${WebSearchSettings.QUERY_PLACEHOLDER}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
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
