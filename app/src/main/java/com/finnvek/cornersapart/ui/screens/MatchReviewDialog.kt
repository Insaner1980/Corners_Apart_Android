package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.review.MoveAssessment
import com.finnvek.cornersapart.review.MoveClassification
import com.finnvek.cornersapart.review.ReviewAction
import com.finnvek.cornersapart.review.ReviewTimelineStep
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.components.CandyIconButton
import com.finnvek.cornersapart.ui.components.CandyStatusChip
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.util.displayNameResource
import com.finnvek.cornersapart.viewmodel.MatchReviewPhase
import com.finnvek.cornersapart.viewmodel.MatchReviewPlayerUiState
import com.finnvek.cornersapart.viewmodel.MatchReviewUiState
import kotlin.math.roundToInt

@Composable
fun MatchReviewDialog(
    state: MatchReviewUiState,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CandyDialog(
        title = stringResource(R.string.match_review_title),
        onDismiss = onClose,
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onClose,
                style = CandyButtonStyle.Primary,
            )
        },
    ) {
        if (state.phase == MatchReviewPhase.FAILED) {
            Text(
                text = stringResource(R.string.match_review_error),
                style = MaterialTheme.typography.bodyLarge,
                color = CornersApartColors.TextOnDarkSecondary,
            )
            return@CandyDialog
        }

        ReviewProgressAndSummary(state)
        if (state.timeline.isNotEmpty()) {
            ReviewStepContent(
                state = state,
                onStepBack = onStepBack,
                onStepForward = onStepForward,
                onJumpTo = onJumpTo,
            )
        }
    }
}

@Composable
private fun ReviewProgressAndSummary(state: MatchReviewUiState) {
    if (state.phase == MatchReviewPhase.ANALYZING) {
        Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            Text(
                text = stringResource(R.string.match_review_analyzing),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = {
                    if (state.totalCount == 0) 0f else state.analyzedCount.toFloat() / state.totalCount
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text =
                    stringResource(
                        R.string.match_review_progress,
                        state.analyzedCount,
                        state.totalCount,
                    ),
                color = CornersApartColors.TextOnDarkSecondary,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            Text(
                text =
                    state.accuracy
                        ?.let { accuracy ->
                            stringResource(R.string.match_review_accuracy, (accuracy * 100).roundToInt())
                        } ?: stringResource(R.string.match_review_no_moves),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
            ) {
                state.classificationCounts
                    .filterValues { count -> count > 0 }
                    .forEach { (classification, count) ->
                        Text(
                            text = pluralStringResource(classification.countResource(), count, count),
                            color = CornersApartColors.TextOnDarkSecondary,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ReviewStepContent(
    state: MatchReviewUiState,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpTo: (Int) -> Unit,
) {
    val stepIndex = state.currentStepIndex.coerceIn(0, state.timeline.lastIndex)
    val step = state.timeline[stepIndex]
    val assessment = state.assessmentsByStepIndex[stepIndex]
    val player = state.players.playerFor(step)
    val actionText = step.action.description()
    var showBestMove by remember(stepIndex) { mutableStateOf(false) }
    LaunchedEffect(stepIndex) {
        showBestMove = false
    }
    val boardDescription =
        if (showBestMove && assessment != null) {
            stringResource(
                R.string.match_review_board_description_best,
                stepIndex + 1,
                state.timeline.size,
                player.name,
                actionText,
            )
        } else {
            stringResource(
                R.string.match_review_board_description,
                stepIndex + 1,
                state.timeline.size,
                player.name,
                actionText,
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Text(
            text = stringResource(R.string.match_review_step, stepIndex + 1, state.timeline.size),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.match_review_action, player.name, actionText),
            color = CornersApartColors.TextOnDarkSecondary,
        )
        ReviewAssessmentStatus(state, step, assessment)
        assessment
            ?.takeIf { value -> value.claimedBonusTileCount > 0 }
            ?.let { value ->
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.match_review_bonus_claims,
                            value.claimedBonusTileCount,
                            value.claimedBonusTileCount,
                        ),
                )
            }
        ReviewBoard(
            step = step,
            players = state.players,
            assessment = assessment,
            showBestMove = showBestMove,
            contentDescription = boardDescription,
        )
        if (assessment != null) {
            CandyButton(
                text =
                    stringResource(
                        if (showBestMove) {
                            R.string.match_review_hide_best_move
                        } else {
                            R.string.match_review_show_best_move
                        },
                    ),
                onClick = { showBestMove = !showBestMove },
                modifier = Modifier.fillMaxWidth(),
                style = CandyButtonStyle.Neutral,
            )
        }
        ReviewNavigation(
            currentIndex = stepIndex,
            lastIndex = state.timeline.lastIndex,
            onStepBack = onStepBack,
            onStepForward = onStepForward,
            onJumpTo = onJumpTo,
        )
    }
}

@Composable
private fun ReviewAssessmentStatus(
    state: MatchReviewUiState,
    step: ReviewTimelineStep,
    assessment: MoveAssessment?,
) {
    if (assessment != null) {
        val colors = assessment.classification.statusColors()
        CandyStatusChip(
            label = stringResource(assessment.classification.labelResource()),
            face = colors.first,
            bevel = colors.second,
        )
        return
    }
    val playerIndex = step.action.playerIndex()
    val ownedByReviewedPlayer = step.stateBefore.players[playerIndex].ownerIndex == REVIEWED_OWNER_INDEX
    if (state.phase == MatchReviewPhase.ANALYZING && ownedByReviewedPlayer) {
        Text(
            text = stringResource(R.string.match_review_analyzing_move),
            color = CornersApartColors.TextOnDarkSecondary,
        )
    }
}

@Composable
private fun ReviewNavigation(
    currentIndex: Int,
    lastIndex: Int,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onJumpTo: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReviewNavigationButton(
            iconResource = R.drawable.ic_skip_next_24,
            contentDescription = stringResource(R.string.match_review_first_step),
            mirrored = true,
            enabled = currentIndex > 0,
            onClick = { onJumpTo(0) },
        )
        ReviewNavigationButton(
            iconResource = R.drawable.ic_chevron_right_24,
            contentDescription = stringResource(R.string.match_review_previous_step),
            mirrored = true,
            enabled = currentIndex > 0,
            onClick = onStepBack,
        )
        ReviewNavigationButton(
            iconResource = R.drawable.ic_chevron_right_24,
            contentDescription = stringResource(R.string.match_review_next_step),
            enabled = currentIndex < lastIndex,
            onClick = onStepForward,
        )
        ReviewNavigationButton(
            iconResource = R.drawable.ic_skip_next_24,
            contentDescription = stringResource(R.string.match_review_last_step),
            enabled = currentIndex < lastIndex,
            onClick = { onJumpTo(lastIndex) },
        )
    }
}

@Composable
private fun ReviewNavigationButton(
    iconResource: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    mirrored: Boolean = false,
) {
    CandyIconButton(
        contentDescription = contentDescription,
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(iconResource),
            contentDescription = null,
            modifier = Modifier.graphicsLayer { scaleX = if (mirrored) -1f else 1f },
        )
    }
}

private fun List<MatchReviewPlayerUiState>.playerFor(step: ReviewTimelineStep): MatchReviewPlayerUiState {
    val playerIndex = step.action.playerIndex()
    return firstOrNull { player -> player.index == playerIndex }
        ?: MatchReviewPlayerUiState(playerIndex, step.stateBefore.players[playerIndex].name, playerIndex)
}

private fun ReviewAction.playerIndex(): Int =
    when (this) {
        is ReviewAction.Placement -> move.playerIndex
        is ReviewAction.Pass -> playerIndex
    }

@Composable
private fun ReviewAction.description(): String =
    when (this) {
        is ReviewAction.Placement -> {
            stringResource(
                R.string.match_review_placement,
                stringResource(PieceCatalog.require(move.pieceId).displayNameResource()),
            )
        }

        is ReviewAction.Pass -> {
            stringResource(R.string.match_review_pass)
        }
    }

private fun MoveClassification.labelResource(): Int =
    when (this) {
        MoveClassification.GREAT -> R.string.match_review_great
        MoveClassification.GOOD -> R.string.match_review_good
        MoveClassification.INACCURACY -> R.string.match_review_inaccuracy
        MoveClassification.MISTAKE -> R.string.match_review_mistake
    }

private fun MoveClassification.countResource(): Int =
    when (this) {
        MoveClassification.GREAT -> R.plurals.match_review_great_count
        MoveClassification.GOOD -> R.plurals.match_review_good_count
        MoveClassification.INACCURACY -> R.plurals.match_review_inaccuracy_count
        MoveClassification.MISTAKE -> R.plurals.match_review_mistake_count
    }

private fun MoveClassification.statusColors() =
    when (this) {
        MoveClassification.GREAT -> {
            CornersApartColors.ReviewGreatFace to CornersApartColors.ReviewGreatBevel
        }

        MoveClassification.GOOD -> {
            CornersApartColors.ReviewGoodFace to CornersApartColors.ReviewGoodBevel
        }

        MoveClassification.INACCURACY -> {
            CornersApartColors.ReviewInaccuracyFace to CornersApartColors.ReviewInaccuracyBevel
        }

        MoveClassification.MISTAKE -> {
            CornersApartColors.ReviewMistakeFace to CornersApartColors.ReviewMistakeBevel
        }
    }

private const val REVIEWED_OWNER_INDEX = 0
