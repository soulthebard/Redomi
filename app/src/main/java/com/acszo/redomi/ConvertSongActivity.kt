package com.acszo.redomi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acszo.redomi.conversion.IncomingRequest
import com.acszo.redomi.data.IconShape
import com.acszo.redomi.data.ListOrientation
import com.acszo.redomi.ui.page.bottom_sheet.ConversionSheet
import com.acszo.redomi.ui.theme.RedomiTheme
import com.acszo.redomi.viewmodel.ConversionViewModel
import com.acszo.redomi.viewmodel.DataStoreViewModel
import com.acszo.redomi.viewmodel.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

/*
* ACTION_SEND -> shows sharing apps -> shows action menu
* ACTION_VIEW -> shows opening apps -> opens app
* */
@AndroidEntryPoint
class ConvertSongActivity : ComponentActivity() {

    private val conversionViewModel: ConversionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A recreated activity gets its view model back with the same request, which is ignored.
        conversionViewModel.submit(incomingRequest(intent))

        setContent {
            val updateViewModel: UpdateViewModel = hiltViewModel()
            val dataStoreViewModel: DataStoreViewModel = hiltViewModel()

            val conversion by conversionViewModel.state.collectAsStateWithLifecycle()
            val updatePageUiState by updateViewModel.updatePageUiState.collectAsStateWithLifecycle()
            val theme by dataStoreViewModel.themeMode.collectAsStateWithLifecycle()
            val iconShape by dataStoreViewModel.iconShape.collectAsStateWithLifecycle()
            val listOrientation by dataStoreViewModel.listOrientation.collectAsStateWithLifecycle()
            val gridSize by dataStoreViewModel.gridSize.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                if (isGithubBuild) {
                    if (!updatePageUiState.isLoaded)
                        updateViewModel.checkUpdate(versionCode)
                }
            }

            RedomiTheme(
                theme = theme
            ) {
                ConversionSheet(
                    onDismiss = { this.finish() },
                    content = conversion.content,
                    generation = conversion.generation,
                    isActionSend = conversion.request?.isActionSend == true,
                    isUpdateAvailable = updatePageUiState.isUpdateAvailable,
                    iconShape = IconShape.entries[iconShape].shape,
                    listOrientation = ListOrientation.entries[listOrientation],
                    gridSize = gridSize,
                    retry = { conversionViewModel.retry() }
                )
            }
        }
    }

    // launchMode is singleInstance, so a second share or link lands here while the sheet may still
    // be open. It must start over with the new link instead of showing the old one.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        conversionViewModel.submit(incomingRequest(intent), force = true)
    }

    // Handed to the planner as received; it finds the link in shared text itself.
    private fun incomingRequest(intent: Intent?): IncomingRequest {
        val isActionSend = intent?.action == Intent.ACTION_SEND
        return IncomingRequest(
            input = if (isActionSend) {
                intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            } else {
                intent?.dataString
            },
            isActionSend = isActionSend
        )
    }
}
