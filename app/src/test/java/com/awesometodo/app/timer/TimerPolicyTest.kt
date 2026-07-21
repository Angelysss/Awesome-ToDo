package com.awesometodo.app.timer

import com.awesometodo.app.data.ActiveTimerEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerPolicyTest {
    @Test fun exactlyTenMinutesIsNotCreditedWhenEndedEarly() {
        val credit = TimerPolicy.earlyCredit(600)
        assertEquals(SessionOutcome.EARLY_UNCREDITED, credit.outcome)
        assertEquals(0, credit.creditedMinutes)
        assertFalse(credit.countsTowardStats)
    }

    @Test fun moreThanTenMinutesCreditsOnlyWholeMinutes() {
        val credit = TimerPolicy.earlyCredit(661)
        assertEquals(SessionOutcome.EARLY_CREDITED, credit.outcome)
        assertEquals(11, credit.creditedMinutes)
        assertTrue(credit.countsTowardStats)
    }

    @Test fun pausedTimeDoesNotAdvance() {
        val paused = timer(status = TimerStatus.PAUSED, accumulated = 125, resumedAt = null)
        assertEquals(125, TimerPolicy.focusedSeconds(paused, 50_000))
    }

    @Test fun runningTimeUsesAnchorAndCapsAtPlan() {
        val running = timer(status = TimerStatus.RUNNING, accumulated = 30, resumedAt = 1_000)
        assertEquals(150, TimerPolicy.focusedSeconds(running, 121_000))
        assertEquals(0, TimerPolicy.remainingSeconds(running, 999_999))
    }

    private fun timer(status: TimerStatus, accumulated: Long, resumedAt: Long?) = ActiveTimerEntity(
        todoId = "todo", todoTitle = "test", plannedSeconds = 300,
        accumulatedFocusSeconds = accumulated, lastResumedAt = resumedAt, startedAt = 0, status = status,
    )
}
