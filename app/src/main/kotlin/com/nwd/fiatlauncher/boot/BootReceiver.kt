package com.nwd.fiatlauncher.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nwd.diagnostic.collector.worker.DiagnosticWorker
import com.nwd.ota.github.GitHubOtaWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Açılışta her ikisi de tek-seferlik tetiklenir; periyodikler zaten Application'da kayıtlı.
        val wm = WorkManager.getInstance(context)
        wm.enqueue(OneTimeWorkRequestBuilder<DiagnosticWorker>()
            .setConstraints(DiagnosticWorker.constraints()).build())
        wm.enqueue(OneTimeWorkRequestBuilder<GitHubOtaWorker>()
            .setConstraints(GitHubOtaWorker.constraints()).build())
    }
}
