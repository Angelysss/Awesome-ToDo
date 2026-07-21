package com.awesometodo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF147D9E), secondary = Color(0xFF4F6F78), tertiary = Color(0xFF725A8C),
    background = Color(0xFFF5F8FA), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE4EEF1),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF75D1EF), secondary = Color(0xFFB4CBD2), tertiary = Color(0xFFD9B9F5),
    background = Color(0xFF0E1518), surface = Color(0xFF162126), surfaceVariant = Color(0xFF25343A),
)

@Composable
fun AwesomeTodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
