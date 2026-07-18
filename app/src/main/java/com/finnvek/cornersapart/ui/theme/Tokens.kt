package com.finnvek.cornersapart.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CornersApartColors {
    val PlayerIndigo = Color(0xFF4338CA)
    val PlayerIndigoDark = Color(0xFF312E81)
    val PlayerIndigoHighlight = Color(0xFF6366F1)
    val PlayerIndigoGhost = Color(0x4D4338CA)

    val PlayerAmber = Color(0xFFE88C0A)
    val PlayerAmberDark = Color(0xFFB56E08)
    val PlayerAmberHighlight = Color(0xFFF5B040)
    val PlayerAmberGhost = Color(0x4DE88C0A)

    val PlayerCoral = Color(0xFFE8513D)
    val PlayerCoralDark = Color(0xFFB02A20)
    val PlayerCoralHighlight = Color(0xFFF08070)
    val PlayerCoralGhost = Color(0x4DE8513D)

    val PlayerTeal = Color(0xFF0D9488)
    val PlayerTealDark = Color(0xFF0A6B62)
    val PlayerTealHighlight = Color(0xFF2DD4BF)
    val PlayerTealGhost = Color(0x4D0D9488)

    val AppBackground = Color(0xFFE4E4E8)
    val BoardCellGap = Color(0xFFDCDCE0)
    val BoardCellSurface = Color(0xFFFAFAFA)
    val BoardFrame = Color(0xFF2C2C30)
    val CardSurface = Color(0xFFFFFFFF)
    val BonusAccent = Color(0xFFD8A928)

    val TextPrimary = Color(0xFF1A1A1E)
    val TextSecondary = Color(0xFF4A4A52)
    val TextMuted = Color(0xFF8A8A92)
    val OnPlayerColor = Color(0xFFFFFFFF)
    val PieceShadowOverlay = Color(0xFF000000)

    val BackgroundGradientTop = Color(0xFF312B63)
    val BackgroundGradientBottom = Color(0xFF1D1940)
    val BoardPanel = Color(0xFF241F4E)
    val BoardCellEmpty = Color(0xFF1B173D)
    val PanelSurface = Color(0xFF2E2960)
    val PanelSurfaceRaised = Color(0xFF3A3475)
    val DialogSurface = Color(0xFF2A2458)
    val DialogSurfaceEdge = Color(0xFF171335)

    val TextOnDarkPrimary = Color(0xFFFFFFFF)
    val TextOnDarkSecondary = Color(0xFFBCB6EA)
    val TextOnDarkMuted = Color(0xFF7E78B4)
    val TextShadow = Color(0xFF000000)

    val BonusAccentBright = Color(0xFFFFC53D)

    val ButtonPrimaryFace = Color(0xFF5B4FE8)
    val ButtonPrimaryBevel = Color(0xFF3A31A8)
    val ButtonPositiveFace = Color(0xFF22B573)
    val ButtonPositiveBevel = Color(0xFF15804F)
    val ButtonWarnFace = Color(0xFFE8513D)
    val ButtonWarnBevel = Color(0xFFA83224)
    val ButtonNeutralFace = Color(0xFF454078)
    val ButtonNeutralBevel = Color(0xFF2B2760)
}

object CornersApartSpacing {
    val ScreenPadding = 16.dp
    val SectionGap = 12.dp
    val CompactGap = 8.dp
    val TinyGap = 4.dp
    val BoardCellGap = 2.dp
    val BoardFrameWidth = 4.dp
    val TouchTargetMin = 48.dp
    val PieceCardSize = 64.dp
    val PiecePreviewSize = 84.dp
    val ScoreCardMinHeight = 40.dp
    val ActivePlayerBorderWidth = 2.dp

    val BoardPanelRadius = 20.dp
    val BoardPanelPadding = 8.dp
    val CandyButtonRadius = 18.dp
    val CandyButtonBevel = 4.dp
    val CandyButtonHeight = 52.dp
    val DialogRadius = 24.dp
}

@Suppress("ktlint:standard:property-naming")
object CornersApartAlpha {
    const val PassedPlayer = 0.40f
    const val UsedPiece = 0.35f
    const val PieceHighlight = 0.35f
    const val PieceShadow = 0.50f
    const val PieceInnerInset = 0.08f
    const val PieceDropShadow = 0.12f
    const val StartMarker = 0.55f
    const val CellGloss = 0.18f
    const val TextShadow = 0.35f
    const val BonusGlow = 0.30f
    const val EmptyCellInnerShadow = 0.25f
    const val GhostOutline = 0.60f
    const val DisabledCandy = 0.40f
}
