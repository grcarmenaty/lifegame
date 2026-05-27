package com.grcarmenaty.lifegame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Parchment,
    onPrimary = Ink,
    secondary = Ember,
    background = Ink,
    onBackground = Parchment,
    surface = InkSoft,
    onSurface = Parchment,
    surfaceVariant = InkSoft,
    onSurfaceVariant = ParchmentDim,
)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Parchment,
    secondary = Ember,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
)

@Composable
fun LifegameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LifegameTypography,
        content = content,
    )
}
