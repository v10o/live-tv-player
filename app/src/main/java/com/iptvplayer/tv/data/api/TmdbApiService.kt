package com.iptvplayer.tv.data.api

import com.iptvplayer.tv.data.repository.SettingsRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TMDB API service for trending content.
 * Uses free TMDB API v3.
 *
 * API key configured in Settings > TMDB API Key
 * Get free key at: https://www.themoviedb.org/settings/api
 */
@Singleton
class TmdbApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val baseUrl = "https://api.themoviedb.org/3"
    private val imageBaseUrl = "https://image.tmdb.org/t/p"

    private suspend fun getApiKey(): String {
        return settingsRepository.getTmdbApiKeyOnce()
    }

    suspend fun isEnabled(): Boolean = getApiKey().isNotBlank()

    /**
     * Get trending movies (weekly)
     */
    suspend fun getTrendingMovies(limit: Int = 10): Result<List<TmdbItem>> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            android.util.Log.d("TmdbAPI", "TMDB disabled - no API key configured")
            return Result.success(emptyList())
        }
        return try {
            val url = "$baseUrl/trending/movie/week?api_key=$apiKey"
            android.util.Log.d("TmdbAPI", "Fetching trending movies...")
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            android.util.Log.d("TmdbAPI", "Trending movies response: ${body.take(200)}")
            val result = json.decodeFromString<TmdbResponse>(body)
            android.util.Log.d("TmdbAPI", "Parsed ${result.results.size} trending movies")
            Result.success(result.results.take(limit).mapIndexed { index, item ->
                item.copy(rank = index + 1, mediaType = "movie")
            })
        } catch (e: Exception) {
            android.util.Log.e("TmdbAPI", "Error fetching trending movies: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get trending TV shows (weekly)
     */
    suspend fun getTrendingShows(limit: Int = 10): Result<List<TmdbItem>> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return Result.success(emptyList())
        }
        return try {
            val url = "$baseUrl/trending/tv/week?api_key=$apiKey"
            android.util.Log.d("TmdbAPI", "Fetching trending shows...")
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            val result = json.decodeFromString<TmdbResponse>(body)
            android.util.Log.d("TmdbAPI", "Parsed ${result.results.size} trending shows")
            Result.success(result.results.take(limit).mapIndexed { index, item ->
                item.copy(rank = index + 1, mediaType = "tv")
            })
        } catch (e: Exception) {
            android.util.Log.e("TmdbAPI", "Error fetching trending shows: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getPosterUrl(path: String?, size: String = "w342"): String? {
        return path?.let { "$imageBaseUrl/$size$it" }
    }

    fun getBackdropUrl(path: String?, size: String = "w780"): String? {
        return path?.let { "$imageBaseUrl/$size$it" }
    }
}

@Serializable
data class TmdbResponse(
    val page: Int = 1,
    val results: List<TmdbItem> = emptyList(),
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("total_results")
    val totalResults: Int = 0
)

@Serializable
data class TmdbItem(
    val id: Int,
    val title: String? = null,  // For movies
    val name: String? = null,   // For TV shows
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double? = null,
    @SerialName("vote_count")
    val voteCount: Int? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,  // For movies
    @SerialName("first_air_date")
    val firstAirDate: String? = null,  // For TV shows
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("genre_ids")
    val genreIds: List<Int> = emptyList(),
    val popularity: Double? = null,
    val rank: Int = 0  // Position in trending list (1-10)
) {
    val displayTitle: String
        get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"

    val year: String?
        get() = (releaseDate ?: firstAirDate)?.take(4)

    val rating: String
        get() = voteAverage?.let { String.format("%.1f", it) } ?: "N/A"
}
