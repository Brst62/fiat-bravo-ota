package com.nwd.fiatlauncher.compat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Eski 16 NWD APK'sının broadcast'lerini yanıtlayan tek nokta.
 *
 * Migration süresince (vCode 22..29) gerekli — eski APK'lar henüz kaldırılmadıysa
 * gelen action'ları in-process AiBrain/OBD/CAN core modüllerine yönlendirir.
 * vCode 30+ kaldırılabilir.
 */
class LegacyIpcRouter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag("LegacyIpc").i("rx action=${intent.action} extras=${intent.extras?.keySet()}")
        // TODO: aksiyon başına in-process handler routing
        // when (intent.action) {
        //   "com.nwd.aibrain.AI_QUERY"  -> aiBrain.query(intent.getStringExtra("q"))
        //   "com.nwd.obd2.DATA_UPDATE"  -> obd2Bus.publish(intent.extras)
        //   "com.nwd.action.STEERING_KEY" -> steeringInput.dispatch(...)
        // }
    }
}
