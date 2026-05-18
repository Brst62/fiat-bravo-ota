package com.nwd.core.common

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Stabil, anonim cihaz tanımlayıcı.
 *
 * IMEI / serial / MAC TOPLAMAZ — sadece ANDROID_ID'yi SHA-256'lar.
 * Cihaz factory reset olursa değişir. Telemetri repo'sunda klasör adı olur.
 */
object DeviceId {
    @Volatile private var cached: String? = null

    fun get(ctx: Context): String {
        cached?.let { return it }
        @Suppress("HardwareIds")
        val raw = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val seed = "$raw|${ctx.packageName}".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(seed)
        val hex = sha.joinToString("") { "%02x".format(it) }.take(16)
        val id = "dev-$hex"
        cached = id
        return id
    }

    fun newSessionId(): String = UUID.randomUUID().toString()
}
