package com.iptvplayer.tv.ui.screens.watchlist

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.ui.components.TopNavBar
import com.iptvplayer.tv.ui.components.TopNavItem
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun WatchlistScreen(
    onItemClick: (WatchlistItem) -> Unit,
    onBackPress: () -> Unit,
    onHomeClick: () -> Unit = {},
    onLiveTVClick: () -> Unit = {},
    onMoviesClick: () -> Unit = {},
    onShowsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WatchlistTab.ALL) }
    val focusRequester = remember { FocusRequester() }

    val displayedItems = when (selectedTab) {
        WatchlistTab.ALL -> uiState.allItems
        WatchlistTab.MOVIES -> uiState.movieItems
        WatchlistTab.SERIES -> uiState.seriesItems
    }

    // Auto-focus first item
    LaunchedEffect(displayedItems.isNotEmpty()) {
        if (displayedItems.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    BackHandler { onBackPress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Top Navigation Bar
        TopNavBar(
            selectedItem = TopNavItem.WATCHLIST,
            onItemSelected = { item ->
                when (item) {
                    TopNavItem.HOME -> onHomeClick()
                    TopNavItem.LIVE_TV -> onLiveTVClick()
                    TopNavItem.MOVIES -> onMoviesClick()
                    TopNavItem.SHOWS -> onShowsClick()
                    TopNavItem.WATCHLIST -> { /* Already here */ }
                    TopNavItem.SEARCH -> onSearchClick()
                }
            },
            onSettingsClick = onSettingsClick
        )

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "My Watchlist",
                color = NovaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${uiState.allItems.size} items saved",
                color = NovaColors.TextMuted,
                fontSize = 14.sp
            )
        }

        // Tab selector
        TvLazyRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(WatchlistTab.entries) { tab ->
                TabChip(
                    label = tab.label,
                    count = when (tab) {
                        WatchlistTab.ALL -> uiState.allItems.size
                        WatchlistTab.MOVIES -> uiState.movieItems.size
                        WatchlistTab.SERIES -> uiState.seriesItems.size
                    },
                    isSelected = selectedTab == tab,
                    onClick = { selectedTab = tab }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (displayedItems.isEmpty()) {
            EmptyState(selectedTab)
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayedItems, key = { _, item -> item.id }) { index, item ->
                    WatchlistCard(
                        item = item,
                        focusRequester = if (index == 0) focusRequester else null,
                        onClick = { onItemClick(item) },
                        onRemove = { viewModel.removeFromWatchlist(item.itemId) }
                    )
                }
            }
        }
    }
}

enum class WatchlistTab(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("Series")
}

@Composable
private fun TabChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
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
                    Modifier.border(2.dp, NovaColors.Primary, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) NovaColors.OnPrimary else NovaColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) NovaColors.OnPrimary.copy(alpha = 0.2f) else NovaColors.SurfaceVariant,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = if (isSelected) NovaColors.OnPrimary else NovaColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WatchlistCard(
    item: WatchlistItem,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(150.dp)
            .scale(if (isFocused) 1.08f else 1f)
            .let { m -> if (focusRequester != null) m.focusRequester(focusRequester) else m }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NovaColors.Surface)
                .then(
                    if (isFocused) {
                        Modifier.border(3.dp, NovaColors.Primary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier.border(1.dp, NovaColors.Border, RoundedCornerShape(12.dp))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.cover != null) {
                AsyncImage(
                    model = item.cover,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = if (item.type == "vod") "🎬" else "📺",
                    fontSize = 40.sp
                )
            }

            // Type badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        if (item.type == "vod") NovaColors.Secondary else NovaColors.Primary,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item.type == "vod") "MOVIE" else "SERIES",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.name,
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
private fun EmptyState(tab: WatchlistTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (tab) {
                    WatchlistTab.ALL -> "📚"
                    WatchlistTab.MOVIES -> "🎬"
                    WatchlistTab.SERIES -> "📺"
                },
                fontSize = 64.sp
            )
            Text(
                text = when (tab) {
                    WatchlistTab.ALL -> "Your watchlist is empty"
                    WatchlistTab.MOVIES -> "No movies in watchlist"
                    WatchlistTab.SERIES -> "No series in watchlist"
                },
                color = NovaColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Browse Movies or Shows to add items",
                color = NovaColors.TextMuted,
                fontSize = 14.sp
            )
        }
    }
}
