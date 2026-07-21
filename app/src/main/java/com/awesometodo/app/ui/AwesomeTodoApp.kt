package com.awesometodo.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awesometodo.app.data.TimerMode
import com.awesometodo.app.data.TodoEntity
import com.awesometodo.app.timer.FocusTimerService

@Composable
fun AwesomeTodoApp(vm: AppViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var editorTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<TodoEntity?>(null) }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = state.activeTimer != null && !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.activeTimer?.singletonId) {
        if (state.activeTimer != null) ContextCompat.startForegroundService(context, FocusTimerService.startIntent(context))
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingStart?.let(vm::startTimer)
        pendingStart = null
    }

    fun start(todo: TodoEntity) {
        if (todo.timerMode != TimerMode.UNTIMED && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingStart = todo
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else vm.startTimer(todo)
    }

    when {
        state.activeTimer != null -> TimerScreen(state.activeTimer!!, vm::pauseTimer, vm::resumeTimer, vm::finishEarly, vm::abandon)
        showHistory -> HistoryScreen(state.sessions, onBack = { showHistory = false })
        else -> Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Box(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding()
                ) {
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Text("待", fontWeight = FontWeight.Bold) }, label = { Text("待办") })
                        NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { Text("统", fontWeight = FontWeight.Bold) }, label = { Text("统计") })
                        NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Text("设", fontWeight = FontWeight.Bold) }, label = { Text("设置") })
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(onClick = { editorTodo = null; showEditor = true }) {
                        Text("＋", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            },
        ) { padding ->
            when (selectedTab) {
                0 -> TodoScreen(
                    todos = state.todos, sessions = state.sessions, padding = padding, onStart = ::start,
                    onEdit = { editorTodo = it; showEditor = true },
                    onCompleted = { vm.setCompleted(it, true) }, onDelete = vm::deleteTodo,
                )
                1 -> StatsScreen(state.sessions, state.summary, padding, onHistory = { showHistory = true })
                else -> SettingsScreen(vm, state.activeTimer == null, padding)
            }
        }
    }

    if (showEditor) {
        TodoEditorDialog(
            todo = editorTodo,
            onDismiss = { showEditor = false },
            onSave = { id, title, minutes, theme, mode ->
                vm.saveTodo(id, title, minutes, theme, mode)
                showEditor = false
            },
        )
    }
}
