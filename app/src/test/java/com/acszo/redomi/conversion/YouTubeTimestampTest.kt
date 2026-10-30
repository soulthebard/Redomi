package com.acszo.redomi.conversion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTimestampTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun plainSecondsAndSecondsWithUnit() {
        assertEquals(58, YouTubeTimestamp.parseSeconds("58"))
        assertEquals(58, YouTubeTimestamp.parseSeconds("58s"))
        assertEquals(90, YouTubeTimestamp.parseSeconds("90s"))
        assertEquals(1, YouTubeTimestamp.parseSeconds("1"))
    }

    @Test
    fun minutesAndHours() {
        assertEquals(90, YouTubeTimestamp.parseSeconds("1m30s"))
        assertEquals(120, YouTubeTimestamp.parseSeconds("2m"))
        assertEquals(3600, YouTubeTimestamp.parseSeconds("1h"))
        assertEquals(3723, YouTubeTimestamp.parseSeconds("1h2m3s"))
        assertEquals(3630, YouTubeTimestamp.parseSeconds("1h30s"))
    }

    @Test
    fun theLimitIsTwentyFourHours() {
        assertEquals(86_400, YouTubeTimestamp.parseSeconds("86400"))
        assertEquals(86_400, YouTubeTimestamp.parseSeconds("24h"))
        assertNull(YouTubeTimestamp.parseSeconds("86401"))
        assertNull(YouTubeTimestamp.parseSeconds("24h1s"))
    }

    @Test
    fun zeroEmptyAndMalformedValuesAreRejected() {
        listOf(
            "", "0", "0s", "0m0s", "-5", "+58", "1.5", "58 s", "58S", "1M30S", "1m30", "m30s", "s",
            "abc", "30s1m", "1m1m", "58ss", "1m 30s", "58%73", "０５８"
        ).forEach { assertNull("<$it>", YouTubeTimestamp.parseSeconds(it)) }
    }

    @Test
    fun absurdValuesAreRejectedWithoutOverflowing() {
        listOf("99999999", "100000000000000000000", "1000h", "9999m", "999999s", "1000000").forEach {
            assertNull(it, YouTubeTimestamp.parseSeconds(it))
        }
    }

    @Test
    fun theTAndStartQueryParametersAndTheFragmentAreRead() {
        assertEquals(58, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=58"))
        assertEquals(58, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=58s"))
        assertEquals(90, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=1m30s"))
        assertEquals(90, YouTubeTimestamp.fromUrl("https://www.youtube.com/watch?v=$id&start=90"))
        assertEquals(58, YouTubeTimestamp.fromUrl("https://www.youtube.com/watch?v=$id#t=58"))
        assertEquals(90, YouTubeTimestamp.fromUrl("https://www.youtube.com/watch?v=$id#t=1m30s"))
    }

    @Test
    fun parametersAreFoundAnywhereInTheQuery() {
        assertEquals(58, YouTubeTimestamp.fromUrl("https://www.youtube.com/watch?si=a&t=58&v=$id&list=PL"))
        assertEquals(58, YouTubeTimestamp.fromUrl("https://www.youtube.com/watch?v=$id&t=58&is=b"))
    }

    @Test
    fun tBeatsStartAndStartBeatsTheFragment() {
        assertEquals(10, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=10&start=20#t=30"))
        assertEquals(20, YouTubeTimestamp.fromUrl("https://youtu.be/$id?start=20#t=30"))
        assertEquals(30, YouTubeTimestamp.fromUrl("https://youtu.be/$id#t=30"))
    }

    @Test
    fun aMalformedValueFallsThroughToTheNextValidOne() {
        assertEquals(20, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=abc&start=20"))
        assertEquals(30, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=0&start=-1#t=30"))
        assertEquals(45, YouTubeTimestamp.fromUrl("https://youtu.be/$id?t=bad&t=45"))
    }

    @Test
    fun noValidValueMeansNoTimestamp() {
        listOf(
            "https://youtu.be/$id",
            "https://youtu.be/$id?",
            "https://youtu.be/$id?t=",
            "https://youtu.be/$id?t",
            "https://youtu.be/$id?t=abc&start=0#t=-1",
            "https://youtu.be/$id?t=999999"
        ).forEach { assertNull(it, YouTubeTimestamp.fromUrl(it)) }
    }

    @Test
    fun nothingButTAndStartIsRead() {
        listOf(
            "https://youtu.be/$id?si=58",
            "https://youtu.be/$id?is=58",
            "https://youtu.be/$id?time_continue=58",
            "https://youtu.be/$id?list=PL&index=58",
            "https://youtu.be/$id?start_radio=58",
            "https://youtu.be/$id?xt=58&Start=58&T=58",
            "https://youtu.be/$id#start=58",
            "https://youtu.be/$id#si=58"
        ).forEach { assertNull(it, YouTubeTimestamp.fromUrl(it)) }
    }

    @Test
    fun aQuestionMarkInsideTheFragmentIsNotAQuery() {
        assertNull(YouTubeTimestamp.fromUrl("https://youtu.be/$id#frag?t=58"))
    }
}
