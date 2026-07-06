package com.iptvplayer.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * EPG Program - a single show/episode in the guide
 */
data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,  // Unix timestamp in milliseconds
    val endTime: Long,    // Unix timestamp in milliseconds
    val category: String? = null,
    val episodeInfo: String? = null,  // e.g. "S2 E2"
    val thumbnail: String? = null
) {
    val durationMinutes: Int
        get() = ((endTime - startTime) / 60000).toInt()

    fun isCurrentlyAiring(now: Long = System.currentTimeMillis()): Boolean =
        now in startTime until endTime

    fun hasEnded(now: Long = System.currentTimeMillis()): Boolean =
        now >= endTime
}

/**
 * EPG Channel with its programs
 */
data class EpgChannel(
    val id: String,
    val number: Int,
    val name: String,
    val logo: String? = null,
    val url: String? = null,
    val programs: List<EpgProgram> = emptyList()
)

/**
 * Xtream EPG response models
 */
@Serializable
data class XtreamEpgListing(
    @SerialName("epg_listings")
    val listings: List<XtreamEpgProgram>? = null
)

@Serializable
data class XtreamEpgProgram(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("start")
    val start: String? = null,
    @SerialName("end")
    val end: String? = null,
    @SerialName("start_timestamp")
    val startTimestamp: Long? = null,
    @SerialName("stop_timestamp")
    val stopTimestamp: Long? = null,
    @SerialName("channel_id")
    val channelId: String? = null
)
