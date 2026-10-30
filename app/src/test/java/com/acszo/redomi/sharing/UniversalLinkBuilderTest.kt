package com.acszo.redomi.sharing

import com.acszo.redomi.parser.MusicService
import com.acszo.redomi.sharing.UniversalLinkResult.Failure
import com.acszo.redomi.sharing.UniversalLinkResult.Success
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class UniversalLinkBuilderTest {

    private val prefix = "https://musiclink.one/resolve?q="
    private val videoId = "dQw4w9WgXcQ"
    private val spotifyTrackId = "4uLU6hMCjMI75M1A2tKUQC"
    private val spotifyAlbumId = "1DFixLWuPkv3KT3TnV35m3"
    private val spotifyPlaylistId = "37i9dQZF1DXcBWIGoYBM5M"
    private val spotifyArtistId = "0TnOYISbd1XYRBk9myaseg"

    private fun success(input: String?): Success {
        val result = UniversalLinkBuilder.build(input)
        assertTrue("Expected Success for <$input> but was $result", result is Success)
        return result as Success
    }

    private fun failure(input: String?): UniversalLinkFailure {
        val result = UniversalLinkBuilder.build(input)
        assertTrue("Expected Failure for <$input> but was $result", result is Failure)
        return (result as Failure).reason
    }

    private fun decodedQuery(universalUrl: String): String =
        URLDecoder.decode(universalUrl.removePrefix(prefix), "UTF-8")

    // Parsed services

    @Test
    fun spotifyTrack() {
        val result = success("https://open.spotify.com/track/$spotifyTrackId")
        assertEquals(
            "${prefix}https%3A%2F%2Fopen.spotify.com%2Ftrack%2F$spotifyTrackId",
            result.universalUrl
        )
        assertEquals("https://open.spotify.com/track/$spotifyTrackId", result.sourceUrl)
        assertEquals(MusicService.SPOTIFY, result.service)
    }

    @Test
    fun spotifyParametersArePreservedWhenCleanLinksIsOff() {
        val input = "https://open.spotify.com/intl-de/track/$spotifyTrackId?si=0123456789abcdef&utm_source=copy-link"
        val result = success(input)
        assertEquals(input, result.sourceUrl)
        assertEquals(
            "${prefix}https%3A%2F%2Fopen.spotify.com%2Fintl-de%2Ftrack%2F$spotifyTrackId%3Fsi%3D0123456789abcdef%26utm_source%3Dcopy-link",
            result.universalUrl
        )
    }

    @Test
    fun spotifyTrackingParametersAreRemovedWhenCleanLinksIsOn() {
        val result = UniversalLinkBuilder.build(
            "https://open.spotify.com/intl-de/track/$spotifyTrackId?si=0123456789abcdef&utm_source=copy-link",
            cleanLinks = true
        ) as Success
        assertEquals("https://open.spotify.com/intl-de/track/$spotifyTrackId", result.sourceUrl)
        assertEquals(
            "${prefix}https%3A%2F%2Fopen.spotify.com%2Fintl-de%2Ftrack%2F$spotifyTrackId",
            result.universalUrl
        )
    }

    @Test
    fun cleanLinksIsOffByDefault() {
        val input = "https://open.spotify.com/track/$spotifyTrackId?si=abc"
        assertEquals(UniversalLinkBuilder.build(input, cleanLinks = false), UniversalLinkBuilder.build(input))
    }

    @Test
    fun spotifyAlbum() {
        assertEquals(
            "${prefix}https%3A%2F%2Fopen.spotify.com%2Falbum%2F$spotifyAlbumId",
            success("https://open.spotify.com/album/$spotifyAlbumId").universalUrl
        )
    }

    @Test
    fun youtubeLinksKeepTheirFormAndParametersWhenCleanLinksIsOff() {
        assertEquals(
            "${prefix}https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D$videoId%26t%3D42s%26si%3Dabc",
            success("https://www.youtube.com/watch?v=$videoId&t=42s&si=abc").universalUrl
        )
        assertEquals(
            "${prefix}https%3A%2F%2Fyoutu.be%2F$videoId%3Fsi%3Dabc",
            success("https://youtu.be/$videoId?si=abc").universalUrl
        )
    }

    @Test
    fun youtubeCleaningKeepsVideoAndTimestampButDropsSi() {
        fun clean(input: String) = (UniversalLinkBuilder.build(input, cleanLinks = true) as Success).universalUrl

        assertEquals(
            "${prefix}https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D$videoId%26t%3D42s",
            clean("https://www.youtube.com/watch?v=$videoId&t=42s&si=abc")
        )
        assertEquals("${prefix}https%3A%2F%2Fyoutu.be%2F$videoId", clean("https://youtu.be/$videoId?si=abc"))
    }

    @Test
    fun youtubeMusicKeepsItsHostAndListParameter() {
        val input = "https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId&si=abc"

        val off = success(input)
        assertEquals(input, off.sourceUrl)
        assertEquals(MusicService.YOUTUBE_MUSIC, off.service)

        val on = UniversalLinkBuilder.build(input, cleanLinks = true) as Success
        assertEquals(
            "${prefix}https%3A%2F%2Fmusic.youtube.com%2Fwatch%3Fv%3D$videoId%26list%3DRDAMVM$videoId",
            on.universalUrl
        )
    }

    @Test
    fun linkInsideSharedText() {
        val text = "Listen to this on Spotify!\nhttps://open.spotify.com/track/$spotifyTrackId?si=abc\nEnjoy."
        assertEquals("https://open.spotify.com/track/$spotifyTrackId?si=abc", success(text).sourceUrl)
        assertEquals(
            "https://open.spotify.com/track/$spotifyTrackId",
            (UniversalLinkBuilder.build(text, cleanLinks = true) as Success).sourceUrl
        )
    }

    @Test
    fun cleaningHappensBeforeEncodingAndTheEncodingIsStillSingle() {
        val input = "https://music.apple.com/us/album/a%20b/1558533900?i=1558534271&uo=4&utm_source=x"
        val result = UniversalLinkBuilder.build(input, cleanLinks = true) as Success

        assertEquals("https://music.apple.com/us/album/a%20b/1558533900?i=1558534271", result.sourceUrl)
        assertTrue(result.universalUrl, result.universalUrl.contains("a%2520b"))
        assertFalse(result.universalUrl, result.universalUrl.contains("%252520"))
        assertEquals(result.sourceUrl, decodedQuery(result.universalUrl))
        // the outer q parameter is still the only query
        assertEquals(1, result.universalUrl.count { it == '?' })
        assertTrue(result.universalUrl.startsWith(prefix))
    }

    @Test
    fun cleaningNeverTurnsAShortLinkIntoAGuess() {
        val input = "https://spotify.link/AbCdEfG?utm_source=x"
        val result = UniversalLinkBuilder.build(input, cleanLinks = true)
        assertEquals(UniversalLinkFailure.UNSUPPORTED_SERVICE, (result as Failure).reason)
    }

    @Test
    fun recognizedLinkBeatsAnEarlierLinkOfAnotherSite() {
        val result = success("https://example.com/x https://youtu.be/$videoId")
        assertEquals(MusicService.YOUTUBE, result.service)
    }

    // Content types

    @Test
    fun playlistsArtistsAndNonMusicContentAreNotShared() {
        listOf(
            "https://open.spotify.com/playlist/$spotifyPlaylistId",
            "https://open.spotify.com/artist/$spotifyArtistId",
            "https://open.spotify.com/episode/$spotifyTrackId",
            "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
            "https://www.youtube.com/@someone",
            "https://music.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw"
        ).forEach { assertEquals(it, UniversalLinkFailure.UNSUPPORTED_CONTENT, failure(it)) }
    }

    // Other Redomi services are forwarded as given

    @Test
    fun otherServicesAreForwardedUnchanged() {
        mapOf(
            "https://www.deezer.com/track/3135556" to MusicService.DEEZER,
            "https://tidal.com/browse/track/1234567" to MusicService.TIDAL,
            "https://music.amazon.com/albums/B000000000?trackAsin=B000000001" to MusicService.AMAZON_MUSIC,
            "https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271" to MusicService.APPLE_MUSIC,
            "https://soundcloud.com/rick-astley-official/never-gonna-give-you-up-4" to MusicService.SOUNDCLOUD,
            "https://play.napster.com/track/tra.416878691" to MusicService.NAPSTER,
            "https://music.yandex.ru/track/609676" to MusicService.YANDEX_MUSIC,
            "https://play.anghami.com/song/106295734" to MusicService.ANGHAMI
        ).forEach { (url, service) ->
            val result = success(url)
            assertEquals(url, url, result.sourceUrl)
            assertEquals(url, service, result.service)
            assertEquals(url, url, decodedQuery(result.universalUrl))
        }
    }

    @Test
    fun appleMusicTrackSelectorSurvives() {
        val result = success("https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271&uo=4")
        assertEquals(
            "${prefix}https%3A%2F%2Fmusic.apple.com%2Fus%2Falbum%2Fnever-gonna-give-you-up%2F1558533900%3Fi%3D1558534271%26uo%3D4",
            result.universalUrl
        )
    }

    @Test
    fun forwardedLinkInSharedTextLosesOnlyTheSurroundingPunctuation() {
        val result = success("Try (https://soundcloud.com/artist/track?in=playlist&x=1).")
        assertEquals("https://soundcloud.com/artist/track?in=playlist&x=1", result.sourceUrl)
    }

    // Encoding

    @Test
    fun separatorsInTheSourceCannotLeakIntoTheOuterQuery() {
        val source = "https://soundcloud.com/a/b?x=1&y=2+3#frag"
        val result = success(source)
        val q = result.universalUrl.removePrefix(prefix)
        assertTrue(q, q.none { it in "&?#+/:= " })
        assertEquals(source, decodedQuery(result.universalUrl))
    }

    @Test
    fun existingPercentEscapesAreEncodedOnceAndDecodeBackUnchanged() {
        val source = "https://music.apple.com/us/album/a%20b%2Fc/1?i=2&q=%C3%A9"
        val result = success(source)
        assertTrue(result.universalUrl, result.universalUrl.contains("a%2520b%252Fc"))
        assertTrue(result.universalUrl, result.universalUrl.contains("q%3D%25C3%25A9"))
        // one level of decoding gives the exact source back: nothing was escaped twice
        assertEquals(source, decodedQuery(result.universalUrl))
    }

    @Test
    fun unicodeIsEncodedAsUtf8() {
        val source = "https://soundcloud.com/artist/東京-café"
        val result = success(source)
        assertTrue(result.universalUrl, result.universalUrl.endsWith("%E6%9D%B1%E4%BA%AC-caf%C3%A9"))
        assertFalse(result.universalUrl, result.universalUrl.contains("%25"))
        assertEquals(source, decodedQuery(result.universalUrl))
    }

    @Test
    fun percentEncodeKeepsOnlyUnreservedCharacters() {
        assertEquals("AZaz09-._~", UniversalLinkBuilder.percentEncode("AZaz09-._~"))
        assertEquals("a%20b%2Bc%26d%3De%25", UniversalLinkBuilder.percentEncode("a b+c&d=e%"))
        assertEquals("%C3%A9", UniversalLinkBuilder.percentEncode("é"))
        assertEquals("%F0%9F%8E%B5", UniversalLinkBuilder.percentEncode("🎵"))
        assertEquals("", UniversalLinkBuilder.percentEncode(""))
    }

    @Test
    fun unpairedSurrogatesAreRejectedInsteadOfBeingReplaced() {
        assertNull(UniversalLinkBuilder.percentEncode("a\uD800b"))
        assertEquals(
            UniversalLinkFailure.INVALID_LINK,
            failure("https://soundcloud.com/artist/\uD800")
        )
    }

    @Test
    fun everySuccessStartsWithTheResolveEndpointAndDecodesToItsSource() {
        listOf(
            "https://open.spotify.com/track/$spotifyTrackId",
            "https://youtu.be/$videoId",
            "https://music.youtube.com/watch?v=$videoId",
            "https://www.deezer.com/track/3135556?utm_campaign=share"
        ).forEach {
            val result = success(it)
            assertTrue(result.universalUrl, result.universalUrl.startsWith(prefix))
            assertEquals(result.sourceUrl, decodedQuery(result.universalUrl))
        }
    }

    // Absent, empty, malformed and unsupported input

    @Test
    fun absentAndEmptyInput() {
        assertEquals(UniversalLinkFailure.EMPTY_INPUT, failure(null))
        assertEquals(UniversalLinkFailure.EMPTY_INPUT, failure(""))
        assertEquals(UniversalLinkFailure.EMPTY_INPUT, failure("  \n\t "))
    }

    @Test
    fun textWithoutALink() {
        assertEquals(UniversalLinkFailure.NO_LINK_FOUND, failure("just some words"))
        assertEquals(UniversalLinkFailure.NO_LINK_FOUND, failure("youtu.be/$videoId"))
    }

    @Test
    fun malformedAndDeceptiveLinks() {
        listOf(
            "https://",
            "ftp://www.youtube.com/watch?v=$videoId",
            "javascript:alert(1)",
            "https://www.youtube.com/watch",
            "https://www.youtube.com/watch?v=short",
            "https://open.spotify.com/track/",
            "https://youtube.com@evil.example/watch?v=$videoId",
            "https://www.youtube.com:8080/watch?v=$videoId"
        ).forEach { assertEquals(it, UniversalLinkFailure.INVALID_LINK, failure(it)) }
    }

    @Test
    fun unknownAndLookAlikeHostsAreNotShared() {
        listOf(
            "https://example.com/song/1",
            "https://bandcamp.com/album/x",
            "https://youtube.com.evil.example/watch?v=$videoId",
            "https://open.spotify.com.evil.example/track/$spotifyTrackId",
            "https://deezer.com.evil.example/track/1",
            "https://evil.example/?u=https://open.spotify.com/track/$spotifyTrackId"
        ).forEach { assertEquals(it, UniversalLinkFailure.UNSUPPORTED_SERVICE, failure(it)) }
    }
}
