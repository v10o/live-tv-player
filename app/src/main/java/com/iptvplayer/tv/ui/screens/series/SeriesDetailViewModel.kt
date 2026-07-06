package com.iptvplayer.tv.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.api.XtreamApiService
import com.iptvplayer.tv.data.model.*
import com.iptvplayer.tv.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val xtreamApiService: XtreamApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    private var currentPlaylist: Playlist? = null

    fun loadSeriesDetail(playlistId: String, seriesId: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            currentPlaylist = playlistRepository.getPlaylistById(playlistId)
            val playlist = currentPlaylist
            if (playlist == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Playlist not found"
                )
                return@launch
            }

            val credentials = XtreamCredentials(
                serverUrl = playlist.serverUrl ?: "",
                username = playlist.username ?: "",
                password = playlist.password ?: ""
            )

            val result = xtreamApiService.getSeriesInfo(credentials, seriesId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load series"
                )
                return@launch
            }

            val seriesInfo = result.getOrNull()
            if (seriesInfo == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No series info"
                )
                return@launch
            }

            val info = seriesInfo.info
            val seasons = seriesInfo.seasons.sortedBy { it.seasonNumber }
            val allEpisodes = seriesInfo.episodes.flatMap { (seasonNum, episodes) ->
                episodes.map { episode ->
                    Episode(
                        id = episode.id,
                        episodeNum = episode.episodeNum,
                        title = episode.title,
                        season = episode.season ?: seasonNum.toIntOrNull() ?: 1,
                        containerExtension = episode.containerExtension ?: "mkv",
                        plot = episode.info?.plot,
                        image = episode.info?.movieImage,
                        duration = episode.info?.duration,
                        rating = episode.info?.rating
                    )
                }
            }.sortedWith(compareBy({ it.season }, { it.episodeNum }))

            val seasonNumbers = seasons.map { it.seasonNumber }.ifEmpty {
                allEpisodes.map { it.season }.distinct().sorted()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                seriesName = info?.name ?: "",
                plot = info?.plot,
                cover = info?.cover,
                backdrop = info?.backdropPath?.filterNotNull()?.firstOrNull(),
                genre = info?.genre,
                rating = info?.rating,
                cast = info?.cast,
                director = info?.director,
                releaseDate = info?.releaseDate,
                seasons = seasonNumbers,
                episodes = allEpisodes,
                selectedSeason = seasonNumbers.firstOrNull() ?: 1
            )
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun getEpisodeStreamUrl(episode: Episode): String? {
        val playlist = currentPlaylist ?: return null
        val credentials = XtreamCredentials(
            serverUrl = playlist.serverUrl ?: "",
            username = playlist.username ?: "",
            password = playlist.password ?: ""
        )
        return XtreamApiService.buildSeriesStreamUrl(
            credentials,
            episode.id.toIntOrNull() ?: return null,
            episode.containerExtension
        )
    }
}

data class SeriesDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val seriesName: String = "",
    val plot: String? = null,
    val cover: String? = null,
    val backdrop: String? = null,
    val genre: String? = null,
    val rating: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val releaseDate: String? = null,
    val seasons: List<Int> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val selectedSeason: Int = 1
) {
    val currentSeasonEpisodes: List<Episode>
        get() = episodes.filter { it.season == selectedSeason }
}

data class Episode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val season: Int,
    val containerExtension: String,
    val plot: String? = null,
    val image: String? = null,
    val duration: String? = null,
    val rating: Double? = null
)
