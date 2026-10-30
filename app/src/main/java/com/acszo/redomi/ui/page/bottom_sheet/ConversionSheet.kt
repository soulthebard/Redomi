package com.acszo.redomi.ui.page.bottom_sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acszo.redomi.R
import com.acszo.redomi.conversion.Destination
import com.acszo.redomi.conversion.DestinationKind
import com.acszo.redomi.conversion.SheetContent
import com.acszo.redomi.conversion.SheetContentBuilder
import com.acszo.redomi.conversion.SheetFailure
import com.acszo.redomi.conversion.SheetNotice
import com.acszo.redomi.data.ListOrientation
import com.acszo.redomi.ui.common.enterFadeInTransition
import com.acszo.redomi.ui.page.bottom_sheet.components.ActionsMenu
import com.acszo.redomi.ui.page.bottom_sheet.components.BottomSheetError
import com.acszo.redomi.ui.page.bottom_sheet.components.DestinationsList
import com.acszo.redomi.ui.page.bottom_sheet.components.SourceCard
import com.acszo.redomi.utils.IntentUtil.onIntentView

/**
 * The conversion sheet driven by [com.acszo.redomi.conversion.ConversionPlanner]. It keeps the
 * look and the settings of the old sheet but lists destinations instead of Songlink results.
 *
 * - ACTION_VIEW: choosing a destination opens it; a single real destination opens at once.
 * - ACTION_SEND: choosing a destination offers Open, Copy and Share for its URL.
 *
 * [generation] changes with every new request, so a link delivered to a sheet that is still open
 * starts from scratch: no leftover selection, no leftover actions menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionSheet(
    onDismiss: () -> Unit,
    content: SheetContent?,
    generation: Int,
    isActionSend: Boolean,
    isUpdateAvailable: Boolean,
    iconShape: Shape,
    listOrientation: ListOrientation,
    gridSize: Int,
    retry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = listOrientation == ListOrientation.HORIZONTAL
    )

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (content) {
                null -> {
                    Box(
                        modifier = Modifier.height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SheetContent.Failed -> {
                    BottomSheetError(
                        message = stringResource(
                            when (content.reason) {
                                SheetFailure.UNSUPPORTED_LINK -> R.string.error_unsupported_conversion
                                SheetFailure.INVALID_LINK -> R.string.error_incorrect_url
                            }
                        ),
                        onClick = retry
                    )
                }
                is SheetContent.Ready -> {
                    ReadyContent(
                        content = content,
                        generation = generation,
                        onDismiss = onDismiss,
                        isActionSend = isActionSend,
                        isUpdateAvailable = isUpdateAvailable,
                        iconShape = iconShape,
                        listOrientation = listOrientation,
                        gridSize = gridSize
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(
    content: SheetContent.Ready,
    generation: Int,
    onDismiss: () -> Unit,
    isActionSend: Boolean,
    isUpdateAvailable: Boolean,
    iconShape: Shape,
    listOrientation: ListOrientation,
    gridSize: Int
) {
    val context = LocalContext.current
    val destinations = content.destinations

    val autoOpen = !isActionSend && SheetContentBuilder.shouldAutoOpen(destinations)
    var selected by remember(content, generation) {
        mutableStateOf(if (destinations.size == 1) destinations.first() else null)
    }
    var showActionsMenu by remember(content, generation) {
        mutableStateOf(destinations.size == 1 && (isActionSend || autoOpen))
    }

    if (destinations.size > 1 || isActionSend) {
        SourceCard(
            source = content.source,
            isUpdateAvailable = isUpdateAvailable
        )
    }

    if (content.notice == SheetNotice.CANNOT_CONVERT && (destinations.size > 1 || isActionSend)) {
        Text(
            text = stringResource(id = R.string.notice_cannot_convert),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    if (!showActionsMenu) {
        DestinationsList(
            listOrientation = listOrientation,
            destinations = destinations,
            iconShape = iconShape,
            gridSize = gridSize,
            onClick = { destination ->
                if (!isActionSend) {
                    onIntentView(context, destination.url)
                } else {
                    selected = destination
                    showActionsMenu = true
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showActionsMenu,
        enter = enterFadeInTransition(),
    ) {
        val destination = selected
        if (destination != null) {
            if (isActionSend) {
                ActionsMenu(url = destination.url, note = noteFor(destination)) {
                    onDismiss()
                }
            } else {
                Box(
                    modifier = Modifier.height(200.dp),
                ) {
                    LaunchedEffect(generation, destination) {
                        onIntentView(context, destination.url)
                        onDismiss()
                    }
                }
            }
        }
    }
}

/** Says what a destination is when that is not obvious from its icon. */
@Composable
private fun noteFor(destination: Destination): String? = when (destination.kind) {
    DestinationKind.ORIGINAL -> null
    DestinationKind.SAME_ID -> stringResource(id = R.string.note_same_id)
    DestinationKind.RESOLVER -> stringResource(id = R.string.note_resolver)
}
