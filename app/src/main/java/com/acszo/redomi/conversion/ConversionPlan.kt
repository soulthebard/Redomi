package com.acszo.redomi.conversion

import com.acszo.redomi.parser.InvalidReason
import com.acszo.redomi.parser.MusicContentType
import com.acszo.redomi.parser.MusicService

/** How much a [ConversionAction] can be trusted to point at the same music as the source. */
enum class ActionClassification(val isVerifiedMatch: Boolean = false) {
    /** The link the user started from. */
    ORIGINAL,

    /**
     * The same identifier on a sibling service. It is not a verified match: it does not say that
     * the target holds the same recording or can play it.
     */
    SAME_ID_CONVERSION,

    /** A page that resolves the source link when opened. Nothing is looked up by Redomi. */
    UNIVERSAL_RESOLVER,

    /** Reserved: a search built from metadata that is actually known. */
    SEARCH_SUGGESTION,

    /** Reserved: a match confirmed by a lookup service. */
    VERIFIED_MATCH(isVerifiedMatch = true),

    /** A destination that cannot be offered without a lookup. */
    UNAVAILABLE
}

sealed interface ConversionAction {

    val classification: ActionClassification

    /** The source link as found in the input, parameters included. */
    data class Original(val service: MusicService, val url: String) : ConversionAction {
        override val classification get() = ActionClassification.ORIGINAL
    }

    /** [url] holds the source's identifier on [target]. */
    data class SameIdConversion(val target: MusicService, val url: String) : ConversionAction {
        override val classification get() = ActionClassification.SAME_ID_CONVERSION
    }

    /** [url] is a MusicLink resolve page for the source; it has no single target service. */
    data class UniversalResolver(val url: String) : ConversionAction {
        override val classification get() = ActionClassification.UNIVERSAL_RESOLVER
    }

    /**
     * Reserved. Only to be created from a [query] made of metadata that is really known, never
     * guessed from a URL. The planner does not create these today.
     */
    data class SearchSuggestion(val target: MusicService, val url: String, val query: String) : ConversionAction {
        override val classification get() = ActionClassification.SEARCH_SUGGESTION
    }

    /** Reserved for matches confirmed by an API. The planner does not create these today. */
    data class VerifiedMatch(val target: MusicService, val url: String) : ConversionAction {
        override val classification get() = ActionClassification.VERIFIED_MATCH
    }

    /** [target] cannot be offered, for [reason]. There is deliberately no URL. */
    data class Unavailable(val target: MusicService, val reason: UnavailableReason) : ConversionAction {
        override val classification get() = ActionClassification.UNAVAILABLE
    }
}

enum class UnavailableReason {
    /** Reaching the target needs a lookup that is not available offline. */
    REQUIRES_LOOKUP
}

/**
 * What is known about the source link. [contentType], [id] and [normalizedUrl] are null for
 * services whose links are not parsed yet.
 */
data class ConversionSource(
    val service: MusicService,
    val contentType: MusicContentType?,
    val id: String?,
    /** As found in the input, parameters included. */
    val originalUrl: String,
    /** Rebuilt from the identifier, so tracking parameters and host variants are gone. */
    val normalizedUrl: String?
)

sealed interface ConversionPlanResult {

    /** [actions] are ordered: original, same-ID conversions, universal resolver, unavailable. */
    data class Planned(
        val source: ConversionSource,
        val actions: List<ConversionAction>
    ) : ConversionPlanResult

    /** A well-formed link that cannot be converted. [sourceUrl] can still be opened as it is. */
    data class Unsupported(
        val reason: UnsupportedConversionReason,
        val service: MusicService?,
        val contentType: MusicContentType?,
        val sourceUrl: String
    ) : ConversionPlanResult

    data class Invalid(val reason: InvalidReason) : ConversionPlanResult
}

enum class UnsupportedConversionReason {
    UNKNOWN_SERVICE,
    UNSUPPORTED_CONTENT
}
