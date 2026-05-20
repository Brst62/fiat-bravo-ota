package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.nwd.diagnostic.collector.SensorInfo

internal object SensorReader {
    fun snapshot(ctx: Context): List<SensorInfo> {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return emptyList()
        val list = runCatching { sm.getSensorList(Sensor.TYPE_ALL) }.getOrNull().orEmpty()
        return list.map { s ->
            SensorInfo(
                name = s.name.orEmpty(),
                vendor = s.vendor.orEmpty(),
                type = s.type,
                stringType = s.stringType.orEmpty(),
                version = s.version,
                power = s.power,
                resolution = s.resolution,
                maximumRange = s.maximumRange,
            )
        }
    }
}
