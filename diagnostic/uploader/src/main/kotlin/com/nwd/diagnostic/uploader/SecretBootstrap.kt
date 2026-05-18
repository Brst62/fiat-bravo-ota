package com.nwd.diagnostic.uploader

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boot/onCreate'te calistirilir.
 *
 * adb shell setprop persist.nwd.telemetry_pat ghp_xxx
 *   ile cihaza yuklenen PAT'i okur, SecretStore'a yazar.
 *
 * Not: persist.nwd.* prop'lari SETPROP icin selinux izni ister; Android 13+
 * unrooted cihazlarda "persist.nwd." prefix'i bir vendor selinux policy ile
 * acilmis olmali. Acik degilse manuel UI ile PAT girisi gerekli (Settings'te
 * "PAT yapistir" alani — sonraki turda eklenecek).
 */
@Singleton
class SecretBootstrap @Inject constructor(
    private val store: SecretStore,
) {
    fun importFromSystemProps() {
        val tele = readProp(PROP_TELEMETRY)
        val ota = readProp(PROP_OTA)

        if (!tele.isNullOrBlank()) {
            store.telemetryPat = tele
            Timber.tag(TAG).i("telemetry PAT imported from setprop (len=${tele.length})")
            clearProp(PROP_TELEMETRY)
        }
        if (!ota.isNullOrBlank()) {
            store.otaPat = ota
            Timber.tag(TAG).i("ota PAT imported from setprop (len=${ota.length})")
            clearProp(PROP_OTA)
        }
    }

    private fun readProp(key: String): String? {
        val cls = runCatching { Class.forName("android.os.SystemProperties") }.getOrNull() ?: return null
        val m = runCatching { cls.getMethod("get", String::class.java, String::class.java) }.getOrNull() ?: return null
        return runCatching { (m.invoke(null, key, "") as String).takeIf { it.isNotBlank() } }.getOrNull()
    }

    /**
     * setprop'u silmek icin SystemProperties.set() yansimasi — write izni
     * vendor'a aittir. Basarisiz olursa prop'da kalir; bir sonraki boot'ta
     * yeniden import edilir (idempotent: SharedPrefs uzerine ayni degeri yazar).
     */
    private fun clearProp(key: String) {
        val cls = runCatching { Class.forName("android.os.SystemProperties") }.getOrNull() ?: return
        val m = runCatching { cls.getMethod("set", String::class.java, String::class.java) }.getOrNull() ?: return
        runCatching { m.invoke(null, key, "") }
    }

    companion object {
        const val PROP_TELEMETRY = "persist.nwd.telemetry_pat"
        const val PROP_OTA = "persist.nwd.ota_pat"
        private const val TAG = "SecretBootstrap"
    }
}
