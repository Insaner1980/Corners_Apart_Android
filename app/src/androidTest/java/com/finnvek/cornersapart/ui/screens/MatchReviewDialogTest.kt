package com.finnvek.cornersapart.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.review.AssessmentReason
import com.finnvek.cornersapart.review.MatchReviewFailure
import com.finnvek.cornersapart.review.MoveAssessment
import com.finnvek.cornersapart.review.MoveClassification
import com.finnvek.cornersapart.review.ReviewAction
import com.finnvek.cornersapart.review.ReviewTimelineStep
import com.finnvek.cornersapart.testing.ComposeTestActivity
import com.finnvek.cornersapart.ui.theme.CornersApartTheme
import com.finnvek.cornersapart.viewmodel.MatchReviewPhase
import com.finnvek.cornersapart.viewmodel.MatchReviewPlayerUiState
import com.finnvek.cornersapart.viewmodel.MatchReviewUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchReviewDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun analyzingStateShowsProgressAndBrowsableTimeline() {
        val state = reviewState(phase = MatchReviewPhase.ANALYZING)

        setReviewContent(state)

        composeRule.onNodeWithText("Analyzing match").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2 moves analyzed").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1 of 1").assertIsDisplayed()
        composeRule.onNodeWithText("Analyzing this move").assertIsDisplayed()
    }

    @Test
    fun completedStateShowsAccuracyClassificationAndBoundaryControls() {
        val state = reviewState(phase = MatchReviewPhase.COMPLETE)

        setReviewContent(state)

        composeRule.onNodeWithText("Accuracy: 92%").assertIsDisplayed()
        composeRule.onNodeWithText("Great").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("First step").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Previous step").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Next step").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Last step").assertIsNotEnabled()
    }

    @Test
    fun bestMoveToggleChangesLabelAndBoardDescription() {
        val state = reviewState(phase = MatchReviewPhase.COMPLETE)

        setReviewContent(state)

        composeRule.onNodeWithText("Show best move").performClick()
        composeRule.onNodeWithText("Hide best move").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Review board, step 1 of 1, Player placed Five Bar, showing best move")
            .assertIsDisplayed()
    }

    @Test
    fun navigationButtonsSendExpectedIndexes() {
        val base = reviewState(phase = MatchReviewPhase.COMPLETE)
        val state =
            base.copy(
                timeline = base.timeline + base.timeline,
                currentStepIndex = 1,
            )
        var jumpedTo: Int? = null
        var backCount = 0
        composeRule.setContent {
            CornersApartTheme {
                MatchReviewDialog(
                    state = state,
                    onStepBack = { backCount += 1 },
                    onStepForward = {},
                    onJumpTo = { index -> jumpedTo = index },
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("First step").assertIsEnabled().performClick()
        assertEquals(0, jumpedTo)
        composeRule.onNodeWithContentDescription("Previous step").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun failureStateShowsOnlyGeneralFailureAndCanClose() {
        var closeCount = 0
        val state =
            reviewState(phase = MatchReviewPhase.FAILED).copy(
                timeline = emptyList(),
                assessmentsByStepIndex = emptyMap(),
                failure = MatchReviewFailure.FinalStateMismatch(),
            )
        composeRule.setContent {
            CornersApartTheme {
                MatchReviewDialog(
                    state = state,
                    onStepBack = {},
                    onStepForward = {},
                    onJumpTo = {},
                    onClose = { closeCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Match review could not be completed.").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        assertEquals(1, closeCount)
    }

    private fun setReviewContent(state: MatchReviewUiState) {
        composeRule.setContent {
            CornersApartTheme {
                MatchReviewDialog(
                    state = state,
                    onStepBack = {},
                    onStepForward = {},
                    onJumpTo = {},
                    onClose = {},
                )
            }
        }
    }

    private fun reviewState(phase: MatchReviewPhase): MatchReviewUiState {
        val engine = GameEngine()
        val before =
            engine.newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 71L,
                    bonusTiles = emptyList(),
                ),
            )
        val move =
            engine
                .getValidMoves(before, playerIndex = 0)
                .first { candidate -> candidate.pieceId == "five-bar" }
        val after = (engine.applyMove(before, move) as MoveResult.Accepted).state
        val step =
            ReviewTimelineStep(
                stateBefore = before,
                action = ReviewAction.Placement(move),
                stateAfter = after,
            )
        val assessment =
            MoveAssessment(
                classification = MoveClassification.GREAT,
                reason = AssessmentReason.SCORE_GAP,
                playedTotal = 100.0,
                bestTotal = 100.0,
                bestMove = move,
                relativeGap = 0.0,
                accuracy = 0.92,
                claimedBonusTileCount = 0,
            )
        return MatchReviewUiState(
            phase = phase,
            players =
                before.players.map { player ->
                    MatchReviewPlayerUiState(
                        index = player.index,
                        name = if (player.index == 0) "Player" else player.name,
                        colorIndex = player.colorIndex,
                    )
                },
            timeline = listOf(step),
            assessmentsByStepIndex =
                if (phase == MatchReviewPhase.COMPLETE) mapOf(0 to assessment) else emptyMap(),
            analyzedCount = 1,
            totalCount = 2,
            currentStepIndex = 0,
            accuracy = if (phase == MatchReviewPhase.COMPLETE) 0.92 else null,
            classificationCounts =
                MoveClassification.entries.associateWith { classification ->
                    if (classification == MoveClassification.GREAT) 1 else 0
                },
        )
    }
}
