package com.mrndstvndv.search.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.provider.ProviderRankingRepository

@Composable
fun ProviderRankingSection(rankingRepository: ProviderRankingRepository) {
    val providerOrder by rankingRepository.providerOrder.collectAsState()

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
            text = "Drag or reorder providers to control result display order",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
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
