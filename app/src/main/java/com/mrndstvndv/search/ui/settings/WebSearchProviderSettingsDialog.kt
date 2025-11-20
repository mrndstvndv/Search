package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrndstvndv.search.provider.settings.WebSearchSettings
import com.mrndstvndv.search.provider.settings.WebSearchSite
import com.mrndstvndv.search.ui.settings.SettingsToggleRow

@Composable
fun WebSearchProviderSettingsDialog(
    initialSettings: WebSearchSettings,
    onDismiss: () -> Unit,
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
    val scrollState = rememberScrollState()

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Web Search Sites",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick your preferred search engine and add custom websites. Use $placeholder as the query placeholder.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                sites.forEachIndexed { index, site ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = defaultSiteId == site.id,
                                    onClick = { defaultSiteId = site.id }
                                )
                                Text(text = "Default", fontSize = 12.sp)
                            }
                            if (sites.size > 1) {
                                TextButton(onClick = { removeSite(index) }) {
                                    Text(text = "Remove")
                                }
                            }
                        }
                        SettingsToggleRow(
                            title = "Enabled",
                            subtitle = "Allow this site to appear in search results.",
                            checked = site.isEnabled,
                            onCheckedChange = { newValue ->
                                updateSite(index) { it.copy(isEnabled = newValue) }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = site.displayName,
                            onValueChange = { newValue ->
                                updateSite(index) { it.copy(displayName = newValue) }
                            },
                            label = { Text("Display name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = site.urlTemplate,
                            onValueChange = { newValue ->
                                updateSite(index) { it.copy(urlTemplate = newValue) }
                            },
                            label = { Text("URL template") },
                            supportingText = {
                                Text(text = "Example: https://www.example.com/search?q={query}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!site.urlTemplate.contains(placeholder)) {
                            Text(
                                text = "Template is missing $placeholder",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Preview: ${site.buildUrl("compose")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Add a custom site", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = newSiteName,
                    onValueChange = { newSiteName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = newSiteTemplate,
                    onValueChange = { newSiteTemplate = it },
                    label = { Text("URL template") },
                    supportingText = { Text(text = "Include $placeholder in the template") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = ::addCustomSite) {
                        Text(text = "Add site")
                    }
                }

                if (!allTemplatesValid) {
                    Text(
                        text = "Every template needs $placeholder",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val resolvedDefault = sites.firstOrNull { it.id == defaultSiteId }?.id
                                ?: sites.firstOrNull()?.id
                                ?: ""
                            if (resolvedDefault.isNotBlank()) {
                                onSave(WebSearchSettings(resolvedDefault, sites))
                            }
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}
