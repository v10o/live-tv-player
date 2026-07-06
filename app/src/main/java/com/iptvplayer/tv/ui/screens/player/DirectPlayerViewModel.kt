package com.iptvplayer.tv.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.tv.player.PlayerManager
import com.iptvplayer.tv.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DirectPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    val player: ExoPlayer?
        get() = playerManager.player

    fun playUrl(url: String) {
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
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
