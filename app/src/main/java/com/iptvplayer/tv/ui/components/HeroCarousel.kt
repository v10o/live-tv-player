package com.iptvplayer.tv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.ui.theme.NovaColors
import kotlinx.coroutines.delay

/**
 * Hero carousel for top-rated content.
 * Auto-advances every 5 seconds, pauses when focused.
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroSlide(
                item = items[page],
                onClick = { onItemClick(items[page]) }
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

@Composable
private fun HeroSlide(
    item: Any,
    onClick: () -> Unit
) {
    val (title, posterUrl, backdropUrl, rating, genre, year) = remember(item) {
        when (item) {
            is VodItem -> HeroData(
                title = item.name,
                posterUrl = item.icon,
                backdropUrl = item.icon, // VOD doesn't have separate backdrop
                rating = item.rating5based,
                genre = item.categoryName,
                year = item.added?.take(4) // "added" is unix timestamp string, extract year
            )
            is SeriesItem -> HeroData(
                title = item.name,
                posterUrl = item.cover,
                backdropUrl = item.backdropPath ?: item.cover,
                rating = item.rating5based,
                genre = item.genre,
                year = item.releaseDate
            )
            else -> HeroData("", null, null, null, null, null)
        }
    }

    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        // Backdrop image
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 200f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp, end = 200.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Rating badge
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

                    // Genre and year
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

            // Title
            Text(
                text = title,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Watch Now button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFocused) NovaColors.Primary else NovaColors.Primary.copy(alpha = 0.9f)
                    )
                    .then(
                        if (isFocused) {
                            Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        } else Modifier
                    )
                    .clickable(onClick = onClick)
                    .focusable()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "▶  Watch Now",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class HeroData(
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double?,
    val genre: String?,
    val year: String?
)
