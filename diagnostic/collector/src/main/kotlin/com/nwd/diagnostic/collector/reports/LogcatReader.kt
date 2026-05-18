package com.nwd.diagnostic.collector.reports

import com.nwd.diagnostic.collector.CrashEntry
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

internal object LogcatReader {
    /** Son N satırı yakalar, dosyaya yazar, dosya yolunu döner. */
    fun dump(targetDir: File, tag: String = "NWD"): File {
        targetDir.mkdirs()
        val out = File(targetDir, "logcat-$tag-${System.currentTimeMillis()}.txt")
        try {
            val pb = ProcessBuilder("logcat", "-d", "-t", "5000", "*:I").redirectErrorStream(true)
            val p = pb.start()
            BufferedReader(InputStreamReader(p.inputStream)).useLines { seq ->
                out.bufferedWriter().use { w ->
                    seq.take(5000).forEach { line -> w.write(line); w.newLine() }
                }
            }
            p.waitFor()
        } catch (_: Throwable) {
            // logcat read izni rooted/system tarafında daha açık — fail OK
        }
        return out
    }

    /** Logcat'ten "FATAL EXCEPTION" pattern'i ile crash özetlerini çıkarır. */
    fun recentCrashes(logcatFile: File): List<CrashEntry> {
        if (!logcatFile.exists()) return emptyList()
        val list = mutableListOf<CrashEntry>()
        logcatFile.useLines { seq ->
            seq.forEach { line ->
                if (line.contains("FATAL EXCEPTION") || line.contains("AndroidRuntime: java.lang")) {
                    val pkg = Regex("""[a-z][a-z0-9_.]+\.[A-Za-z]+""").find(line)?.value ?: "unknown"
                    list += CrashEntry(
                        pkg = pkg, tsMs = System.currentTimeMillis(),
                        summary = line.take(300),
                    )
                }
            }
        }
        return list.takeLast(20)
    }
}
