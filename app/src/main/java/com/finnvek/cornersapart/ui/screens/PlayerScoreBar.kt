package com.finnvek.cornersapart.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.components.drawCandyCell
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.PlayerUiState

@Composable
fun PlayerScoreBar(
    players: List<PlayerUiState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
    ) {
        players.chunked(SCORE_COLUMNS).forEach { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
            ) {
                rowPlayers.forEach { player ->
                    PlayerScoreCard(
                        player = player,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    player: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    val colors = CornersApartPlayerPalette.colorsFor(player.colorIndex)
    Surface(
        modifier =
            modifier
                .heightIn(min = CornersApartSpacing.ScoreCardMinHeight)
                .alpha(if (player.hasPassed) CornersApartAlpha.PassedPlayer else 1f),
        shape = MaterialTheme.shapes.medium,
        color =
            if (player.isCurrentTurn) {
                CornersApartColors.PanelSurfaceRaised
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (player.isCurrentTurn) {
                BorderStroke(CornersApartSpacing.ActivePlayerBorderWidth, colors.base)
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(CornersApartSpacing.ScoreSwatchSize)) {
                drawCandyCell(
                    topLeft = Offset.Zero,
                    cellSize = size.minDimension,
                    colors = colors,
                )
            }
            val animatedScore by
                animateIntAsState(
                    targetValue = player.totalScore,
                    label = "scoreCount",
                )
            Column {
                Text(
                    text = stringResource(R.string.player_score_label, player.name, animatedScore),
                    style = MaterialTheme.typography.titleMedium,
                    color = CornersApartColors.TextOnDarkPrimary,
                )
                if (player.hasPassed) {
                    Text(
                        text = stringResource(R.string.player_passed_suffix),
                        style = MaterialTheme.typography.labelSmall,
                        color = CornersApartColors.TextOnDarkSecondary,
                    )
                }
            }
        }
    }
}

private const val SCORE_COLUMNS = 2
