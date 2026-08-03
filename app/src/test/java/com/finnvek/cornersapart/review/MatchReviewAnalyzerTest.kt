package com.finnvek.cornersapart.review

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.opponents.MoveEvaluator
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import com.finnvek.cornersapart.opponents.OpponentStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchReviewAnalyzerTest {
    private val engine = GameEngine()

    @Test
    fun scoringClassificationIncludesExactThresholdsAndBothSides() {
        val cases =
            listOf(
                0.0 to MoveClassification.GREAT,
                0.02 to MoveClassification.GREAT,
                0.020001 to MoveClassification.GOOD,
                0.15 to MoveClassification.GOOD,
                0.150001 to MoveClassification.INACCURACY,
                0.40 to MoveClassification.INACCURACY,
                0.400001 to MoveClassification.MISTAKE,
            )

        cases.forEach { (gap, expected) ->
            assertEquals(expected, ReviewScoring.classify(gap))
        }
    }

    @Test
    fun scoringAccuracyHandlesPositiveZeroAndNegativeBestTotals() {
        assertEquals(0.75, ReviewScoring.accuracy(playedTotal = 75.0, bestTotal = 100.0), 0.0)
        assertEquals(1.0, ReviewScoring.accuracy(playedTotal = 0.0, bestTotal = 0.0), 0.0)
        assertEquals(0.5, ReviewScoring.accuracy(playedTotal = -1.0, bestTotal = 0.0), 0.0)
        assertEquals(1.0, ReviewScoring.accuracy(playedTotal = -5.0, bestTotal = -5.0), 0.0)
        assertEquals(0.5, ReviewScoring.accuracy(playedTotal = -6.0, bestTotal = -5.0), 0.0)
    }

    @Test
    fun bestEvaluatedOpeningMoveIsGreat() =
        runTest {
            val initial = newGame()
            val bestMove = bestMove(initial)
            val finalState = finishAfterMove(initial, bestMove)

            val result = analyzer().completedResult(finalState)
            val placementAssessment =
                result.assessmentsByStepIndex.entries
                    .first { (index, _) -> result.timeline[index].action is ReviewAction.Placement }
                    .value

            assertEquals(MoveClassification.GREAT, placementAssessment.classification)
            assertEquals(1.0, placementAssessment.accuracy, 0.0)
            assertEquals(bestMove, placementAssessment.bestMove)
        }

    @Test
    fun deliberatelyWeakMonominoHasProvenLargeGap() =
        runTest {
            val initial = newGame()
            val weakMove =
                engine
                    .getValidMoves(initial, initial.currentPlayerIndex)
                    .first { move -> move.pieceId == PieceCatalog.SINGLE_CELL_ID }
            val evaluator = MoveEvaluator(engine)
            val weakTotal = evaluator.evaluateMaster(initial, weakMove)
            val bestTotal = evaluator.evaluateMaster(initial, bestMove(initial))
            assertTrue(bestTotal - weakTotal > 0.15 * kotlin.math.abs(bestTotal))
            val finalState = finishAfterMove(initial, weakMove)

            val assessment =
                analyzer()
                    .completedResult(finalState)
                    .assessmentsByStepIndex
                    .values
                    .first { value -> value.playedTotal != null }

            assertTrue(
                assessment.classification == MoveClassification.INACCURACY ||
                    assessment.classification == MoveClassification.MISTAKE,
            )
        }

    @Test
    fun voluntaryPassIsMistakeWithZeroAccuracy() =
        runTest {
            var state = newGame()
            assertTrue(engine.hasValidMove(state, playerIndex = 0))
            state = engine.pass(state, playerIndex = 0)
            val finalState = finishByPassing(state)

            val result = analyzer().completedResult(finalState)
            val firstAssessment = result.assessmentsByStepIndex.getValue(0)

            assertEquals(MoveClassification.MISTAKE, firstAssessment.classification)
            assertEquals(AssessmentReason.PASSED_WITH_AVAILABLE_MOVES, firstAssessment.reason)
            assertNull(firstAssessment.playedTotal)
            assertEquals(0.0, firstAssessment.accuracy, 0.0)
            assertNotNull(firstAssessment.bestMove)
        }

    @Test
    fun forcedPassIsNotAssessed() =
        runTest {
            var state =
                newGame().let { created ->
                    created.copy(
                        players =
                            created.players.map { player ->
                                if (player.index == 0) {
                                    player.copy(startCorner = CellPosition(row = -1, col = -1))
                                } else {
                                    player
                                }
                            },
                    )
                }
            assertTrue(!engine.hasValidMove(state, playerIndex = 0))
            state = engine.pass(state, playerIndex = 0)
            val finalState = finishByPassing(state)

            val result = analyzer().completedResult(finalState)

            assertTrue(result.timeline.first().action is ReviewAction.Pass)
            assertTrue(0 !in result.assessmentsByStepIndex)
        }

    @Test
    fun twoColorDuelAssessesBothColorSlotsOwnedByPlayerZero() =
        runTest {
            var state =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.TWO_COLOR_DUEL,
                        randomSeed = 53L,
                        bonusTiles = emptyList(),
                    ),
                )
            state = engine.applyMove(state, bestMove(state)).acceptedState()
            state = engine.pass(state, playerIndex = 1)
            state = engine.applyMove(state, bestMove(state)).acceptedState()
            val finalState = finishByPassing(state)

            val result = analyzer().completedResult(finalState)
            val assessedPlacementPlayers =
                result.assessmentsByStepIndex.keys
                    .mapNotNull { index ->
                        (result.timeline[index].action as? ReviewAction.Placement)?.move?.playerIndex
                    }.toSet()

            assertTrue(0 in assessedPlacementPlayers)
            assertTrue(2 in assessedPlacementPlayers)
        }

    @Test
    fun progressStartsWithTimelineAndGrowsOneAssessmentAtATime() =
        runTest {
            val initial = newGame()
            val finalState = finishAfterMove(initial, bestMove(initial))

            val updates = analyzer().analyze(finalState, reviewedOwnerIndex = 0).toList()
            val progress = updates.filterIsInstance<MatchReviewUpdate.Progress>()
            val completed = updates.filterIsInstance<MatchReviewUpdate.Completed>().single().result

            assertEquals(completed.timeline, progress.first().value.timeline)
            assertEquals(0, progress.first().value.analyzedCount)
            assertTrue(
                progress
                    .first()
                    .value.assessmentsByStepIndex
                    .isEmpty(),
            )
            assertEquals(
                (0..completed.assessmentsByStepIndex.size).toList(),
                progress.map { update -> update.value.analyzedCount },
            )
            progress.zipWithNext().forEach { (before, after) ->
                assertEquals(
                    before.value.assessmentsByStepIndex.size + 1,
                    after.value.assessmentsByStepIndex.size,
                )
            }
        }

    @Test
    fun sameStateProducesIdenticalResults() =
        runTest {
            val initial = newGame()
            val finalState = finishAfterMove(initial, bestMove(initial))
            val analyzer = analyzer()

            val first = analyzer.completedResult(finalState)
            val second = analyzer.completedResult(finalState)

            assertEquals(first, second)
            assertEquals(
                MoveClassification.entries.toSet(),
                first.classificationCounts.keys,
            )
        }

    private fun analyzer(): MatchReviewAnalyzer =
        MatchReviewAnalyzer(
            gameEngine = engine,
            gameReplayer = GameReplayer(engine),
            dispatcher = UnconfinedTestDispatcher(),
        )

    private suspend fun MatchReviewAnalyzer.completedResult(finalState: GameState): MatchReviewResult =
        analyze(finalState = finalState, reviewedOwnerIndex = 0)
            .toList()
            .filterIsInstance<MatchReviewUpdate.Completed>()
            .single()
            .result

    private fun newGame(): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.FOUR_PLAYER,
                randomSeed = 47L,
                bonusTiles = emptyList(),
            ),
        )

    private fun bestMove(state: GameState): Move {
        val evaluator = MoveEvaluator(engine)
        return engine
            .getValidMoves(state, state.currentPlayerIndex)
            .minWith(
                compareByDescending<Move> { move -> evaluator.evaluateMaster(state, move) }
                    .thenBy { move -> move.pieceId }
                    .thenBy { move -> move.orientationIndex }
                    .thenBy { move -> move.anchorRow }
                    .thenBy { move -> move.anchorCol },
            )
    }

    private fun MoveEvaluator.evaluateMaster(
        state: GameState,
        move: Move,
    ): Double =
        evaluate(
            state = state,
            move = move,
            style = OpponentStyle.BLOCKER,
            difficulty = OpponentDifficulty.MASTER,
        ).total

    private fun finishAfterMove(
        initialState: GameState,
        move: Move,
    ): GameState = finishByPassing(engine.applyMove(initialState, move).acceptedState())

    private fun finishByPassing(initialState: GameState): GameState {
        var state = initialState
        repeat(state.players.size) {
            if (!state.isGameOver) {
                state = engine.pass(state, state.currentPlayerIndex)
            }
        }
        check(state.isGameOver)
        return state
    }

    private fun MoveResult.acceptedState(): GameState = (this as MoveResult.Accepted).state
}
