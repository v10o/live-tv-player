package com.iptvplayer.tv.data.repository

import com.iptvplayer.tv.data.api.M3uParser
import com.iptvplayer.tv.data.api.XtreamApiService
import com.iptvplayer.tv.data.local.ChannelDao
import com.iptvplayer.tv.data.local.FavoriteDao
import com.iptvplayer.tv.data.local.PlaylistDao
import com.iptvplayer.tv.data.local.WatchHistoryDao
import com.iptvplayer.tv.data.model.*
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val xtreamApiService: XtreamApiService,
    private val m3uParser: M3uParser,
    private val httpClient: HttpClient,
    private val vodRepository: dagger.Lazy<VodRepository>  // Lazy to avoid circular dependency
) {
    fun getAllPlaylists(): Flow<List<Playlist>> {
        android.util.Log.d("PlaylistRepo", "getAllPlaylists() called")
        return playlistDao.getAllPlaylists()
    }

    suspend fun getPlaylistById(id: String): Playlist? = playlistDao.getPlaylistById(id)

    suspend fun deletePlaylist(playlistId: String) {
        channelDao.deleteChannelsByPlaylist(playlistId)
        vodRepository.get().deletePlaylistData(playlistId)
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun updatePlaylist(
        playlistId: String,
        name: String,
        serverUrl: String?,
        username: String?,
        password: String?,
        m3uUrl: String?
    ): Result<Playlist> {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return Result.failure(Exception("Playlist not found"))

        val updated = playlist.copy(
            name = name,
            serverUrl = serverUrl ?: playlist.serverUrl,
            username = username ?: playlist.username,
            password = password ?: playlist.password,
            m3uUrl = m3uUrl ?: playlist.m3uUrl,
            updateDate = System.currentTimeMillis()
        )

        playlistDao.updatePlaylist(updated)
        return Result.success(updated)
    }

    suspend fun refreshPlaylist(playlistId: String): Result<Playlist> {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return Result.failure(Exception("Playlist not found"))

        // Delete existing channels
        channelDao.deleteChannelsByPlaylist(playlistId)

        return when (playlist.type) {
            PlaylistType.XTREAM -> refreshXtreamPlaylist(playlist)
            PlaylistType.M3U_URL -> refreshM3uPlaylist(playlist)
            PlaylistType.M3U_FILE -> Result.failure(Exception("Cannot refresh file-based playlist"))
        }
    }

    private suspend fun refreshXtreamPlaylist(playlist: Playlist): Result<Playlist> {
        val serverUrl = playlist.serverUrl ?: return Result.failure(Exception("No server URL"))
        val username = playlist.username ?: return Result.failure(Exception("No username"))
        val password = playlist.password ?: return Result.failure(Exception("No password"))

        val credentials = XtreamCredentials(serverUrl, username, password)
        val accountResult = xtreamApiService.tryGetAccountInfoWithPorts(credentials)

        if (accountResult.isFailure) {
            return Result.failure(accountResult.exceptionOrNull() ?: Exception("Connection failed"))
        }

        val workingCreds = accountResult.getOrNull()?.workingCredentials ?: credentials

        // Fetch categories to map IDs to names
        val categoryMap = fetchCategoryMap(workingCreds)

        val liveResult = xtreamApiService.getLiveStreams(workingCreds)
        if (liveResult.isFailure) {
            return Result.failure(liveResult.exceptionOrNull() ?: Exception("Failed to fetch channels"))
        }

        val channels = liveResult.getOrNull()?.map { stream ->
            Channel(
                id = java.util.UUID.randomUUID().toString(),
                playlistId = playlist.id,
                name = stream.name,
                url = XtreamApiService.buildLiveStreamUrl(workingCreds, stream.streamId),
                logo = stream.streamIcon,
                group = categoryMap[stream.categoryId] ?: stream.categoryId,
                tvgId = stream.epgChannelId,
                streamId = stream.streamId,
                categoryId = stream.categoryId,
                archiveDays = stream.tvArchiveDuration
            )
        } ?: emptyList()

        channelDao.insertChannels(channels)

        // Note: VOD and Series loaded lazily on first visit to avoid OOM with large catalogs

        val updated = playlist.copy(
            serverUrl = workingCreds.serverUrl,
            channelsCount = channels.size,
            updateDate = System.currentTimeMillis()
        )
        playlistDao.updatePlaylist(updated)

        android.util.Log.d("PlaylistRepo", "Refreshed playlist: ${updated.id} with ${channels.size} channels")
        return Result.success(updated)
    }

    private suspend fun fetchCategoryMap(credentials: XtreamCredentials): Map<String?, String> {
        val categoryMap = mutableMapOf<String?, String>()

        val categoriesResult = xtreamApiService.getLiveCategories(credentials)
        if (categoriesResult.isSuccess) {
            categoriesResult.getOrNull()?.forEach { category ->
                categoryMap[category.categoryId] = category.categoryName
            }
            android.util.Log.d("PlaylistRepo", "Loaded ${categoryMap.size} categories")
        }

        return categoryMap
    }

    private suspend fun refreshM3uPlaylist(playlist: Playlist): Result<Playlist> {
        val url = playlist.m3uUrl ?: return Result.failure(Exception("No M3U URL"))

        val parseResult = m3uParser.fetchAndParse(url, playlist.id, httpClient)
        if (parseResult.isFailure) {
            return Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse M3U"))
        }

        val channels = parseResult.getOrNull() ?: emptyList()
        channelDao.insertChannels(channels)

        val updated = playlist.copy(
            channelsCount = channels.size,
            updateDate = System.currentTimeMillis()
        )
        playlistDao.updatePlaylist(updated)

        return Result.success(updated)
    }

    // Xtream Playlist
    suspend fun addXtreamPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Playlist> {
        val credentials = XtreamCredentials(serverUrl, username, password)

        // Verify credentials (tries common ports if needed)
        val accountResult = xtreamApiService.tryGetAccountInfoWithPorts(credentials)
        if (accountResult.isFailure) {
            return Result.failure(accountResult.exceptionOrNull() ?: Exception("Failed to connect"))
        }

        val accountData = accountResult.getOrNull()
        val status = resolvePortalStatus(accountData?.accountInfo)
        // Allow ACTIVE and EXPIRED (user might still want to browse)
        if (status == PortalStatus.UNAVAILABLE || status == PortalStatus.INACTIVE) {
            return Result.failure(Exception("Authentication failed. Check credentials."))
        }
        val isExpired = status == PortalStatus.EXPIRED

        // Use the working credentials (may have different URL/port)
        val workingCreds = accountData?.workingCredentials ?: credentials

        val playlistId = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = playlistId,
            name = name,
            type = PlaylistType.XTREAM,
            serverUrl = workingCreds.serverUrl,  // Save the working URL
            username = workingCreds.username,
            password = workingCreds.password
        )

        // Fetch categories to map IDs to names
        val categoryMap = fetchCategoryMap(workingCreds)

        // Fetch and save channels using working credentials
        val liveResult = xtreamApiService.getLiveStreams(workingCreds)
        if (liveResult.isSuccess) {
            val channels = liveResult.getOrNull()?.map { stream ->
                Channel(
                    id = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    name = stream.name,
                    url = XtreamApiService.buildLiveStreamUrl(workingCreds, stream.streamId),
                    logo = stream.streamIcon,
                    group = categoryMap[stream.categoryId] ?: stream.categoryId,
                    tvgId = stream.epgChannelId,
                    streamId = stream.streamId,
                    categoryId = stream.categoryId,
                    archiveDays = stream.tvArchiveDuration
                )
            } ?: emptyList()

            channelDao.insertChannels(channels)
            val savedPlaylist = playlist.copy(channelsCount = channels.size)
            playlistDao.insertPlaylist(savedPlaylist)
            android.util.Log.d("PlaylistRepo", "Saved playlist: ${savedPlaylist.id} with ${channels.size} channels, ${categoryMap.size} categories")
        } else {
            playlistDao.insertPlaylist(playlist)
            android.util.Log.d("PlaylistRepo", "Saved playlist (no channels): ${playlist.id}")
        }

        return Result.success(playlist)
    }

    // M3U URL Playlist
    suspend fun addM3uUrlPlaylist(name: String, url: String): Result<Playlist> {
        val playlistId = UUID.randomUUID().toString()

        val parseResult = m3uParser.fetchAndParse(url, playlistId, httpClient)
        if (parseResult.isFailure) {
            return Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse M3U"))
        }

        val channels = parseResult.getOrNull() ?: emptyList()
        channelDao.insertChannels(channels)

        val playlist = Playlist(
            id = playlistId,
            name = name,
            type = PlaylistType.M3U_URL,
            m3uUrl = url,
            channelsCount = channels.size
        )
        playlistDao.insertPlaylist(playlist)

        return Result.success(playlist)
    }

    // Channels
    fun getChannelsByPlaylist(playlistId: String): Flow<List<Channel>> =
        channelDao.getChannelsByPlaylist(playlistId)

    suspend fun getChannelById(id: String): Channel? = channelDao.getChannelById(id)

    suspend fun getGroupsByPlaylist(playlistId: String): List<String> =
        channelDao.getGroupsByPlaylist(playlistId)

    suspend fun getChannelsByGroup(playlistId: String, group: String): List<Channel> =
        channelDao.getChannelsByGroup(playlistId, group)

    suspend fun searchChannels(playlistId: String, query: String): List<Channel> =
        channelDao.searchChannels(playlistId, query)

    // Global search across all playlists
    suspend fun searchChannels(query: String): List<Channel> =
        channelDao.searchAllChannels(query)

    suspend fun getAllPlaylistsOnce(): List<Playlist> =
        playlistDao.getAllPlaylistsOnce()

    // Favorites
    fun getFavoritesByPlaylist(playlistId: String) = favoriteDao.getFavoritesByPlaylist(playlistId)

    suspend fun toggleFavorite(playlistId: String, channelId: String) {
        if (favoriteDao.isFavorite(channelId)) {
            favoriteDao.deleteFavorite(channelId)
        } else {
            favoriteDao.insertFavorite(
                com.iptvplayer.tv.data.local.Favorite(
                    id = UUID.randomUUID().toString(),
                    playlistId = playlistId,
                    channelId = channelId
                )
            )
        }
    }

    suspend fun isFavorite(channelId: String): Boolean = favoriteDao.isFavorite(channelId)

    // Watch History
    fun getRecentByPlaylist(playlistId: String) = watchHistoryDao.getRecentByPlaylist(playlistId)

    suspend fun addToWatchHistory(playlistId: String, channelId: String) {
        watchHistoryDao.insertWatchHistory(
            com.iptvplayer.tv.data.local.WatchHistory(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                channelId = channelId
            )
        )
    }

    suspend fun updatePlaybackPosition(channelId: String, position: Long) {
        watchHistoryDao.updatePlaybackPosition(channelId, position)
    }

    suspend fun getPlaybackPosition(channelId: String): Long? =
        watchHistoryDao.getPlaybackPosition(channelId)
}
