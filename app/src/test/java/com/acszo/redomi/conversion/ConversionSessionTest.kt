package com.acszo.redomi.conversion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionSessionTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private val spotify = IncomingRequest("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC", isActionSend = true)
    private val youtube = IncomingRequest("https://youtu.be/dQw4w9WgXcQ", isActionSend = true)

    private val planned = mutableListOf<IncomingRequest>()

    // Recognizable results without needing a real plan: the failure reason names the request.
    private fun contentFor(request: IncomingRequest): SheetContent =
        SheetContent.Failed(if (request == spotify) SheetFailure.UNSUPPORTED_LINK else SheetFailure.INVALID_LINK)

    private fun session(plan: suspend (IncomingRequest) -> SheetContent = { contentFor(it) }) =
        ConversionSession(scope) {
            planned += it
            plan(it)
        }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun theFirstRequestIsPlannedAndPublished() {
        val session = session()

        assertTrue(session.submit(spotify))

        val state = session.state.value
        assertEquals(spotify, state.request)
        assertEquals(contentFor(spotify), state.content)
        assertEquals(1, state.generation)
    }

    @Test
    fun anEqualRequestIsIgnoredSoRecompositionAndRecreationDoNotPlanTwice() {
        val session = session()
        session.submit(spotify)

        assertFalse(session.submit(spotify))
        assertFalse(session.submit(IncomingRequest(spotify.input, spotify.isActionSend)))

        assertEquals(listOf(spotify), planned)
        assertEquals(1, session.state.value.generation)
    }

    @Test
    fun aNewIntentForTheSameLinkStartsOverWhenForced() {
        val session = session()
        session.submit(spotify)

        assertTrue(session.submit(spotify, force = true))

        assertEquals(listOf(spotify, spotify), planned)
        assertEquals(2, session.state.value.generation)
    }

    @Test
    fun aDifferentLinkReplacesTheSourceAndPlansAgain() {
        val session = session()
        session.submit(spotify)

        assertTrue(session.submit(youtube))

        val state = session.state.value
        assertEquals(youtube, state.request)
        assertEquals(contentFor(youtube), state.content)
        assertEquals(2, state.generation)
    }

    @Test
    fun aChangeOfActionModeIsANewRequest() {
        val session = session()
        session.submit(spotify)

        assertTrue(session.submit(spotify.copy(isActionSend = false)))

        assertEquals(2, planned.size)
        assertFalse(session.state.value.request!!.isActionSend)
    }

    @Test
    fun theOldContentIsClearedWhileTheNewRequestIsBeingPlanned() {
        val gate = CompletableDeferred<Unit>()
        val session = session { if (it == youtube) gate.await(); contentFor(it) }
        session.submit(spotify)
        assertEquals(contentFor(spotify), session.state.value.content)

        session.submit(youtube)

        assertNull(session.state.value.content)
        assertEquals(youtube, session.state.value.request)

        gate.complete(Unit)
        assertEquals(contentFor(youtube), session.state.value.content)
    }

    @Test
    fun aSlowOlderPlanCannotOverwriteANewerOne() {
        val slow = CompletableDeferred<Unit>()
        var firstFinished = false
        val session = session {
            if (it == spotify) {
                slow.await()
                firstFinished = true
            }
            contentFor(it)
        }

        session.submit(spotify)
        session.submit(youtube)
        assertEquals(contentFor(youtube), session.state.value.content)

        slow.complete(Unit)

        assertEquals(contentFor(youtube), session.state.value.content)
        assertEquals(youtube, session.state.value.request)
        // the older plan was cancelled, so it never even got to finish
        assertFalse(firstFinished)
    }

    @Test
    fun evenAPlanThatIgnoresCancellationCannotPublishAfterBeingReplaced() {
        val slow = CompletableDeferred<Unit>()
        val session = session {
            if (it == spotify) withContext(NonCancellable) { slow.await() }
            contentFor(it)
        }

        session.submit(spotify)
        session.submit(youtube)
        slow.complete(Unit)

        val state = session.state.value
        assertEquals(youtube, state.request)
        assertEquals(contentFor(youtube), state.content)
        assertEquals(2, state.generation)
    }

    @Test
    fun theNewestOfSeveralQuickRequestsWins() {
        val gates = List(3) { CompletableDeferred<Unit>() }
        val requests = List(3) { IncomingRequest("https://youtu.be/dQw4w9WgXc$it", isActionSend = true) }
        val session = session { request -> gates[requests.indexOf(request)].await(); SheetContent.Failed(SheetFailure.INVALID_LINK) }

        requests.forEach { session.submit(it) }
        gates.reversed().forEach { it.complete(Unit) }

        assertEquals(requests.last(), session.state.value.request)
        assertEquals(3, session.state.value.generation)
        assertEquals(SheetContent.Failed(SheetFailure.INVALID_LINK), session.state.value.content)
    }

    @Test
    fun retryPlansTheCurrentRequestAgain() {
        val session = session()
        session.submit(spotify)

        assertTrue(session.retry())

        assertEquals(listOf(spotify, spotify), planned)
        assertEquals(2, session.state.value.generation)
        assertEquals(contentFor(spotify), session.state.value.content)
    }

    @Test
    fun retryBeforeAnyRequestDoesNothing() {
        val session = session()

        assertFalse(session.retry())

        assertTrue(planned.isEmpty())
        assertEquals(ConversionUiState(), session.state.value)
    }

    @Test
    fun missingInputIsAValidRequestThatPlansToAFailure() {
        val session = session()

        assertTrue(session.submit(IncomingRequest(null, isActionSend = false)))
        assertFalse(session.submit(IncomingRequest(null, isActionSend = false)))
    }
}
