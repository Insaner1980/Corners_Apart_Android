package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.Profile

/** Omistaja 0 on paikallisissa peleissä laitteen pelaaja, jota aktiivinen profiili edustaa. */
internal const val DEFAULT_PROFILE_OWNER_INDEX = 0

/**
 * Mappaa aktiivisen profiilin nimen ja värivalinnan laitteen pelaajalle
 * paikallisissa peleissä — pelkkä esitystason muunnos, pelitila ei muutu.
 */
internal class ProfileDisplayMapper(
    private val isLocalSession: () -> Boolean,
    private val activeProfile: () -> Profile?,
) {
    fun displayName(
        engineName: String,
        ownerIndex: Int,
        colorIndex: Int,
    ): String {
        if (!isLocalSession()) return engineName
        if (ownerIndex == DEFAULT_PROFILE_OWNER_INDEX) {
            return activeProfile()
                ?.name
                ?.takeIf { name -> name.isNotBlank() }
                ?: engineName
        }
        // Värinvaihdossa mukana ollut värinimetty pelaaja nimetään uuden värinsä mukaan
        if (engineName == GameConstants.PLAYER_NAMES.getOrNull(colorIndex)) {
            return GameConstants.PLAYER_NAMES.getOrNull(visualColorIndex(colorIndex)) ?: engineName
        }
        return engineName
    }

    /**
     * Profiilin väri ja väri 0 vaihtavat paikkaa (bijektio), jotta laitteen
     * pelaaja pelaa valitsemallaan värillä ja kaikki pelivärit pysyvät uniikkeina.
     */
    fun visualColorIndex(colorIndex: Int): Int {
        if (!isLocalSession()) return colorIndex
        val profileColor = activeProfile()?.colorIndex ?: return colorIndex
        return when (colorIndex) {
            DEFAULT_PROFILE_OWNER_INDEX -> profileColor
            profileColor -> DEFAULT_PROFILE_OWNER_INDEX
            else -> colorIndex
        }
    }
}
