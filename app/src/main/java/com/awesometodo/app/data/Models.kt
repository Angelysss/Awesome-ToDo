package com.awesometodo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val plannedMinutes: Int,
    val themeId: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

enum class SessionOutcome { NATURAL_COMPLETION, EARLY_CREDITED, EARLY_UNCREDITED, ABANDONED }

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val todoId: String?,
    val todoTitle: String,
    val plannedSeconds: Long,
    val actualFocusSeconds: Long,
    val creditedMinutes: Int,
    val outcome: SessionOutcome,
    val countsTowardStats: Boolean,
    val startedAt: Long,
    val endedAt: Long,
    val endedLocalDate: String,
    val endedZoneId: String,
)

enum class TimerStatus { RUNNING, PAUSED }

@Entity(tableName = "active_timer")
data class ActiveTimerEntity(
    @PrimaryKey val singletonId: Int = 1,
    val todoId: String,
    val todoTitle: String,
    val plannedSeconds: Long,
    val accumulatedFocusSeconds: Long,
    val lastResumedAt: Long?,
    val startedAt: Long,
    val status: TimerStatus,
)

class AppTypeConverters {
    @TypeConverter fun fromOutcome(value: SessionOutcome): String = value.name
    @TypeConverter fun toOutcome(value: String): SessionOutcome = SessionOutcome.valueOf(value)
    @TypeConverter fun fromStatus(value: TimerStatus): String = value.name
    @TypeConverter fun toStatus(value: String): TimerStatus = TimerStatus.valueOf(value)
}
