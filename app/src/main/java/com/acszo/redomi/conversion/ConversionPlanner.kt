package com.acszo.redomi.conversion

import com.acszo.redomi.links.LinkCleaner
import com.acszo.redomi.parser.InvalidReason
import com.acszo.redomi.parser.MusicContentType
import com.acszo.redomi.parser.MusicLinkParseResult
import com.acszo.redomi.parser.MusicLinkParser
import com.acszo.redomi.parser.MusicService
import com.acszo.redomi.parser.UnsupportedReason
import com.acszo.redomi.sharing.UniversalLinkBuilder
import com.acszo.redomi.sharing.UniversalLinkFailure
import com.acszo.redomi.sharing.UniversalLinkResult

/**
 * Plans what can be done with a music link using only the link itself: no Songlink, MusicLink API
 * or other service is contacted, and no title, artist or search term is ever invented.
 */
object ConversionPlanner {

    /**
     * Plans from [input], a music URL or shared text containing one. With [cleanLinks] on, the URLs
     * of the actions lose their known tracking parameters; [ConversionSource.originalUrl] never
     * changes, so the link as received stays available.
     */
    fun plan(input: String?, cleanLinks: Boolean = false): ConversionPlanResult {
        if (input.isNullOrBlank()) return ConversionPlanResult.Invalid(InvalidReason.EMPTY_INPUT)

        return when (val parsed = MusicLinkParser.parse(input)) {
            is MusicLinkParseResult.Recognized -> {
                val link = parsed.link
                val source = ConversionSource(
                    service = link.service,
                    contentType = link.contentType,
                    id = link.id,
                    originalUrl = parsed.sourceUrl,
                    normalizedUrl = link.canonicalUrl
                )
                plan(source, sameIdConversions(source), cleanLinks)
            }
            is MusicLinkParseResult.Unsupported -> planUnsupported(parsed, cleanLinks)
            is MusicLinkParseResult.Invalid -> ConversionPlanResult.Invalid(parsed.reason)
        }
    }

    private fun planUnsupported(parsed: MusicLinkParseResult.Unsupported, cleanLinks: Boolean): ConversionPlanResult {
        val service = parsed.service
        return when {
            service == null -> ConversionPlanResult.Unsupported(
                UnsupportedConversionReason.UNKNOWN_SERVICE, null, null, parsed.url
            )
            parsed.reason == UnsupportedReason.UNSUPPORTED_CONTENT_TYPE -> ConversionPlanResult.Unsupported(
                UnsupportedConversionReason.UNSUPPORTED_CONTENT, service, null, parsed.url
            )
            // A known service whose links are not parsed yet: keep the link as it is.
            else -> plan(
                ConversionSource(service, contentType = null, id = null, originalUrl = parsed.url, normalizedUrl = null),
                conversions = emptyList(),
                cleanLinks = cleanLinks
            )
        }
    }

    /**
     * Adds the universal resolver, which also decides whether the content is convertible at all,
     * so playlists, artists and the like never get actions.
     */
    private fun plan(
        source: ConversionSource,
        conversions: List<ConversionAction>,
        cleanLinks: Boolean
    ): ConversionPlanResult {
        val universal = when (val result = UniversalLinkBuilder.build(source.originalUrl, cleanLinks)) {
            is UniversalLinkResult.Success -> result
            is UniversalLinkResult.Failure -> return when (result.reason) {
                UniversalLinkFailure.UNSUPPORTED_CONTENT -> ConversionPlanResult.Unsupported(
                    UnsupportedConversionReason.UNSUPPORTED_CONTENT, source.service, source.contentType, source.originalUrl
                )
                UniversalLinkFailure.UNSUPPORTED_SERVICE -> ConversionPlanResult.Unsupported(
                    UnsupportedConversionReason.UNKNOWN_SERVICE, null, null, source.originalUrl
                )
                else -> ConversionPlanResult.Invalid(InvalidReason.MALFORMED_URL)
            }
        }

        val actions = buildList {
            add(ConversionAction.Original(source.service, LinkCleaner.forOutbound(source.originalUrl, cleanLinks)))
            addAll(conversions)
            add(ConversionAction.UniversalResolver(universal.universalUrl))
            // Only YouTube and YouTube Music share identifiers; everything else would need a lookup.
            if (source.service !in YOUTUBE_SERVICES) {
                YOUTUBE_SERVICES.forEach { add(ConversionAction.Unavailable(it, UnavailableReason.REQUIRES_LOOKUP)) }
            }
        }
        return ConversionPlanResult.Planned(source, actions)
    }

    /**
     * A YouTube video id is the same on YouTube and YouTube Music. A valid starting time of the
     * source follows as a plain `t=<seconds>`; no other parameter is carried over.
     */
    private fun sameIdConversions(source: ConversionSource): List<ConversionAction> {
        val id = source.id
        if (id == null || source.contentType != MusicContentType.VIDEO || source.service !in YOUTUBE_SERVICES) {
            return emptyList()
        }
        val target = if (source.service == MusicService.YOUTUBE) MusicService.YOUTUBE_MUSIC else MusicService.YOUTUBE
        val start = YouTubeTimestamp.fromUrl(source.originalUrl)?.let { "&t=$it" }.orEmpty()
        return listOf(ConversionAction.SameIdConversion(target, watchUrl(target, id) + start))
    }

    private fun watchUrl(service: MusicService, videoId: String) = when (service) {
        MusicService.YOUTUBE_MUSIC -> "https://music.youtube.com/watch?v=$videoId"
        else -> "https://www.youtube.com/watch?v=$videoId"
    }

    private val YOUTUBE_SERVICES = listOf(MusicService.YOUTUBE_MUSIC, MusicService.YOUTUBE)

}
