package com.mrndstvndv.search

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mrndstvndv.search.BuildConfig
import com.mrndstvndv.search.alias.AliasCreationCandidate
import com.mrndstvndv.search.alias.AliasEntry
import com.mrndstvndv.search.alias.AliasRepository
import com.mrndstvndv.search.alias.AppLaunchAliasTarget
import com.mrndstvndv.search.alias.WebSearchAliasTarget
import com.mrndstvndv.search.provider.apps.AppListProvider
import com.mrndstvndv.search.provider.calculator.CalculatorProvider
import com.mrndstvndv.search.provider.debug.DebugLongOperationProvider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.provider.settings.ProviderSettingsRepository
import com.mrndstvndv.search.provider.settings.WebSearchSettings
import com.mrndstvndv.search.provider.web.WebSearchProvider
import com.mrndstvndv.search.ui.components.ItemsList
import com.mrndstvndv.search.ui.components.SearchField
import com.mrndstvndv.search.ui.settings.AliasCreationDialog
import com.mrndstvndv.search.ui.theme.SearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val defaultAppIconSize by lazy { resources.getDimensionPixelSize(android.R.dimen.app_icon_size) }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val textState = remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val settingsRepository = remember(this@MainActivity) { ProviderSettingsRepository(this@MainActivity) }
            val aliasRepository = remember(this@MainActivity) { AliasRepository(this@MainActivity) }
            val aliasEntries by aliasRepository.aliases.collectAsState()
            val webSearchSettings by settingsRepository.webSearchSettings.collectAsState()

            val providers = remember(this@MainActivity) {
                buildList {
                    add(AppListProvider(this@MainActivity, defaultAppIconSize))
                    add(CalculatorProvider(this@MainActivity))
                    add(WebSearchProvider(this@MainActivity, settingsRepository))
                    if (BuildConfig.DEBUG) {
                        add(DebugLongOperationProvider(this@MainActivity))
                    }
                }
            }
            val providerResults = remember { mutableStateListOf<ProviderResult>() }
            var shouldShowResults by remember { mutableStateOf(false) }

            var aliasDialogCandidate by remember { mutableStateOf<AliasCreationCandidate?>(null) }
            var aliasDialogValue by remember { mutableStateOf("") }
            var aliasDialogError by remember { mutableStateOf<String?>(null) }
            var isPerformingAction by remember { mutableStateOf(false) }
            var pendingAction by remember { mutableStateOf<(suspend () -> Unit)?>(null) }

            fun startPendingAction(action: (suspend () -> Unit)?) {
                if (action == null || isPerformingAction) return
                isPerformingAction = true
                pendingAction = action
            }

            LaunchedEffect(textState.value, aliasEntries, webSearchSettings) {
                val match = aliasRepository.matchAlias(textState.value)
                val normalizedText = match?.remainingQuery ?: textState.value
                val query = Query(normalizedText, originalText = textState.value)
                val aggregated = withContext(Dispatchers.Default) {
                    val results = mutableListOf<ProviderResult>()
                    providers.forEach { provider ->
                        if (provider.canHandle(query)) {
                            results += provider.query(query)
                        }
                    }
                    results
                }
                val filtered = match?.entry?.target?.let { aliasTarget ->
                    aggregated.filterNot { it.aliasTarget == aliasTarget }
                } ?: aggregated
                val aliasResult = match?.let { buildAliasResult(it.entry, normalizedText, webSearchSettings) }
                providerResults.clear()
                aliasResult?.let { providerResults.add(it) }
                providerResults.addAll(filtered)
                shouldShowResults = normalizedText.isNotBlank() || match != null
            }

            SearchTheme {
                val hasVisibleResults = shouldShowResults && providerResults.isNotEmpty()
                val spacerWeight by animateFloatAsState(
                    targetValue = if (hasVisibleResults) 0.01f else 1f,
                    animationSpec = tween(durationMillis = 300)
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        }
                        .padding(top = 50.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.weight(spacerWeight))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            SearchField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                value = textState.value,
                                onValueChange = { textState.value = it },
                                singleLine = true,
                                placeholder = { Text("Search") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    val primaryAction = providerResults.firstOrNull()?.onSelect
                                    if (primaryAction != null) {
                                        startPendingAction(primaryAction)
                                    } else {
                                        val query = textState.value.trim()
                                        if (query.isNotEmpty()) {
                                            handleQuerySubmission(query)
                                        }
                                    }
                                })
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                                    startActivity(intent)
                                }) {
                                    Text(text = "Settings")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (!hasVisibleResults) {
                            Spacer(Modifier.weight(spacerWeight))
                        }

                        AnimatedVisibility(
                            visible = hasVisibleResults,
                            modifier = Modifier
                                .weight(if (hasVisibleResults) 1f else 0.01f)
                                .imePadding()
                                .padding(bottom = 8.dp),
                            enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                                    expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(durationMillis = 250)
                                    ),
                            exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                                    shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(durationMillis = 200)
                                    )
                        ) {
                            ItemsList(
                                modifier = Modifier.fillMaxSize(),
                                results = providerResults,
                                onItemClick = { result -> startPendingAction(result.onSelect) },
                                onItemLongPress = onItemLongPress@{ result ->
                                    val target = result.aliasTarget ?: return@onItemLongPress
                                    val suggestion = sanitizeAliasSuggestion(result.title)
                                    aliasDialogValue = suggestion
                                    aliasDialogError = null
                                    aliasDialogCandidate = AliasCreationCandidate(
                                        target = target,
                                        suggestion = suggestion,
                                        description = result.subtitle ?: result.title
                                    )
                                }
                            )
                        }
                    }

                    if (isPerformingAction) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                                            .copy(alpha = 0.95f),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .padding(horizontal = 28.dp, vertical = 24.dp)
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier
                                        .size(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(pendingAction) {
                val action = pendingAction ?: return@LaunchedEffect
                try {
                    withContext(Dispatchers.Default) {
                        action()
                    }
                } finally {
                    isPerformingAction = false
                    pendingAction = null
                }
            }

            aliasDialogCandidate?.let { candidate ->
                AliasCreationDialog(
                    candidate = candidate,
                    alias = aliasDialogValue,
                    errorMessage = aliasDialogError,
                    onAliasChange = { aliasDialogValue = it },
                    onDismiss = {
                        aliasDialogCandidate = null
                        aliasDialogError = null
                    },
                    onSave = {
                        when (aliasRepository.addAlias(aliasDialogValue, candidate.target)) {
                            AliasRepository.SaveResult.SUCCESS -> {
                                aliasDialogCandidate = null
                                aliasDialogError = null
                            }
                            AliasRepository.SaveResult.DUPLICATE -> {
                                aliasDialogError = "Alias already exists"
                            }
                            AliasRepository.SaveResult.INVALID_ALIAS -> {
                                aliasDialogError = "Alias cannot be empty"
                            }
                        }
                    }
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        applyWindowBlur()
    }

    private fun handleQuerySubmission(query: String) {
        if (Patterns.WEB_URL.matcher(query).matches()) {
            val normalizedUrl = if (query.startsWith("http://", ignoreCase = true) || query.startsWith("https://", ignoreCase = true)) {
                query
            } else {
                "https://$query"
            }
            val intent = Intent(Intent.ACTION_VIEW, normalizedUrl.toUri())
            startActivity(intent)
        } else {
            val url = "https://www.bing.com/search?q=${Uri.encode(query)}&form=QBLH"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
        finish()
    }

    private fun buildAliasResult(entry: AliasEntry, query: String, webSearchSettings: WebSearchSettings): ProviderResult? {
        return when (val target = entry.target) {
            is WebSearchAliasTarget -> {
                val site = webSearchSettings.siteForId(target.siteId) ?: webSearchSettings.sites.firstOrNull()
                val resolvedSite = site ?: return null
                val searchUrl = resolvedSite.buildUrl(query)
                val action: suspend () -> Unit = {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Intent.ACTION_VIEW, searchUrl.toUri())
                        startActivity(intent)
                        finish()
                    }
                }
                ProviderResult(
                    id = "alias:web:${entry.alias}:${query.hashCode()}",
                    title = if (query.isBlank()) resolvedSite.displayName else query,
                    subtitle = "Alias \"${entry.alias}\" → ${resolvedSite.displayName}",
                    providerId = target.providerId,
                    onSelect = action,
                    aliasTarget = target
                )
            }
            is AppLaunchAliasTarget -> {
                val action: suspend () -> Unit = {
                    withContext(Dispatchers.Main) {
                        val launchIntent = packageManager.getLaunchIntentForPackage(target.packageName)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                            finish()
                        }
                    }
                }
                ProviderResult(
                    id = "alias:app:${entry.alias}",
                    title = target.label,
                    subtitle = "Alias \"${entry.alias}\"",
                    providerId = target.providerId,
                    onSelect = action,
                    aliasTarget = target
                )
            }
            else -> null
        }
    }

    private fun sanitizeAliasSuggestion(text: String): String {
        val normalized = text.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return normalized.ifBlank { "alias" }
    }

    private fun applyWindowBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.decorView?.let { window.setBackgroundBlurRadius(40) }
        }
    }
}
