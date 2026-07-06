package com.iptvplayer.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.ui.components.AddPlaylistDialog
import com.iptvplayer.tv.ui.components.ContentRow
import com.iptvplayer.tv.ui.components.HeroCarousel
import com.iptvplayer.tv.ui.components.NavItem
import com.iptvplayer.tv.ui.components.NavRail
import com.iptvplayer.tv.ui.components.PlaylistCard
import com.iptvplayer.tv.ui.components.PlaylistOptionsDialog
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun HomeScreen(
    onPlaylistClick: (String) -> Unit,
    onVodClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onGuideClick: (String) -> Unit = {},
    onVodItemClick: (VodItem) -> Unit = {},
    onSeriesItemClick: (SeriesItem) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val dashboardState by viewModel.dashboardState.collectAsState()
    val addState by viewModel.addPlaylistState.collectAsState()
    val optionsState by viewModel.playlistOptionsState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf(NavItem.HOME) }

    // Get first Xtream playlist for quick navigation (Live TV/Movies/Series need playlist context)
    val firstXtreamPlaylist = playlists.firstOrNull {
        it.type == com.iptvplayer.tv.data.model.PlaylistType.XTREAM
    }
    val firstPlaylist = playlists.firstOrNull()

    // Auto-close dialog on success
    LaunchedEffect(addState.success) {
        if (addState.success != null) {
            kotlinx.coroutines.delay(1500)
            showAddDialog = false
            viewModel.clearAddPlaylistState()
        }
    }

    // Refresh dashboard when playlists change
    LaunchedEffect(playlists.size) {
        viewModel.refreshDashboard()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Sidebar Navigation
        NavRail(
            selectedItem = selectedNav,
            onItemSelected = { item ->
                selectedNav = item
                when (item) {
                    NavItem.HOME -> { /* Already on home */ }
                    NavItem.LIVE_TV -> {
                        firstPlaylist?.let { onPlaylistClick(it.id) }
                    }
                    NavItem.GUIDE -> {
                        firstPlaylist?.let { onGuideClick(it.id) }
                    }
                    NavItem.MOVIES -> {
                        firstXtreamPlaylist?.let { onVodClick(it.id) }
                    }
                    NavItem.SERIES -> {
                        firstXtreamPlaylist?.let { onSeriesClick(it.id) }
                    }
                    NavItem.SETTINGS -> onSettingsClick()
                }
            }
        )

        // Main Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp)
        ) {
            // Search button - top right
            SearchButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp)
            )

            TvLazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Carousel - top rated content
                if (dashboardState.heroItems.isNotEmpty()) {
                    item {
                        HeroCarousel(
                            items = dashboardState.heroItems,
                            onItemClick = { item ->
                                when (item) {
                                    is VodItem -> onVodItemClick(item)
                                    is SeriesItem -> onSeriesItemClick(item)
                                }
                            }
                        )
                    }
                } else {
                    // Fallback welcome hero if no content yet
                    item {
                        WelcomeHero(onAddClick = { showAddDialog = true })
                    }
                }

                // Top Rated Movies
                if (dashboardState.topMovies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ContentRow(
                            title = "Top Rated Movies",
                            items = dashboardState.topMovies,
                            onItemClick = { item ->
                                if (item is VodItem) onVodItemClick(item)
                            }
                        )
                    }
                }

                // Popular Series
                if (dashboardState.topSeries.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ContentRow(
                            title = "Popular Series",
                            items = dashboardState.topSeries,
                            onItemClick = { item ->
                                if (item is SeriesItem) onSeriesItemClick(item)
                            }
                        )
                    }
                }

                // Recently Added
                if (dashboardState.recentlyAdded.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ContentRow(
                            title = "Recently Added",
                            items = dashboardState.recentlyAdded,
                            onItemClick = { item ->
                                when (item) {
                                    is VodItem -> onVodItemClick(item)
                                    is SeriesItem -> onSeriesItemClick(item)
                                }
                            }
                        )
                    }
                }

                // Your Playlists Section
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    PlaylistsSection(
                        title = "Your Playlists",
                        subtitle = "${playlists.size} sources",
                        showAddButton = true,
                        onAddClick = { showAddDialog = true }
                    )
                }

                item {
                    if (playlists.isEmpty()) {
                        EmptyPlaylistsState(onAddClick = { showAddDialog = true })
                    } else {
                        TvLazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist.id) },
                                    onLongClick = { viewModel.showPlaylistOptions(playlist) }
                                )
                            }
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = {
                if (!addState.isLoading) {
                    showAddDialog = false
                    viewModel.clearAddPlaylistState()
                }
            },
            onAddXtream = { name, url, user, pass ->
                viewModel.addXtreamPlaylist(name, url, user, pass)
            },
            onAddM3u = { name, url ->
                viewModel.addM3uPlaylist(name, url)
            },
            isLoading = addState.isLoading,
            error = addState.error,
            success = addState.success
        )
    }

    // Playlist options dialog (edit/refresh/delete)
    optionsState.selectedPlaylist?.let { playlist ->
        PlaylistOptionsDialog(
            playlist = playlist,
            onDismiss = { viewModel.hidePlaylistOptions() },
            onEdit = { name, serverUrl, username, password, m3uUrl ->
                viewModel.editPlaylist(playlist.id, name, serverUrl, username, password, m3uUrl)
            },
            onRefresh = { viewModel.refreshPlaylist(playlist.id) },
            onDelete = { viewModel.deletePlaylist(playlist.id) },
            onLiveTV = { onPlaylistClick(playlist.id) },
            onMovies = { onVodClick(playlist.id) },
            onSeries = { onSeriesClick(playlist.id) },
            isLoading = optionsState.isLoading,
            error = optionsState.error,
            success = optionsState.success
        )
    }
}

@Composable
private fun WelcomeHero(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NovaColors.PrimaryContainer,
                        NovaColors.Surface,
                        NovaColors.Background
                    )
                )
            )
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(200.dp)
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
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "nova",
                    color = NovaColors.TextPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ".tv",
                    color = NovaColors.Primary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Stream live TV, movies, and series",
                color = NovaColors.TextMuted,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(label = "Live TV", icon = "📺")
                StatBadge(label = "Movies", icon = "🎬")
                StatBadge(label = "Series", icon = "📚")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaColors.Primary)
                    .clickable(onClick = onAddClick)
                    .focusable()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "+ Add Your First Playlist",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, icon: String) {
    Row(
        modifier = Modifier
            .background(
                NovaColors.Surface.copy(alpha = 0.8f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text = label,
            color = NovaColors.TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PlaylistsSection(
    title: String,
    subtitle: String,
    showAddButton: Boolean = false,
    onAddClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = NovaColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = NovaColors.TextMuted,
                fontSize = 14.sp
            )
        }

        if (showAddButton) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaColors.Primary.copy(alpha = 0.15f))
                    .clickable(onClick = onAddClick)
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Add",
                    color = NovaColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistsState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NovaColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No playlists added yet",
                color = NovaColors.TextMuted,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaColors.Primary)
                    .clickable(onClick = onAddClick)
                    .focusable()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Add Playlist",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isFocused) NovaColors.Primary else NovaColors.Surface.copy(alpha = 0.8f)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = if (isFocused) Color.Black else NovaColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}
