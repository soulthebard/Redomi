package com.acszo.redomi.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCleanerTest {

    private val videoId = "dQw4w9WgXcQ"
    private val spotifyId = "4uLU6hMCjMI75M1A2tKUQC"
    private val spotifyAlbumId = "1DFixLWuPkv3KT3TnV35m3"
    private val ytPlaylistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"

    private fun assertCleaned(expected: String, input: String) {
        val cleaned = LinkCleaner.clean(input)
        assertEquals(input, expected, cleaned)
        // cleaning again must not change anything further
        assertEquals("not idempotent: $input", cleaned, LinkCleaner.clean(cleaned))
    }

    private fun assertUntouched(input: String) = assertCleaned(input, input)

    // Spotify

    @Test
    fun spotifyShareParametersAreRemoved() {
        assertCleaned(
            "https://open.spotify.com/track/$spotifyId",
            "https://open.spotify.com/track/$spotifyId?si=0123456789abcdef"
        )
        assertCleaned(
            "https://open.spotify.com/album/$spotifyAlbumId",
            "https://open.spotify.com/album/$spotifyAlbumId?si=abc&utm_source=copy-link&utm_medium=share"
        )
    }

    @Test
    fun spotifyContextAndUnknownParametersStay() {
        assertCleaned(
            "https://open.spotify.com/track/$spotifyId?context=spotify%3Aalbum%3A$spotifyAlbumId",
            "https://open.spotify.com/track/$spotifyId?si=abc&context=spotify%3Aalbum%3A$spotifyAlbumId"
        )
        assertCleaned(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?pi=u-AbCdEf&x=1",
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc&pi=u-AbCdEf&x=1"
        )
    }

    @Test
    fun spotifyPathIsNeverRewritten() {
        assertCleaned(
            "https://open.spotify.com/intl-de/track/$spotifyId",
            "https://open.spotify.com/intl-de/track/$spotifyId?si=abc"
        )
        assertCleaned(
            "https://open.spotify.com/embed/track/$spotifyId",
            "https://open.spotify.com/embed/track/$spotifyId?si=abc"
        )
    }

    // YouTube and YouTube Music

    @Test
    fun youtubeKeepsVideoPlaylistIndexAndTimestamp() {
        assertCleaned(
            "https://www.youtube.com/watch?v=$videoId&list=$ytPlaylistId&index=2&t=42s",
            "https://www.youtube.com/watch?v=$videoId&list=$ytPlaylistId&index=2&t=42s&si=AbCdEf&feature=share"
        )
    }

    @Test
    fun youtubeParameterOrderIsPreserved() {
        assertCleaned(
            "https://www.youtube.com/watch?t=1&v=$videoId&list=$ytPlaylistId",
            "https://www.youtube.com/watch?si=a&t=1&v=$videoId&feature=share&list=$ytPlaylistId"
        )
    }

    @Test
    fun youtubeShortLinkKeepsItsTimestamp() {
        assertCleaned("https://youtu.be/$videoId?t=30", "https://youtu.be/$videoId?si=abc&t=30")
        assertCleaned("https://youtu.be/$videoId", "https://youtu.be/$videoId?si=abc&feature=youtu.be")
    }

    @Test
    fun youtubeShortsAndEmbedAreCleaned() {
        assertCleaned("https://www.youtube.com/shorts/$videoId", "https://www.youtube.com/shorts/$videoId?si=abc")
        assertCleaned("https://www.youtube.com/embed/$videoId?start=5", "https://www.youtube.com/embed/$videoId?si=abc&start=5")
    }

    @Test
    fun youtubeMusicKeepsTheListAndDropsSi() {
        assertCleaned(
            "https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId",
            "https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId&si=abc"
        )
        assertCleaned(
            "https://music.youtube.com/playlist?list=$ytPlaylistId",
            "https://music.youtube.com/playlist?list=$ytPlaylistId&si=abc"
        )
    }

    @Test
    fun youtubeRedirectsSearchAndOtherPagesAreNotTouched() {
        assertUntouched("https://www.youtube.com/redirect?event=video_description&q=https%3A%2F%2Fexample.com%2F%3Futm_source%3Dx&si=1")
        assertUntouched("https://www.youtube.com/attribution_link?a=abc&u=%2Fwatch%3Fv%3D$videoId&utm_source=x")
        assertUntouched("https://www.youtube.com/results?search_query=rick&utm_source=a")
        assertUntouched("https://www.youtube.com/?utm_source=a")
    }

    @Test
    fun youtubeLinkWithoutAVideoIdIsLeftAlone() {
        assertUntouched("https://www.youtube.com/watch?si=abc")
    }

    // Apple Music

    @Test
    fun appleMusicKeepsTheTrackSelectorAndAffiliateParameters() {
        assertCleaned(
            "https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271",
            "https://music.apple.com/us/album/never-gonna-give-you-up/1558533900?i=1558534271&uo=4"
        )
        assertCleaned(
            "https://music.apple.com/us/album/x/1558533900?i=1558534271&at=1000lHKX&ct=newsletter&app=music",
            "https://music.apple.com/us/album/x/1558533900?i=1558534271&uo=4&itscg=30200&itsct=music_box_link&at=1000lHKX&ct=newsletter&app=music"
        )
    }

    @Test
    fun appleMusicSiIsNotTreatedAsTracking() {
        assertUntouched("https://music.apple.com/us/album/x/1?i=2&si=keep")
    }

    // Amazon Music

    @Test
    fun amazonMusicKeepsTrackAsinAndAffiliateTag() {
        assertCleaned(
            "https://music.amazon.com/albums/B000000000?trackAsin=B000000001&tag=partner-20",
            "https://music.amazon.com/albums/B000000000?trackAsin=B000000001&ref=dm_sh_AbC123&tag=partner-20"
        )
    }

    @Test
    fun amazonMusicOtherRefValuesAreKept() {
        assertUntouched("https://music.amazon.com/albums/B000000000?trackAsin=B000000001&ref=nav_newsletter")
        assertUntouched("https://music.amazon.de/albums/B000000000?marketplaceId=A1&musicTerritory=DE")
    }

    // SoundCloud

    @Test
    fun soundCloudShareParametersAreRemoved() {
        assertCleaned(
            "https://soundcloud.com/artist/track",
            "https://soundcloud.com/artist/track?si=abc123&utm_source=clipboard&utm_medium=text&utm_campaign=social_sharing"
        )
    }

    @Test
    fun soundCloudPlaylistContextAndPrivateTrackPathStay() {
        assertCleaned(
            "https://soundcloud.com/artist/track?in=artist/sets/mix",
            "https://soundcloud.com/artist/track?si=abc&in=artist/sets/mix"
        )
        assertCleaned(
            "https://soundcloud.com/artist/track/s-AbCdEfGhIjK",
            "https://soundcloud.com/artist/track/s-AbCdEfGhIjK?si=abc"
        )
    }

    // Generic tracking parameters

    @Test
    fun utmFbclidAndGclidAreRemovedOnKnownServices() {
        assertCleaned(
            "https://www.deezer.com/track/3135556?app_id=1",
            "https://www.deezer.com/track/3135556?utm_campaign=share&fbclid=IwAR1&app_id=1&gclid=x"
        )
        assertCleaned(
            "https://tidal.com/browse/track/1234567",
            "https://tidal.com/browse/track/1234567?utm_source=a&utm_medium=b"
        )
    }

    @Test
    fun trackingNamesAreMatchedCaseInsensitively() {
        assertCleaned("https://tidal.com/browse/track/1?x=1", "https://tidal.com/browse/track/1?UTM_Source=a&x=1&FBCLID=z")
    }

    @Test
    fun siIsOnlyRemovedWhereItIsKnownToBeTracking() {
        assertUntouched("https://www.deezer.com/track/1?si=keep")
        assertUntouched("https://tidal.com/browse/track/1?si=keep")
        assertUntouched("https://music.yandex.ru/track/609676?si=keep")
    }

    // Unknown, signed, affiliate and referral parameters

    @Test
    fun unknownAffiliateAndReferralParametersAreKept() {
        assertCleaned(
            "https://www.deezer.com/track/1?affiliate=abc&partner=x&refid=7&aff_id=9&irclickid=q&ref=nav",
            "https://www.deezer.com/track/1?affiliate=abc&utm_source=a&partner=x&refid=7&aff_id=9&irclickid=q&ref=nav"
        )
        assertCleaned(
            "https://play.anghami.com/song/106295734?refer=linktree",
            "https://play.anghami.com/song/106295734?refer=linktree&utm_source=x"
        )
    }

    @Test
    fun signedOrTokenUrlsAreLeftEntirelyUntouched() {
        assertUntouched("https://open.spotify.com/track/$spotifyId?si=abc&sig=zzz")
        assertUntouched("https://open.spotify.com/track/$spotifyId?utm_source=a&token=abc")
        assertUntouched("https://soundcloud.com/artist/track?secret_token=s-AbC&si=abc")
        assertUntouched("https://www.deezer.com/track/1?utm_source=a&X-Amz-Signature=abc&X-Amz-Credential=d")
        assertUntouched("https://www.deezer.com/track/1?utm_source=a&Expires=1700000000&Signature=abc")
    }

    // Query structure

    @Test
    fun repeatedTrackingParametersAreAllRemovedAndRepeatedContentParametersStay() {
        assertCleaned(
            "https://www.youtube.com/watch?v=$videoId&t=1",
            "https://www.youtube.com/watch?v=$videoId&si=a&si=b&t=1"
        )
        assertUntouched("https://www.youtube.com/watch?v=$videoId&list=A&list=B")
    }

    @Test
    fun percentEncodingOfKeptParametersIsPreservedExactly() {
        assertCleaned(
            "https://www.youtube.com/watch?v=$videoId&title=caf%C3%A9%20%2B%20bar&q=a%26b",
            "https://www.youtube.com/watch?v=$videoId&si=a&title=caf%C3%A9%20%2B%20bar&q=a%26b"
        )
    }

    @Test
    fun valuelessAndEmptyParametersAreKept() {
        assertCleaned("https://tidal.com/browse/track/123?u", "https://tidal.com/browse/track/123?u&utm_source=x")
        assertUntouched("https://tidal.com/browse/track/123?")
        assertUntouched("https://tidal.com/browse/track/123?a=1&&b=")
    }

    @Test
    fun theQuestionMarkGoesWhenNothingIsLeft() {
        assertCleaned("https://open.spotify.com/track/$spotifyId", "https://open.spotify.com/track/$spotifyId?si=a")
        assertCleaned("https://open.spotify.com/track/$spotifyId", "https://open.spotify.com/track/$spotifyId?si=a&")
    }

    @Test
    fun fragmentsAreKeptVerbatim() {
        assertCleaned(
            "https://www.youtube.com/watch?v=$videoId#t=10",
            "https://www.youtube.com/watch?v=$videoId&si=a#t=10"
        )
        assertCleaned(
            "https://open.spotify.com/track/$spotifyId#frag?si=keep&utm_source=keep",
            "https://open.spotify.com/track/$spotifyId?si=a#frag?si=keep&utm_source=keep"
        )
        assertUntouched("https://open.spotify.com/track/$spotifyId#si=keep")
    }

    @Test
    fun schemeHostCaseAndPortAreNotNormalized() {
        assertCleaned(
            "HTTPS://Open.Spotify.com:443/track/$spotifyId",
            "HTTPS://Open.Spotify.com:443/track/$spotifyId?si=a"
        )
    }

    // Links that must not be guessed at

    @Test
    fun shortenedLinksThatNeedNetworkResolutionAreNeverRewritten() {
        listOf(
            "https://spotify.link/AbCdEfG?si=abc&utm_source=x",
            "https://spotify.app.link/AbCdEfG?utm_source=x",
            "https://on.soundcloud.com/AbCdEf?utm_source=x",
            "https://link.deezer.com/s/AbCdEf?utm_source=x",
            "https://deezer.page.link/AbCdEf?utm_source=x",
            "https://amzn.to/3AbCdEf?utm_source=x",
            "https://amzn.eu/d/AbCdEf?utm_source=x"
        ).forEach { assertUntouched(it) }
    }

    @Test
    fun unknownAndLookAlikeHostsAreNeverRewritten() {
        listOf(
            "https://example.com/x?utm_source=a&si=b&fbclid=c",
            "https://open.spotify.com.evil.example/track/$spotifyId?si=abc",
            "https://youtube.com.evil.example/watch?v=$videoId&si=abc",
            "https://evil.example/?u=https://open.spotify.com/track/$spotifyId%3Fsi%3Dabc&utm_source=a"
        ).forEach { assertUntouched(it) }
    }

    @Test
    fun invalidOrMalformedLinksAreLeftAlone() {
        listOf(
            "https://open.spotify.com/track/short?si=abc",
            "https://open.spotify.com@evil.example/track/$spotifyId?si=abc",
            "https://open.spotify.com/track/$spotifyId?si=abc and more",
            "ftp://open.spotify.com/track/$spotifyId?si=abc",
            "javascript:alert(1)",
            "open.spotify.com/track/$spotifyId?si=abc"
        ).forEach { assertUntouched(it) }
    }

    @Test
    fun textContainingALinkIsNotARewrittenString() {
        assertUntouched("Listen: https://open.spotify.com/track/$spotifyId?si=abc")
        assertUntouched("")
        assertUntouched("   ")
    }

    @Test
    fun knownServicePagesThatAreNotContentAreStillOnlyLosingKnownParameters() {
        assertCleaned("https://open.spotify.com/episode/$spotifyId", "https://open.spotify.com/episode/$spotifyId?si=abc")
        assertUntouched("https://open.spotify.com/user/someone?x=1")
    }

    // Setting on/off

    @Test
    fun outboundLinksAreUntouchedWhenTheSettingIsOff() {
        val url = "https://open.spotify.com/track/$spotifyId?si=abc&utm_source=copy-link"
        assertEquals(url, LinkCleaner.forOutbound(url, cleanLinks = false))
    }

    @Test
    fun outboundLinksAreCleanedWhenTheSettingIsOn() {
        val url = "https://open.spotify.com/track/$spotifyId?si=abc&utm_source=copy-link"
        assertEquals("https://open.spotify.com/track/$spotifyId", LinkCleaner.forOutbound(url, cleanLinks = true))
    }

    @Test
    fun theSettingDoesNotAffectLinksWithNothingToClean() {
        val url = "https://music.apple.com/us/album/x/1?i=2"
        assertEquals(url, LinkCleaner.forOutbound(url, cleanLinks = true))
        assertEquals(url, LinkCleaner.forOutbound(url, cleanLinks = false))
    }

    // Logging

    @Test
    fun redactedFormKeepsHostAndParameterNamesOnly() {
        val redacted = LinkCleaner.redactForLog(
            "https://soundcloud.com/artist/track/s-SecretToken?si=abc123&secret_token=s-Hidden&utm_source=x#frag"
        )
        assertEquals("https://soundcloud.com/…?si=…&secret_token=…&utm_source=…", redacted)
        listOf("abc123", "s-Hidden", "SecretToken", "artist", "frag").forEach { assertFalse(it, redacted.contains(it)) }
    }

    @Test
    fun redactedFormHandlesLinksWithoutParametersAndNonUrls() {
        assertEquals("https://youtu.be/…", LinkCleaner.redactForLog("https://youtu.be/$videoId"))
        assertTrue(LinkCleaner.redactForLog("not a url").startsWith("<"))
        assertTrue(LinkCleaner.redactForLog("https://user:pw@example.com/?a=1").startsWith("<"))
    }
}
