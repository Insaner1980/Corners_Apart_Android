package com.finnvek.cornersapart.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.components.RivalAvatar
import com.finnvek.cornersapart.ui.components.drawCandyCell
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow
import com.finnvek.cornersapart.viewmodel.RivalUiState
import kotlinx.coroutines.launch

/**
 * Rivals-ottelun aloitusanimaatio: pelaaja liukuu vasemmalta, vastustaja
 * oikealta ja iso VS pomppaa keskelle. Napautus ohittaa animaation.
 */
@Composable
fun RivalMatchIntro(
    rival: RivalUiState,
    playerName: String,
    playerColorIndex: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slide = remember { Animatable(1f) }
    val versusScale = remember { Animatable(0f) }
    LaunchedEffect(rival.id) {
        launch {
            slide.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
        }
        launch {
            versusScale.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = VERSUS_SCALE_THRESHOLD,
                    ),
            )
        }
    }
    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(rival.id) {
        fadeIn.animateTo(1f, animationSpec = tween(durationMillis = INTRO_FADE_MS))
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fadeIn.value }
                .background(CornersApartColors.BackgroundGradientBottom.copy(alpha = INTRO_SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IntroContestant(
                name = playerName,
                slideOffsetFraction = -slide.value,
            ) {
                PlayerIntroTile(colorIndex = playerColorIndex)
            }
            Text(
                text = stringResource(R.string.rival_intro_versus),
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = versusScale.value
                        scaleY = versusScale.value
                    },
                style = MaterialTheme.typography.displayMedium.withCandyShadow(),
                fontWeight = FontWeight.Black,
                color = CornersApartColors.BonusAccentBright,
            )
            IntroContestant(
                name = rival.name,
                slideOffsetFraction = slide.value,
            ) {
                RivalAvatar(
                    style = rival.style,
                    colorIndex = rival.colorIndex,
                    modifier = Modifier.size(CornersApartSpacing.RivalIntroAvatarSize),
                    showCrown = rival.tier == MASTER_TIER,
                )
            }
        }
    }
}

@Composable
private fun IntroContestant(
    name: String,
    slideOffsetFraction: Float,
    avatar: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier.graphicsLayer {
                translationX = slideOffsetFraction * size.width * INTRO_SLIDE_DISTANCE_FACTOR
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
    ) {
        avatar()
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.withCandyShadow(),
            fontWeight = FontWeight.Bold,
            color = CornersApartColors.TextOnDarkPrimary,
        )
    }
}

/** Pelaajan puolen laatta ilman kasvoja — sama candy-kieli kuin pelinapeilla. */
@Composable
private fun PlayerIntroTile(colorIndex: Int) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    Canvas(
        modifier =
            Modifier
                .size(CornersApartSpacing.RivalIntroAvatarSize)
                .padding(CornersApartSpacing.TinyGap),
    ) {
        drawCandyCell(
            topLeft = Offset.Zero,
            cellSize = size.minDimension,
            colors = colors,
        )
        drawCircle(
            color = CornersApartColors.TextOnDarkPrimary.copy(alpha = CornersApartAlpha.CellGloss),
            radius = size.minDimension * PLAYER_TILE_SHEEN_FRACTION,
            center = center,
        )
    }
}

private const val INTRO_SCRIM_ALPHA = 0.92f
private const val INTRO_FADE_MS = 150
private const val INTRO_SLIDE_DISTANCE_FACTOR = 3f
private const val VERSUS_SCALE_THRESHOLD = 0.001f
private const val PLAYER_TILE_SHEEN_FRACTION = 0.28f
