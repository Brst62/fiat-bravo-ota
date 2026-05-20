package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.nwd.diagnostic.collector.NwdPackageInfo
import java.security.MessageDigest

internal object NwdInventoryReader {

    private val PREFIX_WHITELIST = listOf("com.nwd.", "com.navimex.")

    fun snapshot(ctx: Context): List<NwdPackageInfo> {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS or
            PackageManager.GET_PERMISSIONS or
            PackageManager.GET_SIGNING_CERTIFICATES or
            PackageManager.MATCH_DISABLED_COMPONENTS or
            PackageManager.MATCH_UNINSTALLED_PACKAGES

        val all = runCatching { pm.getInstalledPackages(flags) }.getOrDefault(emptyList())
        return all
            .filter { pi -> PREFIX_WHITELIST.any { pi.packageName.startsWith(it) } }
            .map { pi -> map(pm, pi) }
            .sortedBy { it.pkg }
    }

    private fun map(pm: PackageManager, pi: PackageInfo): NwdPackageInfo {
        val appInfo: ApplicationInfo? = pi.applicationInfo
        val isSystem = (appInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
        val enabled = runCatching {
            appInfo?.enabled
                ?: (pm.getApplicationEnabledSetting(pi.packageName) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    pm.getApplicationEnabledSetting(pi.packageName) ==
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        }.getOrDefault(true)

        val installer = runCatching {
            pm.getInstallSourceInfo(pi.packageName).installingPackageName
        }.getOrNull()

        val signing = firstSigSha256(pi)

        return NwdPackageInfo(
            pkg = pi.packageName,
            versionCode = pi.longVersionCode,
            versionName = pi.versionName,
            installerPackage = installer,
            firstInstallMs = pi.firstInstallTime,
            lastUpdateMs = pi.lastUpdateTime,
            enabled = enabled,
            isSystem = isSystem,
            signingSha256 = signing,
            activities = pi.activities?.map { it.name }.orEmpty(),
            services = pi.services?.map { it.name }.orEmpty(),
            receivers = pi.receivers?.map { it.name }.orEmpty(),
            providers = pi.providers?.map { it.name }.orEmpty(),
            permissions = pi.permissions?.map { it.name }.orEmpty(),
            requestedPermissions = pi.requestedPermissions?.toList().orEmpty(),
        )
    }

    private fun firstSigSha256(pi: PackageInfo): String? {
        val sigs: Array<Signature>? = runCatching {
            pi.signingInfo?.let { info ->
                if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
            }
        }.getOrNull()
        val first = sigs?.firstOrNull() ?: return null
        val md = MessageDigest.getInstance("SHA-256").digest(first.toByteArray())
        return md.joinToString(":") { "%02X".format(it) }
    }
}
