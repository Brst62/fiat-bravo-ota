package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.nwd.diagnostic.collector.NetworkInfo
import java.net.NetworkInterface
import java.security.MessageDigest

internal object NetworkReader {
    fun snapshot(ctx: Context): NetworkInfo {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active = cm?.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> null
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
            else -> "other"
        }
        val ssid = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.connectionInfo?.ssid?.trim('"')
            ?.let { hash(it) }
        val ipv4 = firstIpv4()?.let { anonymize24(it) }
        return NetworkInfo(
            online = active != null && caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            type = type, ssid = ssid, ipv4 = ipv4,
        )
    }

    private fun firstIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains('.') == true }
            ?.hostAddress
    }.getOrNull()

    private fun anonymize24(ip: String): String =
        ip.split('.').let { if (it.size == 4) "${it[0]}.${it[1]}.${it[2]}.0/24" else "anon" }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(12)
}
