package com.finnvek.cornersapart.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CornersApartColors {
    val PlayerIndigo = Color(0xFF4338CA)
    val PlayerIndigoDark = Color(0xFF312E81)
    val PlayerIndigoHighlight = Color(0xFF6366F1)
    val PlayerIndigoGhost = Color(0x4D4338CA)

    val PlayerAmber = Color(0xFFE88C0A)
    val PlayerAmberDark = Color(0xFFA16207)
    val PlayerAmberHighlight = Color(0xFFF5B040)
    val PlayerAmberGhost = Color(0x4DE88C0A)

    val PlayerCoral = Color(0xFFE8513D)
    val PlayerCoralDark = Color(0xFF991B1B)
    val PlayerCoralHighlight = Color(0xFFF08070)
    val PlayerCoralGhost = Color(0x4DE8513D)

    val PlayerTeal = Color(0xFF0D9488)
    val PlayerTealDark = Color(0xFF134E4A)
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
}

object CornersApartSpacing {
    val ScreenPadding = 16.dp
    val SectionGap = 12.dp
    val CompactGap = 8.dp
    val TinyGap = 4.dp
    val BoardCellGap = 2.dp
    val BoardFrameWidth = 4.dp
    val PieceInnerInset = 2.dp
    val PieceShadowOffset = 1.dp
    val PieceShadowBlur = 2.dp
    val TouchTargetMin = 48.dp
    val PieceCardSize = 64.dp
    val PiecePreviewSize = 84.dp
    val ScoreCardMinHeight = 48.dp
    val ActivePlayerBorderWidth = 2.dp
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
}
