package com.lyx.sms.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = White,
    secondary = Teal500,
    tertiary = Amber500,
    background = Cloud50,
    surface = White,
    surfaceVariant = Slate100,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Slate100,
    onSurface = Slate950,
    onSurfaceVariant = Slate800
)

private val DarkColors = darkColorScheme(
    primary = Blue300,
    onPrimary = Slate950,
    secondary = Teal300,
    onSecondary = Slate950,
    tertiary = Amber500,
    background = Slate950,
    surface = Slate900,
    surfaceVariant = Slate800,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate900,
    onSurface = White,
    onSurfaceVariant = Slate200
)

@Composable
fun MySmsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
