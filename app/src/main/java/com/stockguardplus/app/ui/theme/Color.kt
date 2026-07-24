package com.stockguardplus.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light — "Kağıt" (original palette, unchanged)
val PaperBackgroundLight = Color(0xFFF4F6F8)
val PaperSurfaceLight = Color(0xFFFFFFFF)
val PaperTextLight = Color(0xFF1F2933)
val PaperMutedLight = Color(0xFF788492)
val PaperBorderLight = Color(0xFFE1E6EA)
val PaperAccentLight = Color(0xFF3B4B8C)
val StockGoodLight = Color(0xFF2F855A)
val StockWarnLight = Color(0xFFB7791F)
val StockBadLight = Color(0xFFC53030)

// Dark — same family, inverted for low-light legibility
val PaperBackgroundDark = Color(0xFF15171B)
val PaperSurfaceDark = Color(0xFF1D2025)
val PaperTextDark = Color(0xFFEAECEF)
val PaperMutedDark = Color(0xFF8B93A0)
val PaperBorderDark = Color(0xFF2E3238)
val PaperAccentDark = Color(0xFF7C8BC4)
val StockGoodDark = Color(0xFF4FAE72)
val StockWarnDark = Color(0xFFD9A548)
val StockBadDark = Color(0xFFE2635F)

data class StockGuardPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val good: Color,
    val warn: Color,
    val bad: Color
)

val LightStockGuardPalette = StockGuardPalette(
    background = PaperBackgroundLight,
    surface = PaperSurfaceLight,
    text = PaperTextLight,
    muted = PaperMutedLight,
    border = PaperBorderLight,
    accent = PaperAccentLight,
    good = StockGoodLight,
    warn = StockWarnLight,
    bad = StockBadLight
)

val DarkStockGuardPalette = StockGuardPalette(
    background = PaperBackgroundDark,
    surface = PaperSurfaceDark,
    text = PaperTextDark,
    muted = PaperMutedDark,
    border = PaperBorderDark,
    accent = PaperAccentDark,
    good = StockGoodDark,
    warn = StockWarnDark,
    bad = StockBadDark
)

val LocalStockGuardPalette = staticCompositionLocalOf { LightStockGuardPalette }

// Existing call sites across the app read these as plain Color values inside
// Composables (e.g. `.background(PaperSurface, ...)`) — keeping the same names
// as @Composable-getter properties means none of those call sites needed to
// change when dark mode was added; they now resolve through the current theme.
val PaperBackground: Color @Composable get() = LocalStockGuardPalette.current.background
val PaperSurface: Color @Composable get() = LocalStockGuardPalette.current.surface
val PaperText: Color @Composable get() = LocalStockGuardPalette.current.text
val PaperMuted: Color @Composable get() = LocalStockGuardPalette.current.muted
val PaperBorder: Color @Composable get() = LocalStockGuardPalette.current.border
val PaperAccent: Color @Composable get() = LocalStockGuardPalette.current.accent
val StockGood: Color @Composable get() = LocalStockGuardPalette.current.good
val StockWarn: Color @Composable get() = LocalStockGuardPalette.current.warn
val StockBad: Color @Composable get() = LocalStockGuardPalette.current.bad
