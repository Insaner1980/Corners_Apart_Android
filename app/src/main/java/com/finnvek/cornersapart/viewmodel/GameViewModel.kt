package com.finnvek.cornersapart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.cornersapart.data.GameRepository
import com.finnvek.cornersapart.data.ProfileRepository
import com.finnvek.cornersapart.data.SettingsRepository
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.Scoring
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.multiplayer.LocalSession
import com.finnvek.cornersapart.multiplayer.LocalSessionFactory
import com.finnvek.cornersapart.multiplayer.NearbyConnectionsCoordinator
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.opponents.OpponentDifficultyMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class GameViewModel
    @Inject
    constructor(
        private val sessionFactory: LocalSessionFactory,
        private val gameRepository: GameRepository,
        private val profileRepository: ProfileRepository,
        private val settingsRepository: SettingsRepository,
        private val timeProvider: TimeProvider,
        private val nearbyConnectionsCoordinator: NearbyConnectionsCoordinator,
    ) : ViewModel() {
        private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
        private var selectedPieceId: String = PieceCatalog.SINGLE_CELL_ID
        private var selectedOrientationIndex: Int = 0
        private var settings: GameSettings = GameSettings()
        private var savedGameData: SavedGameData = SavedGameData()
        private var profiles: List<Profile> = emptyList()
        private var nearbyState: NearbyUiState = NearbyUiState()
        private var gameStartedAtMillis: Long = timeProvider.nowEpochMillis()
        private var session: LocalSession = createLocalSession(settings)
        private var resumeDecisionMade: Boolean = false
        private var recordedGameOverTurn: Int? = null
        private var defaultProfileCreationRequested: Boolean = false
        private val _uiState: MutableStateFlow<GameUiState> = MutableStateFlow(session.gameState.value.toUiState())

        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
        val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

        init {
            viewModelScope.launch {
                settingsRepository.settings.collect { persistedSettings ->
                    settings = persistedSettings.normalized()
                    refreshUiState()
                }
            }
            viewModelScope.launch {
                gameRepository.savedGameData.collect { data ->
                    savedGameData = data
                    refreshUiState()
                }
            }
            viewModelScope.launch {
                profileRepository.profiles.collect { storedProfiles ->
                    profiles = storedProfiles
                    ensureDefaultProfileIfNeeded(storedProfiles)
                    refreshUiState()
                }
            }
            viewModelScope.launch {
                nearbyConnectionsCoordinator.nearbyState.collect { state ->
                    nearbyState = state
                    refreshUiState()
                }
            }
        }

        fun startFourPlayerGame() {
            startGame(GameMode.FOUR_PLAYER)
        }

        fun startSoloGame() {
            startGame(GameMode.SOLO)
        }

        fun startGame(mode: GameMode) {
            val nextSettings = settings.copy(preferredMode = mode).normalized()
            settings = nextSettings
            resumeDecisionMade = true
            startLocalSession(nextSettings)
            viewModelScope.launch {
                settingsRepository.updateSettings { it.copy(preferredMode = mode).normalized() }
                gameRepository.clearSavedGame()
            }
        }

        fun resumeSavedGame() {
            viewModelScope.launch {
                val savedState = savedGameData.gameState ?: return@launch
                val savedSettings = savedGameData.settings.normalized()
                settings = savedSettings
                settingsRepository.updateSettings { savedSettings }
                session = createLocalSession(savedSettings.copy(preferredMode = savedState.gameMode))
                session.replaceState(savedState)
                selectedPieceId = PieceCatalog.SINGLE_CELL_ID
                selectedOrientationIndex = 0
                gameStartedAtMillis = timeProvider.nowEpochMillis()
                recordedGameOverTurn = null
                resumeDecisionMade = true
                refreshUiState()
            }
        }

        fun discardSavedGameAndStartNewGame() {
            viewModelScope.launch {
                gameRepository.clearSavedGame()
                resumeDecisionMade = true
                startLocalSession(settings)
            }
        }

        fun setSoundEnabled(enabled: Boolean) {
            updateSettings { it.copy(soundEnabled = enabled) }
        }

        fun setHapticsEnabled(enabled: Boolean) {
            updateSettings { it.copy(hapticsEnabled = enabled) }
        }

        fun setReducedMotionEnabled(enabled: Boolean) {
            updateSettings { it.copy(reducedMotionEnabled = enabled) }
        }

        fun setPreferredDifficulty(level: Int) {
            updateSettings { it.copy(preferredDifficulty = level).normalized() }
        }

        fun setPreferredMode(mode: GameMode) {
            updateSettings { it.copy(preferredMode = mode) }
        }

        fun startNearbyHosting() {
            nearbyConnectionsCoordinator.startHosting(LocalSession.defaultConfigFor(settings.preferredMode))
        }

        fun startNearbyDiscovery() {
            nearbyConnectionsCoordinator.startDiscovery()
        }

        fun connectToNearbyEndpoint(endpointId: String) {
            nearbyConnectionsCoordinator.connectToEndpoint(endpointId)
        }

        fun acceptPendingNearbyConnection(endpointId: String) {
            nearbyConnectionsCoordinator.acceptPendingConnection(endpointId)
        }

        fun rejectPendingNearbyConnection(endpointId: String) {
            nearbyConnectionsCoordinator.rejectPendingConnection(endpointId)
        }

        fun disconnectNearby() {
            nearbyConnectionsCoordinator.disconnect()
        }

        fun setActiveProfile(profileId: String) {
            viewModelScope.launch {
                profileRepository.setActiveProfile(profileId)
            }
        }

        fun addProfile(
            name: String,
            colorIndex: Int,
            avatarStyle: LocalAvatarStyle,
        ) {
            viewModelScope.launch {
                val resolvedName = name.trim().ifBlank { DEFAULT_PROFILE_NAME }
                val id = "local-${timeProvider.nowEpochMillis()}-${resolvedName.lowercase().filter {
                    it
                        .isLetterOrDigit()
                }}"
                profileRepository.upsertProfile(
                    Profile(
                        id = id,
                        name = resolvedName,
                        colorIndex = colorIndex.coerceIn(0, GameConstants.PLAYER_COLORS.lastIndex),
                        avatarStyle = avatarStyle,
                        avatarSeed = id,
                        active = true,
                    ),
                )
            }
        }

        fun updateProfile(
            profileId: String,
            name: String,
            colorIndex: Int,
            avatarStyle: LocalAvatarStyle,
        ) {
            viewModelScope.launch {
                val existing = profiles.firstOrNull { profile -> profile.id == profileId } ?: return@launch
                profileRepository.upsertProfile(
                    existing.copy(
                        name = name.trim().ifBlank { existing.name },
                        colorIndex = colorIndex.coerceIn(0, GameConstants.PLAYER_COLORS.lastIndex),
                        avatarStyle = avatarStyle,
                        avatarSeed = existing.avatarSeed.ifBlank { existing.id },
                    ),
                )
            }
        }

        fun selectPiece(pieceId: String) {
            if (isPieceAvailableForCurrentPlayer(pieceId)) {
                selectedPieceId = pieceId
                selectedOrientationIndex = 0
                refreshUiState()
            }
        }

        fun rotateSelectedClockwise() {
            val piece = PieceCatalog.require(selectedPieceId)
            val orientations = PieceTransforms.getAllOrientations(piece)
            selectedOrientationIndex = (selectedOrientationIndex + 1) % orientations.size
            refreshUiState()
        }

        fun rotateSelectedCounterClockwise() {
            val piece = PieceCatalog.require(selectedPieceId)
            val orientations = PieceTransforms.getAllOrientations(piece)
            selectedOrientationIndex = (selectedOrientationIndex + orientations.lastIndex) % orientations.size
            refreshUiState()
        }

        fun flipSelected() {
            val piece = PieceCatalog.require(selectedPieceId)
            val orientations = PieceTransforms.getAllOrientations(piece)
            val current = orientations[selectedOrientationIndex]
            val flipped = PieceTransforms.normalize(PieceTransforms.flipH(current))
            selectedOrientationIndex =
                orientations.indexOf(flipped).takeIf { index -> index >= 0 } ?: selectedOrientationIndex
            refreshUiState()
        }

        fun placeSelectedAt(
            row: Int,
            col: Int,
        ) {
            viewModelScope.launch {
                val stateBefore = session.gameState.value
                val currentPlayer = stateBefore.players[stateBefore.currentPlayerIndex]
                val result =
                    session.sendMove(
                        Move(
                            playerIndex = currentPlayer.index,
                            pieceId = selectedPieceId,
                            anchorRow = row,
                            anchorCol = col,
                            orientationIndex = selectedOrientationIndex,
                        ),
                    )
                if (result.isSuccess) {
                    refreshUiState()
                    emitAcceptedEffect(stateBefore)
                    persistAfterAcceptedTurn(session.gameState.value)
                } else {
                    _effects.tryEmit(GameEffect.MoveRejected(result.moveRejectionReason()))
                }
            }
        }

        fun passCurrentPlayer() {
            viewModelScope.launch {
                val stateBefore = session.gameState.value
                if (stateBefore.isGameOver) return@launch
                val playerIndex = stateBefore.currentPlayerIndex
                val result = session.sendPass(playerIndex)
                if (result.isSuccess) {
                    refreshUiState()
                    val stateAfter = session.gameState.value
                    if (!stateBefore.isGameOver && stateAfter.isGameOver) {
                        _effects.tryEmit(GameEffect.GameOver)
                    }
                    persistAfterAcceptedTurn(stateAfter)
                } else {
                    _effects.tryEmit(GameEffect.MoveRejected(result.moveRejectionReason()))
                }
            }
        }

        private fun updateSettings(transform: (GameSettings) -> GameSettings) {
            viewModelScope.launch {
                val nextSettings = transform(settings).normalized()
                settings = nextSettings
                settingsRepository.updateSettings { transform(it).normalized() }
                refreshUiState()
            }
        }

        private fun startLocalSession(nextSettings: GameSettings) {
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            session = createLocalSession(nextSettings)
            refreshUiState()
        }

        private fun createLocalSession(nextSettings: GameSettings): LocalSession =
            sessionFactory.create(
                initialConfig = LocalSession.defaultConfigFor(nextSettings.preferredMode),
                persistedDifficulty = nextSettings.preferredDifficulty,
            )

        private suspend fun persistAfterAcceptedTurn(state: GameState) {
            if (state.isGameOver) {
                recordFinishedGameOnce(state)
                gameRepository.clearSavedGame()
            } else {
                gameRepository.saveGame(
                    state = state,
                    settings = settings,
                    savedAtEpochMillis = timeProvider.nowEpochMillis(),
                )
            }
        }

        private suspend fun recordFinishedGameOnce(state: GameState) {
            if (recordedGameOverTurn == state.turnNumber) return
            recordedGameOverTurn = state.turnNumber
            val profile = activeProfile() ?: defaultProfile().also { profileRepository.upsertProfile(it) }
            profileRepository.appendHistory(profile.id, state.toHistoryEntry())
        }

        private fun GameState.toHistoryEntry(): HistoryEntry {
            val rankedScores = Scoring.rankPlayers(this)
            val activeOwnerIndex = DEFAULT_PROFILE_OWNER_INDEX
            val profileScore = rankedScores.firstOrNull { score -> score.ownerIndex == activeOwnerIndex }
            val ownerPlayers = players.filter { player -> player.ownerIndex == activeOwnerIndex }
            return HistoryEntry(
                date = timeProvider.todayIsoDate(),
                rank =
                    rankedScores.indexOfFirst { score -> score.ownerIndex == activeOwnerIndex }.let { index ->
                        index +
                            1
                    },
                totalScore = profileScore?.totalScore ?: 0,
                scoreBreakdown = profileScore?.scoreBreakdown ?: ownerPlayers.combinedScoreBreakdown(),
                claimedBonusTiles = profileScore?.claimedBonusTiles ?: 0,
                piecesPlaced = ownerPlayers.sumOf { player -> player.usedPieceIds.size },
                difficulty = settings.preferredDifficulty,
                ruleset = ruleset,
                gameMode = gameMode,
                timeSeconds = elapsedGameSeconds(),
                scores = rankedScores,
            )
        }

        private fun List<com.finnvek.cornersapart.model.Player>.combinedScoreBreakdown():
            com.finnvek.cornersapart.model.ScoreBreakdown =
            com.finnvek.cornersapart.model.ScoreBreakdown(
                placedCellPoints = sumOf { player -> player.scoreBreakdown.placedCellPoints },
                bonusTilePoints = sumOf { player -> player.scoreBreakdown.bonusTilePoints },
                completionBonus = sumOf { player -> player.scoreBreakdown.completionBonus },
            )

        private fun refreshUiState() {
            normalizeSelectionForCurrentPlayer()
            _uiState.value = session.gameState.value.toUiState()
        }

        private fun GameState.toUiState(): GameUiState {
            val currentPlayer = players[currentPlayerIndex]
            val selectedPiece = PieceCatalog.require(selectedPieceId)
            val selectedCells =
                PieceTransforms.getOrientation(selectedPiece, selectedOrientationIndex) ?: selectedPiece.cells
            val activeProfile = activeProfile()
            return GameUiState(
                gameMode = gameMode,
                board = board,
                bonusTiles = bonusTiles,
                players =
                    players.map { player ->
                        val claimedBonusTiles =
                            bonusTiles.count { tile -> tile.claimedByPlayerIndex == player.index }
                        PlayerUiState(
                            index = player.index,
                            name = player.name,
                            colorIndex = player.colorIndex,
                            ownerIndex = player.ownerIndex,
                            startRow = player.startCorner.row,
                            startCol = player.startCorner.col,
                            totalScore = player.scoreBreakdown.total,
                            placedCellPoints = player.scoreBreakdown.placedCellPoints,
                            bonusTilePoints = player.scoreBreakdown.bonusTilePoints,
                            completionBonus = player.scoreBreakdown.completionBonus,
                            claimedBonusTiles = claimedBonusTiles,
                            piecesPlaced = player.usedPieceIds.size,
                            piecesRemaining = PieceCatalog.all.size - player.usedPieceIds.size,
                            hasPassed = player.passed,
                            isCurrentTurn = player.index == currentPlayer.index,
                            isComputerControlled = player.isComputerControlled,
                        )
                    },
                currentPlayerIndex = currentPlayer.index,
                selectedPieceId = selectedPieceId,
                selectedOrientationIndex = selectedOrientationIndex,
                selectedCells = selectedCells,
                pieces = PieceCatalog.all.map { piece -> piece.toPanelItem(currentPlayer.usedPieceIds) },
                isGameOver = isGameOver,
                soundEnabled = settings.soundEnabled,
                hapticsEnabled = settings.hapticsEnabled,
                reducedMotionEnabled = settings.reducedMotionEnabled,
                gameDurationSeconds = elapsedGameSeconds(),
                preferredDifficulty = settings.preferredDifficulty,
                preferredMode = settings.preferredMode,
                history = activeProfile?.history.orEmpty(),
                activeProfileName = activeProfile?.name ?: DEFAULT_PROFILE_NAME,
                hasSavedGame = savedGameData.gameState != null && !resumeDecisionMade,
                resumeSummary = savedGameData.toResumeSummary(),
                rankedScores = Scoring.rankPlayers(this),
                nearbyState = nearbyState,
                profiles = profiles.map { profile -> profile.toUiState() },
            )
        }

        private fun SavedGameData.toResumeSummary(): ResumeGameSummary? {
            val state = gameState ?: return null
            if (resumeDecisionMade) return null
            val rankedScores = Scoring.rankPlayers(state)
            val leader = rankedScores.firstOrNull()
            return ResumeGameSummary(
                savedAtEpochMillis = savedAtEpochMillis,
                gameMode = state.gameMode,
                leadingPlayerName = leader?.name.orEmpty(),
                leadingScore = leader?.totalScore ?: 0,
                claimedBonusTiles = state.bonusTiles.count { tile -> tile.claimedByPlayerIndex != null },
                difficulty = settings.preferredDifficulty,
            )
        }

        private fun Profile.toUiState(): ProfileUiState =
            ProfileUiState(
                id = id,
                name = name,
                colorIndex = colorIndex,
                avatarStyle = avatarStyle,
                active = active,
            )

        private fun com.finnvek.cornersapart.model.PieceDef.toPanelItem(usedPieceIds: Set<String>): PiecePanelItem =
            PiecePanelItem(
                piece = this,
                isSelected = id == selectedPieceId,
                isUsed = id in usedPieceIds,
            )

        private fun normalizeSelectionForCurrentPlayer() {
            val currentPlayer = session.gameState.value.players[session.gameState.value.currentPlayerIndex]
            if (selectedPieceId !in currentPlayer.usedPieceIds) return
            selectedPieceId =
                PieceCatalog.all.firstOrNull { piece -> piece.id !in currentPlayer.usedPieceIds }?.id ?: selectedPieceId
            selectedOrientationIndex = 0
        }

        private fun isPieceAvailableForCurrentPlayer(pieceId: String): Boolean {
            val currentPlayer = session.gameState.value.players[session.gameState.value.currentPlayerIndex]
            return PieceCatalog.find(pieceId) != null && pieceId !in currentPlayer.usedPieceIds
        }

        private fun emitAcceptedEffect(stateBefore: GameState) {
            val stateAfter = session.gameState.value
            val playerBefore = stateBefore.players[stateBefore.currentPlayerIndex]
            val playerAfter = stateAfter.players[playerBefore.index]
            val delta = playerAfter.scoreBreakdown.total - playerBefore.scoreBreakdown.total
            if (delta > 0) {
                _effects.tryEmit(
                    GameEffect.MoveAccepted(
                        playerName = playerBefore.name,
                        scoreDelta = delta,
                        bonusTileClaimed =
                            playerAfter.scoreBreakdown.bonusTilePoints >
                                playerBefore.scoreBreakdown.bonusTilePoints,
                    ),
                )
            }
            if (stateAfter.isGameOver) {
                _effects.tryEmit(GameEffect.GameOver)
            }
        }

        private fun Result<Unit>.moveRejectionReason(): MoveRejectionReason {
            val message = exceptionOrNull()?.message.orEmpty()
            return MoveRejectionReason.entries.firstOrNull { reason -> reason.name == message }
                ?: MoveRejectionReason.CELL_OCCUPIED
        }

        private fun activeProfile(): Profile? = profiles.firstOrNull { profile -> profile.active }

        private fun ensureDefaultProfileIfNeeded(storedProfiles: List<Profile>) {
            if (storedProfiles.isNotEmpty() || defaultProfileCreationRequested) return
            defaultProfileCreationRequested = true
            viewModelScope.launch {
                profileRepository.upsertProfile(defaultProfile())
            }
        }

        private fun defaultProfile(): Profile =
            Profile(
                id = DEFAULT_PROFILE_ID,
                name = DEFAULT_PROFILE_NAME,
                colorIndex = DEFAULT_PROFILE_OWNER_INDEX,
                avatarStyle = LocalAvatarStyle.INITIALS,
                avatarSeed = DEFAULT_PROFILE_ID,
                active = true,
            )

        private fun GameSettings.normalized(): GameSettings =
            copy(preferredDifficulty = OpponentDifficultyMapper.toPersistedLevel(preferredDifficulty))

        private fun elapsedGameSeconds(): Int =
            ((timeProvider.nowEpochMillis() - gameStartedAtMillis) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)
    }

private const val DEFAULT_PROFILE_ID = "local-default"
private const val DEFAULT_PROFILE_NAME = "Player"
private const val DEFAULT_PROFILE_OWNER_INDEX = 0
private const val MILLIS_PER_SECOND = 1_000L
