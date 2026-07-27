package com.finnvek.cornersapart.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CornersApartColors {
    val PlayerPink = Color(0xFFF0509E)
    val PlayerPinkDark = Color(0xFFB62E72)
    val PlayerPinkHighlight = Color(0xFFFF8AC2)

    val PlayerMango = Color(0xFFFFA726)
    val PlayerMangoDark = Color(0xFFC67908)
    val PlayerMangoHighlight = Color(0xFFFFC46B)

    val PlayerCyan = Color(0xFF29C8E0)
    val PlayerCyanDark = Color(0xFF17849B)
    val PlayerCyanHighlight = Color(0xFF7BE3F2)

    val PlayerLime = Color(0xFF9BD934)
    val PlayerLimeDark = Color(0xFF6FA51F)
    val PlayerLimeHighlight = Color(0xFFC4EE7D)

    val BonusAccent = Color(0xFFD8A928)

    val BackgroundGradientTop = Color(0xFF3A3378)
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
    val GlossTint = Color(0xFFFFFFFF)

    val ButtonPrimaryFace = Color(0xFF5B4FE8)
    val ButtonPrimaryBevel = Color(0xFF3A31A8)
    val ButtonPositiveFace = Color(0xFF22B573)
    val ButtonPositiveBevel = Color(0xFF15804F)
    val ButtonWarnFace = Color(0xFFE8513D)
    val ButtonWarnBevel = Color(0xFFA83224)
    val InvalidPreviewHighlight = Color(0xFFFF9A85)
    val ButtonNeutralFace = Color(0xFF454078)
    val ButtonNeutralBevel = Color(0xFF2B2760)

    val MedalSilver = Color(0xFFCBD0EE)
    val MedalSilverDark = Color(0xFF878DBA)
    val MedalBronze = Color(0xFFD98F55)
    val MedalBronzeDark = Color(0xFF9C6134)
    val StreakFlameFace = Color(0xFFFF8A3C)
    val StreakFlameDeep = Color(0xFFE8513D)
}

object CornersApartSpacing {
    val ScreenPadding = 16.dp
    val SectionGap = 12.dp
    val CompactGap = 8.dp
    val TinyGap = 4.dp
    val BoardCellGap = 2.dp
    val TouchTargetMin = 48.dp
    val PieceCardSize = 64.dp
    val PiecePreviewSize = 84.dp
    val ScoreCardMinHeight = 40.dp
    val ActivePlayerBorderWidth = 2.dp

    val ScoreSwatchSize = 16.dp
    val ColorSwatchSize = 44.dp
    val ColorSwatchRingWidth = 3.dp
    val ConfettiHeight = 96.dp
    val PieceMeterHeight = 6.dp
    val BoardPanelRadius = 20.dp
    val BoardPanelPadding = 8.dp
    val BoardPanelBorderWidth = 1.dp
    val TitleAccentBarHeight = 4.dp
    val TitleAccentBarWidth = 120.dp
    val CandyButtonRadius = 18.dp
    val CandyButtonBevel = 4.dp
    val CandyButtonHeight = 52.dp
    val DialogRadius = 24.dp

    val RivalCardRadius = 16.dp
    val RivalChallengerBorderWidth = 2.dp
    val RivalAvatarSize = 56.dp
    val RivalPipSize = 8.dp
    val RivalIntroAvatarSize = 112.dp

    val PodiumHeightFirst = 88.dp
    val PodiumHeightSecond = 64.dp
    val PodiumHeightThird = 48.dp
    val PodiumMedalSize = 40.dp
    val PodiumRadius = 12.dp
    val HallOfFameRankWidth = 32.dp
    val HallOfFameSwatchSize = 12.dp
    val StreakBadgeRadius = 20.dp
}

object CornersApartBreakpoints {
    const val EXPANDED_WIDTH_DP = 840
}

@Suppress("ktlint:standard:property-naming")
object CornersApartAlpha {
    const val PassedPlayer = 0.40f
    const val UsedPiece = 0.35f
    const val StartMarker = 0.55f
    const val CellGloss = 0.18f
    const val TextShadow = 0.35f
    const val BonusGlow = 0.30f
    const val DisabledCandy = 0.40f
    const val PreviewCell = 0.88f
    const val PreviewOutline = 0.90f
    const val PreviewDim = 0.30f
    const val GuideBand = 0.08f
    const val GuideLine = 0.30f
}
