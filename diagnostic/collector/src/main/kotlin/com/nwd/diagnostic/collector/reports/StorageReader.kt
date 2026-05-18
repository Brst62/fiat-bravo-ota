package com.nwd.diagnostic.collector.reports

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.nwd.diagnostic.collector.StorageInfo

internal object StorageReader {
    fun snapshot(ctx: Context): StorageInfo {
        val ext = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val data = StatFs(ctx.filesDir.absolutePath)
        fun mb(b: Long) = b / 1024 / 1024
        return StorageInfo(
            internalTotalMb = mb(ext.blockSizeLong * ext.blockCountLong),
            internalFreeMb = mb(ext.blockSizeLong * ext.availableBlocksLong),
            dataTotalMb = mb(data.blockSizeLong * data.blockCountLong),
            dataFreeMb = mb(data.blockSizeLong * data.availableBlocksLong),
        )
    }
}
