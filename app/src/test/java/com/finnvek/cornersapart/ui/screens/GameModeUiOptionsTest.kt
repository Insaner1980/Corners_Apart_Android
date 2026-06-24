package com.finnvek.cornersapart.ui.screens

import com.finnvek.cornersapart.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GameModeUiOptionsTest {
    @Test
    fun gameModeUiOptionsExposeEveryModeOnce() {
        assertEquals(GameMode.entries, GameModeUiOptions.modes)
    }
}
