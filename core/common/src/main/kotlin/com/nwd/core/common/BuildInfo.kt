package com.nwd.core.common

import android.content.Context
import android.os.Build

/** Cihaz + uygulama kimliği — telemetri ve OTA için */
data class BuildInfo(
    val appVersionCode: Int,
    val appVersionName: String,
    val applicationId: String,
    val androidVersion: String,
    val sdkInt: Int,
    val board: String,
    val device: String,
    val manufacturer: String,
    val model: String,
    val product: String,
    val hardware: String,
    val soc: String,
    val fingerprint: String,
    val cpuAbi: String,
    val supportedAbis: List<String>,
    val installSource: String,
) {
    val platformTag: String = when {
        board.contains("navimex", ignoreCase = true) -> "navimex"
        soc.contains("t507", ignoreCase = true) -> "k2501-t507"
        product.contains("k2501", ignoreCase = true) -> "k2501-t507"
        else -> "unknown-$board-$soc"
    }

    companion object {
        fun of(ctx: Context): BuildInfo {
            val pkg = ctx.packageName
            val pi = ctx.packageManager.getPackageInfo(pkg, 0)
            val installer = runCatching {
                ctx.packageManager.getInstallSourceInfo(pkg).installingPackageName ?: "unknown"
            }.getOrDefault("unknown")
            @Suppress("DEPRECATION")
            val versionCode = pi.longVersionCode.toInt()
            return BuildInfo(
                appVersionCode = versionCode,
                appVersionName = pi.versionName.orEmpty(),
                applicationId = pkg,
                androidVersion = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                board = Build.BOARD,
                device = Build.DEVICE,
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                product = Build.PRODUCT,
                hardware = Build.HARDWARE,
                soc = if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER}/${Build.SOC_MODEL}" else "unknown",
                fingerprint = Build.FINGERPRINT,
                cpuAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                installSource = installer,
            )
        }
    }
}
