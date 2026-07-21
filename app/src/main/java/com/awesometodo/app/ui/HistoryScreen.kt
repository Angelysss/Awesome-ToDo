package com.awesometodo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun HistoryScreen(
    sessions: List<FocusSessionEntity>,
    onBack: () -> Unit,
    onDelete: (FocusSessionEntity) -> Unit,
) {
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()
    ) {
        GradientHeader("历史记录", "专注与普通待办的每一次完成")
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("还没有历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    HistoryRow(
                        session = session,
                        confirmingDelete = confirmDeleteId == session.id,
                        onDeleteClick = {
                            if (confirmDeleteId == session.id) {
                                confirmDeleteId = null
                                onDelete(session)
                            } else {
                                confirmDeleteId = session.id
                            }
                        },
                        onClosed = {
                            if (confirmDeleteId == session.id) confirmDeleteId = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    session: FocusSessionEntity,
    confirmingDelete: Boolean,
    onDeleteClick: () -> Unit,
    onClosed: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val targetActionWidth = if (confirmingDelete) 108.dp else 72.dp
    val actionWidth by animateDpAsState(targetActionWidth, label = "history-delete-width")
    val offset = remember(session.id) { Animatable(0f) }
    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val maxReveal = with(density) { targetActionWidth.toPx() }
            offset.snapTo((offset.value + delta).coerceIn(-maxReveal, 0f))
        }
    }
    LaunchedEffect(confirmingDelete) {
        if (confirmingDelete && offset.value < 0f) {
            offset.animateTo(-with(density) { targetActionWidth.toPx() })
        }
    }
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
    Box(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)),
    ) {
        if (offset.value < 0f) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).width(actionWidth).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error).clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (confirmingDelete) "确认删除" else "删除",
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Card(
            modifier = Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .clickable(enabled = offset.value < 0f) {
                    scope.launch {
                        offset.animateTo(0f)
                        onClosed()
                    }
                }
                .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    val reveal = with(density) { targetActionWidth.toPx() }
                    val target = if (offset.value <= -reveal * .35f) -reveal else 0f
                    offset.animateTo(target)
                    if (target == 0f) onClosed()
                },
            ),
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
}

private val historyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val historyClockFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun formatDateTime(epoch: Long, zoneId: String): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId)).format(historyDateFormatter)
private fun formatClock(epoch: Long, zoneId: String): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId)).format(historyClockFormatter)
