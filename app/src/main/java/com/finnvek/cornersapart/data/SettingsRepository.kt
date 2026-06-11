package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val store: JsonStateStore<GameSettings>,
) {
    val settings: Flow<GameSettings> = store.data

    suspend fun updateSettings(transform: suspend (GameSettings) -> GameSettings) {
        store.update(transform)
    }
}
