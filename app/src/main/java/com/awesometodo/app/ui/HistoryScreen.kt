package com.awesometodo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun HistoryScreen(sessions: List<FocusSessionEntity>, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        GradientHeader("历史记录", "专注与普通待办的每一次完成", "返回", onBack)
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("还没有历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { HistoryRow(it) }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: FocusSessionEntity) {
    val outcomeText = when (session.outcome) {
        SessionOutcome.NATURAL_COMPLETION -> "自然完成"
        SessionOutcome.EARLY_CREDITED -> "提前结束 · 已计入"
        SessionOutcome.EARLY_UNCREDITED -> "提前结束 · 未计入"
        SessionOutcome.ABANDONED -> "已放弃 · 未计入"
        SessionOutcome.UNTIMED_COMPLETION -> "不计时完成"
    }
    val modeText = when (session.timerMode) {
        TimerMode.COUNTDOWN -> "倒计时"
        TimerMode.COUNT_UP -> "正向计时"
        TimerMode.UNTIMED -> "普通待办"
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(session.todoTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    if (session.timerMode == TimerMode.UNTIMED) "完成 1 次" else "${session.actualFocusSeconds / 60} 分",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            val timeDetails = if (session.timerMode == TimerMode.UNTIMED) {
                formatDateTime(session.endedAt, session.endedZoneId)
            } else {
                "${formatDateTime(session.startedAt, session.endedZoneId)} – ${formatClock(session.endedAt, session.endedZoneId)} · 计划 ${session.plannedSeconds / 60} 分"
            }
            Text(timeDetails, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$modeText · $outcomeText",
                style = MaterialTheme.typography.labelMedium,
                color = if (session.countsTowardStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val historyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val historyClockFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun formatDateTime(epoch: Long, zoneId: String): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId)).format(historyDateFormatter)
private fun formatClock(epoch: Long, zoneId: String): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId)).format(historyClockFormatter)
