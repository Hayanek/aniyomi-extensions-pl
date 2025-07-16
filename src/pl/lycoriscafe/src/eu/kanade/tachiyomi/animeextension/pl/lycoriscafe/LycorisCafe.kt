package eu.kanade.tachiyomi.animeextension.pl.lycoriscafe

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.parseAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LycorisCafe : ConfigurableAnimeSource, AnimeHttpSource() {

    override val name = "LycorisCafe"

    override val baseUrl = "https://www.lycoris.cafe"

    override val lang = "pl"

    override val supportsLatest = false

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    // ============================== Popular ===============================

    override fun popularAnimeRequest(page: Int) =
        GET("$baseUrl/api/search?page=$page&pageSize=8&sortField=popularity&sortDirection=desc&preferRomaji=true")

    override fun popularAnimeParse(response: Response): AnimesPage {
        val animeData = response.body.string().parseAs<DataJson>()
        val entries = animeData.data.map { animeDetail ->
            SAnime.create().apply {
                title = animeDetail.title
                url = animeDetail.title.lowercase().replace(Regex("/[^a-z0-9]+/g"), "\"-\"").replace(Regex("/[^a-z0-9]+/g"), "")
                thumbnail_url = animeDetail.poster
            }
        }
        return AnimesPage(entries, animeData.hasMore)
    }

    // =============================== Latest ===============================

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    // =============================== Search ===============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/api/search?page=$page&pageSize=8&search=$query&sortField=popularity&sortDirection=desc&preferRomaji=true")

    override fun searchAnimeParse(response: Response): AnimesPage {
        val animeData = response.body.string().parseAs<DataJson>()
        val entries = animeData.data.map { animeDetail ->
            SAnime.create().apply {
                title = animeDetail.title
                url = animeDetail.title.lowercase().replace(Regex("/[^a-z0-9]+/g"), "\"-\"").replace(Regex("/[^a-z0-9]+/g"), "")
                thumbnail_url = animeDetail.poster
            }
        }
        return AnimesPage(entries, animeData.hasMore)
    }
    // ============================== Episodes ==============================

    override fun episodeListRequest(anime: SAnime): Request =
        // GET("$baseApiUrl/v1/episodes/count/${anime.url.substringAfterLast("/")}")
        throw UnsupportedOperationException()
    override fun episodeListParse(response: Response): List<SEpisode> {
//        val episodeList: List<EpisodeList> = json.decodeFromString(response.body.string())
//        return episodeList.map { episode ->
//            SEpisode.create().apply {
//                name = "${episode.anime_episode_number.toInt()} Odcinek"
//                url = "$baseUrl/production/as/${episode.anime_id}/${episode.anime_episode_number}"
//                episode_number = episode.anime_episode_number
//            }
//        }.reversed()
        throw UnsupportedOperationException()
    }

    // =========================== Anime Details ============================

    override fun animeDetailsRequest(anime: SAnime): Request =
        // GET("$baseApiUrl/v1/series/find/${anime.url.substringAfterLast("/")}")
        throw UnsupportedOperationException()

    override fun animeDetailsParse(response: Response): SAnime {
//        val animeDetail: ApiDetail = json.decodeFromString(response.body.string())
//
//        return SAnime.create().apply {
//            title = animeDetail.title
//            description = animeDetail.description
//            genre = animeDetail.genres.joinToString(", ")
//        }
        throw UnsupportedOperationException()
    }

    // ============================ Video Links =============================

    override fun videoListRequest(episode: SEpisode): Request = // GET(
//        "$baseApiUrl/v1/episodes/find/${
//            episode.url.substringBeforeLast("/").substringAfterLast("/")
//        }/${episode.episode_number}",
//    )
        throw UnsupportedOperationException()

    // ============================= Utilities ==============================

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString("preferred_quality", "1080")!!
        val server = preferences.getString("preferred_server", "cda.pl")!!

        return this.sortedWith(
            compareBy(
                { it.quality.contains(quality) },
                { it.quality.contains(server, true) },
            ),
        ).reversed()
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val videoQualityPref = ListPreference(screen.context).apply {
            key = "preferred_quality"
            title = "Preferowana jakość"
            entries = arrayOf("1080p", "720p", "480p", "360p")
            entryValues = arrayOf("1080", "720", "480", "360")
            setDefaultValue("1080")
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(key, entry).commit()
            }
        }
        screen.addPreference(videoQualityPref)
    }

    @Serializable
    data class DataJson(
        val data: List<AnimeList>,
        val total: Int,
        val hasMore: Boolean,
    )

    @Serializable
    data class AnimeList(
        val id: Int,
        val title: String,
        val format: String,
        val genres: List<String>,
        val poster: String,
        val rating: Float,
        val season: String,
        val source: String,
        val status: String,
        val studio: String,
        val synopsis: String?,
        val seasonYear: Int,
        val englishTitle: String,
        val totalEpisodes: Int?,
    )
}
