package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MealouxColorScheme = lightColorScheme(
    primary = MealouxOrange,
    onPrimary = MealouxWhite,
    primaryContainer = MealouxCreamDeep,
    onPrimaryContainer = MealouxInk,
    background = MealouxCream,
    onBackground = MealouxInk,
    surface = MealouxCream,
    onSurface = MealouxInk,
    surfaceVariant = MealouxCreamDeep,
    onSurfaceVariant = MealouxInk
)

@Composable
fun MealouxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MealouxColorScheme,
        typography = Typography,
        content = content
    )
}
