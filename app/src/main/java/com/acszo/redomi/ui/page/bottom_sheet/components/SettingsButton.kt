package com.acszo.redomi.ui.page.bottom_sheet.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.acszo.redomi.R
import com.acszo.redomi.ui.common.RotatingIcon
import com.acszo.redomi.utils.IntentUtil.onIntentSettingsPage

/** The settings shortcut of the sheet header, with the update badge. */
@Composable
fun SettingsButton(isUpdateAvailable: Boolean) {
    val context = LocalContext.current

    IconButton(
        onClick = {
            onIntentSettingsPage(context)
        }
    ) {
        RotatingIcon(
            icon = R.drawable.ic_settings,
            size = 24.dp,
            startValue = 120f,
            contentDescription = stringResource(id = R.string.settings)
        )

        if (isUpdateAvailable) {
            val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            val errorColor = MaterialTheme.colorScheme.error

            Canvas(
                modifier = Modifier
                    .size(14.dp)
                    .offset(9.dp, -(6.dp))
            ) {
                drawCircle(color = surfaceColor)
            }
            Canvas(
                modifier = Modifier
                    .size(8.dp)
                    .offset(9.dp, -(6.dp))
            ) {
                drawCircle(color = errorColor)
            }
        }
    }
}
