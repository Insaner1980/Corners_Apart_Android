package com.finnvek.cornersapart.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedMixerTest {
    @Test
    fun saltedIndexIsStableAndNonNegative() {
        assertEquals(1, SeedMixer.index(seed = 42L, salt = 0x6A09E667F3BCC909L, bound = 3))
        assertEquals(1, SeedMixer.index(seed = -42L, salt = 0x3C6EF372FE94F82BL, bound = 4))
    }

    @Test
    fun unitIntervalKeepsMixedSeedInsideUnitRange() {
        assertEquals(0.7966119150137317, SeedMixer.unitInterval(47L), 0.0)
    }
}
