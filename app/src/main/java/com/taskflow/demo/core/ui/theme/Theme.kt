package com.taskflow.demo.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = FlowYellowDeep,
    onPrimary = FlowInk,
    primaryContainer = FlowYellow,
    onPrimaryContainer = FlowInk,
    secondary = FlowBlue,
    tertiary = FlowCoral,
    background = FlowSurface,
    surface = FlowCard,
    onSurface = FlowInk,
    surfaceVariant = Color(0xFFFFF7D6),
    outlineVariant = FlowLine,
    error = FlowError
)

private val DarkScheme = darkColorScheme(
    primary = FlowYellow,
    onPrimary = FlowInk,
    secondary = FlowBlue,
    tertiary = FlowCoral,
    error = FlowError
)

@Composable
fun TaskFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
