package com.iptvplayer.tv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPlaylistState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

data class PlaylistOptionsState(
    val selectedPlaylist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

data class DashboardState(
    val heroItems: List<Any> = emptyList(),
    val topMovies: List<VodItem> = emptyList(),
    val topSeries: List<SeriesItem> = emptyList(),
    val recentlyAdded: List<Any> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    val playlists = repository.getAllPlaylists()

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    init {
        loadDashboardContent()
    }

    private fun loadDashboardContent() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState(isLoading = true)

            try {
                val heroItems = vodRepository.getHeroItems(5)
                val topMovies = vodRepository.getTopRatedMovies(20)
                val topSeries = vodRepository.getTopRatedSeries(20)
                val recentlyAdded = vodRepository.getRecentlyAddedContent(20)

                _dashboardState.value = DashboardState(
                    heroItems = heroItems,
                    topMovies = topMovies,
                    topSeries = topSeries,
                    recentlyAdded = recentlyAdded,
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Failed to load dashboard: ${e.message}")
                _dashboardState.value = DashboardState(isLoading = false)
            }
        }
    }

    fun refreshDashboard() {
        loadDashboardContent()
    }

    private val _addPlaylistState = MutableStateFlow(AddPlaylistState())
    val addPlaylistState: StateFlow<AddPlaylistState> = _addPlaylistState.asStateFlow()

    private val _playlistOptionsState = MutableStateFlow(PlaylistOptionsState())
    val playlistOptionsState: StateFlow<PlaylistOptionsState> = _playlistOptionsState.asStateFlow()

    fun addXtreamPlaylist(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _addPlaylistState.value = AddPlaylistState(isLoading = true)

            val result = repository.addXtreamPlaylist(name, serverUrl, username, password)

            result.fold(
                onSuccess = { playlist ->
                    _addPlaylistState.value = AddPlaylistState(
                        success = "Added '${playlist.name}' with ${playlist.channelsCount} channels"
                    )
                },
                onFailure = { error ->
                    _addPlaylistState.value = AddPlaylistState(
                        error = error.message ?: "Failed to add playlist"
                    )
                }
            )
        }
    }

    fun addM3uPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _addPlaylistState.value = AddPlaylistState(isLoading = true)

            val result = repository.addM3uUrlPlaylist(name, url)

            result.fold(
                onSuccess = { playlist ->
                    _addPlaylistState.value = AddPlaylistState(
                        success = "Added '${playlist.name}' with ${playlist.channelsCount} channels"
                    )
                },
                onFailure = { error ->
                    _addPlaylistState.value = AddPlaylistState(
                        error = error.message ?: "Failed to parse M3U"
                    )
                }
            )
        }
    }

    fun clearAddPlaylistState() {
        _addPlaylistState.value = AddPlaylistState()
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _playlistOptionsState.value = PlaylistOptionsState()
        }
    }

    fun showPlaylistOptions(playlist: Playlist) {
        _playlistOptionsState.value = PlaylistOptionsState(selectedPlaylist = playlist)
    }

    fun hidePlaylistOptions() {
        _playlistOptionsState.value = PlaylistOptionsState()
    }

    fun editPlaylist(
        playlistId: String,
        name: String,
        serverUrl: String?,
        username: String?,
        password: String?,
        m3uUrl: String?
    ) {
        viewModelScope.launch {
            _playlistOptionsState.value = _playlistOptionsState.value.copy(isLoading = true, error = null)

            val result = repository.updatePlaylist(playlistId, name, serverUrl, username, password, m3uUrl)

            result.fold(
                onSuccess = { updated ->
                    _playlistOptionsState.value = _playlistOptionsState.value.copy(
                        selectedPlaylist = updated,
                        isLoading = false,
                        success = "Playlist updated successfully"
                    )
                },
                onFailure = { error ->
                    _playlistOptionsState.value = _playlistOptionsState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update playlist"
                    )
                }
            )
        }
    }

    fun refreshPlaylist(playlistId: String) {
        viewModelScope.launch {
            _playlistOptionsState.value = _playlistOptionsState.value.copy(isLoading = true, error = null, success = null)

            val result = repository.refreshPlaylist(playlistId)

            result.fold(
                onSuccess = { updated ->
                    _playlistOptionsState.value = _playlistOptionsState.value.copy(
                        selectedPlaylist = updated,
                        isLoading = false,
                        success = "Refreshed: ${updated.channelsCount} channels"
                    )
                },
                onFailure = { error ->
                    _playlistOptionsState.value = _playlistOptionsState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to refresh playlist"
                    )
                }
            )
        }
    }
}
