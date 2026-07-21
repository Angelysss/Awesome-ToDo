package com.awesometodo.app.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.stats.ChartPoint
import com.awesometodo.app.stats.PieSlice
import com.awesometodo.app.stats.Statistics
import com.awesometodo.app.stats.SummaryStats
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

private enum class RangeMode(val label: String) { DAY("日"), WEEK("周"), MONTH("月"), CUSTOM("自定义") }

private val pieColors = listOf(
    Color(0xFF147D9E), Color(0xFF6D5DA8), Color(0xFF1F9D8A), Color(0xFFE07A5F),
    Color(0xFF4A6FA5), Color(0xFFE9A23B), Color(0xFFB85C76), Color(0xFF769F6D),
)

@Composable
internal fun StatsScreen(
    sessions: List<FocusSessionEntity>,
    summary: SummaryStats,
    padding: PaddingValues,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var rangeMode by remember { mutableStateOf(RangeMode.DAY) }
    var anchor by remember { mutableStateOf(today) }
    var customStart by remember { mutableStateOf(today.minusDays(29)) }
    var customEnd by remember { mutableStateOf(today) }
    var monthAnchor by remember { mutableStateOf(YearMonth.from(today)) }
    var year by remember { mutableIntStateOf(today.year) }

    val (start, end) = rangeBounds(rangeMode, anchor, customStart, customEnd)
    val pie = Statistics.pieByTodo(sessions, start, end)
    val todaySessions = sessions.filter { it.countsTowardStats && it.endedLocalDate == today.toString() }
    val monthlyPoints = Statistics.dateSeries(sessions, monthAnchor.atDay(1), monthAnchor.atEndOfMonth())
    val periodLabel = when (rangeMode) {
        RangeMode.DAY -> start.toString()
        RangeMode.WEEK, RangeMode.CUSTOM -> "$start 至 $end"
        RangeMode.MONTH -> YearMonth.from(anchor).toString()
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        GradientHeader("统计", "看见每一段投入带来的积累")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard("累计专注") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Metric("次数", summary.count.toString())
                        Metric("时长", "${summary.minutes} 分")
                        Metric("活跃日均", "${summary.activeDayAverage} 分")
                    }
                }
            }
            item {
                SectionCard("今日专注 · $today") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Metric("次数", todaySessions.size.toString())
                        Metric("时长", "${todaySessions.sumOf { it.creditedMinutes }} 分")
                    }
                }
            }
            item {
                SectionCard("专注时长分布") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RangeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = rangeMode == mode,
                                onClick = { rangeMode = mode },
                                label = { Text(mode.label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (rangeMode == RangeMode.CUSTOM) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { pickDate(context, customStart) { customStart = it.coerceAtMost(customEnd) } },
                                modifier = Modifier.weight(1f),
                            ) { Text("起 $customStart") }
                            OutlinedButton(
                                onClick = { pickDate(context, customEnd) { customEnd = it.coerceAtLeast(customStart).coerceAtMost(today) } },
                                modifier = Modifier.weight(1f),
                            ) { Text("止 $customEnd") }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { anchor = shift(anchor, rangeMode, -1) }) { Text("‹ 上一${rangeMode.label}") }
                            Text(periodLabel, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            TextButton(
                                onClick = { anchor = shift(anchor, rangeMode, 1) },
                                enabled = !shift(anchor, rangeMode, 1).isAfter(today),
                            ) { Text("下一${rangeMode.label} ›") }
                        }
                    }
                    if (rangeMode == RangeMode.CUSTOM) {
                        Text(periodLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FocusPieChart(pie)
                    Button(onClick = onHistory, modifier = Modifier.fillMaxWidth().height(42.dp)) { Text("历史记录") }
                }
            }
            item {
                SectionCard("月度专注统计 · ${monthAnchor.year}年${monthAnchor.monthValue}月") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { monthAnchor = monthAnchor.minusMonths(1) }) { Text("‹ 上一月") }
                        TextButton(
                            onClick = { monthAnchor = monthAnchor.plusMonths(1) },
                            enabled = monthAnchor < YearMonth.from(today),
                        ) { Text("下一月 ›") }
                    }
                    FocusBarChart(monthlyPoints, "本月还没有有效专注")
                }
            }
            item {
                SectionCard("年度专注统计 · $year 年") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { year-- }) { Text("‹ 上一年") }
                        TextButton(onClick = { year++ }, enabled = year < today.year) { Text("下一年 ›") }
                    }
                    FocusBarChart(Statistics.yearSeries(sessions, year), "这一年还没有有效专注")
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusPieChart(slices: List<PieSlice>) {
    val total = slices.sumOf { it.minutes }
    val centerColor = MaterialTheme.colorScheme.surface
    if (total == 0) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("这个时段还没有有效专注", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = slice.minutes.toFloat() / total * 360f
                drawArc(pieColors[index % pieColors.size], startAngle, sweep, true)
                startAngle += sweep
            }
            drawCircle(color = centerColor, radius = size.minDimension * .22f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("总计", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$total 分", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEachIndexed { index, slice ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(pieColors[index % pieColors.size], CircleShape))
                Spacer(Modifier.size(8.dp))
                Text(slice.title, modifier = Modifier.weight(1f), maxLines = 1)
                Text("${slice.minutes} 分 · ${(slice.minutes * 100f / total).roundToInt()}%", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FocusBarChart(points: List<ChartPoint>, emptyText: String) {
    val max = points.maxOfOrNull { it.minutes } ?: 0
    if (max == 0) {
        Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Canvas(Modifier.fillMaxWidth().height(126.dp).padding(vertical = 8.dp)) {
        val count = points.size.coerceAtLeast(1)
        val gap = if (count > 20) 2.dp.toPx() else 5.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        points.forEachIndexed { index, point ->
            val height = size.height * point.minutes / max.toFloat()
            drawRoundRect(
                color = Color(0xFF26A6C6),
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(points.firstOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall)
        Text("最高 $max 分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(points.lastOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall)
    }
}

private fun rangeBounds(
    mode: RangeMode,
    anchor: LocalDate,
    customStart: LocalDate,
    customEnd: LocalDate,
): Pair<LocalDate, LocalDate> = when (mode) {
    RangeMode.DAY -> anchor to anchor
    RangeMode.WEEK -> anchor.minusDays((anchor.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()).let { it to it.plusDays(6) }
    RangeMode.MONTH -> YearMonth.from(anchor).let { it.atDay(1) to it.atEndOfMonth() }
    RangeMode.CUSTOM -> customStart to customEnd
}

private fun shift(date: LocalDate, mode: RangeMode, amount: Long): LocalDate = when (mode) {
    RangeMode.DAY -> date.plusDays(amount)
    RangeMode.WEEK -> date.plusWeeks(amount)
    RangeMode.MONTH -> date.plusMonths(amount)
    RangeMode.CUSTOM -> date
}

private fun pickDate(context: Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

internal fun hourlySeries(sessions: List<FocusSessionEntity>, date: LocalDate): List<ChartPoint> {
    val totals = sessions.filter { it.countsTowardStats && it.endedLocalDate == date.toString() }
        .groupBy { Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.of(it.endedZoneId)).hour }
        .mapValues { (_, values) -> values.sumOf { it.creditedMinutes } }
    return (0..23).map { ChartPoint("${it}时", totals[it] ?: 0) }
}
