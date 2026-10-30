package com.acszo.redomi.ui.page.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acszo.redomi.R
import com.acszo.redomi.data.IconShape
import com.acszo.redomi.data.ListOrientation
import com.acszo.redomi.data.Theme
import com.acszo.redomi.isGithubBuild
import com.acszo.redomi.ui.common.DefaultDialog
import com.acszo.redomi.ui.common.IconDescription
import com.acszo.redomi.ui.common.RadioButtonItemDialog
import com.acszo.redomi.ui.common.ScaffoldWithLargeTopAppBar
import com.acszo.redomi.ui.nav.Pages.APPS_PAGE
import com.acszo.redomi.ui.nav.Pages.LAYOUT_PAGE
import com.acszo.redomi.ui.nav.Pages.UPDATE_PAGE
import com.acszo.redomi.ui.theme.bottomItemShape
import com.acszo.redomi.ui.theme.middleLargeItemShape
import com.acszo.redomi.ui.theme.middleSmallItemShape
import com.acszo.redomi.ui.theme.topItemShape
import com.acszo.redomi.utils.ClipboardUtils.copyText
import com.acszo.redomi.utils.IntentUtil.onIntentOpenDefaultsApp
import com.acszo.redomi.versionName
import com.acszo.redomi.viewmodel.DataStoreViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    dataStoreViewModel: DataStoreViewModel,
    navigateTo: (String) -> Unit,
    isUpdateAvailable: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isFirstTime by dataStoreViewModel.isFirstTime.collectAsStateWithLifecycle()
    val listOrientation by dataStoreViewModel.listOrientation.collectAsStateWithLifecycle()
    val iconShape by dataStoreViewModel.iconShape.collectAsStateWithLifecycle()
    val themeMode by dataStoreViewModel.themeMode.collectAsStateWithLifecycle()
    val cleanLinks by dataStoreViewModel.cleanLinks.collectAsStateWithLifecycle()

    val redomi = stringResource(id = R.string.app_name)

    val uriHandle = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val appVersion = "Version: $versionName"
    val modelName = "Model: ${Build.MODEL}"
    val androidVersion = "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    var openIconShapeDialog by rememberSaveable { mutableStateOf(false) }
    var openThemeDialog by rememberSaveable { mutableStateOf(false) }

    ScaffoldWithLargeTopAppBar(
        modifier = modifier,
        title = redomi
    ) { padding, pageTitle ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                pageTitle()
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_grid_view,
                    title = stringResource(id = R.string.apps),
                    description = stringResource(id = R.string.apps_description),
                    itemShape = topItemShape
                ) {
                    navigateTo(APPS_PAGE)
                }
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_format_list_bulleted,
                    title = stringResource(id = R.string.layout),
                    description = stringResource(id = ListOrientation.entries[listOrientation].toRes),
                    itemShape = middleSmallItemShape
                ) {
                    navigateTo(LAYOUT_PAGE)
                }
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_category_filled,
                    title = stringResource(id = R.string.icon_shape),
                    description = stringResource(id = IconShape.entries[iconShape].toRes),
                    itemShape = middleSmallItemShape
                ) {
                    openIconShapeDialog = true
                }
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_color_lens_filled,
                    title = stringResource(id = R.string.theme),
                    description = stringResource(id = Theme.entries[themeMode].toRes),
                    itemShape = bottomItemShape
                ) {
                    openThemeDialog = true
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_link,
                    title = stringResource(id = R.string.clean_links),
                    description = stringResource(id = R.string.clean_links_description),
                    itemShape = middleLargeItemShape,
                    trailingItem = {
                        Switch(
                            checked = cleanLinks,
                            onCheckedChange = null
                        )
                    }
                ) {
                    dataStoreViewModel.setCleanLinks(!cleanLinks)
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isGithubBuild) {
                item {
                    SettingsItem(
                        icon = R.drawable.ic_update,
                        title = stringResource(id = R.string.update),
                        description = stringResource(id = R.string.update_description),
                        itemShape = topItemShape,
                        trailingItem = {
                            if (isUpdateAvailable) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_new_releases_outline),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                    contentDescription = stringResource(id = R.string.update),
                                )
                            }
                        }
                    ) {
                        navigateTo(UPDATE_PAGE)
                    }
                }
            }

            item {
                val repository = stringResource(id = R.string.repository)
                SettingsItem(
                    icon = R.drawable.ic_github,
                    title = stringResource(id = R.string.github),
                    description = stringResource(id = R.string.github_description, repository),
                    itemShape = if (isGithubBuild) middleSmallItemShape else topItemShape
                ) {
                    uriHandle.openUri("https://github.com/acszo/Redomi")
                }
            }

            item {
                SettingsItem(
                    icon = R.drawable.ic_info_filled,
                    title = stringResource(id = R.string.version),
                    description = versionName,
                    itemShape = bottomItemShape
                ) {
                    scope.launch {
                        copyText(
                            clipboard = clipboard,
                            text = appVersion + '\n' + modelName + '\n' + androidVersion
                        )
                    }
                }
            }
        }
    }

    if (isFirstTime && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DefaultDialog(
            icon = R.drawable.ic_description,
            title = stringResource(id = R.string.dialog_setup_title),
            verticalSpaceBy = 16.dp,
            content = {
                Text(text = stringResource(id = R.string.dialog_setup_description, redomi))

                IconDescription(
                    icon = R.drawable.ic_done_all,
                    description = stringResource(id = R.string.dialog_setup_description_checked, redomi)
                )

                IconDescription(
                    icon = R.drawable.ic_remove_done,
                    description = stringResource(id = R.string.dialog_setup_description_unchecked)
                )
            },
            onConfirmAction = {
                dataStoreViewModel.setIsFirstTime()
                onIntentOpenDefaultsApp(context)
            }
        )
    }

    if (openIconShapeDialog) {
        DefaultDialog(
            icon = R.drawable.ic_category_outline,
            title = stringResource(id = R.string.icon_shape),
            content = {
                IconShape.entries.forEach { item ->
                    RadioButtonItemDialog(
                        item = item,
                        isSelected = item == IconShape.entries[iconShape],
                        onClick = dataStoreViewModel::setIconShape
                    )
                }
            },
            onDismissRequest = { openIconShapeDialog = false },
            onConfirmAction = { openIconShapeDialog = false }
        )
    }

    if (openThemeDialog) {
        DefaultDialog(
            icon = R.drawable.ic_color_lens_outline,
            title = stringResource(id = R.string.theme),
            content = {
                Theme.entries.forEach { item ->
                    RadioButtonItemDialog(
                        item = item,
                        isSelected = item == Theme.entries[themeMode],
                        onClick = dataStoreViewModel::setThemeMode
                    )
                }
            },
            onDismissRequest = { openThemeDialog = false },
            onConfirmAction = { openThemeDialog = false }
        )
    }

}