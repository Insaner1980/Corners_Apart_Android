package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.data.GameRepository
import com.finnvek.cornersapart.data.InMemoryJsonStateStore
import com.finnvek.cornersapart.data.ProfileRepository
import com.finnvek.cornersapart.data.SettingsRepository
import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.ProfilesData
import com.finnvek.cornersapart.model.SavedGameData
import com.finnvek.cornersapart.multiplayer.ConnectionsClientFacade
import com.finnvek.cornersapart.multiplayer.ConnectionState
import com.finnvek.cornersapart.multiplayer.LocalSessionFactory
import com.finnvek.cornersapart.multiplayer.NearbyConnectionLifecycleCallback
import com.finnvek.cornersapart.multiplayer.NearbyConnectionsCoordinator
import com.finnvek.cornersapart.multiplayer.NearbyEndpointDiscoveryCallback
import com.finnvek.cornersapart.multiplayer.NearbyPayloadCallback
import com.finnvek.cornersapart.opponents.ComputerOpponentEngine
import com.google.android.gms.nearby.connection.Strategy
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
            assertEquals(GameMode.THREE_PLAYER, harness.viewModel.uiState.value.resumeSummary?.gameMode)

            harness.viewModel.resumeSavedGame()
            advanceUntilIdle()

            assertEquals(GameMode.THREE_PLAYER, harness.viewModel.uiState.value.gameMode)
            assertEquals(false, harness.viewModel.uiState.value.hasSavedGame)
            assertEquals(null, harness.viewModel.uiState.value.resumeSummary)
            assertEquals(4, harness.settingsRepository.settings.first().preferredDifficulty)

            harness.viewModel.discardSavedGameAndStartNewGame()
            advanceUntilIdle()

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

            assertEquals(defaultProfileId, harness.profileRepository.activeProfile.first()?.id)
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

    private fun createViewModel(): GameViewModel = createViewModelHarness().viewModel

    private fun createViewModelHarness(
        initialSettings: GameSettings = GameSettings(),
        initialSavedGameData: SavedGameData = SavedGameData(),
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
        val harness =
            GameViewModelHarness(
                sessionFactory = sessionFactory,
                gameRepository = gameRepository,
                profileRepository = profileRepository,
                settingsRepository = settingsRepository,
                timeProvider = FixedTimeProvider,
                nearbyConnectionsCoordinator =
                    NearbyConnectionsCoordinator(
                        facade = NoOpConnectionsClientFacade,
                        gameEngine = engine,
                        localEndpointName = "Corners Apart",
                    ),
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
        private val nearbyConnectionsCoordinator: NearbyConnectionsCoordinator,
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
        strategy: Strategy,
        callback: NearbyConnectionLifecycleCallback,
    ) = Unit

    override fun startDiscovery(
        serviceId: String,
        strategy: Strategy,
        callback: NearbyEndpointDiscoveryCallback,
    ) = Unit

    override fun requestConnection(
        localEndpointName: String,
        endpointId: String,
        callback: NearbyConnectionLifecycleCallback,
    ) = Unit

    override fun acceptConnection(
        endpointId: String,
        callback: NearbyPayloadCallback,
    ) = Unit

    override fun rejectConnection(endpointId: String) = Unit

    override fun sendPayload(
        endpointId: String,
        bytes: ByteArray,
    ) = Unit

    override fun stopDiscovery() = Unit

    override fun stopAdvertising() = Unit

    override fun stopAllEndpoints() = Unit
}
