package com.iptvplayer.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

@Serializable
data class XtreamAccountInfo(
    @SerialName("user_info")
    val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info")
    val serverInfo: XtreamServerInfo? = null
)

@Serializable
data class XtreamUserInfo(
    val username: String? = null,
    val password: String? = null,
    val auth: Int? = null,
    val status: String? = null,
    @SerialName("exp_date")
    val expDate: String? = null,
    @SerialName("is_trial")
    val isTrial: String? = null,
    @SerialName("active_cons")
    val activeCons: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("max_connections")
    val maxConnections: String? = null,
    @SerialName("allowed_output_formats")
    val allowedOutputFormats: List<String> = emptyList()
) {
    val isAuthenticated: Boolean
        get() = auth == 1
}

@Serializable
data class XtreamServerInfo(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port")
    val httpsPort: String? = null,
    @SerialName("server_protocol")
    val serverProtocol: String? = null,
    @SerialName("rtmp_port")
    val rtmpPort: String? = null,
    val timezone: String? = null,
    @SerialName("timestamp_now")
    val timestampNow: Long? = null
)

@Serializable
data class XtreamLiveStream(
    @SerialName("num")
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type")
    val streamType: String? = null,
    @SerialName("stream_id")
    val streamId: Int,
    @SerialName("stream_icon")
    val streamIcon: String? = null,
    @SerialName("epg_channel_id")
    val epgChannelId: String? = null,
    val added: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("custom_sid")
    val customSid: String? = null,
    @SerialName("tv_archive")
    val tvArchive: Int = 0,
    @SerialName("direct_source")
    val directSource: String? = null,
    @SerialName("tv_archive_duration")
    val tvArchiveDuration: Int = 0
)

@Serializable
data class XtreamVodStream(
    @SerialName("num")
    val num: Int? = null,
    val name: String,
    @SerialName("stream_type")
    val streamType: String? = null,
    @SerialName("stream_id")
    val streamId: Int,
    @SerialName("stream_icon")
    val streamIcon: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based")
    val rating5based: Double? = null,
    val added: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("container_extension")
    val containerExtension: String? = null,
    @SerialName("custom_sid")
    val customSid: String? = null,
    @SerialName("direct_source")
    val directSource: String? = null
)

@Serializable
data class XtreamSeriesItem(
    @SerialName("num")
    val num: Int? = null,
    val name: String,
    @SerialName("series_id")
    val seriesId: Int,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("last_modified")
    val lastModified: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based")
    val rating5based: Double? = null,
    @SerialName("backdrop_path")
    val backdropPath: List<String?>? = null,
    val youtube_trailer: String? = null,
    @SerialName("episode_run_time")
    val episodeRunTime: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null
)

// VOD Info Response (from get_vod_info)
@Serializable
data class XtreamVodInfo(
    val info: XtreamVodDetail? = null,
    @SerialName("movie_data")
    val movieData: XtreamVodMovieData? = null
)

@Serializable
data class XtreamVodDetail(
    val name: String? = null,
    @SerialName("movie_image")
    val movieImage: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val releasedate: String? = null,  // Some servers use this
    val rating: String? = null,
    @SerialName("rating_5based")
    val rating5based: Double? = null,
    @SerialName("backdrop_path")
    val backdropPath: List<String?>? = null,
    @SerialName("youtube_trailer")
    val youtubeTrailer: String? = null,
    val duration: String? = null,
    @SerialName("duration_secs")
    val durationSecs: Int? = null,
    val year: String? = null,
    val country: String? = null,
    @SerialName("tmdb_id")
    val tmdbId: String? = null
)

@Serializable
data class XtreamVodMovieData(
    @SerialName("stream_id")
    val streamId: Int? = null,
    val name: String? = null,
    @SerialName("container_extension")
    val containerExtension: String? = null
)

// Series Info Response (from get_series_info)
@Serializable
data class XtreamSeriesInfo(
    val seasons: List<XtreamSeason> = emptyList(),
    val info: XtreamSeriesDetail? = null,
    val episodes: Map<String, List<XtreamEpisode>> = emptyMap()
)

@Serializable
data class XtreamSeason(
    @SerialName("season_number")
    val seasonNumber: Int,
    val name: String? = null,
    val cover: String? = null,
    @SerialName("air_date")
    val airDate: String? = null,
    @SerialName("episode_count")
    val episodeCount: Int? = null
)

@Serializable
data class XtreamSeriesDetail(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based")
    val rating5based: Double? = null,
    @SerialName("backdrop_path")
    val backdropPath: List<String?>? = null,
    @SerialName("youtube_trailer")
    val youtubeTrailer: String? = null,
    @SerialName("episode_run_time")
    val episodeRunTime: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null
)

@Serializable
data class XtreamEpisode(
    val id: String,
    @SerialName("episode_num")
    val episodeNum: Int,
    val title: String,
    @SerialName("container_extension")
    val containerExtension: String? = null,
    val info: XtreamEpisodeInfo? = null,
    val season: Int? = null
)

@Serializable
data class XtreamEpisodeInfo(
    @SerialName("movie_image")
    val movieImage: String? = null,
    val plot: String? = null,
    @SerialName("releasedate")
    val releaseDate: String? = null,
    val rating: Double? = null,
    val duration: String? = null,
    @SerialName("duration_secs")
    val durationSecs: Int? = null
)

enum class PortalStatus {
    ACTIVE,
    INACTIVE,
    EXPIRED,
    UNAVAILABLE
}

fun resolvePortalStatus(accountInfo: XtreamAccountInfo?): PortalStatus {
    val userInfo = accountInfo?.userInfo ?: return PortalStatus.UNAVAILABLE
    if (!userInfo.isAuthenticated) return PortalStatus.INACTIVE

    // Check if status says expired or exp_date is in the past
    val statusExpired = userInfo.status?.lowercase() == "expired"
    val expDate = userInfo.expDate?.toLongOrNull()
    val dateExpired = expDate != null && expDate < System.currentTimeMillis() / 1000

    if (statusExpired || dateExpired) {
        return PortalStatus.EXPIRED
    }

    return PortalStatus.ACTIVE
}
