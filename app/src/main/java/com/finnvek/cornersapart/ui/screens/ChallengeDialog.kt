package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.ChallengeLevels
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyChip
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing

/** Muodostaa tähtirivin, esim. 2/3 -> "★★☆". */
fun starsLabel(stars: Int): String =
    STAR_FILLED.repeat(stars.coerceIn(0, ChallengeLevels.MAX_STARS)) +
        STAR_EMPTY.repeat(ChallengeLevels.MAX_STARS - stars.coerceIn(0, ChallengeLevels.MAX_STARS))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeDialog(
    challengeStars: Map<Int, Int>,
    onStartLevel: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CandyDialog(
        title = stringResource(R.string.challenge_title),
        onDismiss = onDismiss,
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Neutral,
            )
        },
    ) {
        val totalStars = challengeStars.values.sum()
        Text(
            text = "$totalStars / ${ChallengeLevels.LEVEL_COUNT * ChallengeLevels.MAX_STARS} $STAR_FILLED",
            style = MaterialTheme.typography.titleMedium,
            color = CornersApartColors.BonusAccentBright,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            ChallengeLevels.all.forEach { level ->
                val stars = challengeStars[level.number] ?: 0
                val unlocked = ChallengeLevels.isUnlocked(level.number, challengeStars)
                val description =
                    if (unlocked) {
                        stringResource(R.string.challenge_level_content_description, level.number, stars)
                    } else {
                        stringResource(R.string.challenge_locked_content_description, level.number)
                    }
                CandyChip(
                    label =
                        if (stars > 0) {
                            "${level.number} ${starsLabel(stars)}"
                        } else {
                            "${level.number}"
                        },
                    selected = stars > 0,
                    onClick = {
                        if (unlocked) {
                            onStartLevel(level.number)
                            onDismiss()
                        }
                    },
                    modifier =
                        Modifier
                            .alpha(if (unlocked) 1f else CornersApartAlpha.DisabledCandy)
                            .semantics { contentDescription = description },
                )
            }
        }
    }
}

private const val STAR_FILLED = "★"
private const val STAR_EMPTY = "☆"
