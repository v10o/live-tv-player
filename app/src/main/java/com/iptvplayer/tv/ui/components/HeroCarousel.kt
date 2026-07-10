package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.api.TmdbItem
import com.iptvplayer.tv.ui.theme.NovaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hero carousel for top-rated content.
 * - Backdrop image (full bleed) lives in the pager.
 * - Title / rating / Watch Now live in a fixed overlay so they don't shift
 *   position when swiping.
 * - Auto-advances every 5 seconds, pauses when focused.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    items: List<Any>,
    onItemClick: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    var isPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-advance every 5 seconds when not paused
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .onFocusChanged { isPaused = it.hasFocus }
    ) {
        // Pager: image only, full bleed
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroBackdrop(url = backdropUrlFor(items[page]))
        }

        // Subtle left-side darken so the fixed text overlay stays readable
        // regardless of which image is showing. No full-bleed darkening.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.6f),
                            0.35f to Color.Black.copy(alpha = 0.25f),
                            0.7f to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    )
                )
        )

        // Fixed content overlay (does not move with pager)
        if (pagerState.currentPage in items.indices) {
            HeroContentOverlay(
                item = items[pagerState.currentPage],
                onClick = { onItemClick(items[pagerState.currentPage]) },
                onLeft = {
                    val prev = (pagerState.currentPage - 1 + items.size) % items.size
                    scope.launch { pagerState.animateScrollToPage(prev) }
                },
                onRight = {
                    val next = (pagerState.currentPage + 1) % items.size
                    scope.launch { pagerState.animateScrollToPage(next) }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(start = 48.dp, end = 200.dp)
            )
        }

        // Page indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .background(
                            color = if (index == pagerState.currentPage) {
                                NovaColors.Primary
                            } else {
                                NovaColors.TextMuted.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/** Image-only slide — fills the pager, no overlay. */
@Composable
private fun HeroBackdrop(url: String?) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/** Fixed overlay holding rating + title + Watch Now. */
@Composable
private fun HeroContentOverlay(
    item: Any,
    onClick: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, rating, genre, year) = remember(item) {
        when (item) {
            is VodItem -> Quad(title = item.name, rating = item.rating5based, genre = item.categoryName, year = item.added?.take(4))
            is SeriesItem -> Quad(title = item.name, rating = item.rating5based, genre = item.genre, year = item.releaseDate)
            is TmdbItem -> Quad(title = item.displayTitle, rating = item.voteAverage, genre = null, year = item.year)
            else -> Quad("", null, null, null)
        }
    }

    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        if (rating != null && rating > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                rating >= 4.0 -> NovaColors.Primary
                                rating >= 3.0 -> Color(0xFFFFB800)
                                else -> NovaColors.TextMuted
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", rating)}",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val metaText = listOfNotNull(genre, year).joinToString(" | ")
                if (metaText.isNotEmpty()) {
                    Text(
                        text = "  |  $metaText",
                        color = NovaColors.TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        val buttonScale by animateFloatAsState(
            targetValue = if (isFocused) 1.1f else 1f,
            animationSpec = tween(150),
            label = "watchScale"
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                    shadowElevation = if (isFocused) 16f else 0f
                    shape = RoundedCornerShape(8.dp)
                    clip = false
                }
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft, Key.MediaPrevious -> {
                            onLeft(); true
                        }
                        Key.DirectionRight, Key.MediaNext -> {
                            onRight(); true
                        }
                        else -> false
                    }
                }
                .focusable()
                .clickable(onClick = onClick)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isFocused) NovaColors.Primary
                    else Color(0xFF2A2A2A)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "▶  Watch Now",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun backdropUrlFor(item: Any): String? = when (item) {
    is VodItem -> item.icon
    is SeriesItem -> item.backdropPath ?: item.cover
    is TmdbItem -> item.backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
    else -> null
}

private data class Quad(val title: String, val rating: Double?, val genre: String?, val year: String?)
