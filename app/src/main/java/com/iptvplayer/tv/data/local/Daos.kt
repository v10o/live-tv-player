package com.iptvplayer.tv.data.local

import androidx.room.*
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY importDate DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists ORDER BY importDate DESC")
    suspend fun getAllPlaylistsOnce(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelsByPlaylistSync(playlistId: String): List<Channel>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: String): Channel?

    @Query("SELECT DISTINCT `group` FROM channels WHERE playlistId = :playlistId AND `group` IS NOT NULL")
    suspend fun getGroupsByPlaylist(playlistId: String): List<String>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND `group` = :group")
    suspend fun getChannelsByGroup(playlistId: String, group: String): List<Channel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: String)

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%'")
    suspend fun searchChannels(playlistId: String, query: String): List<Channel>

    // Global search across all playlists
    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchAllChannels(query: String): List<Channel>
}

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val channelId: String,
    val addedDate: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE playlistId = :playlistId ORDER BY addedDate DESC")
    fun getFavoritesByPlaylist(playlistId: String): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun deleteFavorite(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean
}

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val channelId: String,
    val watchedDate: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE playlistId = :playlistId ORDER BY watchedDate DESC LIMIT 50")
    fun getRecentByPlaylist(playlistId: String): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistory)

    @Query("UPDATE watch_history SET playbackPosition = :position, watchedDate = :time WHERE channelId = :channelId")
    suspend fun updatePlaybackPosition(channelId: String, position: Long, time: Long = System.currentTimeMillis())

    @Query("SELECT playbackPosition FROM watch_history WHERE channelId = :channelId")
    suspend fun getPlaybackPosition(channelId: String): Long?
}

@Dao
interface VodDao {
    @Query("SELECT * FROM vod_items WHERE playlistId = :playlistId")
    fun getVodByPlaylist(playlistId: String): Flow<List<VodItem>>

    // Paginated queries (IPTVnator pattern - load 25-50 at a time)
    @Query("SELECT * FROM vod_items WHERE playlistId = :playlistId LIMIT :limit OFFSET :offset")
    suspend fun getVodPaginated(playlistId: String, limit: Int, offset: Int): List<VodItem>

    @Query("SELECT * FROM vod_items WHERE playlistId = :playlistId AND categoryName = :category LIMIT :limit OFFSET :offset")
    suspend fun getVodByCategoryPaginated(playlistId: String, category: String, limit: Int, offset: Int): List<VodItem>

    @Query("SELECT * FROM vod_items WHERE id = :id")
    suspend fun getVodById(id: String): VodItem?

    @Query("SELECT DISTINCT categoryName FROM vod_items WHERE playlistId = :playlistId AND categoryName IS NOT NULL")
    suspend fun getVodCategories(playlistId: String): List<String>

    @Query("SELECT * FROM vod_items WHERE playlistId = :playlistId AND categoryName = :category")
    suspend fun getVodByCategory(playlistId: String, category: String): List<VodItem>

    @Query("SELECT COUNT(*) FROM vod_items WHERE playlistId = :playlistId AND categoryName = :category")
    suspend fun getVodCountByCategory(playlistId: String, category: String): Int

    @Query("SELECT * FROM vod_items WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchVod(playlistId: String, query: String): List<VodItem>

    // Global search across all playlists
    @Query("SELECT * FROM vod_items WHERE name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchAllVod(query: String): List<VodItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodItems(items: List<VodItem>)

    @Query("DELETE FROM vod_items WHERE playlistId = :playlistId")
    suspend fun deleteVodByPlaylist(playlistId: String)

    @Query("SELECT COUNT(*) FROM vod_items WHERE playlistId = :playlistId")
    suspend fun getVodCount(playlistId: String): Int

    // Dashboard queries - cross-playlist
    @Query("SELECT * FROM vod_items WHERE rating5based IS NOT NULL ORDER BY rating5based DESC LIMIT :limit")
    suspend fun getTopRatedVod(limit: Int): List<VodItem>

    @Query("SELECT * FROM vod_items ORDER BY added DESC LIMIT :limit")
    suspend fun getRecentlyAddedVod(limit: Int): List<VodItem>
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series_items WHERE playlistId = :playlistId")
    fun getSeriesByPlaylist(playlistId: String): Flow<List<SeriesItem>>

    // Paginated queries
    @Query("SELECT * FROM series_items WHERE playlistId = :playlistId LIMIT :limit OFFSET :offset")
    suspend fun getSeriesPaginated(playlistId: String, limit: Int, offset: Int): List<SeriesItem>

    @Query("SELECT * FROM series_items WHERE playlistId = :playlistId AND categoryName = :category LIMIT :limit OFFSET :offset")
    suspend fun getSeriesByCategoryPaginated(playlistId: String, category: String, limit: Int, offset: Int): List<SeriesItem>

    @Query("SELECT * FROM series_items WHERE id = :id")
    suspend fun getSeriesById(id: String): SeriesItem?

    @Query("SELECT DISTINCT categoryName FROM series_items WHERE playlistId = :playlistId AND categoryName IS NOT NULL")
    suspend fun getSeriesCategories(playlistId: String): List<String>

    @Query("SELECT * FROM series_items WHERE playlistId = :playlistId AND categoryName = :category")
    suspend fun getSeriesByCategory(playlistId: String, category: String): List<SeriesItem>

    @Query("SELECT COUNT(*) FROM series_items WHERE playlistId = :playlistId AND categoryName = :category")
    suspend fun getSeriesCountByCategory(playlistId: String, category: String): Int

    @Query("SELECT * FROM series_items WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchSeries(playlistId: String, query: String): List<SeriesItem>

    // Global search across all playlists
    @Query("SELECT * FROM series_items WHERE name LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchAllSeries(query: String): List<SeriesItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesItems(items: List<SeriesItem>)

    @Query("DELETE FROM series_items WHERE playlistId = :playlistId")
    suspend fun deleteSeriesByPlaylist(playlistId: String)

    @Query("SELECT COUNT(*) FROM series_items WHERE playlistId = :playlistId")
    suspend fun getSeriesCount(playlistId: String): Int

    // Dashboard queries - cross-playlist
    @Query("SELECT * FROM series_items WHERE rating5based IS NOT NULL ORDER BY rating5based DESC LIMIT :limit")
    suspend fun getTopRatedSeries(limit: Int): List<SeriesItem>

    @Query("SELECT * FROM series_items ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentlyAddedSeries(limit: Int): List<SeriesItem>
}
