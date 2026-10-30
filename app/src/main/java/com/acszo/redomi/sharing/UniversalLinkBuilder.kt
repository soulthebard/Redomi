package com.acszo.redomi.sharing

import com.acszo.redomi.parser.InvalidReason
import com.acszo.redomi.parser.MusicContentType
import com.acszo.redomi.parser.MusicLinkParseResult
import com.acszo.redomi.parser.MusicLinkParser
import com.acszo.redomi.parser.MusicService
import com.acszo.redomi.parser.UnsupportedReason
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * Builds `https://musiclink.one/resolve?q=<source url>` links. Nothing is requested from MusicLink:
 * the link is only constructed here and resolved by whoever opens it.
 */
object UniversalLinkBuilder {

    private const val RESOLVE_URL = "https://musiclink.one/resolve?q="
    private const val HEX = "0123456789ABCDEF"

    // MusicLink documents tracks and albums. Playlists are not documented, and artist links
    // are unconfirmed, so no universal link is built for them.
    private val SHAREABLE_TYPES = setOf(
        MusicContentType.TRACK,
        MusicContentType.VIDEO,
        MusicContentType.ALBUM
    )

    /**
     * Builds a universal link from [input], a source URL or shared text containing one.
     *
     * Links of YouTube, YouTube Music and Spotify are rebuilt from their parsed identifier, which
     * drops tracking parameters. Links of the other services Redomi supports are not parsed yet,
     * so their URL is forwarded unchanged, since parts like Apple Music's `?i=` select the track.
     */
    fun build(input: String?): UniversalLinkResult {
        if (input.isNullOrBlank()) return failure(UniversalLinkFailure.EMPTY_INPUT)

        val service: MusicService
        val sourceUrl: String
        when (val parsed = MusicLinkParser.parse(input)) {
            is MusicLinkParseResult.Recognized -> {
                if (parsed.link.contentType !in SHAREABLE_TYPES) {
                    return failure(UniversalLinkFailure.UNSUPPORTED_CONTENT)
                }
                service = parsed.link.service
                sourceUrl = parsed.link.canonicalUrl
            }
            is MusicLinkParseResult.Unsupported -> {
                service = parsed.service ?: return failure(UniversalLinkFailure.UNSUPPORTED_SERVICE)
                if (parsed.reason == UnsupportedReason.UNSUPPORTED_CONTENT_TYPE) {
                    return failure(UniversalLinkFailure.UNSUPPORTED_CONTENT)
                }
                sourceUrl = parsed.url
            }
            is MusicLinkParseResult.Invalid -> return failure(
                when (parsed.reason) {
                    InvalidReason.EMPTY_INPUT -> UniversalLinkFailure.EMPTY_INPUT
                    InvalidReason.NO_URL_FOUND -> UniversalLinkFailure.NO_LINK_FOUND
                    else -> UniversalLinkFailure.INVALID_LINK
                }
            )
        }

        val encoded = percentEncode(sourceUrl) ?: return failure(UniversalLinkFailure.INVALID_LINK)
        return UniversalLinkResult.Success(RESOLVE_URL + encoded, sourceUrl, service)
    }

    /**
     * Percent-encodes [value] as UTF-8, leaving only the unreserved characters `A-Za-z0-9-._~`.
     * Applied once to the whole source URL: the `%` of an existing escape becomes `%25`, so
     * decoding the parameter once gives back exactly the source URL. Returns null for text that
     * is not valid UTF-16 (unpaired surrogates).
     */
    internal fun percentEncode(value: String): String? {
        val bytes = try {
            Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(value))
                .toByteArray()
        } catch (_: CharacterCodingException) {
            return null
        }

        val encoded = StringBuilder(bytes.size)
        for (byte in bytes) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
                encoded.append(char)
            } else {
                encoded.append('%').append(HEX[code shr 4]).append(HEX[code and 0x0F])
            }
        }
        return encoded.toString()
    }

    private fun ByteBuffer.toByteArray(): ByteArray = ByteArray(remaining()).also { get(it) }

    private fun failure(reason: UniversalLinkFailure) = UniversalLinkResult.Failure(reason)

}

sealed interface UniversalLinkResult {

    /** [universalUrl] is safe to share; [sourceUrl] is what it resolves. */
    data class Success(
        val universalUrl: String,
        val sourceUrl: String,
        val service: MusicService
    ) : UniversalLinkResult

    data class Failure(val reason: UniversalLinkFailure) : UniversalLinkResult
}

enum class UniversalLinkFailure {
    EMPTY_INPUT,
    NO_LINK_FOUND,
    INVALID_LINK,
    UNSUPPORTED_SERVICE,
    UNSUPPORTED_CONTENT
}
