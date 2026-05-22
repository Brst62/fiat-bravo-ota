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
        // persist.* prefix'i vendor SELinux policy gerektiriyor (Pixel'de bloklu).
        // Önce persist.*'ı dene, yoksa debug.*'a düş — debug.* shell user'ı için
        // izinli, reboot'ta uçar ama setprop ile her zaman yeniden yazılabilir.
        val tele = readProp(PROP_TELEMETRY) ?: readProp(PROP_TELEMETRY_DEBUG)
        val ota = readProp(PROP_OTA) ?: readProp(PROP_OTA_DEBUG)

        if (!tele.isNullOrBlank()) {
            store.telemetryPat = tele
            Timber.tag(TAG).i("telemetry PAT imported from setprop (len=${tele.length})")
            clearProp(PROP_TELEMETRY)
            clearProp(PROP_TELEMETRY_DEBUG)
        }
        if (!ota.isNullOrBlank()) {
            store.otaPat = ota
            Timber.tag(TAG).i("ota PAT imported from setprop (len=${ota.length})")
            clearProp(PROP_OTA)
            clearProp(PROP_OTA_DEBUG)
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
        const val PROP_TELEMETRY_DEBUG = "debug.nwd.telemetry_pat"
        const val PROP_OTA_DEBUG = "debug.nwd.ota_pat"
        private const val TAG = "SecretBootstrap"
    }
}
