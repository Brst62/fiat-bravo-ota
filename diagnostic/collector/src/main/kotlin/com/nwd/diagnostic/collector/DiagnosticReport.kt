package com.nwd.diagnostic.collector

import com.nwd.diagnostic.uploader.TelemetryArtifact
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiagnosticReport(
    val deviceId: String,
    val sessionId: String,
    val createdAtMs: Long,
    val platformTag: String,            // "navimex" | "k2501-t507" | ...
    val build: Map<String, Any?>,
    val packages: List<PackageInfoEntry>,
    val proc: Map<String, String>,      // /proc/* anahtarları (kısaltılmış)
    val thermalC: Map<String, Double>,
    val sysProps: Map<String, String>,  // getprop seçmeli
    val storage: StorageInfo,
    val battery: BatteryInfo?,
    val network: NetworkInfo,
    val recentCrashes: List<CrashEntry>,
    val canLogPath: String?,
)

@JsonClass(generateAdapter = true)
data class PackageInfoEntry(
    val pkg: String,
    val versionCode: Long,
    val versionName: String?,
    val isSystem: Boolean,
    val installerPackage: String?,
)

@JsonClass(generateAdapter = true)
data class StorageInfo(
    val internalTotalMb: Long,
    val internalFreeMb: Long,
    val dataTotalMb: Long,
    val dataFreeMb: Long,
)

@JsonClass(generateAdapter = true)
data class BatteryInfo(
    val percent: Int?,
    val voltageMv: Int?,
    val temperatureC: Double?,
    val isCharging: Boolean?,
    val status: Int?,
)

@JsonClass(generateAdapter = true)
data class NetworkInfo(
    val online: Boolean,
    val type: String?,        // wifi/cell/eth
    val ssid: String?,        // hashed
    val ipv4: String?,        // local — anonimleştirilmiş /24
)

@JsonClass(generateAdapter = true)
data class CrashEntry(
    val pkg: String,
    val tsMs: Long,
    val summary: String,      // logcat'ten kısa "FATAL EXCEPTION: ..." satırı
)

@JsonClass(generateAdapter = true)
data class CollectedArtifact(
    override val path: String,         // tar.gz dosya yolu
    override val sizeBytes: Long,
    val report: DiagnosticReport,
) : TelemetryArtifact
