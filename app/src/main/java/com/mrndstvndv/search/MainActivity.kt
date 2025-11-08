package com.mrndstvndv.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.ui.theme.SearchTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = applicationContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val packageNames = resolveInfos.map { it.activityInfo.packageName }.distinct()

        enableEdgeToEdge()
        setContent {

            val textState = remember { mutableStateOf("") }

            val focusRequester = remember { FocusRequester() }

            val filteredPackages = remember(textState.value) {
                packageNames.filter { packageName ->
                    try {
                        val app = pm.getApplicationInfo(packageName, 0)
                        pm.getApplicationLabel(app).toString().contains(textState.value, ignoreCase = true)
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            SearchTheme {
                Box(Modifier.fillMaxSize().padding(top=50.dp)) {
                    Column {
                        Surface {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                value = textState.value,
                                onValueChange = { textState.value = it },
                                singleLine = true,
                                label = { Text("Search") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                     if (filteredPackages.isNotEmpty()) {
                                     val firstPackage = filteredPackages.first()
                                         val launchIntent = pm.getLaunchIntentForPackage(firstPackage)
                                         if (launchIntent != null) {
                                             startActivity(launchIntent)
                                         }
                                         finish()
                                     } else {
                                 val query = textState.value.trim()
                                if (query.isNotEmpty()) {
                                val url = "https://www.bing.com/search?q=${Uri.encode(query)}&form=QBLH"
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                startActivity(intent)
                                finish()
                                }
                                }
                                })
                            )
                        }

                        Surface() {
                            LazyColumn() {
                                items(filteredPackages) { packageName ->
                                    val app = packageManager.getApplicationInfo(packageName, 0)
                                    Box(Modifier.fillMaxWidth().height(50.dp).clickable {
                                    val intent = pm.getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                    startActivity(intent)
                                    }
                                    finish()
                                    }, contentAlignment = Alignment.Center) {
                                        Text(pm.getApplicationLabel(app).toString())
                                    }
                                }
                            }
                        }
                    }

                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}