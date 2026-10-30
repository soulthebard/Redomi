package com.acszo.redomi.links

import com.acszo.redomi.parser.MusicLinkParseResult
import com.acszo.redomi.parser.MusicLinkParser
import com.acszo.redomi.parser.MusicService

/**
 * Removes known tracking parameters from outbound music URLs. Rules are deliberately narrow:
 *
 * - Only a single http(s) URL of a known music service host is touched; anything else, including
 *   shared text, unknown hosts and look-alike hosts, is returned as it is.
 * - Shortened links that need a network round trip to resolve (spotify.link, on.soundcloud.com,
 *   deezer.page.link, ...) are never rewritten.
 * - A URL carrying a signature or token parameter is returned untouched, because dropping any
 *   other parameter could invalidate it.
 * - Only the parameters listed in [shouldRemove] go. Everything else, including unknown,
 *   affiliate and referral parameters, stays in its original order and encoding, and the path and
 *   fragment are never modified.
 *
 * Cleaning is idempotent. Pure string handling, no Android classes and no network access.
 */
object LinkCleaner {

    private val SHORT_LINK_HOSTS = setOf(
        "spotify.link", "spotify.app.link", "on.soundcloud.com", "link.deezer.com",
        "deezer.page.link", "amzn.to", "amzn.eu", "a.co"
    )

    // YouTube also serves redirects and attribution links whose parameters carry other URLs.
    private val YOUTUBE_CLEANABLE_PATHS = setOf(
        "watch", "shorts", "playlist", "embed", "live", "v", "channel", "browse"
    )

    private val SIGNATURE_KEYS = setOf(
        "sig", "signature", "hmac", "token", "access_token", "auth", "secret_token",
        "expires", "expire", "x-amz-signature", "x-amz-credential"
    )

    private val APPLE_MUSIC_TRACKING = setOf("uo", "itscg", "itsct")

    /** Returns [url] without known tracking parameters, or [url] itself if nothing applies. */
    fun clean(url: String): String {
        val parts = split(url) ?: return url
        val query = parts.query ?: return url
        if (parts.host in SHORT_LINK_HOSTS || SHORT_LINK_HOSTS.any { parts.host.endsWith(".$it") }) return url

        val service = serviceOf(url) ?: return url
        if (service.isYouTube() && !parts.isCleanableYouTubePath()) return url

        val tokens = query.split('&')
        if (tokens.any { it.substringBefore('=').lowercase() in SIGNATURE_KEYS }) return url

        val kept = tokens.filterNot {
            shouldRemove(service, it.substringBefore('='), it.substringAfter('=', ""))
        }
        if (kept.size == tokens.size) return url

        val newQuery = if (kept.all { it.isEmpty() }) "" else "?" + kept.joinToString("&")
        return parts.base + newQuery + parts.fragment
    }

    /** The one place the "Clean Links" setting is applied to an outbound URL. */
    fun forOutbound(url: String, cleanLinks: Boolean): String = if (cleanLinks) clean(url) else url

    /**
     * A form of [url] that is safe to log: scheme and host, parameter names only, no path, no
     * parameter values and no fragment.
     */
    fun redactForLog(url: String): String {
        val parts = split(url) ?: return "<not an http(s) url>"
        val scheme = url.substringBefore("://").lowercase()
        val keys = parts.query
            ?.split('&')
            ?.map { it.substringBefore('=') }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val query = if (keys.isEmpty()) "" else keys.joinToString("&", prefix = "?") { "$it=…" }
        return "$scheme://${parts.host}/…$query"
    }

    private fun shouldRemove(service: MusicService, key: String, value: String): Boolean {
        val lowerKey = key.lowercase()
        if (lowerKey.startsWith("utm_") || lowerKey == "fbclid" || lowerKey == "gclid") return true

        return when (service) {
            MusicService.SPOTIFY, MusicService.SOUNDCLOUD -> key == "si"
            // YouTube's share sheet now writes the same tracking token as `is` instead of `si`.
            MusicService.YOUTUBE, MusicService.YOUTUBE_MUSIC -> key == "si" || key == "is" || key == "feature"
            MusicService.APPLE_MUSIC -> key in APPLE_MUSIC_TRACKING
            // Amazon's share sheet tags links with ref=dm_sh_...; other ref values are left alone.
            MusicService.AMAZON_MUSIC -> key == "ref" && value.startsWith("dm_sh_")
            else -> false
        }
    }

    /** Identifies the service only when [url] is exactly one URL, not text containing one. */
    private fun serviceOf(url: String): MusicService? = when (val parsed = MusicLinkParser.parse(url)) {
        is MusicLinkParseResult.Recognized -> parsed.link.service.takeIf { parsed.sourceUrl == url }
        is MusicLinkParseResult.Unsupported -> parsed.service.takeIf { parsed.url == url }
        is MusicLinkParseResult.Invalid -> null
    }

    private fun MusicService.isYouTube() = this == MusicService.YOUTUBE || this == MusicService.YOUTUBE_MUSIC

    private fun UrlParts.isCleanableYouTubePath(): Boolean {
        if (host == "youtu.be") return true
        val first = pathSegments.firstOrNull() ?: return false
        return first in YOUTUBE_CLEANABLE_PATHS || first.startsWith("@")
    }

    private class UrlParts(
        val host: String,
        val pathSegments: List<String>,
        /** Everything before the `?`, exactly as given. */
        val base: String,
        /** The raw text after the `?`, or null if there is none. */
        val query: String?,
        /** The `#...` part exactly as given, or empty. */
        val fragment: String
    )

    private fun split(url: String): UrlParts? {
        if (url.any { it.isWhitespace() }) return null
        val prefixLength = when {
            url.startsWith("https://", ignoreCase = true) -> 8
            url.startsWith("http://", ignoreCase = true) -> 7
            else -> return null
        }

        val fragmentStart = url.indexOf('#').let { if (it < 0) url.length else it }
        val withoutFragment = url.substring(0, fragmentStart)
        val queryStart = withoutFragment.indexOf('?')
        val base = if (queryStart < 0) withoutFragment else withoutFragment.substring(0, queryStart)
        val query = if (queryStart < 0) null else withoutFragment.substring(queryStart + 1)

        val afterScheme = base.substring(prefixLength)
        val pathStart = afterScheme.indexOf('/').let { if (it < 0) afterScheme.length else it }
        val authority = afterScheme.substring(0, pathStart)
        if ('@' in authority) return null

        return UrlParts(
            host = authority.substringBefore(':').lowercase().removeSuffix("."),
            pathSegments = afterScheme.substring(pathStart).split('/').filter { it.isNotEmpty() },
            base = base,
            query = query,
            fragment = url.substring(fragmentStart)
        )
    }

}
