package com.finnvek.cornersapart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.data.GameRepository
import com.finnvek.cornersapart.data.ProfileRepository
import com.finnvek.cornersapart.data.SettingsRepository
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectedException
import com.finnvek.cornersapart.engine.Scoring
import com.finnvek.cornersapart.model.AchievementEvaluator
import com.finnvek.cornersapart.model.ChallengeLevels
import com.finnvek.cornersapart.model.DailyStreakCalculator
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.HallOfFameCalculator
import com.finnvek.cornersapart.model.HallOfFameEntry
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.Player
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.model.Profile
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.model.hasValidIndexDomains
import com.finnvek.cornersapart.model.toSnapshotCopy
import com.finnvek.cornersapart.model.toSnapshotList
import com.finnvek.cornersapart.multiplayer.GameSession
import com.finnvek.cornersapart.multiplayer.GameSessionEvent
import com.finnvek.cornersapart.multiplayer.LocalSession
import com.finnvek.cornersapart.multiplayer.LocalSessionFactory
import com.finnvek.cornersapart.multiplayer.NearbyConnectionsCoordinator
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.multiplayer.SessionType
import com.finnvek.cornersapart.opponents.OpponentCharacter
import com.finnvek.cornersapart.opponents.OpponentDifficultyMapper
import com.finnvek.cornersapart.opponents.OpponentRoster
import com.finnvek.cornersapart.review.MatchReviewAnalyzer
import com.finnvek.cornersapart.review.MatchReviewFailure
import com.finnvek.cornersapart.review.MatchReviewUpdate
import com.finnvek.cornersapart.review.MoveClassification
import com.finnvek.cornersapart.runtime.StringProvider
import com.finnvek.cornersapart.runtime.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.finnvek.cornersapart.multiplayer.toSnapshotCopy as toNearbySnapshotCopy

@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class GameViewModel
    @Inject
    constructor(
        private val sessionFactory: LocalSessionFactory,
        private val gameRepository: GameRepository,
        private val profileRepository: ProfileRepository,
        private val settingsRepository: SettingsRepository,
        private val stringProvider: StringProvider,
        private val timeProvider: TimeProvider,
        private val nearbyConnectionsCoordinator: NearbyConnectionsCoordinator,
        private val gameEngine: GameEngine,
        private val matchReviewAnalyzer: MatchReviewAnalyzer,
    ) : ViewModel() {
        private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
        private var selectedPieceId: String = PieceCatalog.SINGLE_CELL_ID
        private var selectedOrientationIndex: Int = 0
        private var settings: GameSettings = GameSettings()
        private var savedGameData: SavedGameData = SavedGameData()
        private var profiles: List<Profile> = emptyList()
        private var nearbyState: NearbyUiState = NearbyUiState()
        private var gameStartedAtMillis: Long = timeProvider.nowEpochMillis()
        private var localSession: LocalSession = createLocalSession(settings)
        private var nearbySession: GameSession? = null
        private var nearbyGameStateJob: Job? = null
        private var nearbyEventsJob: Job? = null
        private var localSessionActionJob: Job = newLocalSessionActionJob()
        private var resumeDecisionMade: Boolean = false
        private var activeChallengeLevel: Int? = null
        private var lastChallengeResult: ChallengeResult? = null
        private var activeRivalId: String? = null
        private var lastRivalResult: RivalMatchResult? = null
        private var lastGameAllTimeRank: Int? = null
        private var hallOfFameCache: Pair<List<Profile>, Map<GameMode?, List<HallOfFameEntry>>>? = null
        private var lastGameWasBestScore: Boolean = false
        private var lastGameNewAchievements: List<String> = emptyList()
        private var activeDailyDate: String? = null
        private val profileDisplayMapper =
            ProfileDisplayMapper(
                isLocalSession = { session.sessionType == SessionType.LOCAL },
                activeProfile = ::activeProfile,
            )
        private var recordedGameOverTurn: Int? = null
        private var defaultProfileCreationRequested: Boolean = false
        private var finishedGameRanking: FinishedGameRanking? = null
        private var reviewableFinalState: GameState? = null
        private var matchReviewUiState: MatchReviewUiState? = null
        private var matchReviewJob: Job? = null
        private var matchReviewGeneration: Long = 0L
        private val _uiState: MutableStateFlow<GameUiState> = MutableStateFlow(session.gameState.value.toUiState())
        private val session: GameSession
            get() = nearbySession ?: localSession

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
                    if (data.gameState?.hasValidIndexDomains() == false) {
                        savedGameData = SavedGameData()
                        gameRepository.clearSavedGame()
                    } else {
                        savedGameData = data
                    }
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
            viewModelScope.launch {
                nearbyConnectionsCoordinator.currentSession.collect { currentSession ->
                    nearbyGameStateJob?.cancel()
                    nearbyEventsJob?.cancel()
                    nearbySession = currentSession
                    if (currentSession != null) {
                        resetMatchReview()
                        resumeDecisionMade = true
                        selectedPieceId = PieceCatalog.SINGLE_CELL_ID
                        selectedOrientationIndex = 0
                        gameStartedAtMillis = timeProvider.nowEpochMillis()
                        recordedGameOverTurn = null
                        nearbyGameStateJob =
                            viewModelScope.launch {
                                currentSession.gameState.collect {
                                    refreshUiState()
                                }
                            }
                        nearbyEventsJob =
                            viewModelScope.launch {
                                currentSession.events.collect { event ->
                                    _effects.tryEmit(event.toEffect())
                                }
                            }
                    } else {
                        nearbyGameStateJob = null
                        nearbyEventsJob = null
                    }
                    refreshUiState()
                }
            }
        }

        fun startGame(mode: GameMode) {
            val nextSettings = settings.copy(preferredMode = mode).normalized()
            settings = nextSettings
            resumeDecisionMade = true
            leaveNearbySessionForLocalPlay()
            startLocalSession(nextSettings)
            viewModelScope.launch {
                settingsRepository.updateSettings { it.copy(preferredMode = mode).normalized() }
                gameRepository.clearSavedGame()
            }
        }

        fun resumeSavedGame() {
            viewModelScope.launch {
                val savedState = savedGameData.gameState ?: return@launch
                if (!savedState.hasValidIndexDomains()) {
                    gameRepository.clearSavedGame()
                    savedGameData = SavedGameData()
                    refreshUiState()
                    return@launch
                }
                val savedSettings = savedGameData.settings.normalized()
                settings = savedSettings
                resetMatchReview()
                leaveNearbySessionForLocalPlay()
                resetLocalSessionActions()
                settingsRepository.updateSettings { savedSettings }
                localSession = createLocalSession(savedSettings.copy(preferredMode = savedState.gameMode))
                localSession.replaceState(savedState)
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
                resumeDecisionMade = true
                leaveNearbySessionForLocalPlay()
                startLocalSession(settings)
                gameRepository.clearSavedGame()
            }
        }

        fun setSoundEnabled(enabled: Boolean) {
            updateSettings { it.copy(soundEnabled = enabled) }
        }

        fun setHapticsEnabled(enabled: Boolean) {
            updateSettings { it.copy(hapticsEnabled = enabled) }
        }

        fun setPreferredDifficulty(level: Int) {
            updateSettings { it.copy(preferredDifficulty = level).normalized() }
        }

        fun setPreferredMode(mode: GameMode) {
            updateSettings { it.copy(preferredMode = mode) }
        }

        fun startNearbyHosting() {
            prepareForNearbySession()
            nearbyConnectionsCoordinator.startHosting(LocalSession.defaultConfigFor(settings.preferredMode))
        }

        fun startNearbyDiscovery() {
            prepareForNearbySession()
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
                val resolvedName = name.trim().ifBlank { defaultProfileName() }
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

        fun deleteProfile(profileId: String) {
            viewModelScope.launch {
                if (profiles.size <= 1) return@launch
                profileRepository.deleteProfile(profileId)
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

        /** Kertoo esikatselulle, olisiko valitun palan sijoitus annettuun ankkuriin laillinen. */
        fun isPlacementLegal(
            row: Int,
            col: Int,
        ): Boolean {
            val state = session.gameState.value
            return gameEngine
                .previewPlacement(
                    state,
                    Move(
                        playerIndex = state.currentPlayerIndex,
                        pieceId = selectedPieceId,
                        anchorRow = row,
                        anchorCol = col,
                        orientationIndex = selectedOrientationIndex,
                    ),
                ).isValid
        }

        fun placeSelectedAt(
            row: Int,
            col: Int,
        ) {
            // Pelaaminen on päätös jatkaa käynnissä olevaa peliä — estää
            // resume-dialogin ponnahtamisen oman autosaven takia.
            resumeDecisionMade = true
            launchGameplayAction { gameplaySession ->
                val stateBefore = gameplaySession.gameState.value
                val currentPlayer = stateBefore.players[stateBefore.currentPlayerIndex]
                val result =
                    gameplaySession.sendMove(
                        Move(
                            playerIndex = currentPlayer.index,
                            pieceId = selectedPieceId,
                            anchorRow = row,
                            anchorCol = col,
                            orientationIndex = selectedOrientationIndex,
                        ),
                    )
                if (result.isSuccess) {
                    val stateAfter = gameplaySession.gameState.value
                    captureReviewableFinalState(gameplaySession, stateAfter)
                    refreshUiState()
                    emitAcceptedEffect(gameplaySession, stateBefore)
                    if (gameplaySession.sessionType == SessionType.LOCAL) {
                        persistAfterAcceptedTurn(stateAfter)
                    }
                } else {
                    _effects.tryEmit(result.toFailureEffect(stringProvider))
                }
            }
        }

        fun passCurrentPlayer() {
            resumeDecisionMade = true
            launchGameplayAction { gameplaySession ->
                val stateBefore = gameplaySession.gameState.value
                if (stateBefore.isGameOver) return@launchGameplayAction
                val playerIndex = stateBefore.currentPlayerIndex
                val result = gameplaySession.sendPass(playerIndex)
                if (result.isSuccess) {
                    val stateAfter = gameplaySession.gameState.value
                    captureReviewableFinalState(gameplaySession, stateAfter)
                    refreshUiState()
                    if (!stateBefore.isGameOver && stateAfter.isGameOver) {
                        _effects.tryEmit(GameEffect.GameOver)
                    }
                    if (gameplaySession.sessionType == SessionType.LOCAL) {
                        persistAfterAcceptedTurn(stateAfter)
                    }
                } else {
                    _effects.tryEmit(result.toFailureEffect(stringProvider))
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
            resetMatchReview()
            activeChallengeLevel = null
            activeDailyDate = null
            activeRivalId = null
            lastChallengeResult = null
            lastRivalResult = null
            lastGameWasBestScore = false
            lastGameNewAchievements = emptyList()
            lastGameAllTimeRank = null
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            resetLocalSessionActions()
            localSession = createLocalSession(nextSettings)
            refreshUiState()
        }

        /** Päivän haaste: sama lauta kaikille saman päivän aikana. */
        fun startDailyChallenge() {
            val date = timeProvider.todayIsoDate()
            resumeDecisionMade = true
            resetMatchReview()
            leaveNearbySessionForLocalPlay()
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            resetLocalSessionActions()
            localSession =
                sessionFactory.create(
                    initialConfig =
                        GameModeConfigs.defaultGameConfig(
                            mode = GameMode.SOLO,
                            randomSeed = date.hashCode().toLong(),
                        ),
                    persistedDifficulty = settings.preferredDifficulty,
                )
            activeChallengeLevel = null
            activeDailyDate = date
            activeRivalId = null
            lastChallengeResult = null
            lastRivalResult = null
            lastGameWasBestScore = false
            lastGameNewAchievements = emptyList()
            lastGameAllTimeRank = null
            refreshUiState()
        }

        /** Käynnistää solo-haastetason kiinteällä siemenellä ja tason vaikeudella. */
        fun startChallengeLevel(levelNumber: Int) {
            val level = ChallengeLevels.forNumber(levelNumber) ?: return
            resumeDecisionMade = true
            resetMatchReview()
            leaveNearbySessionForLocalPlay()
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            resetLocalSessionActions()
            localSession =
                sessionFactory.create(
                    initialConfig =
                        GameModeConfigs.defaultGameConfig(
                            mode = GameMode.SOLO,
                            randomSeed = level.randomSeed,
                        ),
                    persistedDifficulty = level.difficultyLevel,
                )
            activeChallengeLevel = level.number
            activeDailyDate = null
            activeRivalId = null
            lastChallengeResult = null
            lastRivalResult = null
            lastGameWasBestScore = false
            lastGameNewAchievements = emptyList()
            lastGameAllTimeRank = null
            refreshUiState()
        }

        /** Käynnistää Rivals-ottelun: 1v1 kompaktilla laudalla nimettyä vastustajaa vastaan. */
        fun startRivalMatch(rivalId: String) {
            val rival = OpponentRoster.forId(rivalId) ?: return
            if (!OpponentRoster.isUnlocked(rival.id, activeProfile()?.rivalWins.orEmpty())) return
            resumeDecisionMade = true
            resetMatchReview()
            leaveNearbySessionForLocalPlay()
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            resetLocalSessionActions()
            localSession =
                sessionFactory.createRivalMatch(
                    initialConfig =
                        GameModeConfigs.defaultGameConfig(
                            mode = GameMode.COMPACT_DUEL,
                            randomSeed = timeProvider.nowEpochMillis(),
                        ),
                    character = rival,
                    rivalColorIndex = rivalDisplayColorIndex(rival),
                )
            activeChallengeLevel = null
            activeDailyDate = null
            activeRivalId = rival.id
            lastChallengeResult = null
            lastRivalResult = null
            lastGameWasBestScore = false
            lastGameNewAchievements = emptyList()
            lastGameAllTimeRank = null
            refreshUiState()
        }

        /**
         * Valitsee vastustajan pelipaikan värin niin, ettei se osu ihmispaikan
         * väriin 0 eikä profiilin näyttöväriin (0 ↔ profiiliväri -vaihto).
         */
        private fun rivalDisplayColorIndex(rival: OpponentCharacter): Int {
            val profileColor = activeProfile()?.colorIndex ?: DEFAULT_PROFILE_OWNER_INDEX
            val colorCount = GameConstants.PLAYER_COLORS.size
            return (0 until colorCount)
                .map { offset -> (rival.colorIndex + offset) % colorCount }
                .first { color -> color != DEFAULT_PROFILE_OWNER_INDEX && color != profileColor }
        }

        private fun prepareForNearbySession() {
            resetMatchReview()
            resetLocalSessionActions()
            lastGameAllTimeRank = null
            selectedPieceId = PieceCatalog.SINGLE_CELL_ID
            selectedOrientationIndex = 0
            gameStartedAtMillis = timeProvider.nowEpochMillis()
            recordedGameOverTurn = null
            resumeDecisionMade = true
        }

        private fun leaveNearbySessionForLocalPlay() {
            nearbyGameStateJob?.cancel()
            nearbyGameStateJob = null
            nearbyEventsJob?.cancel()
            nearbyEventsJob = null
            nearbySession = null
            nearbyConnectionsCoordinator.disconnect()
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
            val entry = state.toHistoryEntry()
            lastGameWasBestScore =
                profile.history.isNotEmpty() &&
                entry.totalScore > profile.history.maxOf { previous -> previous.totalScore }
            lastGameAllTimeRank =
                HallOfFameCalculator.allTimeRank(
                    profiles = profiles,
                    mode = entry.gameMode,
                    score = entry.totalScore,
                )
            profileRepository.appendHistory(profile.id, entry)
            recordChallengeResult(state, profile.id)
            recordRivalMatchResult(state, profile)
            recordAchievements(profile, entry)
            activeDailyDate?.let { date ->
                profileRepository.recordDailyBest(profile.id, date, entry.totalScore)
            }
        }

        private suspend fun recordAchievements(
            profile: Profile,
            entry: HistoryEntry,
        ) {
            val starsAfterGame =
                lastChallengeResult
                    ?.takeIf { result -> result.stars > (profile.challengeStars[result.level] ?: 0) }
                    ?.let { result -> profile.challengeStars + (result.level to result.stars) }
                    ?: profile.challengeStars
            val newAchievements =
                AchievementEvaluator
                    .earnedAfterGame(entry, profile.history, starsAfterGame)
                    .map { achievement -> achievement.id }
                    .filterNot { id -> id in profile.achievements }
            lastGameNewAchievements = newAchievements
            profileRepository.addAchievements(profile.id, newAchievements)
        }

        private suspend fun recordChallengeResult(
            state: GameState,
            profileId: String,
        ) {
            val level = activeChallengeLevel?.let(ChallengeLevels::forNumber) ?: return
            val ranked = state.rankedScoresForUiAndHistory()
            val rank = ranked.indexOfFirst { score -> score.ownerIndex == DEFAULT_PROFILE_OWNER_INDEX } + 1
            val score = ranked.firstOrNull { it.ownerIndex == DEFAULT_PROFILE_OWNER_INDEX }?.totalScore ?: 0
            val stars = ChallengeLevels.starsFor(level, rank, score)
            lastChallengeResult = ChallengeResult(level = level.number, stars = stars)
            if (stars > 0) {
                profileRepository.recordChallengeStars(profileId, level.number, stars)
            }
        }

        private suspend fun recordRivalMatchResult(
            state: GameState,
            profile: Profile,
        ) {
            val rival = activeRivalId?.let(OpponentRoster::forId) ?: return
            val won =
                state.rankedScoresForUiAndHistory().firstOrNull()?.ownerIndex == DEFAULT_PROFILE_OWNER_INDEX
            val firstWin = won && (profile.rivalWins[rival.id] ?: 0) == 0
            lastRivalResult =
                RivalMatchResult(
                    rivalId = rival.id,
                    rivalName = rival.name,
                    won = won,
                    unlockedRivalName = if (firstWin) OpponentRoster.unlockedByFirstWinOf(rival.id)?.name else null,
                )
            profileRepository.recordRivalResult(profile.id, rival.id, won)
        }

        private fun GameState.toHistoryEntry(): HistoryEntry {
            val rankedScores = rankedScoresForUiAndHistory()
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
                scoreBreakdown = profileScore?.scoreBreakdown ?: Scoring.combinedScoreBreakdown(ownerPlayers),
                claimedBonusTiles = profileScore?.claimedBonusTiles ?: 0,
                piecesPlaced = ownerPlayers.sumOf { player -> player.usedPieceIds.size },
                difficulty = settings.preferredDifficulty,
                ruleset = ruleset,
                gameMode = gameMode,
                timeSeconds = elapsedGameSeconds(),
                scores = rankedScores,
            )
        }

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
                board = board.toSnapshotCopy(),
                bonusTiles = bonusTiles.toSnapshotList(),
                players = toPlayerUiStates(currentPlayer),
                currentPlayerIndex = currentPlayer.index,
                selectedPieceId = selectedPieceId,
                selectedOrientationIndex = selectedOrientationIndex,
                selectedCells = selectedCells.toSnapshotList(),
                pieces =
                    PieceCatalog.all
                        .map { piece ->
                            piece.toPanelItem(currentPlayer.usedPieceIds)
                        }.toSnapshotList(),
                isGameOver = isGameOver,
                soundEnabled = settings.soundEnabled,
                hapticsEnabled = settings.hapticsEnabled,
                gameDurationSeconds = elapsedGameSeconds(),
                preferredDifficulty = settings.preferredDifficulty,
                preferredMode = settings.preferredMode,
                history = activeProfile.historyUiState(),
                activeProfileName = activeProfile?.name ?: defaultProfileName(),
                hasSavedGame = savedGameData.gameState?.hasValidIndexDomains() == true && !resumeDecisionMade,
                resumeSummary = savedGameData.toResumeSummary(),
                rankedScores = rankedScoresForUiAndHistory(),
                sessionType = session.sessionType,
                nearbyState = nearbyState.toNearbySnapshotCopy(),
                profiles = profilesUiState(),
                activeChallengeLevel = activeChallengeLevel,
                challengeStars = activeProfile()?.challengeStars.orEmpty(),
                challengeResult = lastChallengeResult,
                isNewBestScore = lastGameWasBestScore,
                newAchievements = lastGameNewAchievements,
                unlockedAchievements = activeProfile()?.achievements.orEmpty().toSet(),
                isDailyChallenge = activeDailyDate != null,
                dailyBestScore = activeProfile()?.dailyBestScores?.get(timeProvider.todayIsoDate()),
                rivals = rivalsUiState(),
                activeRivalId = activeRivalId,
                rivalResult = lastRivalResult,
                dailyStreak =
                    DailyStreakCalculator.currentStreak(
                        playedDates = activeProfile()?.dailyBestScores?.keys.orEmpty(),
                        todayIsoDate = timeProvider.todayIsoDate(),
                    ),
                bestDailyStreak = activeProfile()?.bestDailyStreak ?: 0,
                allTimeRank = lastGameAllTimeRank,
                hallOfFameByMode = hallOfFameUiState(),
                canReviewFinishedGame =
                    reviewableFinalState?.isGameOver == true &&
                        session.sessionType == SessionType.LOCAL,
                matchReview = matchReviewUiState,
            )
        }

        @Suppress("TooGenericExceptionCaught")
        fun startMatchReview() {
            val finalState = reviewableFinalState ?: return
            if (!finalState.isGameOver ||
                session.sessionType != SessionType.LOCAL ||
                session.gameState.value != finalState
            ) {
                return
            }

            matchReviewJob?.cancel()
            matchReviewGeneration += 1
            val generation = matchReviewGeneration
            matchReviewUiState = initialMatchReviewUiState(finalState)
            refreshUiState()
            matchReviewJob =
                viewModelScope.launch {
                    try {
                        matchReviewAnalyzer
                            .analyze(finalState, reviewedOwnerIndex = DEFAULT_PROFILE_OWNER_INDEX)
                            .collect { update ->
                                if (generation != matchReviewGeneration ||
                                    reviewableFinalState != finalState
                                ) {
                                    return@collect
                                }
                                applyMatchReviewUpdate(update)
                            }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        if (generation == matchReviewGeneration) {
                            applyMatchReviewFailure(
                                MatchReviewFailure.UnexpectedAnalysisError(cause = error),
                            )
                        }
                    }
                }
        }

        private fun initialMatchReviewUiState(finalState: GameState): MatchReviewUiState =
            MatchReviewUiState(
                phase = MatchReviewPhase.ANALYZING,
                players =
                    finalState.players.map { player ->
                        MatchReviewPlayerUiState(
                            index = player.index,
                            name =
                                profileDisplayMapper.displayName(
                                    player.name,
                                    player.ownerIndex,
                                    player.colorIndex,
                                ),
                            colorIndex = profileDisplayMapper.visualColorIndex(player.colorIndex),
                        )
                    },
                timeline = emptyList(),
                assessmentsByStepIndex = emptyMap(),
                analyzedCount = 0,
                totalCount = 0,
                currentStepIndex = 0,
                accuracy = null,
                classificationCounts = emptyClassificationCounts(),
            )

        fun reviewStepForward() {
            val review = matchReviewUiState ?: return
            updateReviewStep(review.currentStepIndex + 1)
        }

        fun reviewStepBack() {
            val review = matchReviewUiState ?: return
            updateReviewStep(review.currentStepIndex - 1)
        }

        fun reviewJumpTo(index: Int) {
            updateReviewStep(index)
        }

        fun closeMatchReview() {
            matchReviewJob?.cancel()
            matchReviewJob = null
            matchReviewGeneration += 1
            matchReviewUiState = null
            refreshUiState()
        }

        private fun updateReviewStep(index: Int) {
            val review = matchReviewUiState ?: return
            if (review.timeline.isEmpty()) return
            matchReviewUiState =
                review.copy(currentStepIndex = index.coerceIn(0, review.timeline.lastIndex))
            refreshUiState()
        }

        private fun applyMatchReviewUpdate(update: MatchReviewUpdate) {
            val current = matchReviewUiState ?: return
            matchReviewUiState =
                when (update) {
                    is MatchReviewUpdate.Progress -> {
                        current.copy(
                            phase = MatchReviewPhase.ANALYZING,
                            timeline = update.value.timeline,
                            assessmentsByStepIndex = update.value.assessmentsByStepIndex,
                            analyzedCount = update.value.analyzedCount,
                            totalCount = update.value.totalCount,
                            currentStepIndex =
                                current.currentStepIndex.coerceInTimeline(update.value.timeline.lastIndex),
                            accuracy = update.value.runningAccuracy,
                        )
                    }

                    is MatchReviewUpdate.Completed -> {
                        current.copy(
                            phase = MatchReviewPhase.COMPLETE,
                            timeline = update.result.timeline,
                            assessmentsByStepIndex = update.result.assessmentsByStepIndex,
                            analyzedCount = update.result.assessmentsByStepIndex.size,
                            totalCount = update.result.assessmentsByStepIndex.size,
                            currentStepIndex =
                                current.currentStepIndex.coerceInTimeline(update.result.timeline.lastIndex),
                            accuracy = update.result.accuracy,
                            classificationCounts = update.result.classificationCounts,
                        )
                    }

                    is MatchReviewUpdate.Failed -> {
                        current.copy(
                            phase = MatchReviewPhase.FAILED,
                            failure = update.failure,
                        )
                    }
                }
            refreshUiState()
        }

        private fun applyMatchReviewFailure(failure: MatchReviewFailure) {
            val current = matchReviewUiState ?: return
            matchReviewUiState =
                current.copy(
                    phase = MatchReviewPhase.FAILED,
                    failure = failure,
                )
            refreshUiState()
        }

        private fun captureReviewableFinalState(
            gameplaySession: GameSession,
            stateAfter: GameState,
        ) {
            if (gameplaySession.sessionType == SessionType.LOCAL && stateAfter.isGameOver) {
                reviewableFinalState = stateAfter.toSnapshotCopy()
            }
        }

        private fun resetMatchReview() {
            matchReviewJob?.cancel()
            matchReviewJob = null
            matchReviewGeneration += 1
            reviewableFinalState = null
            matchReviewUiState = null
        }

        private fun emptyClassificationCounts(): Map<MoveClassification, Int> =
            MoveClassification.entries.associateWith { 0 }

        private fun Int.coerceInTimeline(lastIndex: Int): Int = if (lastIndex < 0) 0 else coerceIn(0, lastIndex)

        private fun GameState.toPlayerUiStates(currentPlayer: Player): List<PlayerUiState> =
            players
                .map { player ->
                    player.toUiState(
                        claimedBonusTiles = bonusTiles.count { tile -> tile.claimedByPlayerIndex == player.index },
                        isCurrentTurn = player.index == currentPlayer.index,
                    )
                }.toSnapshotList()

        private fun Player.toUiState(
            claimedBonusTiles: Int,
            isCurrentTurn: Boolean,
        ): PlayerUiState =
            PlayerUiState(
                index = index,
                name = profileDisplayMapper.displayName(name, ownerIndex, colorIndex),
                colorIndex = profileDisplayMapper.visualColorIndex(colorIndex),
                ownerIndex = ownerIndex,
                startRow = startCorner.row,
                startCol = startCorner.col,
                totalScore = scoreBreakdown.total,
                placedCellPoints = scoreBreakdown.placedCellPoints,
                bonusTilePoints = scoreBreakdown.bonusTilePoints,
                completionBonus = scoreBreakdown.completionBonus,
                claimedBonusTiles = claimedBonusTiles,
                piecesPlaced = usedPieceIds.size,
                piecesRemaining = PieceCatalog.all.size - usedPieceIds.size,
                hasPassed = passed,
                isCurrentTurn = isCurrentTurn,
                isComputerControlled = isComputerControlled,
            )

        private fun Profile?.historyUiState(): List<HistoryEntry> =
            this
                ?.history
                .orEmpty()
                .map { entry -> entry.toSnapshotCopy() }
                .toSnapshotList()

        private fun SavedGameData.toResumeSummary(): ResumeGameSummary? {
            val state = gameState ?: return null
            if (!state.hasValidIndexDomains()) return null
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

        private fun launchGameplayAction(action: suspend (GameSession) -> Unit) {
            val gameplaySession = session
            val actionContext: CoroutineContext =
                if (gameplaySession.sessionType == SessionType.LOCAL) {
                    localSessionActionJob
                } else {
                    EmptyCoroutineContext
                }
            viewModelScope.launch(actionContext) {
                action(gameplaySession)
            }
        }

        private fun resetLocalSessionActions() {
            localSessionActionJob.cancel()
            localSessionActionJob = newLocalSessionActionJob()
        }

        private fun newLocalSessionActionJob(): Job = SupervisorJob(viewModelScope.coroutineContext[Job])

        private fun List<PlayerScore>.withDisplayNames(): List<PlayerScore> =
            map { score ->
                score.copy(
                    name = profileDisplayMapper.displayName(score.name, score.ownerIndex, score.colorIndex),
                    colorIndex = profileDisplayMapper.visualColorIndex(score.colorIndex),
                )
            }.toSnapshotList()

        private fun GameState.rankedScoresForUiAndHistory(): List<PlayerScore> {
            if (!isGameOver) return Scoring.rankPlayers(this).withDisplayNames()
            finishedGameRanking
                ?.takeIf { ranking -> ranking.state == this }
                ?.let { ranking -> return ranking.scores }
            return Scoring.rankPlayers(this).withDisplayNames().also { scores ->
                finishedGameRanking = FinishedGameRanking(state = this, scores = scores)
            }
        }

        private fun Profile.toUiState(): ProfileUiState =
            ProfileUiState(
                id = id,
                name = name,
                colorIndex = colorIndex,
                avatarStyle = avatarStyle,
                active = active,
            )

        private fun profilesUiState(): List<ProfileUiState> =
            profiles
                .map { profile -> profile.toUiState() }
                .toSnapshotList()

        private fun hallOfFameUiState(): Map<GameMode?, List<HallOfFameEntry>> {
            val cached = hallOfFameCache
            if (cached != null && cached.first === profiles) return cached.second
            val computed =
                buildMap<GameMode?, List<HallOfFameEntry>> {
                    put(null, HallOfFameCalculator.topEntries(profiles))
                    GameMode.entries.forEach { mode ->
                        put(mode, HallOfFameCalculator.topEntries(profiles, mode))
                    }
                }
            hallOfFameCache = profiles to computed
            return computed
        }

        private fun rivalsUiState(): List<RivalUiState> {
            val profile = activeProfile()
            val wins = profile?.rivalWins.orEmpty()
            val losses = profile?.rivalLosses.orEmpty()
            val nextChallengerId = OpponentRoster.nextChallenger(wins)?.id
            return OpponentRoster.all
                .map { rival ->
                    RivalUiState(
                        id = rival.id,
                        name = rival.name,
                        tier = rival.tier,
                        style = rival.style,
                        colorIndex = rival.colorIndex,
                        wins = wins[rival.id] ?: 0,
                        losses = losses[rival.id] ?: 0,
                        unlocked = OpponentRoster.isUnlocked(rival.id, wins),
                        isNextChallenger = rival.id == nextChallengerId,
                    )
                }.toSnapshotList()
        }

        private data class FinishedGameRanking(
            val state: GameState,
            val scores: List<PlayerScore>,
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

        private fun emitAcceptedEffect(
            gameplaySession: GameSession,
            stateBefore: GameState,
        ) {
            val stateAfter = gameplaySession.gameState.value
            val playerBefore = stateBefore.players[stateBefore.currentPlayerIndex]
            val playerAfter = stateAfter.players[playerBefore.index]
            val delta = playerAfter.scoreBreakdown.total - playerBefore.scoreBreakdown.total
            if (delta > 0) {
                _effects.tryEmit(
                    GameEffect.MoveAccepted(
                        playerName =
                            profileDisplayMapper.displayName(
                                playerBefore.name,
                                playerBefore.ownerIndex,
                                playerBefore.colorIndex,
                            ),
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

        private fun GameSessionEvent.toEffect(): GameEffect =
            when (this) {
                is GameSessionEvent.MoveRejected -> GameEffect.MoveRejected(reason)
                is GameSessionEvent.ActionFailed -> GameEffect.ActionFailed(message)
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
                name = defaultProfileName(),
                colorIndex = DEFAULT_PROFILE_OWNER_INDEX,
                avatarStyle = LocalAvatarStyle.INITIALS,
                avatarSeed = DEFAULT_PROFILE_ID,
                active = true,
            )

        private fun defaultProfileName(): String = stringProvider.getString(R.string.default_profile_name)

        private fun GameSettings.normalized(): GameSettings =
            copy(preferredDifficulty = OpponentDifficultyMapper.toPersistedLevel(preferredDifficulty))

        private fun elapsedGameSeconds(): Int =
            ((timeProvider.nowEpochMillis() - gameStartedAtMillis) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)
    }

private const val DEFAULT_PROFILE_ID = "local-default"
private const val MILLIS_PER_SECOND = 1_000L

internal fun Result<Unit>.toFailureEffect(stringProvider: StringProvider): GameEffect =
    when (val error = exceptionOrNull()) {
        is MoveRejectedException -> {
            GameEffect.MoveRejected(error.reason)
        }

        else -> {
            GameEffect.ActionFailed(
                error
                    ?.message
                    ?.takeIf { message -> message.isNotBlank() }
                    ?: stringProvider.getString(R.string.action_failed_default_reason),
            )
        }
    }
