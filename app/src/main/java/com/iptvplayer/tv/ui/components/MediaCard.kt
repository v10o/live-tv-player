package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.ui.theme.NovaColors

/**
 * Unified card for VOD movies and Series.
 * Shows poster, title, and rating badge.
 * Focus scales 1.08x with border highlight.
 */
@Composable
fun MediaCard(
    item: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, posterUrl, rating) = remember(item) {
        when (item) {
            is VodItem -> Triple(item.name, item.icon, item.rating5based)
            is SeriesItem -> Triple(item.name, item.cover, item.rating5based)
            is WatchlistItem -> Triple(item.name, item.cover, null)
            else -> Triple("Unknown", null, null)
        }
    }

    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(width = 150.dp, height = 220.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.08f else 1f)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = shape,
                ambientColor = if (isFocused) NovaColors.Primary.copy(alpha = 0.4f) else Color.Black,
                spotColor = if (isFocused) NovaColors.Primary.copy(alpha = 0.4f) else Color.Black
            )
            .clip(shape)
            .background(NovaColors.Surface)
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, NovaColors.Primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .focusable()
    ) {
        // Poster image
        AsyncImage(
            model = posterUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay at bottom for text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Rating badge (top right)
        if (rating != null && rating > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = when {
                            rating >= 4.0 -> NovaColors.Primary.copy(alpha = 0.9f)
                            rating >= 3.0 -> Color(0xFFFFB800).copy(alpha = 0.9f)
                            else -> NovaColors.TextMuted.copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "★ ${String.format("%.1f", rating)}",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Title at bottom
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}
