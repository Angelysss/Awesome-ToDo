package com.awesometodo.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awesometodo.app.data.ActiveTimerEntity
import com.awesometodo.app.data.FocusSessionEntity
import com.awesometodo.app.data.SessionOutcome
import com.awesometodo.app.data.TimerStatus
import com.awesometodo.app.data.TodoEntity
import com.awesometodo.app.stats.ChartPoint
import com.awesometodo.app.stats.Statistics
import com.awesometodo.app.timer.TimerPolicy
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val cardThemes = listOf(
    listOf(Color(0xFF087EA4), Color(0xFF5BC0BE)),
    listOf(Color(0xFF6D5DA8), Color(0xFFB39DDB)),
    listOf(Color(0xFF167D7F), Color(0xFF98D7C2)),
    listOf(Color(0xFFB85C76), Color(0xFFF0A6CA)),
    listOf(Color(0xFF4A6FA5), Color(0xFF89C2D9)),
    listOf(Color(0xFF9B5D2E), Color(0xFFE9A86B)),
)

@Composable
fun AwesomeTodoApp(vm: AppViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<TodoEntity?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.activeTimer?.singletonId) {
        if (state.activeTimer != null) {
            ContextCompat.startForegroundService(context, com.awesometodo.app.timer.FocusTimerService.startIntent(context))
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingStart?.let(vm::startTimer)
        pendingStart = null
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    fun start(todo: TodoEntity) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = todo
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else vm.startTimer(todo)
    }

    if (state.activeTimer != null) {
        TimerScreen(state.activeTimer!!, vm::pauseTimer, vm::resumeTimer, vm::finishEarly, vm::abandon)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("待", fontWeight = FontWeight.Bold) }, label = { Text("待办") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("统", fontWeight = FontWeight.Bold) }, label = { Text("统计") })
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) FloatingActionButton(onClick = { showCreate = true }) { Text("＋", fontSize = 26.sp) }
            },
        ) { padding ->
            if (selectedTab == 0) {
                TodoScreen(state.todos, padding, ::start, { editingTodo = it }, vm::setCompleted, vm::deleteTodo)
            } else {
                StatsScreen(
                    sessions = state.sessions,
                    summary = state.summary,
                    padding = padding,
                    backupEnabled = state.activeTimer == null,
                    onExport = { exportLauncher.launch("awesome-todo-backup-${timestampForFile()}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                )
            }
        }
    }

    if (showCreate || editingTodo != null) {
        TodoEditorDialog(
            todo = editingTodo,
            onDismiss = { showCreate = false; editingTodo = null },
            onSave = { id, title, minutes, theme ->
                vm.saveTodo(id, title, minutes, theme)
                showCreate = false
                editingTodo = null
            },
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("恢复备份？") },
            text = { Text("当前待办、专注记录和设置将被备份内容整体替换。此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { vm.importBackup(uri); pendingImport = null }) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun TodoScreen(
    todos: List<TodoEntity>,
    padding: PaddingValues,
    onStart: (TodoEntity) -> Unit,
    onEdit: (TodoEntity) -> Unit,
    onCompleted: (TodoEntity, Boolean) -> Unit,
    onDelete: (TodoEntity) -> Unit,
) {
    var completed by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<TodoEntity?>(null) }
    val visible = todos.filter { it.isCompleted == completed }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF147D9E), Color(0xFF56B4D3)))).statusBarsPadding().padding(20.dp)
        ) {
            Text("待办", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("把注意力留给真正重要的事", color = Color.White.copy(alpha = .82f))
        }
        TabRow(selectedTabIndex = if (completed) 1 else 0) {
            Tab(selected = !completed, onClick = { completed = false }, text = { Text("进行中") })
            Tab(selected = completed, onClick = { completed = true }, text = { Text("已完成") })
        }
        if (visible.isEmpty()) {
            EmptyState(if (completed) "还没有已完成的待办" else "点击右下角 ＋ 创建第一项待办")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible, key = { it.id }) { todo ->
                    TodoCard(todo, onStart, onEdit, onCompleted, { deleteCandidate = it })
                }
            }
        }
    }
    deleteCandidate?.let { todo ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("删除待办？") },
            text = { Text("待办会被删除，但过去的专注记录仍会保留。") },
            confirmButton = { TextButton(onClick = { onDelete(todo); deleteCandidate = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun TodoCard(
    todo: TodoEntity,
    onStart: (TodoEntity) -> Unit,
    onEdit: (TodoEntity) -> Unit,
    onCompleted: (TodoEntity, Boolean) -> Unit,
    onDelete: (TodoEntity) -> Unit,
) {
    val colors = cardThemes[todo.themeId.coerceIn(0, 5)]
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(colors)).padding(18.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(todo.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("${todo.plannedMinutes} 分钟", color = Color.White.copy(alpha = .88f))
                }
                if (!todo.isCompleted) Button(
                    onClick = { onStart(todo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = colors.first()),
                ) { Text("开始") }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onEdit(todo) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("编辑") }
                TextButton(onClick = { onCompleted(todo, !todo.isCompleted) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text(if (todo.isCompleted) "恢复" else "完成")
                }
                TextButton(onClick = { onDelete(todo) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("删除") }
            }
        }
    }
}

@Composable
private fun TodoEditorDialog(todo: TodoEntity?, onDismiss: () -> Unit, onSave: (String?, String, Int, Int) -> Unit) {
    var title by remember(todo?.id) { mutableStateOf(todo?.title.orEmpty()) }
    var minutesText by remember(todo?.id) { mutableStateOf((todo?.plannedMinutes ?: 25).toString()) }
    var theme by remember(todo?.id) { mutableIntStateOf(todo?.themeId ?: 0) }
    val minutes = minutesText.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo == null) "新建待办" else "编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("待办标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, label = { Text("专注分钟（1–180）") },
                    singleLine = true, isError = minutes == null || minutes !in 1..180,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                listOf(listOf(5, 10, 15), listOf(25, 45, 60)).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { value ->
                            FilterChip(selected = minutes == value, onClick = { minutesText = value.toString() }, label = { Text("$value 分") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Text("卡片主题", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    cardThemes.forEachIndexed { index, colors ->
                        Box(
                            Modifier.size(if (theme == index) 42.dp else 34.dp).clip(CircleShape)
                                .background(Brush.linearGradient(colors)).clickable { theme = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && minutes != null && minutes in 1..180, onClick = { onSave(todo?.id, title, minutes!!, theme) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TimerScreen(
    active: ActiveTimerEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
) {
    var now by remember(active.singletonId, active.lastResumedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    LaunchedEffect(active) { while (true) { now = System.currentTimeMillis(); delay(250) } }
    BackHandler { confirmAbandon = true }
    val remaining = TimerPolicy.remainingSeconds(active, now)
    val progress = 1f - remaining.toFloat() / active.plannedSeconds.toFloat()
    val actual = TimerPolicy.focusedSeconds(active, now)
    val creditHint = if (actual <= 600) "提前结束将不计入统计" else "提前结束将计入 ${actual / 60} 分钟"

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("正在专注", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Text(active.todoTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(.5f))
            Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val progressColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(trackColor, style = Stroke(18.dp.toPx()))
                    drawArc(progressColor, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatTimer(remaining), fontSize = 54.sp, fontWeight = FontWeight.Bold)
                    Text(if (active.status == TimerStatus.PAUSED) "已暂停" else "保持专注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(creditHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(.5f))
            Button(onClick = if (active.status == TimerStatus.PAUSED) onResume else onPause, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (active.status == TimerStatus.PAUSED) "继续" else "暂停")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { confirmFinish = true }, modifier = Modifier.weight(1f)) { Text("提前结束") }
                TextButton(onClick = { confirmAbandon = true }, modifier = Modifier.weight(1f)) { Text("放弃") }
            }
        }
    }
    if (confirmFinish) ConfirmDialog("提前结束专注？", creditHint, "结束并保存", { confirmFinish = false }, { onFinish(); confirmFinish = false })
    if (confirmAbandon) ConfirmDialog("放弃本次专注？", "本次时长不会计入统计，但会保留一条放弃记录。", "确认放弃", { confirmAbandon = false }, { onAbandon(); confirmAbandon = false })
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, dismiss: () -> Unit, action: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = action) { Text(confirm) } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

private enum class RangeMode(val label: String) { DAY("日"), WEEK("周"), MONTH("月"), CUSTOM("自定义") }

@Composable
private fun StatsScreen(
    sessions: List<FocusSessionEntity>,
    summary: com.awesometodo.app.stats.SummaryStats,
    padding: PaddingValues,
    backupEnabled: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(RangeMode.DAY) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var customStart by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val todayValid = sessions.filter { it.countsTowardStats && it.endedLocalDate == LocalDate.now().toString() }
    val points = when (mode) {
        RangeMode.DAY -> hourlySeries(sessions, anchor)
        RangeMode.WEEK -> {
            val start = anchor.minusDays((anchor.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
            Statistics.dateSeries(sessions, start, start.plusDays(6))
        }
        RangeMode.MONTH -> Statistics.dateSeries(sessions, YearMonth.from(anchor).atDay(1), YearMonth.from(anchor).atEndOfMonth())
        RangeMode.CUSTOM -> Statistics.dateSeries(sessions, customStart, customEnd)
    }
    val periodLabel = when (mode) {
        RangeMode.DAY -> anchor.toString()
        RangeMode.WEEK -> "${points.firstOrNull()?.label ?: ""} – ${points.lastOrNull()?.label ?: ""}"
        RangeMode.MONTH -> YearMonth.from(anchor).toString()
        RangeMode.CUSTOM -> "$customStart – $customEnd"
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("统计数据", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("每一次专注，都在积累更好的自己", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            StatCard("累计专注") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metric("次数", summary.count.toString())
                    Metric("时长", "${summary.minutes} 分")
                    Metric("活跃日均", "${summary.activeDayAverage} 分")
                }
            }
        }
        item {
            StatCard("今日专注  ${LocalDate.now()}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Metric("次数", todayValid.size.toString())
                    Metric("时长", "${todayValid.sumOf { it.creditedMinutes }} 分")
                }
            }
        }
        item {
            StatCard("专注时长分布  $periodLabel") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RangeMode.entries.forEach { item ->
                        FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item.label) }, modifier = Modifier.weight(1f))
                    }
                }
                if (mode == RangeMode.CUSTOM) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickDate(context, customStart) { customStart = it.coerceAtMost(customEnd) } }, modifier = Modifier.weight(1f)) { Text("起 $customStart") }
                        OutlinedButton(onClick = { pickDate(context, customEnd) { customEnd = it.coerceAtLeast(customStart) } }, modifier = Modifier.weight(1f)) { Text("止 $customEnd") }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { anchor = shift(anchor, mode, -1) }) { Text("‹ 上一${mode.label}") }
                        TextButton(onClick = { anchor = shift(anchor, mode, 1) }, enabled = shift(anchor, mode, 1) <= LocalDate.now()) { Text("下一${mode.label} ›") }
                    }
                }
                FocusBarChart(points)
            }
        }
        item {
            StatCard("年度专注统计  $year 年") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { year-- }) { Text("‹ 上一年") }
                    TextButton(onClick = { year++ }, enabled = year < LocalDate.now().year) { Text("下一年 ›") }
                }
                FocusBarChart(Statistics.yearSeries(sessions, year))
            }
        }
        item {
            StatCard("数据备份") {
                Text("备份包含待办、专注记录和设置；恢复时会整体替换当前数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onExport, enabled = backupEnabled, modifier = Modifier.weight(1f)) { Text("导出备份") }
                    OutlinedButton(onClick = onImport, enabled = backupEnabled, modifier = Modifier.weight(1f)) { Text("恢复备份") }
                }
            }
        }
        item { Text("专注记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (sessions.isEmpty()) item { Text("暂无记录，完成一次专注后会显示在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(sessions, key = { it.id }) { SessionRow(it) }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun FocusBarChart(points: List<ChartPoint>) {
    val max = points.maxOfOrNull { it.minutes } ?: 0
    if (max == 0) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("这个时段还没有有效专注", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    Canvas(Modifier.fillMaxWidth().height(170.dp).padding(vertical = 12.dp)) {
        val gap = 3.dp.toPx()
        val width = ((size.width - gap * (points.size - 1)) / points.size).coerceAtLeast(2.dp.toPx())
        points.forEachIndexed { index, point ->
            val h = size.height * point.minutes / max.toFloat()
            drawRoundRect(
                color = Color(0xFF26A6C6),
                topLeft = Offset(index * (width + gap), size.height - h),
                size = androidx.compose.ui.geometry.Size(width, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2, width / 2),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(points.firstOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall)
        Text("最高 $max 分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(points.lastOrNull()?.label.orEmpty(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SessionRow(session: FocusSessionEntity) {
    val outcome = when (session.outcome) {
        SessionOutcome.NATURAL_COMPLETION -> "自然完成"
        SessionOutcome.EARLY_CREDITED -> "提前结束 · 已计入"
        SessionOutcome.EARLY_UNCREDITED -> "提前结束 · 未计入"
        SessionOutcome.ABANDONED -> "已放弃 · 未计入"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(session.todoTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${session.actualFocusSeconds / 60} 分", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("${formatDateTime(session.endedAt, session.endedZoneId)} · 计划 ${session.plannedSeconds / 60} 分钟", style = MaterialTheme.typography.bodySmall)
            Text(outcome, style = MaterialTheme.typography.labelMedium, color = if (session.countsTowardStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(text: String) = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

private fun hourlySeries(sessions: List<FocusSessionEntity>, date: LocalDate): List<ChartPoint> {
    val totals = sessions.filter { it.countsTowardStats && it.endedLocalDate == date.toString() }
        .groupBy { Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.of(it.endedZoneId)).hour }
        .mapValues { (_, values) -> values.sumOf { it.creditedMinutes } }
    return (0..23).map { ChartPoint("${it}时", totals[it] ?: 0) }
}

private fun shift(date: LocalDate, mode: RangeMode, amount: Long): LocalDate = when (mode) {
    RangeMode.DAY -> date.plusDays(amount)
    RangeMode.WEEK -> date.plusWeeks(amount)
    RangeMode.MONTH -> date.plusMonths(amount)
    RangeMode.CUSTOM -> date
}

private fun pickDate(context: android.content.Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}

private fun formatTimer(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun formatDateTime(epoch: Long, zoneId: String): String = Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
private fun timestampForFile(): String = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
