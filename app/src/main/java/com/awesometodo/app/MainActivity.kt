package com.awesometodo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.awesometodo.app.data.ThemeMode
import com.awesometodo.app.ui.AwesomeTodoApp
import com.awesometodo.app.ui.AwesomeTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as AwesomeTodoApplication).settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            AwesomeTodoTheme(themeMode) { AwesomeTodoApp() }
        }
    }
}
