package com.iptvplayer.tv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "channels")
@Serializable
data class Channel(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isRadio: Boolean = false,
    val archiveDays: Int = 0,
    val userAgent: String? = null,
    val referer: String? = null,
    val streamId: Int? = null,
    val categoryId: String? = null
)
