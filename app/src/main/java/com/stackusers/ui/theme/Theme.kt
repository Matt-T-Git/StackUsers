package com.stackusers.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StackOverflowOrange = Color(0xFFF48024)
private val StackOverflowOrangeDark = Color(0xFFD2691E)
private val SurfaceDark = Color(0xFF1E1E1E)
private val BackgroundDark = Color(0xFF121212)

private val LightColorScheme = lightColorScheme(
    primary = StackOverflowOrange,
    onPrimary = Color.White,
    secondary = StackOverflowOrangeDark,
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = StackOverflowOrange,
    onPrimary = Color.Black,
    secondary = StackOverflowOrangeDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun StackUsersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
