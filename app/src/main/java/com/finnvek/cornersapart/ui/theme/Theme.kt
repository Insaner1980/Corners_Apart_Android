package com.finnvek.cornersapart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = CornersApartColors.PlayerIndigo,
        onPrimary = CornersApartColors.OnPlayerColor,
        secondary = CornersApartColors.PlayerTeal,
        onSecondary = CornersApartColors.OnPlayerColor,
        tertiary = CornersApartColors.BonusAccent,
        background = CornersApartColors.AppBackground,
        onBackground = CornersApartColors.TextPrimary,
        surface = CornersApartColors.CardSurface,
        onSurface = CornersApartColors.TextPrimary,
        surfaceVariant = CornersApartColors.BoardCellGap,
        onSurfaceVariant = CornersApartColors.TextSecondary,
        outline = CornersApartColors.TextMuted,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = CornersApartColors.PlayerIndigoHighlight,
        onPrimary = CornersApartColors.OnPlayerColor,
        secondary = CornersApartColors.PlayerTealHighlight,
        onSecondary = CornersApartColors.TextPrimary,
        tertiary = CornersApartColors.BonusAccent,
        background = CornersApartColors.BoardFrame,
        onBackground = CornersApartColors.AppBackground,
        surface = CornersApartColors.TextPrimary,
        onSurface = CornersApartColors.AppBackground,
        surfaceVariant = CornersApartColors.TextSecondary,
        onSurfaceVariant = CornersApartColors.AppBackground,
        outline = CornersApartColors.TextMuted,
    )

@Composable
fun CornersApartTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = CornersApartTypography,
        shapes = CornersApartShapes,
        content = content,
    )
}
