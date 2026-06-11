package com.finnvek.cornersapart.data

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
        savedAtEpochMillis: Long,
    ) {
        store.update {
            SavedGameData(
                gameState = state,
                savedAtEpochMillis = savedAtEpochMillis,
            )
        }
    }

    suspend fun clearSavedGame() {
        store.update { SavedGameData() }
    }
}
