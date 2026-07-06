package com.iptvplayer.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val playlists = playlistRepository.getAllPlaylists()

    fun loadGroupsForPlaylist(playlistId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedPlaylistId = playlistId,
                isLoadingGroups = true
            )

            val groups = playlistRepository.getGroupsByPlaylist(playlistId)

            // Combine with hidden groups flow
            settingsRepository.getHiddenGroups(playlistId).collect { hiddenGroups ->
                _uiState.value = _uiState.value.copy(
                    allGroups = groups,
                    hiddenGroups = hiddenGroups,
                    isLoadingGroups = false
                )
            }
        }
    }

    fun toggleGroup(group: String) {
        val playlistId = _uiState.value.selectedPlaylistId ?: return
        viewModelScope.launch {
            val currentHidden = _uiState.value.hiddenGroups.toMutableSet()
            if (currentHidden.contains(group)) {
                currentHidden.remove(group)
            } else {
                currentHidden.add(group)
            }
            settingsRepository.setHiddenGroups(playlistId, currentHidden)
        }
    }

    fun selectAll() {
        val playlistId = _uiState.value.selectedPlaylistId ?: return
        viewModelScope.launch {
            settingsRepository.showAllGroups(playlistId)
        }
    }

    fun unselectAll() {
        val playlistId = _uiState.value.selectedPlaylistId ?: return
        viewModelScope.launch {
            settingsRepository.hideAllGroups(playlistId, _uiState.value.allGroups)
        }
    }

    fun clearGroupSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPlaylistId = null,
            allGroups = emptyList(),
            hiddenGroups = emptySet()
        )
    }
}

data class SettingsUiState(
    val selectedPlaylistId: String? = null,
    val allGroups: List<String> = emptyList(),
    val hiddenGroups: Set<String> = emptySet(),
    val isLoadingGroups: Boolean = false
)
