package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PieceDef
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.multiplayer.SessionType
import com.finnvek.cornersapart.opponents.OpponentStyle

data class GameUiState(
    val gameMode: GameMode,
    val board: BoardSnapshot,
    val bonusTiles: List<BonusTile>,
    val players: List<PlayerUiState>,
    val currentPlayerIndex: Int,
    val selectedPieceId: String,
    val selectedOrientationIndex: Int,
    val selectedCells: List<CellOffset>,
    val pieces: List<PiecePanelItem>,
    val isGameOver: Boolean,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val gameDurationSeconds: Int = 0,
    val preferredDifficulty: Int = 3,
    val preferredMode: GameMode = GameModeConfigs.defaultMode,
    val history: List<HistoryEntry> = emptyList(),
    val activeProfileName: String = "Player",
    val hasSavedGame: Boolean = false,
    val resumeSummary: ResumeGameSummary? = null,
    val rankedScores: List<PlayerScore> = emptyList(),
    val sessionType: SessionType = SessionType.LOCAL,
    val nearbyState: NearbyUiState = NearbyUiState(),
    val profiles: List<ProfileUiState> = emptyList(),
    val activeChallengeLevel: Int? = null,
    val challengeStars: Map<Int, Int> = emptyMap(),
    val challengeResult: ChallengeResult? = null,
    val isNewBestScore: Boolean = false,
    val newAchievements: List<String> = emptyList(),
    val unlockedAchievements: Set<String> = emptySet(),
    val isDailyChallenge: Boolean = false,
    val dailyBestScore: Int? = null,
    val rivals: List<RivalUiState> = emptyList(),
    val activeRivalId: String? = null,
    val rivalResult: RivalMatchResult? = null,
) {
    val currentPlayer: PlayerUiState
        get() = players[currentPlayerIndex]
}

data class ChallengeResult(
    val level: Int,
    val stars: Int,
)

data class RivalUiState(
    val id: String,
    val name: String,
    val tier: Int,
    val style: OpponentStyle,
    val colorIndex: Int,
    val wins: Int,
    val losses: Int,
    val unlocked: Boolean,
    val isNextChallenger: Boolean,
) {
    val defeated: Boolean
        get() = wins > 0
}

data class RivalMatchResult(
    val rivalId: String,
    val rivalName: String,
    val won: Boolean,
    val unlockedRivalName: String? = null,
)

data class PlayerUiState(
    val index: Int,
    val name: String,
    val colorIndex: Int,
    val ownerIndex: Int,
    val startRow: Int,
    val startCol: Int,
    val totalScore: Int,
    val placedCellPoints: Int,
    val bonusTilePoints: Int,
    val completionBonus: Int,
    val claimedBonusTiles: Int,
    val piecesPlaced: Int,
    val piecesRemaining: Int,
    val hasPassed: Boolean,
    val isCurrentTurn: Boolean,
    val isComputerControlled: Boolean,
)

data class PiecePanelItem(
    val piece: PieceDef,
    val isSelected: Boolean,
    val isUsed: Boolean,
)

data class ResumeGameSummary(
    val savedAtEpochMillis: Long,
    val gameMode: GameMode,
    val leadingPlayerName: String,
    val leadingScore: Int,
    val claimedBonusTiles: Int,
    val difficulty: Int,
)

data class ProfileUiState(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val avatarStyle: LocalAvatarStyle,
    val active: Boolean,
)
