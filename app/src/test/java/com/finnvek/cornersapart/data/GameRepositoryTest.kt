package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.engine.EngineTestFixtures
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.SavedGameData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryTest {
    @Test
    fun savedGameCanBeSavedAndCleared() =
        runTest {
            val store = InMemoryJsonStateStore(SavedGameData())
            val repository = GameRepository(store)
            val state = EngineTestFixtures.standardState(GameEngine(), randomSeed = 71L)
            val settings = GameSettings(preferredDifficulty = 4, preferredMode = GameMode.SOLO)

            repository.saveGame(state, settings, savedAtEpochMillis = 1234L)

            assertEquals(state, repository.savedGame.first())
            assertEquals(1234L, repository.savedGameData.first().savedAtEpochMillis)
            assertEquals(settings, repository.savedGameData.first().settings)

            repository.clearSavedGame()

            assertNull(repository.savedGame.first())
        }

    @Test
    fun saveGameTakesSnapshotOfMutableGameStateInputs() =
        runTest {
            val repository = GameRepository(InMemoryJsonStateStore(SavedGameData()))
            val baseState = EngineTestFixtures.standardState(GameEngine(), randomSeed = 73L)
            val boardCells = baseState.board.cells.toMutableList()
            val usedPieceIds = mutableSetOf<String>()
            val bonusTiles = mutableListOf(BonusTile(row = 4, col = 4))
            val moveHistory = mutableListOf<Move>()
            val state =
                baseState.copy(
                    board = BoardSnapshot(size = baseState.board.size, cells = boardCells),
                    players =
                        baseState.players.map { player ->
                            if (player.index == 0) player.copy(usedPieceIds = usedPieceIds) else player
                        },
                    bonusTiles = bonusTiles,
                    moveHistory = moveHistory,
                )

            repository.saveGame(state, GameSettings(), savedAtEpochMillis = 4321L)
            boardCells[0] = 99
            usedPieceIds += PieceCatalog.SINGLE_CELL_ID
            bonusTiles.clear()
            moveHistory +=
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                )

            val savedState = checkNotNull(repository.savedGame.first())
            assertEquals(BoardSnapshot.EMPTY, savedState.board.get(row = 0, col = 0))
            assertTrue(savedState.players[0].usedPieceIds.isEmpty())
            assertEquals(listOf(BonusTile(row = 4, col = 4)), savedState.bonusTiles)
            assertTrue(savedState.moveHistory.isEmpty())
        }
}
