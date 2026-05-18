package com.nwd.diagnostic.uploader

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences ile PAT saklama.
 *
 * Token cihaza pair akışında ulaşır (QR / Settings / adb shell setprop) —
 * APK içine HARDCODE EDİLMEZ. Decompile edilince herkes telemetry repo'na yazar.
 */
@Singleton
class SecretStore @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val prefs by lazy {
        val key = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx, "nwd_secrets", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var telemetryPat: String?
        get() = prefs.getString(KEY_TELEMETRY_PAT, null)
        set(v) { prefs.edit().putString(KEY_TELEMETRY_PAT, v).apply() }

    var otaPat: String?
        get() = prefs.getString(KEY_OTA_PAT, null)
        set(v) { prefs.edit().putString(KEY_OTA_PAT, v).apply() }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_TELEMETRY_PAT = "telemetry_pat"
        private const val KEY_OTA_PAT = "ota_pat"
    }
}
