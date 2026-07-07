package com.iptvplayer.tv.data.repository

import com.iptvplayer.tv.data.local.WatchlistDao
import com.iptvplayer.tv.data.local.WatchlistItem
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao
) {
    fun getAllWatchlist(): Flow<List<WatchlistItem>> = watchlistDao.getAllWatchlist()

    suspend fun getAllWatchlistOnce(): List<WatchlistItem> = watchlistDao.getAllWatchlistOnce()

    fun getMovieWatchlist(): Flow<List<WatchlistItem>> = watchlistDao.getWatchlistByType("vod")

    fun getSeriesWatchlist(): Flow<List<WatchlistItem>> = watchlistDao.getWatchlistByType("series")

    suspend fun addToWatchlist(vodItem: VodItem) {
        watchlistDao.insertWatchlistItem(
            WatchlistItem(
                id = UUID.randomUUID().toString(),
                itemId = vodItem.id,
                playlistId = vodItem.playlistId,
                type = "vod",
                name = vodItem.name,
                cover = vodItem.icon
            )
        )
    }

    suspend fun addToWatchlist(seriesItem: SeriesItem) {
        watchlistDao.insertWatchlistItem(
            WatchlistItem(
                id = UUID.randomUUID().toString(),
                itemId = seriesItem.seriesId.toString(),  // Store seriesId for navigation
                playlistId = seriesItem.playlistId,
                type = "series",
                name = seriesItem.name,
                cover = seriesItem.cover
            )
        )
    }

    suspend fun removeFromWatchlist(itemId: String) {
        watchlistDao.removeFromWatchlist(itemId)
    }

    suspend fun isInWatchlist(itemId: String): Boolean = watchlistDao.isInWatchlist(itemId)

    fun isInWatchlistFlow(itemId: String): Flow<Boolean> = watchlistDao.isInWatchlistFlow(itemId)

    suspend fun toggleWatchlist(vodItem: VodItem): Boolean {
        return if (isInWatchlist(vodItem.id)) {
            removeFromWatchlist(vodItem.id)
            false
        } else {
            addToWatchlist(vodItem)
            true
        }
    }

    suspend fun toggleWatchlist(seriesItem: SeriesItem): Boolean {
        return if (isInWatchlist(seriesItem.id)) {
            removeFromWatchlist(seriesItem.id)
            false
        } else {
            addToWatchlist(seriesItem)
            true
        }
    }
}
