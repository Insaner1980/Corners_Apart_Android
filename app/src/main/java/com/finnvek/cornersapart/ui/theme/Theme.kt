package com.finnvek.cornersapart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CandyColorScheme =
    darkColorScheme(
        primary = CornersApartColors.ButtonPrimaryFace,
        onPrimary = CornersApartColors.TextOnDarkPrimary,
        secondary = CornersApartColors.ButtonPositiveFace,
        onSecondary = CornersApartColors.TextOnDarkPrimary,
        tertiary = CornersApartColors.BonusAccentBright,
        background = CornersApartColors.BackgroundGradientBottom,
        onBackground = CornersApartColors.TextOnDarkPrimary,
        surface = CornersApartColors.PanelSurface,
        onSurface = CornersApartColors.TextOnDarkPrimary,
        surfaceVariant = CornersApartColors.PanelSurfaceRaised,
        onSurfaceVariant = CornersApartColors.TextOnDarkSecondary,
        outline = CornersApartColors.TextOnDarkMuted,
        surfaceContainerHigh = CornersApartColors.DialogSurface,
    )

@Composable
fun CornersApartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CandyColorScheme,
        typography = CornersApartTypography,
        shapes = CornersApartShapes,
        content = content,
    )
}
