package com.example.nextclass.data.remote

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ReleaseAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
)

@Serializable
data class ReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<ReleaseAssetDto> = emptyList(),
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val build: Int) : UpdateState
    data class Available(val build: Int, val url: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Looks at the latest GitHub release and compares its build number with the installed one. */
class UpdateChecker(context: Context) {

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .dns(ResilientDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun currentBuild(): Int = try {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info).toInt()
    } catch (e: Exception) {
        0
    }

    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "NextClass")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@use UpdateState.Failed("No release has been published yet.")
                }
                if (!response.isSuccessful) {
                    return@use UpdateState.Failed("GitHub answered with error ${response.code}. Try again later.")
                }
                val body = response.body?.string().orEmpty()
                val release = json.decodeFromString(ReleaseDto.serializer(), body)
                val latest = release.tagName.removePrefix("build-").toIntOrNull()
                    ?: return@use UpdateState.Failed("Could not read the latest version.")
                val current = currentBuild()
                if (latest > current) {
                    val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    UpdateState.Available(latest, apk?.downloadUrl ?: release.htmlUrl)
                } else {
                    UpdateState.UpToDate(current)
                }
            }
        } catch (e: IOException) {
            UpdateState.Failed("Could not reach GitHub. Check your connection and try again.")
        } catch (e: SerializationException) {
            UpdateState.Failed("Could not read the latest version.")
        }
    }

    private companion object {
        const val REPO = "amazingasad/Next_Class"
    }
}
