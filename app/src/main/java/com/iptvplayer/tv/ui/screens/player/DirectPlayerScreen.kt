package com.iptvplayer.tv.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.iptvplayer.tv.ui.theme.NovaColors

@Composable
fun DirectPlayerScreen(
    onBackPress: () -> Unit,
    viewModel: DirectPlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    var showUrlInput by remember { mutableStateOf(true) }
    var streamUrl by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        viewModel.stop()
        onBackPress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player (shown when playing)
        if (!showUrlInput) {
            viewModel.player?.let { player ->
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
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
                                    Key.Back, Key.Escape -> {
                                        showUrlInput = true
                                        viewModel.stop()
                                        true
                                    }
                                    else -> {
                                        showControls = true
                                        false
                                    }
                                }
                            } else false
                        }
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

            // Loading
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
                        Text("Loading stream...", color = NovaColors.TextPrimary, fontSize = 16.sp)
                    }
                }
            }

            // Error
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
                            ActionButton("Retry", true) { viewModel.playUrl(streamUrl) }
                            ActionButton("Change URL", false) { showUrlInput = true; viewModel.stop() }
                        }
                    }
                }
            }

            // Controls overlay
            if (showControls && playerState.error == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NovaColors.Surface.copy(alpha = 0.8f))
                                .clickable { showUrlInput = true; viewModel.stop() }
                                .focusable(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("←", fontSize = 20.sp, color = NovaColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Direct Stream", color = NovaColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    // Center play/pause
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark)
                                )
                            )
                            .clickable { if (playerState.isPlaying) viewModel.pause() else viewModel.resume() }
                            .focusable(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Auto-hide controls
                LaunchedEffect(showControls) {
                    if (showControls) {
                        kotlinx.coroutines.delay(4000)
                        showControls = false
                    }
                }
            }
        }

        // URL Input Screen
        if (showUrlInput) {
            UrlInputScreen(
                currentUrl = streamUrl,
                onUrlChange = { streamUrl = it },
                onPlay = {
                    if (streamUrl.isNotBlank()) {
                        showUrlInput = false
                        viewModel.playUrl(streamUrl)
                    }
                },
                onBack = onBackPress
            )
        }
    }
}

@Composable
private fun UrlInputScreen(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NovaColors.Surface)
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🎬", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Test Stream URL",
                color = NovaColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter any HLS (.m3u8) or MPEG-TS (.ts) URL",
                color = NovaColors.TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // URL Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Stream URL", color = NovaColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NovaColors.Background)
                        .then(
                            if (isFocused) Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(NovaColors.Primary.copy(alpha = 0.1f), NovaColors.Background)
                                )
                            ) else Modifier
                        )
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = currentUrl,
                        onValueChange = onUrlChange,
                        textStyle = TextStyle(color = NovaColors.TextPrimary, fontSize = 16.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (currentUrl.isEmpty()) {
                                    Text(
                                        "http://example.com/stream.m3u8",
                                        color = NovaColors.TextMuted,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sample URLs
            Text("Quick test URLs:", color = NovaColors.TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SampleUrlChip("Big Buck Bunny") {
                    onUrlChange("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                }
                SampleUrlChip("Sintel") {
                    onUrlChange("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ActionButton("Back", false, onBack)
                Spacer(modifier = Modifier.width(12.dp))
                ActionButton("Play Stream", true, onPlay)
            }
        }
    }
}

@Composable
private fun SampleUrlChip(label: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) NovaColors.Primary.copy(alpha = 0.2f) else NovaColors.SurfaceVariant)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = NovaColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ActionButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPrimary) {
                    Brush.linearGradient(colors = listOf(NovaColors.Primary, NovaColors.PrimaryDark))
                } else {
                    Brush.linearGradient(colors = listOf(NovaColors.SurfaceVariant, NovaColors.SurfaceVariant))
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = if (isPrimary) NovaColors.OnPrimary else NovaColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
