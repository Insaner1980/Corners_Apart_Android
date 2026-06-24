package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.data.GameRepository
import com.finnvek.cornersapart.data.InMemoryJsonStateStore
import com.finnvek.cornersapart.data.ProfileRepository
import com.finnvek.cornersapart.data.SettingsRepository
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.INVALID_GAME_STATE_INDEX_DOMAINS
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.multiplayer.ConnectionState
import com.finnvek.cornersapart.multiplayer.ConnectionsClientFacade
import com.finnvek.cornersapart.multiplayer.GameMessage
import com.finnvek.cornersapart.multiplayer.GameProtocol
import com.finnvek.cornersapart.multiplayer.LocalSessionFactory
import com.finnvek.cornersapart.multiplayer.NearbyConnectionLifecycleCallback
import com.finnvek.cornersapart.multiplayer.NearbyConnectionResult
import com.finnvek.cornersapart.multiplayer.NearbyConnectionsCoordinator
import com.finnvek.cornersapart.multiplayer.NearbyEndpointDiscoveryCallback
import com.finnvek.cornersapart.multiplayer.NearbyOperationFailureCallback
import com.finnvek.cornersapart.multiplayer.NearbyPayloadCallback
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateExposesFourPlayerLocalGame() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertEquals(GameMode.FOUR_PLAYER, state.gameMode)
        assertEquals(GameConstants.PLAYER_COUNT, state.players.size)
        assertEquals(PieceCatalog.SINGLE_CELL_ID, state.selectedPieceId)
        assertEquals(GameConstants.PIECE_COUNT, state.pieces.size)
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun placingSelectedPieceUpdatesBoardAndTurn() =
        runTest {
            val viewModel = createViewModel()

            viewModel.placeSelectedAt(row = 0, col = 0)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(0, state.board.get(row = 0, col = 0))
            assertEquals(1, state.currentPlayerIndex)
            assertEquals(BoardSnapshot.EMPTY, state.board.get(row = 0, col = 1))
        }

    @Test
    fun rotateSelectedPieceChangesOrientationIndex() {
        val viewModel = createViewModel()

        viewModel.selectPiece(PieceCatalog.THREE_BEND_ID)
        viewModel.rotateSelectedClockwise()

        assertEquals(PieceCatalog.THREE_BEND_ID, viewModel.uiState.value.selectedPieceId)
        assertEquals(1, viewModel.uiState.value.selectedOrientationIndex)
    }

    @Test
    fun soloGameRunsComputerTurnsBackToHumanPlayer() =
        runTest {
            val viewModel = createViewModel()

            viewModel.startSoloGame()
            viewModel.placeSelectedAt(row = 19, col = 19)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(GameMode.SOLO, state.gameMode)
            assertEquals(0, state.currentPlayerIndex)
            assertTrue(state.players.drop(1).all { player -> player.isComputerControlled })
            assertTrue(state.players.drop(1).all { player -> player.totalScore > 0 })
        }

    @Test
    fun startGameSupportsPassAndPlayModes() {
        val viewModel = createViewModel()

        viewModel.startGame(GameMode.TWO_COLOR_DUEL)

        assertEquals(GameMode.TWO_COLOR_DUEL, viewModel.uiState.value.gameMode)
        assertEquals(
            listOf(0, 1, 0, 1),
            viewModel.uiState.value.players
                .map { player -> player.ownerIndex },
        )

        viewModel.startGame(GameMode.COMPACT_DUEL)

        assertEquals(GameMode.COMPACT_DUEL, viewModel.uiState.value.gameMode)
        assertEquals(GameConstants.COMPACT_BOARD_SIZE, viewModel.uiState.value.board.size)

        viewModel.startGame(GameMode.THREE_PLAYER)

        assertEquals(GameMode.THREE_PLAYER, viewModel.uiState.value.gameMode)
        assertEquals(3, viewModel.uiState.value.players.size)

        viewModel.startFourPlayerGame()

        assertEquals(GameMode.FOUR_PLAYER, viewModel.uiState.value.gameMode)
        assertEquals(GameConstants.PLAYER_COUNT, viewModel.uiState.value.players.size)
    }

    @Test
    fun polishSettingsCanToggleSoundHapticsAndReducedMotion() {
        runTest {
            val harness = createViewModelHarness()
            val viewModel = harness.viewModel

            viewModel.setSoundEnabled(false)
            viewModel.setHapticsEnabled(false)
            viewModel.setReducedMotionEnabled(true)
            viewModel.setPreferredDifficulty(3)
            viewModel.setPreferredMode(GameMode.COMPACT_DUEL)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.soundEnabled)
            assertEquals(false, state.hapticsEnabled)
            assertEquals(true, state.reducedMotionEnabled)
            assertEquals(3, state.preferredDifficulty)
            assertEquals(GameMode.COMPACT_DUEL, state.preferredMode)
        }
    }

    @Test
    fun settingsTogglePersistsAcrossViewModelInstances() =
        runTest {
            val harness = createViewModelHarness()

            harness.viewModel.setSoundEnabled(false)
            advanceUntilIdle()

            val recreated = harness.createViewModel()
            advanceUntilIdle()

            assertEquals(false, recreated.uiState.value.soundEnabled)
            assertEquals(
                false,
                harness.settingsRepository.settings
                    .first()
                    .soundEnabled,
            )
        }

    @Test
    fun startGamePersistsPreferredMode() =
        runTest {
            val harness = createViewModelHarness()

            harness.viewModel.startGame(GameMode.THREE_PLAYER)
            advanceUntilIdle()

            assertEquals(
                GameMode.THREE_PLAYER,
                harness.settingsRepository.settings
                    .first()
                    .preferredMode,
            )
            assertEquals(GameMode.THREE_PLAYER, harness.viewModel.uiState.value.preferredMode)
        }

    @Test
    fun acceptedMoveSavesGameWithSettingsSnapshot() =
        runTest {
            val harness = createViewModelHarness(initialSettings = GameSettings(preferredDifficulty = 4))

            harness.viewModel.placeSelectedAt(row = 0, col = 0)
            advanceUntilIdle()

            val savedGame = harness.gameRepository.savedGameData.first()
            assertEquals(0, savedGame.gameState?.board?.get(row = 0, col = 0))
            assertEquals(4, savedGame.settings.preferredDifficulty)
            assertEquals(FIXED_NOW_MILLIS, savedGame.savedAtEpochMillis)
        }

    @Test
    fun rejectedMoveEmitsRejectedEffect() =
        runTest {
            val viewModel = createViewModel()
            val effect = async { viewModel.effects.first() }

            viewModel.placeSelectedAt(row = 10, col = 10)
            advanceUntilIdle()

            assertTrue(effect.await() is GameEffect.MoveRejected)
        }

    @Test
    fun counterClockwiseRotationAndFlipUpdateOrientation() {
        val viewModel = createViewModel()
        val piece = PieceCatalog.require(PieceCatalog.THREE_BEND_ID)
        val orientationCount = PieceTransforms.getAllOrientations(piece).size

        viewModel.selectPiece(PieceCatalog.THREE_BEND_ID)
        viewModel.rotateSelectedCounterClockwise()

        assertEquals(orientationCount - 1, viewModel.uiState.value.selectedOrientationIndex)

        viewModel.flipSelected()

        assertTrue(viewModel.uiState.value.selectedOrientationIndex in 0 until orientationCount)
    }

    @Test
    fun savedGameCanBeResumedOrDiscarded() =
        runTest {
            val savedState =
                GameEngine().newGame(
                    GameConfig(mode = GameMode.THREE_PLAYER, randomSeed = 91L, bonusTiles = emptyList()),
                )
            val savedSettings = GameSettings(preferredDifficulty = 4, preferredMode = GameMode.SOLO)
            val harness =
                createViewModelHarness(
                    initialSavedGameData =
                        SavedGameData(
                            gameState = savedState,
                            savedAtEpochMillis = 1234L,
                            settings = savedSettings,
                        ),
                )
            advanceUntilIdle()

            assertEquals(true, harness.viewModel.uiState.value.hasSavedGame)
            assertEquals(
                GameMode.THREE_PLAYER,
                harness.viewModel.uiState.value.resumeSummary
                    ?.gameMode,
            )

            harness.viewModel.resumeSavedGame()
            advanceUntilIdle()

            assertEquals(GameMode.THREE_PLAYER, harness.viewModel.uiState.value.gameMode)
            assertEquals(false, harness.viewModel.uiState.value.hasSavedGame)
            assertEquals(null, harness.viewModel.uiState.value.resumeSummary)
            assertEquals(
                4,
                harness.settingsRepository.settings
                    .first()
                    .preferredDifficulty,
            )

            harness.viewModel.discardSavedGameAndStartNewGame()
            advanceUntilIdle()

            assertEquals(null, harness.gameRepository.savedGame.first())
        }

    @Test
    fun invalidSavedGameIndexDomainsAreNotExposedOrResumed() =
        runTest {
            val invalidState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.THREE_PLAYER, randomSeed = 93L, bonusTiles = emptyList()))
                    .copy(currentPlayerIndex = 99)
            val harness =
                createViewModelHarness(
                    initialSavedGameData =
                        SavedGameData(
                            gameState = invalidState,
                            savedAtEpochMillis = 1234L,
                            settings = GameSettings(preferredMode = GameMode.THREE_PLAYER),
                        ),
                )
            advanceUntilIdle()

            assertEquals(false, harness.viewModel.uiState.value.hasSavedGame)

            harness.viewModel.resumeSavedGame()
            advanceUntilIdle()

            assertEquals(GameMode.FOUR_PLAYER, harness.viewModel.uiState.value.gameMode)
            assertEquals(0, harness.viewModel.uiState.value.currentPlayerIndex)
            assertEquals(null, harness.gameRepository.savedGame.first())
        }

    @Test
    fun profileActionsCreateUpdateAndActivateProfiles() =
        runTest {
            val harness = createViewModelHarness()
            val viewModel = harness.viewModel
            advanceUntilIdle()
            val defaultProfileId = checkNotNull(harness.profileRepository.activeProfile.first()).id

            viewModel.addProfile(
                name = "  Ada Lovelace  ",
                colorIndex = 99,
                avatarStyle = LocalAvatarStyle.RINGS,
            )
            advanceUntilIdle()

            val addedProfile = checkNotNull(harness.profileRepository.activeProfile.first())
            assertEquals("Ada Lovelace", addedProfile.name)
            assertEquals(GameConstants.PLAYER_COLORS.lastIndex, addedProfile.colorIndex)
            assertEquals(LocalAvatarStyle.RINGS, addedProfile.avatarStyle)

            viewModel.updateProfile(
                profileId = addedProfile.id,
                name = "   ",
                colorIndex = -1,
                avatarStyle = LocalAvatarStyle.GEOMETRIC,
            )
            advanceUntilIdle()

            val updatedProfile = checkNotNull(harness.profileRepository.activeProfile.first())
            assertEquals("Ada Lovelace", updatedProfile.name)
            assertEquals(0, updatedProfile.colorIndex)
            assertEquals(LocalAvatarStyle.GEOMETRIC, updatedProfile.avatarStyle)
            assertEquals(addedProfile.avatarSeed, updatedProfile.avatarSeed)

            viewModel.setActiveProfile(defaultProfileId)
            advanceUntilIdle()

            assertEquals(
                defaultProfileId,
                harness.profileRepository.activeProfile
                    .first()
                    ?.id,
            )
        }

    @Test
    fun nearbyActionsDelegateToCoordinator() =
        runTest {
            val harness = createViewModelHarness()
            val viewModel = harness.viewModel

            viewModel.startNearbyHosting()
            advanceUntilIdle()

            assertEquals(ConnectionState.CONNECTED, viewModel.uiState.value.nearbyState.connectionState)

            viewModel.startNearbyDiscovery()
            viewModel.connectToNearbyEndpoint("endpoint-1")
            viewModel.acceptPendingNearbyConnection("endpoint-1")
            viewModel.rejectPendingNearbyConnection("endpoint-1")
            viewModel.disconnectNearby()
            advanceUntilIdle()

            assertEquals(ConnectionState.DISCONNECTED, viewModel.uiState.value.nearbyState.connectionState)
        }

    @Test
    fun nearbyHostMovesUseCoordinatorSessionAsAuthoritativeState() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val harness = createViewModelHarness(facade = facade)

            harness.viewModel.startNearbyHosting()
            advanceUntilIdle()
            harness.viewModel.placeSelectedAt(row = 0, col = 0)
            advanceUntilIdle()

            assertEquals(
                0,
                harness.nearbyConnectionsCoordinator.currentSession.value
                    ?.gameState
                    ?.value
                    ?.board
                    ?.get(row = 0, col = 0),
            )
            assertEquals(
                0,
                harness.viewModel.uiState.value.board
                    .get(row = 0, col = 0),
            )
        }

    @Test
    fun nearbyClientFullSyncUpdatesPlayableUiState() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val harness = createViewModelHarness(facade = facade)
            val initialState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 101L, bonusTiles = emptyList()))
            val result =
                GameEngine().applyMove(
                    initialState,
                    Move(
                        playerIndex = 0,
                        pieceId = PieceCatalog.SINGLE_CELL_ID,
                        anchorRow = 0,
                        anchorCol = 0,
                        orientationIndex = 0,
                    ),
                )
            check(result is MoveResult.Accepted)
            val syncedState = result.state

            harness.viewModel.startNearbyDiscovery()
            harness.viewModel.connectToNearbyEndpoint("host-1")
            facade.connectionCallback?.onConnectionInitiated("host-1", "Host", "9876")
            harness.viewModel.acceptPendingNearbyConnection("host-1")
            facade.connectionCallback?.onConnectionResult("host-1", NearbyConnectionResult.Accepted)
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(syncedState)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                0,
                harness.viewModel.uiState.value.board
                    .get(row = 0, col = 0),
            )
        }

    @Test
    fun nearbyClientHostRejectionEmitsRejectedEffect() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val harness = createViewModelHarness(facade = facade)
            val initialState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 103L, bonusTiles = emptyList()))
            val rejectedMove =
                Move(
                    playerIndex = 0,
                    pieceId = PieceCatalog.SINGLE_CELL_ID,
                    anchorRow = 0,
                    anchorCol = 1,
                    orientationIndex = 0,
                )
            val effect = async(start = CoroutineStart.UNDISPATCHED) { harness.viewModel.effects.first() }

            harness.viewModel.startNearbyDiscovery()
            harness.viewModel.connectToNearbyEndpoint("host-1")
            facade.connectionCallback?.onConnectionInitiated("host-1", "Host", "9876")
            harness.viewModel.acceptPendingNearbyConnection("host-1")
            facade.connectionCallback?.onConnectionResult("host-1", NearbyConnectionResult.Accepted)
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(initialState)).encodeToByteArray(),
            )
            advanceUntilIdle()
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol
                    .encode(
                        GameMessage.MoveRejected(
                            move = rejectedMove,
                            reason = MoveRejectionReason.START_CORNER_NOT_COVERED,
                        ),
                    ).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                GameEffect.MoveRejected(MoveRejectionReason.START_CORNER_NOT_COVERED),
                effect.await(),
            )
        }

    @Test
    fun nearbyClientInvalidSyncEmitsActionFailedEffect() =
        runTest {
            val facade = RecordingConnectionsClientFacade()
            val harness = createViewModelHarness(facade = facade)
            val initialState =
                GameEngine()
                    .newGame(GameConfig(mode = GameMode.FOUR_PLAYER, randomSeed = 105L, bonusTiles = emptyList()))
            val invalidState = initialState.copy(currentPlayerIndex = 99)

            harness.viewModel.startNearbyDiscovery()
            harness.viewModel.connectToNearbyEndpoint("host-1")
            facade.connectionCallback?.onConnectionInitiated("host-1", "Host", "9876")
            harness.viewModel.acceptPendingNearbyConnection("host-1")
            facade.connectionCallback?.onConnectionResult("host-1", NearbyConnectionResult.Accepted)
            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(initialState)).encodeToByteArray(),
            )
            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { harness.viewModel.effects.first() }

            facade.payloadCallback?.onBytesPayload(
                "host-1",
                GameProtocol.encode(GameMessage.FullSync(invalidState)).encodeToByteArray(),
            )
            advanceUntilIdle()

            assertEquals(
                GameEffect.ActionFailed(INVALID_GAME_STATE_INDEX_DOMAINS),
                effect.await(),
            )
        }

    @Test
    fun gameOverAppendsHistoryOnceAndClearsSavedGame() =
        runTest {
            val harness = createViewModelHarness()

            repeat(GameConstants.PLAYER_COUNT) {
                harness.viewModel.passCurrentPlayer()
                advanceUntilIdle()
            }
            harness.viewModel.passCurrentPlayer()
            advanceUntilIdle()

            val activeProfile = harness.profileRepository.activeProfile.first()
            assertEquals(1, activeProfile?.history?.size)
            assertEquals(1, activeProfile?.history?.single()?.rank)
            assertEquals(null, harness.gameRepository.savedGame.first())
        }

    @Test
    fun twoColorDuelHistoryAggregatesScoresByOwner() =
        runTest {
            val harness = createViewModelHarness()
            val viewModel = harness.viewModel

            viewModel.startGame(GameMode.TWO_COLOR_DUEL)
            advanceUntilIdle()
            listOf(
                0 to 0,
                0 to 19,
                19 to 19,
                19 to 0,
            ).forEach { (row, col) ->
                viewModel.placeSelectedAt(row = row, col = col)
                advanceUntilIdle()
            }
            repeat(GameConstants.PLAYER_COUNT) {
                viewModel.passCurrentPlayer()
                advanceUntilIdle()
            }

            val historyEntry = checkNotNull(harness.profileRepository.activeProfile.first()).history.single()
            assertEquals(1, historyEntry.rank)
            assertEquals(2, historyEntry.totalScore)
            assertEquals(2, historyEntry.scoreBreakdown.placedCellPoints)
            assertEquals(listOf(0, 1), historyEntry.scores.map { score -> score.ownerIndex })
            assertEquals(listOf("Player 1", "Player 2"), historyEntry.scores.map { score -> score.name })
            assertEquals(listOf(2, 2), historyEntry.scores.map { score -> score.totalScore })
        }

    private fun createViewModel(): GameViewModel = createViewModelHarness().viewModel

    private fun createViewModelHarness(
        initialSettings: GameSettings = GameSettings(),
        initialSavedGameData: SavedGameData = SavedGameData(),
        facade: ConnectionsClientFacade = NoOpConnectionsClientFacade,
    ): GameViewModelHarness {
        val engine = GameEngine()
        val sessionFactory =
            LocalSessionFactory(
                engine = engine,
                opponentEngine =
                    ComputerOpponentEngine(
                        gameEngine = engine,
                        dispatcher = mainDispatcherRule.testDispatcher,
                    ),
            )
        val gameRepository = GameRepository(InMemoryJsonStateStore(initialSavedGameData))
        val profileRepository = ProfileRepository(InMemoryJsonStateStore(ProfilesData()))
        val settingsRepository = SettingsRepository(InMemoryJsonStateStore(initialSettings))
        val nearbyConnectionsCoordinator =
            NearbyConnectionsCoordinator(
                facade = facade,
                gameEngine = engine,
                localEndpointName = "Corners Apart",
            )
        val harness =
            GameViewModelHarness(
                sessionFactory = sessionFactory,
                gameRepository = gameRepository,
                profileRepository = profileRepository,
                settingsRepository = settingsRepository,
                timeProvider = FixedTimeProvider,
                nearbyConnectionsCoordinator = nearbyConnectionsCoordinator,
            )
        harness.viewModel = harness.createViewModel()
        return harness
    }

    private class GameViewModelHarness(
        private val sessionFactory: LocalSessionFactory,
        val gameRepository: GameRepository,
        val profileRepository: ProfileRepository,
        val settingsRepository: SettingsRepository,
        private val timeProvider: TimeProvider,
        val nearbyConnectionsCoordinator: NearbyConnectionsCoordinator,
    ) {
        lateinit var viewModel: GameViewModel

        fun createViewModel(): GameViewModel =
            GameViewModel(
                sessionFactory = sessionFactory,
                gameRepository = gameRepository,
                profileRepository = profileRepository,
                settingsRepository = settingsRepository,
                timeProvider = timeProvider,
                nearbyConnectionsCoordinator = nearbyConnectionsCoordinator,
            )
    }

    private object FixedTimeProvider : TimeProvider {
        override fun nowEpochMillis(): Long = FIXED_NOW_MILLIS

        override fun todayIsoDate(): String = "2026-06-13"
    }

    private companion object {
        const val FIXED_NOW_MILLIS = 1_781_328_000_000L
    }
}

private object NoOpConnectionsClientFacade : ConnectionsClientFacade {
    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun rejectConnection(
        endpointId: String,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun stopDiscovery() = Unit

    override fun stopAdvertising() = Unit

    override fun stopAllEndpoints() = Unit
}

private class RecordingConnectionsClientFacade : ConnectionsClientFacade {
    var connectionCallback: NearbyConnectionLifecycleCallback? = null
    var payloadCallback: NearbyPayloadCallback? = null

    override fun startAdvertising(
        localEndpointName: String,
        serviceId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        connectionCallback = callback
    }

    override fun startDiscovery(
        serviceId: String,
        callback: NearbyEndpointDiscoveryCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        connectionCallback = callback
    }

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
        failureCallback: NearbyOperationFailureCallback,
    ) {
        payloadCallback = callback
    }

    override fun rejectConnection(
        endpointId: String,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
        failureCallback: NearbyOperationFailureCallback,
    ) = Unit

    override fun stopDiscovery() = Unit

    override fun stopAdvertising() = Unit

    override fun stopAllEndpoints() = Unit
}
