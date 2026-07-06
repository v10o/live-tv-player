package com.iptvplayer.tv.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun SearchScreen(
    onChannelClick: (Channel) -> Unit,
    onVodClick: (VodItem, String) -> Unit,
    onSeriesClick: (SeriesItem) -> Unit,
    onBackPress: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    BackHandler { onBackPress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Header with search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaColors.Surface)
                    .clickable { onBackPress() }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NovaColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Search input
            SearchInput(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.search(it)
                },
                focusRequester = searchFocusRequester,
                modifier = Modifier.weight(1f)
            )
        }

        // Results
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NovaColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            searchText.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NovaColors.TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search channels, movies, and series",
                            color = NovaColors.TextMuted,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            uiState.isEmpty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No results found",
                            color = NovaColors.TextMuted,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try a different search term",
                            color = NovaColors.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            else -> {
                SearchResults(
                    channels = uiState.channels,
                    vodItems = uiState.vodItems,
                    seriesItems = uiState.seriesItems,
                    onChannelClick = onChannelClick,
                    onVodClick = { vodItem ->
                        val url = viewModel.getVodStreamUrl(vodItem)
                        if (url != null) onVodClick(vodItem, url)
                    },
                    onSeriesClick = onSeriesClick
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) NovaColors.SurfaceVariant else NovaColors.Surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (isFocused) NovaColors.Primary else NovaColors.TextMuted,
            modifier = Modifier.size(24.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                color = NovaColors.TextPrimary,
                fontSize = 16.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(NovaColors.Primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search...",
                            color = NovaColors.TextMuted,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = NovaColors.TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

@Composable
private fun SearchResults(
    channels: List<Channel>,
    vodItems: List<VodItem>,
    seriesItems: List<SeriesItem>,
    onChannelClick: (Channel) -> Unit,
    onVodClick: (VodItem) -> Unit,
    onSeriesClick: (SeriesItem) -> Unit
) {
    TvLazyVerticalGrid(
        columns = TvGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Channels section
        if (channels.isNotEmpty()) {
            item {
                Text(
                    text = "Channels (${channels.size})",
                    color = NovaColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(channels, key = { "ch_${it.id}" }) { channel ->
            SearchResultCard(
                title = channel.name,
                subtitle = channel.group ?: "Live TV",
                imageUrl = channel.logo,
                badge = "LIVE",
                onClick = { onChannelClick(channel) }
            )
        }

        // VOD section
        if (vodItems.isNotEmpty()) {
            item {
                Text(
                    text = "Movies (${vodItems.size})",
                    color = NovaColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(vodItems, key = { "vod_${it.id}" }) { vodItem ->
            SearchResultCard(
                title = vodItem.name,
                subtitle = vodItem.categoryName ?: "Movie",
                imageUrl = vodItem.icon,
                badge = vodItem.rating?.let { "★ $it" },
                onClick = { onVodClick(vodItem) }
            )
        }

        // Series section
        if (seriesItems.isNotEmpty()) {
            item {
                Text(
                    text = "Series (${seriesItems.size})",
                    color = NovaColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(seriesItems, key = { "ser_${it.id}" }) { seriesItem ->
            SearchResultCard(
                title = seriesItem.name,
                subtitle = seriesItem.categoryName ?: "Series",
                imageUrl = seriesItem.cover,
                badge = seriesItem.rating?.let { "★ $it" },
                onClick = { onSeriesClick(seriesItem) }
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    badge: String?,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) NovaColors.SurfaceVariant else NovaColors.Surface)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(NovaColors.SurfaceVariant)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(2).uppercase(),
                        color = NovaColors.TextMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badge
            badge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            if (it == "LIVE") NovaColors.Secondary else NovaColors.Primary,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Info
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = NovaColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
