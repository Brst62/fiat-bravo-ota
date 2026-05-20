package com.nwd.diagnostic.collector.reports

import java.io.File

internal object ProcReader {
    /** /proc/cpuinfo, meminfo, version, loadavg, uptime — sinirli bayt */
    fun snapshot(): Map<String, String> = mapOf(
        "cpuinfo" to safeRead("/proc/cpuinfo", 8192),
        "meminfo" to safeRead("/proc/meminfo", 4096),
        "version" to safeRead("/proc/version", 1024),
        "loadavg" to safeRead("/proc/loadavg", 256),
        "uptime"  to safeRead("/proc/uptime", 256),
        "stat"    to safeRead("/proc/stat", 4096),
    )

    fun thermalZones(): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        val root = File("/sys/class/thermal")
        if (!root.exists()) return out
        root.listFiles { f -> f.name.startsWith("thermal_zone") }?.forEach { zone ->
            val type = runCatching { File(zone, "type").readText().trim() }.getOrNull() ?: zone.name
            val temp = runCatching { File(zone, "temp").readText().trim().toLong() }.getOrNull() ?: return@forEach
            val tc = if (temp > 1000) temp / 1000.0 else temp.toDouble()
            out[type] = tc
        }
        return out
    }

    private fun safeRead(path: String, max: Int): String {
        val f = File(path)
        if (!f.exists()) return ""
        return runCatching { f.bufferedReader().use { it.readText() }.take(max) }.getOrDefault("")
    }
}
