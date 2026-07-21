package com.awesometodo.app.timer

import com.awesometodo.app.data.ActiveTimerEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerStatus

data class SessionCredit(val outcome: SessionOutcome, val creditedMinutes: Int, val countsTowardStats: Boolean)

object TimerPolicy {
    const val EARLY_CREDIT_THRESHOLD_SECONDS = 10 * 60L

    fun focusedSeconds(timer: ActiveTimerEntity, nowMillis: Long): Long {
        val currentSegment = if (timer.status == TimerStatus.RUNNING && timer.lastResumedAt != null) {
            ((nowMillis - timer.lastResumedAt).coerceAtLeast(0L) / 1000L)
        } else 0L
        return (timer.accumulatedFocusSeconds + currentSegment).coerceIn(0L, timer.plannedSeconds)
    }

    fun remainingSeconds(timer: ActiveTimerEntity, nowMillis: Long): Long =
        (timer.plannedSeconds - focusedSeconds(timer, nowMillis)).coerceAtLeast(0L)

    fun earlyCredit(actualSeconds: Long): SessionCredit = if (actualSeconds > EARLY_CREDIT_THRESHOLD_SECONDS) {
        SessionCredit(SessionOutcome.EARLY_CREDITED, (actualSeconds / 60L).toInt(), true)
    } else {
        SessionCredit(SessionOutcome.EARLY_UNCREDITED, 0, false)
    }
}
