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
    val bluetooth: BluetoothInfo?,
    val usb: UsbInfo?,
    val displays: List<DisplayInfo>,
    val sensors: List<SensorInfo>,
    val audio: AudioInfo?,
    val nwdInventory: List<NwdPackageInfo>,
    val dumpsys: Map<String, String>,
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
data class BluetoothInfo(
    val supported: Boolean,
    val enabled: Boolean,
    val adapterName: String?,   // hashed
    val bondedDevices: List<BluetoothDeviceInfo>,
    val elm327Detected: Boolean,
)

@JsonClass(generateAdapter = true)
data class BluetoothDeviceInfo(
    val nameHash: String?,      // hashed name (privacy)
    val nameHint: String?,      // "OBDII"/"ELM327" gibi tanımlayıcı bir alt-string kalıbı varsa
    val bondState: Int,
    val deviceClass: Int,
    val majorClass: Int,
    val type: Int,
    val uuids: List<String>,
)

@JsonClass(generateAdapter = true)
data class UsbInfo(
    val hostSupported: Boolean,
    val devices: List<UsbDeviceInfo>,
)

@JsonClass(generateAdapter = true)
data class UsbDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val manufacturerName: String?,
    val productName: String?,
    val interfaceCount: Int,
)

@JsonClass(generateAdapter = true)
data class DisplayInfo(
    val displayId: Int,
    val name: String,
    val state: Int,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val rotation: Int,
)

@JsonClass(generateAdapter = true)
data class SensorInfo(
    val name: String,
    val vendor: String,
    val type: Int,
    val stringType: String,
    val version: Int,
    val power: Float,
    val resolution: Float,
    val maximumRange: Float,
)

@JsonClass(generateAdapter = true)
data class AudioInfo(
    val musicVolume: Int,
    val musicVolumeMax: Int,
    val ringerMode: Int,
    val outputs: List<AudioDeviceEntry>,
    val inputs: List<AudioDeviceEntry>,
)

@JsonClass(generateAdapter = true)
data class AudioDeviceEntry(
    val id: Int,
    val type: Int,
    val typeName: String,
    val productName: String?,
    val isSink: Boolean,
    val sampleRates: List<Int>,
    val channelCounts: List<Int>,
)

@JsonClass(generateAdapter = true)
data class NwdPackageInfo(
    val pkg: String,
    val versionCode: Long,
    val versionName: String?,
    val installerPackage: String?,
    val firstInstallMs: Long,
    val lastUpdateMs: Long,
    val enabled: Boolean,
    val isSystem: Boolean,
    val signingSha256: String?,        // imza fingerprint'i (apk doğrulama için)
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val permissions: List<String>,
    val requestedPermissions: List<String>,
)

@JsonClass(generateAdapter = true)
data class CollectedArtifact(
    override val path: String,         // tar.gz dosya yolu
    override val sizeBytes: Long,
    val report: DiagnosticReport,
) : TelemetryArtifact
