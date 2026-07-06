package com.iptvplayer.tv.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private var allChannels: List<Channel> = emptyList()
    private var currentPlaylistId: String = ""
    private var hiddenGroups: Set<String> = emptySet()

    fun loadPlaylist(playlistId: String) {
        // Skip reload if same playlist already loaded
        if (currentPlaylistId == playlistId && allChannels.isNotEmpty()) {
            android.util.Log.d("ChannelsVM", "Playlist already loaded, skipping reload")
            return
        }

        currentPlaylistId = playlistId
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val playlist = repository.getPlaylistById(playlistId)
            _uiState.value = _uiState.value.copy(
                playlistName = playlist?.name ?: "Playlist"
            )

            // Load hidden groups setting
            settingsRepository.getHiddenGroups(playlistId).collect { hidden ->
                hiddenGroups = hidden
                applyFilters()
            }
        }

        // Load all groups
        viewModelScope.launch {
            val groups = repository.getGroupsByPlaylist(playlistId)
            _uiState.value = _uiState.value.copy(allGroups = groups)
        }

        // Load channels
        viewModelScope.launch {
            repository.getChannelsByPlaylist(playlistId).collect { channels ->
                allChannels = channels
                applyFilters()
            }
        }

        // Load favorites
        viewModelScope.launch {
            repository.getFavoritesByPlaylist(playlistId).collect { favorites ->
                _uiState.value = _uiState.value.copy(
                    favoriteIds = favorites.map { it.channelId }.toSet()
                )
            }
        }
    }

    private fun applyFilters() {
        // Filter out hidden groups
        val visibleChannels = allChannels.filter { channel ->
            val group = channel.group ?: "Uncategorized"
            !hiddenGroups.contains(group)
        }

        // Get visible groups (for display)
        val visibleGroups = _uiState.value.allGroups.filter { !hiddenGroups.contains(it) }

        // Apply selected group filter if any
        val filteredChannels = if (_uiState.value.selectedGroup != null) {
            visibleChannels.filter { it.group == _uiState.value.selectedGroup }
        } else {
            visibleChannels
        }

        _uiState.value = _uiState.value.copy(
            channels = filteredChannels,
            groups = visibleGroups,
            isLoading = false
        )
    }

    fun selectGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)

        viewModelScope.launch {
            val channels = if (group == null) {
                allChannels
            } else {
                repository.getChannelsByGroup(currentPlaylistId, group)
            }
            _uiState.value = _uiState.value.copy(channels = channels)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val channels = if (query.isBlank()) {
                if (_uiState.value.selectedGroup != null) {
                    repository.getChannelsByGroup(currentPlaylistId, _uiState.value.selectedGroup!!)
                } else {
                    allChannels
                }
            } else {
                repository.searchChannels(currentPlaylistId, query)
            }
            _uiState.value = _uiState.value.copy(channels = channels)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(currentPlaylistId, channelId)
        }
    }

    fun setSelectedChannel(channelId: String) {
        _uiState.value = _uiState.value.copy(selectedChannelId = channelId)
    }

    fun getSelectedCategoryIndex(): Int {
        val group = _uiState.value.selectedGroup ?: return 0
        val index = _uiState.value.groups.indexOf(group)
        return if (index >= 0) index + 1 else 0  // +1 because "All" is at index 0
    }
}

data class ChannelsUiState(
    val playlistName: String = "",
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),        // Visible groups only
    val allGroups: List<String> = emptyList(),     // All groups (for settings)
    val selectedGroup: String? = null,
    val selectedChannelId: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
)
