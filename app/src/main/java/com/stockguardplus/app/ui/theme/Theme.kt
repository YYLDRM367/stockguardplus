package com.stockguardplus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StockGuardColorScheme = lightColorScheme(
    primary = PaperAccent,
    onPrimary = Color.White,
    background = PaperBackground,
    onBackground = PaperText,
    surface = PaperSurface,
    onSurface = PaperText,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = PaperMuted,
    outline = PaperBorder,
    error = StockBad
)

@Composable
fun StockGuardPlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StockGuardColorScheme,
        typography = StockGuardTypography,
        shapes = StockGuardShapes,
        content = content
    )
}
