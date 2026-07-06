package com.iptvplayer.tv.ui.screens.series

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
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun SeriesScreen(
    playlistId: String,
    onSeriesClick: (SeriesItem) -> Unit,
    onBackPress: () -> Unit,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val selectedItem = remember(uiState.selectedItemId, uiState.seriesItems) {
        uiState.seriesItems.find { it.id == uiState.selectedItemId }
            ?: uiState.seriesItems.firstOrNull()
    }

    val selectedCategoryIndex = viewModel.getSelectedCategoryIndex()

    LaunchedEffect(playlistId) {
        viewModel.loadSeries(playlistId)
    }

    LaunchedEffect(uiState.seriesItems) {
        if (uiState.selectedItemId == null && uiState.seriesItems.isNotEmpty()) {
            viewModel.setSelectedItem(uiState.seriesItems.first().id)
        }
    }

    BackHandler { onBackPress() }

    if (uiState.isLoading) {
        LoadingState()
        return
    }

    if (uiState.error != null) {
        ErrorState(error = uiState.error!!, onBackPress = onBackPress)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Left Sidebar
        CategorySidebar(
            playlistName = uiState.playlistName,
            categories = listOf("All") + uiState.categories,
            selectedIndex = selectedCategoryIndex,
            onCategorySelected = { index ->
                val category = if (index == 0) null else uiState.categories.getOrNull(index - 1)
                viewModel.selectCategory(category)
            },
            onBackPress = onBackPress
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Empty state when no series
            if (uiState.seriesItems.isEmpty() && !uiState.isLoading) {
                EmptySeriesState(
                    totalCount = uiState.totalCount,
                    onRetry = { viewModel.loadSeries(playlistId, forceReload = true) }
                )
            } else {
                // Hero Section
                selectedItem?.let { item ->
                    HeroPreview(
                        seriesItem = item,
                        onSelect = { onSeriesClick(item) }
                    )
                }

                // Series Grid with infinite scroll
                val gridState = rememberTvLazyGridState()

            // Trigger load more when near end
            LaunchedEffect(gridState.firstVisibleItemIndex, uiState.seriesItems.size) {
                val lastVisibleIndex = gridState.firstVisibleItemIndex + 10
                if (lastVisibleIndex >= uiState.seriesItems.size - 6 && uiState.canLoadMore && !uiState.isLoadingMore) {
                    viewModel.loadMore()
                }
            }

            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(minSize = 150.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(uiState.seriesItems, key = { _, item -> item.id }) { index, item ->
                    SeriesCard(
                        seriesItem = item,
                        onFocus = {
                            viewModel.setSelectedItem(item.id)
                            if (index >= uiState.seriesItems.size - 6 && uiState.canLoadMore) {
                                viewModel.loadMore()
                            }
                        },
                        onClick = { onSeriesClick(item) }
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading more...", color = NovaColors.TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
            } // End of else block for non-empty state
        }
    }
}

@Composable
private fun EmptySeriesState(
    totalCount: Int,
    onRetry: () -> Unit
) {
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
            Text("📺", fontSize = 64.sp)

            Text(
                text = "No series found",
                color = NovaColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "DB count: $totalCount items",
                color = NovaColors.TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            var isFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFocused) NovaColors.Primary else NovaColors.Surface)
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { onRetry() }
                    .focusable()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    "Retry Loading",
                    color = if (isFocused) NovaColors.OnPrimary else NovaColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Check logcat for SeriesVM tags",
                color = NovaColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CategorySidebar(
    playlistName: String,
    categories: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onBackPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(NovaColors.Surface)
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NovaColors.SurfaceVariant)
                    .clickable { onBackPress() }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = NovaColors.TextPrimary, fontSize = 16.sp)
            }
            Column {
                Text(
                    text = playlistName,
                    color = NovaColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "TV Series",
                    color = NovaColors.Primary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                CategoryItem(
                    name = category,
                    isSelected = index == selectedIndex,
                    onClick = { onCategorySelected(index) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isSelected -> NovaColors.Primary.copy(alpha = 0.15f)
        isFocused -> NovaColors.SurfaceVariant
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected || isFocused) 1.5.dp else 0.dp,
                color = if (isSelected) NovaColors.Primary else if (isFocused) NovaColors.Primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📺", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            color = when {
                isSelected -> NovaColors.Primary
                isFocused -> NovaColors.TextPrimary
                else -> NovaColors.TextSecondary
            },
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroPreview(
    seriesItem: SeriesItem,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(24.dp)
    ) {
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
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (seriesItem.cover != null) {
                    AsyncImage(
                        model = seriesItem.cover,
                        contentDescription = seriesItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📺", fontSize = 32.sp)
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(NovaColors.Primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("SERIES", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        seriesItem.genre?.let {
                            Text(it, color = NovaColors.TextMuted, fontSize = 12.sp, maxLines = 1)
                        }
                        seriesItem.rating?.let {
                            Text("⭐ $it", color = NovaColors.TextMuted, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = seriesItem.name,
                        color = NovaColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    seriesItem.plot?.let { plot ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plot,
                            color = NovaColors.TextMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                var isFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .scale(if (isFocused) 1.05f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                            )
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable { onSelect() }
                        .focusable()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📺", fontSize = 16.sp)
                        Text("View Details", color = NovaColors.OnPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(
    seriesItem: SeriesItem,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(150.dp)
            .scale(if (isFocused) 1.08f else 1f)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
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
            if (seriesItem.cover != null) {
                AsyncImage(
                    model = seriesItem.cover,
                    contentDescription = seriesItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("📺", fontSize = 40.sp)
            }

            seriesItem.rating5based?.let { rating ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(NovaColors.Primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⭐ %.1f".format(rating),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = seriesItem.name,
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
            Text("📺", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Loading series...", color = NovaColors.TextMuted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ErrorState(error: String, onBackPress: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("❌", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = NovaColors.TextMuted, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaColors.Surface)
                    .clickable { onBackPress() }
                    .focusable()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Go Back", color = NovaColors.TextPrimary)
            }
        }
    }
}
