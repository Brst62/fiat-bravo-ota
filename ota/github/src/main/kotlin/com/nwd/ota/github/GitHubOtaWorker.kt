package com.nwd.ota.github

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class GitHubOtaWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ota: GitHubOtaUpdater,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val res = ota.checkAndDownload()
        Timber.tag(TAG).i("ota=$res")
        return when (res) {
            is OtaResult.Available -> {
                // İndirme tamam — kurulum kullanıcı onayıyla. Notification ile bildir.
                // Şimdilik doğrudan install çağrısı (kioskta arzu edilen).
                if (ota.install(res.apk)) Result.success() else Result.retry()
            }
            OtaResult.NoUpdate -> Result.success()
            is OtaResult.Failed -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "nwd.ota.periodic"
        private const val TAG = "OtaWorker"
        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
