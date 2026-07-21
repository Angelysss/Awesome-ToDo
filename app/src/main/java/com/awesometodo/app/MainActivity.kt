package com.awesometodo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.awesometodo.app.ui.AwesomeTodoApp
import com.awesometodo.app.ui.AwesomeTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AwesomeTodoTheme { AwesomeTodoApp() } }
    }
}
