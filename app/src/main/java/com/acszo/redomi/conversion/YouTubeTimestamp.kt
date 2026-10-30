package com.acszo.redomi.conversion

/**
 * Reads a starting time from a YouTube or YouTube Music link, so it can follow a same-ID
 * conversion. Only plain, sane values are accepted; anything else is ignored rather than guessed.
 */
object YouTubeTimestamp {

    /** 24 hours: far beyond any video, so larger values are treated as malformed. */
    const val MAX_SECONDS = 24 * 60 * 60

    private val DURATION = Regex("""(?:(\d{1,3})h)?(?:(\d{1,4})m)?(?:(\d{1,6})s)?""")
    private val SECONDS = Regex("""\d{1,6}""")

    /**
     * Seconds for `58`, `58s`, `1m30s` or `1h2m3s`, or null if [value] is malformed, zero or over
     * [MAX_SECONDS]. Units must be lowercase and in order; `1m30` and `-5` are rejected.
     */
    fun parseSeconds(value: String): Int? {
        if (value.isEmpty()) return null

        val seconds = if (SECONDS.matches(value)) {
            value.toLong()
        } else {
            val match = DURATION.matchEntire(value) ?: return null
            val (hours, minutes, secs) = match.destructured
            hours.toLongOrZero() * 3600 + minutes.toLongOrZero() * 60 + secs.toLongOrZero()
        }
        return if (seconds in 1..MAX_SECONDS) seconds.toInt() else null
    }

    /**
     * The first valid starting time of [url], looking at the `t` and `start` query parameters and
     * then at a `#t=` fragment. Nothing else is read, so tracking parameters, `list` and the like
     * can never come along.
     */
    fun fromUrl(url: String): Int? {
        val fragmentStart = url.indexOf('#')
        val beforeFragment = if (fragmentStart < 0) url else url.substring(0, fragmentStart)
        val fragment = if (fragmentStart < 0) "" else url.substring(fragmentStart + 1)

        val query = beforeFragment.substringAfter('?', "").split('&')
        val fragmentParams = fragment.split('&')

        return sequenceOf(
            valuesOf(query, "t"),
            valuesOf(query, "start"),
            valuesOf(fragmentParams, "t")
        ).flatten().firstNotNullOfOrNull { parseSeconds(it) }
    }

    private fun valuesOf(params: List<String>, key: String): Sequence<String> =
        params.asSequence()
            .filter { it.substringBefore('=') == key && it.contains('=') }
            .map { it.substringAfter('=') }

    private fun String.toLongOrZero(): Long = if (isEmpty()) 0 else toLong()
}
