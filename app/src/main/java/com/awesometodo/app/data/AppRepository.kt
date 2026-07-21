package com.awesometodo.app.data

import androidx.room.withTransaction
import com.awesometodo.app.timer.TimerPolicy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class AppRepository(private val database: AppDatabase) {
    private val dao = database.appDao()
    private val timerMutex = Mutex()

    val todos = dao.observeTodos()
    val sessions = dao.observeSessions()
    val activeTimer = dao.observeActiveTimer()

    suspend fun saveTodo(existingId: String?, title: String, minutes: Int, themeId: Int) {
        require(title.isNotBlank())
        require(minutes in 1..180)
        val now = System.currentTimeMillis()
        val existing = existingId?.let { dao.getTodo(it) }
        dao.upsertTodo(
            TodoEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                plannedMinutes = minutes,
                themeId = themeId.coerceIn(0, 5),
                isCompleted = existing?.isCompleted ?: false,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                completedAt = existing?.completedAt,
            )
        )
    }

    suspend fun setCompleted(todo: TodoEntity, completed: Boolean) {
        val now = System.currentTimeMillis()
        dao.upsertTodo(todo.copy(isCompleted = completed, completedAt = if (completed) now else null, updatedAt = now))
    }

    suspend fun deleteTodo(todo: TodoEntity) = dao.deleteTodo(todo)

    suspend fun startTimer(todo: TodoEntity): ActiveTimerEntity = timerMutex.withLock {
        dao.getActiveTimer() ?: ActiveTimerEntity(
            todoId = todo.id,
            todoTitle = todo.title,
            plannedSeconds = todo.plannedMinutes * 60L,
            accumulatedFocusSeconds = 0L,
            lastResumedAt = System.currentTimeMillis(),
            startedAt = System.currentTimeMillis(),
            status = TimerStatus.RUNNING,
        ).also { dao.upsertActiveTimer(it) }
    }

    suspend fun pauseTimer(now: Long = System.currentTimeMillis()) = timerMutex.withLock {
        val active = dao.getActiveTimer() ?: return@withLock
        if (active.status == TimerStatus.RUNNING) {
            dao.upsertActiveTimer(active.copy(
                accumulatedFocusSeconds = TimerPolicy.focusedSeconds(active, now),
                lastResumedAt = null,
                status = TimerStatus.PAUSED,
            ))
        }
    }

    suspend fun resumeTimer(now: Long = System.currentTimeMillis()) = timerMutex.withLock {
        val active = dao.getActiveTimer() ?: return@withLock
        if (active.status == TimerStatus.PAUSED) {
            dao.upsertActiveTimer(active.copy(lastResumedAt = now, status = TimerStatus.RUNNING))
        }
    }

    suspend fun finishEarly(now: Long = System.currentTimeMillis()) = timerMutex.withLock {
        val active = dao.getActiveTimer() ?: return@withLock
        val actual = TimerPolicy.focusedSeconds(active, now)
        val credit = TimerPolicy.earlyCredit(actual)
        finalizeSession(active, actual, credit.outcome, credit.creditedMinutes, credit.countsTowardStats, now)
    }

    suspend fun abandon(now: Long = System.currentTimeMillis()) = timerMutex.withLock {
        val active = dao.getActiveTimer() ?: return@withLock
        finalizeSession(active, TimerPolicy.focusedSeconds(active, now), SessionOutcome.ABANDONED, 0, false, now)
    }

    suspend fun completeNaturally(now: Long = System.currentTimeMillis()): Boolean = timerMutex.withLock {
        val active = dao.getActiveTimer() ?: return@withLock false
        finalizeSession(
            active = active,
            actual = active.plannedSeconds,
            outcome = SessionOutcome.NATURAL_COMPLETION,
            creditedMinutes = (active.plannedSeconds / 60L).toInt(),
            counts = true,
            endedAt = now,
        )
        true
    }

    private suspend fun finalizeSession(
        active: ActiveTimerEntity,
        actual: Long,
        outcome: SessionOutcome,
        creditedMinutes: Int,
        counts: Boolean,
        endedAt: Long,
    ) {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(endedAt).atZone(zone).toLocalDate().toString()
        database.withTransaction {
            dao.insertSession(
                FocusSessionEntity(
                    id = UUID.randomUUID().toString(),
                    todoId = active.todoId,
                    todoTitle = active.todoTitle,
                    plannedSeconds = active.plannedSeconds,
                    actualFocusSeconds = actual,
                    creditedMinutes = creditedMinutes,
                    outcome = outcome,
                    countsTowardStats = counts,
                    startedAt = active.startedAt,
                    endedAt = endedAt,
                    endedLocalDate = date,
                    endedZoneId = zone.id,
                )
            )
            dao.clearActiveTimer()
        }
    }

    suspend fun snapshot(): Pair<List<TodoEntity>, List<FocusSessionEntity>> = dao.getTodos() to dao.getSessions()

    suspend fun replaceAll(todos: List<TodoEntity>, sessions: List<FocusSessionEntity>) {
        check(dao.getActiveTimer() == null) { "计时进行中，不能恢复备份" }
        database.withTransaction {
            dao.clearSessions()
            dao.clearTodos()
            dao.insertTodos(todos)
            dao.insertSessions(sessions)
        }
    }
}
