package com.finnvek.cornersapart.data

import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.INVALID_GAME_STATE_INDEX_DOMAINS
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.model.hasValidIndexDomains
import com.finnvek.cornersapart.model.toSnapshotCopy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(
    private val store: JsonStateStore<SavedGameData>,
) {
    val savedGameData: Flow<SavedGameData> = store.data.map { data -> data.toSnapshotCopy() }
    val savedGame: Flow<GameState?> = savedGameData.map { data -> data.gameState }

    suspend fun saveGame(
        state: GameState,
        settings: GameSettings,
        savedAtEpochMillis: Long,
    ) {
        require(state.hasValidIndexDomains()) { INVALID_GAME_STATE_INDEX_DOMAINS }
        store.update {
            SavedGameData(
                gameState = state.toSnapshotCopy(),
                savedAtEpochMillis = savedAtEpochMillis,
                settings = settings,
            ).toSnapshotCopy()
        }
    }

    suspend fun clearSavedGame() {
        store.update { SavedGameData() }
    }
}
