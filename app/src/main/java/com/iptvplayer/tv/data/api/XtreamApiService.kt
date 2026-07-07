package com.iptvplayer.tv.data.api

import com.iptvplayer.tv.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalSerializationApi::class)
@Singleton
class XtreamApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // IPTVnator uses these fallback actions for different server implementations
    private val accountInfoActions = listOf("get_account_info", null, "get_profile")

    suspend fun getAccountInfo(credentials: XtreamCredentials): Result<XtreamAccountInfo> {
        // Try multiple action types like IPTVnator does
        for (action in accountInfoActions) {
            val result = tryGetAccountInfoWithAction(credentials, action)
            if (result.isSuccess) return result
        }
        return Result.failure(Exception("Failed to authenticate with server"))
    }

    private suspend fun tryGetAccountInfoWithAction(
        credentials: XtreamCredentials,
        action: String?
    ): Result<XtreamAccountInfo> {
        return try {
            val url = if (action != null) {
                buildUrl(credentials, action)
            } else {
                // Some servers work without action parameter
                buildUrlNoAction(credentials)
            }
            android.util.Log.d("XtreamAPI", "Calling: $url")

            val response = httpClient.get(url)
            val body = response.bodyAsText()

            android.util.Log.d("XtreamAPI", "Response status: ${response.status}")
            android.util.Log.d("XtreamAPI", "Response body: ${body.take(200)}")

            // Check for error responses
            if (body.isBlank()) {
                return Result.failure(Exception("Empty response"))
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            if (!body.trimStart().startsWith("{")) {
                return Result.failure(Exception("Not JSON"))
            }

            val accountInfo = json.decodeFromString<XtreamAccountInfo>(body)

            // Verify we got valid user info
            if (accountInfo.userInfo == null) {
                return Result.failure(Exception("No user info in response"))
            }

            Result.success(accountInfo)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "Error with action=$action: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLiveCategories(credentials: XtreamCredentials): Result<List<Category>> {
        return try {
            val response = httpClient.get(buildUrl(credentials, "get_live_categories"))
            val body = response.bodyAsText()
            val categories = json.decodeFromString<List<Category>>(body)
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVodCategories(credentials: XtreamCredentials): Result<List<Category>> {
        return try {
            val response = httpClient.get(buildUrl(credentials, "get_vod_categories"))
            val body = response.bodyAsText()
            val categories = json.decodeFromString<List<Category>>(body)
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeriesCategories(credentials: XtreamCredentials): Result<List<Category>> {
        return try {
            val response = httpClient.get(buildUrl(credentials, "get_series_categories"))
            val body = response.bodyAsText()
            val categories = json.decodeFromString<List<Category>>(body)
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveStreams(credentials: XtreamCredentials): Result<List<XtreamLiveStream>> {
        return try {
            val url = buildUrl(credentials, "get_live_streams")
            android.util.Log.d("XtreamAPI", "Fetching live streams from: $url")
            val response = httpClient.get(url)

            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            val streams = response.body<java.io.InputStream>().use { inputStream ->
                json.decodeFromStream<List<XtreamLiveStream>>(inputStream)
            }

            android.util.Log.d("XtreamAPI", "Parsed ${streams.size} live streams")
            Result.success(streams)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "Live streams fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getVodStreams(credentials: XtreamCredentials): Result<List<XtreamVodStream>> {
        return try {
            val url = buildUrl(credentials, "get_vod_streams")
            android.util.Log.d("XtreamAPI", "Fetching VOD streams from: $url")
            val response = httpClient.get(url)

            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            // Stream parse to avoid OOM
            val streams = response.body<java.io.InputStream>().use { inputStream ->
                json.decodeFromStream<List<XtreamVodStream>>(inputStream)
            }

            android.util.Log.d("XtreamAPI", "Parsed ${streams.size} VOD streams")
            Result.success(streams)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "VOD fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSeriesStreams(credentials: XtreamCredentials): Result<List<XtreamSeriesItem>> {
        return try {
            val url = buildUrl(credentials, "get_series")
            android.util.Log.d("XtreamAPI", "Fetching series from: $url")
            val response = httpClient.get(url)

            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            val series = response.body<java.io.InputStream>().use { inputStream ->
                json.decodeFromStream<List<XtreamSeriesItem>>(inputStream)
            }

            android.util.Log.d("XtreamAPI", "Parsed ${series.size} series")
            Result.success(series)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "Series fetch error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSeriesInfo(credentials: XtreamCredentials, seriesId: Int): Result<XtreamSeriesInfo> {
        return try {
            val url = buildUrlWithParam(credentials, "get_series_info", "series_id", seriesId.toString())
            android.util.Log.d("XtreamAPI", "Getting series info: $url")
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            android.util.Log.d("XtreamAPI", "Series info response: ${body.take(500)}")
            val seriesInfo = json.decodeFromString<XtreamSeriesInfo>(body)
            Result.success(seriesInfo)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "Error getting series info", e)
            Result.failure(e)
        }
    }

    suspend fun getVodInfo(credentials: XtreamCredentials, vodId: Int): Result<XtreamVodInfo> {
        return try {
            val url = buildUrlWithParam(credentials, "get_vod_info", "vod_id", vodId.toString())
            android.util.Log.d("XtreamAPI", "Getting VOD info: $url")
            val response = httpClient.get(url)
            val body = response.bodyAsText()
            android.util.Log.d("XtreamAPI", "VOD info response: ${body.take(500)}")
            val vodInfo = json.decodeFromString<XtreamVodInfo>(body)
            Result.success(vodInfo)
        } catch (e: Exception) {
            android.util.Log.e("XtreamAPI", "Error getting VOD info", e)
            Result.failure(e)
        }
    }

    private fun buildUrl(credentials: XtreamCredentials, action: String): String {
        val baseUrl = normalizeServerUrl(credentials.serverUrl)
        return URLBuilder(baseUrl).apply {
            appendPathSegments("player_api.php")
            parameters.append("username", credentials.username.trim())
            parameters.append("password", credentials.password.trim())
            parameters.append("action", action)
        }.buildString()
    }

    private fun buildUrlNoAction(credentials: XtreamCredentials): String {
        val baseUrl = normalizeServerUrl(credentials.serverUrl)
        return URLBuilder(baseUrl).apply {
            appendPathSegments("player_api.php")
            parameters.append("username", credentials.username.trim())
            parameters.append("password", credentials.password.trim())
        }.buildString()
    }

    private fun buildUrlWithParam(credentials: XtreamCredentials, action: String, paramName: String, paramValue: String): String {
        val baseUrl = normalizeServerUrl(credentials.serverUrl)
        return URLBuilder(baseUrl).apply {
            appendPathSegments("player_api.php")
            parameters.append("username", credentials.username.trim())
            parameters.append("password", credentials.password.trim())
            parameters.append("action", action)
            parameters.append(paramName, paramValue)
        }.buildString()
    }

    private fun normalizeServerUrl(url: String): String {
        var normalized = url.trim().replace(" ", "")  // Remove ALL spaces
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        normalized = normalized.trimEnd('/')
        if (normalized.endsWith("/player_api.php")) {
            normalized = normalized.removeSuffix("/player_api.php")
        }
        if (normalized.endsWith("/get.php")) {
            normalized = normalized.removeSuffix("/get.php")
        }
        // Remove trailing /c path if present (some URLs include this)
        if (normalized.endsWith("/c")) {
            normalized = normalized.removeSuffix("/c")
        }
        return normalized
    }

    // Data class to return both account info and working credentials
    data class AccountResult(
        val accountInfo: XtreamAccountInfo,
        val workingCredentials: XtreamCredentials
    )

    // Helper to try with common ports if base URL fails
    suspend fun tryGetAccountInfoWithPorts(credentials: XtreamCredentials): Result<AccountResult> {
        // First try the URL as-is
        val result = getAccountInfo(credentials)
        if (result.isSuccess) {
            return Result.success(AccountResult(result.getOrThrow(), credentials))
        }

        val baseUrl = normalizeServerUrl(credentials.serverUrl)

        // Try HTTPS version first
        if (baseUrl.startsWith("http://")) {
            val httpsUrl = baseUrl.replace("http://", "https://")
            val httpsCreds = credentials.copy(serverUrl = httpsUrl)
            android.util.Log.d("XtreamAPI", "Trying HTTPS: $httpsUrl")
            val httpsResult = getAccountInfo(httpsCreds)
            if (httpsResult.isSuccess) {
                return Result.success(AccountResult(httpsResult.getOrThrow(), httpsCreds))
            }
        }

        // If no port in URL, try common Xtream ports
        if (!baseUrl.contains(Regex(":\\d+"))) {
            val portsToTry = listOf(
                "8080" to "http",
                "25461" to "http",
                "8000" to "http",
                "443" to "https"
            )
            for ((port, protocol) in portsToTry) {
                val host = baseUrl.removePrefix("http://").removePrefix("https://")
                val urlWithPort = "$protocol://$host:$port"
                val credWithPort = credentials.copy(serverUrl = urlWithPort)
                android.util.Log.d("XtreamAPI", "Trying $protocol port $port: $urlWithPort")
                val portResult = getAccountInfo(credWithPort)
                if (portResult.isSuccess) {
                    return Result.success(AccountResult(portResult.getOrThrow(), credWithPort))
                }
            }
        }

        return Result.failure(Exception("Could not connect. Check server URL, port, and credentials."))
    }

    companion object {
        fun buildLiveStreamUrl(credentials: XtreamCredentials, streamId: Int, format: String = "m3u8"): String {
            val baseUrl = credentials.serverUrl.trim().trimEnd('/')
            return "$baseUrl/live/${credentials.username.trim()}/${credentials.password.trim()}/$streamId.$format"
        }

        fun buildVodStreamUrl(credentials: XtreamCredentials, streamId: Int, extension: String): String {
            val baseUrl = credentials.serverUrl.trim().trimEnd('/')
            return "$baseUrl/movie/${credentials.username.trim()}/${credentials.password.trim()}/$streamId.$extension"
        }

        fun buildSeriesStreamUrl(credentials: XtreamCredentials, episodeId: Int, extension: String): String {
            val baseUrl = credentials.serverUrl.trim().trimEnd('/')
            return "$baseUrl/series/${credentials.username.trim()}/${credentials.password.trim()}/$episodeId.$extension"
        }
    }
}
