package com.acszo.redomi.conversion

import com.acszo.redomi.parser.MusicContentType
import com.acszo.redomi.parser.MusicService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class SheetContentBuilderTest {

    private val resolve = "https://musiclink.one/resolve?q="
    private val videoId = "dQw4w9WgXcQ"
    private val spotifyTrackId = "4uLU6hMCjMI75M1A2tKUQC"
    private val spotifyPlaylistId = "37i9dQZF1DXcBWIGoYBM5M"
    private val everyApp = MusicService.values().map { it.platformKey }.toSet()

    private fun content(
        input: String?,
        selectedApps: Set<String> = everyApp,
        cleanLinks: Boolean = false
    ): SheetContent =
        SheetContentBuilder.build(ConversionPlanner.plan(input, cleanLinks), selectedApps, cleanLinks)

    private fun ready(
        input: String?,
        selectedApps: Set<String> = everyApp,
        cleanLinks: Boolean = false
    ): SheetContent.Ready {
        val result = content(input, selectedApps, cleanLinks)
        assertTrue("Expected Ready for <$input> but was $result", result is SheetContent.Ready)
        return result as SheetContent.Ready
    }

    private fun failed(input: String?): SheetFailure {
        val result = content(input)
        assertTrue("Expected Failed for <$input> but was $result", result is SheetContent.Failed)
        return (result as SheetContent.Failed).reason
    }

    private fun SheetContent.Ready.kinds() = destinations.map { it.kind }

    // YouTube <-> YouTube Music

    @Test
    fun youtubeShowsItsOriginalTheYouTubeMusicSameIdAndTheResolver() {
        val sheet = ready("https://www.youtube.com/watch?v=$videoId&t=42s")

        assertEquals(
            listOf(DestinationKind.ORIGINAL, DestinationKind.SAME_ID, DestinationKind.RESOLVER),
            sheet.kinds()
        )
        assertEquals(
            listOf(MusicService.YOUTUBE, MusicService.YOUTUBE_MUSIC, null),
            sheet.destinations.map { it.service }
        )
        assertEquals("https://www.youtube.com/watch?v=$videoId&t=42s", sheet.destinations[0].url)
        assertEquals("https://music.youtube.com/watch?v=$videoId&t=42", sheet.destinations[1].url)
        assertTrue(sheet.destinations[2].url.startsWith(resolve))
        assertNull(sheet.notice)
    }

    @Test
    fun youtubeMusicShowsYouTubeAsTheSameIdDestination() {
        val sheet = ready("https://music.youtube.com/watch?v=$videoId")

        assertEquals(
            listOf(MusicService.YOUTUBE_MUSIC, MusicService.YOUTUBE, null),
            sheet.destinations.map { it.service }
        )
        assertEquals("https://www.youtube.com/watch?v=$videoId", sheet.destinations[1].url)
    }

    @Test
    fun sameIdDestinationsAreNeverClassifiedAsVerifiedMatches() {
        val sameId = ready("https://youtu.be/$videoId").destinations.single { it.kind == DestinationKind.SAME_ID }

        assertEquals(ActionClassification.SAME_ID_CONVERSION, sameId.classification)
        assertFalse(sameId.classification.isVerifiedMatch)
    }

    @Test
    fun theResolverIsASeparateEntryWithoutAServiceAndUnlikeAnyStreamingService() {
        val resolver = ready("https://youtu.be/$videoId").destinations.single { it.kind == DestinationKind.RESOLVER }

        assertNull(resolver.service)
        assertEquals(ActionClassification.UNIVERSAL_RESOLVER, resolver.classification)
    }

    // Spotify and the other services

    @Test
    fun spotifyShowsOnlyItsOriginalAndTheResolver() {
        val sheet = ready("https://open.spotify.com/track/$spotifyTrackId?si=abc")

        assertEquals(listOf(DestinationKind.ORIGINAL, DestinationKind.RESOLVER), sheet.kinds())
        assertEquals("https://open.spotify.com/track/$spotifyTrackId?si=abc", sheet.destinations[0].url)
        assertEquals(MusicContentType.TRACK, sheet.source.contentType)
    }

    @Test
    fun spotifyNeverShowsAYouTubeOrYouTubeMusicDestination() {
        listOf(
            "https://open.spotify.com/track/$spotifyTrackId",
            "https://open.spotify.com/album/1DFixLWuPkv3KT3TnV35m3?si=abc"
        ).forEach {
            val sheet = ready(it)
            assertTrue(it, sheet.destinations.none { d -> d.service == MusicService.YOUTUBE || d.service == MusicService.YOUTUBE_MUSIC })
            assertTrue(it, sheet.destinations.none { d -> d.url.contains("youtube", ignoreCase = true) })
        }
    }

    @Test
    fun everyRecognizedServiceShowsItsOwnIconAndTheResolver() {
        mapOf(
            "https://open.spotify.com/track/$spotifyTrackId" to MusicService.SPOTIFY,
            "https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271" to MusicService.APPLE_MUSIC,
            "https://www.deezer.com/track/3135556" to MusicService.DEEZER,
            "https://tidal.com/browse/track/1234567" to MusicService.TIDAL,
            "https://soundcloud.com/rick-astley-official/never-gonna-give-you-up-4" to MusicService.SOUNDCLOUD,
            "https://play.napster.com/track/tra.416878691" to MusicService.NAPSTER,
            "https://music.yandex.ru/track/609676" to MusicService.YANDEX_MUSIC,
            "https://play.anghami.com/song/106295734" to MusicService.ANGHAMI,
            "https://music.amazon.com/albums/B000000000?trackAsin=B000000001" to MusicService.AMAZON_MUSIC
        ).forEach { (url, service) ->
            val sheet = ready(url)
            assertEquals(url, listOf(DestinationKind.ORIGINAL, DestinationKind.RESOLVER), sheet.kinds())
            assertEquals(url, service, sheet.destinations[0].service)
            assertEquals(url, url, sheet.destinations[0].url)
        }
    }

    // Selected apps

    @Test
    fun selectedAppsFilterTheOriginalAndTheSameIdDestinations() {
        val url = "https://www.youtube.com/watch?v=$videoId"

        assertEquals(
            listOf(DestinationKind.ORIGINAL, DestinationKind.RESOLVER),
            ready(url, selectedApps = setOf("youtube")).kinds()
        )
        assertEquals(
            listOf(DestinationKind.SAME_ID, DestinationKind.RESOLVER),
            ready(url, selectedApps = setOf("youtubeMusic")).kinds()
        )
    }

    @Test
    fun theResolverSurvivesAnySelectionOfApps() {
        listOf(
            "https://www.youtube.com/watch?v=$videoId",
            "https://open.spotify.com/track/$spotifyTrackId",
            "https://www.deezer.com/track/3135556"
        ).forEach {
            val sheet = ready(it, selectedApps = emptySet())
            assertEquals(it, listOf(DestinationKind.RESOLVER), sheet.kinds())
        }
    }

    @Test
    fun aLoneResolverIsNeverOpenedAutomatically() {
        val sheet = ready("https://open.spotify.com/track/$spotifyTrackId", selectedApps = setOf("youtube"))

        assertEquals(listOf(DestinationKind.RESOLVER), sheet.kinds())
        assertFalse(SheetContentBuilder.shouldAutoOpen(sheet.destinations))
    }

    @Test
    fun aLoneRealDestinationIsOpenedAutomaticallyAndSeveralAreNot() {
        val playlist = ready("https://open.spotify.com/playlist/$spotifyPlaylistId")
        assertTrue(SheetContentBuilder.shouldAutoOpen(playlist.destinations))

        val track = ready("https://open.spotify.com/track/$spotifyTrackId")
        assertFalse(SheetContentBuilder.shouldAutoOpen(track.destinations))
        assertFalse(SheetContentBuilder.shouldAutoOpen(emptyList()))
    }

    // Content that cannot be converted

    @Test
    fun playlistsAreOfferedAsTheirOriginalRegardlessOfSelectedApps() {
        val url = "https://open.spotify.com/playlist/$spotifyPlaylistId?si=abc"
        val sheet = ready(url, selectedApps = emptySet())

        assertEquals(SheetNotice.CANNOT_CONVERT, sheet.notice)
        assertEquals(1, sheet.destinations.size)
        assertEquals(DestinationKind.ORIGINAL, sheet.destinations[0].kind)
        assertEquals(MusicService.SPOTIFY, sheet.destinations[0].service)
        assertEquals(url, sheet.destinations[0].url)
        assertEquals(MusicContentType.PLAYLIST, sheet.source.contentType)
    }

    @Test
    fun otherKnownPagesThatCannotBeConvertedAreOfferedAsTheirOriginal() {
        listOf(
            "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
            "https://open.spotify.com/artist/0TnOYISbd1XYRBk9myaseg",
            "https://open.spotify.com/episode/$spotifyTrackId",
            "https://www.youtube.com/@someone"
        ).forEach {
            val sheet = ready(it)
            assertEquals(it, SheetNotice.CANNOT_CONVERT, sheet.notice)
            assertEquals(it, listOf(DestinationKind.ORIGINAL), sheet.kinds())
            assertEquals(it, it, sheet.destinations[0].url)
        }
    }

    @Test
    fun unknownHostsAndInvalidInputFailInsteadOfInventingDestinations() {
        listOf(
            "https://example.com/song/1",
            "https://youtube.com.evil.example/watch?v=$videoId",
            "https://open.spotify.com.evil.example/track/$spotifyTrackId"
        ).forEach { assertEquals(it, SheetFailure.UNSUPPORTED_LINK, failed(it)) }

        listOf(
            null,
            "",
            "just some words",
            "ftp://www.youtube.com/watch?v=$videoId",
            "https://www.youtube.com/watch",
            "https://open.spotify.com/track/short",
            "https://youtube.com@evil.example/watch?v=$videoId"
        ).forEach { assertEquals(it, SheetFailure.INVALID_LINK, failed(it)) }
    }

    // Clean Links

    @Test
    fun cleanLinksCleansEveryDestinationUrlIncludingYouTubeIs() {
        val sheet = ready(
            "https://youtu.be/xCUUItmmDDc?t=58&is=edqQ_EaMEl-eeORJ",
            cleanLinks = true
        )

        assertEquals("https://youtu.be/xCUUItmmDDc?t=58", sheet.destinations[0].url)
        assertEquals("https://music.youtube.com/watch?v=xCUUItmmDDc&t=58", sheet.destinations[1].url)
        assertEquals(
            "https://youtu.be/xCUUItmmDDc?t=58",
            URLDecoder.decode(sheet.destinations[2].url.removePrefix(resolve), "UTF-8")
        )
        // the link as received stays available to the planner
        assertEquals(
            "https://youtu.be/xCUUItmmDDc?t=58&is=edqQ_EaMEl-eeORJ",
            sheet.source.originalUrl
        )
    }

    @Test
    fun linksKeepTheirParametersWhenCleanLinksIsOff() {
        val url = "https://youtu.be/xCUUItmmDDc?t=58&is=edqQ_EaMEl-eeORJ"
        val sheet = ready(url)

        assertEquals(url, sheet.destinations[0].url)
        assertEquals(url, URLDecoder.decode(sheet.destinations[2].url.removePrefix(resolve), "UTF-8"))
    }

    @Test
    fun cleanLinksAlsoAppliesToLinksThatAreOnlyOpenedAsTheyAre() {
        val url = "https://open.spotify.com/playlist/$spotifyPlaylistId?si=abc"

        assertEquals(
            "https://open.spotify.com/playlist/$spotifyPlaylistId",
            ready(url, cleanLinks = true).destinations[0].url
        )
        assertEquals(url, ready(url, cleanLinks = false).destinations[0].url)
    }

    @Test
    fun theIncomingLinkIsNeverRewrittenBeforePlanning() {
        val url = "https://open.spotify.com/track/$spotifyTrackId?si=abc"
        val result = ConversionPlanner.plan(url, cleanLinks = true) as ConversionPlanResult.Planned

        assertEquals(url, result.source.originalUrl)
    }

    // Shared text

    @Test
    fun aLinkInsideSharedTextIsPlannedFromTheLinkOnly() {
        val sheet = ready("Check this out!\nhttps://open.spotify.com/track/$spotifyTrackId?si=abc\nCool, right?")

        assertEquals("https://open.spotify.com/track/$spotifyTrackId?si=abc", sheet.destinations[0].url)
    }
}
