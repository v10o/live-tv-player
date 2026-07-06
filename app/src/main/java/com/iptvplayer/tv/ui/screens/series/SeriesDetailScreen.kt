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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun SeriesDetailScreen(
    playlistId: String,
    seriesId: Int,
    onEpisodeClick: (title: String, url: String) -> Unit,
    onBackPress: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(playlistId, seriesId) {
        viewModel.loadSeriesDetail(playlistId, seriesId)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Backdrop
        uiState.backdrop?.let { backdrop ->
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NovaColors.Background.copy(alpha = 0.7f),
                            NovaColors.Background
                        )
                    )
                )
        )

        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header with back button and series info
            item {
                SeriesHeader(
                    uiState = uiState,
                    onBackPress = onBackPress
                )
            }

            // Season selector
            item {
                if (uiState.seasons.isNotEmpty()) {
                    SeasonSelector(
                        seasons = uiState.seasons,
                        selectedSeason = uiState.selectedSeason,
                        onSeasonSelected = { viewModel.selectSeason(it) }
                    )
                }
            }

            // Episodes list
            items(uiState.currentSeasonEpisodes) { episode ->
                EpisodeCard(
                    episode = episode,
                    seriesName = uiState.seriesName,
                    onClick = {
                        val url = viewModel.getEpisodeStreamUrl(episode)
                        if (url != null) {
                            val title = "${uiState.seriesName} - S${episode.season}E${episode.episodeNum}"
                            onEpisodeClick(title, url)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SeriesHeader(
    uiState: SeriesDetailUiState,
    onBackPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NovaColors.Surface.copy(alpha = 0.8f))
                .clickable { onBackPress() }
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "←", fontSize = 20.sp, color = NovaColors.TextPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NovaColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.cover != null) {
                    AsyncImage(
                        model = uiState.cover,
                        contentDescription = uiState.seriesName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📺", fontSize = 48.sp)
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    uiState.rating?.let {
                        Text("⭐ $it", color = NovaColors.TextMuted, fontSize = 12.sp)
                    }
                    Text(
                        "${uiState.seasons.size} Seasons",
                        color = NovaColors.TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = uiState.seriesName,
                    color = NovaColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                uiState.genre?.let { genre ->
                    Text(
                        text = genre,
                        color = NovaColors.Primary,
                        fontSize = 14.sp
                    )
                }

                uiState.plot?.let { plot ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plot,
                        color = NovaColors.TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.director?.let {
                        Column {
                            Text("Director", color = NovaColors.TextMuted, fontSize = 11.sp)
                            Text(it, color = NovaColors.TextSecondary, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                    uiState.releaseDate?.let {
                        Column {
                            Text("Released", color = NovaColors.TextMuted, fontSize = 11.sp)
                            Text(it, color = NovaColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Seasons",
            color = NovaColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(seasons) { season ->
                SeasonChip(
                    seasonNumber = season,
                    isSelected = season == selectedSeason,
                    onClick = { onSeasonSelected(season) }
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(
    seasonNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .scale(if (isFocused) 1.1f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(listOf(NovaColors.Primary, NovaColors.PrimaryDark))
                } else {
                    Brush.linearGradient(listOf(NovaColors.Surface, NovaColors.SurfaceVariant))
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Season $seasonNumber",
            color = if (isSelected) Color.White else NovaColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    seriesName: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .scale(if (isFocused) 1.02f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) NovaColors.SurfaceVariant else NovaColors.Surface)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NovaColors.Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode number
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NovaColors.Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${episode.episodeNum}",
                color = NovaColors.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NovaColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (episode.image != null) {
                AsyncImage(
                    model = episode.image,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("▶", fontSize = 24.sp)
            }
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.title,
                color = if (isFocused) NovaColors.TextPrimary else NovaColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "S${episode.season} E${episode.episodeNum}",
                    color = NovaColors.TextMuted,
                    fontSize = 12.sp
                )
                episode.duration?.let {
                    Text(
                        text = it,
                        color = NovaColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                episode.rating?.let {
                    Text(
                        text = "⭐ %.1f".format(it),
                        color = NovaColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            episode.plot?.let { plot ->
                Text(
                    text = plot,
                    color = NovaColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Play indicator
        if (isFocused) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NovaColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 16.sp, color = Color.White)
            }
        }
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
