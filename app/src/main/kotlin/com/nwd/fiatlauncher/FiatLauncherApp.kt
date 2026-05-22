package com.nwd.fiatlauncher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nwd.diagnostic.collector.worker.DiagnosticWorker
import com.nwd.diagnostic.uploader.SecretBootstrap
import com.nwd.ota.github.GitHubOtaWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FiatLauncherApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var secretBootstrap: SecretBootstrap

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Release'te de logcat'e yaz — geliştirme aşaması, observability gerekli
        Timber.plant(Timber.DebugTree())
        secretBootstrap.importFromSystemProps()
        scheduleBackgroundWork()
    }

    private fun scheduleBackgroundWork() {
        val wm = WorkManager.getInstance(this)
        // v23: developer-mode telemetri — WorkManager minimum periyod 15dk
        wm.enqueueUniquePeriodicWork(
            DiagnosticWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DiagnosticWorker>(15, TimeUnit.MINUTES)
                .setConstraints(DiagnosticWorker.constraints())
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            GitHubOtaWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GitHubOtaWorker>(12, TimeUnit.HOURS)
                .setConstraints(GitHubOtaWorker.constraints())
                .build()
        )
    }
}
