package com.acszo.redomi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acszo.redomi.conversion.ConversionPlanner
import com.acszo.redomi.conversion.SheetContent
import com.acszo.redomi.conversion.SheetContentBuilder
import com.acszo.redomi.data.DataStoreConst.CLEAN_LINKS
import com.acszo.redomi.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Plans the conversion sheet from the incoming link, without any network request. */
@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
): ViewModel() {

    private val _content = MutableStateFlow<SheetContent?>(null)

    /** Null until the first plan is ready. */
    val content = _content.asStateFlow()

    /** [input] is the incoming link or shared text, untouched; [appListKey] picks the selected apps. */
    fun plan(input: String?, appListKey: String) = viewModelScope.launch {
        val selectedApps = settingsDataStore.getSetOfStrings(appListKey).first()
        val cleanLinks = settingsDataStore.getBoolean(CLEAN_LINKS).first() == true

        _content.value = SheetContentBuilder.build(
            result = ConversionPlanner.plan(input, cleanLinks),
            selectedApps = selectedApps,
            cleanLinks = cleanLinks
        )
    }

}
