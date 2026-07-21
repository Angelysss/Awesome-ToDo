package com.awesometodo.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val version: String, val title: String, val url: String, val notes: String)
sealed interface UpdateResult {
    data class Available(val release: ReleaseInfo) : UpdateResult
    data class Current(val version: String) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

class UpdateChecker {
    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Awesome-ToDo-Android")
            try {
                require(connection.responseCode in 200..299) { "GitHub 返回 ${connection.responseCode}" }
                val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                val version = json.getString("tag_name").removePrefix("v")
                val release = ReleaseInfo(
                    version = version,
                    title = json.optString("name", "Awesome ToDo v$version"),
                    url = json.getString("html_url"),
                    notes = json.optString("body", ""),
                )
                if (compareVersions(version, currentVersion) > 0) UpdateResult.Available(release)
                else UpdateResult.Current(currentVersion)
            } finally {
                connection.disconnect()
            }
        }.getOrElse { UpdateResult.Failed(it.message ?: "检查更新失败") }
    }

    companion object {
        const val API_URL = "https://api.github.com/repos/Angelysss/Awesome-ToDo/releases/latest"

        fun compareVersions(left: String, right: String): Int {
            val a = left.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val b = right.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            return (0 until maxOf(a.size, b.size)).firstNotNullOfOrNull { index ->
                val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
                comparison.takeIf { it != 0 }
            } ?: 0
        }
    }
}
