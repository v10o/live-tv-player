package com.iptvplayer.tv.ui.screens.epg

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvplayer.tv.data.model.EpgChannel
import com.iptvplayer.tv.data.model.EpgProgram
import com.iptvplayer.tv.player.PlayerState
import com.iptvplayer.tv.ui.components.TopNavBar
import com.iptvplayer.tv.ui.components.TopNavItem
import com.iptvplayer.tv.ui.theme.NovaColors
import java.text.SimpleDateFormat
import java.util.*

private val PIXELS_PER_MINUTE = 4.dp
private val CHANNEL_ROW_HEIGHT = 56.dp
private val TIME_HEADER_HEIGHT = 40.dp
private val CHANNEL_INFO_WIDTH = 100.dp

@Composable
fun EpgScreen(
    playlistId: String,
    onChannelClick: (String) -> Unit,
    onBackPress: () -> Unit,
    onHomeClick: () -> Unit = {},
    onMoviesClick: () -> Unit = {},
    onShowsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val currentPlayingChannelId by viewModel.currentPlayingChannelId.collectAsState()
    val gridFocusRequester = remember { FocusRequester() }

    // Keep screen on while mini player is playing
    val view = LocalView.current
    DisposableEffect(playerState.isPlaying) {
        view.keepScreenOn = playerState.isPlaying
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(playlistId) {
        viewModel.loadEpg(playlistId)
    }

    // Request focus on grid when channels are loaded
    LaunchedEffect(uiState.channels) {
        if (uiState.channels.isNotEmpty()) {
            try {
                gridFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request can fail if not yet attached
            }
        }
    }

    BackHandler {
        viewModel.stopPlayback()
        onBackPress()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaColors.Background)
        ) {
            // Top Navigation Bar
            TopNavBar(
            selectedItem = TopNavItem.LIVE_TV,
            onItemSelected = { item ->
                when (item) {
                    TopNavItem.HOME -> onHomeClick()
                    TopNavItem.LIVE_TV -> { /* Already here */ }
                    TopNavItem.MOVIES -> onMoviesClick()
                    TopNavItem.SHOWS -> onShowsClick()
                    TopNavItem.WATCHLIST -> onHomeClick()
                    TopNavItem.SEARCH -> onSearchClick()
                }
            },
            onSettingsClick = onSettingsClick,
            onRefreshClick = { viewModel.refresh() },
            isRefreshing = uiState.isRefreshing
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left sidebar
            EpgSidebar(
                currentTime = uiState.currentTime,
                selectedCategory = uiState.selectedCategory,
                categories = uiState.categories,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

        // Main content
        Column(modifier = Modifier.weight(1f)) {
            // Preview section with mini player
            uiState.selectedProgram?.let { program ->
                EpgPreview(
                    program = program,
                    channelName = uiState.channels.find {
                        it.programs.contains(program)
                    }?.name ?: "",
                    player = viewModel.player,
                    playerState = playerState
                )
            }

            // EPG Grid
            EpgGrid(
                channels = uiState.channels,
                currentTime = uiState.currentTime,
                startTime = uiState.gridStartTime,
                selectedProgram = uiState.selectedProgram,
                selectedChannelId = uiState.selectedProgram?.channelId,
                currentPlayingChannelId = currentPlayingChannelId,
                focusRequester = gridFocusRequester,
                onProgramSelected = { viewModel.selectProgram(it) },
                onProgramClick = { program ->
                    // Find channel
                    val channel = uiState.channels.find { ch ->
                        ch.programs.any { it.id == program.id }
                    }
                    channel?.let { ch ->
                        if (viewModel.isChannelPlaying(ch.id)) {
                            // Second click on playing channel → full player
                            onChannelClick(ch.id)
                        } else {
                            // First click → play in mini player
                            viewModel.playChannelById(ch.id, ch.url)
                        }
                    }
                }
            )
            }
        }
        }

        // Loading overlay on top of content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NovaColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun EpgSidebar(
    currentTime: Long,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mma", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(NovaColors.Surface)
            .padding(vertical = 16.dp)
    ) {
        // Current time
        Text(
            text = timeFormat.format(Date(currentTime)).lowercase(),
            color = NovaColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Categories
        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(categories) { category ->
                SidebarItem(
                    label = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> NovaColors.Primary.copy(alpha = 0.2f)
                    isFocused -> NovaColors.SurfaceVariant
                    else -> Color.Transparent
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Marquee text - only animate when selected or focused
        MarqueeText(
            text = label,
            color = when {
                isSelected -> NovaColors.Primary
                isFocused -> NovaColors.TextPrimary
                else -> NovaColors.TextSecondary
            },
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            isActive = isSelected || isFocused,
            modifier = Modifier.weight(1f)
        )
        if (label == "Favorites") {
            Spacer(modifier = Modifier.width(4.dp))
            Text("♥", color = NovaColors.Secondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MarqueeText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }

    // Only animate when active (selected or focused) AND text overflows
    val shouldAnimate = isActive && textWidth > containerWidth && containerWidth > 0

    // Reset scroll when not active
    LaunchedEffect(isActive) {
        if (!isActive) {
            scrollState.scrollTo(0)
        }
    }

    // Animate scroll only when active and text overflows
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            while (true) {
                kotlinx.coroutines.delay(500) // Initial delay
                // Scroll to end
                scrollState.animateScrollTo(
                    scrollState.maxValue,
                    animationSpec = tween(
                        durationMillis = ((textWidth - containerWidth) * 25).coerceAtLeast(1000),
                        easing = LinearEasing
                    )
                )
                kotlinx.coroutines.delay(1000)
                // Scroll back to start
                scrollState.animateScrollTo(
                    0,
                    animationSpec = tween(durationMillis = 300)
                )
                kotlinx.coroutines.delay(1500)
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { containerWidth = it.size.width }
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState, enabled = false)
                .onGloballyPositioned { textWidth = it.size.width }
        ) {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun EpgPreview(
    program: EpgProgram,
    channelName: String,
    player: ExoPlayer?,
    playerState: PlayerState
) {
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(NovaColors.Surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mini Player
        Box(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            if (player != null) {
                AndroidView(
                    factory = { context ->
                        android.view.SurfaceView(context).apply {
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    player.setVideoSurfaceHolder(holder)
                                }
                                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                    player.clearVideoSurfaceHolder(holder)
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Loading indicator
                if (playerState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = NovaColors.Primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Error indicator
                playerState.error?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NovaColors.Secondary.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Fallback placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = program.title.take(2).uppercase(),
                        color = NovaColors.TextMuted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                Text(
                    text = program.title,
                    color = NovaColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val startStr = timeFormat.format(Date(program.startTime))
                val endStr = timeFormat.format(Date(program.endTime))
                val episodeStr = program.episodeInfo?.let { "$it | " } ?: ""
                Text(
                    text = "$episodeStr$startStr - $endStr",
                    color = NovaColors.TextMuted,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                program.description?.let { desc ->
                    Text(
                        text = desc,
                        color = NovaColors.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgGrid(
    channels: List<EpgChannel>,
    currentTime: Long,
    startTime: Long,
    selectedProgram: EpgProgram?,
    selectedChannelId: String?,
    currentPlayingChannelId: String?,
    focusRequester: FocusRequester,
    onProgramSelected: (EpgProgram) -> Unit,
    onProgramClick: (EpgProgram) -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("EEE. M/d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mma", Locale.getDefault()) }

    // Calculate time slots (every 30 minutes for 4 hours)
    val timeSlots = remember(startTime) {
        (0 until 8).map { i ->
            startTime + (i * 30 * 60 * 1000)
        }
    }

    // Scroll to current time on initial load
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            // Calculate scroll position to show current time
            // Grid starts 1 hour before current time, so scroll by ~1 hour worth of pixels
            val minutesFromStart = ((currentTime - startTime) / 60000).toInt().coerceAtLeast(0)
            val scrollTo = (minutesFromStart * PIXELS_PER_MINUTE.value).toInt()
            // Scroll to show current time near the left edge (with some padding)
            val scrollPosition = (scrollTo - 50).coerceAtLeast(0)
            scrollState.animateScrollTo(scrollPosition)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Time header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TIME_HEADER_HEIGHT)
                .background(NovaColors.Surface)
        ) {
            // Empty space for channel info column
            Spacer(modifier = Modifier.width(CHANNEL_INFO_WIDTH))

            // Time slots
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                // Each slot is 30 minutes wide
                val slotWidth = (30 * PIXELS_PER_MINUTE.value).dp  // 30 min * 4dp = 120dp

                timeSlots.forEachIndexed { index, time ->
                    val label = if (index == 0) {
                        dateFormat.format(Date(time))
                    } else {
                        timeFormat.format(Date(time)).lowercase()
                    }

                    Box(
                        modifier = Modifier
                            .width(slotWidth)
                            .fillMaxHeight()
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = label,
                            color = NovaColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Channel rows
        TvLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels, key = { it.id }) { channel ->
                // First channel's first program gets focus requester
                val shouldFocus = channel.id == (selectedChannelId ?: channels.firstOrNull()?.id)
                EpgChannelRow(
                    channel = channel,
                    startTime = startTime,
                    currentTime = currentTime,
                    selectedProgram = selectedProgram,
                    isPlaying = channel.id == currentPlayingChannelId,
                    scrollState = scrollState,
                    focusRequester = if (shouldFocus) focusRequester else null,
                    onProgramSelected = onProgramSelected,
                    onProgramClick = onProgramClick
                )
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: EpgChannel,
    startTime: Long,
    currentTime: Long,
    selectedProgram: EpgProgram?,
    isPlaying: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    focusRequester: FocusRequester?,
    onProgramSelected: (EpgProgram) -> Unit,
    onProgramClick: (EpgProgram) -> Unit
) {
    // Find currently airing program or first program
    val currentProgram = channel.programs.find { it.isCurrentlyAiring(currentTime) }
        ?: channel.programs.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHANNEL_ROW_HEIGHT)
            .background(NovaColors.Background)
    ) {
        // Channel info (number + logo)
        Row(
            modifier = Modifier
                .width(CHANNEL_INFO_WIDTH)
                .fillMaxHeight()
                .background(NovaColors.Surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = channel.number.toString(),
                color = NovaColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.width(28.dp)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NovaColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo != null) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Text(
                        text = channel.name.take(3).uppercase(),
                        color = NovaColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Programs
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            channel.programs.forEach { program ->
                val offsetMinutes = ((program.startTime - startTime) / 60000).toInt()
                    .coerceAtLeast(0)
                val widthMinutes = program.durationMinutes
                    .coerceAtLeast(15) // Min 15 min width

                // Calculate left offset from grid start
                val leftOffset = (offsetMinutes * PIXELS_PER_MINUTE.value).dp
                val width = (widthMinutes * PIXELS_PER_MINUTE.value).dp

                // Pass focus requester to currently airing program
                val programFocusRequester = if (focusRequester != null && program.id == currentProgram?.id) {
                    focusRequester
                } else null

                ProgramBlock(
                    program = program,
                    width = width,
                    isSelected = selectedProgram?.id == program.id,
                    isCurrentlyAiring = program.isCurrentlyAiring(currentTime),
                    isPlaying = isPlaying,
                    focusRequester = programFocusRequester,
                    onFocus = { onProgramSelected(program) },
                    onClick = { onProgramClick(program) }
                )
            }
        }
    }
}

@Composable
private fun ProgramBlock(
    program: EpgProgram,
    width: Dp,
    isSelected: Boolean,
    isCurrentlyAiring: Boolean,
    isPlaying: Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPlaying -> NovaColors.Primary.copy(alpha = 0.4f)
        isSelected || isFocused -> NovaColors.Primary.copy(alpha = 0.3f)
        isCurrentlyAiring -> NovaColors.Surface
        else -> NovaColors.SurfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isPlaying -> NovaColors.Primary
        isSelected || isFocused -> NovaColors.Primary
        else -> NovaColors.Border.copy(alpha = 0.3f)
    }

    val modifier = Modifier
        .width(width)
        .fillMaxHeight()
        .padding(1.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(backgroundColor)
        .border(
            width = if (isSelected || isFocused) 2.dp else 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(4.dp)
        )
        .let { m ->
            if (focusRequester != null) m.focusRequester(focusRequester) else m
        }
        .onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) onFocus()
        }
        .clickable { onClick() }
        .focusable()
        .padding(horizontal = 8.dp, vertical = 4.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Playing indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NovaColors.Primary, RoundedCornerShape(4.dp))
                )
            }
            Text(
                text = program.title,
                color = when {
                    isPlaying -> NovaColors.TextPrimary
                    isSelected || isFocused -> NovaColors.TextPrimary
                    else -> NovaColors.TextSecondary
                },
                fontSize = 13.sp,
                fontWeight = if (isPlaying || isSelected || isFocused) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
