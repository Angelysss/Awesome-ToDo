package com.awesometodo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.awesometodo.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF147D9E), secondary = Color(0xFF4F6F78), tertiary = Color(0xFF725A8C),
    background = Color(0xFFF5F8FA), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE4EEF1),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF75D1EF), secondary = Color(0xFFB4CBD2), tertiary = Color(0xFFD9B9F5),
    background = Color(0xFF0E1518), surface = Color(0xFF162126), surfaceVariant = Color(0xFF25343A),
)

private val CompactTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontSize = 27.sp, lineHeight = 34.sp),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 23.sp, lineHeight = 29.sp),
    headlineSmall = Typography().headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = Typography().titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = Typography().titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = Typography().titleSmall.copy(fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 9.sp, lineHeight = 13.sp),
)

@Composable
fun AwesomeTodoTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val useDarkColors = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDarkColors) DarkColors else LightColors,
        typography = CompactTypography,
        content = content,
    )
}
