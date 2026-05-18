package com.nwd.diagnostic.collector.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.nwd.diagnostic.collector.DiagnosticCollector
import com.nwd.diagnostic.uploader.GitHubTelemetryUploader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DiagnosticWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val collector: DiagnosticCollector,
    private val uploader: GitHubTelemetryUploader,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val artifact = collector.collect()
        val ok = uploader.upload(artifact)
        if (ok) Result.success() else Result.retry()
    }.getOrElse { t ->
        Timber.tag(TAG).e(t, "doWork failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "nwd.diagnostic.periodic"
        private const val TAG = "DiagWorker"

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // sadece WiFi
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
