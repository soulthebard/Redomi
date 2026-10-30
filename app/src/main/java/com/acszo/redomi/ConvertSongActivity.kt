package com.acszo.redomi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acszo.redomi.data.AppList
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isActionSend = (intent.action == Intent.ACTION_SEND)
        val appList = if (isActionSend) AppList.SHARING else AppList.OPENING
        // Handed to the planner as received; it finds the link in shared text itself.
        val input = if (isActionSend) {
            intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        } else {
            intent?.dataString
        }

        setContent {
            val conversionViewModel: ConversionViewModel = hiltViewModel()
            val updateViewModel: UpdateViewModel = hiltViewModel()
            val dataStoreViewModel: DataStoreViewModel = hiltViewModel()

            val content by conversionViewModel.content.collectAsStateWithLifecycle()
            val updatePageUiState by updateViewModel.updatePageUiState.collectAsStateWithLifecycle()
            val theme by dataStoreViewModel.themeMode.collectAsStateWithLifecycle()
            val iconShape by dataStoreViewModel.iconShape.collectAsStateWithLifecycle()
            val listOrientation by dataStoreViewModel.listOrientation.collectAsStateWithLifecycle()
            val gridSize by dataStoreViewModel.gridSize.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                if (content == null)
                    conversionViewModel.plan(input, appList.key)
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
                    content = content,
                    isActionSend = isActionSend,
                    isUpdateAvailable = updatePageUiState.isUpdateAvailable,
                    iconShape = IconShape.entries[iconShape].shape,
                    listOrientation = ListOrientation.entries[listOrientation],
                    gridSize = gridSize,
                    retry = { conversionViewModel.plan(input, appList.key) }
                )
            }
        }
    }
}
