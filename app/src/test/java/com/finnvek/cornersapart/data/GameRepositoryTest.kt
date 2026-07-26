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

            assertEquals(state, repository.savedGameData.first().gameState)
            assertEquals(1234L, repository.savedGameData.first().savedAtEpochMillis)
            assertEquals(settings, repository.savedGameData.first().settings)

            repository.clearSavedGame()

            assertNull(repository.savedGameData.first().gameState)
        }

    @Test
    fun saveGameTakesSnapshotOfMutableGameStateInputs() =
        runTest {
            val repository = GameRepository(InMemoryJsonStateStore(SavedGameData()))
            val mutableInput = EngineTestFixtures.mutableSnapshotInput(GameEngine(), randomSeed = 73L)

            repository.saveGame(mutableInput.state, GameSettings(), savedAtEpochMillis = 4321L)
            mutableInput.boardCells[0] = 99
            mutableInput.usedPieceIds += PieceCatalog.SINGLE_CELL_ID
            mutableInput.bonusTiles.clear()
            mutableInput.moveHistory +=
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 0,
                    orientationIndex = 0,
                )

            val savedState = checkNotNull(repository.savedGameData.first().gameState)
            assertEquals(BoardSnapshot.EMPTY, savedState.board.get(row = 0, col = 0))
            assertTrue(savedState.players[0].usedPieceIds.isEmpty())
            assertEquals(listOf(BonusTile(row = 4, col = 4)), savedState.bonusTiles)
            assertTrue(savedState.moveHistory.isEmpty())
        }
}
