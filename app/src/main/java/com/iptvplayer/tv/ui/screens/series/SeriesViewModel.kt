package com.iptvplayer.tv.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private var currentPlaylistId: String = ""
    private var currentPlaylist: Playlist? = null
    private var currentPage = 0
    private var hasMoreItems = true
    private val pageSize = 30

    fun loadSeries(playlistId: String, forceReload: Boolean = false) {
        android.util.Log.d("SeriesVM", "loadSeries called with playlistId=$playlistId, currentPlaylistId=$currentPlaylistId, force=$forceReload")

        // Always update currentPlaylistId for refresh to work
        currentPlaylistId = playlistId

        if (!forceReload && _uiState.value.seriesItems.isNotEmpty()) {
            android.util.Log.d("SeriesVM", "Skipping - already loaded ${_uiState.value.seriesItems.size} items")
            return
        }
        currentPage = 0
        hasMoreItems = true
        _uiState.value = _uiState.value.copy(isLoading = true, seriesItems = emptyList())
        android.util.Log.d("SeriesVM", "Starting Series load...")

        viewModelScope.launch {
            try {
                android.util.Log.d("SeriesVM", "Fetching playlist from DB...")
                currentPlaylist = playlistRepository.getPlaylistById(playlistId)
                android.util.Log.d("SeriesVM", "Playlist: ${currentPlaylist?.name}, type=${currentPlaylist?.type}")

                if (currentPlaylist == null) {
                    android.util.Log.e("SeriesVM", "Playlist not found for ID: $playlistId")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Playlist not found")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    playlistName = currentPlaylist?.name ?: "Series"
                )

                // Check if cached data exists, if not fetch from API
                val hasCachedData = if (forceReload) {
                    android.util.Log.d("SeriesVM", "Force reload - ignoring cache")
                    false
                } else {
                    vodRepository.hasSeriesData(playlistId)
                }
                android.util.Log.d("SeriesVM", "hasCachedData: $hasCachedData")

                if (!hasCachedData) {
                    android.util.Log.d("SeriesVM", "Fetching Series from API...")
                    val loadResult = vodRepository.loadSeriesForPlaylist(playlistId)
                    if (loadResult.isFailure) {
                        android.util.Log.e("SeriesVM", "API load failed: ${loadResult.exceptionOrNull()?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = loadResult.exceptionOrNull()?.message
                        )
                        return@launch
                    }
                    android.util.Log.d("SeriesVM", "API load success: ${loadResult.getOrNull()} items")
                }

                val categories = vodRepository.getSeriesCategories(playlistId)
                val totalCount = vodRepository.getSeriesCount(playlistId)
                android.util.Log.d("SeriesVM", "categories: ${categories.size}, totalCount: $totalCount")
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    totalCount = totalCount
                )

                loadPage()
                android.util.Log.d("SeriesVM", "Series load complete. Items: ${_uiState.value.seriesItems.size}")
            } catch (e: Exception) {
                android.util.Log.e("SeriesVM", "Exception during Series load: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private suspend fun loadPage() {
        val category = _uiState.value.selectedCategory
        android.util.Log.d("SeriesVM", "loadPage: page=$currentPage, category=$category, playlistId=$currentPlaylistId")

        val items = if (category != null) {
            vodRepository.getSeriesByCategoryPaginated(currentPlaylistId, category, currentPage, pageSize)
        } else {
            vodRepository.getSeriesPaginated(currentPlaylistId, currentPage, pageSize)
        }

        android.util.Log.d("SeriesVM", "loadPage: got ${items.size} items")

        hasMoreItems = items.size == pageSize

        val currentItems = if (currentPage == 0) items else _uiState.value.seriesItems + items

        _uiState.value = _uiState.value.copy(
            seriesItems = currentItems,
            isLoading = false,
            isLoadingMore = false,
            canLoadMore = hasMoreItems
        )
    }

    fun loadMore() {
        if (!hasMoreItems || _uiState.value.isLoadingMore) return

        currentPage++
        _uiState.value = _uiState.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            loadPage()
        }
    }

    fun selectCategory(category: String?) {
        if (_uiState.value.selectedCategory == category) return

        currentPage = 0
        hasMoreItems = true
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            seriesItems = emptyList(),
            selectedItemId = null,  // Clear selection so first item gets auto-selected
            isLoading = true
        )

        viewModelScope.launch {
            val count = if (category != null) {
                vodRepository.getSeriesCountByCategory(currentPlaylistId, category)
            } else {
                vodRepository.getSeriesCount(currentPlaylistId)
            }
            _uiState.value = _uiState.value.copy(totalCount = count)
            loadPage()
        }
    }

    fun setSelectedItem(itemId: String) {
        _uiState.value = _uiState.value.copy(selectedItemId = itemId)
    }

    fun getSelectedCategoryIndex(): Int {
        val category = _uiState.value.selectedCategory ?: return 0
        val index = _uiState.value.categories.indexOf(category)
        return if (index >= 0) index + 1 else 0
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                // Reset to paginated view
                currentPage = 0
                hasMoreItems = true
                _uiState.value = _uiState.value.copy(seriesItems = emptyList(), isLoading = true)
                loadPage()
            } else {
                // Search returns limited results (50 max)
                val items = vodRepository.searchSeries(currentPlaylistId, query)
                _uiState.value = _uiState.value.copy(
                    seriesItems = items,
                    canLoadMore = false
                )
            }
        }
    }

    /**
     * Refresh series data from API (clears cache and re-fetches)
     */
    fun refresh() {
        if (currentPlaylistId.isEmpty()) return
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        // Show full loading state - clear content
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = true,
            seriesItems = emptyList(),
            selectedCategory = null,
            error = null
        )

        viewModelScope.launch {
            try {
                android.util.Log.d("SeriesVM", "Refreshing series data...")
                // Re-fetch from API (loadSeriesForPlaylist handles clearing old data after successful fetch)
                val loadResult = vodRepository.loadSeriesForPlaylist(currentPlaylistId)

                if (loadResult.isFailure) {
                    android.util.Log.e("SeriesVM", "Refresh failed: ${loadResult.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Refresh failed: ${loadResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                // Reload categories and items
                val categories = vodRepository.getSeriesCategories(currentPlaylistId)
                val totalCount = vodRepository.getSeriesCount(currentPlaylistId)
                currentPage = 0
                hasMoreItems = true

                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    totalCount = totalCount,
                    lastUpdated = System.currentTimeMillis()
                )

                loadPage()

                _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
                android.util.Log.d("SeriesVM", "Series refresh complete. Items: ${_uiState.value.seriesItems.size}")
            } catch (e: Exception) {
                android.util.Log.e("SeriesVM", "Refresh exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Refresh failed: ${e.message}"
                )
            }
        }
    }
}

data class SeriesUiState(
    val playlistName: String = "",
    val seriesItems: List<SeriesItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedItemId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = true,
    val totalCount: Int = 0,
    val error: String? = null,
    val lastUpdated: Long = 0L
)
