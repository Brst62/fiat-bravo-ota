package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import com.nwd.diagnostic.collector.UsbDeviceInfo
import com.nwd.diagnostic.collector.UsbInfo

internal object UsbReader {
    fun snapshot(ctx: Context): UsbInfo {
        val hostSupported = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val mgr = ctx.getSystemService(Context.USB_SERVICE) as? UsbManager
        val devices = runCatching {
            mgr?.deviceList?.values?.map { d ->
                UsbDeviceInfo(
                    vendorId = d.vendorId,
                    productId = d.productId,
                    deviceClass = d.deviceClass,
                    deviceSubclass = d.deviceSubclass,
                    deviceProtocol = d.deviceProtocol,
                    manufacturerName = runCatching { d.manufacturerName }.getOrNull(),
                    productName = runCatching { d.productName }.getOrNull(),
                    interfaceCount = d.interfaceCount,
                )
            } ?: emptyList()
        }.getOrDefault(emptyList())
        return UsbInfo(hostSupported = hostSupported, devices = devices)
    }
}
