package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.PieceCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

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
    fun everyDifficultyReturnsLegalMoveWithinReasonableBudget() =
        runTest {
            val state = soloState(seed = 53L).copy(currentPlayerIndex = 1)

            OpponentDifficulty.entries.forEach { difficulty ->
                val mark = TimeSource.Monotonic.markNow()
                val action =
                    opponentEngine.chooseAction(
                        state = state,
                        playerIndex = 1,
                        style = OpponentStyle.BLOCKER,
                        difficulty = difficulty,
                    )
                val elapsed = mark.elapsedNow()

                assertTrue(elapsed < difficulty.timeBudget + 250.milliseconds)
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
}
