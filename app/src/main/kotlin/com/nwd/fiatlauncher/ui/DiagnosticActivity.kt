package com.nwd.fiatlauncher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nwd.diagnostic.collector.DiagnosticCollector
import com.nwd.diagnostic.collector.worker.DiagnosticWorker
import com.nwd.fiatlauncher.databinding.ActivityDiagnosticBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DiagnosticActivity : AppCompatActivity() {

    @Inject lateinit var collector: DiagnosticCollector
    private lateinit var binding: ActivityDiagnosticBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnCollectNow.setOnClickListener { runCollect() }
        binding.btnUploadNow.setOnClickListener {
            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<DiagnosticWorker>().build())
            binding.txtStatus.text = "Yükleme tetiklendi (WorkManager)..."
        }
    }

    private fun runCollect() {
        binding.txtStatus.text = "Toplanıyor..."
        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) { collector.collect() }
            binding.txtStatus.text = "Topladı: ${report.path}  (${report.sizeBytes} bytes)"
        }
    }
}
