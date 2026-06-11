package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.multiplayer.LocalSession
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateExposesFourPlayerLocalGame() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertEquals(GameMode.FOUR_PLAYER, state.gameMode)
        assertEquals(GameConstants.PLAYER_COUNT, state.players.size)
        assertEquals(PieceCatalog.SINGLE_CELL_ID, state.selectedPieceId)
        assertEquals(GameConstants.PIECE_COUNT, state.pieces.size)
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun placingSelectedPieceUpdatesBoardAndTurn() =
        runTest {
            val viewModel = createViewModel()

            viewModel.placeSelectedAt(row = 0, col = 0)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.board.get(row = 0, col = 0))
            assertEquals(1, state.currentPlayerIndex)
            assertEquals(BoardSnapshot.EMPTY, state.board.get(row = 0, col = 1))
        }

    @Test
    fun rotateSelectedPieceChangesOrientationIndex() {
        val viewModel = createViewModel()

        viewModel.selectPiece(PieceCatalog.THREE_BEND_ID)
        viewModel.rotateSelectedClockwise()

        assertEquals(PieceCatalog.THREE_BEND_ID, viewModel.uiState.value.selectedPieceId)
        assertEquals(1, viewModel.uiState.value.selectedOrientationIndex)
    }

    @Test
    fun soloGameRunsComputerTurnsBackToHumanPlayer() =
        runTest {
            val viewModel = createViewModel()

            viewModel.startSoloGame()
            viewModel.placeSelectedAt(row = 19, col = 19)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(GameMode.SOLO, state.gameMode)
            assertEquals(0, state.currentPlayerIndex)
            assertTrue(state.players.drop(1).all { player -> player.isComputerControlled })
            assertTrue(state.players.drop(1).all { player -> player.totalScore > 0 })
        }

    @Test
    fun startGameSupportsPassAndPlayModes() {
        val viewModel = createViewModel()

        viewModel.startGame(GameMode.TWO_COLOR_DUEL)

        assertEquals(GameMode.TWO_COLOR_DUEL, viewModel.uiState.value.gameMode)
        assertEquals(
            listOf(0, 1, 0, 1),
            viewModel.uiState.value.players
                .map { player -> player.ownerIndex },
        )

        viewModel.startGame(GameMode.COMPACT_DUEL)

        assertEquals(GameMode.COMPACT_DUEL, viewModel.uiState.value.gameMode)
        assertEquals(GameConstants.COMPACT_BOARD_SIZE, viewModel.uiState.value.board.size)

        viewModel.startGame(GameMode.THREE_PLAYER)

        assertEquals(GameMode.THREE_PLAYER, viewModel.uiState.value.gameMode)
        assertEquals(3, viewModel.uiState.value.players.size)

        viewModel.startGame(GameMode.FOUR_PLAYER)

        assertEquals(GameMode.FOUR_PLAYER, viewModel.uiState.value.gameMode)
        assertEquals(GameConstants.PLAYER_COUNT, viewModel.uiState.value.players.size)
    }

    @Test
    fun polishSettingsCanToggleSoundHapticsAndReducedMotion() {
        val viewModel = createViewModel()

        viewModel.setSoundEnabled(false)
        viewModel.setHapticsEnabled(false)
        viewModel.setReducedMotionEnabled(true)

        val state = viewModel.uiState.value
        assertEquals(false, state.soundEnabled)
        assertEquals(false, state.hapticsEnabled)
        assertEquals(true, state.reducedMotionEnabled)
    }

    private fun createViewModel(): GameViewModel {
        val engine = GameEngine()
        return GameViewModel(
            LocalSession(
                engine = engine,
                opponentEngine =
                    ComputerOpponentEngine(
                        gameEngine = engine,
                        dispatcher = mainDispatcherRule.testDispatcher,
                    ),
            ),
        )
    }
}
