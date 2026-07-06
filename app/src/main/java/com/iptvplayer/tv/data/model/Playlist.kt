package com.iptvplayer.tv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "playlists")
@Serializable
data class Playlist(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: PlaylistType,
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
    val m3uUrl: String? = null,
    val channelsCount: Int = 0,
    val importDate: Long = System.currentTimeMillis(),
    val updateDate: Long = System.currentTimeMillis(),
    val autoRefresh: Boolean = false
)

@Serializable
enum class PlaylistType {
    XTREAM,
    M3U_URL,
    M3U_FILE
}
