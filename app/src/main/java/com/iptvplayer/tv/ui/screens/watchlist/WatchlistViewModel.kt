package com.iptvplayer.tv.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        loadWatchlist()
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            watchlistRepository.getAllWatchlist().collect { items ->
                _uiState.value = WatchlistUiState(
                    allItems = items,
                    movieItems = items.filter { it.type == "vod" },
                    seriesItems = items.filter { it.type == "series" }
                )
            }
        }
    }

    fun removeFromWatchlist(itemId: String) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(itemId)
        }
    }
}

data class WatchlistUiState(
    val allItems: List<WatchlistItem> = emptyList(),
    val movieItems: List<WatchlistItem> = emptyList(),
    val seriesItems: List<WatchlistItem> = emptyList()
)
