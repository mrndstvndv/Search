package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.provider.ProviderRankingRepository

@Composable
fun ProviderRankingSection(rankingRepository: ProviderRankingRepository) {
    val providerOrder by rankingRepository.providerOrder.collectAsState()
    val useFrequencyRanking by rankingRepository.useFrequencyRanking.collectAsState()
    val resultFrequency by rankingRepository.resultFrequency.collectAsState()
    var showFrequencyModal by remember { mutableStateOf(false) }

    if (showFrequencyModal) {
        FrequencyRankingModal(
            frequency = resultFrequency,
            onDismiss = { showFrequencyModal = false },
            onReset = { rankingRepository.resetResultFrequency() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Provider Order",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Control how providers are ranked in search results",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Toggle for frequency-based ranking
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use frequency-based ranking",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (useFrequencyRanking) "Most used items appear first" else "Manual provider order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useFrequencyRanking,
                    onCheckedChange = { rankingRepository.setUseFrequencyRanking(it) }
                )
            }
        }

        // View frequency data button (only show if frequency ranking is enabled)
        if (useFrequencyRanking && resultFrequency.isNotEmpty()) {
            TextButton(
                onClick = { showFrequencyModal = true },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 12.dp)
            ) {
                Text(text = "View frequency data (${resultFrequency.size})")
            }
        }

        Text(
            text = "Manual provider order",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            providerOrder.forEachIndexed { index, providerId ->
                ProviderRankingItem(
                    providerId = providerId,
                    isFirst = index == 0,
                    isLast = index == providerOrder.size - 1,
                    onMoveUp = { rankingRepository.moveUp(providerId) },
                    onMoveDown = { rankingRepository.moveDown(providerId) }
                )
                if (index < providerOrder.size - 1) {
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun FrequencyRankingModal(
    frequency: Map<String, Int>,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequency Ranking Data",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close"
                    )
                }
            }

            HorizontalDivider()

            // Sorted frequency list
            if (frequency.isEmpty()) {
                Text(
                    text = "No usage data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        frequency.entries
                            .sortedByDescending { it.value }
                            .toList()
                    ) { (resultId, count) ->
                        FrequencyItem(resultId = resultId, count = count)
                    }
                }
            }

            HorizontalDivider()

            // Reset button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReset) {
                    Text(text = "Reset data")
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Done")
                }
            }
        }
    }
}

@Composable
private fun FrequencyItem(resultId: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = resultId,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count uses",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProviderRankingItem(
    providerId: String,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val displayName = getProviderDisplayName(providerId)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.width(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Move up",
                        modifier = Modifier.width(16.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.width(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Move down",
                        modifier = Modifier.width(16.dp)
                    )
                }
            }
        }
    }
}

private fun getProviderDisplayName(providerId: String): String {
    return when (providerId) {
        "app-list" -> "Applications"
        "calculator" -> "Calculator"
        "text-utilities" -> "Text Utilities"
        "file-search" -> "Files & Folders"
        "web-search" -> "Web Search"
        "debug-long-operation" -> "Debug • Long Operation"
        else -> providerId
    }
}
