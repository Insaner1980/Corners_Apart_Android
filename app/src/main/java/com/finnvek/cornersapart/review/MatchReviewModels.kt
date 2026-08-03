package com.finnvek.cornersapart.review

import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move

sealed interface ReviewAction {
    data class Placement(
        val move: Move,
    ) : ReviewAction

    data class Pass(
        val playerIndex: Int,
        val hadValidMoves: Boolean,
    ) : ReviewAction
}

data class ReviewTimelineStep(
    val stateBefore: GameState,
    val action: ReviewAction,
    val stateAfter: GameState,
)

enum class MoveClassification {
    GREAT,
    GOOD,
    INACCURACY,
    MISTAKE,
}

enum class AssessmentReason {
    SCORE_GAP,
    PASSED_WITH_AVAILABLE_MOVES,
}

data class MoveAssessment(
    val classification: MoveClassification,
    val reason: AssessmentReason,
    val playedTotal: Double?,
    val bestTotal: Double,
    val bestMove: Move,
    val relativeGap: Double,
    val accuracy: Double,
    val claimedBonusTileCount: Int,
)

data class MatchReviewResult(
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val accuracy: Double?,
    val classificationCounts: Map<MoveClassification, Int>,
)

data class MatchReviewProgress(
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val analyzedCount: Int,
    val totalCount: Int,
    val runningAccuracy: Double?,
)

sealed interface MatchReviewUpdate {
    data class Progress(
        val value: MatchReviewProgress,
    ) : MatchReviewUpdate

    data class Completed(
        val result: MatchReviewResult,
    ) : MatchReviewUpdate

    data class Failed(
        val failure: MatchReviewFailure,
    ) : MatchReviewUpdate
}

sealed interface MatchReviewFailure {
    val sourceHistoryIndex: Int?
    val rejectionReason: MoveRejectionReason?

    data class InvalidFinalState(
        override val sourceHistoryIndex: Int? = null,
        override val rejectionReason: MoveRejectionReason? = null,
    ) : MatchReviewFailure

    data class TurnAlignmentMismatch(
        override val sourceHistoryIndex: Int,
        val expectedPlayerIndex: Int,
        val actualPlayerIndex: Int,
        override val rejectionReason: MoveRejectionReason? = null,
    ) : MatchReviewFailure

    data class HistoryMoveRejected(
        override val sourceHistoryIndex: Int,
        override val rejectionReason: MoveRejectionReason,
    ) : MatchReviewFailure

    data class FinalPassMismatch(
        val playerIndex: Int,
        override val sourceHistoryIndex: Int? = null,
        override val rejectionReason: MoveRejectionReason? = null,
    ) : MatchReviewFailure

    data class FinalStateMismatch(
        override val sourceHistoryIndex: Int? = null,
        override val rejectionReason: MoveRejectionReason? = null,
    ) : MatchReviewFailure

    data class UnexpectedAnalysisError(
        val cause: Throwable,
        override val sourceHistoryIndex: Int? = null,
        override val rejectionReason: MoveRejectionReason? = null,
    ) : MatchReviewFailure
}

sealed interface GameReplayResult {
    data class Success(
        val timeline: List<ReviewTimelineStep>,
        val finalState: GameState,
    ) : GameReplayResult

    data class Failed(
        val failure: MatchReviewFailure,
    ) : GameReplayResult
}
