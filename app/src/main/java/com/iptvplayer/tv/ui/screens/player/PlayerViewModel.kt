package com.iptvplayer.tv.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.player.PlayerManager
import com.iptvplayer.tv.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    val player: ExoPlayer?
        get() = playerManager.player

    private var currentChannelId: String? = null

    fun loadChannel(channelId: String) {
        if (currentChannelId == channelId) return
        currentChannelId = channelId

        viewModelScope.launch {
            val channel = repository.getChannelById(channelId)
            if (channel == null) {
                android.util.Log.e("PlayerVM", "Channel not found: $channelId")
                return@launch
            }

            android.util.Log.d("PlayerVM", "Loading channel: ${channel.name}")
            android.util.Log.d("PlayerVM", "Stream URL: ${channel.url}")

            _currentChannel.value = channel

            // Get resume position
            val position = repository.getPlaybackPosition(channelId) ?: 0

            // Start playback
            playerManager.play(
                url = channel.url,
                userAgent = channel.userAgent,
                referer = channel.referer,
                startPosition = position
            )

            // Add to watch history
            repository.addToWatchHistory(channel.playlistId, channelId)
        }
    }

    fun pause() {
        playerManager.pause()
        savePosition()
    }

    fun resume() {
        playerManager.resume()
    }

    fun stop() {
        savePosition()
        playerManager.stop()
    }

    fun seekForward() {
        playerManager.seekForward()
    }

    fun seekBack() {
        playerManager.seekBack()
    }

    fun getCurrentPosition(): Long = playerManager.getCurrentPosition()

    fun getDuration(): Long = playerManager.getDuration()

    fun retry() {
        currentChannel.value?.let { channel ->
            playerManager.play(
                url = channel.url,
                userAgent = channel.userAgent,
                referer = channel.referer
            )
        }
    }

    private fun savePosition() {
        currentChannelId?.let { channelId ->
            val position = playerManager.getCurrentPosition()
            if (position > 0) {
                viewModelScope.launch {
                    repository.updatePlaybackPosition(channelId, position)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        savePosition()
        // Don't release - PlayerManager is singleton, just stop
        playerManager.stop()
    }
}
