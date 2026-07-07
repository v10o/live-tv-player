package com.iptvplayer.tv.di

import android.content.Context
import androidx.room.Room
import com.iptvplayer.tv.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import java.util.concurrent.TimeUnit
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@OptIn(ExperimentalSerializationApi::class)
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    explicitNulls = false
                }, contentType = ContentType.Any)
            }

            install(Logging) {
                level = LogLevel.HEADERS
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 300_000  // 5 min for large VOD catalogs
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 300_000   // 5 min for large responses
            }

            defaultRequest {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Accept", "application/json, text/plain, */*")
            }

            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(5, TimeUnit.MINUTES)  // 5 min for 2M items
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "iptv_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()  // Safety net for DB issues
            .build()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    @Singleton
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao = database.watchHistoryDao()

    @Provides
    @Singleton
    fun provideVodDao(database: AppDatabase): VodDao = database.vodDao()

    @Provides
    @Singleton
    fun provideSeriesDao(database: AppDatabase): SeriesDao = database.seriesDao()

    @Provides
    @Singleton
    fun provideWatchlistDao(database: AppDatabase): WatchlistDao = database.watchlistDao()
}
