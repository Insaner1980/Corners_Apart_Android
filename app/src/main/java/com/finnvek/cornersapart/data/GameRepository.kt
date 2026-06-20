package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.SavedGameData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(
    private val store: JsonStateStore<SavedGameData>,
) {
    val savedGameData: Flow<SavedGameData> = store.data
    val savedGame: Flow<GameState?> = store.data.map { data -> data.gameState }

    suspend fun saveGame(
        state: GameState,
        settings: GameSettings,
        savedAtEpochMillis: Long,
    ) {
        store.update {
            SavedGameData(
                gameState = state,
                savedAtEpochMillis = savedAtEpochMillis,
                settings = settings,
            )
        }
    }

    suspend fun clearSavedGame() {
        store.update { SavedGameData() }
    }
}
