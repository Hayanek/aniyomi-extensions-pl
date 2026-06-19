package aniyomi.lib.lycorisextractor

import android.util.Base64
import aniyomi.lib.rumbleextractor.RumbleExtractor
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import keiyoushi.utils.bodyString
import keiyoushi.utils.parseAs
import keiyoushi.utils.toJsonRequestBody
import keiyoushi.utils.useAsJsoup
import kotlinx.serialization.Serializable
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class LycorisExtractor(private val client: OkHttpClient, private val headers: Headers) {

    companion object {
        private const val GETLNKURL = "https://www.lycoris.cafe/api/watch/getVideoLink"

        private const val DECRYPTURL = "https://www.lycoris.cafe/api/watch/decryptVideoLink"

        private const val DECRYPT_API_KEY = "303a897d-sd12-41a8-84d1-5e4f5e208878"
    }

    private val rumbleExtractor by lazy { RumbleExtractor(client, headers) }

    // Credit: https://github.com/skoruppa/docchi-players/blob/main/lycoris.py
    suspend fun getVideosFromUrl(url: String, headers: Headers, prefix: String): List<Video> {
        val videos = mutableListOf<Video>()

        val document = client.newCall(
            GET(url, headers = headers),
        ).awaitSuccess().useAsJsoup()

        val episodeNumFromUrl = url.toHttpUrl().queryParameter("episode")?.toIntOrNull()

        val scripts = document.select("script[type='application/json']")

        var episodeId: String? = null
        var rumbleUrl: String? = null

        for (script in scripts) {
            val scriptText = script.data().takeIf { it.isNotBlank() } ?: continue

            val scriptBody = try {
                scriptText.parseAs<ScriptBody>()
            } catch (e: Exception) {
                continue
            }

            val body = scriptBody.body ?: continue

            val data = try {
                body.parseAs<ScriptEpisode>()
            } catch (e: Exception) {
                continue
            }

            data.episodeInfo?.let {
                episodeId = it.id?.toString()
                rumbleUrl = it.rumbleLink
            }

            if (episodeId != null) break

            val episode = data.anime?.episodes?.find { it.number == episodeNumFromUrl }
            if (episode != null) {
                episodeId = episode.id?.toString()
                rumbleUrl = episode.rumbleLink
                break
            }
        }

        if (episodeId == null) return emptyList()

        val linkList = fetchAndDecodeVideo(client, headers, episodeId)

        linkList.FHD?.let { raw ->
            extractUrls(raw).forEach { link ->
                if (checkLinks(client, link)) {
                    videos.add(Video(link, "${prefix}lycoris.cafe - 1080p", link))
                }
            }
        }
        linkList.HD?.let { raw ->
            extractUrls(raw).forEach { link ->
                if (checkLinks(client, link)) {
                    videos.add(Video(link, "${prefix}lycoris.cafe - 720p", link))
                }
            }
        }
        linkList.SD?.let { raw ->
            extractUrls(raw).forEach { link ->
                if (checkLinks(client, link)) {
                    videos.add(Video(link, "${prefix}lycoris.cafe - 480p", link))
                }
            }
        }
        if (videos.isEmpty() && !rumbleUrl.isNullOrBlank()) {
            try {
                videos.addAll(rumbleExtractor.videosFromUrl(rumbleUrl, prefix))
            } catch (e: Exception) {
                return emptyList()
            }
        }
        return videos
    }

    private suspend fun fetchAndDecodeVideo(client: OkHttpClient, headers: Headers, episodeId: String): VideoLinksApi {
        val decryptHeaders = headers.newBuilder()
            .add("x-api-key", DECRYPT_API_KEY)
            .add("Content-Type", "application/json")
            .add("User-Agent", "Mozilla/5.0 (Windows; U; MSIE 5.01; Windows NT 4.0; Netscape6/6.2; Gecko/20010726)")
            .build()

        val url: HttpUrl = GETLNKURL.toHttpUrl().newBuilder()
            .addQueryParameter("id", episodeId)
            .build()

        val encryptedText = client.newCall(GET(url))
            .awaitSuccess().bodyString()

        val textByte = encryptedText.toByteArray(Charsets.ISO_8859_1)

        val base64Data = Base64.encodeToString(textByte, Base64.DEFAULT)

        val payload = EncodedPayload(base64Data)

        return client.newCall(POST(DECRYPTURL, headers = decryptHeaders, body = payload.toJsonRequestBody()))
            .awaitSuccess()
            .parseAs<VideoLinksApi>()
    }

    private fun extractUrls(raw: String): List<String> = raw.split(Regex("""\s+or\s+"""))
        .map { it.trim() }
        .filter { it.isNotBlank() && it.contains("https://") }

    private suspend fun checkLinks(client: OkHttpClient, link: String): Boolean {
        if (!link.contains("https://")) return false

        return try {
            client.newCall(GET(link)).await().use { response ->
                response.code == 200
            }
        } catch (e: Exception) {
            false
        }
    }

    @Serializable
    data class EncodedPayload(
        val encoded: String,
    )

    @Serializable
    data class ScriptBody(
        val body: String,
    )

    @Serializable
    data class ScriptEpisode(
        val episodeInfo: EpisodeInfo? = null,
        val anime: AnimeData? = null,
    )

    @Serializable
    data class AnimeData(
        val episodes: List<EpisodeInfo>? = emptyList(),
    )

    @Serializable
    data class EpisodeInfo(
        val id: Int? = null,
        val number: Int? = null,
        val rumbleLink: String? = null,
        val FHD: String? = null,
        val HD: String? = null,
        val SD: String? = null,
    )

    @Serializable
    data class VideoLinksApi(
        val FHD: String? = null,
        val HD: String? = null,
        val SD: String? = null,
    )
}
