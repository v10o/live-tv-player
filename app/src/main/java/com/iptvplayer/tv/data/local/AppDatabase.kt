package com.iptvplayer.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iptvplayer.tv.data.model.Channel
import com.iptvplayer.tv.data.model.Playlist
import com.iptvplayer.tv.data.model.SeriesItem
import com.iptvplayer.tv.data.model.VodItem

@Database(
    entities = [
        Playlist::class,
        Channel::class,
        Favorite::class,
        WatchHistory::class,
        VodItem::class,
        SeriesItem::class,
        WatchlistItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create VOD table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS vod_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        playlistId TEXT NOT NULL,
                        streamId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT,
                        rating TEXT,
                        rating5based REAL,
                        categoryId TEXT,
                        categoryName TEXT,
                        containerExtension TEXT,
                        added TEXT
                    )
                """)

                // Create Series table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS series_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        playlistId TEXT NOT NULL,
                        seriesId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        cover TEXT,
                        plot TEXT,
                        cast TEXT,
                        director TEXT,
                        genre TEXT,
                        releaseDate TEXT,
                        rating TEXT,
                        rating5based REAL,
                        categoryId TEXT,
                        categoryName TEXT,
                        backdropPath TEXT,
                        youtubeTrailer TEXT,
                        episodeRunTime TEXT
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create Watchlist table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS watchlist (
                        id TEXT NOT NULL PRIMARY KEY,
                        itemId TEXT NOT NULL,
                        playlistId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        name TEXT NOT NULL,
                        cover TEXT,
                        addedDate INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
