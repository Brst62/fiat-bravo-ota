package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nwd.diagnostic.collector.BatteryInfo

internal object BatteryReader {
    fun snapshot(ctx: Context): BatteryInfo? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = ctx.registerReceiver(null, filter) ?: return null
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val volt = sticky.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val tempD = sticky.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else null
        return BatteryInfo(
            percent = pct,
            voltageMv = volt.takeIf { it > 0 },
            temperatureC = if (tempD > 0) tempD / 10.0 else null,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            status = status,
        )
    }
}
