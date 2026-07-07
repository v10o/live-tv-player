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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.PlaylistType
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 160.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1f)
            .shadow(
                elevation = if (isFocused) 20.dp else 8.dp,
                shape = shape,
                ambientColor = if (isFocused) NovaColors.Primary.copy(alpha = 0.3f) else Color.Black,
                spotColor = if (isFocused) NovaColors.Primary.copy(alpha = 0.3f) else Color.Black
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = if (isFocused) {
                        listOf(NovaColors.PrimaryContainer, NovaColors.Surface)
                    } else {
                        listOf(NovaColors.Surface, NovaColors.SurfaceVariant)
                    }
                )
            )
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
        // Decorative gradient corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NovaColors.Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Type badge
                Box(
                    modifier = Modifier
                        .background(
                            when (playlist.type) {
                                PlaylistType.XTREAM -> NovaColors.Primary.copy(alpha = 0.2f)
                                else -> NovaColors.SurfaceVariant
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (playlist.type) {
                            PlaylistType.XTREAM -> "XTREAM"
                            PlaylistType.M3U_URL -> "M3U"
                            PlaylistType.M3U_FILE -> "FILE"
                        },
                        color = when (playlist.type) {
                            PlaylistType.XTREAM -> NovaColors.Primary
                            else -> NovaColors.TextMuted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = playlist.name,
                    color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Channel count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NovaColors.Primary, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${playlist.channelsCount} channels",
                    color = NovaColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}
