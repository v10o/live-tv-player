package com.iptvplayer.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.Surface
import com.iptvplayer.tv.navigation.AppNavigation
import com.iptvplayer.tv.player.PlayerManager
import com.iptvplayer.tv.ui.theme.IPTVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPTVPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Pause playback when app goes to background
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release player resources
        playerManager.release()
    }
}
