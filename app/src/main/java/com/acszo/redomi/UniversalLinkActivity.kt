package com.acszo.redomi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.acszo.redomi.data.DataStoreConst.CLEAN_LINKS
import com.acszo.redomi.data.SettingsDataStore
import com.acszo.redomi.sharing.UniversalLinkBuilder
import com.acszo.redomi.sharing.UniversalLinkFailure
import com.acszo.redomi.sharing.UniversalLinkResult
import com.acszo.redomi.utils.IntentUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UniversalLinkActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val cleanLinks = runCatching {
            runBlocking { SettingsDataStore(applicationContext).getBoolean(CLEAN_LINKS).first() }
        }.getOrNull() == true

        when (val result = UniversalLinkBuilder.build(sharedText, cleanLinks)) {
            is UniversalLinkResult.Success -> IntentUtil.onIntentSend(this@UniversalLinkActivity, result.universalUrl)
            is UniversalLinkResult.Failure -> {
                val message = when (result.reason) {
                    UniversalLinkFailure.UNSUPPORTED_SERVICE,
                    UniversalLinkFailure.UNSUPPORTED_CONTENT -> R.string.error_unsupported_link
                    else -> R.string.error_incorrect_url
                }
                Toast.makeText(this@UniversalLinkActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
        this.finish()
    }

}
