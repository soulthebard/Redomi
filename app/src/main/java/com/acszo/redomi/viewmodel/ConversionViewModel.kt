package com.acszo.redomi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acszo.redomi.conversion.ConversionPlanner
import com.acszo.redomi.conversion.ConversionSession
import com.acszo.redomi.conversion.IncomingRequest
import com.acszo.redomi.conversion.SheetContent
import com.acszo.redomi.conversion.SheetContentBuilder
import com.acszo.redomi.data.AppList
import com.acszo.redomi.data.DataStoreConst.CLEAN_LINKS
import com.acszo.redomi.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Plans the conversion sheet from the incoming link, without any network request. */
@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
): ViewModel() {

    private val session = ConversionSession(viewModelScope) { plan(it) }

    val state = session.state

    /**
     * [request] holds the incoming link or shared text, untouched. Set [force] for a new intent
     * delivered to a running activity; without it a request equal to the current one is ignored.
     */
    fun submit(request: IncomingRequest, force: Boolean = false) = session.submit(request, force)

    fun retry() = session.retry()

    private suspend fun plan(request: IncomingRequest): SheetContent {
        val appList = if (request.isActionSend) AppList.SHARING else AppList.OPENING
        val selectedApps = settingsDataStore.getSetOfStrings(appList.key).first()
        val cleanLinks = settingsDataStore.getBoolean(CLEAN_LINKS).first() == true

        return SheetContentBuilder.build(
            result = ConversionPlanner.plan(request.input, cleanLinks),
            selectedApps = selectedApps,
            cleanLinks = cleanLinks
        )
    }

}
