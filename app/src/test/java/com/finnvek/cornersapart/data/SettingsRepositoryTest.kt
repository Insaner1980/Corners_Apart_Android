package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun settingsRepositoryUpdatesDifficultySoundHapticsAndPreferredMode() =
        runTest {
            val repository = SettingsRepository(InMemoryJsonStateStore(GameSettings()))

            repository.updateSettings {
                it.copy(
                    preferredDifficulty = 3,
                    soundEnabled = false,
                    hapticsEnabled = false,
                    preferredMode = GameMode.THREE_PLAYER,
                )
            }

            val settings = repository.settings.first()
            assertEquals(3, settings.preferredDifficulty)
            assertFalse(settings.soundEnabled)
            assertFalse(settings.hapticsEnabled)
            assertEquals(GameMode.THREE_PLAYER, settings.preferredMode)
        }
}
