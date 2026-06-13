package com.finnvek.cornersapart.ui.screens

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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HistoryEntry
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
    val onShowHistoryStats: () -> Unit = {},
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
    val onReducedMotionEnabledChange: (Boolean) -> Unit = {},
)

data class GameDialogState(
    val accessibilityAnnouncement: String? = null,
    val showHistoryStatsDialog: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val onDismissHistoryStats: () -> Unit = {},
)

@Composable
fun GameRoute(
    viewModel: GameViewModel = hiltViewModel(),
    onRequestNearbyPermissions: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHistoryStats by remember { mutableStateOf(false) }
    var accessibilityAnnouncement by remember { mutableStateOf<AccessibilityAnnouncement?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled by rememberUpdatedState(state.hapticsEnabled)
    val accessibilityAnnouncementText = accessibilityAnnouncement?.toAnnouncementText()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            accessibilityAnnouncement = effect.toAccessibilityAnnouncement()
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
                onCreateNearbyGame = onRequestNearbyPermissions,
                onFindNearbyGame = onRequestNearbyPermissions,
                onShowHistoryStats = { showHistoryStats = true },
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
                onReducedMotionEnabledChange = viewModel::setReducedMotionEnabled,
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
        AccessibilityAnnouncement.GameOver -> stringResource(R.string.accessibility_game_over)
    }

@Composable
fun GameScreenContent(
    state: GameUiState,
    modifier: Modifier = Modifier,
    screenActions: GameScreenActions = GameScreenActions(),
    pieceActions: GamePieceActions = GamePieceActions(),
    settingsActions: GameSettingsActions = GameSettingsActions(),
    dialogState: GameDialogState = GameDialogState(),
) {
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    if (dialogState.showHistoryStatsDialog) {
        HistoryStatsDialog(
            history = dialogState.history,
            onDismiss = dialogState.onDismissHistoryStats,
        )
    }
    if (showSettings) {
        GameSettingsDialog(
            soundEnabled = state.soundEnabled,
            hapticsEnabled = state.hapticsEnabled,
            reducedMotionEnabled = state.reducedMotionEnabled,
            onSoundEnabledChange = settingsActions.onSoundEnabledChange,
            onHapticsEnabledChange = settingsActions.onHapticsEnabledChange,
            onReducedMotionEnabledChange = settingsActions.onReducedMotionEnabledChange,
            onDismiss = { showSettings = false },
        )
    }
    if (showHelp) {
        GameHelpDialog(onDismiss = { showHelp = false })
    }
    if (state.isGameOver) {
        GameOverDialog(
            players = state.players,
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
        when (layoutMode) {
            GameLayoutMode.COMPACT ->
                CompactGameLayout(
                    state = state,
                    accessibilityAnnouncement = dialogState.accessibilityAnnouncement,
                    screenActions = screenActions,
                    pieceActions = pieceActions,
                    onShowSettings = { showSettings = true },
                    onShowHelp = { showHelp = true },
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(CornersApartSpacing.ScreenPadding),
                )
            GameLayoutMode.EXPANDED ->
                ExpandedGameLayout(
                    state = state,
                    accessibilityAnnouncement = dialogState.accessibilityAnnouncement,
                    screenActions = screenActions,
                    pieceActions = pieceActions,
                    onShowSettings = { showSettings = true },
                    onShowHelp = { showHelp = true },
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(CornersApartSpacing.ScreenPadding),
                )
        }
    }
}

@Composable
private fun CompactGameLayout(
    state: GameUiState,
    accessibilityAnnouncement: String?,
    screenActions: GameScreenActions,
    pieceActions: GamePieceActions,
    onShowSettings: () -> Unit,
    onShowHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
    ) {
        Header(state = state, onModeSelected = screenActions.onModeSelected)
        NearbyActions(
            onCreateNearbyGame = screenActions.onCreateNearbyGame,
            onFindNearbyGame = screenActions.onFindNearbyGame,
        )
        UtilityActions(
            onShowHistoryStats = screenActions.onShowHistoryStats,
            onShowSettings = onShowSettings,
            onShowHelp = onShowHelp,
        )
        PlayerScoreBar(players = state.players)
        GameBoard(state = state, onPlaceCell = pieceActions.onPlaceCell)
        AccessibilityAnnouncementNode(accessibilityAnnouncement)
        StatusLine(state)
        ControlBar(
            onRotateCounterClockwise = pieceActions.onRotateCounterClockwise,
            onRotateClockwise = pieceActions.onRotateClockwise,
            onFlip = pieceActions.onFlip,
            onPass = pieceActions.onPass,
        )
        SelectedPiecePreview(state)
        PiecePanel(
            pieces = state.pieces,
            colorIndex = state.currentPlayer.colorIndex,
            onSelectPiece = pieceActions.onSelectPiece,
        )
    }
}

@Composable
private fun ExpandedGameLayout(
    state: GameUiState,
    accessibilityAnnouncement: String?,
    screenActions: GameScreenActions,
    pieceActions: GamePieceActions,
    onShowSettings: () -> Unit,
    onShowHelp: () -> Unit,
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
            Header(state = state, onModeSelected = screenActions.onModeSelected)
            NearbyActions(
                onCreateNearbyGame = screenActions.onCreateNearbyGame,
                onFindNearbyGame = screenActions.onFindNearbyGame,
            )
            UtilityActions(
                onShowHistoryStats = screenActions.onShowHistoryStats,
                onShowSettings = onShowSettings,
                onShowHelp = onShowHelp,
            )
            PlayerScoreBar(players = state.players)
            AccessibilityAnnouncementNode(accessibilityAnnouncement)
            StatusLine(state)
            ControlBar(
                onRotateCounterClockwise = pieceActions.onRotateCounterClockwise,
                onRotateClockwise = pieceActions.onRotateClockwise,
                onFlip = pieceActions.onFlip,
                onPass = pieceActions.onPass,
            )
            SelectedPiecePreview(state)
        }
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        ) {
            GameBoard(state = state, onPlaceCell = pieceActions.onPlaceCell)
            PiecePanel(
                pieces = state.pieces,
                colorIndex = state.currentPlayer.colorIndex,
                onSelectPiece = pieceActions.onSelectPiece,
            )
        }
    }
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
            ModeChip(
                text = stringResource(R.string.game_mode_four_player),
                selected = state.gameMode == GameMode.FOUR_PLAYER,
                onClick = { onModeSelected(GameMode.FOUR_PLAYER) },
            )
            ModeChip(
                text = stringResource(R.string.game_mode_solo),
                selected = state.gameMode == GameMode.SOLO,
                onClick = { onModeSelected(GameMode.SOLO) },
            )
            ModeChip(
                text = stringResource(R.string.game_mode_two_color_duel),
                selected = state.gameMode == GameMode.TWO_COLOR_DUEL,
                onClick = { onModeSelected(GameMode.TWO_COLOR_DUEL) },
            )
            ModeChip(
                text = stringResource(R.string.game_mode_compact_duel),
                selected = state.gameMode == GameMode.COMPACT_DUEL,
                onClick = { onModeSelected(GameMode.COMPACT_DUEL) },
            )
            ModeChip(
                text = stringResource(R.string.game_mode_three_player),
                selected = state.gameMode == GameMode.THREE_PLAYER,
                onClick = { onModeSelected(GameMode.THREE_PLAYER) },
            )
        }
    }
}

@Composable
private fun NearbyActions(
    onCreateNearbyGame: () -> Unit,
    onFindNearbyGame: () -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                Button(onClick = onCreateNearbyGame) {
                    Text(text = stringResource(R.string.create_nearby_game))
                }
                Button(onClick = onFindNearbyGame) {
                    Text(text = stringResource(R.string.find_nearby_game))
                }
            }
        }
    }
}

@Composable
private fun UtilityActions(
    onShowHistoryStats: () -> Unit,
    onShowSettings: () -> Unit,
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
