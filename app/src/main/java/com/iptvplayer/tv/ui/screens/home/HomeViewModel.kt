package com.iptvplayer.tv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.api.TmdbApiService
import com.iptvplayer.tv.data.api.TmdbItem
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import com.iptvplayer.tv.data.repository.WatchlistRepository
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
    val watchlist: List<WatchlistItem> = emptyList(),
    val trendingMovies: List<TmdbItem> = emptyList(),
    val trendingShows: List<TmdbItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val vodRepository: VodRepository,
    private val watchlistRepository: WatchlistRepository,
    private val tmdbApiService: TmdbApiService
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
                val watchlist = watchlistRepository.getAllWatchlistOnce()

                // Fetch TMDB trending content
                android.util.Log.d("HomeVM", "Fetching TMDB trending content...")
                val trendingMoviesResult = tmdbApiService.getTrendingMovies(10)
                val trendingShowsResult = tmdbApiService.getTrendingShows(10)
                val trendingMovies = trendingMoviesResult.getOrNull() ?: emptyList()
                val trendingShows = trendingShowsResult.getOrNull() ?: emptyList()
                android.util.Log.d("HomeVM", "TMDB trending - Movies: ${trendingMovies.size}, Shows: ${trendingShows.size}")
                if (trendingMoviesResult.isFailure) {
                    android.util.Log.e("HomeVM", "TMDB movies failed: ${trendingMoviesResult.exceptionOrNull()?.message}")
                }
                if (trendingShowsResult.isFailure) {
                    android.util.Log.e("HomeVM", "TMDB shows failed: ${trendingShowsResult.exceptionOrNull()?.message}")
                }

                _dashboardState.value = DashboardState(
                    heroItems = heroItems,
                    topMovies = topMovies,
                    topSeries = topSeries,
                    recentlyAdded = recentlyAdded,
                    watchlist = watchlist,
                    trendingMovies = trendingMovies,
                    trendingShows = trendingShows,
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Failed to load dashboard: ${e.message}")
                _dashboardState.value = DashboardState(isLoading = false)
            }
        }
    }

    fun getTmdbPosterUrl(path: String?): String? = tmdbApiService.getPosterUrl(path)

    /**
     * Find matching content in user's playlists by TMDB title
     * Returns Pair(type, id) where type is "vod" or "series"
     */
    suspend fun findContentByTitle(title: String, mediaType: String): Pair<String, String>? {
        return if (mediaType == "movie") {
            val vod = vodRepository.findVodByTitle(title)
            vod?.let { "vod" to it.id }
        } else {
            val series = vodRepository.findSeriesByTitle(title)
            series?.let { "series" to "${it.playlistId}/${it.seriesId}" }
        }
    }

    /**
     * Find ALL matching movies in user's playlists by TMDB title
     */
    suspend fun findAllMoviesByTitle(title: String): List<VodItem> {
        return vodRepository.findAllVodByTitle(title)
    }

    suspend fun getVodById(id: String): VodItem? = vodRepository.getVodById(id)

    suspend fun getSeriesById(playlistId: String, seriesId: Int): SeriesItem? {
        // Search by seriesId across all series
        return vodRepository.findSeriesBySeriesId(seriesId)
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
