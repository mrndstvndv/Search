package com.mrndstvndv.search

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.mrndstvndv.search.ui.components.SearchField
import com.mrndstvndv.search.ui.theme.SearchTheme
import androidx.core.net.toUri
import android.util.Patterns
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.pow

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

            val calculatorResult = remember(textState.value) {
                getCalculatorResult(textState.value)
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
                                        if (Patterns.WEB_URL.matcher(query).matches()) {
                                            val normalizedUrl = if (query.startsWith("http://", ignoreCase = true) ||
                                                query.startsWith("https://", ignoreCase = true)
                                            ) {
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
                                }
                            })
                        )

                        Spacer(modifier = Modifier.height(6.dp)) // Adding a gap

                        // Items
                        Surface(shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().clipToBounds()) {
                            if (calculatorResult != null) {
                                Text(
                                    text = "= $calculatorResult",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(filteredPackages.size) { index ->
                                val packageName = filteredPackages[index]
                                val app = packageManager.getApplicationInfo(packageName, 0)
                                val shape = if (filteredPackages.size == 1) {
                                    RoundedCornerShape(20.dp)
                                } else if (index == 0) {
                                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, 5.dp, 5.dp)
                                } else if (index == filteredPackages.lastIndex) {
                                    RoundedCornerShape(5.dp, 5.dp,  bottomStart = 20.dp, bottomEnd = 20.dp)
                                } else {
                                    RoundedCornerShape(5.dp)
                                }
                                Surface(shape = shape, tonalElevation = 1.dp) {
                                    Box(Modifier.fillMaxWidth().height(60.dp).clickable {
                                        val intent = pm.getLaunchIntentForPackage(packageName)
                                        if (intent != null) {
                                            startActivity(intent)
                                        }
                                        finish()
                                    }, contentAlignment = Alignment.CenterStart) {
                                        Text(pm.getApplicationLabel(app).toString(), modifier = Modifier.padding(start = 16.dp))
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
        applyWindowBlur()
    }

    private fun applyWindowBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.decorView?.let { window.setBackgroundBlurRadius(40) }
        }
    }
}

private val expressionRegex = Regex("^[0-9+\\-*/().\\s]+\$")

private fun getCalculatorResult(input: String): String? {
    val cleaned = input.trim()
    if (cleaned.isEmpty()) return null
    if (!expressionRegex.matches(cleaned)) return null
    val value = evaluateExpression(cleaned) ?: return null
    return formatCalculatorResult(value)
}

private fun formatCalculatorResult(value: Double): String =
    "%.8f".format(value).trimEnd('0').trimEnd('.')

private fun evaluateExpression(expression: String): Double? {
    return try {
        object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                pos++
                ch = if (pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw IllegalArgumentException("Unexpected: ${expression[pos]}")
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    x = when {
                        eat('+'.code) -> x + parseTerm()
                        eat('-'.code) -> x - parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    x = when {
                        eat('*'.code) -> x * parseFactor()
                        eat('/'.code) -> x / parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                val startPos = pos
                val x: Double = when {
                    eat('('.code) -> {
                        val inner = parseExpression()
                        if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
                        inner
                    }

                    ch in '0'.code..'9'.code || ch == '.'.code -> {
                        while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                        expression.substring(startPos, pos).toDouble()
                    }

                    else -> throw IllegalArgumentException("Unexpected: ${if (ch == -1) "end" else expression[pos]}")
                }

                return if (eat('^'.code)) {
                    x.pow(parseFactor())
                } else {
                    x
                }
            }
        }.parse()
    } catch (_: Exception) {
        null
    }
}
