package com.finnvek.cornersapart.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.multiplayer.ConnectionState
import com.finnvek.cornersapart.multiplayer.NearbyEndpointUiState
import com.finnvek.cornersapart.multiplayer.NearbyPendingConnection
import com.finnvek.cornersapart.multiplayer.NearbyPermissions
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.ui.components.PieceShape
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.GameEffect
import com.finnvek.cornersapart.viewmodel.GameUiState
import com.finnvek.cornersapart.viewmodel.GameViewModel
import com.finnvek.cornersapart.viewmodel.PiecePanelItem

data class GameScreenActions(
    val onModeSelected: (GameMode) -> Unit = {},
    val onCreateNearbyGame: () -> Unit = {},
    val onFindNearbyGame: () -> Unit = {},
    val onConnectToNearbyEndpoint: (String) -> Unit = {},
    val onAcceptPendingNearbyConnection: (String) -> Unit = {},
    val onRejectPendingNearbyConnection: (String) -> Unit = {},
    val onShowHistoryStats: () -> Unit = {},
    val onResumeSavedGame: () -> Unit = {},
    val onDiscardSavedGameAndStartNewGame: () -> Unit = {},
)

data class GamePieceActions(
    val onSelectPiece: (String) -> Unit = {},
    val onRotateCounterClockwise: () -> Unit = {},
    val onRotateClockwise: () -> Unit = {},
    val onFlip: () -> Unit = {},
    val onPass: () -> Unit = {},
    val onPlaceCell: (row: Int, col: Int) -> Unit = { _, _ -> },
)

data class GameSettingsActions(
    val onSoundEnabledChange: (Boolean) -> Unit = {},
    val onHapticsEnabledChange: (Boolean) -> Unit = {},
    val onPreferredDifficultyChange: (Int) -> Unit = {},
    val onPreferredModeChange: (GameMode) -> Unit = {},
)

data class GameProfileActions(
    val onSetActiveProfile: (String) -> Unit = {},
    val onAddProfile: (name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit = { _, _, _ -> },
    val onUpdateProfile: (profileId: String, name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit =
        { _, _, _, _ -> },
)

data class GameDialogState(
    val accessibilityAnnouncement: String? = null,
    val showHistoryStatsDialog: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val onDismissHistoryStats: () -> Unit = {},
)

private data class GameLayoutContent(
    val state: GameUiState,
    val accessibilityAnnouncement: String?,
    val screenActions: GameScreenActions,
    val pieceActions: GamePieceActions,
    val onShowSettings: () -> Unit,
    val onShowProfiles: () -> Unit,
    val onShowHelp: () -> Unit,
)

@Composable
fun GameRoute(viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHistoryStats by remember { mutableStateOf(false) }
    var accessibilityAnnouncement by remember { mutableStateOf<AccessibilityAnnouncement?>(null) }
    var pendingNearbyAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val soundPlayer = rememberGameSoundPlayer()
    val hapticsEnabled by rememberUpdatedState(state.hapticsEnabled)
    val soundEnabled by rememberUpdatedState(state.soundEnabled)
    val accessibilityAnnouncementText = accessibilityAnnouncement?.toAnnouncementText()
    val nearbyPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
            val action = pendingNearbyAction
            pendingNearbyAction = null
            if (grantResults.values.all { granted -> granted }) {
                action?.invoke()
            }
        }

    fun runWithNearbyPermissions(action: () -> Unit) {
        val missingPermissions =
            NearbyPermissions
                .requiredRuntimePermissions()
                .filter { permission ->
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                }
        if (missingPermissions.isEmpty()) {
            action()
        } else {
            pendingNearbyAction = action
            nearbyPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            accessibilityAnnouncement = effect.toAccessibilityAnnouncement()
            GameSoundPolicy.eventFor(effect, soundEnabled)?.let(soundPlayer::play)
            if (hapticsEnabled) {
                hapticFeedback.performHapticFeedback(effect.hapticFeedbackType())
            }
        }
    }

    fun performInputHaptic(type: HapticFeedbackType) {
        if (state.hapticsEnabled) {
            hapticFeedback.performHapticFeedback(type)
        }
    }

    GameScreenContent(
        state = state,
        screenActions =
            GameScreenActions(
                onModeSelected = viewModel::startGame,
                onCreateNearbyGame = { runWithNearbyPermissions(viewModel::startNearbyHosting) },
                onFindNearbyGame = { runWithNearbyPermissions(viewModel::startNearbyDiscovery) },
                onConnectToNearbyEndpoint = viewModel::connectToNearbyEndpoint,
                onAcceptPendingNearbyConnection = viewModel::acceptPendingNearbyConnection,
                onRejectPendingNearbyConnection = viewModel::rejectPendingNearbyConnection,
                onShowHistoryStats = { showHistoryStats = true },
                onResumeSavedGame = viewModel::resumeSavedGame,
                onDiscardSavedGameAndStartNewGame = viewModel::discardSavedGameAndStartNewGame,
            ),
        pieceActions =
            GamePieceActions(
                onSelectPiece = { pieceId ->
                    performInputHaptic(HapticFeedbackType.TextHandleMove)
                    viewModel.selectPiece(pieceId)
                },
                onRotateCounterClockwise = {
                    performInputHaptic(HapticFeedbackType.TextHandleMove)
                    viewModel.rotateSelectedCounterClockwise()
                },
                onRotateClockwise = {
                    performInputHaptic(HapticFeedbackType.TextHandleMove)
                    viewModel.rotateSelectedClockwise()
                },
                onFlip = {
                    performInputHaptic(HapticFeedbackType.TextHandleMove)
                    viewModel.flipSelected()
                },
                onPass = {
                    performInputHaptic(HapticFeedbackType.LongPress)
                    viewModel.passCurrentPlayer()
                },
                onPlaceCell = viewModel::placeSelectedAt,
            ),
        settingsActions =
            GameSettingsActions(
                onSoundEnabledChange = viewModel::setSoundEnabled,
                onHapticsEnabledChange = viewModel::setHapticsEnabled,
                onPreferredDifficultyChange = viewModel::setPreferredDifficulty,
                onPreferredModeChange = viewModel::setPreferredMode,
            ),
        profileActions =
            GameProfileActions(
                onSetActiveProfile = viewModel::setActiveProfile,
                onAddProfile = viewModel::addProfile,
                onUpdateProfile = viewModel::updateProfile,
            ),
        dialogState =
            GameDialogState(
                accessibilityAnnouncement = accessibilityAnnouncementText,
                showHistoryStatsDialog = showHistoryStats,
                onDismissHistoryStats = { showHistoryStats = false },
            ),
    )
}

private sealed interface AccessibilityAnnouncement {
    data class ScoreGained(
        val playerName: String,
        val scoreDelta: Int,
        val bonusTileClaimed: Boolean,
    ) : AccessibilityAnnouncement

    data object MoveRejected : AccessibilityAnnouncement

    data class ActionFailed(
        val message: String,
    ) : AccessibilityAnnouncement

    data object GameOver : AccessibilityAnnouncement
}

private fun GameEffect.toAccessibilityAnnouncement(): AccessibilityAnnouncement =
    when (this) {
        is GameEffect.MoveAccepted ->
            AccessibilityAnnouncement.ScoreGained(
                playerName = playerName,
                scoreDelta = scoreDelta,
                bonusTileClaimed = bonusTileClaimed,
            )
        is GameEffect.MoveRejected -> AccessibilityAnnouncement.MoveRejected
        is GameEffect.ActionFailed -> AccessibilityAnnouncement.ActionFailed(message)
        GameEffect.GameOver -> AccessibilityAnnouncement.GameOver
    }

private fun GameEffect.hapticFeedbackType(): HapticFeedbackType =
    when (this) {
        is GameEffect.MoveAccepted ->
            if (bonusTileClaimed) {
                HapticFeedbackType.LongPress
            } else {
                HapticFeedbackType.TextHandleMove
            }
        is GameEffect.MoveRejected,
        is GameEffect.ActionFailed,
        GameEffect.GameOver,
        -> HapticFeedbackType.LongPress
    }

@Composable
private fun AccessibilityAnnouncement.toAnnouncementText(): String =
    when (this) {
        is AccessibilityAnnouncement.ScoreGained -> {
            val scoreText = pluralStringResource(R.plurals.points_count, scoreDelta, scoreDelta)
            val bonusText =
                pluralStringResource(
                    R.plurals.points_count,
                    GameConstants.BONUS_TILE_POINTS,
                    GameConstants.BONUS_TILE_POINTS,
                )
            if (bonusTileClaimed) {
                stringResource(
                    R.string.accessibility_bonus_claimed,
                    playerName,
                    scoreText,
                    bonusText,
                )
            } else {
                stringResource(R.string.accessibility_score_gained, playerName, scoreText)
            }
        }
        AccessibilityAnnouncement.MoveRejected -> stringResource(R.string.accessibility_move_rejected)
        is AccessibilityAnnouncement.ActionFailed -> stringResource(R.string.accessibility_action_failed, message)
        AccessibilityAnnouncement.GameOver -> stringResource(R.string.accessibility_game_over)
    }

@Composable
fun GameScreenContent(
    state: GameUiState,
    modifier: Modifier = Modifier,
    screenActions: GameScreenActions = GameScreenActions(),
    pieceActions: GamePieceActions = GamePieceActions(),
    settingsActions: GameSettingsActions = GameSettingsActions(),
    profileActions: GameProfileActions = GameProfileActions(),
    dialogState: GameDialogState = GameDialogState(),
) {
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showProfiles by remember { mutableStateOf(false) }
    if (state.hasSavedGame && state.resumeSummary != null) {
        ResumeGameDialog(
            summary = state.resumeSummary,
            onContinue = screenActions.onResumeSavedGame,
            onNewGame = screenActions.onDiscardSavedGameAndStartNewGame,
        )
    }
    if (dialogState.showHistoryStatsDialog) {
        HistoryStatsDialog(
            history = state.history,
            onDismiss = dialogState.onDismissHistoryStats,
        )
    }
    if (showSettings) {
        GameSettingsDialog(
            settings =
                GameSettingsDialogState(
                    soundEnabled = state.soundEnabled,
                    hapticsEnabled = state.hapticsEnabled,
                    preferredDifficulty = state.preferredDifficulty,
                    preferredMode = state.preferredMode,
                ),
            actions = settingsActions,
            onDismiss = { showSettings = false },
        )
    }
    if (showProfiles) {
        ProfilesDialog(
            profiles = state.profiles,
            onSetActiveProfile = profileActions.onSetActiveProfile,
            onAddProfile = profileActions.onAddProfile,
            onUpdateProfile = profileActions.onUpdateProfile,
            onDismiss = { showProfiles = false },
        )
    }
    if (showHelp) {
        GameHelpDialog(onDismiss = { showHelp = false })
    }
    if (state.isGameOver) {
        GameOverDialog(
            rankedScores = state.rankedScores,
            durationSeconds = state.gameDurationSeconds,
            onPlayAgain = { screenActions.onModeSelected(state.gameMode) },
            onShowStats = screenActions.onShowHistoryStats,
        )
    }
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(CornersApartColors.AppBackground)
                .safeDrawingPadding(),
    ) {
        val scrollState = rememberScrollState()
        val layoutMode = GameLayoutPolicy.modeForWidthDp(maxWidth.value.toInt())
        val layoutContent =
            GameLayoutContent(
                state = state,
                accessibilityAnnouncement = dialogState.accessibilityAnnouncement,
                screenActions = screenActions,
                pieceActions = pieceActions,
                onShowSettings = { showSettings = true },
                onShowProfiles = { showProfiles = true },
                onShowHelp = { showHelp = true },
            )
        val layoutModifier =
            Modifier
                .verticalScroll(scrollState)
                .padding(CornersApartSpacing.ScreenPadding)
        when (layoutMode) {
            GameLayoutMode.COMPACT ->
                CompactGameLayout(
                    content = layoutContent,
                    modifier = layoutModifier,
                )
            GameLayoutMode.EXPANDED ->
                ExpandedGameLayout(
                    content = layoutContent,
                    modifier = layoutModifier,
                )
        }
    }
}

@Composable
private fun CompactGameLayout(
    content: GameLayoutContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
    ) {
        GameHeaderActions(content)
        GameBoard(state = content.state, onPlaceCell = content.pieceActions.onPlaceCell)
        AccessibilityAnnouncementNode(content.accessibilityAnnouncement)
        StatusLine(content.state)
        ControlBar(content.pieceActions)
        SelectedPiecePreview(content.state)
        PiecePanel(
            pieces = content.state.pieces,
            colorIndex = content.state.currentPlayer.colorIndex,
            onSelectPiece = content.pieceActions.onSelectPiece,
        )
    }
}

@Composable
private fun ExpandedGameLayout(
    content: GameLayoutContent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        ) {
            GameHeaderActions(content)
            AccessibilityAnnouncementNode(content.accessibilityAnnouncement)
            StatusLine(content.state)
            ControlBar(content.pieceActions)
            SelectedPiecePreview(content.state)
        }
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        ) {
            GameBoard(state = content.state, onPlaceCell = content.pieceActions.onPlaceCell)
            PiecePanel(
                pieces = content.state.pieces,
                colorIndex = content.state.currentPlayer.colorIndex,
                onSelectPiece = content.pieceActions.onSelectPiece,
            )
        }
    }
}

@Composable
private fun GameHeaderActions(content: GameLayoutContent) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Header(state = content.state, onModeSelected = content.screenActions.onModeSelected)
        NearbyActions(
            nearbyState = content.state.nearbyState,
            onCreateNearbyGame = content.screenActions.onCreateNearbyGame,
            onFindNearbyGame = content.screenActions.onFindNearbyGame,
            onConnectToNearbyEndpoint = content.screenActions.onConnectToNearbyEndpoint,
            onAcceptPendingNearbyConnection = content.screenActions.onAcceptPendingNearbyConnection,
            onRejectPendingNearbyConnection = content.screenActions.onRejectPendingNearbyConnection,
        )
        UtilityActions(
            onShowHistoryStats = content.screenActions.onShowHistoryStats,
            onShowSettings = content.onShowSettings,
            onShowProfiles = content.onShowProfiles,
            onShowHelp = content.onShowHelp,
        )
        PlayerScoreBar(players = content.state.players)
    }
}

@Composable
private fun ControlBar(pieceActions: GamePieceActions) {
    ControlBar(
        onRotateCounterClockwise = pieceActions.onRotateCounterClockwise,
        onRotateClockwise = pieceActions.onRotateClockwise,
        onFlip = pieceActions.onFlip,
        onPass = pieceActions.onPass,
    )
}

@Composable
private fun Header(
    state: GameUiState,
    onModeSelected: (GameMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = CornersApartColors.TextPrimary,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        ) {
            GameModeUiOptions.modes.forEach { mode ->
                ModeChip(
                    text = stringResource(mode.labelRes()),
                    selected = state.gameMode == mode,
                    onClick = { onModeSelected(mode) },
                )
            }
        }
    }
}

@Composable
private fun NearbyActions(
    nearbyState: NearbyUiState,
    onCreateNearbyGame: () -> Unit,
    onFindNearbyGame: () -> Unit,
    onConnectToNearbyEndpoint: (String) -> Unit,
    onAcceptPendingNearbyConnection: (String) -> Unit,
    onRejectPendingNearbyConnection: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        ) {
            Text(
                text = stringResource(R.string.nearby_game),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(nearbyState.connectionState.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
            )
            nearbyState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                Button(onClick = onCreateNearbyGame) {
                    Text(text = stringResource(R.string.create_nearby_game))
                }
                Button(onClick = onFindNearbyGame) {
                    Text(text = stringResource(R.string.find_nearby_game))
                }
            }
            nearbyState.pendingConnection?.let { pendingConnection ->
                NearbyPendingConnectionActions(
                    pendingConnection = pendingConnection,
                    onAccept = onAcceptPendingNearbyConnection,
                    onReject = onRejectPendingNearbyConnection,
                )
            }
            NearbyEndpointList(
                endpoints = nearbyState.discoveredEndpoints,
                onConnect = onConnectToNearbyEndpoint,
            )
        }
    }
}

@Composable
private fun NearbyPendingConnectionActions(
    pendingConnection: NearbyPendingConnection,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.nearby_pending_connection, pendingConnection.endpointName))
        Text(text = stringResource(R.string.nearby_authentication_code, pendingConnection.authenticationToken))
        Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            Button(onClick = { onAccept(pendingConnection.endpointId) }) {
                Text(text = stringResource(R.string.nearby_accept_connection))
            }
            Button(onClick = { onReject(pendingConnection.endpointId) }) {
                Text(text = stringResource(R.string.nearby_reject_connection))
            }
        }
    }
}

@Composable
private fun NearbyEndpointList(
    endpoints: List<NearbyEndpointUiState>,
    onConnect: (String) -> Unit,
) {
    if (endpoints.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.nearby_discovered_endpoints))
        endpoints.forEach { endpoint ->
            Button(onClick = { onConnect(endpoint.endpointId) }) {
                Text(text = stringResource(R.string.nearby_connect_endpoint, endpoint.endpointName))
            }
        }
    }
}

@StringRes
private fun ConnectionState.labelRes(): Int =
    when (this) {
        ConnectionState.DISCONNECTED -> R.string.nearby_connection_disconnected
        ConnectionState.CONNECTED -> R.string.nearby_connection_connected
        ConnectionState.RECONNECTING -> R.string.nearby_connection_reconnecting
        ConnectionState.FAILED -> R.string.nearby_connection_failed
    }

@Composable
private fun UtilityActions(
    onShowHistoryStats: () -> Unit,
    onShowSettings: () -> Unit,
    onShowProfiles: () -> Unit,
    onShowHelp: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onShowHistoryStats,
            modifier = Modifier.heightIn(min = CornersApartSpacing.TouchTargetMin),
        ) {
            Icon(Icons.Filled.History, contentDescription = null)
            Text(text = stringResource(R.string.history_stats_title))
        }
        Button(
            onClick = onShowProfiles,
            modifier = Modifier.heightIn(min = CornersApartSpacing.TouchTargetMin),
        ) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Text(text = stringResource(R.string.profiles_title))
        }
        Button(
            onClick = onShowSettings,
            modifier = Modifier.heightIn(min = CornersApartSpacing.TouchTargetMin),
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Text(text = stringResource(R.string.settings_title))
        }
        Button(
            onClick = onShowHelp,
            modifier = Modifier.heightIn(min = CornersApartSpacing.TouchTargetMin),
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
            Text(text = stringResource(R.string.help_title))
        }
    }
}

@Composable
private fun AccessibilityAnnouncementNode(accessibilityAnnouncement: String?) {
    if (accessibilityAnnouncement == null) return
    Box(
        modifier =
            Modifier
                .size(CornersApartSpacing.TinyGap)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = accessibilityAnnouncement
                },
    )
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
}

@Composable
private fun StatusLine(state: GameUiState) {
    val text =
        if (state.isGameOver) {
            stringResource(R.string.game_status_game_over)
        } else {
            stringResource(R.string.game_status_turn, state.currentPlayer.name)
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ControlBar(
    onRotateCounterClockwise: () -> Unit,
    onRotateClockwise: () -> Unit,
    onFlip: () -> Unit,
    onPass: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIconButton(
            contentDescription = stringResource(R.string.control_rotate_counterclockwise),
            onClick = onRotateCounterClockwise,
        ) {
            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null)
        }
        GameIconButton(
            contentDescription = stringResource(R.string.control_rotate_clockwise),
            onClick = onRotateClockwise,
        ) {
            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
        }
        GameIconButton(
            contentDescription = stringResource(R.string.control_flip),
            onClick = onFlip,
        ) {
            Icon(Icons.Filled.Flip, contentDescription = null)
        }
        PassButton(onPass)
    }
}

@Composable
private fun GameIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .sizeIn(
                    minWidth = CornersApartSpacing.TouchTargetMin,
                    minHeight = CornersApartSpacing.TouchTargetMin,
                ).semantics { this.contentDescription = contentDescription },
    ) {
        icon()
    }
}

@Composable
private fun RowScope.PassButton(onPass: () -> Unit) {
    val description = stringResource(R.string.control_pass)
    Button(
        onClick = onPass,
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = CornersApartSpacing.TouchTargetMin)
                .semantics { contentDescription = description },
    ) {
        Icon(Icons.Filled.SkipNext, contentDescription = null)
        Text(text = description)
    }
}

@Composable
private fun SelectedPiecePreview(state: GameUiState) {
    val colors = CornersApartPlayerPalette.colorsFor(state.currentPlayer.colorIndex)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.selected_piece_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Box(
                modifier =
                    Modifier
                        .size(CornersApartSpacing.PiecePreviewSize)
                        .border(CornersApartSpacing.ActivePlayerBorderWidth, colors.base, MaterialTheme.shapes.small)
                        .padding(CornersApartSpacing.CompactGap),
            ) {
                PieceShape(
                    cells = state.selectedCells,
                    colorIndex = state.currentPlayer.colorIndex,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PiecePanel(
    pieces: List<PiecePanelItem>,
    colorIndex: Int,
    onSelectPiece: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Text(
            text = stringResource(R.string.piece_panel_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        ) {
            pieces.forEach { item ->
                PieceCard(
                    item = item,
                    colorIndex = colorIndex,
                    onSelectPiece = onSelectPiece,
                )
            }
        }
    }
}

@Composable
private fun PieceCard(
    item: PiecePanelItem,
    colorIndex: Int,
    onSelectPiece: (String) -> Unit,
) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    val description =
        if (item.isUsed) {
            stringResource(R.string.piece_used_content_description, item.piece.displayName)
        } else {
            stringResource(R.string.piece_content_description, item.piece.displayName)
        }
    Surface(
        modifier =
            Modifier
                .size(CornersApartSpacing.PieceCardSize)
                .alpha(if (item.isUsed) CornersApartAlpha.UsedPiece else 1f)
                .semantics { contentDescription = description }
                .clickable(enabled = !item.isUsed) { onSelectPiece(item.piece.id) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border =
            if (item.isSelected) {
                BorderStroke(CornersApartSpacing.ActivePlayerBorderWidth, colors.base)
            } else {
                null
            },
    ) {
        Box(modifier = Modifier.padding(CornersApartSpacing.TinyGap)) {
            PieceShape(
                cells = item.piece.cells,
                colorIndex = colorIndex,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
