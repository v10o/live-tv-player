package com.iptvplayer.tv.ui.screens.channels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun ChannelsScreen(
    playlistId: String,
    onChannelClick: (String) -> Unit,
    onBackPress: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Derive selected channel from ViewModel state
    val selectedChannel = remember(uiState.selectedChannelId, uiState.channels) {
        uiState.channels.find { it.id == uiState.selectedChannelId }
            ?: uiState.channels.firstOrNull()
    }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    // Set initial selected channel if none selected
    LaunchedEffect(uiState.channels) {
        if (uiState.selectedChannelId == null && uiState.channels.isNotEmpty()) {
            viewModel.setSelectedChannel(uiState.channels.first().id)
        }
    }

    BackHandler { onBackPress() }

    if (uiState.isLoading) {
        LoadingState()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Compact header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NovaColors.Surface)
                    .clickable { onBackPress() }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = NovaColors.TextPrimary, fontSize = 16.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.playlistName,
                    color = NovaColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.channels.size} channels",
                    color = NovaColors.TextMuted,
                    fontSize = 13.sp
                )
            }

            // Group filter chips (horizontal scroll if many)
            if (uiState.groups.isNotEmpty()) {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(2f)
                ) {
                    item {
                        FilterChip(
                            label = "All",
                            isSelected = uiState.selectedGroup == null,
                            onClick = { viewModel.selectGroup(null) }
                        )
                    }
                    items(uiState.groups.take(10)) { group ->
                        FilterChip(
                            label = group,
                            isSelected = uiState.selectedGroup == group,
                            onClick = { viewModel.selectGroup(group) }
                        )
                    }
                }
            }
        }

        // Hero Section - Selected Channel Preview
        selectedChannel?.let { channel ->
            HeroPreview(
                channel = channel,
                isFavorite = uiState.favoriteIds.contains(channel.id),
                onPlay = { onChannelClick(channel.id) },
                onFavorite = { viewModel.toggleFavorite(channel.id) }
            )
        }

        // Channel Grid/Rows
        if (uiState.groups.isEmpty()) {
            // No categories - show flat list
            ChannelRow(
                title = "All Channels",
                channels = uiState.channels,
                favoriteIds = uiState.favoriteIds,
                onChannelFocus = { viewModel.setSelectedChannel(it.id) },
                onChannelClick = onChannelClick
            )
        } else {
            // Group by categories
            ChannelsByCategory(
                channels = uiState.channels,
                groups = uiState.groups,
                favoriteIds = uiState.favoriteIds,
                selectedGroup = uiState.selectedGroup,
                onChannelFocus = { viewModel.setSelectedChannel(it.id) },
                onChannelClick = onChannelClick
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> NovaColors.Primary
                    isFocused -> NovaColors.SurfaceVariant
                    else -> NovaColors.Surface
                }
            )
            .then(
                if (isFocused && !isSelected) {
                    Modifier.border(1.dp, NovaColors.Primary, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = when {
                isSelected -> Color.Black
                isFocused -> NovaColors.TextPrimary
                else -> NovaColors.TextSecondary
            },
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroPreview(
    channel: Channel,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(24.dp)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NovaColors.PrimaryContainer,
                            NovaColors.Surface,
                            NovaColors.SurfaceVariant
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Channel Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NovaColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo != null) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        color = NovaColors.Primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Channel Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Live badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(NovaColors.Secondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        channel.group?.let { group ->
                            Text(
                                text = group,
                                color = NovaColors.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = channel.name,
                        color = NovaColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        label = "Watch Now",
                        icon = "▶",
                        isPrimary = true,
                        onClick = onPlay
                    )
                    ActionButton(
                        label = if (isFavorite) "Favorited" else "Favorite",
                        icon = if (isFavorite) "★" else "☆",
                        isPrimary = false,
                        onClick = onFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale = if (isFocused) 1.05f else 1f

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPrimary) {
                    Brush.linearGradient(
                        colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                    )
                } else {
                    Brush.linearGradient(
                        colors = if (isFocused) {
                            listOf(NovaColors.SurfaceVariant, NovaColors.Surface)
                        } else {
                            listOf(NovaColors.Surface, NovaColors.Surface)
                        }
                    )
                }
            )
            .then(
                if (isFocused && !isPrimary) {
                    Modifier.border(2.dp, NovaColors.Primary, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = label,
                color = if (isPrimary) NovaColors.OnPrimary else NovaColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChannelsByCategory(
    channels: List<Channel>,
    groups: List<String>,
    favoriteIds: Set<String>,
    selectedGroup: String?,
    onChannelFocus: (Channel) -> Unit,
    onChannelClick: (String) -> Unit
) {
    TvLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (selectedGroup != null) {
            // Show only selected category
            item {
                ChannelRow(
                    title = selectedGroup,
                    channels = channels,
                    favoriteIds = favoriteIds,
                    onChannelFocus = onChannelFocus,
                    onChannelClick = onChannelClick
                )
            }
        } else {
            // Show all categories as rows
            val channelsByGroup = channels.groupBy { it.group ?: "Uncategorized" }

            items(groups.take(10)) { group ->  // Limit to first 10 categories for performance
                val groupChannels = channelsByGroup[group] ?: emptyList()
                if (groupChannels.isNotEmpty()) {
                    ChannelRow(
                        title = group,
                        channels = groupChannels,
                        favoriteIds = favoriteIds,
                        onChannelFocus = onChannelFocus,
                        onChannelClick = onChannelClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    title: String,
    channels: List<Channel>,
    favoriteIds: Set<String>,
    onChannelFocus: (Channel) -> Unit,
    onChannelClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Row header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getCategoryIcon(title),
                    fontSize = 20.sp
                )
                Text(
                    text = title,
                    color = NovaColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${channels.size} channels",
                color = NovaColors.TextMuted,
                fontSize = 13.sp
            )
        }

        // Channel cards
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = favoriteIds.contains(channel.id),
                    onFocus = { onChannelFocus(channel) },
                    onClick = { onChannelClick(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale = if (isFocused) 1.08f else 1f

    Column(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable { onClick() }
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo container
        Box(
            modifier = Modifier
                .size(140.dp, 100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) NovaColors.PrimaryContainer else NovaColors.Surface
                )
                .then(
                    if (isFocused) {
                        Modifier.border(3.dp, NovaColors.Primary, RoundedCornerShape(16.dp))
                    } else {
                        Modifier.border(1.dp, NovaColors.Border, RoundedCornerShape(16.dp))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo != null) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = channel.name.take(3).uppercase(),
                    color = if (isFocused) NovaColors.Primary else NovaColors.TextMuted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Favorite indicator
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(NovaColors.Secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Channel name
        Text(
            text = channel.name,
            color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📺", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading channels...",
                color = NovaColors.TextMuted,
                fontSize = 18.sp
            )
        }
    }
}

private fun getCategoryIcon(category: String): String {
    val lower = category.lowercase()
    return when {
        lower == "all" -> "📺"
        lower.contains("sport") -> "⚽"
        lower.contains("news") -> "📰"
        lower.contains("movie") || lower.contains("film") -> "🎬"
        lower.contains("music") -> "🎵"
        lower.contains("kids") || lower.contains("child") -> "🧸"
        lower.contains("document") -> "🎥"
        lower.contains("entertainment") -> "🎭"
        lower.contains("usa") || lower.contains("us |") -> "🇺🇸"
        lower.contains("uk |") || lower.contains("british") -> "🇬🇧"
        lower.contains("canada") -> "🇨🇦"
        lower.contains("germany") || lower.contains("german") -> "🇩🇪"
        lower.contains("france") || lower.contains("french") -> "🇫🇷"
        lower.contains("spain") || lower.contains("spanish") -> "🇪🇸"
        lower.contains("italy") || lower.contains("italian") -> "🇮🇹"
        lower.contains("arab") || lower.contains("arabic") -> "🇸🇦"
        lower.contains("india") || lower.contains("hindi") -> "🇮🇳"
        lower.contains("latin") || lower.contains("latino") -> "🌎"
        lower.contains("adult") || lower.contains("xxx") -> "🔞"
        lower.contains("religious") || lower.contains("faith") -> "⛪"
        lower.contains("cooking") || lower.contains("food") -> "🍳"
        lower.contains("travel") -> "✈️"
        lower.contains("science") -> "🔬"
        lower.contains("history") -> "📜"
        lower.contains("nature") || lower.contains("animal") -> "🌿"
        lower.contains("24/7") -> "🔄"
        else -> "📡"
    }
}
