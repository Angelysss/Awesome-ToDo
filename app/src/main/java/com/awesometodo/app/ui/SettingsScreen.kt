package com.awesometodo.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awesometodo.app.BuildConfig
import com.awesometodo.app.update.UpdateResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val REPOSITORY_URL = "https://github.com/Angelysss/Awesome-ToDo"

@Composable
internal fun SettingsScreen(vm: AppViewModel, backupEnabled: Boolean, padding: PaddingValues) {
    val context = LocalContext.current
    val updateResult by vm.updateResult.collectAsState()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { vm.showMessage("无法打开浏览器") }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        GradientHeader("设置", "管理数据与了解 Awesome ToDo")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard("关于与支持") {
                    SettingAction("检查更新", "从 GitHub Releases 获取最新测试版本") { vm.checkForUpdates() }
                    SettingAction("捐赠", "暂未开放") { vm.showMessage("捐赠功能暂未开放") }
                    SettingAction("GitHub", "查看源码、版本说明与问题反馈") { openUrl(REPOSITORY_URL) }
                }
            }
            item {
                SectionCard("数据备份") {
                    Text("备份包含待办、历史记录和设置；恢复会整体替换当前数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = { exportLauncher.launch("awesome-todo-backup-${timestampForFile()}.json") },
                            enabled = backupEnabled,
                            modifier = Modifier.weight(1f),
                        ) { Text("导出备份") }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            enabled = backupEnabled,
                            modifier = Modifier.weight(1f),
                        ) { Text("恢复备份") }
                    }
                    if (!backupEnabled) Text("计时进行中，暂不能备份或恢复。", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Awesome ToDo v${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("恢复备份？") },
            text = { Text("当前待办、历史记录和设置将被备份内容整体替换。验证失败时现有数据不会改变。") },
            confirmButton = { TextButton(onClick = { vm.importBackup(uri); pendingImport = null }) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } },
        )
    }

    updateResult?.let { result ->
        when (result) {
            is UpdateResult.Available -> AlertDialog(
                onDismissRequest = vm::clearUpdateResult,
                title = { Text("发现新版本 v${result.release.version}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(result.release.title, fontWeight = FontWeight.SemiBold)
                        if (result.release.notes.isNotBlank()) Text(result.release.notes.take(500))
                        Text("将打开 GitHub Release 页面下载安装包。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { TextButton(onClick = { openUrl(result.release.url); vm.clearUpdateResult() }) { Text("前往下载") } },
                dismissButton = { TextButton(onClick = vm::clearUpdateResult) { Text("稍后") } },
            )
            is UpdateResult.Current -> AlertDialog(
                onDismissRequest = vm::clearUpdateResult,
                title = { Text("已是最新版本") },
                text = { Text("当前版本 v${result.version}") },
                confirmButton = { TextButton(onClick = vm::clearUpdateResult) { Text("知道了") } },
            )
            is UpdateResult.Failed -> AlertDialog(
                onDismissRequest = vm::clearUpdateResult,
                title = { Text("检查更新失败") },
                text = { Text("${result.message}\n请检查网络后重试。") },
                confirmButton = { TextButton(onClick = vm::clearUpdateResult) { Text("知道了") } },
            )
        }
    }
}

@Composable
private fun SettingAction(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge)
    }
}

private fun timestampForFile(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
