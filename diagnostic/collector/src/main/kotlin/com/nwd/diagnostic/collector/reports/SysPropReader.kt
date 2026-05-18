package com.nwd.diagnostic.collector.reports

/**
 * Seçici getprop okuyucusu (yansıma ile android.os.SystemProperties).
 *
 * Hassas property'leri (serial, IMEI gibi) toplamaz.
 */
internal object SysPropReader {
    private val WHITELIST = listOf(
        "ro.product.board",
        "ro.product.brand",
        "ro.product.cpu.abi",
        "ro.product.cpu.abilist",
        "ro.product.device",
        "ro.product.manufacturer",
        "ro.product.model",
        "ro.product.name",
        "ro.build.fingerprint",
        "ro.build.id",
        "ro.build.tags",
        "ro.build.type",
        "ro.build.version.release",
        "ro.build.version.sdk",
        "ro.build.version.security_patch",
        "ro.boot.hardware",
        "ro.boot.bootloader",
        "ro.hardware",
        "ro.soc.manufacturer",
        "ro.soc.model",
        "ro.vendor.product.board",
        "ro.kernel.version",
        "persist.sys.locale",
        "persist.sys.timezone",
    )

    fun read(): Map<String, String> {
        val cls = runCatching { Class.forName("android.os.SystemProperties") }.getOrNull()
            ?: return emptyMap()
        val getter = runCatching {
            cls.getMethod("get", String::class.java, String::class.java)
        }.getOrNull() ?: return emptyMap()
        return WHITELIST.associateWith { key ->
            runCatching { getter.invoke(null, key, "") as String }.getOrDefault("")
        }.filterValues { it.isNotEmpty() }
    }
}
