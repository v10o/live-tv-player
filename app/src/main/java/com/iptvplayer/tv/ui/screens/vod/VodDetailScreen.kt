package com.iptvplayer.tv.ui.screens.vod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun VodDetailScreen(
    vodId: String? = null,
    searchTitle: String? = null,
    onPlayClick: (title: String, url: String) -> Unit,
    onBackPress: () -> Unit,
    viewModel: VodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playButtonFocusRequester = remember { FocusRequester() }

    // Focus play button when content loads
    LaunchedEffect(uiState.isLoading, uiState.error) {
        if (!uiState.isLoading && uiState.error == null && uiState.title.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            try {
                playButtonFocusRequester.requestFocus()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(vodId, searchTitle) {
        when {
            searchTitle != null -> viewModel.loadByTitle(searchTitle)
            vodId != null -> viewModel.loadVodDetail(vodId)
        }
    }

    BackHandler { onBackPress() }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaColors.Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NovaColors.Primary)
        }
        return
    }

    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error!!, color = NovaColors.TextMuted)
                Spacer(modifier = Modifier.height(16.dp))
                ActionButton(
                    icon = Icons.Default.ArrowBack,
                    label = "Go Back",
                    isPrimary = false,
                    onClick = onBackPress
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background)
    ) {
        // Backdrop/Poster
        uiState.poster?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NovaColors.Background.copy(alpha = 0.8f),
                            NovaColors.Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState())
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
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NovaColors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NovaColors.Surface)
                ) {
                    if (uiState.poster != null) {
                        AsyncImage(
                            model = uiState.poster,
                            contentDescription = uiState.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.title.take(2).uppercase(),
                                color = NovaColors.TextMuted,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Badge row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(NovaColors.Secondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("MOVIE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        uiState.rating?.let { rating ->
                            Text("⭐ $rating", color = NovaColors.TextMuted, fontSize = 12.sp)
                        }
                        uiState.year?.let { year ->
                            Text(year, color = NovaColors.TextMuted, fontSize = 12.sp)
                        }
                        uiState.duration?.let { duration ->
                            Text(duration, color = NovaColors.TextMuted, fontSize = 12.sp)
                        }
                    }

                    // Title
                    Text(
                        text = uiState.title,
                        color = NovaColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Genre
                    uiState.genre?.let { genre ->
                        Text(
                            text = genre,
                            color = NovaColors.Primary,
                            fontSize = 14.sp
                        )
                    }

                    // Plot/Description
                    uiState.plot?.let { plot ->
                        Text(
                            text = plot,
                            color = NovaColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Director & Release info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        uiState.director?.let { director ->
                            Column {
                                Text("Director", color = NovaColors.TextMuted, fontSize = 11.sp)
                                Text(director, color = NovaColors.TextSecondary, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                        uiState.releaseDate?.let { releaseDate ->
                            Column {
                                Text("Released", color = NovaColors.TextMuted, fontSize = 11.sp)
                                Text(releaseDate, color = NovaColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                        uiState.country?.let { country ->
                            Column {
                                Text("Country", color = NovaColors.TextMuted, fontSize = 11.sp)
                                Text(country, color = NovaColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }

                    // Cast
                    uiState.cast?.let { cast ->
                        Column {
                            Text("Cast", color = NovaColors.TextMuted, fontSize = 11.sp)
                            Text(
                                text = cast,
                                color = NovaColors.TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Watchlist button
                    ActionButton(
                        icon = if (uiState.isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                        label = if (uiState.isInWatchlist) "In Watchlist" else "Add to Watchlist",
                        isPrimary = false,
                        onClick = { viewModel.toggleWatchlist() }
                    )
                }
            }

            // Sources section - inline on detail page
            if (uiState.alternateLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Available Sources (${uiState.alternateLinks.size})",
                    color = NovaColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.alternateLinks.forEachIndexed { index, link ->
                        SourceItem(
                            link = link,
                            focusRequester = if (index == 0) playButtonFocusRequester else null,
                            onClick = { onPlayClick(uiState.title, link.streamUrl) }
                        )
                    }
                }
            } else {
                // Single source - show play button
                Spacer(modifier = Modifier.height(16.dp))

                ActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Play",
                    isPrimary = true,
                    focusRequester = playButtonFocusRequester,
                    onClick = {
                        val url = viewModel.getStreamUrl()
                        if (url != null) {
                            onPlayClick(uiState.title, url)
                        }
                    }
                )
            }

            // Bottom padding for scroll
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .scale(if (isFocused) 1.05f else 1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPrimary) {
                    Brush.linearGradient(
                        colors = if (isFocused)
                            listOf(NovaColors.Primary.copy(alpha = 0.9f), NovaColors.PrimaryDark.copy(alpha = 0.9f))
                        else
                            listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                    )
                } else {
                    Brush.linearGradient(
                        colors = if (isFocused)
                            listOf(NovaColors.Primary.copy(alpha = 0.3f), NovaColors.Primary.copy(alpha = 0.2f))
                        else
                            listOf(NovaColors.Surface, NovaColors.Surface)
                    )
                }
            )
            .then(
                if (isFocused) Modifier.border(3.dp, if (isPrimary) Color.White else NovaColors.Primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) NovaColors.OnPrimary else if (isFocused) NovaColors.Primary else NovaColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (isPrimary) NovaColors.OnPrimary else if (isFocused) NovaColors.Primary else NovaColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SourceItem(
    link: VodLinkOption,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.02f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) NovaColors.Primary.copy(alpha = 0.15f) else NovaColors.Surface)
            .then(
                if (isFocused) Modifier.border(2.dp, NovaColors.Primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Play icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isFocused) NovaColors.Primary else NovaColors.SurfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isFocused) Color.Black else NovaColors.TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = link.playlistName,
                color = if (isFocused) NovaColors.Primary else NovaColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                link.vodItem.categoryName?.let { cat ->
                    Text(
                        text = cat,
                        color = NovaColors.TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                link.vodItem.containerExtension?.let { ext ->
                    Box(
                        modifier = Modifier
                            .background(NovaColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ext.uppercase(),
                            color = NovaColors.Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                link.vodItem.rating5based?.let { rating ->
                    Text(
                        text = "⭐ %.1f".format(rating),
                        color = NovaColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Arrow indicator when focused
        if (isFocused) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = NovaColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
