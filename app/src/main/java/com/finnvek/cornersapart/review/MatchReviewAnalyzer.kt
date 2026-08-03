package com.finnvek.cornersapart.review

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.opponents.MoveEvaluation
import com.finnvek.cornersapart.opponents.MoveEvaluator
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import com.finnvek.cornersapart.opponents.OpponentStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.abs
import kotlin.math.max

class MatchReviewAnalyzer(
    private val gameEngine: GameEngine,
    private val gameReplayer: GameReplayer,
    private val dispatcher: CoroutineDispatcher,
) {
    @Suppress("TooGenericExceptionCaught")
    fun analyze(
        finalState: GameState,
        reviewedOwnerIndex: Int,
    ): Flow<MatchReviewUpdate> =
        flow {
            try {
                val replay =
                    when (val result = gameReplayer.replay(finalState)) {
                        is GameReplayResult.Success -> {
                            result
                        }

                        is GameReplayResult.Failed -> {
                            emit(MatchReviewUpdate.Failed(result.failure))
                            return@flow
                        }
                    }
                emitAnalysis(replay, reviewedOwnerIndex)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                emit(
                    MatchReviewUpdate.Failed(
                        MatchReviewFailure.UnexpectedAnalysisError(cause = error),
                    ),
                )
            }
        }.flowOn(dispatcher)

    private suspend fun FlowCollector<MatchReviewUpdate>.emitAnalysis(
        replay: GameReplayResult.Success,
        reviewedOwnerIndex: Int,
    ) {
        val evaluator = MoveEvaluator(gameEngine)
        val assessableStepIndexes =
            replay.timeline.indices.filter { stepIndex ->
                replay.timeline[stepIndex].isAssessableFor(reviewedOwnerIndex)
            }
        val assessments = linkedMapOf<Int, MoveAssessment>()
        var accuracySum = 0.0
        emit(progress(replay.timeline, assessments, assessableStepIndexes.size, accuracySum))

        assessableStepIndexes.forEach { stepIndex ->
            currentCoroutineContext().ensureActive()
            val assessment = assess(replay.timeline[stepIndex], evaluator)
            assessments[stepIndex] = assessment
            accuracySum += assessment.accuracy
            emit(progress(replay.timeline, assessments, assessableStepIndexes.size, accuracySum))
        }

        emit(
            MatchReviewUpdate.Completed(
                MatchReviewResult(
                    timeline = replay.timeline,
                    assessmentsByStepIndex = assessments.toMap(),
                    accuracy = assessments.takeIf { it.isNotEmpty() }?.let { accuracySum / it.size },
                    classificationCounts =
                        MoveClassification.entries.associateWith { classification ->
                            assessments.values.count { assessment ->
                                assessment.classification == classification
                            }
                        },
                ),
            ),
        )
    }

    private fun progress(
        timeline: List<ReviewTimelineStep>,
        assessments: Map<Int, MoveAssessment>,
        totalCount: Int,
        accuracySum: Double,
    ): MatchReviewUpdate.Progress =
        MatchReviewUpdate.Progress(
            MatchReviewProgress(
                timeline = timeline,
                assessmentsByStepIndex = assessments.toMap(),
                analyzedCount = assessments.size,
                totalCount = totalCount,
                runningAccuracy =
                    assessments.takeIf { it.isNotEmpty() }?.let { accuracySum / it.size },
            ),
        )

    private fun ReviewTimelineStep.isAssessableFor(reviewedOwnerIndex: Int): Boolean {
        val playerIndex =
            when (val reviewAction = action) {
                is ReviewAction.Placement -> reviewAction.move.playerIndex
                is ReviewAction.Pass -> reviewAction.playerIndex
            }
        val ownedByReviewedPlayer = stateBefore.players[playerIndex].ownerIndex == reviewedOwnerIndex
        val actionCanBeAssessed = action !is ReviewAction.Pass || action.hadValidMoves
        return ownedByReviewedPlayer && actionCanBeAssessed
    }

    private fun assess(
        step: ReviewTimelineStep,
        evaluator: MoveEvaluator,
    ): MoveAssessment =
        when (val action = step.action) {
            is ReviewAction.Placement -> assessPlacement(step, action.move, evaluator)
            is ReviewAction.Pass -> assessVoluntaryPass(step, action.playerIndex, evaluator)
        }

    private fun assessPlacement(
        step: ReviewTimelineStep,
        playedMove: Move,
        evaluator: MoveEvaluator,
    ): MoveAssessment {
        val best = bestMove(step.stateBefore, playedMove.playerIndex, evaluator)
        val playedTotal = evaluator.evaluateReference(step.stateBefore, playedMove).total
        val gap = max(best.evaluation.total - playedTotal, 0.0)
        val relativeGap = gap / max(abs(best.evaluation.total), 1.0)
        return MoveAssessment(
            classification = ReviewScoring.classify(relativeGap),
            reason = AssessmentReason.SCORE_GAP,
            playedTotal = playedTotal,
            bestTotal = best.evaluation.total,
            bestMove = best.move,
            relativeGap = relativeGap,
            accuracy = ReviewScoring.accuracy(playedTotal, best.evaluation.total),
            claimedBonusTileCount =
                gameEngine.previewPlacement(step.stateBefore, playedMove).claimedBonusTileCount,
        )
    }

    private fun assessVoluntaryPass(
        step: ReviewTimelineStep,
        playerIndex: Int,
        evaluator: MoveEvaluator,
    ): MoveAssessment {
        val best = bestMove(step.stateBefore, playerIndex, evaluator)
        return MoveAssessment(
            classification = MoveClassification.MISTAKE,
            reason = AssessmentReason.PASSED_WITH_AVAILABLE_MOVES,
            playedTotal = null,
            bestTotal = best.evaluation.total,
            bestMove = best.move,
            relativeGap = 1.0,
            accuracy = 0.0,
            claimedBonusTileCount = 0,
        )
    }

    private fun bestMove(
        state: GameState,
        playerIndex: Int,
        evaluator: MoveEvaluator,
    ): EvaluatedMove =
        gameEngine
            .getValidMoves(state, playerIndex)
            .map { move -> EvaluatedMove(move, evaluator.evaluateReference(state, move)) }
            .minWithOrNull(EVALUATED_MOVE_COMPARATOR)
            ?: error("Assessable action must have at least one valid move.")

    private fun MoveEvaluator.evaluateReference(
        state: GameState,
        move: Move,
    ): MoveEvaluation =
        evaluate(
            state = state,
            move = move,
            style = OpponentStyle.BLOCKER,
            difficulty = OpponentDifficulty.MASTER,
        )

    private data class EvaluatedMove(
        val move: Move,
        val evaluation: MoveEvaluation,
    )

    private companion object {
        val EVALUATED_MOVE_COMPARATOR =
            compareByDescending<EvaluatedMove> { evaluated -> evaluated.evaluation.total }
                .thenBy { evaluated -> evaluated.move.pieceId }
                .thenBy { evaluated -> evaluated.move.orientationIndex }
                .thenBy { evaluated -> evaluated.move.anchorRow }
                .thenBy { evaluated -> evaluated.move.anchorCol }
    }
}

object ReviewScoring {
    fun classify(relativeGap: Double): MoveClassification =
        when {
            relativeGap <= GREAT_MAX_GAP -> MoveClassification.GREAT
            relativeGap <= GOOD_MAX_GAP -> MoveClassification.GOOD
            relativeGap <= INACCURACY_MAX_GAP -> MoveClassification.INACCURACY
            else -> MoveClassification.MISTAKE
        }

    fun accuracy(
        playedTotal: Double,
        bestTotal: Double,
    ): Double =
        when {
            bestTotal > 0.0 -> (playedTotal / bestTotal).coerceIn(0.0, 1.0)
            abs(bestTotal - playedTotal) <= SCORE_EPSILON -> 1.0
            else -> 0.5
        }

    private const val GREAT_MAX_GAP = 0.02
    private const val GOOD_MAX_GAP = 0.15
    private const val INACCURACY_MAX_GAP = 0.40
    private const val SCORE_EPSILON = 1e-9
}
