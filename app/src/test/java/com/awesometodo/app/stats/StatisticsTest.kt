package com.awesometodo.app.stats

import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatisticsTest {
    @Test fun summaryExcludesUncreditedAndUsesActiveDays() {
        val sessions = listOf(
            session("a", "2026-07-20", 25, true),
            session("b", "2026-07-20", 15, true),
            session("c", "2026-07-21", 20, true),
            session("d", "2026-07-21", 0, false),
        )
        assertEquals(SummaryStats(count = 3, minutes = 60, activeDayAverage = 30), Statistics.summary(sessions))
    }

    @Test fun dateSeriesFillsEmptyDays() {
        val points = Statistics.dateSeries(
            listOf(session("a", "2026-07-20", 25, true)),
            LocalDate.parse("2026-07-19"), LocalDate.parse("2026-07-21"),
        )
        assertEquals(listOf(0, 25, 0), points.map { it.minutes })
    }

    @Test fun yearlySeriesHasTwelveMonths() {
        val points = Statistics.yearSeries(listOf(session("a", "2026-07-20", 25, true)), 2026)
        assertEquals(12, points.size)
        assertEquals(25, points[6].minutes)
    }

    @Test fun untimedCompletionIsHistoryOnlyAndExcludedFromSummaryAndPie() {
        val sessions = listOf(
            session("focus", "2026-07-21", 25, true, title = "阅读", todoId = "reading"),
            session(
                "ordinary", "2026-07-21", 0, false,
                title = "喝水", todoId = "water", mode = TimerMode.UNTIMED,
                outcome = SessionOutcome.UNTIMED_COMPLETION,
            ),
        )

        assertEquals(SummaryStats(1, 25, 25), Statistics.summary(sessions))
        assertEquals(
            listOf(PieSlice("reading:阅读", "阅读", 25)),
            Statistics.pieByTodo(sessions, LocalDate.parse("2026-07-21"), LocalDate.parse("2026-07-21")),
        )
        assertEquals(2, sessions.size)
    }

    @Test fun pieGroupsRepeatedSessionsByTodo() {
        val sessions = listOf(
            session("a", "2026-07-20", 10, true, title = "写作", todoId = "write"),
            session("b", "2026-07-21", 15, true, title = "写作", todoId = "write"),
            session("c", "2026-07-21", 20, true, title = "阅读", todoId = "read"),
        )
        val slices = Statistics.pieByTodo(sessions, LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-21"))
        assertEquals(listOf(25, 20), slices.map { it.minutes })
        assertEquals(listOf("写作", "阅读"), slices.map { it.title })
    }

    private fun session(
        id: String,
        date: String,
        minutes: Int,
        counts: Boolean,
        title: String = "test",
        todoId: String? = null,
        mode: TimerMode = TimerMode.COUNTDOWN,
        outcome: SessionOutcome = if (counts) SessionOutcome.NATURAL_COMPLETION else SessionOutcome.EARLY_UNCREDITED,
    ) = FocusSessionEntity(
        id = id, todoId = todoId, todoTitle = title, plannedSeconds = if (mode == TimerMode.COUNTDOWN) 1500 else 0,
        actualFocusSeconds = minutes * 60L, creditedMinutes = minutes,
        outcome = outcome,
        countsTowardStats = counts, startedAt = 0, endedAt = 0, endedLocalDate = date, endedZoneId = "Asia/Shanghai",
        timerMode = mode,
    )
}
