package com.awesometodo.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.awesometodo.app.AwesomeTodoApplication
import com.awesometodo.app.backup.BackupManager
import com.awesometodo.app.data.ActiveTimerEntity
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.TodoEntity
import com.awesometodo.app.data.TimerMode
import com.awesometodo.app.data.ThemeMode
import com.awesometodo.app.stats.Statistics
import com.awesometodo.app.stats.SummaryStats
import com.awesometodo.app.timer.FocusTimerService
import com.awesometodo.app.BuildConfig
import com.awesometodo.app.update.UpdateChecker
import com.awesometodo.app.update.UpdateResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val todos: List<TodoEntity> = emptyList(),
    val sessions: List<FocusSessionEntity> = emptyList(),
    val activeTimer: ActiveTimerEntity? = null,
    val summary: SummaryStats = SummaryStats(0, 0, 0),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AwesomeTodoApplication
    private val repository = app.repository
    private val backup = BackupManager(app, repository, app.settingsRepository)
    private val updateChecker = UpdateChecker()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult = _updateResult.asStateFlow()
    val themeMode = app.settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val uiState: StateFlow<AppUiState> = combine(repository.todos, repository.sessions, repository.activeTimer) { todos, sessions, active ->
        AppUiState(todos, sessions, active, Statistics.summary(sessions))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun saveTodo(id: String?, title: String, minutes: Int, themeId: Int, timerMode: TimerMode) = viewModelScope.launch {
        runCatching { repository.saveTodo(id, title, minutes, themeId, timerMode) }
            .onFailure { messages.emit(it.message ?: "保存失败") }
    }

    fun setCompleted(todo: TodoEntity, completed: Boolean) = viewModelScope.launch { repository.setCompleted(todo, completed) }
    fun deleteTodo(todo: TodoEntity) = viewModelScope.launch { repository.deleteTodo(todo) }
    fun deleteSession(session: FocusSessionEntity) = viewModelScope.launch { repository.deleteSession(session) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { app.settingsRepository.setThemeMode(mode) }

    fun startTimer(todo: TodoEntity) = viewModelScope.launch {
        if (todo.timerMode == TimerMode.UNTIMED) {
            repository.completeUntimed(todo)
            return@launch
        }
        repository.startTimer(todo)
        ContextCompat.startForegroundService(app, FocusTimerService.startIntent(app))
    }

    fun pauseTimer() = viewModelScope.launch { repository.pauseTimer() }
    fun resumeTimer() = viewModelScope.launch { repository.resumeTimer() }

    fun finishEarly() = viewModelScope.launch {
        repository.finishEarly()
        app.stopService(Intent(app, FocusTimerService::class.java))
    }

    fun abandon() = viewModelScope.launch {
        repository.abandon()
        app.stopService(Intent(app, FocusTimerService::class.java))
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        if (uiState.value.activeTimer != null) return@launch messages.emit("计时进行中，暂不能备份")
        runCatching { backup.exportTo(uri) }
            .onSuccess { messages.emit("备份已导出") }
            .onFailure { messages.emit(it.message ?: "导出失败") }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        if (uiState.value.activeTimer != null) return@launch messages.emit("计时进行中，暂不能恢复")
        runCatching { backup.importFrom(uri) }
            .onSuccess { messages.emit("备份已恢复") }
            .onFailure { messages.emit(it.message ?: "恢复失败") }
    }

    fun checkForUpdates() = viewModelScope.launch {
        messages.emit("正在检查更新…")
        _updateResult.value = updateChecker.check(BuildConfig.VERSION_NAME)
    }

    fun clearUpdateResult() { _updateResult.value = null }
    fun showMessage(message: String) { messages.tryEmit(message) }
}
