package com.example.kaspotify.data.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-accuracy online genre lookup, one song at a time.
 *
 * Pipeline per song:
 *  1. Clean the local metadata — strip "(Official Video)", "feat. X", "[Remastered]", leading
 *     track numbers, "- Topic" channel suffixes and other tag noise that poisons search terms.
 *  2. Search the iTunes catalog (free, key-less) for up to [SEARCH_LIMIT] candidates and *score*
 *     each against the cleaned artist+title (token-set similarity). A genre is only accepted from
 *     a candidate that demonstrably IS this song — not just whatever the API returned first.
 *  3. When the artist tag is missing/unknown, match by title alone with a stricter threshold and
 *     take a majority vote among strong candidates, which suppresses wrong-song matches.
 *  4. If iTunes yields no confident match, fall back to the Deezer catalog (also key-less).
 *  5. Canonicalize the raw label ("Hip-Hop/Rap" → "Hip-Hop") so playlists don't fragment.
 *
 * HTTP 403/429 rate-limit responses back off and retry; hard failures return null so the caller
 * can fall back to the file's embedded tag.
 */
@Singleton
class GenreClassifier @Inject constructor() {

    /** Best-effort genre for [artist] + [title], canonicalized, or null when no confident match. */
    suspend fun lookupGenre(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        if (cleanTitle.isBlank()) return@withContext null
        val cleanArtist = cleanArtist(artist)

        val fromITunes = searchITunes(cleanArtist, cleanTitle)
        val raw = fromITunes ?: searchDeezer(cleanArtist, cleanTitle)
        raw?.let { canonicalize(it) }?.takeIf { it.isNotBlank() }
    }

    // ---- Metadata cleaning -----------------------------------------------------------------

    private fun cleanTitle(raw: String): String = raw
        .replace(Regex("^\\s*\\d{1,3}[.\\-_)\\s]+"), "")                    // leading track numbers
        .replace(Regex("(?i)[(\\[][^)\\]]*(official|video|audio|lyric|visualizer|remaster|live|hd|hq|mv|4k)[^)\\]]*[)\\]]"), "")
        .replace(Regex("(?i)\\b(feat|ft)\\.?\\s+.*$"), "")                  // trailing feat. X
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun cleanArtist(raw: String): String {
        val a = raw
            .replace(Regex("(?i)\\s*-\\s*topic$"), "")                      // YouTube "Artist - Topic"
            .replace(Regex("(?i)\\b(feat|ft)\\.?\\s+.*$"), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (a.equals("<unknown>", ignoreCase = true) || a.equals("unknown", ignoreCase = true)) "" else a
    }

    // ---- Matching ---------------------------------------------------------------------------

    private fun tokens(s: String): Set<String> =
        s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .split(' ').filter { it.isNotBlank() }.toSet()

    /** Token-set similarity in [0,1]; containment counts as a strong match. */
    private fun similarity(a: String, b: String): Float {
        val ta = tokens(a)
        val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0f
        val inter = ta.intersect(tb).size.toFloat()
        val jaccard = inter / ta.union(tb).size
        val containment = inter / minOf(ta.size, tb.size)   // 1.0 when one is a subset of the other
        return maxOf(jaccard, containment * 0.9f)
    }

    private data class Candidate(val title: String, val artist: String, val genre: String)

    /**
     * Picks the genre from scored [candidates]. With a known artist: best combined score above
     * [MATCH_THRESHOLD]. Without: strict title score + majority vote among strong candidates.
     */
    private fun pickGenre(candidates: List<Candidate>, artist: String, title: String): String? {
        if (candidates.isEmpty()) return null
        return if (artist.isNotBlank()) {
            candidates
                .map { it to (similarity(title, it.title) * 0.6f + similarity(artist, it.artist) * 0.4f) }
                .filter { it.second >= MATCH_THRESHOLD }
                .maxByOrNull { it.second }
                ?.first?.genre
        } else {
            val strong = candidates.filter { similarity(title, it.title) >= TITLE_ONLY_THRESHOLD }
            strong.groupingBy { it.genre }.eachCount().maxByOrNull { it.value }?.key
        }
    }

    // ---- Providers ---------------------------------------------------------------------------

    private fun searchITunes(artist: String, title: String): String? {
        val term = URLEncoder.encode(listOf(artist, title).filter { it.isNotBlank() }.joinToString(" "), "UTF-8")
        val body = httpGet("https://itunes.apple.com/search?term=$term&entity=song&limit=$SEARCH_LIMIT")
            ?: return null
        val results = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return null
        val candidates = buildList {
            for (i in 0 until results.length()) {
                val o = results.optJSONObject(i) ?: continue
                val genre = o.optString("primaryGenreName").trim()
                if (genre.isEmpty() || genre.equals("Music", ignoreCase = true)) continue
                add(Candidate(o.optString("trackName"), o.optString("artistName"), genre))
            }
        }
        return pickGenre(candidates, artist, title)
    }

    /** Deezer fallback: search → best match's album → the album's first genre. */
    private fun searchDeezer(artist: String, title: String): String? {
        val q = if (artist.isNotBlank()) """artist:"$artist" track:"$title"""" else title
        val body = httpGet("https://api.deezer.com/search?q=${URLEncoder.encode(q, "UTF-8")}&limit=$SEARCH_LIMIT")
            ?: return null
        val results = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return null
        var bestAlbumId = -1L
        var bestScore = 0f
        for (i in 0 until results.length()) {
            val o = results.optJSONObject(i) ?: continue
            val rTitle = o.optString("title")
            val rArtist = o.optJSONObject("artist")?.optString("name") ?: ""
            val score = if (artist.isNotBlank()) {
                similarity(title, rTitle) * 0.6f + similarity(artist, rArtist) * 0.4f
            } else {
                similarity(title, rTitle)
            }
            val threshold = if (artist.isNotBlank()) MATCH_THRESHOLD else TITLE_ONLY_THRESHOLD
            if (score >= threshold && score > bestScore) {
                bestScore = score
                bestAlbumId = o.optJSONObject("album")?.optLong("id", -1) ?: -1
            }
        }
        if (bestAlbumId <= 0) return null
        val albumBody = httpGet("https://api.deezer.com/album/$bestAlbumId") ?: return null
        return runCatching {
            JSONObject(albumBody).optJSONObject("genres")?.optJSONArray("data")
                ?.optJSONObject(0)?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    // ---- HTTP with rate-limit backoff ----------------------------------------------------------

    private fun httpGet(url: String): String? {
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "Kaspotify-App")
                }
                try {
                    when (conn.responseCode) {
                        in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                        403, 429 -> THROTTLED
                        else -> null
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
            when {
                result === THROTTLED -> Thread.sleep(BACKOFF_MS[attempt.coerceAtMost(BACKOFF_MS.lastIndex)])
                result != null -> return result
                else -> return null   // hard failure (no network / parse) — don't spin
            }
        }
        return null
    }

    // ---- Canonicalization ----------------------------------------------------------------------

    /** Maps raw provider/tag labels to one clean canonical genre so playlists don't fragment. */
    fun canonicalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val lower = trimmed.lowercase(Locale.ROOT)
        CANONICAL[lower]?.let { return it }
        // Multi-genre labels ("Rock/Pop", "Electro; House") → first component, canonicalized.
        val first = trimmed.split(Regex("[/;,]")).first().trim()
        if (first != trimmed) return canonicalize(first)
        return trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private companion object {
        const val SEARCH_LIMIT = 8
        const val MATCH_THRESHOLD = 0.60f
        const val TITLE_ONLY_THRESHOLD = 0.85f
        const val MAX_ATTEMPTS = 3
        val BACKOFF_MS = longArrayOf(2_000, 5_000, 10_000)
        val THROTTLED = String()   // identity sentinel for 403/429 responses

        val CANONICAL: Map<String, String> = mapOf(
            "hip-hop/rap" to "Hip-Hop", "hip hop/rap" to "Hip-Hop", "rap" to "Hip-Hop",
            "hip hop" to "Hip-Hop", "hip-hop" to "Hip-Hop",
            "r&b/soul" to "R&B", "rnb" to "R&B", "r&b" to "R&B",
            "singer/songwriter" to "Singer-Songwriter",
            "electronica" to "Electronic", "electro" to "Electronic", "edm" to "Electronic",
            "dance & edm" to "Dance", "dance/electronic" to "Dance",
            "alt rock" to "Alternative", "alt. rock" to "Alternative", "alternative rock" to "Alternative",
            "indie rock" to "Indie", "indie pop" to "Indie",
            "soundtrack/film scores" to "Soundtrack", "film scores" to "Soundtrack",
            "original score" to "Soundtrack", "video game" to "Soundtrack",
            "worldwide" to "World", "world music" to "World",
            "classical crossover" to "Classical",
            "heavy metal" to "Metal", "hard rock" to "Rock",
            "country & folk" to "Country",
            "reggaeton" to "Latin", "latino" to "Latin",
            "k-pop" to "K-Pop", "kpop" to "K-Pop", "j-pop" to "J-Pop", "jpop" to "J-Pop",
            "house music" to "House", "deep house" to "House",
            "other" to "", "misc" to "", "unknown" to "", "default" to "", "genre" to ""
        )
    }
}
