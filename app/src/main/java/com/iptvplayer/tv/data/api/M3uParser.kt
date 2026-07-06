package com.iptvplayer.tv.data.api

import com.iptvplayer.tv.data.model.Channel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3uParser @Inject constructor() {

    fun parse(content: String, playlistId: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var currentInfo: ExtInfInfo? = null

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("#EXTINF:") -> {
                    currentInfo = parseExtInf(trimmed)
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentInfo != null -> {
                    channels.add(
                        Channel(
                            id = UUID.randomUUID().toString(),
                            playlistId = playlistId,
                            name = currentInfo.name,
                            url = trimmed,
                            logo = currentInfo.tvgLogo,
                            group = currentInfo.groupTitle,
                            tvgId = currentInfo.tvgId,
                            tvgName = currentInfo.tvgName,
                            isRadio = currentInfo.radio == "true",
                            archiveDays = parseArchiveDays(currentInfo),
                            userAgent = currentInfo.userAgent,
                            referer = currentInfo.referer
                        )
                    )
                    currentInfo = null
                }
            }
        }

        return channels
    }

    private fun parseExtInf(line: String): ExtInfInfo {
        val attrs = mutableMapOf<String, String>()
        var name = ""

        // Parse attributes: tvg-id="xxx" tvg-name="xxx" etc.
        val attrRegex = """(\S+)=["']([^"']*)["']""".toRegex()
        attrRegex.findAll(line).forEach { match ->
            val (key, value) = match.destructured
            attrs[key.lowercase()] = value
        }

        // Get channel name (after last comma)
        val commaIndex = line.lastIndexOf(',')
        if (commaIndex != -1) {
            name = line.substring(commaIndex + 1).trim()
        }

        return ExtInfInfo(
            name = name,
            tvgId = attrs["tvg-id"],
            tvgName = attrs["tvg-name"],
            tvgLogo = attrs["tvg-logo"],
            groupTitle = attrs["group-title"],
            radio = attrs["radio"],
            tvgRec = attrs["tvg-rec"],
            catchupDays = attrs["catchup-days"],
            timeshift = attrs["timeshift"],
            userAgent = attrs["http-user-agent"],
            referer = attrs["http-referrer"] ?: attrs["http-referer"]
        )
    }

    private fun parseArchiveDays(info: ExtInfInfo): Int {
        return info.catchupDays?.toIntOrNull()
            ?: info.timeshift?.toIntOrNull()
            ?: info.tvgRec?.toIntOrNull()
            ?: 0
    }

    private data class ExtInfInfo(
        val name: String,
        val tvgId: String?,
        val tvgName: String?,
        val tvgLogo: String?,
        val groupTitle: String?,
        val radio: String?,
        val tvgRec: String?,
        val catchupDays: String?,
        val timeshift: String?,
        val userAgent: String?,
        val referer: String?
    )

    suspend fun fetchAndParse(url: String, playlistId: String, httpClient: HttpClient): Result<List<Channel>> {
        return try {
            val content: String = httpClient.get(url).body()
            Result.success(parse(content, playlistId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
