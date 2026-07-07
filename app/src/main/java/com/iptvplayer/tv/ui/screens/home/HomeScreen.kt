package com.iptvplayer.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.iptvplayer.tv.data.api.TmdbItem
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.ui.components.AddPlaylistDialog
import com.iptvplayer.tv.ui.components.ContentRow
import com.iptvplayer.tv.ui.components.HeroCarousel
import com.iptvplayer.tv.ui.components.TopNavBar
import com.iptvplayer.tv.ui.components.TopNavItem
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
    onWatchlistClick: () -> Unit = {},
    onVodItemClick: (VodItem) -> Unit = {},
    onSeriesItemClick: (SeriesItem) -> Unit = {},
    onWatchlistItemClick: (WatchlistItem) -> Unit = {},
    onTmdbMovieClick: (title: String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val dashboardState by viewModel.dashboardState.collectAsState()
    val addState by viewModel.addPlaylistState.collectAsState()
    val optionsState by viewModel.playlistOptionsState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf(TopNavItem.HOME) }
    val coroutineScope = rememberCoroutineScope()

    // Handler for TMDB item clicks - navigate to detail page
    val onTmdbItemClick: (TmdbItem) -> Unit = { item ->
        android.util.Log.d("HomeScreen", "TMDB click: ${item.displayTitle}, type=${item.mediaType}")
        coroutineScope.launch {
            if (item.mediaType == "movie" || item.mediaType == null) {
                // Navigate to movie detail page (will find all matches there)
                onTmdbMovieClick(item.displayTitle)
            } else {
                // Series - find and navigate
                val match = viewModel.findContentByTitle(item.displayTitle, "tv")
                android.util.Log.d("HomeScreen", "Series match result: $match")
                if (match != null) {
                    val (_, id) = match
                    val parts = id.split("/")
                    if (parts.size == 2) {
                        val seriesItem = viewModel.getSeriesById(parts[0], parts[1].toIntOrNull() ?: 0)
                        android.util.Log.d("HomeScreen", "Series found: ${seriesItem?.name}")
                        seriesItem?.let { onSeriesItemClick(it) }
                    }
                }
            }
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Top Navigation Bar
        TopNavBar(
            selectedItem = selectedNav,
            onItemSelected = { item ->
                selectedNav = item
                when (item) {
                    TopNavItem.HOME -> { /* Already on home */ }
                    TopNavItem.LIVE_TV -> {
                        firstPlaylist?.let { onGuideClick(it.id) }
                    }
                    TopNavItem.MOVIES -> {
                        firstXtreamPlaylist?.let { onVodClick(it.id) }
                    }
                    TopNavItem.SHOWS -> {
                        firstXtreamPlaylist?.let { onSeriesClick(it.id) }
                    }
                    TopNavItem.WATCHLIST -> onWatchlistClick()
                    TopNavItem.SEARCH -> onSearchClick()
                }
            },
            onSettingsClick = onSettingsClick,
            onRefreshClick = { viewModel.refreshDashboard() },
            isRefreshing = dashboardState.isLoading
        )

        // Main Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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

                // Trending Movies (Top 10)
                if (dashboardState.trendingMovies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        TrendingRow(
                            title = "Top 10 Movies This Week",
                            items = dashboardState.trendingMovies,
                            getPosterUrl = { viewModel.getTmdbPosterUrl(it) },
                            onItemClick = onTmdbItemClick
                        )
                    }
                }

                // Trending Shows (Top 10)
                if (dashboardState.trendingShows.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        TrendingRow(
                            title = "Top 10 Shows This Week",
                            items = dashboardState.trendingShows,
                            getPosterUrl = { viewModel.getTmdbPosterUrl(it) },
                            onItemClick = onTmdbItemClick
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

                // My Watchlist
                if (dashboardState.watchlist.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ContentRow(
                            title = "My Watchlist",
                            items = dashboardState.watchlist,
                            onItemClick = { item ->
                                if (item is WatchlistItem) {
                                    onWatchlistItemClick(item)
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

            // Full screen loading overlay
            if (dashboardState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = NovaColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
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
                    text = "live",
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
                StatBadge(label = "Live TV", icon = Icons.Default.LiveTv)
                StatBadge(label = "Movies", icon = Icons.Default.Movie)
                StatBadge(label = "Series", icon = Icons.Default.VideoLibrary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            var addBtnFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .scale(if (addBtnFocused) 1.05f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaColors.Primary)
                    .then(
                        if (addBtnFocused) Modifier.border(3.dp, Color.White, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .onFocusChanged { addBtnFocused = it.isFocused }
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
private fun StatBadge(label: String, icon: ImageVector) {
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
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
            var isFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .scale(if (isFocused) 1.05f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFocused) NovaColors.Primary else NovaColors.Primary.copy(alpha = 0.15f))
                    .then(
                        if (isFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable(onClick = onAddClick)
                    .focusable()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Add",
                    color = if (isFocused) Color.Black else NovaColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistsState(onAddClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

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
                    .scale(if (isFocused) 1.05f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaColors.Primary)
                    .then(
                        if (isFocused) Modifier.border(3.dp, Color.White, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .onFocusChanged { isFocused = it.isFocused }
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
private fun TrendingRow(
    title: String,
    items: List<TmdbItem>,
    getPosterUrl: (String?) -> String?,
    onItemClick: (TmdbItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = NovaColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                TrendingCard(
                    item = item,
                    posterUrl = getPosterUrl(item.posterPath),
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun TrendingCard(
    item: TmdbItem,
    posterUrl: String?,
    onClick: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(240.dp)
            .scale(if (isFocused) 1.08f else 1f)
            .clip(shape)
            .background(NovaColors.Surface)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(3.dp, NovaColors.Primary, shape)
                } else Modifier
            )
            .clickable { onClick() }
            .focusable()
    ) {
        // Poster image
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovaColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.mediaType == "movie") Icons.Default.Movie else Icons.Default.Tv,
                    contentDescription = null,
                    tint = NovaColors.TextMuted,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Rank badge (Top 10 style)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(32.dp)
                .background(NovaColors.Primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${item.rank}",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Rating badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = item.rating,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Title and year at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = item.displayTitle,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.year?.let { year ->
                Text(
                    text = year,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
