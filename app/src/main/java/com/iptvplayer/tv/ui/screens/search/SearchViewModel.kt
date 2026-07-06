package com.iptvplayer.tv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = SearchUiState()
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce search
            delay(300)

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Global search across all playlists
                val channels = playlistRepository.searchChannels(query)
                val vodResults = vodRepository.searchVod(query)
                val seriesResults = vodRepository.searchSeries(query)

                _uiState.value = SearchUiState(
                    channels = channels.take(20),
                    vodItems = vodResults.take(20),
                    seriesItems = seriesResults.take(20),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun getVodStreamUrl(vodItem: VodItem): String? {
        return viewModelScope.let {
            val playlist = kotlinx.coroutines.runBlocking {
                playlistRepository.getPlaylistById(vodItem.playlistId)
            }
            playlist?.let { vodRepository.buildVodStreamUrl(it, vodItem) }
        }
    }
}

data class SearchUiState(
    val channels: List<Channel> = emptyList(),
    val vodItems: List<VodItem> = emptyList(),
    val seriesItems: List<SeriesItem> = emptyList(),
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean
        get() = channels.isEmpty() && vodItems.isEmpty() && seriesItems.isEmpty() && !isLoading
}
