package com.nwd.ota.github

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.core.content.FileProvider
import com.nwd.core.common.BuildInfo
import com.nwd.diagnostic.uploader.SecretStore
import com.nwd.diagnostic.uploader.api.GitHubApi
import com.nwd.diagnostic.uploader.api.ReleaseAssetDto
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class OtaResult {
    data object NoUpdate : OtaResult()
    data class Available(val tag: String, val versionCode: Int, val apk: File) : OtaResult()
    data class Failed(val reason: String) : OtaResult()
}

@Singleton
class GitHubOtaUpdater @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: GitHubApi,
    private val secrets: SecretStore,
    private val http: OkHttpClient,
) {
    private val repo = BuildConfig.OTA_REPO

    /**
     * 1) latest release çek
     * 2) tag'den versionCode parse et (örn. v22 → 22, ya da release body içinde "versionCode: 22")
     * 3) Mevcut sürümden büyükse → uygun asset'i indir
     * 4) Available döner; install ayrı çağrıyla yapılır (kullanıcıya kontrol verilmesi için)
     */
    suspend fun checkAndDownload(): OtaResult {
        val (owner, name) = parseRepo(repo) ?: return OtaResult.Failed("repo parse")
        val pat = secrets.otaPat // public repo'da null OK; rate-limit için PAT önerilir
        val auth = pat?.let { "Bearer $it" } ?: ""

        val resp = runCatching {
            api.latestRelease(auth = auth, owner = owner, repo = name)
        }.getOrNull() ?: return OtaResult.Failed("network")

        if (!resp.isSuccessful) {
            return OtaResult.Failed("http ${resp.code()}")
        }
        val rel = resp.body() ?: return OtaResult.Failed("empty body")
        if (rel.draft || rel.prerelease) return OtaResult.NoUpdate

        val tagVersion = parseTagVersion(rel.tag_name) ?: return OtaResult.Failed("tag parse: ${rel.tag_name}")
        val current = BuildInfo.of(ctx).appVersionCode
        if (tagVersion <= current) {
            Timber.tag(TAG).i("no update: latest=$tagVersion current=$current")
            return OtaResult.NoUpdate
        }

        val asset = pickAsset(rel.assets) ?: return OtaResult.Failed("no apk asset")
        val apk = downloadApk(asset, auth) ?: return OtaResult.Failed("download fail")
        return OtaResult.Available(rel.tag_name, tagVersion, apk)
    }

    /** PackageInstaller session ile APK kurar — REQUEST_INSTALL_PACKAGES gerekli. */
    fun install(apk: File): Boolean = runCatching {
        val authority = "${ctx.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(ctx, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(intent)
        true
    }.getOrElse { Timber.tag(TAG).e(it); false }

    private fun pickAsset(assets: List<ReleaseAssetDto>): ReleaseAssetDto? {
        // İsim önceliği: app-release.apk → *.apk
        return assets.firstOrNull { it.name.equals("app-release.apk", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    }

    private fun downloadApk(asset: ReleaseAssetDto, auth: String): File? {
        val out = File(ctx.cacheDir, "ota/${asset.name}").apply { parentFile?.mkdirs() }
        val req = Request.Builder()
            .url(asset.browser_download_url)
            .apply { if (auth.isNotBlank()) header("Authorization", auth) }
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.byteStream()?.use { input -> out.outputStream().use { input.copyTo(it) } }
            }
            if (out.length() == asset.size || out.length() > 0) out else null
        }.getOrNull()
    }

    private fun parseRepo(s: String): Pair<String, String>? {
        val parts = s.split("/"); return if (parts.size == 2) parts[0] to parts[1] else null
    }

    /**
     * Tag formatı: "v22" | "v22.0.1" | "10.0.0-vc22" | "vc22"
     * versionCode tag'den int olarak çekilir.
     */
    private fun parseTagVersion(tag: String): Int? {
        // Önce "vc<N>" patterni
        Regex("""vc(\d+)""", RegexOption.IGNORE_CASE).find(tag)?.let { return it.groupValues[1].toInt() }
        // Sonra "v<N>" (sadece başta)
        Regex("""^v(\d+)""", RegexOption.IGNORE_CASE).find(tag)?.let { return it.groupValues[1].toInt() }
        return null
    }

    companion object { private const val TAG = "OtaUpdater" }
}
