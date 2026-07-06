package com.iptvplayer.tv.ui.screens.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.VodItem
import com.iptvplayer.tv.data.repository.PlaylistRepository
import com.iptvplayer.tv.data.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VodViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val vodRepository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VodUiState())
    val uiState: StateFlow<VodUiState> = _uiState.asStateFlow()

    private var currentPlaylistId: String = ""
    private var currentPlaylist: Playlist? = null
    private var currentPage = 0
    private var hasMoreItems = true
    private val pageSize = 30  // IPTVnator uses 25, we use 30

    fun loadVod(playlistId: String, forceReload: Boolean = false) {
        android.util.Log.d("VodVM", "loadVod called with playlistId=$playlistId, currentPlaylistId=$currentPlaylistId, force=$forceReload")

        if (!forceReload && currentPlaylistId == playlistId && _uiState.value.vodItems.isNotEmpty()) {
            android.util.Log.d("VodVM", "Skipping - already loaded ${_uiState.value.vodItems.size} items")
            return
        }

        currentPlaylistId = playlistId
        currentPage = 0
        hasMoreItems = true
        _uiState.value = _uiState.value.copy(isLoading = true, vodItems = emptyList())
        android.util.Log.d("VodVM", "Starting VOD load...")

        viewModelScope.launch {
            try {
                android.util.Log.d("VodVM", "Fetching playlist from DB...")
                currentPlaylist = playlistRepository.getPlaylistById(playlistId)
                android.util.Log.d("VodVM", "Playlist: ${currentPlaylist?.name}, type=${currentPlaylist?.type}")

                if (currentPlaylist == null) {
                    android.util.Log.e("VodVM", "Playlist not found for ID: $playlistId")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Playlist not found")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    playlistName = currentPlaylist?.name ?: "Movies"
                )

                // Check if cached data exists, if not fetch from API
                val hasCachedData = if (forceReload) {
                    android.util.Log.d("VodVM", "Force reload - ignoring cache")
                    false
                } else {
                    vodRepository.hasVodData(playlistId)
                }
                android.util.Log.d("VodVM", "hasCachedData: $hasCachedData")

                if (!hasCachedData) {
                    android.util.Log.d("VodVM", "Fetching VOD from API...")
                    val loadResult = vodRepository.loadVodForPlaylist(playlistId)
                    if (loadResult.isFailure) {
                        android.util.Log.e("VodVM", "API load failed: ${loadResult.exceptionOrNull()?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = loadResult.exceptionOrNull()?.message
                        )
                        return@launch
                    }
                    android.util.Log.d("VodVM", "API load success: ${loadResult.getOrNull()} items")
                }

                // Load categories
                val categories = vodRepository.getVodCategories(playlistId)
                val totalCount = vodRepository.getVodCount(playlistId)
                android.util.Log.d("VodVM", "categories: ${categories.size}, totalCount: $totalCount")
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    totalCount = totalCount
                )

                // Load first page
                loadPage()
                android.util.Log.d("VodVM", "VOD load complete. Items: ${_uiState.value.vodItems.size}")
            } catch (e: Exception) {
                android.util.Log.e("VodVM", "Exception during VOD load: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private suspend fun loadPage() {
        val category = _uiState.value.selectedCategory
        android.util.Log.d("VodVM", "loadPage: page=$currentPage, category=$category, playlistId=$currentPlaylistId")

        val items = if (category != null) {
            vodRepository.getVodByCategoryPaginated(currentPlaylistId, category, currentPage, pageSize)
        } else {
            vodRepository.getVodPaginated(currentPlaylistId, currentPage, pageSize)
        }

        android.util.Log.d("VodVM", "loadPage: got ${items.size} items")

        hasMoreItems = items.size == pageSize

        val currentItems = if (currentPage == 0) items else _uiState.value.vodItems + items

        _uiState.value = _uiState.value.copy(
            vodItems = currentItems,
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
            vodItems = emptyList(),
            isLoading = true
        )

        viewModelScope.launch {
            // Update total count for category
            val count = if (category != null) {
                vodRepository.getVodCountByCategory(currentPlaylistId, category)
            } else {
                vodRepository.getVodCount(currentPlaylistId)
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

    fun getStreamUrl(vodItem: VodItem): String? {
        val playlist = currentPlaylist ?: return null
        return vodRepository.buildVodStreamUrl(playlist, vodItem)
    }

    /**
     * Build stream URL for a VodItem without prior playlist context.
     * Fetches playlist from DB synchronously - use for dashboard navigation.
     */
    fun buildVodStreamUrl(vodItem: VodItem): String? {
        // If we already have the playlist cached, use it
        if (currentPlaylist?.id == vodItem.playlistId) {
            return vodRepository.buildVodStreamUrl(currentPlaylist!!, vodItem)
        }

        // Otherwise fetch synchronously (blocking - OK for navigation action)
        return kotlinx.coroutines.runBlocking {
            val playlist = playlistRepository.getPlaylistById(vodItem.playlistId)
            if (playlist != null) {
                vodRepository.buildVodStreamUrl(playlist, vodItem)
            } else null
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                // Reset to paginated view
                currentPage = 0
                hasMoreItems = true
                _uiState.value = _uiState.value.copy(vodItems = emptyList(), isLoading = true)
                loadPage()
            } else {
                // Search returns limited results (50 max)
                val items = vodRepository.searchVod(currentPlaylistId, query)
                _uiState.value = _uiState.value.copy(
                    vodItems = items,
                    canLoadMore = false
                )
            }
        }
    }

    private suspend fun getVodCount(playlistId: String): Int {
        return vodRepository.getVodCount(playlistId)
    }
}

data class VodUiState(
    val playlistName: String = "",
    val vodItems: List<VodItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedItemId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val totalCount: Int = 0,
    val error: String? = null
)
