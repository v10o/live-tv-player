package com.iptvplayer.tv.ui.screens.epg

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.model.EpgChannel
import com.iptvplayer.tv.data.model.EpgProgram
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.SettingsRepository
import com.iptvplayer.tv.player.PlayerManager
import com.iptvplayer.tv.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpgUiState())
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val player: ExoPlayer?
        get() = playerManager.player

    private var allChannels: List<Channel> = emptyList()

    // Persist selection across process death
    private var savedCategory: String?
        get() = savedStateHandle["selectedCategory"]
        set(value) { savedStateHandle["selectedCategory"] = value }

    private var savedChannelId: String?
        get() = savedStateHandle["selectedChannelId"]
        set(value) { savedStateHandle["selectedChannelId"] = value }

    private var savedProgramId: String?
        get() = savedStateHandle["selectedProgramId"]
        set(value) { savedStateHandle["selectedProgramId"] = value }

    init {
        // Update current time every minute
        viewModelScope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(
                    currentTime = System.currentTimeMillis()
                )
                delay(60_000)
            }
        }
    }

    fun loadEpg(playlistId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load channels from playlist
            playlistRepository.getChannelsByPlaylist(playlistId).collect { channels ->
                allChannels = channels

                // Get hidden groups
                settingsRepository.getHiddenGroups(playlistId).collect { hiddenGroups ->
                    val visibleChannels = channels.filter { channel ->
                        val group = channel.group ?: "Uncategorized"
                        !hiddenGroups.contains(group)
                    }

                    // Extract unique categories from groups (no limit)
                    val categories = listOf("All", "Recent", "Favorites") +
                            visibleChannels.mapNotNull { it.group }.distinct()

                    // Restore saved category or default to "All"
                    val restoredCategory = savedCategory ?: "All"

                    // Filter channels based on restored category
                    val filteredChannels = when (restoredCategory) {
                        "All" -> visibleChannels
                        "Recent" -> visibleChannels.take(10)
                        "Favorites" -> emptyList()
                        else -> visibleChannels.filter { it.group == restoredCategory }
                    }

                    // Generate mock EPG data
                    val epgChannels = generateMockEpg(filteredChannels)
                    val now = System.currentTimeMillis()

                    // Restore selection - prefer currently airing program
                    val restoredProgram = savedChannelId?.let { channelId ->
                        val channel = epgChannels.find { it.id == channelId }
                        channel?.programs?.find { it.isCurrentlyAiring(now) }
                            ?: channel?.programs?.firstOrNull()
                    } ?: epgChannels.firstOrNull()?.programs?.find { it.isCurrentlyAiring(now) }
                    ?: epgChannels.firstOrNull()?.programs?.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        channels = epgChannels,
                        categories = categories,
                        selectedCategory = restoredCategory,
                        isLoading = false,
                        selectedProgram = restoredProgram
                    )

                    // Auto-play the initially selected channel
                    restoredProgram?.let { program ->
                        val channel = epgChannels.find { it.id == program.channelId }
                        channel?.url?.let { url ->
                            playChannel(url)
                        }
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) {
        // Save to persisted state
        savedCategory = category
        _uiState.value = _uiState.value.copy(selectedCategory = category)

        // Filter channels by category
        viewModelScope.launch {
            val filtered = when (category) {
                "All" -> allChannels
                "Recent" -> allChannels.take(10)
                "Favorites" -> emptyList() // TODO: Integrate with favorites
                else -> allChannels.filter { it.group == category }
            }

            val epgChannels = generateMockEpg(filtered)
            val now = System.currentTimeMillis()

            // Select currently airing program on first channel
            val currentProgram = epgChannels.firstOrNull()?.programs?.find {
                it.isCurrentlyAiring(now)
            } ?: epgChannels.firstOrNull()?.programs?.firstOrNull()

            // Save channel selection
            savedChannelId = epgChannels.firstOrNull()?.id

            _uiState.value = _uiState.value.copy(
                channels = epgChannels,
                selectedProgram = currentProgram
            )

            // Auto-play the first channel in new category
            currentProgram?.let { program ->
                val channel = epgChannels.find { it.id == program.channelId }
                channel?.url?.let { url ->
                    playChannel(url)
                }
            }
        }
    }

    private var playbackJob: kotlinx.coroutines.Job? = null
    private var currentPlayingUrl: String? = null

    fun selectProgram(program: EpgProgram) {
        // Save channel ID for restoration
        savedChannelId = program.channelId
        savedProgramId = program.id
        _uiState.value = _uiState.value.copy(selectedProgram = program)

        // Cancel previous playback request
        playbackJob?.cancel()

        // Debounce: wait 300ms before playing to avoid rapid switching
        val channel = _uiState.value.channels.find { it.id == program.channelId }
        channel?.url?.let { url ->
            playbackJob = viewModelScope.launch {
                delay(300)
                playChannel(url)
            }
        }
    }

    fun playChannel(url: String) {
        if (url == currentPlayingUrl) return
        currentPlayingUrl = url
        playerManager.play(url)
    }

    fun stopPlayback() {
        playerManager.stop()
        currentPlayingUrl = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
    }

    /**
     * Generate mock EPG data for testing UI
     * TODO: Replace with actual EPG API integration
     */
    private fun generateMockEpg(channels: List<Channel>): List<EpgChannel> {
        val now = System.currentTimeMillis()
        val gridStart = now - (now % (30 * 60 * 1000)) // Round to nearest 30 min

        val mockPrograms = listOf(
            "News at Noon" to 30,
            "Morning Show" to 60,
            "The Daily Report" to 30,
            "Sports Center" to 60,
            "Movie: Action Hero" to 120,
            "Sitcom Reruns" to 30,
            "Reality TV" to 60,
            "Documentary" to 90,
            "Late Night Talk" to 60,
            "Infomercials" to 30
        )

        return channels.take(50).mapIndexed { index, channel ->
            val programs = mutableListOf<EpgProgram>()
            var currentStart = gridStart - (60 * 60 * 1000) // Start 1 hour before grid

            // Generate 4 hours of programs
            while (currentStart < gridStart + (4 * 60 * 60 * 1000)) {
                val (title, duration) = mockPrograms.random()
                val durationMs = duration * 60 * 1000L

                programs.add(
                    EpgProgram(
                        id = UUID.randomUUID().toString(),
                        channelId = channel.id,
                        title = title,
                        description = "This is a sample program description for $title. " +
                                "Tune in to watch this exciting content.",
                        startTime = currentStart,
                        endTime = currentStart + durationMs,
                        category = channel.group,
                        episodeInfo = if (Math.random() > 0.5) "S${(1..10).random()} E${(1..20).random()}" else null
                    )
                )

                currentStart += durationMs
            }

            EpgChannel(
                id = channel.id,
                number = 100 + index,
                name = channel.name,
                logo = channel.logo,
                url = channel.url,
                programs = programs
            )
        }
    }
}

data class EpgUiState(
    val channels: List<EpgChannel> = emptyList(),
    val categories: List<String> = listOf("All", "Recent", "Favorites"),
    val selectedCategory: String = "All",
    val selectedProgram: EpgProgram? = null,
    val currentTime: Long = System.currentTimeMillis(),
    val gridStartTime: Long = System.currentTimeMillis() - (System.currentTimeMillis() % (30 * 60 * 1000)),
    val isLoading: Boolean = false
)
