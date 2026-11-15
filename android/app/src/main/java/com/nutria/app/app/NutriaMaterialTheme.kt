package com.nutria.app.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nutria.app.core.data.AppTheme

@Composable
fun NutriaMaterialTheme(
    theme: AppTheme,
    content: @Composable () -> Unit,
) {
    val colors = when (theme) {
        AppTheme.SAGE -> ThemeColors.sage
        AppTheme.OCEAN -> ThemeColors.ocean
        AppTheme.LAVENDER -> ThemeColors.lavender
        AppTheme.CORAL -> ThemeColors.coral
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private object ThemeColors {
    private val background = Color(0xFFF7F8F3)
    private val surface = Color(0xFFFFFFFF)
    private val onSurface = Color(0xFF202720)
    private val onSurfaceVariant = Color(0xFF59635A)

    val sage = lightColorScheme(
        primary = Color(0xFF537E5C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2EEE2),
        onPrimaryContainer = Color(0xFF203A27),
        secondary = Color(0xFF496650),
        onSecondary = Color.White,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
    )
    val ocean = lightColorScheme(
        primary = Color(0xFF397D8A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEEF0),
        onPrimaryContainer = Color(0xFF17383F),
        secondary = Color(0xFF37636B),
        onSecondary = Color.White,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
    )
    val lavender = lightColorScheme(
        primary = Color(0xFF82659B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFECE2F4),
        onPrimaryContainer = Color(0xFF362345),
        secondary = Color(0xFF66517A),
        onSecondary = Color.White,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
    )
    val coral = lightColorScheme(
        primary = Color(0xFFB45846),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF9E3DD),
        onPrimaryContainer = Color(0xFF4A251D),
        secondary = Color(0xFF855044),
        onSecondary = Color.White,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
    )
}
