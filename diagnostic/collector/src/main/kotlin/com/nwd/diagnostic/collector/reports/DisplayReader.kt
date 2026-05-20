package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import com.nwd.diagnostic.collector.DisplayInfo

internal object DisplayReader {
    fun snapshot(ctx: Context): List<DisplayInfo> {
        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return emptyList()
        val list = mutableListOf<DisplayInfo>()
        dm.displays.forEach { d ->
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            d.getRealMetrics(metrics)
            list += DisplayInfo(
                displayId = d.displayId,
                name = runCatching { d.name }.getOrDefault("display-${d.displayId}"),
                state = runCatching { d.state }.getOrDefault(Display.STATE_UNKNOWN),
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                refreshRateHz = runCatching { d.refreshRate }.getOrDefault(0f),
                rotation = runCatching { d.rotation }.getOrDefault(0),
            )
        }
        return list
    }
}
