package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.PieceCatalog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputerOpponentEngineTest {
    private val engine = GameEngine()
    private val opponentEngine = ComputerOpponentEngine(gameEngine = engine)

    @Test
    fun sameSeedProducesSameDecision() =
        runTest {
            val state = soloState(seed = 47L).copy(currentPlayerIndex = 1)

            val first =
                opponentEngine.chooseAction(
                    state = state,
                    playerIndex = 1,
                    style = OpponentStyle.EXPANSIONIST,
                    difficulty = OpponentDifficulty.MEDIUM,
                )
            val second =
                opponentEngine.chooseAction(
                    state = state,
                    playerIndex = 1,
                    style = OpponentStyle.EXPANSIONIST,
                    difficulty = OpponentDifficulty.MEDIUM,
                )

            assertEquals(first, second)
        }

    @Test
    fun decisionUsesFullDeterministicCandidateSetEvenWhenFirstEvaluationIsSlow() =
        runTest {
            val state = soloState(seed = 61L).copy(currentPlayerIndex = 1)
            val legalMoves = engine.getValidMoves(state, playerIndex = 1)
            assertTrue(legalMoves.size >= 2)
            val slowMove = legalMoves[0]
            val betterMove = legalMoves[1]
            val slowCandidate = slowMove.toCandidate()
            val betterCandidate = betterMove.toCandidate()
            val moveGenerator =
                mockk<MoveGenerator> {
                    every {
                        generateMoves(state, playerIndex = 1, difficulty = OpponentDifficulty.BEGINNER)
                    } returns listOf(slowCandidate, betterCandidate)
                }
            val moveEvaluator =
                mockk<MoveEvaluator> {
                    every {
                        evaluate(state, slowCandidate, OpponentStyle.BLOCKER, OpponentDifficulty.BEGINNER)
                    } answers {
                        Thread.sleep(SLOW_EVALUATION_MILLIS)
                        MoveEvaluation(
                            placedCellScore = 1.0,
                            bonusScore = 0.0,
                            spreadScore = 0.0,
                            centerScore = 0.0,
                            blockingScore = 0.0,
                        )
                    }
                    every {
                        evaluate(state, betterCandidate, OpponentStyle.BLOCKER, OpponentDifficulty.BEGINNER)
                    } returns
                        MoveEvaluation(
                            placedCellScore = 1_000.0,
                            bonusScore = 0.0,
                            spreadScore = 0.0,
                            centerScore = 0.0,
                            blockingScore = 0.0,
                        )
                }
            val deterministicOpponent =
                ComputerOpponentEngine(
                    gameEngine = engine,
                    moveGenerator = moveGenerator,
                    moveEvaluator = moveEvaluator,
                )

            val action =
                deterministicOpponent.chooseAction(
                    state = state,
                    playerIndex = 1,
                    style = OpponentStyle.BLOCKER,
                    difficulty = OpponentDifficulty.BEGINNER,
                )

            assertEquals(OpponentAction.PlaceMove(betterMove), action)
        }

    @Test
    fun everyDifficultyReturnsLegalMove() =
        runTest {
            val state = soloState(seed = 53L).copy(currentPlayerIndex = 1)

            OpponentDifficulty.entries.forEach { difficulty ->
                val action =
                    opponentEngine.chooseAction(
                        state = state,
                        playerIndex = 1,
                        style = OpponentStyle.BLOCKER,
                        difficulty = difficulty,
                    )

                assertTrue(action is OpponentAction.PlaceMove)
                assertTrue(engine.applyMove(state, (action as OpponentAction.PlaceMove).move) is MoveResult.Accepted)
            }
        }

    @Test
    fun returnsPassWhenNoLegalMoveExists() =
        runTest {
            val base = soloState(seed = 59L).copy(currentPlayerIndex = 1)
            val state =
                base.copy(
                    players =
                        base.players.map { player ->
                            if (player.index == 1) {
                                player.copy(usedPieceIds = PieceCatalog.all.map { piece -> piece.id }.toSet())
                            } else {
                                player
                            }
                        },
                )

            val action =
                opponentEngine.chooseAction(
                    state = state,
                    playerIndex = 1,
                    style = OpponentStyle.OPPORTUNIST,
                    difficulty = OpponentDifficulty.EXPERT,
                )

            assertEquals(OpponentAction.Pass(playerIndex = 1), action)
        }

    private fun soloState(seed: Long): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.SOLO,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = seed,
                bonusTiles = emptyList(),
            ),
        )

    private fun com.finnvek.cornersapart.model.Move.toCandidate(): MoveCandidate =
        MoveCandidate(
            move = this,
            placedCellCount = PieceCatalog.require(pieceId).cells.size,
            claimedBonusTileCount = 0,
        )

    private companion object {
        const val SLOW_EVALUATION_MILLIS = 300L
    }
}
