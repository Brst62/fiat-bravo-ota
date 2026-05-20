package com.nwd.diagnostic.collector.reports

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

internal object DumpsysReader {

    private const val MAX_BYTES = 32 * 1024  // her komut için cap

    /**
     * Userspace'ten dumpsys çoğu servis için PERMISSION_DENIED dönebilir; başarılı
     * olanlar telemetri'ye eklenir. Cihazda sistem imzasıyla imzalıysak fazlası gelir.
     */
    fun snapshot(): Map<String, String> {
        val targets = listOf(
            "package" to listOf("dumpsys", "package", "com.nwd.fiatlauncher"),
            "package_features" to listOf("dumpsys", "package", "features"),
            "input" to listOf("dumpsys", "input"),
            "audio" to listOf("dumpsys", "audio"),
            "media_audio_focus" to listOf("dumpsys", "media.audio_focus"),
            "display" to listOf("dumpsys", "display"),
            "power" to listOf("dumpsys", "power"),
            "battery" to listOf("dumpsys", "battery"),
            "wifi" to listOf("dumpsys", "wifi"),
            "connectivity" to listOf("dumpsys", "connectivity"),
            "usb" to listOf("dumpsys", "usb"),
            "bluetooth_manager" to listOf("dumpsys", "bluetooth_manager"),
            "thermalservice" to listOf("dumpsys", "thermalservice"),
            "sensorservice" to listOf("dumpsys", "sensorservice"),
            "SurfaceFlinger" to listOf("dumpsys", "SurfaceFlinger", "--latency-clear"),
            "meminfo_summary" to listOf("dumpsys", "meminfo", "-a", "com.nwd.fiatlauncher"),
            "activity_recents" to listOf("dumpsys", "activity", "recents"),
        )
        val out = LinkedHashMap<String, String>()
        for ((name, cmd) in targets) {
            val s = runCmd(cmd)
            if (s.isNotBlank()) out[name] = s
        }
        return out
    }

    private fun runCmd(cmd: List<String>): String {
        return runCatching {
            val pb = ProcessBuilder(cmd).redirectErrorStream(true)
            val p = pb.start()
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(p.inputStream)).use { br ->
                var total = 0
                while (total < MAX_BYTES) {
                    val line = br.readLine() ?: break
                    sb.append(line).append('\n')
                    total += line.length + 1
                }
            }
            p.waitFor(5, TimeUnit.SECONDS)
            runCatching { p.destroy() }
            sb.toString()
        }.getOrDefault("")
    }
}
