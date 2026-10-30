package com.acszo.redomi.parser

import com.acszo.redomi.parser.MusicLinkParseResult.Invalid
import com.acszo.redomi.parser.MusicLinkParseResult.Recognized
import com.acszo.redomi.parser.MusicLinkParseResult.Unsupported

/**
 * Recognizes music URLs in free text and normalizes them. Pure string handling: no Android
 * classes and no network access, so a link is never resolved or followed.
 *
 * Only http(s) URLs with an explicit scheme are considered. Hosts must match a known host exactly,
 * so look-alikes such as `youtube.com.evil.example` end up as [Unsupported], and URLs with
 * credentials, non-ASCII hosts, backslashes or unusual ports are rejected as malformed.
 */
object MusicLinkParser {

    private const val MAX_URL_LENGTH = 2048
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}>\"'"

    private val URL_CANDIDATE = Regex("""[A-Za-z][A-Za-z0-9+.\-]*://\S+""")
    private val BARE_SCHEME = Regex("""[A-Za-z][A-Za-z0-9+.\-]*:\S*""")
    private val HOST = Regex("""[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*""")
    private val PORT = Regex("""\d{1,5}""")

    private val YOUTUBE_VIDEO_ID = Regex("""[A-Za-z0-9_-]{11}""")
    private val YOUTUBE_PLAYLIST_ID = Regex("""[A-Za-z0-9_-]{13,64}""")
    private val YOUTUBE_CHANNEL_ID = Regex("""UC[A-Za-z0-9_-]{22}""")
    private val YOUTUBE_MUSIC_ALBUM_ID = Regex("""MPREb_[A-Za-z0-9_-]{6,20}""")
    private val SPOTIFY_ID = Regex("""[A-Za-z0-9]{22}""")
    private val SPOTIFY_LOCALE = Regex("""intl-[A-Za-z]{2}(-[A-Za-z]{2,4})?""")

    private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com")
    private const val YOUTUBE_MUSIC_HOST = "music.youtube.com"
    private const val YOUTUBE_SHORT_HOST = "youtu.be"
    private const val SPOTIFY_HOST = "open.spotify.com"

    // Known services without a parser yet; only used to label Unsupported results.
    private val OTHER_SERVICE_DOMAINS = mapOf(
        "deezer.com" to MusicService.DEEZER,
        "tidal.com" to MusicService.TIDAL,
        "music.amazon.com" to MusicService.AMAZON_MUSIC,
        "music.apple.com" to MusicService.APPLE_MUSIC,
        "soundcloud.com" to MusicService.SOUNDCLOUD,
        "napster.com" to MusicService.NAPSTER,
        "music.yandex.com" to MusicService.YANDEX_MUSIC,
        "music.yandex.ru" to MusicService.YANDEX_MUSIC,
        "anghami.com" to MusicService.ANGHAMI
    )

    /**
     * Parses [input], which may be a bare URL or ordinary shared text containing one or more URLs.
     * The first recognized link wins; otherwise the result for the first URL found is returned.
     */
    fun parse(input: String): MusicLinkParseResult {
        val text = input.trim()
        if (text.isEmpty()) return Invalid(InvalidReason.EMPTY_INPUT)

        val results = extractCandidates(text).map { parseUrl(it) }
        if (results.isEmpty()) return Invalid(InvalidReason.NO_URL_FOUND)
        return results.firstOrNull { it is Recognized } ?: results.first()
    }

    private fun extractCandidates(text: String): List<String> {
        val candidates = URL_CANDIDATE.findAll(text)
            .map { it.value.trimEnd { char -> char in TRAILING_PUNCTUATION } }
            .filter { it.isNotEmpty() }
            .toList()
        if (candidates.isNotEmpty()) return candidates
        // Scheme-only forms such as "javascript:alert(1)" or "mailto:a@b.c" are reported as such.
        return if (BARE_SCHEME.matches(text)) listOf(text) else emptyList()
    }

    private fun parseUrl(url: String): MusicLinkParseResult {
        if (url.length > MAX_URL_LENGTH || url.any { it <= ' ' || it == '\u007f' || it == '\\' }) {
            return Invalid(InvalidReason.MALFORMED_URL)
        }

        val schemeEnd = url.indexOf(':')
        val scheme = url.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https") return Invalid(InvalidReason.UNSUPPORTED_SCHEME)
        if (!url.startsWith("://", schemeEnd)) return Invalid(InvalidReason.MALFORMED_URL)

        val afterScheme = url.substring(schemeEnd + 3).substringBefore('#')
        val authorityEnd = afterScheme.indexOfFirst { it == '/' || it == '?' }
            .let { if (it < 0) afterScheme.length else it }
        val host = parseHost(afterScheme.substring(0, authorityEnd))
            ?: return Invalid(InvalidReason.MALFORMED_URL)

        val pathAndQuery = afterScheme.substring(authorityEnd)
        val segments = pathAndQuery.substringBefore('?').split('/').filter { it.isNotEmpty() }
        val params = parseQuery(pathAndQuery.substringAfter('?', ""))

        return when (host) {
            in YOUTUBE_HOSTS -> parseYouTube(url, segments, params, music = false)
            YOUTUBE_MUSIC_HOST -> parseYouTube(url, segments, params, music = true)
            YOUTUBE_SHORT_HOST -> withId(segments.firstOrNull(), YOUTUBE_VIDEO_ID) {
                MusicLink(MusicService.YOUTUBE, MusicContentType.VIDEO, it, "https://www.youtube.com/watch?v=$it")
            }
            SPOTIFY_HOST -> parseSpotify(url, segments)
            else -> {
                val service = otherServiceFor(host)
                val reason = if (service != null) {
                    UnsupportedReason.SERVICE_NOT_SUPPORTED_YET
                } else {
                    UnsupportedReason.UNKNOWN_SERVICE
                }
                Unsupported(url, service, reason)
            }
        }
    }

    /** Returns the lowercase ASCII host, or null if the authority is not a plain `host[:80|443]`. */
    private fun parseHost(authority: String): String? {
        if (authority.isEmpty() || '@' in authority || authority.any { it.code > 127 }) return null

        val colon = authority.lastIndexOf(':')
        if (colon >= 0) {
            val port = authority.substring(colon + 1)
            if (!PORT.matches(port) || port.trimStart('0') !in setOf("80", "443")) return null
        }

        val host = (if (colon < 0) authority else authority.substring(0, colon))
            .lowercase()
            .removeSuffix(".")
        return host.takeIf { it.length <= 253 && HOST.matches(it) }
    }

    /** Keeps the first value of each key, left undecoded; identifiers are validated strictly later. */
    private fun parseQuery(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        for (pair in query.split('&')) {
            val key = pair.substringBefore('=')
            if (key.isNotEmpty() && key !in params) params[key] = pair.substringAfter('=', "")
        }
        return params
    }

    private fun parseYouTube(
        url: String,
        segments: List<String>,
        params: Map<String, String>,
        music: Boolean
    ): MusicLinkParseResult {
        val service = if (music) MusicService.YOUTUBE_MUSIC else MusicService.YOUTUBE
        val host = if (music) YOUTUBE_MUSIC_HOST else "www.youtube.com"
        val unsupported = Unsupported(url, service, UnsupportedReason.UNSUPPORTED_CONTENT_TYPE)

        fun artist(id: String?) = withId(id, YOUTUBE_CHANNEL_ID) {
            MusicLink(service, MusicContentType.ARTIST, it, "https://$host/channel/$it")
        }

        return when (segments.firstOrNull()) {
            "watch" -> withId(params["v"], YOUTUBE_VIDEO_ID) {
                val type = if (music) MusicContentType.TRACK else MusicContentType.VIDEO
                MusicLink(service, type, it, "https://$host/watch?v=$it")
            }
            "shorts", "embed", "live", "v" -> if (music) {
                unsupported
            } else {
                withId(segments.getOrNull(1), YOUTUBE_VIDEO_ID) {
                    MusicLink(service, MusicContentType.VIDEO, it, "https://$host/watch?v=$it")
                }
            }
            "playlist" -> withId(params["list"], YOUTUBE_PLAYLIST_ID) {
                // YouTube Music exposes albums as playlists whose id starts with OLAK5uy_
                val type = if (music && it.startsWith("OLAK5uy_")) {
                    MusicContentType.ALBUM
                } else {
                    MusicContentType.PLAYLIST
                }
                MusicLink(service, type, it, "https://$host/playlist?list=$it")
            }
            "channel" -> if (music) artist(segments.getOrNull(1)) else unsupported
            "browse" -> {
                val id = segments.getOrNull(1)
                when {
                    !music -> unsupported
                    id == null -> Invalid(InvalidReason.MISSING_IDENTIFIER)
                    id.startsWith("MPREb_") -> withId(id, YOUTUBE_MUSIC_ALBUM_ID) {
                        MusicLink(service, MusicContentType.ALBUM, it, "https://$host/browse/$it")
                    }
                    id.startsWith("UC") -> artist(id)
                    else -> unsupported
                }
            }
            else -> unsupported
        }
    }

    private fun parseSpotify(url: String, segments: List<String>): MusicLinkParseResult {
        var parts = segments
        if (parts.firstOrNull()?.let { SPOTIFY_LOCALE.matches(it) } == true) parts = parts.drop(1)
        if (parts.firstOrNull() == "embed") parts = parts.drop(1)

        val type = when (parts.firstOrNull()) {
            "track" -> MusicContentType.TRACK
            "album" -> MusicContentType.ALBUM
            "playlist" -> MusicContentType.PLAYLIST
            "artist" -> MusicContentType.ARTIST
            else -> return Unsupported(url, MusicService.SPOTIFY, UnsupportedReason.UNSUPPORTED_CONTENT_TYPE)
        }
        return withId(parts.getOrNull(1), SPOTIFY_ID) {
            MusicLink(MusicService.SPOTIFY, type, it, "https://$SPOTIFY_HOST/${type.name.lowercase()}/$it")
        }
    }

    private fun otherServiceFor(host: String): MusicService? =
        OTHER_SERVICE_DOMAINS.entries
            .firstOrNull { host == it.key || host.endsWith(".${it.key}") }
            ?.value

    private fun withId(
        id: String?,
        pattern: Regex,
        build: (String) -> MusicLink
    ): MusicLinkParseResult = when {
        id == null || id.isEmpty() -> Invalid(InvalidReason.MISSING_IDENTIFIER)
        !pattern.matches(id) -> Invalid(InvalidReason.INVALID_IDENTIFIER)
        else -> Recognized(build(id))
    }

}
