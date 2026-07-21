package com.awesometodo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerMode
import com.awesometodo.app.data.TodoEntity
import java.time.LocalDate

@Composable
internal fun TodoScreen(
    todos: List<TodoEntity>,
    sessions: List<FocusSessionEntity>,
    padding: PaddingValues,
    onStart: (TodoEntity) -> Unit,
    onEdit: (TodoEntity) -> Unit,
    onCompleted: (TodoEntity) -> Unit,
    onDelete: (TodoEntity) -> Unit,
) {
    var selected by remember { mutableStateOf<TodoEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<TodoEntity?>(null) }
    val sorted = todos.sortedWith(compareBy<TodoEntity> { it.isCompleted }.thenByDescending { it.updatedAt })
    val today = LocalDate.now().toString()

    Column(Modifier.fillMaxSize().padding(padding)) {
        GradientHeader("待办", "把注意力留给真正重要的事")
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("点击右下角 ＋ 添加一个吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 76.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorted, key = { it.id }) { todo ->
                    val count = sessions.count { session ->
                        session.todoId == todo.id && session.endedLocalDate == today &&
                            if (todo.timerMode == TimerMode.UNTIMED) session.outcome == SessionOutcome.UNTIMED_COMPLETION
                            else session.countsTowardStats
                    }
                    TodoCard(todo, count, onStart, onMenu = { selected = todo })
                }
            }
        }
    }

    selected?.let { todo ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(todo.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEdit(todo); selected = null }, Modifier.fillMaxWidth()) { Text("编辑") }
                    if (!todo.isCompleted) OutlinedButton(onClick = { onCompleted(todo); selected = null }, Modifier.fillMaxWidth()) { Text("完成") }
                    OutlinedButton(onClick = { deleteCandidate = todo; selected = null }, Modifier.fillMaxWidth()) { Text("删除") }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } },
        )
    }

    deleteCandidate?.let { todo ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null }, title = { Text("删除待办？") },
            text = { Text("待办会被删除，历史记录仍会保留。删除后如需再次使用，必须重新添加。") },
            confirmButton = { TextButton(onClick = { onDelete(todo); deleteCandidate = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun TodoCard(todo: TodoEntity, todayCount: Int, onStart: (TodoEntity) -> Unit, onMenu: () -> Unit) {
    val colors = cardThemes[todo.themeId.coerceIn(0, cardThemes.lastIndex)]
    Box(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(colors)).clickable(onClick = onMenu).padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 3.dp, bottom = 1.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    todo.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                )
                Text(
                    modeLabel(todo),
                    color = Color.White.copy(alpha = .88f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier.width(108.dp).fillMaxHeight(),
            ) {
                Button(
                    onClick = { onStart(todo) },
                    modifier = Modifier.height(30.dp).align(Alignment.CenterEnd),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = colors.first()),
                ) { Text("开始", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                Text(
                    if (todo.timerMode == TimerMode.UNTIMED) "今日完成 ${todayCount} 次" else "今日专注 ${todayCount} 次",
                    color = Color.White.copy(alpha = .88f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

private fun modeLabel(todo: TodoEntity) = when (todo.timerMode) {
    TimerMode.COUNTDOWN -> "${todo.plannedMinutes} 分钟"
    TimerMode.COUNT_UP -> "正向计时"
    TimerMode.UNTIMED -> "不计时"
}

@Composable
internal fun TodoEditorDialog(
    todo: TodoEntity?,
    onDismiss: () -> Unit,
    onSave: (String?, String, Int, Int, TimerMode) -> Unit,
) {
    var title by remember(todo?.id) { mutableStateOf(todo?.title.orEmpty()) }
    var minutesText by remember(todo?.id) { mutableStateOf((todo?.plannedMinutes?.takeIf { it > 0 } ?: 25).toString()) }
    var theme by remember(todo?.id) { mutableIntStateOf(todo?.themeId ?: 0) }
    var mode by remember(todo?.id) { mutableStateOf(todo?.timerMode ?: TimerMode.COUNTDOWN) }
    val minutes = minutesText.toIntOrNull()
    val valid = title.isNotBlank() && (mode != TimerMode.COUNTDOWN || minutes != null && minutes in 1..180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo == null) "添加待办" else "编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("待办名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(TimerMode.COUNTDOWN to "倒计时", TimerMode.COUNT_UP to "正向计时", TimerMode.UNTIMED to "不计时").forEach { (value, label) ->
                        FilterChip(mode == value, { mode = value }, { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }
                if (mode == TimerMode.COUNTDOWN) {
                    OutlinedTextField(
                        minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, label = { Text("专注分钟（1–180）") },
                        isError = minutes == null || minutes !in 1..180, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    )
                    listOf(listOf(5, 10, 15), listOf(25, 45, 60)).forEach { values ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            values.forEach { value ->
                                FilterChip(minutes == value, { minutesText = value.toString() }, { Text("$value 分") }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Text(
                        if (mode == TimerMode.COUNT_UP) "从 00:00 开始计时，手动结束后按专注规则记录。"
                        else "点击开始即记录一次普通待办完成，不进入专注统计。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("卡片主题", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    cardThemes.forEachIndexed { index, colors ->
                        Box(
                            Modifier.size(if (theme == index) 34.dp else 28.dp).clip(CircleShape)
                                .background(Brush.linearGradient(colors)).clickable { theme = index }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(todo?.id, title, minutes ?: 0, theme, mode) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
