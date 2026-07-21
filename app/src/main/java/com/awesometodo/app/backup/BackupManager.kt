package com.awesometodo.app.backup

import android.content.Context
import android.net.Uri
import com.awesometodo.app.data.AppRepository
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.SettingsRepository
import com.awesometodo.app.data.ThemeMode
import com.awesometodo.app.data.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

class BackupManager(
    private val context: Context,
    private val repository: AppRepository,
    private val settings: SettingsRepository,
) {
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val (todos, sessions) = repository.snapshot()
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", JSONObject().put("themeMode", settings.themeMode.first().name))
            .put("todos", JSONArray().apply { todos.forEach { put(it.toJson()) } })
            .put("sessions", JSONArray().apply { sessions.forEach { put(it.toJson()) } })
        val output = context.contentResolver.openOutputStream(uri, "w") ?: error("无法打开备份文件")
        output.use { stream -> stream.bufferedWriter().use { writer -> writer.write(root.toString(2)) } }
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取备份文件")
        val text = input.use { stream -> stream.bufferedReader().use { reader -> reader.readText() } }
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) { "不支持的备份版本" }
        val todos = root.getJSONArray("todos").toObjects { todoFromJson(it) }
        val sessions = root.getJSONArray("sessions").toObjects { sessionFromJson(it) }
        require(todos.map { it.id }.distinct().size == todos.size) { "备份中存在重复待办" }
        require(sessions.map { it.id }.distinct().size == sessions.size) { "备份中存在重复记录" }
        val modeName = root.optJSONObject("settings")?.optString("themeMode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val mode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.SYSTEM)
        repository.replaceAll(todos, sessions)
        settings.setThemeMode(mode)
    }

    private fun TodoEntity.toJson() = JSONObject()
        .put("id", id).put("title", title).put("plannedMinutes", plannedMinutes).put("themeId", themeId)
        .put("isCompleted", isCompleted).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("completedAt", completedAt ?: JSONObject.NULL)

    private fun FocusSessionEntity.toJson() = JSONObject()
        .put("id", id).put("todoId", todoId ?: JSONObject.NULL).put("todoTitle", todoTitle)
        .put("plannedSeconds", plannedSeconds).put("actualFocusSeconds", actualFocusSeconds)
        .put("creditedMinutes", creditedMinutes).put("outcome", outcome.name)
        .put("countsTowardStats", countsTowardStats).put("startedAt", startedAt).put("endedAt", endedAt)
        .put("endedLocalDate", endedLocalDate).put("endedZoneId", endedZoneId)

    private fun todoFromJson(json: JSONObject): TodoEntity {
        val minutes = json.getInt("plannedMinutes")
        val theme = json.getInt("themeId")
        require(minutes in 1..180 && theme in 0..5 && json.getString("title").isNotBlank()) { "待办数据无效" }
        return TodoEntity(
            id = json.getString("id"), title = json.getString("title"), plannedMinutes = minutes,
            themeId = theme, isCompleted = json.getBoolean("isCompleted"), createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"), completedAt = json.nullableLong("completedAt"),
        )
    }

    private fun sessionFromJson(json: JSONObject): FocusSessionEntity {
        val planned = json.getLong("plannedSeconds")
        val actual = json.getLong("actualFocusSeconds")
        val credited = json.getInt("creditedMinutes")
        val localDate = json.getString("endedLocalDate")
        val zoneId = json.getString("endedZoneId")
        require(planned > 0 && actual in 0..planned && credited >= 0) { "专注记录数据无效" }
        LocalDate.parse(localDate)
        ZoneId.of(zoneId)
        return FocusSessionEntity(
            id = json.getString("id"), todoId = json.nullableString("todoId"), todoTitle = json.getString("todoTitle"),
            plannedSeconds = planned, actualFocusSeconds = actual, creditedMinutes = credited,
            outcome = SessionOutcome.valueOf(json.getString("outcome")), countsTowardStats = json.getBoolean("countsTowardStats"),
            startedAt = json.getLong("startedAt"), endedAt = json.getLong("endedAt"),
            endedLocalDate = localDate, endedZoneId = zoneId,
        )
    }

    private fun JSONObject.nullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)
    private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else getString(name)
    private fun <T> JSONArray.toObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    companion object { const val SCHEMA_VERSION = 1 }
}
