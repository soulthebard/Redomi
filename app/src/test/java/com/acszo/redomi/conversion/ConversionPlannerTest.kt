package com.acszo.redomi.conversion

import com.acszo.redomi.conversion.ConversionAction.Original
import com.acszo.redomi.conversion.ConversionAction.SameIdConversion
import com.acszo.redomi.conversion.ConversionAction.Unavailable
import com.acszo.redomi.conversion.ConversionAction.UniversalResolver
import com.acszo.redomi.conversion.ConversionPlanResult.Invalid
import com.acszo.redomi.conversion.ConversionPlanResult.Planned
import com.acszo.redomi.conversion.ConversionPlanResult.Unsupported
import com.acszo.redomi.parser.InvalidReason
import com.acszo.redomi.parser.MusicContentType
import com.acszo.redomi.parser.MusicLinkParseResult
import com.acszo.redomi.parser.MusicLinkParser
import com.acszo.redomi.parser.MusicService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class ConversionPlannerTest {

    private val resolve = "https://musiclink.one/resolve?q="
    private val videoId = "dQw4w9WgXcQ"
    private val spotifyTrackId = "4uLU6hMCjMI75M1A2tKUQC"
    private val spotifyAlbumId = "1DFixLWuPkv3KT3TnV35m3"
    private val spotifyPlaylistId = "37i9dQZF1DXcBWIGoYBM5M"
    private val spotifyArtistId = "0TnOYISbd1XYRBk9myaseg"
    private val unavailableYouTube = listOf(
        Unavailable(MusicService.YOUTUBE_MUSIC, UnavailableReason.REQUIRES_LOOKUP),
        Unavailable(MusicService.YOUTUBE, UnavailableReason.REQUIRES_LOOKUP)
    )

    private fun planned(input: String?): Planned {
        val result = ConversionPlanner.plan(input)
        assertTrue("Expected Planned for <$input> but was $result", result is Planned)
        return result as Planned
    }

    private fun unsupported(input: String?): Unsupported {
        val result = ConversionPlanner.plan(input)
        assertTrue("Expected Unsupported for <$input> but was $result", result is Unsupported)
        return result as Unsupported
    }

    private fun invalid(input: String?): InvalidReason {
        val result = ConversionPlanner.plan(input)
        assertTrue("Expected Invalid for <$input> but was $result", result is Invalid)
        return (result as Invalid).reason
    }

    private fun urlOf(action: ConversionAction): String? = when (action) {
        is Original -> action.url
        is SameIdConversion -> action.url
        is UniversalResolver -> action.url
        is ConversionAction.SearchSuggestion -> action.url
        is ConversionAction.VerifiedMatch -> action.url
        is Unavailable -> null
    }

    // YouTube <-> YouTube Music

    @Test
    fun youtubeWatchOffersTheSameIdOnYouTubeMusic() {
        val plan = planned("https://www.youtube.com/watch?v=$videoId&t=42s")

        assertEquals(
            listOf(
                Original(MusicService.YOUTUBE, "https://www.youtube.com/watch?v=$videoId&t=42s"),
                SameIdConversion(MusicService.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=$videoId"),
                UniversalResolver("${resolve}https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D$videoId%26t%3D42s")
            ),
            plan.actions
        )
        assertEquals(MusicService.YOUTUBE, plan.source.service)
        assertEquals(MusicContentType.VIDEO, plan.source.contentType)
        assertEquals(videoId, plan.source.id)
    }

    @Test
    fun youtubeMusicWatchOffersTheSameIdOnYouTube() {
        val plan = planned("https://music.youtube.com/watch?v=$videoId&si=abc")

        assertEquals(
            listOf(
                Original(MusicService.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=$videoId&si=abc"),
                SameIdConversion(MusicService.YOUTUBE, "https://www.youtube.com/watch?v=$videoId"),
                UniversalResolver("${resolve}https%3A%2F%2Fmusic.youtube.com%2Fwatch%3Fv%3D$videoId%26si%3Dabc")
            ),
            plan.actions
        )
    }

    @Test
    fun shortYouTubeLinkKeepsItsOriginalAndConvertsToYouTubeMusic() {
        val plan = planned("https://youtu.be/$videoId?si=abc")

        assertEquals(Original(MusicService.YOUTUBE, "https://youtu.be/$videoId?si=abc"), plan.actions.first())
        assertEquals(
            SameIdConversion(MusicService.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=$videoId"),
            plan.actions[1]
        )
        assertEquals("https://www.youtube.com/watch?v=$videoId", plan.source.normalizedUrl)
    }

    @Test
    fun sameIdConversionsParseBackToTheSameVideoOnTheTargetService() {
        listOf(
            "https://www.youtube.com/watch?v=$videoId" to MusicService.YOUTUBE_MUSIC,
            "https://music.youtube.com/watch?v=$videoId" to MusicService.YOUTUBE
        ).forEach { (input, targetService) ->
            val conversion = planned(input).actions.filterIsInstance<SameIdConversion>().single()
            assertEquals(targetService, conversion.target)

            val reparsed = MusicLinkParser.parse(conversion.url)
            assertTrue(conversion.url, reparsed is MusicLinkParseResult.Recognized)
            val link = (reparsed as MusicLinkParseResult.Recognized).link
            assertEquals(targetService, link.service)
            assertEquals(videoId, link.id)
            assertEquals(conversion.url, link.canonicalUrl)
        }
    }

    @Test
    fun sameIdConversionsAreNotPresentedAsVerifiedMatches() {
        val conversion = planned("https://www.youtube.com/watch?v=$videoId").actions
            .filterIsInstance<SameIdConversion>().single()

        assertEquals(ActionClassification.SAME_ID_CONVERSION, conversion.classification)
        assertFalse(conversion.classification.isVerifiedMatch)
    }

    @Test
    fun youtubeAndYouTubeMusicNeverGetUnavailableEntriesForEachOther() {
        listOf("https://www.youtube.com/watch?v=$videoId", "https://music.youtube.com/watch?v=$videoId").forEach {
            assertTrue(it, planned(it).actions.none { action -> action is Unavailable })
        }
    }

    // Spotify

    @Test
    fun spotifyTrackKeepsItsOriginalAndOffersOnlyTheUniversalResolver() {
        val original = "https://open.spotify.com/track/$spotifyTrackId?si=0123456789abcdef"
        val plan = planned(original)

        assertEquals(
            listOf(
                Original(MusicService.SPOTIFY, original),
                UniversalResolver("${resolve}https%3A%2F%2Fopen.spotify.com%2Ftrack%2F$spotifyTrackId%3Fsi%3D0123456789abcdef")
            ) + unavailableYouTube,
            plan.actions
        )
        assertEquals("https://open.spotify.com/track/$spotifyTrackId", plan.source.normalizedUrl)
        assertEquals(MusicContentType.TRACK, plan.source.contentType)
    }

    @Test
    fun spotifyAlbumIsHandledLikeATrack() {
        val plan = planned("https://open.spotify.com/album/$spotifyAlbumId")

        assertEquals(
            listOf(
                Original(MusicService.SPOTIFY, "https://open.spotify.com/album/$spotifyAlbumId"),
                UniversalResolver("${resolve}https%3A%2F%2Fopen.spotify.com%2Falbum%2F$spotifyAlbumId")
            ) + unavailableYouTube,
            plan.actions
        )
    }

    @Test
    fun spotifyNeverProducesAFabricatedYouTubeOrYouTubeMusicUrl() {
        listOf(
            "https://open.spotify.com/track/$spotifyTrackId",
            "https://open.spotify.com/track/$spotifyTrackId?si=abc&utm_source=copy-link",
            "https://open.spotify.com/intl-de/track/$spotifyTrackId",
            "https://open.spotify.com/embed/track/$spotifyTrackId",
            "https://open.spotify.com/album/$spotifyAlbumId",
            "Listen to this! https://open.spotify.com/track/$spotifyTrackId?si=abc"
        ).forEach { input ->
            val plan = planned(input)
            plan.actions.forEach { action ->
                assertFalse(input, action is SameIdConversion)
                assertFalse(input, action is ConversionAction.VerifiedMatch)
                assertFalse(input, action is ConversionAction.SearchSuggestion)
                // the only URLs are the Spotify original and the encoded MusicLink resolver
                urlOf(action)?.let { url ->
                    assertFalse("$input -> $url", url.contains("youtube", ignoreCase = true))
                    assertFalse("$input -> $url", url.contains("youtu.be", ignoreCase = true))
                }
            }
            // YouTube Music is explicitly unavailable, without a URL
            assertTrue(input, Unavailable(MusicService.YOUTUBE_MUSIC, UnavailableReason.REQUIRES_LOOKUP) in plan.actions)
        }
    }

    @Test
    fun originalParametersAreKeptEverywhereWhenCleanLinksIsOff() {
        val original = "https://open.spotify.com/track/$spotifyTrackId?si=0123456789abcdef"
        val plan = planned("Check this out: $original ok?")

        assertEquals(original, plan.source.originalUrl)
        assertEquals(original, (plan.actions.first() as Original).url)
        assertTrue(urlOf(plan.actions[1])!!.contains("%3Fsi%3D"))
        // only the normalized form drops the parameter
        assertEquals("https://open.spotify.com/track/$spotifyTrackId", plan.source.normalizedUrl)
    }

    @Test
    fun cleanLinksCleansActionUrlsButKeepsTheOriginalForDiagnostics() {
        val original = "https://open.spotify.com/track/$spotifyTrackId?si=0123456789abcdef&utm_source=copy-link"
        val plan = (ConversionPlanner.plan(original, cleanLinks = true) as Planned)

        assertEquals(original, plan.source.originalUrl)
        assertEquals(
            listOf(
                Original(MusicService.SPOTIFY, "https://open.spotify.com/track/$spotifyTrackId"),
                UniversalResolver("${resolve}https%3A%2F%2Fopen.spotify.com%2Ftrack%2F$spotifyTrackId")
            ) + unavailableYouTube,
            plan.actions
        )
    }

    @Test
    fun cleanLinksKeepsContentParametersOfYouTubeLinks() {
        val plan = ConversionPlanner.plan("https://www.youtube.com/watch?v=$videoId&list=RDAMVM$videoId&t=42s&si=abc", cleanLinks = true) as Planned

        assertEquals("https://www.youtube.com/watch?v=$videoId&list=RDAMVM$videoId&t=42s", (plan.actions.first() as Original).url)
        // the same-ID conversion carries only the video, as before
        assertEquals(
            SameIdConversion(MusicService.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=$videoId"),
            plan.actions[1]
        )
    }

    // Other Redomi services

    @Test
    fun otherServicesKeepTheirOriginalUrlAndCanUseTheUniversalResolver() {
        mapOf(
            "https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271&uo=4" to MusicService.APPLE_MUSIC,
            "https://www.deezer.com/track/3135556" to MusicService.DEEZER,
            "https://tidal.com/browse/track/1234567" to MusicService.TIDAL,
            "https://soundcloud.com/rick-astley-official/never-gonna-give-you-up-4" to MusicService.SOUNDCLOUD,
            "https://play.napster.com/track/tra.416878691" to MusicService.NAPSTER,
            "https://music.yandex.ru/track/609676" to MusicService.YANDEX_MUSIC,
            "https://play.anghami.com/song/106295734" to MusicService.ANGHAMI,
            "https://music.amazon.com/albums/B000000000?trackAsin=B000000001" to MusicService.AMAZON_MUSIC
        ).forEach { (url, service) ->
            val plan = planned(url)

            assertEquals(url, service, plan.source.service)
            assertNull(url, plan.source.contentType)
            assertNull(url, plan.source.id)
            assertNull(url, plan.source.normalizedUrl)
            assertEquals(url, url, plan.source.originalUrl)

            assertEquals(url, Original(service, url), plan.actions.first())
            val resolver = plan.actions.filterIsInstance<UniversalResolver>().single()
            assertEquals(url, url, URLDecoder.decode(resolver.url.removePrefix(resolve), "UTF-8"))
            assertEquals(url, unavailableYouTube, plan.actions.filterIsInstance<Unavailable>())
            assertTrue(url, plan.actions.none { it is SameIdConversion })
        }
    }

    // Content that cannot be converted

    @Test
    fun playlistsAndArtistsAreUnsupportedButKeepTheirUrl() {
        mapOf(
            "https://open.spotify.com/playlist/$spotifyPlaylistId" to MusicContentType.PLAYLIST,
            "https://open.spotify.com/artist/$spotifyArtistId" to MusicContentType.ARTIST,
            "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI" to MusicContentType.PLAYLIST,
            "https://music.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw" to MusicContentType.ARTIST
        ).forEach { (url, type) ->
            val result = unsupported(url)
            assertEquals(url, UnsupportedConversionReason.UNSUPPORTED_CONTENT, result.reason)
            assertEquals(url, type, result.contentType)
            assertEquals(url, url, result.sourceUrl)
        }
    }

    @Test
    fun pagesThatAreNotMusicContentAreUnsupported() {
        mapOf(
            "https://open.spotify.com/episode/$spotifyTrackId" to MusicService.SPOTIFY,
            "https://open.spotify.com/user/someone" to MusicService.SPOTIFY,
            "https://www.youtube.com/@someone" to MusicService.YOUTUBE,
            "https://music.youtube.com/browse/FEmusic_home" to MusicService.YOUTUBE_MUSIC
        ).forEach { (url, service) ->
            val result = unsupported(url)
            assertEquals(url, UnsupportedConversionReason.UNSUPPORTED_CONTENT, result.reason)
            assertEquals(url, service, result.service)
            assertEquals(url, url, result.sourceUrl)
        }
    }

    @Test
    fun unknownAndLookAlikeHostsAreUnsupportedWithoutAService() {
        listOf(
            "https://example.com/song/1",
            "https://youtube.com.evil.example/watch?v=$videoId",
            "https://open.spotify.com.evil.example/track/$spotifyTrackId",
            "https://evil.example/?u=https://www.youtube.com/watch?v=$videoId"
        ).forEach {
            val result = unsupported(it)
            assertEquals(it, UnsupportedConversionReason.UNKNOWN_SERVICE, result.reason)
            assertNull(it, result.service)
            assertNull(it, result.contentType)
            assertEquals(it, it, result.sourceUrl)
        }
    }

    // Invalid input

    @Test
    fun absentAndEmptyInput() {
        assertEquals(InvalidReason.EMPTY_INPUT, invalid(null))
        assertEquals(InvalidReason.EMPTY_INPUT, invalid(""))
        assertEquals(InvalidReason.EMPTY_INPUT, invalid(" \n\t"))
    }

    @Test
    fun invalidInputKeepsTheParserReason() {
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("just some words"))
        assertEquals(InvalidReason.UNSUPPORTED_SCHEME, invalid("ftp://www.youtube.com/watch?v=$videoId"))
        assertEquals(InvalidReason.UNSUPPORTED_SCHEME, invalid("javascript:alert(1)"))
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://"))
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://youtube.com@evil.example/watch?v=$videoId"))
        assertEquals(InvalidReason.MISSING_IDENTIFIER, invalid("https://www.youtube.com/watch"))
        assertEquals(InvalidReason.INVALID_IDENTIFIER, invalid("https://open.spotify.com/track/short"))
    }

    @Test
    fun linksWithUnpairedSurrogatesAreInvalidInsteadOfRewritten() {
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://soundcloud.com/artist/\uD800"))
    }

    // Invariants

    @Test
    fun noPlanClaimsAVerifiedMatchOrInventsASearch() {
        listOf(
            "https://www.youtube.com/watch?v=$videoId",
            "https://music.youtube.com/watch?v=$videoId",
            "https://youtu.be/$videoId",
            "https://open.spotify.com/track/$spotifyTrackId",
            "https://open.spotify.com/album/$spotifyAlbumId",
            "https://www.deezer.com/track/3135556"
        ).forEach {
            val classifications = planned(it).actions.map { action -> action.classification }
            assertFalse(it, ActionClassification.VERIFIED_MATCH in classifications)
            assertFalse(it, ActionClassification.SEARCH_SUGGESTION in classifications)
            assertTrue(it, classifications.first() == ActionClassification.ORIGINAL)
            assertEquals(it, 1, classifications.count { c -> c == ActionClassification.UNIVERSAL_RESOLVER })
        }
    }

    @Test
    fun onlyTheReservedVerifiedMatchClassificationCountsAsVerified() {
        ActionClassification.values().forEach {
            assertEquals(it.name, it == ActionClassification.VERIFIED_MATCH, it.isVerifiedMatch)
        }
    }

    @Test
    fun theFirstConvertibleLinkInSharedTextIsPlanned() {
        val plan = planned("https://example.com/a https://open.spotify.com/track/$spotifyTrackId and https://youtu.be/$videoId")
        assertEquals(MusicService.SPOTIFY, plan.source.service)
    }
}
