package com.example.studypoint

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF3B82F6),
    onTertiary = Color.White,
    background = Color(0xFF1E3A8A),
    onBackground = Color.White,
    surface = Color(0xFF1F2937),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFE5E7EB),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = Color(0xFF3B82F6),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun StudyPointTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * Same as [StudyPointTheme] but resolves dark/light from the persisted
 * Settings theme choice (System / Light / Dark) instead of always following
 * the system. Screens using this wrapper follow the user's theme setting.
 */
@Composable
fun StudyPointThemeWithSettings(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dark = remember { AppSettings.isDarkTheme(context) }
    StudyPointTheme(
        darkTheme = dark,
        content = content
    )
}
