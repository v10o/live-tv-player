package com.iptvplayer.tv.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.api.XtreamApiService
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.model.XtreamCredentials
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import com.iptvplayer.tv.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VodDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val vodRepository: VodRepository,
    private val watchlistRepository: WatchlistRepository,
    private val xtreamApiService: XtreamApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(VodDetailUiState())
    val uiState: StateFlow<VodDetailUiState> = _uiState.asStateFlow()

    private var currentVodItem: VodItem? = null
    private var allMatchingItems: List<VodItem> = emptyList()

    fun loadVodDetail(vodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val vodItem = vodRepository.getVodById(vodId)
                if (vodItem == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Movie not found"
                    )
                    return@launch
                }

                currentVodItem = vodItem
                allMatchingItems = listOf(vodItem)

                // Find other versions of same movie by title
                // Clean title first (remove common prefixes/suffixes like year, quality)
                val cleanTitle = cleanMovieTitle(vodItem.name)
                val allMatches = vodRepository.findAllVodByTitle(cleanTitle)
                    .ifEmpty { vodRepository.findAllVodByTitle(vodItem.name) }
                if (allMatches.size > 1) {
                    allMatchingItems = allMatches
                } else if (allMatches.size == 1 && allMatches.first().id != vodItem.id) {
                    // Found different movie with same title, include both
                    allMatchingItems = listOf(vodItem) + allMatches
                }

                val isInWatchlist = watchlistRepository.isInWatchlist(vodId)
                val alternateLinks = buildAlternateLinks(allMatchingItems)

                // Set basic info first
                _uiState.value = VodDetailUiState(
                    vodItem = vodItem,
                    title = vodItem.name,
                    poster = vodItem.icon,
                    rating = vodItem.rating,
                    category = vodItem.categoryName,
                    isInWatchlist = isInWatchlist,
                    isLoading = false,
                    alternateLinks = alternateLinks
                )

                // Fetch detailed VOD info from API
                fetchVodDetails(vodItem)

                // Observe watchlist changes
                watchlistRepository.isInWatchlistFlow(vodId).collect { inWatchlist ->
                    _uiState.value = _uiState.value.copy(isInWatchlist = inWatchlist)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load movie details"
                )
            }
        }
    }

    fun loadByTitle(title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                if (title.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid movie title"
                    )
                    return@launch
                }

                // Try clean title first, fall back to original
                val cleanTitle = cleanMovieTitle(title)
                val allMatches = vodRepository.findAllVodByTitle(cleanTitle)
                    .ifEmpty { vodRepository.findAllVodByTitle(title) }
                if (allMatches.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No movies found matching \"$title\""
                    )
                    return@launch
                }

                val primaryItem = allMatches.first()
                currentVodItem = primaryItem
                allMatchingItems = allMatches

                val isInWatchlist = watchlistRepository.isInWatchlist(primaryItem.id)
                val alternateLinks = buildAlternateLinks(allMatches)

                _uiState.value = VodDetailUiState(
                    vodItem = primaryItem,
                    title = primaryItem.name,
                    poster = primaryItem.icon,
                    rating = primaryItem.rating,
                    category = primaryItem.categoryName,
                    isInWatchlist = isInWatchlist,
                    isLoading = false,
                    alternateLinks = alternateLinks
                )

                // Fetch detailed VOD info from API
                fetchVodDetails(primaryItem)

                // Observe watchlist changes
                watchlistRepository.isInWatchlistFlow(primaryItem.id).collect { inWatchlist ->
                    _uiState.value = _uiState.value.copy(isInWatchlist = inWatchlist)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load movie details"
                )
            }
        }
    }

    private suspend fun buildAlternateLinks(items: List<VodItem>): List<VodLinkOption> {
        return items.mapNotNull { item ->
            val playlist = playlistRepository.getPlaylistById(item.playlistId)
            if (playlist != null) {
                val url = vodRepository.buildVodStreamUrl(playlist, item)
                VodLinkOption(
                    vodItem = item,
                    playlistName = playlist.name,
                    streamUrl = url
                )
            } else null
        }
    }

    private suspend fun fetchVodDetails(vodItem: VodItem) {
        val playlist = playlistRepository.getPlaylistById(vodItem.playlistId)
        if (playlist != null && playlist.serverUrl != null) {
            val credentials = XtreamCredentials(
                serverUrl = playlist.serverUrl,
                username = playlist.username ?: "",
                password = playlist.password ?: ""
            )
            val vodInfoResult = xtreamApiService.getVodInfo(credentials, vodItem.streamId)
            if (vodInfoResult.isSuccess) {
                val vodInfo = vodInfoResult.getOrNull()?.info
                if (vodInfo != null) {
                    _uiState.value = _uiState.value.copy(
                        plot = vodInfo.plot,
                        genre = vodInfo.genre,
                        director = vodInfo.director,
                        cast = vodInfo.cast,
                        releaseDate = vodInfo.releaseDate ?: vodInfo.releasedate,
                        year = vodInfo.year,
                        duration = vodInfo.duration,
                        backdrop = vodInfo.backdropPath?.filterNotNull()?.firstOrNull(),
                        country = vodInfo.country
                    )
                }
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            currentVodItem?.let { vod ->
                watchlistRepository.toggleWatchlist(vod)
            }
        }
    }

    fun getStreamUrl(): String? {
        val vod = currentVodItem ?: return null
        return viewModelScope.let {
            val playlist = kotlinx.coroutines.runBlocking {
                playlistRepository.getPlaylistById(vod.playlistId)
            }
            playlist?.let { vodRepository.buildVodStreamUrl(it, vod) }
        }
    }

    /**
     * Clean movie title by removing common prefixes, suffixes, and quality tags
     * Examples:
     * - "TOP - Movie Name (2024)" -> "Movie Name"
     * - "Movie Name 4K" -> "Movie Name"
     * - "Movie Name (2024) [1080p]" -> "Movie Name"
     */
    private fun cleanMovieTitle(title: String): String {
        var cleaned = title

        // Remove common prefixes (TOP -, NEW -, HD -, etc.)
        cleaned = cleaned.replace(Regex("^(TOP|NEW|HD|UHD|4K|IMAX)\\s*[-:]\\s*", RegexOption.IGNORE_CASE), "")

        // Remove year in parentheses at end: (2024), (2023)
        cleaned = cleaned.replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "")

        // Remove quality tags: 4K, 1080p, 720p, HDR, etc.
        cleaned = cleaned.replace(Regex("\\s*(4K|UHD|HDR|1080p|720p|480p|HD|SD)\\s*", RegexOption.IGNORE_CASE), " ")

        // Remove brackets with content: [1080p], [4K], etc.
        cleaned = cleaned.replace(Regex("\\s*\\[[^\\]]*\\]\\s*"), " ")

        // Remove extra whitespace
        cleaned = cleaned.trim().replace(Regex("\\s+"), " ")

        // If cleaning removed everything, return original title
        return if (cleaned.isEmpty()) title else cleaned
    }
}

data class VodLinkOption(
    val vodItem: VodItem,
    val playlistName: String,
    val streamUrl: String
)

data class VodDetailUiState(
    val vodItem: VodItem? = null,
    val title: String = "",
    val poster: String? = null,
    val backdrop: String? = null,
    val rating: String? = null,
    val category: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val director: String? = null,
    val cast: String? = null,
    val releaseDate: String? = null,
    val year: String? = null,
    val duration: String? = null,
    val country: String? = null,
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val alternateLinks: List<VodLinkOption> = emptyList()
)
