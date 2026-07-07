package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvplayer.tv.ui.theme.NovaColors
import java.text.SimpleDateFormat
import java.util.*

enum class TopNavItem(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    MOVIES("Movies", Icons.Default.Movie),
    SHOWS("Shows", Icons.Default.VideoLibrary),
    WATCHLIST("Watchlist", Icons.Default.BookmarkBorder),
    SEARCH("Search", Icons.Default.Search)
}

@Composable
fun TopNavBar(
    selectedItem: TopNavItem,
    onItemSelected: (TopNavItem) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            kotlinx.coroutines.delay(60_000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(NovaColors.Surface.copy(alpha = 0.95f))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(NovaColors.Primary, CircleShape)
            )
            Text(
                text = "live",
                color = NovaColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = ".tv",
                color = NovaColors.TextMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(48.dp))

        // Nav items
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem.entries.forEach { item ->
                TopNavBarItem(
                    item = item,
                    isSelected = selectedItem == item,
                    onClick = { onItemSelected(item) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right side - Refresh, Time and Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Refresh button (shown on content screens)
            if (onRefreshClick != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) NovaColors.Primary else NovaColors.SurfaceVariant)
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable(enabled = !isRefreshing) { onRefreshClick() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isFocused) NovaColors.OnPrimary else NovaColors.TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (isRefreshing) rotation else 0f
                            }
                    )
                }
            }

            Text(
                text = currentTime,
                color = NovaColors.TextSecondary,
                fontSize = 14.sp
            )

            // Settings icon
            var settingsFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (settingsFocused) NovaColors.Primary else NovaColors.SurfaceVariant)
                    .onFocusChanged { settingsFocused = it.isFocused }
                    .clickable { onSettingsClick() }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (settingsFocused) NovaColors.OnPrimary else NovaColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TopNavBarItem(
    item: TopNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isSelected -> NovaColors.Primary
        isFocused -> NovaColors.SurfaceVariant
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> NovaColors.OnPrimary
        isFocused -> NovaColors.TextPrimary
        else -> NovaColors.TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = item.label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
