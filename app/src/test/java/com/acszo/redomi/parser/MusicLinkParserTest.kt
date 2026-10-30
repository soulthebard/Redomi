package com.acszo.redomi.parser

import com.acszo.redomi.parser.MusicLinkParseResult.Invalid
import com.acszo.redomi.parser.MusicLinkParseResult.Recognized
import com.acszo.redomi.parser.MusicLinkParseResult.Unsupported
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicLinkParserTest {

    private val videoId = "dQw4w9WgXcQ"
    private val spotifyTrackId = "4uLU6hMCjMI75M1A2tKUQC"
    private val spotifyAlbumId = "1DFixLWuPkv3KT3TnV35m3"
    private val spotifyPlaylistId = "37i9dQZF1DXcBWIGoYBM5M"
    private val spotifyArtistId = "0TnOYISbd1XYRBk9myaseg"
    private val ytPlaylistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
    private val ytAlbumPlaylistId = "OLAK5uy_kXq1mVgB3ZRvVr6qfX7mVv5uTe0Zm3tNk"
    private val ytChannelId = "UCuAXFkgsw1L7xaCfnd5JJOw"

    private fun recognized(input: String): MusicLink {
        val result = MusicLinkParser.parse(input)
        assertTrue("Expected Recognized for <$input> but was $result", result is Recognized)
        return (result as Recognized).link
    }

    private fun invalid(input: String): InvalidReason {
        val result = MusicLinkParser.parse(input)
        assertTrue("Expected Invalid for <$input> but was $result", result is Invalid)
        return (result as Invalid).reason
    }

    private fun unsupported(input: String): Unsupported {
        val result = MusicLinkParser.parse(input)
        assertTrue("Expected Unsupported for <$input> but was $result", result is Unsupported)
        return result as Unsupported
    }

    private fun assertLink(
        link: MusicLink,
        service: MusicService,
        type: MusicContentType,
        id: String,
        canonicalUrl: String
    ) {
        assertEquals(MusicLink(service, type, id, canonicalUrl), link)
    }

    // YouTube

    @Test
    fun youtubeWatch() {
        assertLink(
            recognized("https://www.youtube.com/watch?v=$videoId"),
            MusicService.YOUTUBE, MusicContentType.VIDEO, videoId,
            "https://www.youtube.com/watch?v=$videoId"
        )
    }

    @Test
    fun youtubeHostVariantsNormalizeToSameLink() {
        val expected = recognized("https://www.youtube.com/watch?v=$videoId")
        listOf(
            "https://youtube.com/watch?v=$videoId",
            "https://m.youtube.com/watch?v=$videoId",
            "http://www.youtube.com/watch?v=$videoId",
            "HTTPS://WWW.YOUTUBE.COM/watch?v=$videoId",
            "https://www.youtube.com:443/watch?v=$videoId",
            "https://www.youtube.com./watch?v=$videoId"
        ).forEach { assertEquals(it, expected, recognized(it)) }
    }

    @Test
    fun youtubeWatchWithVideoNotFirstParameter() {
        assertEquals(videoId, recognized("https://www.youtube.com/watch?feature=share&v=$videoId").id)
    }

    @Test
    fun youtubeWatchWithPlaylistParameterKeepsTheVideo() {
        val link = recognized("https://www.youtube.com/watch?v=$videoId&list=$ytPlaylistId&index=3")
        assertEquals(MusicContentType.VIDEO, link.contentType)
        assertEquals(videoId, link.id)
    }

    @Test
    fun youtubeShortsAndEmbed() {
        assertEquals(videoId, recognized("https://www.youtube.com/shorts/$videoId").id)
        assertEquals(videoId, recognized("https://www.youtube.com/embed/$videoId").id)
    }

    @Test
    fun youtubePlaylist() {
        assertLink(
            recognized("https://www.youtube.com/playlist?list=$ytPlaylistId"),
            MusicService.YOUTUBE, MusicContentType.PLAYLIST, ytPlaylistId,
            "https://www.youtube.com/playlist?list=$ytPlaylistId"
        )
    }

    @Test
    fun youtubeAlbumPlaylistIsOnlyAnAlbumOnYouTubeMusic() {
        assertEquals(
            MusicContentType.PLAYLIST,
            recognized("https://www.youtube.com/playlist?list=$ytAlbumPlaylistId").contentType
        )
    }

    @Test
    fun youtubeChannelAndHomepageAreUnsupportedContent() {
        listOf(
            "https://www.youtube.com/channel/$ytChannelId",
            "https://www.youtube.com/@someone",
            "https://www.youtube.com/"
        ).forEach {
            val result = unsupported(it)
            assertEquals(it, UnsupportedReason.UNSUPPORTED_CONTENT_TYPE, result.reason)
            assertEquals(it, MusicService.YOUTUBE, result.service)
        }
    }

    // Shortened YouTube URLs

    @Test
    fun youtuBeShortLink() {
        assertLink(
            recognized("https://youtu.be/$videoId"),
            MusicService.YOUTUBE, MusicContentType.VIDEO, videoId,
            "https://www.youtube.com/watch?v=$videoId"
        )
    }

    @Test
    fun youtuBeShortLinkIgnoresTrackingAndTimestamp() {
        assertEquals(videoId, recognized("https://youtu.be/$videoId?si=AbCdEfGhIjKlMnOp&t=42").id)
        assertEquals(videoId, recognized("https://youtu.be/$videoId/?feature=shared#t=10").id)
    }

    @Test
    fun youtuBeWithoutIdIsMissingIdentifier() {
        assertEquals(InvalidReason.MISSING_IDENTIFIER, invalid("https://youtu.be/"))
        assertEquals(InvalidReason.MISSING_IDENTIFIER, invalid("https://youtu.be/?si=abc"))
    }

    // YouTube Music

    @Test
    fun youtubeMusicWatchIsAVideoBecauseItIsNotGuaranteedToBeAudio() {
        assertLink(
            recognized("https://music.youtube.com/watch?v=$videoId"),
            MusicService.YOUTUBE_MUSIC, MusicContentType.VIDEO, videoId,
            "https://music.youtube.com/watch?v=$videoId"
        )
    }

    @Test
    fun youtubeMusicWatchWithTrackingParameters() {
        val link = recognized("https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId&si=XyZ123")
        assertEquals("https://music.youtube.com/watch?v=$videoId", link.canonicalUrl)
    }

    @Test
    fun youtubeMusicPlaylistAndAlbum() {
        assertLink(
            recognized("https://music.youtube.com/playlist?list=$ytPlaylistId"),
            MusicService.YOUTUBE_MUSIC, MusicContentType.PLAYLIST, ytPlaylistId,
            "https://music.youtube.com/playlist?list=$ytPlaylistId"
        )
        assertEquals(
            MusicContentType.ALBUM,
            recognized("https://music.youtube.com/playlist?list=$ytAlbumPlaylistId").contentType
        )
        assertLink(
            recognized("https://music.youtube.com/browse/MPREb_9nqEki4ZDpp"),
            MusicService.YOUTUBE_MUSIC, MusicContentType.ALBUM, "MPREb_9nqEki4ZDpp",
            "https://music.youtube.com/browse/MPREb_9nqEki4ZDpp"
        )
    }

    @Test
    fun youtubeMusicArtist() {
        val expected = MusicLink(
            MusicService.YOUTUBE_MUSIC, MusicContentType.ARTIST, ytChannelId,
            "https://music.youtube.com/channel/$ytChannelId"
        )
        assertEquals(expected, recognized("https://music.youtube.com/channel/$ytChannelId"))
        assertEquals(expected, recognized("https://music.youtube.com/browse/$ytChannelId"))
    }

    @Test
    fun youtubeMusicOtherPagesAreUnsupportedContent() {
        listOf(
            "https://music.youtube.com/",
            "https://music.youtube.com/browse/FEmusic_home",
            "https://music.youtube.com/shorts/$videoId"
        ).forEach {
            assertEquals(it, UnsupportedReason.UNSUPPORTED_CONTENT_TYPE, unsupported(it).reason)
        }
    }

    // Spotify

    @Test
    fun spotifyTrack() {
        assertLink(
            recognized("https://open.spotify.com/track/$spotifyTrackId"),
            MusicService.SPOTIFY, MusicContentType.TRACK, spotifyTrackId,
            "https://open.spotify.com/track/$spotifyTrackId"
        )
    }

    @Test
    fun spotifyTrackWithTrackingParameters() {
        val link = recognized("https://open.spotify.com/track/$spotifyTrackId?si=0123456789abcdef&utm_source=copy-link&context=spotify%3Aalbum%3A$spotifyAlbumId")
        assertEquals("https://open.spotify.com/track/$spotifyTrackId", link.canonicalUrl)
    }

    @Test
    fun spotifyLocalePrefixAndEmbed() {
        assertEquals(spotifyTrackId, recognized("https://open.spotify.com/intl-de/track/$spotifyTrackId").id)
        assertEquals(spotifyTrackId, recognized("https://open.spotify.com/intl-pt-br/track/$spotifyTrackId").id)
        assertEquals(spotifyTrackId, recognized("https://open.spotify.com/embed/track/$spotifyTrackId").id)
    }

    @Test
    fun spotifyAlbumPlaylistAndArtist() {
        assertLink(
            recognized("https://open.spotify.com/album/$spotifyAlbumId"),
            MusicService.SPOTIFY, MusicContentType.ALBUM, spotifyAlbumId,
            "https://open.spotify.com/album/$spotifyAlbumId"
        )
        assertLink(
            recognized("https://open.spotify.com/playlist/$spotifyPlaylistId"),
            MusicService.SPOTIFY, MusicContentType.PLAYLIST, spotifyPlaylistId,
            "https://open.spotify.com/playlist/$spotifyPlaylistId"
        )
        assertLink(
            recognized("https://open.spotify.com/artist/$spotifyArtistId"),
            MusicService.SPOTIFY, MusicContentType.ARTIST, spotifyArtistId,
            "https://open.spotify.com/artist/$spotifyArtistId"
        )
    }

    @Test
    fun spotifyNonMusicContentIsUnsupportedContent() {
        listOf(
            "https://open.spotify.com/episode/$spotifyTrackId",
            "https://open.spotify.com/show/$spotifyTrackId",
            "https://open.spotify.com/user/someone",
            "https://open.spotify.com/"
        ).forEach {
            val result = unsupported(it)
            assertEquals(it, UnsupportedReason.UNSUPPORTED_CONTENT_TYPE, result.reason)
            assertEquals(it, MusicService.SPOTIFY, result.service)
        }
    }

    // Shared text

    @Test
    fun linkEmbeddedInSharedText() {
        val link = recognized("Check out this song! https://open.spotify.com/track/$spotifyTrackId?si=abc123 so good")
        assertEquals(spotifyTrackId, link.id)
    }

    @Test
    fun linkOnItsOwnLineAfterTitle() {
        val link = recognized("Never Gonna Give You Up\nhttps://youtu.be/$videoId?si=abc\n")
        assertEquals(videoId, link.id)
    }

    @Test
    fun surroundingPunctuationIsNotPartOfTheLink() {
        listOf(
            "(https://youtu.be/$videoId)",
            "<https://youtu.be/$videoId>",
            "\"https://youtu.be/$videoId\"",
            "Listen: https://youtu.be/$videoId.",
            "https://youtu.be/$videoId!?"
        ).forEach { assertEquals(it, videoId, recognized(it).id) }
    }

    @Test
    fun firstRecognizedLinkWinsOverEarlierUnsupportedOnes() {
        val link = recognized("https://example.com/a https://deezer.com/track/1 https://youtu.be/$videoId")
        assertEquals(videoId, link.id)
    }

    @Test
    fun leadingAndTrailingWhitespaceIsIgnored() {
        assertEquals(videoId, recognized("  \n\thttps://youtu.be/$videoId \r\n").id)
    }

    // Unsupported services

    @Test
    fun knownServicesWithoutAParserAreLabeled() {
        val deezer = unsupported("https://www.deezer.com/track/3135556")
        assertEquals(MusicService.DEEZER, deezer.service)
        assertEquals(UnsupportedReason.SERVICE_NOT_SUPPORTED_YET, deezer.reason)

        assertEquals(MusicService.APPLE_MUSIC, unsupported("https://music.apple.com/us/album/x/123?i=456").service)
        assertEquals(MusicService.TIDAL, unsupported("https://tidal.com/browse/track/123").service)
        assertEquals(MusicService.SOUNDCLOUD, unsupported("https://soundcloud.com/artist/track").service)
        assertEquals(MusicService.ANGHAMI, unsupported("https://play.anghami.com/song/1").service)
    }

    @Test
    fun regionalHostsOfKnownServicesAreLabeled() {
        assertEquals(MusicService.AMAZON_MUSIC, unsupported("https://music.amazon.de/albums/B000000000").service)
        assertEquals(MusicService.YANDEX_MUSIC, unsupported("https://music.yandex.ru/track/609676").service)
        assertEquals(MusicService.NAPSTER, unsupported("https://play.napster.com/track/tra.416878691").service)
        assertNull(unsupported("https://music.amazon.evil.example/albums/B000000000").service)
    }

    @Test
    fun unknownHostsAreUnsupportedServices() {
        listOf("https://example.com/track/1", "https://bandcamp.com/album/x", "http://localhost:80/").forEach {
            val result = unsupported(it)
            assertNull(it, result.service)
            assertEquals(it, UnsupportedReason.UNKNOWN_SERVICE, result.reason)
        }
    }

    @Test
    fun unsupportedResultKeepsTheOriginalUrl() {
        assertEquals("https://example.com/x?y=1", unsupported("see https://example.com/x?y=1.").url)
    }

    // Missing or invalid identifiers

    @Test
    fun missingIdentifiers() {
        listOf(
            "https://www.youtube.com/watch",
            "https://www.youtube.com/watch?v=",
            "https://www.youtube.com/watch?list=$ytPlaylistId",
            "https://www.youtube.com/playlist",
            "https://www.youtube.com/shorts/",
            "https://music.youtube.com/watch?x=1",
            "https://music.youtube.com/channel",
            "https://music.youtube.com/browse",
            "https://open.spotify.com/track",
            "https://open.spotify.com/track/",
            "https://open.spotify.com/track/?si=abc"
        ).forEach { assertEquals(it, InvalidReason.MISSING_IDENTIFIER, invalid(it)) }
    }

    @Test
    fun invalidIdentifiers() {
        listOf(
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/watch?v=${videoId}X",
            "https://www.youtube.com/watch?v=dQw4w9WgXc!",
            "https://www.youtube.com/watch?v=dQw4w9Wg%20Q",
            "https://youtu.be/not-a-video-id",
            "https://www.youtube.com/playlist?list=tooshort",
            "https://music.youtube.com/channel/UCtooShort",
            "https://music.youtube.com/browse/MPREb_",
            "https://open.spotify.com/track/short",
            "https://open.spotify.com/track/${spotifyTrackId}0",
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKU_!",
            "https://open.spotify.com/track/../../etc/passwd"
        ).forEach { assertEquals(it, InvalidReason.INVALID_IDENTIFIER, invalid(it)) }
    }

    // Hostname spoofing

    @Test
    fun lookAlikeHostsAreNeverRecognized() {
        listOf(
            "https://youtube.com.evil.example/watch?v=$videoId",
            "https://www.youtube.com.evil.example/watch?v=$videoId",
            "https://evil.example/youtube.com/watch?v=$videoId",
            "https://evil.example/?u=https://www.youtube.com/watch?v=$videoId",
            "https://notyoutube.com/watch?v=$videoId",
            "https://youtube.com-evil.example/watch?v=$videoId",
            "https://music.youtube.com.evil.example/watch?v=$videoId",
            "https://youtu.be.evil.example/$videoId",
            "https://open.spotify.com.evil.example/track/$spotifyTrackId",
            "https://evilopen.spotify.com/track/$spotifyTrackId",
            "https://spotify.com/track/$spotifyTrackId",
            "https://deezer.com.evil.example/track/1"
        ).forEach {
            val result = unsupported(it)
            assertNull(it, result.service)
            assertEquals(it, UnsupportedReason.UNKNOWN_SERVICE, result.reason)
        }
    }

    @Test
    fun credentialsInTheAuthorityAreRejected() {
        listOf(
            "https://youtube.com@evil.example/watch?v=$videoId",
            "https://www.youtube.com@evil.example/",
            "https://evil.example@www.youtube.com/watch?v=$videoId",
            "https://user:pass@open.spotify.com/track/$spotifyTrackId"
        ).forEach { assertEquals(it, InvalidReason.MALFORMED_URL, invalid(it)) }
    }

    @Test
    fun backslashesAreRejectedBecauseBrowsersTreatThemAsSlashes() {
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://evil.example\\.youtube.com/watch?v=$videoId"))
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://www.youtube.com\\@evil.example/"))
    }

    @Test
    fun nonAsciiAndEncodedHostsAreRejected() {
        listOf(
            "https://www.yοutube.com/watch?v=$videoId", // Greek omicron
            "https://www.youKube.com/watch?v=$videoId", // Kelvin sign, lowercases to ASCII k
            "https://www.youtube.com%2eevil.example/watch?v=$videoId",
            "https://www.youtube.com%00.evil.example/"
        ).forEach { assertEquals(it, InvalidReason.MALFORMED_URL, invalid(it)) }
    }

    @Test
    fun unusualPortsAreRejected() {
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://www.youtube.com:8080/watch?v=$videoId"))
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://www.youtube.com:/watch?v=$videoId"))
        assertEquals(InvalidReason.MALFORMED_URL, invalid("https://www.youtube.com:abc/watch?v=$videoId"))
    }

    // Schemes and malformed input

    @Test
    fun nonHttpSchemesAreRejected() {
        listOf(
            "ftp://www.youtube.com/watch?v=$videoId",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "javascript://youtube.com/%0Aalert(1)",
            "mailto:someone@example.com",
            "intent://www.youtube.com/watch?v=$videoId#Intent;scheme=https;end",
            "spotify://track/$spotifyTrackId",
            "spotify:track:$spotifyTrackId",
            "data:text/html,hello"
        ).forEach { assertEquals(it, InvalidReason.UNSUPPORTED_SCHEME, invalid(it)) }
    }

    @Test
    fun schemeIsMatchedWholeNotAsASuffix() {
        assertEquals(InvalidReason.UNSUPPORTED_SCHEME, invalid("evilhttps://www.youtube.com/watch?v=$videoId"))
    }

    @Test
    fun malformedUrls() {
        listOf(
            "https://",
            "https:///watch?v=$videoId",
            "https://?v=$videoId",
            "https://.",
            "https://-bad-.example/",
            "https://youtube..com/watch?v=$videoId",
            "https://[::1]/",
            "https://www.youtube.com/watch?v=$videoId\u0000",
            "http://${"a".repeat(300)}.example/"
        ).forEach { assertEquals(it, InvalidReason.MALFORMED_URL, invalid(it)) }
    }

    @Test
    fun missingSlashesAfterTheSchemeAreNotAUrl() {
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("www.youtube.com/watch?v=$videoId"))
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("youtu.be/$videoId"))
    }

    @Test
    fun overlongUrlsAreRejected() {
        assertEquals(
            InvalidReason.MALFORMED_URL,
            invalid("https://www.youtube.com/watch?v=$videoId&x=${"a".repeat(3000)}")
        )
    }

    @Test
    fun emptyAndUrlFreeInputs() {
        assertEquals(InvalidReason.EMPTY_INPUT, invalid(""))
        assertEquals(InvalidReason.EMPTY_INPUT, invalid("   \n\t "))
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("just some words, no link"))
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("Meeting at 10:30 tomorrow"))
        assertEquals(InvalidReason.NO_URL_FOUND, invalid("🎵🎶"))
    }

    // Fragments and queries

    @Test
    fun fragmentsAreDropped() {
        val link = recognized("https://open.spotify.com/track/$spotifyTrackId#section")
        assertEquals("https://open.spotify.com/track/$spotifyTrackId", link.canonicalUrl)
    }

    @Test
    fun aFragmentCannotSmuggleInAnIdentifier() {
        assertEquals(
            InvalidReason.MISSING_IDENTIFIER,
            invalid("https://www.youtube.com/watch#v=$videoId")
        )
    }

    @Test
    fun theFirstValueOfARepeatedParameterIsUsed() {
        assertEquals(videoId, recognized("https://www.youtube.com/watch?v=$videoId&v=aaaaaaaaaaa").id)
    }

    // Model

    @Test
    fun platformKeysAreUniqueAndMatchRedomiPlatforms() {
        val keys = MusicService.values().map { it.platformKey }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(
            setOf(
                "youtubeMusic", "youtube", "spotify", "deezer", "tidal", "amazonMusic",
                "appleMusic", "soundcloud", "napster", "yandex", "anghami"
            ),
            keys.toSet()
        )
    }
}
