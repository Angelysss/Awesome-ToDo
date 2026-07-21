package com.awesometodo.app.stats

import com.awesometodo.app.data.FocusSessionEntity
import java.time.LocalDate
import java.time.YearMonth

data class SummaryStats(val count: Int, val minutes: Int, val activeDayAverage: Int)
data class ChartPoint(val label: String, val minutes: Int)

object Statistics {
    fun summary(sessions: List<FocusSessionEntity>): SummaryStats {
        val credited = sessions.filter { it.countsTowardStats }
        val minutes = credited.sumOf { it.creditedMinutes }
        val activeDays = credited.map { it.endedLocalDate }.distinct().size
        return SummaryStats(credited.size, minutes, if (activeDays == 0) 0 else minutes / activeDays)
    }

    fun daily(sessions: List<FocusSessionEntity>, date: LocalDate): Int = sessions
        .filter { it.countsTowardStats && it.endedLocalDate == date.toString() }
        .sumOf { it.creditedMinutes }

    fun dateSeries(sessions: List<FocusSessionEntity>, start: LocalDate, end: LocalDate): List<ChartPoint> {
        val totals = sessions.filter { it.countsTowardStats }
            .groupBy { it.endedLocalDate }
            .mapValues { (_, value) -> value.sumOf { it.creditedMinutes } }
        return generateSequence(start) { it.plusDays(1).takeUnless { next -> next.isAfter(end) } }
            .map { ChartPoint("${it.monthValue}/${it.dayOfMonth}", totals[it.toString()] ?: 0) }
            .toList()
    }

    fun yearSeries(sessions: List<FocusSessionEntity>, year: Int): List<ChartPoint> {
        val credited = sessions.filter { it.countsTowardStats }
        return (1..12).map { month ->
            val prefix = YearMonth.of(year, month).toString()
            ChartPoint("${month}月", credited.filter { it.endedLocalDate.startsWith(prefix) }.sumOf { it.creditedMinutes })
        }
    }
}
