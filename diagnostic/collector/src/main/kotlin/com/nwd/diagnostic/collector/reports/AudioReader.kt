package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.nwd.diagnostic.collector.AudioDeviceEntry
import com.nwd.diagnostic.collector.AudioInfo

internal object AudioReader {
    fun snapshot(ctx: Context): AudioInfo? {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return null
        val musicVol = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val musicMax = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val ringer = runCatching { am.ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)

        val outs = runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map(::toEntry)
        }.getOrDefault(emptyList())
        val ins = runCatching {
            am.getDevices(AudioManager.GET_DEVICES_INPUTS).map(::toEntry)
        }.getOrDefault(emptyList())

        return AudioInfo(
            musicVolume = musicVol,
            musicVolumeMax = musicMax,
            ringerMode = ringer,
            outputs = outs,
            inputs = ins,
        )
    }

    private fun toEntry(d: AudioDeviceInfo): AudioDeviceEntry = AudioDeviceEntry(
        id = d.id,
        type = d.type,
        typeName = typeName(d.type),
        productName = runCatching { d.productName?.toString() }.getOrNull(),
        isSink = d.isSink,
        sampleRates = d.sampleRates?.toList().orEmpty(),
        channelCounts = d.channelCounts?.toList().orEmpty(),
    )

    private fun typeName(t: Int): String = when (t) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "LINE_DIGITAL"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI_ARC"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_AUX_LINE -> "AUX_LINE"
        AudioDeviceInfo.TYPE_FM -> "FM"
        AudioDeviceInfo.TYPE_FM_TUNER -> "FM_TUNER"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
        AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
        AudioDeviceInfo.TYPE_DOCK -> "DOCK"
        else -> "type_$t"
    }
}
