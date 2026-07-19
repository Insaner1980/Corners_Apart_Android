package com.finnvek.cornersapart.ui.theme

import com.finnvek.cornersapart.model.GameConstants

data class PlayerPieceColors(
    val base: androidx.compose.ui.graphics.Color,
    val dark: androidx.compose.ui.graphics.Color,
    val highlight: androidx.compose.ui.graphics.Color,
)

object CornersApartPlayerPalette {
    /** Punainen laattaväristö laittoman sijoituksen esikatselulle. */
    val invalidPreview =
        PlayerPieceColors(
            base = CornersApartColors.ButtonWarnFace,
            dark = CornersApartColors.ButtonWarnBevel,
            highlight = CornersApartColors.InvalidPreviewHighlight,
        )

    fun colorsFor(colorIndex: Int): PlayerPieceColors =
        when (colorIndex.mod(GameConstants.PLAYER_COLORS.size)) {
            0 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerPink,
                    dark = CornersApartColors.PlayerPinkDark,
                    highlight = CornersApartColors.PlayerPinkHighlight,
                )
            1 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerMango,
                    dark = CornersApartColors.PlayerMangoDark,
                    highlight = CornersApartColors.PlayerMangoHighlight,
                )
            2 ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerCyan,
                    dark = CornersApartColors.PlayerCyanDark,
                    highlight = CornersApartColors.PlayerCyanHighlight,
                )
            else ->
                PlayerPieceColors(
                    base = CornersApartColors.PlayerLime,
                    dark = CornersApartColors.PlayerLimeDark,
                    highlight = CornersApartColors.PlayerLimeHighlight,
                )
        }
}
