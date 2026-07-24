package com.stockguardplus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StockGuardLightColorScheme = lightColorScheme(
    primary = PaperAccentLight,
    onPrimary = Color.White,
    background = PaperBackgroundLight,
    onBackground = PaperTextLight,
    surface = PaperSurfaceLight,
    onSurface = PaperTextLight,
    surfaceVariant = PaperSurfaceLight,
    onSurfaceVariant = PaperMutedLight,
    outline = PaperBorderLight,
    error = StockBadLight
)

private val StockGuardDarkColorScheme = darkColorScheme(
    primary = PaperAccentDark,
    onPrimary = Color.White,
    background = PaperBackgroundDark,
    onBackground = PaperTextDark,
    surface = PaperSurfaceDark,
    onSurface = PaperTextDark,
    surfaceVariant = PaperSurfaceDark,
    onSurfaceVariant = PaperMutedDark,
    outline = PaperBorderDark,
    error = StockBadDark
)

/** null/"system" follows the device setting; "light" and "dark" override it. */
@Composable
fun StockGuardPlusTheme(themeMode: String? = null, content: @Composable () -> Unit) {
    val useDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(
        LocalStockGuardPalette provides if (useDarkTheme) DarkStockGuardPalette else LightStockGuardPalette
    ) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) StockGuardDarkColorScheme else StockGuardLightColorScheme,
            typography = StockGuardTypography,
            shapes = StockGuardShapes,
            content = content
        )
    }
}
