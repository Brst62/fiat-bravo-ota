package com.nwd.diagnostic.uploader.api

import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Telemetry upload yolu: Contents API (PUT /repos/{owner}/{repo}/contents/{path}).
 * Avantaj: tek HTTP çağrısı, gzip içeriği base64 ile push'lanır, git commit oluşur.
 * Kısıtlar: dosya başına ≤ 100 MB (bizim payload ≤ 1 MB hedef).
 */
interface GitHubApi {

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFile(
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2022-11-28",
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body body: PutContentBody,
    ): Response<PutContentResponse>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Header("Authorization") auth: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("X-GitHub-Api-Version") apiVersion: String = "2022-11-28",
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<ReleaseDto>
}

@JsonClass(generateAdapter = true)
data class PutContentBody(
    val message: String,
    val content: String, // base64
    val branch: String? = "main",
)

@JsonClass(generateAdapter = true)
data class PutContentResponse(val commit: CommitDto?, val content: ContentDto?)

@JsonClass(generateAdapter = true)
data class CommitDto(val sha: String?, val html_url: String?)

@JsonClass(generateAdapter = true)
data class ContentDto(val sha: String?, val html_url: String?, val download_url: String?)

@JsonClass(generateAdapter = true)
data class ReleaseDto(
    val id: Long,
    val tag_name: String,
    val name: String?,
    val body: String?,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: List<ReleaseAssetDto>,
    val published_at: String?,
)

@JsonClass(generateAdapter = true)
data class ReleaseAssetDto(
    val id: Long,
    val name: String,
    val size: Long,
    val browser_download_url: String,
    val content_type: String?,
)
