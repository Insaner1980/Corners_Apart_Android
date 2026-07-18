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
                    base = CornersApartColors.PlayerPink,
                    dark = CornersApartColors.PlayerPinkDark,
                    highlight = CornersApartColors.PlayerPinkHighlight,
                    ghost = CornersApartColors.PlayerPinkGhost,
                )
            1 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerMango,
                    dark = CornersApartColors.PlayerMangoDark,
                    highlight = CornersApartColors.PlayerMangoHighlight,
                    ghost = CornersApartColors.PlayerMangoGhost,
                )
            2 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerCyan,
                    dark = CornersApartColors.PlayerCyanDark,
                    highlight = CornersApartColors.PlayerCyanHighlight,
                    ghost = CornersApartColors.PlayerCyanGhost,
                )
            else ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerLime,
                    dark = CornersApartColors.PlayerLimeDark,
                    highlight = CornersApartColors.PlayerLimeHighlight,
                    ghost = CornersApartColors.PlayerLimeGhost,
                )
        }
}
