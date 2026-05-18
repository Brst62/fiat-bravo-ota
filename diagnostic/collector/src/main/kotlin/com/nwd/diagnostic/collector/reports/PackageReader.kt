package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.nwd.diagnostic.collector.PackageInfoEntry

internal object PackageReader {
    fun list(ctx: Context): List<PackageInfoEntry> {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
        return pm.getInstalledPackages(flags).map { pi ->
            val isSystem = (pi.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val installer = runCatching {
                pm.getInstallSourceInfo(pi.packageName).installingPackageName
            }.getOrNull()
            PackageInfoEntry(
                pkg = pi.packageName,
                versionCode = pi.longVersionCode,
                versionName = pi.versionName,
                isSystem = isSystem,
                installerPackage = installer,
            )
        }.sortedBy { it.pkg }
    }
}
