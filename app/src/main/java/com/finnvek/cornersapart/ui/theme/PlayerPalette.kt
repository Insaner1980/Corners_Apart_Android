package com.finnvek.cornersapart.ui.theme

import com.finnvek.cornersapart.model.GameConstants

data class PlayerPieceColors(
    val base: androidx.compose.ui.graphics.Color,
    val dark: androidx.compose.ui.graphics.Color,
    val highlight: androidx.compose.ui.graphics.Color,
    val ghost: androidx.compose.ui.graphics.Color,
)

object CornersApartPlayerPalette {
    fun colorsFor(colorIndex: Int): PlayerPieceColors =
        when (colorIndex.mod(GameConstants.PLAYER_COLORS.size)) {
            0 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerIndigo,
                    dark = CornersApartColors.PlayerIndigoDark,
                    highlight = CornersApartColors.PlayerIndigoHighlight,
                    ghost = CornersApartColors.PlayerIndigoGhost,
                )
            1 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerAmber,
                    dark = CornersApartColors.PlayerAmberDark,
                    highlight = CornersApartColors.PlayerAmberHighlight,
                    ghost = CornersApartColors.PlayerAmberGhost,
                )
            2 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerCoral,
                    dark = CornersApartColors.PlayerCoralDark,
                    highlight = CornersApartColors.PlayerCoralHighlight,
                    ghost = CornersApartColors.PlayerCoralGhost,
                )
            else ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerTeal,
                    dark = CornersApartColors.PlayerTealDark,
                    highlight = CornersApartColors.PlayerTealHighlight,
                    ghost = CornersApartColors.PlayerTealGhost,
                )
        }
}
