package com.mrndstvndv.search.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrndstvndv.search.provider.model.ProviderResult
import com.mrndstvndv.search.ui.theme.motionAwareTween

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemsList(
    modifier: Modifier = Modifier,
    results: List<ProviderResult>,
    onItemClick: (ProviderResult) -> Unit,
    onItemLongPress: ((ProviderResult) -> Unit)? = null,
    translucentItems: Boolean = false,
) {
    if (results.isEmpty()) return

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(
            items = results,
            key = { _, item -> item.id }
        ) { index, item ->
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

            val cornerAnimationSpec = motionAwareTween<Dp>(durationMillis = 250)
            val animatedTopStart by animateDpAsState(targetTopStart, animationSpec = cornerAnimationSpec, label = "shapeTopStart")
            val animatedTopEnd by animateDpAsState(targetTopEnd, animationSpec = cornerAnimationSpec, label = "shapeTopEnd")
            val animatedBottomStart by animateDpAsState(targetBottomStart, animationSpec = cornerAnimationSpec, label = "shapeBottomStart")
            val animatedBottomEnd by animateDpAsState(targetBottomEnd, animationSpec = cornerAnimationSpec, label = "shapeBottomEnd")

            val shape = RoundedCornerShape(
                topStart = animatedTopStart,
                topEnd = animatedTopEnd,
                bottomEnd = animatedBottomEnd,
                bottomStart = animatedBottomStart,
            )

            val containerColor = if (translucentItems) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
            val isDarkTheme = isSystemInDarkTheme()
            val primaryTextColor = if (translucentItems) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val subtitleColor = if (translucentItems) {
                val alpha = if (isDarkTheme) 0.85f else 0.75f
                primaryTextColor.copy(alpha = alpha)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val iconBitmap by produceState<Bitmap?>(
                initialValue = item.icon,
                key1 = item.id,
                key2 = item.iconLoader
            ) {
                if (value != null) return@produceState
                val loader = item.iconLoader ?: return@produceState
                value = loader()
            }

            Surface(
                shape = shape,
                tonalElevation = if (translucentItems) 0.dp else 1.dp,
                color = containerColor
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val rippleIndication = LocalIndication.current
                val clickModifier = if (onItemLongPress != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = rippleIndication,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongPress(item) }
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rippleIndication
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
                    when {
                        iconBitmap != null -> {
                            val painter = remember(iconBitmap) { BitmapPainter(iconBitmap!!.asImageBitmap()) }
                            androidx.compose.foundation.Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        item.vectorIcon != null -> {
                            Icon(
                                imageVector = item.vectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = primaryTextColor
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        item.defaultVectorIcon != null -> {
                            Icon(
                                imageVector = item.defaultVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = primaryTextColor
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.subtitle?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
