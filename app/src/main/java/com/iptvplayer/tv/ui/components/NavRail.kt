package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvplayer.tv.ui.theme.NovaColors

enum class NavItem(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    GUIDE("Guide", Icons.Default.CalendarMonth),
    MOVIES("Movies", Icons.Default.Movie),
    SERIES("Series", Icons.Default.VideoLibrary),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun NavRail(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NovaColors.Surface,
                        NovaColors.Background
                    )
                )
            )
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Live",
                color = NovaColors.OnPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TV",
                color = NovaColors.OnPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main nav items (except Settings)
        NavItem.entries.filter { it != NavItem.SETTINGS }.forEach { item ->
            NavRailItem(
                item = item,
                isSelected = selectedItem == item,
                onClick = { onItemSelected(item) }
            )
        }

        // Push Settings to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Settings at bottom
        NavRailItem(
            item = NavItem.SETTINGS,
            isSelected = selectedItem == NavItem.SETTINGS,
            onClick = { onItemSelected(NavItem.SETTINGS) }
        )
    }
}

@Composable
private fun NavRailItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isSelected -> NovaColors.Primary.copy(alpha = 0.15f)
        isFocused -> NovaColors.SurfaceVariant
        else -> Color.Transparent
    }

    val iconColor = when {
        isSelected -> NovaColors.Primary
        isFocused -> NovaColors.TextPrimary
        else -> NovaColors.TextMuted
    }

    Column(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            color = iconColor,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        // Selected indicator
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(NovaColors.Primary, RoundedCornerShape(2.dp))
            )
        }
    }
}
