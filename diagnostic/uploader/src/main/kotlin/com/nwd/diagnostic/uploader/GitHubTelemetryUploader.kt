package com.nwd.diagnostic.uploader

import android.content.Context
import android.util.Base64
import com.nwd.core.common.BuildInfo
import com.nwd.core.common.DeviceId
import com.nwd.diagnostic.uploader.api.GitHubApi
import com.nwd.diagnostic.uploader.api.PutContentBody
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetryArtifact {
    val path: String
    val sizeBytes: Long
}

@Singleton
class GitHubTelemetryUploader @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: GitHubApi,
    private val secrets: SecretStore,
) {
    private val repo = BuildConfig.TELEMETRY_REPO   // "owner/name"
    private val dateFmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    /**
     * Outbox klasöründeki TÜM tar.gz dosyalarını sırayla push'lar.
     * Başarılı olanlar silinir. Hiçbiri yoksa true döner.
     */
    suspend fun upload(artifact: TelemetryArtifact): Boolean = uploadFromOutbox()

    suspend fun uploadFromOutbox(): Boolean {
        val outbox = File(ctx.filesDir, "telemetry-outbox")
        if (!outbox.exists()) return true
        val files = outbox.listFiles { f -> f.isFile && f.name.endsWith(".tar.gz") }
            ?.sortedBy { it.lastModified() } ?: return true
        if (files.isEmpty()) return true

        val pat = secrets.telemetryPat
        if (pat.isNullOrBlank()) {
            Timber.tag(TAG).w("telemetry PAT eksik — yükleme atlandı; outbox bekliyor (${files.size} dosya)")
            return false
        }
        val (owner, name) = parseRepo(repo) ?: run {
            Timber.tag(TAG).e("repo parse fail: $repo"); return false
        }

        val info = BuildInfo.of(ctx)
        val deviceId = DeviceId.get(ctx)

        var allOk = true
        for (f in files) {
            val path = remotePath(deviceId, info.platformTag, f)
            val content = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
            val msg = "telemetry: ${info.platformTag}/$deviceId/${f.name} (${f.length()}B)"
            val resp = runCatching {
                api.putFile(
                    auth = "Bearer $pat",
                    owner = owner, repo = name, path = path,
                    body = PutContentBody(message = msg, content = content),
                )
            }.getOrNull()
            if (resp?.isSuccessful == true) {
                Timber.tag(TAG).i("uploaded → $path")
                f.delete()
            } else {
                Timber.tag(TAG).w("upload fail code=${resp?.code()} body=${resp?.errorBody()?.string()?.take(200)}")
                allOk = false
                break // sırayla, ilk başarısızda dur — sonraki worker turunda yeniden denenir
            }
        }
        return allOk
    }

    private fun remotePath(deviceId: String, platform: String, file: File): String {
        val day = dateFmt.format(Date(file.lastModified())).take(8) // yyyyMMdd
        // ör: telemetry/navimex/dev-1a2b/20260518/telemetry-dev-1a2b-1747534567.tar.gz
        return "telemetry/$platform/$deviceId/$day/${file.name}"
    }

    private fun parseRepo(s: String): Pair<String, String>? {
        val parts = s.split("/")
        return if (parts.size == 2) parts[0] to parts[1] else null
    }

    companion object { private const val TAG = "TelemUploader" }
}
