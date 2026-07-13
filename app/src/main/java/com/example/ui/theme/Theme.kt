package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorName: String = "Azul Stripe",
    content: @Composable () -> Unit
) {
    // Dynamic Primary Accents
    val primaryColor = when (primaryColorName) {
        "Verde Revolut" -> Color(0xFF00C6FF)
        "Slate Linear" -> Color(0xFF5E6AD2)
        "Coral Monzo" -> Color(0xFFFE5F55)
        else -> if (darkTheme) Color(0xFF4379FF) else Color(0xFF21005D)
    }

    val onAccentBlueColor = when (primaryColorName) {
        "Verde Revolut" -> if (darkTheme) Color(0xFF00E5FF) else Color(0xFF004D61)
        "Slate Linear" -> if (darkTheme) Color(0xFFD9DFFF) else Color(0xFF1E2465)
        "Coral Monzo" -> if (darkTheme) Color(0xFFFFECEB) else Color(0xFF7F100D)
        else -> if (darkTheme) Color(0xFF93C5FD) else Color(0xFF001C38)
    }

    val accentBlueColor = when (primaryColorName) {
        "Verde Revolut" -> if (darkTheme) Color(0xFF002A35) else Color(0xFFE0F7FA)
        "Slate Linear" -> if (darkTheme) Color(0xFF1B1D37) else Color(0xFFEEF0FF)
        "Coral Monzo" -> if (darkTheme) Color(0xFF3D0907) else Color(0xFFFFEBEC)
        else -> if (darkTheme) Color(0xFF172554) else Color(0xFFD3E4FF)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = onAccentBlueColor,
            onSecondary = Color(0xFF121214),
            background = Color(0xFF121214), // Premium deep charcoal grey background
            surface = Color(0xFF18181C),    // Clean, refined grey surface
            surfaceVariant = Color(0xFF222227), // Elevated card/dialog surface
            onBackground = Color(0xFFF3F4F6), // Crispy clear off-white
            onSurface = Color(0xFFF3F4F6),
            error = Color(0xFFF87171)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = primaryColor,
            onSecondary = Color.White,
            background = Color(0xFFFAF9FD),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = accentBlueColor,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            error = Color(0xFFBE123C)
        )
    }

    val brandColors = if (darkTheme) {
        BrandColors(
            brandBackground = Color(0xFF121214), // Beautiful premium matte grey background
            brandSurface = Color(0xFF18181C),    // Pure minimalist grey surface
            brandSurfaceElevated = Color(0xFF222227), // Nesting surface
            brandBorder = Color(0xFF26262B),     // Super fine, clean grey border
            emeraldGreen = Color(0xFF10B981),    // Crisp emerald green
            softEmerald = Color(0xFF0F2620),     // Very soft translucent-style forest container
            roseCoral = Color(0xFFEF4444),       // Sleek, clean rose
            softRose = Color(0xFF2B1010),        // Deep elegant red background
            amberYellow = Color(0xFFF59E0B),
            softAmber = Color(0xFF2E1905),
            textPrimary = Color(0xFFF3F4F6),     // Extremely clean soft-white for text
            textSecondary = Color(0xFF9CA3AF),   // Premium Slate grey
            textMuted = Color(0xFF6B7280),       // Clean muted grey for secondary elements
            accentLavender = Color(0xFF4C1D95),
            onAccentLavender = Color(0xFFF5F3FF),
            accentBlue = accentBlueColor,
            onAccentBlue = onAccentBlueColor
        )
    } else {
        BrandColors(
            brandBackground = Color(0xFFFAF9FD),
            brandSurface = Color(0xFFFFFFFF),
            brandSurfaceElevated = accentBlueColor,
            brandBorder = Color(0xFFE1E2EC),
            emeraldGreen = Color(0xFF15803D),
            softEmerald = Color(0xFFDCFCE7),
            roseCoral = Color(0xFFBE123C),
            softRose = Color(0xFFFFE4E6),
            amberYellow = Color(0xFFB45309),
            softAmber = Color(0xFFFEF3C7),
            textPrimary = Color(0xFF1C1B1F),
            textSecondary = Color(0xFF49454F),
            textMuted = Color(0xFF79747E),
            accentLavender = Color(0xFFEADDFF),
            onAccentLavender = Color(0xFF21005D),
            accentBlue = accentBlueColor,
            onAccentBlue = onAccentBlueColor
        )
    }

    CompositionLocalProvider(LocalBrandColors provides brandColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
