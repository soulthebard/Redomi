package com.acszo.redomi.conversion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A link Redomi was asked to convert: [input] as received, and whether it came from a share. */
data class IncomingRequest(
    val input: String?,
    val isActionSend: Boolean
)

/**
 * What the conversion sheet shows. [generation] changes with every accepted request, so anything
 * the sheet remembers about the previous one (selection, open actions menu) can be keyed to it.
 */
data class ConversionUiState(
    val request: IncomingRequest? = null,
    val content: SheetContent? = null,
    val generation: Int = 0
)

/**
 * Plans incoming requests one at a time. A new request replaces the one before it, resets what was
 * shown for it, and cancels its planning, so a slow earlier plan can never overwrite a newer one.
 * Calls are expected on a single thread, such as the main thread.
 */
class ConversionSession(
    private val scope: CoroutineScope,
    private val plan: suspend (IncomingRequest) -> SheetContent
) {

    private val _state = MutableStateFlow(ConversionUiState())
    val state: StateFlow<ConversionUiState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Starts planning [request]. A request equal to the current one is ignored unless [force] is
     * set, which is how a recreated activity or a recomposition avoids planning twice, while a
     * new intent for the same link still starts the sheet over.
     * Returns whether planning was started.
     */
    fun submit(request: IncomingRequest, force: Boolean = false): Boolean {
        if (!force && _state.value.request == request) return false

        job?.cancel()
        val generation = _state.value.generation + 1
        _state.value = ConversionUiState(request = request, content = null, generation = generation)

        job = scope.launch {
            val content = plan(request)
            // Belt and braces next to cancel(): only the newest request may publish its result.
            _state.update { if (it.generation == generation) it.copy(content = content) else it }
        }
        return true
    }

    /** Plans the current request again, e.g. after a failure. Does nothing before the first request. */
    fun retry(): Boolean {
        val request = _state.value.request ?: return false
        return submit(request, force = true)
    }
}
