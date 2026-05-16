package com.autosort.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary        = Secondary,
    onSecondary      = OnSecondary,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnBackground,
    onSurface        = OnSurface,
    outline          = Outline,
    error            = Error,
    onError          = OnError
)

@Composable
fun AutoSortTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AutoSortTypography,
        content     = content
    )
}
