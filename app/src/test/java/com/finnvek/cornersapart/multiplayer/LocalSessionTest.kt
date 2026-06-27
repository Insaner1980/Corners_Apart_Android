package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.EngineTestFixtures
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.finnvek.cornersapart.opponents.OpponentDifficulty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalSessionTest {
    @Test
    fun fourPlayerLocalMoveUpdatesAuthoritativeGameState() =
        runTest {
            val session =
                LocalSession(
                    engine = GameEngine(),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 17L,
                            bonusTiles = emptyList(),
                        ),
                )

            val result =
                session.sendMove(
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(
                0,
                session.gameState.value.board
                    .get(row = 0, col = 0),
            )
            assertEquals(1, session.gameState.value.currentPlayerIndex)
            assertEquals(1, session.players.value[0].usedPieceCount)
        }

    @Test
    fun soloGameRunsComputerSlotsBackToHumanPlayer() =
        runTest {
            val session =
                LocalSession(
                    engine = GameEngine(),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.SOLO,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 19L,
                            bonusTiles = emptyList(),
                        ),
                )

            val result =
                session.sendMove(
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 19,
                        anchorCol = 19,
                        orientationIndex = 0,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(0, session.gameState.value.currentPlayerIndex)
            assertTrue(
                session.players.value
                    .drop(1)
                    .all { player -> player.isComputerControlled },
            )
            assertTrue(
                session.gameState.value.players
                    .drop(1)
                    .all { player -> player.usedPieceIds.isNotEmpty() },
            )
        }

    @Test
    fun localSessionRunsAnyComputerControlledCurrentSlotAfterHumanMove() =
        runTest {
            val engine = GameEngine()
            val session =
                LocalSession(
                    engine = engine,
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 21L,
                            bonusTiles = emptyList(),
                        ),
                )
            val mixedSlotState =
                session.gameState.value.copy(
                    players =
                        session.gameState.value.players.map { player ->
                            if (player.index == 1) {
                                player.copy(isComputerControlled = true)
                            } else {
                                player
                            }
                        },
                )
            session.replaceState(mixedSlotState)

            val result =
                session.sendMove(
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(2, session.gameState.value.currentPlayerIndex)
            assertTrue(
                session.gameState.value.players[1]
                    .usedPieceIds
                    .isNotEmpty(),
            )
        }

    @Test
    fun localPassPublishesSameStateAsEnginePass() =
        runTest {
            val engine = GameEngine()
            val config =
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    boardSize = GameConstants.STANDARD_BOARD_SIZE,
                    randomSeed = 25L,
                    bonusTiles = emptyList(),
                )
            val session = LocalSession(engine = engine, initialConfig = config)
            val expectedState = engine.pass(session.gameState.value, playerIndex = 0)

            val result = session.sendPass(playerIndex = 0)

            assertTrue(result.isSuccess)
            assertEquals(expectedState, session.gameState.value)
        }

    @Test
    fun soloSessionDoesNotAcceptConcurrentMovesFromSameStaleTurn() =
        runTest {
            val engine = GameEngine()
            val opponentDispatcher = StandardTestDispatcher(testScheduler)
            val session =
                LocalSession(
                    engine = engine,
                    opponentEngine =
                        ComputerOpponentEngine(
                            gameEngine = engine,
                            dispatcher = opponentDispatcher,
                        ),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.SOLO,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 29L,
                            bonusTiles = emptyList(),
                        ),
                )
            val openingMove =
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 19,
                    anchorCol = 19,
                    orientationIndex = 0,
                )

            val first = async { session.sendMove(openingMove) }
            val second = async { session.sendMove(openingMove) }
            advanceUntilIdle()

            assertEquals(
                1,
                listOf(
                    first.await(),
                    second.await(),
                ).count { result ->
                    result.isSuccess
                },
            )
            assertEquals(
                listOf(openingMove),
                session.gameState.value.moveHistory.filter { move ->
                    move.playerIndex == 0
                },
            )
        }

    @Test
    fun localSessionFactoryCreatesSoloSessionWithPersistedMediumDifficulty() {
        val session =
            LocalSessionFactory(
                engine = GameEngine(),
                opponentEngine =
                    com.finnvek.cornersapart.opponents
                        .ComputerOpponentEngine(),
            ).create(
                initialConfig =
                    GameConfig(
                        mode = GameMode.SOLO,
                        boardSize = GameConstants.STANDARD_BOARD_SIZE,
                        randomSeed = 23L,
                        bonusTiles = emptyList(),
                    ),
                persistedDifficulty = 3,
            )

        assertEquals(OpponentDifficulty.MEDIUM, session.opponentDifficulty)
    }

    @Test
    fun localPassRejectsGameOverStateWithoutPublishingMutation() =
        runTest {
            val engine = GameEngine()
            val session = LocalSession(engine = engine)
            val endedState =
                engine
                    .newGame(
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            randomSeed = 37L,
                            bonusTiles = emptyList(),
                        ),
                    ).copy(
                        currentPlayerIndex = 0,
                        turnNumber = 12,
                        isGameOver = true,
                    )
            session.replaceState(endedState)

            val result = session.sendPass(playerIndex = 0)

            assertFalse(result.isSuccess)
            val error = result.exceptionOrNull()
            assertTrue(error is MoveRejectedException)
            assertEquals(MoveRejectionReason.GAME_OVER, (error as MoveRejectedException).reason)
            assertEquals(endedState, session.gameState.value)
        }

    @Test
    fun localPassRejectsWrongTurnAsTypedNotPlayersTurn() =
        runTest {
            val session =
                LocalSession(
                    engine = GameEngine(),
                    initialConfig =
                        GameConfig(
                            mode = GameMode.FOUR_PLAYER,
                            boardSize = GameConstants.STANDARD_BOARD_SIZE,
                            randomSeed = 38L,
                            bonusTiles = emptyList(),
                        ),
                )

            val result = session.sendPass(playerIndex = 1)

            assertFalse(result.isSuccess)
            val error = result.exceptionOrNull()
            assertTrue(error is MoveRejectedException)
            assertEquals(MoveRejectionReason.NOT_PLAYERS_TURN, (error as MoveRejectedException).reason)
            assertEquals(0, session.gameState.value.currentPlayerIndex)
        }

    @Test
    fun replaceStatePublishesBoardPlayersAndCurrentTurn() {
        val engine = GameEngine()
        val original =
            engine.newGame(
                GameConfig(
                    mode = GameMode.FOUR_PLAYER,
                    randomSeed = 31L,
                    bonusTiles = emptyList(),
                ),
            )
        val replacement =
            engine
                .applyMove(
                    original,
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                ).let { result ->
                    check(result is com.finnvek.cornersapart.engine.MoveResult.Accepted)
                    result.state
                }
        val session = LocalSession(engine = engine)

        session.replaceState(replacement)

        assertEquals(replacement.board, session.gameState.value.board)
        assertEquals(replacement.players, session.gameState.value.players)
        assertEquals(replacement.currentPlayerIndex, session.gameState.value.currentPlayerIndex)
        assertEquals(replacement.players[0].usedPieceIds.size, session.players.value[0].usedPieceCount)
    }

    @Test
    fun replaceStateTakesSnapshotOfMutableGameStateInputs() {
        val engine = GameEngine()
        val session = LocalSession(engine = engine)
        val mutableInput = EngineTestFixtures.mutableSnapshotInput(engine, randomSeed = 39L)

        session.replaceState(mutableInput.state)
        mutableInput.boardCells[0] = 99
        mutableInput.usedPieceIds += PieceCatalog.SINGLE_CELL_ID
        mutableInput.bonusTiles.clear()

        val publishedState = session.gameState.value
        assertEquals(BoardSnapshot.EMPTY, publishedState.board.get(row = 0, col = 0))
        assertTrue(publishedState.players[0].usedPieceIds.isEmpty())
        assertEquals(listOf(BonusTile(row = 4, col = 4)), publishedState.bonusTiles)
    }
}
