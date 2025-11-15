package com.mrndstvndv.search.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mrndstvndv.search.provider.model.ProviderResult

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemsList(
    results: List<ProviderResult>,
    onItemClick: (ProviderResult) -> Unit,
    onItemLongPress: ((ProviderResult) -> Unit)? = null
) {
    if (results.isEmpty()) return

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(results) { index, item ->
            val singleItem = results.size == 1
            val targetTopStart = when {
                singleItem -> 20.dp
                index == 0 -> 20.dp
                else -> 5.dp
            }
            val targetTopEnd = when {
                singleItem -> 20.dp
                index == 0 -> 20.dp
                else -> 5.dp
            }
            val targetBottomStart = when {
                singleItem -> 20.dp
                index == results.lastIndex -> 20.dp
                else -> 5.dp
            }
            val targetBottomEnd = when {
                singleItem -> 20.dp
                index == results.lastIndex -> 20.dp
                else -> 5.dp
            }

            val animatedTopStart by animateDpAsState(targetTopStart, animationSpec = tween(durationMillis = 250))
            val animatedTopEnd by animateDpAsState(targetTopEnd, animationSpec = tween(durationMillis = 250))
            val animatedBottomStart by animateDpAsState(targetBottomStart, animationSpec = tween(durationMillis = 250))
            val animatedBottomEnd by animateDpAsState(targetBottomEnd, animationSpec = tween(durationMillis = 250))

            val shape = RoundedCornerShape(
                topStart = animatedTopStart,
                topEnd = animatedTopEnd,
                bottomEnd = animatedBottomEnd,
                bottomStart = animatedBottomStart,
            )

            Surface(shape = shape, tonalElevation = 1.dp) {
                val interactionSource = remember { MutableInteractionSource() }
                val clickModifier = if (onItemLongPress != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongPress(item) }
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onItemClick(item) }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .then(clickModifier)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.icon?.let { bitmap ->
                        val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(text = item.title)
                        item.subtitle?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
