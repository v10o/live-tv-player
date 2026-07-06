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
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(width = 200.dp, height = 140.dp)
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
                    Modifier.border(1.dp, NovaColors.Border, shape)
                }
            )
            .clickable { onClick() }
            .focusable()
    ) {
        // Channel logo
        if (!channel.logo.isNullOrEmpty()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .background(NovaColors.BackgroundDark),
                contentScale = ContentScale.Fit
            )
        } else {
            // Placeholder with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NovaColors.PrimaryContainer,
                                NovaColors.Surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = NovaColors.Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NovaColors.Surface.copy(alpha = 0.9f),
                            NovaColors.Surface
                        )
                    )
                )
        )

        // Channel info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = channel.name,
                color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!channel.group.isNullOrEmpty()) {
                Text(
                    text = channel.group,
                    color = NovaColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Favorite indicator
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(
                        NovaColors.Primary.copy(alpha = 0.9f),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    color = NovaColors.OnPrimary,
                    fontSize = 12.sp
                )
            }
        }

        // Live indicator for non-radio channels
        if (!channel.isRadio && channel.archiveDays == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        NovaColors.Secondary,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
