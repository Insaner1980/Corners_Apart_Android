package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.Ruleset
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
                    reducedMotionEnabled = true,
                    preferredMode = GameMode.THREE_PLAYER,
                    preferredRuleset = Ruleset.STANDARD,
                )
            }

            val settings = repository.settings.first()
            assertEquals(3, settings.preferredDifficulty)
            assertFalse(settings.soundEnabled)
            assertFalse(settings.hapticsEnabled)
            assertEquals(true, settings.reducedMotionEnabled)
            assertEquals(GameMode.THREE_PLAYER, settings.preferredMode)
        }
}
