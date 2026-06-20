package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.engine.EngineTestFixtures
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.SavedGameData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
