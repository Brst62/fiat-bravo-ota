package com.nwd.diagnostic.collector.reports

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.nwd.diagnostic.collector.BluetoothDeviceInfo
import com.nwd.diagnostic.collector.BluetoothInfo
import java.security.MessageDigest

internal object BluetoothReader {

    private val NAME_HINTS = listOf(
        "elm", "obd", "obdii", "obd2", "vgate", "konnwei", "fixd", "ancel"
    )

    fun snapshot(ctx: Context): BluetoothInfo? {
        val supported = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!supported) {
            return BluetoothInfo(
                supported = false,
                enabled = false,
                adapterName = null,
                bondedDevices = emptyList(),
                elm327Detected = false,
            )
        }

        val mgr = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = mgr?.adapter
        if (adapter == null) {
            return BluetoothInfo(
                supported = true,
                enabled = false,
                adapterName = null,
                bondedDevices = emptyList(),
                elm327Detected = false,
            )
        }

        val adapterName = runCatching { adapter.name }.getOrNull()?.let(::hash12)
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)

        val bonded: Set<BluetoothDevice> = runCatching {
            adapter.bondedDevices ?: emptySet()
        }.getOrDefault(emptySet())

        var elm = false
        val list = bonded.map { dev ->
            val rawName = runCatching { dev.name }.getOrNull()
            val lower = rawName?.lowercase().orEmpty()
            val hint = NAME_HINTS.firstOrNull { it in lower }
            if (hint != null) elm = true
            BluetoothDeviceInfo(
                nameHash = rawName?.let(::hash12),
                nameHint = hint,
                bondState = runCatching { dev.bondState }.getOrDefault(0),
                deviceClass = runCatching { dev.bluetoothClass?.deviceClass ?: 0 }.getOrDefault(0),
                majorClass = runCatching { dev.bluetoothClass?.majorDeviceClass ?: 0 }.getOrDefault(0),
                type = runCatching { dev.type }.getOrDefault(0),
                uuids = runCatching {
                    dev.uuids?.map { it.uuid.toString() } ?: emptyList()
                }.getOrDefault(emptyList()),
            )
        }

        return BluetoothInfo(
            supported = true,
            enabled = enabled,
            adapterName = adapterName,
            bondedDevices = list,
            elm327Detected = elm,
        )
    }

    private fun hash12(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(12)
}
