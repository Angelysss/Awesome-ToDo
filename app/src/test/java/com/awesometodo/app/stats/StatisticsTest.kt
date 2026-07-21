package com.awesometodo.app.stats

import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
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

    private fun session(id: String, date: String, minutes: Int, counts: Boolean) = FocusSessionEntity(
        id = id, todoId = null, todoTitle = "test", plannedSeconds = 1500,
        actualFocusSeconds = minutes * 60L, creditedMinutes = minutes,
        outcome = if (counts) SessionOutcome.NATURAL_COMPLETION else SessionOutcome.EARLY_UNCREDITED,
        countsTowardStats = counts, startedAt = 0, endedAt = 0, endedLocalDate = date, endedZoneId = "Asia/Shanghai",
    )
}
