package com.acszo.redomi.parser

/**
 * Music services Redomi knows about. [platformKey] matches the keys used by
 * [com.acszo.redomi.model.Platform] so the two can be joined later.
 */
enum class MusicService(val platformKey: String) {
    YOUTUBE_MUSIC("youtubeMusic"),
    YOUTUBE("youtube"),
    SPOTIFY("spotify"),
    DEEZER("deezer"),
    TIDAL("tidal"),
    AMAZON_MUSIC("amazonMusic"),
    APPLE_MUSIC("appleMusic"),
    SOUNDCLOUD("soundcloud"),
    NAPSTER("napster"),
    YANDEX_MUSIC("yandex"),
    ANGHAMI("anghami")
}

enum class MusicContentType {
    TRACK,
    /** A video that may or may not be a song, e.g. a regular YouTube watch link. */
    VIDEO,
    ALBUM,
    PLAYLIST,
    ARTIST
}

/**
 * A recognized music link. [canonicalUrl] is rebuilt from [service], [contentType] and [id],
 * so tracking parameters, fragments and host variants (m., www.) are gone.
 */
data class MusicLink(
    val service: MusicService,
    val contentType: MusicContentType,
    val id: String,
    val canonicalUrl: String
)

sealed interface MusicLinkParseResult {

    /** A well-formed URL of a supported service and content type. */
    data class Recognized(val link: MusicLink) : MusicLinkParseResult

    /**
     * A well-formed http(s) URL that cannot be handled. [service] is set when the host belongs
     * to a service Redomi knows about but the parser does not handle yet.
     */
    data class Unsupported(
        val url: String,
        val service: MusicService?,
        val reason: UnsupportedReason
    ) : MusicLinkParseResult

    /** Input that is not a usable link at all. */
    data class Invalid(val reason: InvalidReason) : MusicLinkParseResult
}

enum class UnsupportedReason {
    UNKNOWN_SERVICE,
    SERVICE_NOT_SUPPORTED_YET,
    UNSUPPORTED_CONTENT_TYPE
}

enum class InvalidReason {
    EMPTY_INPUT,
    NO_URL_FOUND,
    UNSUPPORTED_SCHEME,
    MALFORMED_URL,
    MISSING_IDENTIFIER,
    INVALID_IDENTIFIER
}
