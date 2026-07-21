package com.awesometodo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awesometodo.app.data.ActiveTimerEntity
import com.awesometodo.app.data.TimerMode
import com.awesometodo.app.data.TimerStatus
import com.awesometodo.app.timer.TimerPolicy
import kotlinx.coroutines.delay

@Composable
internal fun TimerScreen(active: ActiveTimerEntity, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit, onAbandon: () -> Unit) {
    var now by remember(active.lastResumedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    var showEndMenu by remember { mutableStateOf(false) }
    LaunchedEffect(active) { while (true) { now = System.currentTimeMillis(); delay(250) } }
    BackHandler { showEndMenu = true }

    val actual = TimerPolicy.focusedSeconds(active, now)
    val display = if (active.timerMode == TimerMode.COUNT_UP) actual else TimerPolicy.remainingSeconds(active, now)
    val progress = 1f - display.toFloat() / active.plannedSeconds.coerceAtLeast(1).toFloat()
    val creditHint = if (actual <= 600) "现在结束将保留记录，但不计入专注统计" else "现在结束将计入 ${actual / 60} 分钟"
    val deepRingColor = Color(0xFF147D9E)
    val lightProgressColor = Color(0xFF8FD9EE)
    val progressColor = MaterialTheme.colorScheme.primary

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (active.timerMode == TimerMode.COUNT_UP) "正向计时" else "正在专注", color = progressColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(active.todoTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(.5f))
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(deepRingColor, style = Stroke(18.dp.toPx()))
                    if (active.timerMode == TimerMode.COUNTDOWN) {
                        drawArc(
                            lightProgressColor,
                            -90f,
                            360f * progress.coerceIn(0f, 1f),
                            false,
                            style = Stroke(18.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatTimer(display), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (active.status == TimerStatus.PAUSED) "已暂停"
                        else if (active.timerMode == TimerMode.COUNT_UP) "已专注" else "保持专注",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(creditHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(.5f))
            Button(
                onClick = if (active.status == TimerStatus.PAUSED) onResume else onPause,
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) { Text(if (active.status == TimerStatus.PAUSED) "继续" else "暂停") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { showEndMenu = true }, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("结束专注") }
        }
    }

    if (showEndMenu) {
        AlertDialog(
            onDismissRequest = { showEndMenu = false },
            title = { Text("结束本次专注") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(creditHint)
                    Button(onClick = { onFinish(); showEndMenu = false }, modifier = Modifier.fillMaxWidth()) { Text("结束并保存") }
                    OutlinedButton(onClick = { onAbandon(); showEndMenu = false }, modifier = Modifier.fillMaxWidth()) { Text("放弃本次专注") }
                }
            },
            confirmButton = { TextButton(onClick = { showEndMenu = false }) { Text("继续专注") } },
        )
    }
}

private fun formatTimer(seconds: Long): String = if (seconds >= 3600) {
    "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
} else "%02d:%02d".format(seconds / 60, seconds % 60)
