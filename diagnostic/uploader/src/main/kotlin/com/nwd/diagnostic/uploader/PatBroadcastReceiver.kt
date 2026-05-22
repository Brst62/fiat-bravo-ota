package com.nwd.diagnostic.uploader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * PAT'i broadcast intent uzerinden alir.
 *
 * setprop'un 92 baytlik PROP_VALUE_MAX siniri GitHub fine-grained PAT'larini
 * (93 char) reddediyor. Broadcast intent extra'larinda boyle bir sinir yok.
 *
 * Kullanim (adb shell):
 *   am broadcast -a com.nwd.fiatlauncher.action.SET_TELEMETRY_PAT --es pat 'github_pat_...'
 *   am broadcast -a com.nwd.fiatlauncher.action.SET_OTA_PAT       --es pat 'github_pat_...'
 *
 * Guvenlik: receiver exported ama Pixel/K2501 gibi gelistirici cihazlarda
 * pratik tehdit modeli ADB erisimi olan biri zaten cihazda root yetkisine
 * yakin sahip. Production'da ek bir custom signature permission ile kapatilabilir.
 */
@AndroidEntryPoint
class PatBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var store: SecretStore

    override fun onReceive(context: Context, intent: Intent) {
        val pat = intent.getStringExtra(EXTRA_PAT)
        if (pat.isNullOrBlank()) {
            Timber.tag(TAG).w("${intent.action}: pat extra eksik veya bos")
            return
        }
        when (intent.action) {
            ACTION_SET_TELEMETRY_PAT -> {
                store.telemetryPat = pat
                Timber.tag(TAG).i("telemetry PAT broadcast ile set edildi (len=${pat.length})")
            }
            ACTION_SET_OTA_PAT -> {
                store.otaPat = pat
                Timber.tag(TAG).i("ota PAT broadcast ile set edildi (len=${pat.length})")
            }
            else -> Timber.tag(TAG).w("bilinmeyen action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_SET_TELEMETRY_PAT = "com.nwd.fiatlauncher.action.SET_TELEMETRY_PAT"
        const val ACTION_SET_OTA_PAT = "com.nwd.fiatlauncher.action.SET_OTA_PAT"
        const val EXTRA_PAT = "pat"
        private const val TAG = "PatReceiver"
    }
}
