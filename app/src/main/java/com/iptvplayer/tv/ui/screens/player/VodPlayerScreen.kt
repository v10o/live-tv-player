package com.iptvplayer.tv.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.iptvplayer.tv.ui.theme.NovaColors
import kotlinx.coroutines.delay

@Composable
fun VodPlayerScreen(
    title: String,
    streamUrl: String,
    onBackPress: () -> Unit,
    viewModel: VodPlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(streamUrl) {
        viewModel.play(streamUrl)
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    BackHandler {
        viewModel.stop()
        onBackPress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (playerState.isPlaying) viewModel.pause() else viewModel.resume()
                            showControls = true
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.seekBack()
                            showControls = true
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.seekForward()
                            showControls = true
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            showControls = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Video Player
        val player = viewModel.player
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
        }

        // Loading indicator
        if (playerState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = NovaColors.Primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading...", color = NovaColors.TextPrimary, fontSize = 16.sp)
                }
            }
        }

        // Error display
        playerState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NovaColors.Surface)
                        .padding(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(NovaColors.Secondary, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Playback Error", color = NovaColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = NovaColors.TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlayerButton(label = "Retry", isPrimary = true) { viewModel.retry() }
                        PlayerButton(label = "Back", isPrimary = false) {
                            viewModel.stop()
                            onBackPress()
                        }
                    }
                }
            }
        }

        // Controls Overlay
        if (showControls && playerState.error == null) {
            VodControlsOverlay(
                title = title,
                isPlaying = playerState.isPlaying,
                currentPosition = viewModel.getCurrentPosition(),
                duration = viewModel.getDuration(),
                onPlayPause = { if (playerState.isPlaying) viewModel.pause() else viewModel.resume() },
                onSeekBack = { viewModel.seekBack() },
                onSeekForward = { viewModel.seekForward() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onBack = {
                    viewModel.stop()
                    onBackPress()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun VodControlsOverlay(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        // Top bar - Title only (no back arrow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("MOVIE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom controls - White, at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Playback controls - White icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek back
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onSeekBack() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play/Pause - Larger white button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onPlayPause() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Seek forward
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onSeekForward() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seekable progress bar
            if (duration > 0) {
                var isSeeking by remember { mutableStateOf(false) }
                var seekPosition by remember { mutableStateOf(currentPosition) }
                var isProgressFocused by remember { mutableStateOf(false) }

                // Update seek position when not seeking
                LaunchedEffect(currentPosition, isSeeking) {
                    if (!isSeeking) {
                        seekPosition = currentPosition
                    }
                }

                val displayPosition = if (isSeeking) seekPosition else currentPosition
                val progress = (displayPosition.toFloat() / duration).coerceIn(0f, 1f)

                // Progress bar container - focusable for D-pad seeking
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isProgressFocused) 12.dp else 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .focusable()
                        .onFocusChanged {
                            isProgressFocused = it.isFocused
                            if (!it.isFocused && isSeeking) {
                                // Apply seek when losing focus
                                onSeekTo(seekPosition)
                                isSeeking = false
                            }
                        }
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && isProgressFocused) {
                                val seekStep = duration / 100  // 1% of total duration
                                val bigSeekStep = duration / 20  // 5% of total duration
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        isSeeking = true
                                        seekPosition = (seekPosition - seekStep).coerceIn(0, duration)
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        isSeeking = true
                                        seekPosition = (seekPosition + seekStep).coerceIn(0, duration)
                                        true
                                    }
                                    Key.DirectionCenter, Key.Enter -> {
                                        if (isSeeking) {
                                            onSeekTo(seekPosition)
                                            isSeeking = false
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                ) {
                    // Progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                if (isProgressFocused) NovaColors.Primary else Color.White,
                                RoundedCornerShape(6.dp)
                            )
                    )

                    // Seek thumb when focused
                    if (isProgressFocused) {
                        Box(
                            modifier = Modifier
                                .offset(x = (progress * 1000).dp - 8.dp)  // Approximate offset
                                .size(16.dp)
                                .align(Alignment.CenterStart)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(displayPosition),
                        color = if (isSeeking) NovaColors.Primary else Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = if (isSeeking) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(formatDuration(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }

                // Seeking hint
                if (isProgressFocused) {
                    Text(
                        text = "← → to seek, OK to confirm",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPrimary) {
                    Brush.linearGradient(colors = listOf(NovaColors.Secondary, NovaColors.Secondary.copy(alpha = 0.8f)))
                } else {
                    Brush.linearGradient(colors = listOf(NovaColors.SurfaceVariant, NovaColors.Surface))
                }
            )
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (isPrimary) Color.White else NovaColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
