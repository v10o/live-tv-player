package com.iptvplayer.tv.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.tv.player.PlayerManager
import com.iptvplayer.tv.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VodPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    val player: ExoPlayer?
        get() = playerManager.player

    private var currentUrl: String? = null

    fun play(url: String) {
        if (currentUrl == url) return
        currentUrl = url

        android.util.Log.d("VodPlayerVM", "Playing VOD: $url")
        playerManager.play(url = url)
    }

    fun pause() {
        playerManager.pause()
    }

    fun resume() {
        playerManager.resume()
    }

    fun stop() {
        playerManager.stop()
        currentUrl = null
    }

    fun seekForward() {
        playerManager.seekForward()
    }

    fun seekBack() {
        playerManager.seekBack()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun getCurrentPosition(): Long = playerManager.getCurrentPosition()

    fun getDuration(): Long = playerManager.getDuration()

    fun retry() {
        currentUrl?.let { url ->
            playerManager.play(url = url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
    }
}
