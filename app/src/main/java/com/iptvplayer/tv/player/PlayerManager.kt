package com.iptvplayer.tv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _player: ExoPlayer? = null
    val player: ExoPlayer?
        get() = _player

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Track current playback for retry logic
    private var currentUrl: String? = null
    private var currentUserAgent: String? = null
    private var currentReferer: String? = null
    private var currentStartPosition: Long = 0
    private var retryCount = 0
    private var triedFormats = mutableSetOf<String>()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when(playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            android.util.Log.d("PlayerManager", "Playback state: $stateName")

            _playerState.value = _playerState.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING,
                isPlaying = playbackState == Player.STATE_READY && _player?.isPlaying == true,
                isEnded = playbackState == Player.STATE_ENDED
            )

            // Reset retry count on successful playback
            if (playbackState == Player.STATE_READY) {
                retryCount = 0
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d("PlayerManager", "isPlaying: $isPlaying")
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("PlayerManager", "Player error: ${error.errorCode} - ${error.message}", error)

            // Try alternate format before showing error
            if (tryAlternateFormat()) {
                return
            }

            _playerState.value = _playerState.value.copy(
                error = error.message ?: "Playback error (${error.errorCode})",
                isLoading = false,
                isPlaying = false
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun initialize() {
        if (_player == null) {
            android.util.Log.d("PlayerManager", "Creating new ExoPlayer instance")

            // Force software decoding to avoid green screen on emulator
            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .forceEnableMediaCodecAsynchronousQueueing()
                .setEnableDecoderFallback(true)

            _player = ExoPlayer.Builder(context, renderersFactory)
                .setSeekForwardIncrementMs(10_000)
                .setSeekBackIncrementMs(10_000)
                .build()
                .also {
                    it.addListener(playerListener)
                    // Disable video scaling for better compatibility
                    it.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
        }
    }

    @OptIn(UnstableApi::class)
    fun play(
        url: String,
        userAgent: String? = null,
        referer: String? = null,
        startPosition: Long = 0
    ) {
        android.util.Log.d("PlayerManager", "play() called with URL: $url")

        // Skip if already playing the same URL
        if (url == currentUrl && _player?.isPlaying == true) {
            android.util.Log.d("PlayerManager", "Already playing this URL, skipping")
            return
        }

        // Store for retry
        currentUrl = url
        currentUserAgent = userAgent
        currentReferer = referer
        currentStartPosition = startPosition
        retryCount = 0
        triedFormats.clear()
        triedFormats.add(getFormatFromUrl(url))

        playInternal(url, userAgent, referer, startPosition)
    }

    @OptIn(UnstableApi::class)
    private fun playInternal(
        url: String,
        userAgent: String?,
        referer: String?,
        startPosition: Long
    ) {
        initialize()

        _playerState.value = PlayerState(isLoading = true, currentUrl = url)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setUserAgent(userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            setAllowCrossProtocolRedirects(true)
            setConnectTimeoutMs(15_000)
            setReadTimeoutMs(15_000)
            if (!referer.isNullOrEmpty()) {
                setDefaultRequestProperties(mapOf("Referer" to referer))
            }
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSource = createMediaSource(url, dataSourceFactory)

        android.util.Log.d("PlayerManager", "Stream type: ${detectStreamType(url)}, URL: $url")

        _player?.apply {
            // Stop current playback first to avoid overlap
            stop()
            clearMediaItems()
            setMediaSource(mediaSource)
            prepare()
            if (startPosition > 0) {
                seekTo(startPosition)
            }
            playWhenReady = true
        }
    }

    private fun tryAlternateFormat(): Boolean {
        val url = currentUrl ?: return false

        // Only retry for Xtream-style URLs that have format extension
        if (!url.contains("/live/") || retryCount >= 2) {
            return false
        }

        val alternateUrl = getAlternateFormatUrl(url)
        val alternateFormat = getFormatFromUrl(alternateUrl)

        if (triedFormats.contains(alternateFormat)) {
            return false
        }

        android.util.Log.d("PlayerManager", "Trying alternate format: $alternateUrl")
        triedFormats.add(alternateFormat)
        retryCount++

        playInternal(alternateUrl, currentUserAgent, currentReferer, currentStartPosition)
        return true
    }

    private fun getAlternateFormatUrl(url: String): String {
        return when {
            url.endsWith(".m3u8") -> url.replace(".m3u8", ".ts")
            url.endsWith(".ts") -> url.replace(".ts", ".m3u8")
            else -> url
        }
    }

    private fun getFormatFromUrl(url: String): String {
        return when {
            url.endsWith(".m3u8") -> "m3u8"
            url.endsWith(".ts") -> "ts"
            else -> "unknown"
        }
    }

    @OptIn(UnstableApi::class)
    private fun createMediaSource(url: String, dataSourceFactory: DefaultDataSource.Factory): MediaSource {
        val mediaItem = MediaItem.fromUri(url)
        val streamType = detectStreamType(url)

        return when (streamType) {
            StreamType.HLS -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            StreamType.TS, StreamType.OTHER -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    private fun detectStreamType(url: String): StreamType {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains(".m3u8") || lowerUrl.contains("format=m3u8") -> StreamType.HLS
            lowerUrl.contains(".ts") || lowerUrl.contains("format=ts") -> StreamType.TS
            // VOD progressive formats
            lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") ||
            lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov") ||
            lowerUrl.endsWith(".wmv") || lowerUrl.endsWith(".flv") ||
            lowerUrl.endsWith(".webm") -> StreamType.OTHER
            // Xtream VOD paths use /movie/ - always progressive
            lowerUrl.contains("/movie/") || lowerUrl.contains("/series/") -> StreamType.OTHER
            // Live streams default to HLS
            lowerUrl.contains("/live/") -> StreamType.HLS
            // Unknown - try progressive first (safer)
            else -> StreamType.OTHER
        }
    }

    fun pause() {
        _player?.pause()
    }

    fun resume() {
        _player?.play()
    }

    fun stop() {
        _player?.stop()
        _playerState.value = PlayerState()
        currentUrl = null
        retryCount = 0
        triedFormats.clear()
    }

    fun seekTo(position: Long) {
        _player?.seekTo(position)
    }

    fun seekForward() {
        _player?.seekForward()
    }

    fun seekBack() {
        _player?.seekBack()
    }

    fun setVolume(volume: Float) {
        _player?.volume = volume.coerceIn(0f, 1f)
    }

    fun getCurrentPosition(): Long = _player?.currentPosition ?: 0

    fun getDuration(): Long = _player?.duration ?: 0

    fun release() {
        _player?.apply {
            removeListener(playerListener)
            release()
        }
        _player = null
        _playerState.value = PlayerState()
        currentUrl = null
    }
}

data class PlayerState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val error: String? = null,
    val currentUrl: String? = null
)

enum class StreamType {
    HLS, TS, OTHER
}
