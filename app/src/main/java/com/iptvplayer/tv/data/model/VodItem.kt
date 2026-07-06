package com.iptvplayer.tv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vod_items")
data class VodItem(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val streamId: Int,
    val name: String,
    val icon: String? = null,
    val rating: String? = null,
    val rating5based: Double? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val containerExtension: String? = null,
    val added: String? = null
)

@Entity(tableName = "series_items")
data class SeriesItem(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val seriesId: Int,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val rating5based: Double? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val backdropPath: String? = null,
    val youtubeTrailer: String? = null,
    val episodeRunTime: String? = null
)
