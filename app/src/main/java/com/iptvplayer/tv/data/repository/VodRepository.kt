package com.iptvplayer.tv.data.repository

import com.iptvplayer.tv.data.api.XtreamApiService
import com.iptvplayer.tv.data.local.PlaylistDao
import com.iptvplayer.tv.data.local.SeriesDao
import com.iptvplayer.tv.data.local.VodDao
import com.iptvplayer.tv.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val xtreamApiService: XtreamApiService
) {
    // Clean up all VOD/Series data for a playlist
    suspend fun deletePlaylistData(playlistId: String) {
        vodDao.deleteVodByPlaylist(playlistId)
        seriesDao.deleteSeriesByPlaylist(playlistId)
    }

    // Delete only VOD data (for refresh)
    suspend fun deleteVodData(playlistId: String) {
        vodDao.deleteVodByPlaylist(playlistId)
    }

    // Delete only Series data (for refresh)
    suspend fun deleteSeriesData(playlistId: String) {
        seriesDao.deleteSeriesByPlaylist(playlistId)
    }

    // Check if VOD data exists in cache
    suspend fun hasVodData(playlistId: String): Boolean = vodDao.getVodCount(playlistId) > 0
    suspend fun hasSeriesData(playlistId: String): Boolean = seriesDao.getSeriesCount(playlistId) > 0

    // Total counts
    suspend fun getVodCount(playlistId: String): Int = vodDao.getVodCount(playlistId)
    suspend fun getSeriesCount(playlistId: String): Int = seriesDao.getSeriesCount(playlistId)

    // VOD Movies
    fun getVodByPlaylist(playlistId: String): Flow<List<VodItem>> =
        vodDao.getVodByPlaylist(playlistId)

    // Paginated access (IPTVnator pattern - load 30 at a time)
    suspend fun getVodPaginated(playlistId: String, page: Int, pageSize: Int = 30): List<VodItem> =
        vodDao.getVodPaginated(playlistId, pageSize, page * pageSize)

    suspend fun getVodByCategoryPaginated(playlistId: String, category: String, page: Int, pageSize: Int = 30): List<VodItem> =
        vodDao.getVodByCategoryPaginated(playlistId, category, pageSize, page * pageSize)

    suspend fun getVodCountByCategory(playlistId: String, category: String): Int =
        vodDao.getVodCountByCategory(playlistId, category)

    suspend fun getSeriesPaginated(playlistId: String, page: Int, pageSize: Int = 30): List<SeriesItem> =
        seriesDao.getSeriesPaginated(playlistId, pageSize, page * pageSize)

    suspend fun getSeriesByCategoryPaginated(playlistId: String, category: String, page: Int, pageSize: Int = 30): List<SeriesItem> =
        seriesDao.getSeriesByCategoryPaginated(playlistId, category, pageSize, page * pageSize)

    suspend fun getSeriesCountByCategory(playlistId: String, category: String): Int =
        seriesDao.getSeriesCountByCategory(playlistId, category)

    suspend fun getVodById(id: String): VodItem? = vodDao.getVodById(id)

    suspend fun getVodCategories(playlistId: String): List<String> =
        vodDao.getVodCategories(playlistId)

    suspend fun getVodByCategory(playlistId: String, category: String): List<VodItem> =
        vodDao.getVodByCategory(playlistId, category)

    suspend fun searchVod(playlistId: String, query: String): List<VodItem> =
        vodDao.searchVod(playlistId, query)

    // Global search across all playlists
    suspend fun searchVod(query: String): List<VodItem> =
        vodDao.searchAllVod(query)

    suspend fun loadVodForPlaylist(playlistId: String): Result<Int> {
        android.util.Log.d("VodRepo", "loadVodForPlaylist: $playlistId")

        val playlist = playlistDao.getPlaylistById(playlistId)
        if (playlist == null) {
            android.util.Log.e("VodRepo", "Playlist not found: $playlistId")
            return Result.failure(Exception("Playlist not found"))
        }

        android.util.Log.d("VodRepo", "Playlist found: ${playlist.name}, type=${playlist.type}")

        if (playlist.type != PlaylistType.XTREAM) {
            android.util.Log.e("VodRepo", "Not Xtream: ${playlist.type}")
            return Result.failure(Exception("VOD only available for Xtream playlists"))
        }

        val serverUrl = playlist.serverUrl
        val username = playlist.username
        val password = playlist.password

        if (serverUrl == null || username == null || password == null) {
            android.util.Log.e("VodRepo", "Missing credentials: url=$serverUrl, user=$username, pass=${password?.take(3)}...")
            return Result.failure(Exception("Missing credentials"))
        }

        val credentials = XtreamCredentials(
            serverUrl = serverUrl,
            username = username,
            password = password
        )
        android.util.Log.d("VodRepo", "Credentials: ${serverUrl.take(30)}..., user=$username")

        // Fetch categories
        val categoryMap = mutableMapOf<String?, String>()
        val categoriesResult = xtreamApiService.getVodCategories(credentials)
        if (categoriesResult.isSuccess) {
            categoriesResult.getOrNull()?.forEach { cat ->
                categoryMap[cat.categoryId] = cat.categoryName
            }
        }

        // Fetch VOD streams
        android.util.Log.d("VodRepo", "Fetching VOD streams from API...")
        val vodResult = xtreamApiService.getVodStreams(credentials)
        if (vodResult.isFailure) {
            android.util.Log.e("VodRepo", "API failed: ${vodResult.exceptionOrNull()?.message}")
            return Result.failure(vodResult.exceptionOrNull() ?: Exception("Failed to fetch VOD"))
        }

        val streams = vodResult.getOrNull() ?: emptyList()
        android.util.Log.d("VodRepo", "Got ${streams.size} streams from API")

        // Clear old data first
        vodDao.deleteVodByPlaylist(playlistId)

        // Process and insert in batches to avoid OOM (IPTVnator uses 100)
        var totalInserted = 0
        val batchSize = 100
        streams.chunked(batchSize).forEach { batch ->
            val vodItems = batch.map { stream ->
                VodItem(
                    id = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    streamId = stream.streamId,
                    name = stream.name,
                    icon = stream.streamIcon,
                    rating = stream.rating,
                    rating5based = stream.rating5based,
                    categoryId = stream.categoryId,
                    categoryName = categoryMap[stream.categoryId] ?: stream.categoryId,
                    containerExtension = stream.containerExtension,
                    added = stream.added
                )
            }
            vodDao.insertVodItems(vodItems)
            totalInserted += vodItems.size
        }

        android.util.Log.d("VodRepo", "Loaded $totalInserted VOD items for playlist $playlistId")
        return Result.success(totalInserted)
    }

    fun buildVodStreamUrl(playlist: Playlist, vodItem: VodItem): String {
        val credentials = XtreamCredentials(
            serverUrl = playlist.serverUrl ?: "",
            username = playlist.username ?: "",
            password = playlist.password ?: ""
        )
        return XtreamApiService.buildVodStreamUrl(
            credentials,
            vodItem.streamId,
            vodItem.containerExtension ?: "mp4"
        )
    }

    // Series
    fun getSeriesByPlaylist(playlistId: String): Flow<List<SeriesItem>> =
        seriesDao.getSeriesByPlaylist(playlistId)

    suspend fun getSeriesById(id: String): SeriesItem? = seriesDao.getSeriesById(id)

    suspend fun getSeriesCategories(playlistId: String): List<String> =
        seriesDao.getSeriesCategories(playlistId)

    suspend fun getSeriesByCategory(playlistId: String, category: String): List<SeriesItem> =
        seriesDao.getSeriesByCategory(playlistId, category)

    suspend fun searchSeries(playlistId: String, query: String): List<SeriesItem> =
        seriesDao.searchSeries(playlistId, query)

    // Global search across all playlists
    suspend fun searchSeries(query: String): List<SeriesItem> =
        seriesDao.searchAllSeries(query)

    suspend fun loadSeriesForPlaylist(playlistId: String): Result<Int> {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return Result.failure(Exception("Playlist not found"))

        if (playlist.type != PlaylistType.XTREAM) {
            return Result.failure(Exception("Series only available for Xtream playlists"))
        }

        val credentials = XtreamCredentials(
            serverUrl = playlist.serverUrl ?: return Result.failure(Exception("No server URL")),
            username = playlist.username ?: return Result.failure(Exception("No username")),
            password = playlist.password ?: return Result.failure(Exception("No password"))
        )

        // Fetch categories
        val categoryMap = mutableMapOf<String?, String>()
        val categoriesResult = xtreamApiService.getSeriesCategories(credentials)
        if (categoriesResult.isSuccess) {
            categoriesResult.getOrNull()?.forEach { cat ->
                categoryMap[cat.categoryId] = cat.categoryName
            }
        }

        // Fetch series
        val seriesResult = xtreamApiService.getSeriesStreams(credentials)
        if (seriesResult.isFailure) {
            return Result.failure(seriesResult.exceptionOrNull() ?: Exception("Failed to fetch series"))
        }

        val seriesList = seriesResult.getOrNull() ?: emptyList()

        // Clear old data first
        seriesDao.deleteSeriesByPlaylist(playlistId)

        // Process and insert in batches to avoid OOM (IPTVnator uses 100)
        var totalInserted = 0
        val batchSize = 100
        seriesList.chunked(batchSize).forEach { batch ->
            val seriesItems = batch.map { series ->
                SeriesItem(
                    id = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    seriesId = series.seriesId,
                    name = series.name,
                    cover = series.cover,
                    plot = series.plot,
                    cast = series.cast,
                    director = series.director,
                    genre = series.genre,
                    releaseDate = series.releaseDate,
                    rating = series.rating,
                    rating5based = series.rating5based,
                    categoryId = series.categoryId,
                    categoryName = categoryMap[series.categoryId] ?: series.categoryId,
                    backdropPath = series.backdropPath?.filterNotNull()?.firstOrNull(),
                    youtubeTrailer = series.youtube_trailer,
                    episodeRunTime = series.episodeRunTime
                )
            }
            seriesDao.insertSeriesItems(seriesItems)
            totalInserted += seriesItems.size
        }

        android.util.Log.d("VodRepo", "Loaded $totalInserted series for playlist $playlistId")
        return Result.success(totalInserted)
    }

    fun buildSeriesStreamUrl(playlist: Playlist, episodeId: Int, extension: String): String {
        val credentials = XtreamCredentials(
            serverUrl = playlist.serverUrl ?: "",
            username = playlist.username ?: "",
            password = playlist.password ?: ""
        )
        return XtreamApiService.buildSeriesStreamUrl(credentials, episodeId, extension)
    }

    // Dashboard queries - cross-playlist aggregation
    suspend fun getTopRatedMovies(limit: Int = 20): List<VodItem> =
        vodDao.getTopRatedVod(limit)

    suspend fun getTopRatedSeries(limit: Int = 20): List<SeriesItem> =
        seriesDao.getTopRatedSeries(limit)

    suspend fun getRecentlyAddedVod(limit: Int = 20): List<VodItem> =
        vodDao.getRecentlyAddedVod(limit)

    suspend fun getRecentlyAddedSeries(limit: Int = 20): List<SeriesItem> =
        seriesDao.getRecentlyAddedSeries(limit)

    /**
     * Hero items: top 5 rated across VOD and Series, sorted by rating
     */
    suspend fun getHeroItems(limit: Int = 5): List<Any> {
        val topVod = vodDao.getTopRatedVod(limit)
        val topSeries = seriesDao.getTopRatedSeries(limit)

        // Merge and sort by rating, take top N
        return (topVod + topSeries)
            .sortedByDescending { item ->
                when (item) {
                    is VodItem -> item.rating5based ?: 0.0
                    is SeriesItem -> item.rating5based ?: 0.0
                    else -> 0.0
                }
            }
            .take(limit)
    }

    /**
     * Recently added content: mixed VOD and Series, sorted by recency
     */
    suspend fun getRecentlyAddedContent(limit: Int = 20): List<Any> {
        val recentVod = vodDao.getRecentlyAddedVod(limit)
        val recentSeries = seriesDao.getRecentlyAddedSeries(limit)

        // Interleave: alternate VOD and Series for variety
        val result = mutableListOf<Any>()
        val vodIter = recentVod.iterator()
        val seriesIter = recentSeries.iterator()

        while (result.size < limit && (vodIter.hasNext() || seriesIter.hasNext())) {
            if (vodIter.hasNext()) result.add(vodIter.next())
            if (result.size < limit && seriesIter.hasNext()) result.add(seriesIter.next())
        }

        return result
    }

    /**
     * Find VOD by title (for TMDB matching)
     * Tries exact match first, then fuzzy
     */
    suspend fun findVodByTitle(title: String): VodItem? {
        android.util.Log.d("VodRepo", "Finding VOD by title: $title")
        // Try exact match first
        vodDao.findVodByTitle(title)?.let {
            android.util.Log.d("VodRepo", "Exact match found: ${it.name}")
            return it
        }
        // Try fuzzy match
        val fuzzy = vodDao.findVodByTitleFuzzy(title)
        android.util.Log.d("VodRepo", "Fuzzy match result: ${fuzzy?.name ?: "none"}")
        return fuzzy
    }

    /**
     * Find Series by title (for TMDB matching)
     * Tries exact match first, then fuzzy
     */
    suspend fun findSeriesByTitle(title: String): SeriesItem? {
        android.util.Log.d("VodRepo", "Finding Series by title: $title")
        // Try exact match first
        seriesDao.findSeriesByTitle(title)?.let {
            android.util.Log.d("VodRepo", "Exact match found: ${it.name}")
            return it
        }
        // Try fuzzy match
        val fuzzy = seriesDao.findSeriesByTitleFuzzy(title)
        android.util.Log.d("VodRepo", "Fuzzy match result: ${fuzzy?.name ?: "none"}")
        return fuzzy
    }

    /**
     * Find Series by seriesId
     */
    suspend fun findSeriesBySeriesId(seriesId: Int): SeriesItem? = seriesDao.findSeriesBySeriesId(seriesId)

    /**
     * Find ALL VOD items matching title (for showing multiple options)
     */
    suspend fun findAllVodByTitle(title: String): List<VodItem> {
        android.util.Log.d("VodRepo", "Finding all VOD by title: $title")
        // Try exact match first
        val exact = vodDao.findAllVodByTitle(title)
        if (exact.isNotEmpty()) {
            android.util.Log.d("VodRepo", "Found ${exact.size} exact matches")
            return exact
        }
        // Try fuzzy match
        val fuzzy = vodDao.findAllVodByTitleFuzzy(title)
        android.util.Log.d("VodRepo", "Found ${fuzzy.size} fuzzy matches")
        return fuzzy
    }
}
