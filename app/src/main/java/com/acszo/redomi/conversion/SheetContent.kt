package com.acszo.redomi.conversion

import com.acszo.redomi.links.LinkCleaner
import com.acszo.redomi.parser.MusicService

enum class DestinationKind {
    /** The link as received, opened in its own service. */
    ORIGINAL,

    /** The same identifier on a sibling service. Not a verified match. */
    SAME_ID,

    /** The MusicLink page that resolves the link. It is not a streaming service. */
    RESOLVER
}

/** One clickable entry of the conversion sheet. [service] is null for the resolver. */
data class Destination(
    val kind: DestinationKind,
    val service: MusicService?,
    val url: String,
    val classification: ActionClassification
)

enum class SheetNotice {
    /** The link is recognized but cannot be converted, so only the original is offered. */
    CANNOT_CONVERT
}

enum class SheetFailure {
    UNSUPPORTED_LINK,
    INVALID_LINK
}

sealed interface SheetContent {

    /** [destinations] is never empty. */
    data class Ready(
        val source: ConversionSource,
        val destinations: List<Destination>,
        val notice: SheetNotice? = null
    ) : SheetContent

    data class Failed(val reason: SheetFailure) : SheetContent
}

/** Turns a [ConversionPlanResult] into what the sheet shows, applying the user's selected apps. */
object SheetContentBuilder {

    /**
     * [selectedApps] holds the platform keys the user picked for the current action (opening or
     * sharing). They filter the original and same-ID destinations. The resolver is not an app and
     * is never filtered out, so a plan always keeps one working destination.
     * [cleanLinks] is only needed for links the planner could not plan, which are offered as they
     * are; planned links were already cleaned by the planner.
     */
    fun build(result: ConversionPlanResult, selectedApps: Set<String>, cleanLinks: Boolean = false): SheetContent =
        when (result) {
            is ConversionPlanResult.Planned -> SheetContent.Ready(result.source, destinationsOf(result.actions, selectedApps))
            is ConversionPlanResult.Unsupported -> unsupported(result, cleanLinks)
            is ConversionPlanResult.Invalid -> SheetContent.Failed(SheetFailure.INVALID_LINK)
        }

    private fun destinationsOf(actions: List<ConversionAction>, selectedApps: Set<String>): List<Destination> =
        actions.mapNotNull { action ->
            when (action) {
                is ConversionAction.Original ->
                    destination(DestinationKind.ORIGINAL, action.service, action.url, action, selectedApps)
                is ConversionAction.SameIdConversion ->
                    destination(DestinationKind.SAME_ID, action.target, action.url, action, selectedApps)
                is ConversionAction.UniversalResolver ->
                    Destination(DestinationKind.RESOLVER, null, action.url, action.classification)
                // Nothing to click: there is no URL to open.
                is ConversionAction.Unavailable -> null
                // Reserved types the planner does not create yet; not shown until they are supported.
                is ConversionAction.SearchSuggestion, is ConversionAction.VerifiedMatch -> null
            }
        }

    private fun destination(
        kind: DestinationKind,
        service: MusicService,
        url: String,
        action: ConversionAction,
        selectedApps: Set<String>
    ): Destination? =
        Destination(kind, service, url, action.classification).takeIf { service.platformKey in selectedApps }

    /**
     * A recognized service whose content cannot be converted (a playlist, an artist, a channel) is
     * still opened as it is, regardless of the selected apps, so the user is never stranded.
     */
    private fun unsupported(result: ConversionPlanResult.Unsupported, cleanLinks: Boolean): SheetContent {
        val service = result.service ?: return SheetContent.Failed(SheetFailure.UNSUPPORTED_LINK)
        val url = LinkCleaner.forOutbound(result.sourceUrl, cleanLinks)
        return SheetContent.Ready(
            source = ConversionSource(service, result.contentType, id = null, originalUrl = result.sourceUrl, normalizedUrl = null),
            destinations = listOf(Destination(DestinationKind.ORIGINAL, service, url, ActionClassification.ORIGINAL)),
            notice = SheetNotice.CANNOT_CONVERT
        )
    }

    /**
     * Whether the sheet should open [destinations] right away for a view intent: only a single
     * destination that is a real service, never the resolver alone, which the user should choose.
     */
    fun shouldAutoOpen(destinations: List<Destination>): Boolean =
        destinations.size == 1 && destinations.first().kind != DestinationKind.RESOLVER
}
