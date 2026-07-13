package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// Static light colors for preview/initialization
val StaticBrandBackground = Color(0xFFFAF9FD)
val StaticBrandSurface = Color(0xFFFFFFFF)
val StaticBrandSurfaceElevated = Color(0xFFD3E4FF)
val StaticBrandBorder = Color(0xFFE1E2EC)
val StaticEmeraldGreen = Color(0xFF15803D)
val StaticSoftEmerald = Color(0xFFDCFCE7)
val StaticRoseCoral = Color(0xFFBE123C)
val StaticSoftRose = Color(0xFFFFE4E6)
val StaticAmberYellow = Color(0xFFB45309)
val StaticSoftAmber = Color(0xFFFEF3C7)
val StaticTextPrimary = Color(0xFF1C1B1F)
val StaticTextSecondary = Color(0xFF49454F)
val StaticTextMuted = Color(0xFF79747E)
val StaticAccentLavender = Color(0xFFEADDFF)
val StaticOnAccentLavender = Color(0xFF21005D)
val StaticAccentBlue = Color(0xFFD3E4FF)
val StaticOnAccentBlue = Color(0xFF001C38)

val PrimaryLight = Color(0xFF21005D)
val OnPrimaryLight = Color(0xFFFFFFFF)
val SecondaryLight = Color(0xFF001C38)
val BackgroundLight = StaticBrandBackground
val SurfaceLight = StaticBrandSurface
val OnBackgroundLight = StaticTextPrimary
val OnSurfaceLight = StaticTextPrimary

// Dynamic brand color system
data class BrandColors(
    val brandBackground: Color,
    val brandSurface: Color,
    val brandSurfaceElevated: Color,
    val brandBorder: Color,
    val emeraldGreen: Color,
    val softEmerald: Color,
    val roseCoral: Color,
    val softRose: Color,
    val amberYellow: Color,
    val softAmber: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentLavender: Color,
    val onAccentLavender: Color,
    val accentBlue: Color,
    val onAccentBlue: Color
)

val LocalBrandColors = staticCompositionLocalOf {
    BrandColors(
        brandBackground = StaticBrandBackground,
        brandSurface = StaticBrandSurface,
        brandSurfaceElevated = StaticBrandSurfaceElevated,
        brandBorder = StaticBrandBorder,
        emeraldGreen = StaticEmeraldGreen,
        softEmerald = StaticSoftEmerald,
        roseCoral = StaticRoseCoral,
        softRose = StaticSoftRose,
        amberYellow = StaticAmberYellow,
        softAmber = StaticSoftAmber,
        textPrimary = StaticTextPrimary,
        textSecondary = StaticTextSecondary,
        textMuted = StaticTextMuted,
        accentLavender = StaticAccentLavender,
        onAccentLavender = StaticOnAccentLavender,
        accentBlue = StaticAccentBlue,
        onAccentBlue = StaticOnAccentBlue
    )
}

// Package level dynamic Composable properties
val BrandBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.brandBackground

val BrandSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.brandSurface

val BrandSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.brandSurfaceElevated

val BrandBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.brandBorder

val EmeraldGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.emeraldGreen

val SoftEmerald: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.softEmerald

val RoseCoral: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.roseCoral

val SoftRose: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.softRose

val AmberYellow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.amberYellow

val SoftAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.softAmber

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.textMuted

val AccentLavender: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.accentLavender

val OnAccentLavender: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.onAccentLavender

val AccentBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.accentBlue

val OnAccentBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current.onAccentBlue
