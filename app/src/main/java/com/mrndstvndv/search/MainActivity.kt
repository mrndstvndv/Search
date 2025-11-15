package com.mrndstvndv.search

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mrndstvndv.search.provider.apps.AppListProvider
import com.mrndstvndv.search.provider.calculator.CalculatorProvider
import com.mrndstvndv.search.provider.web.WebSearchProvider
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.provider.model.Query
import com.mrndstvndv.search.ui.components.ItemsList
import com.mrndstvndv.search.ui.components.SearchField
import com.mrndstvndv.search.ui.theme.SearchTheme
import com.mrndstvndv.search.util.CalculatorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val defaultAppIconSize by lazy { resources.getDimensionPixelSize(android.R.dimen.app_icon_size) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val textState = remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            val providers = remember(this@MainActivity) {
                listOf(
                    AppListProvider(this@MainActivity, defaultAppIconSize),
                    CalculatorProvider(this@MainActivity),
                    WebSearchProvider(this@MainActivity)
                )
            }
            val providerResults = remember { mutableStateListOf<ProviderResult>() }

            LaunchedEffect(textState.value) {
                val query = Query(textState.value)
                val aggregated = withContext(Dispatchers.Default) {
                    val results = mutableListOf<ProviderResult>()
                    providers.forEach { provider ->
                        if (provider.canHandle(query)) {
                            results += provider.query(query)
                        }
                    }
                    results
                }
                providerResults.clear()
                providerResults.addAll(aggregated)
            }

            val calculatorResult = remember(textState.value) {
                CalculatorEngine.compute(textState.value)
            }

            SearchTheme {
                Box(
                    Modifier.fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        finish()
                    }.padding(top = 50.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SearchField(
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            singleLine = true,
                            placeholder = { Text("Search") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                providerResults.firstOrNull()?.onSelect?.invoke() ?: run {
                                    val query = textState.value.trim()
                                    if (query.isNotEmpty()) {
                                        handleQuerySubmission(query)
                                    }
                                }
                            })
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ItemsList(
                            modifier = Modifier.weight(1f),
                            results = providerResults,
                            onItemClick = { result -> result.onSelect?.invoke() }
                        )
                    }

                }
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

    private fun applyWindowBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.decorView?.let { window.setBackgroundBlurRadius(40) }
        }
    }
}
