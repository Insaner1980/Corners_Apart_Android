package com.finnvek.cornersapart.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.CellOffset
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.multiplayer.ConnectionState
import com.finnvek.cornersapart.multiplayer.NearbyEndpointUiState
import com.finnvek.cornersapart.multiplayer.NearbyPendingConnection
import com.finnvek.cornersapart.multiplayer.NearbyPermissions
import com.finnvek.cornersapart.multiplayer.NearbyUiState
import com.finnvek.cornersapart.multiplayer.SessionType
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyChip
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.components.CandyIconButton
import com.finnvek.cornersapart.ui.components.PieceShape
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.candyBackground
import com.finnvek.cornersapart.ui.theme.withCandyShadow
import com.finnvek.cornersapart.viewmodel.GameEffect
import com.finnvek.cornersapart.viewmodel.GameUiState
import com.finnvek.cornersapart.viewmodel.GameViewModel
import com.finnvek.cornersapart.viewmodel.PiecePanelItem
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class GameScreenActions(
    val onModeSelected: (GameMode) -> Unit = {},
    val onStartChallengeLevel: (Int) -> Unit = {},
    val onStartDailyChallenge: () -> Unit = {},
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
    val isPlacementLegal: (row: Int, col: Int) -> Boolean = { _, _ -> true },
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
    val onDeleteProfile: (String) -> Unit = {},
)

data class GameDialogState(
    val accessibilityAnnouncement: String? = null,
    val statusNotice: String? = null,
    val scoreNotice: String? = null,
    val showHistoryStatsDialog: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val onDismissHistoryStats: () -> Unit = {},
)

private data class GameLayoutContent(
    val state: GameUiState,
    val accessibilityAnnouncement: String?,
    val statusNotice: String?,
    val scoreNotice: String?,
    val screenActions: GameScreenActions,
    val pieceActions: GamePieceActions,
    val dragController: BoardDragController,
    val onShowSettings: () -> Unit,
    val onShowProfiles: () -> Unit,
    val onShowHelp: () -> Unit,
    val onShowChallenges: () -> Unit,
)

/**
 * Välittää palavalikoimasta alkavan raahauksen sormen sijainnin laudalle:
 * laskee esikatselusolun ja pudotuksen laudan koordinaateissa.
 */
@Stable
internal class BoardDragController {
    var rootCoordinates: LayoutCoordinates? = null
    var boardCoordinates: LayoutCoordinates? = null
    var boardCellCount: Int = 0
    var onPlaceCell: (row: Int, col: Int) -> Unit = { _, _ -> }
    var dragCells: List<CellOffset>? by mutableStateOf(null)
    var fingerInRoot: Offset? by mutableStateOf(null)
    var previewAnchor: CellPosition? by mutableStateOf(null)

    fun startDrag(cells: List<CellOffset>) {
        dragCells = cells
    }

    fun updateFinger(
        sourceCoordinates: LayoutCoordinates,
        positionInSource: Offset,
    ) {
        val root = rootCoordinates ?: return
        fingerInRoot = root.localPositionOf(sourceCoordinates, positionInSource)
        previewAnchor = boardAnchorOf(sourceCoordinates, positionInSource)
    }

    fun drop(
        sourceCoordinates: LayoutCoordinates,
        positionInSource: Offset,
    ) {
        val anchor = boardAnchorOf(sourceCoordinates, positionInSource)
        clear()
        if (anchor != null) {
            onPlaceCell(anchor.row, anchor.col)
        }
    }

    fun clear() {
        dragCells = null
        fingerInRoot = null
        previewAnchor = null
    }

    private fun boardAnchorOf(
        sourceCoordinates: LayoutCoordinates,
        positionInSource: Offset,
    ): CellPosition? {
        val board = boardCoordinates ?: return null
        if (boardCellCount <= 0) return null
        val local = board.localPositionOf(sourceCoordinates, positionInSource)
        val insideBoard =
            local.x in 0f..board.size.width.toFloat() &&
                local.y in 0f..board.size.height.toFloat()
        if (!insideBoard) return null
        return liftedBoardAnchor(
            position = local,
            boardSize = boardCellCount,
            boardWidthPx = board.size.width.toFloat(),
            offsets = dragCells.orEmpty(),
        )
    }
}

private enum class PendingNearbyAction {
    Host,
    Discover,
}

private fun PendingNearbyAction.run(viewModel: GameViewModel) {
    when (this) {
        PendingNearbyAction.Host -> viewModel.startNearbyHosting()
        PendingNearbyAction.Discover -> viewModel.startNearbyDiscovery()
    }
}

@Composable
fun GameRoute(viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHistoryStats by remember { mutableStateOf(false) }
    var accessibilityAnnouncement by remember { mutableStateOf<AccessibilityAnnouncement?>(null) }
    var announcementId by remember { mutableIntStateOf(0) }
    var noticeVisible by remember { mutableStateOf(false) }
    var pendingNearbyAction by rememberSaveable { mutableStateOf<PendingNearbyAction?>(null) }
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
                action?.run(viewModel)
            }
        }

    fun runWithNearbyPermissions(action: PendingNearbyAction) {
        val missingPermissions =
            NearbyPermissions
                .requiredRuntimePermissions()
                .filter { permission ->
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                }
        if (missingPermissions.isEmpty()) {
            action.run(viewModel)
        } else {
            pendingNearbyAction = action
            nearbyPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            accessibilityAnnouncement = effect.toAccessibilityAnnouncement()
            announcementId += 1
            GameSoundPolicy.eventFor(effect, soundEnabled)?.let(soundPlayer::play)
            if (hapticsEnabled) {
                hapticFeedback.performHapticFeedback(effect.hapticFeedbackType())
            }
        }
    }

    val statusNotice =
        when (accessibilityAnnouncement) {
            is AccessibilityAnnouncement.MoveRejected,
            is AccessibilityAnnouncement.ActionFailed,
            -> accessibilityAnnouncementText
            else -> null
        }
    val scoreNotice =
        (accessibilityAnnouncement as? AccessibilityAnnouncement.ScoreGained)?.let { gained ->
            if (gained.bonusTileClaimed) {
                "+${gained.scoreDelta} ♦"
            } else {
                "+${gained.scoreDelta}"
            }
        }
    LaunchedEffect(announcementId) {
        if (statusNotice != null || scoreNotice != null) {
            noticeVisible = true
            delay(if (scoreNotice != null) SCORE_NOTICE_DURATION_MS else STATUS_NOTICE_DURATION_MS)
            noticeVisible = false
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
                onStartChallengeLevel = viewModel::startChallengeLevel,
                onStartDailyChallenge = viewModel::startDailyChallenge,
                onCreateNearbyGame = { runWithNearbyPermissions(PendingNearbyAction.Host) },
                onFindNearbyGame = { runWithNearbyPermissions(PendingNearbyAction.Discover) },
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
                isPlacementLegal = viewModel::isPlacementLegal,
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
                onDeleteProfile = viewModel::deleteProfile,
            ),
        dialogState =
            GameDialogState(
                accessibilityAnnouncement = accessibilityAnnouncementText,
                statusNotice = if (noticeVisible) statusNotice else null,
                scoreNotice = if (noticeVisible) scoreNotice else null,
                showHistoryStatsDialog = showHistoryStats,
                onDismissHistoryStats = { showHistoryStats = false },
            ),
    )
}

private const val STATUS_NOTICE_DURATION_MS = 2500L
private const val SCORE_NOTICE_DURATION_MS = 1200L

private sealed interface AccessibilityAnnouncement {
    data class ScoreGained(
        val playerName: String,
        val scoreDelta: Int,
        val bonusTileClaimed: Boolean,
    ) : AccessibilityAnnouncement

    data class MoveRejected(
        val reason: MoveRejectionReason,
    ) : AccessibilityAnnouncement

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
        is GameEffect.MoveRejected -> AccessibilityAnnouncement.MoveRejected(reason)
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

@StringRes
private fun MoveRejectionReason.messageRes(): Int =
    when (this) {
        MoveRejectionReason.START_CORNER_NOT_COVERED -> R.string.move_rejected_start_corner
        MoveRejectionReason.SAME_PLAYER_EDGE_TOUCH -> R.string.move_rejected_edge_touch
        MoveRejectionReason.NO_DIAGONAL_TOUCH -> R.string.move_rejected_no_diagonal
        MoveRejectionReason.CELL_OCCUPIED -> R.string.move_rejected_occupied
        MoveRejectionReason.OUT_OF_BOUNDS -> R.string.move_rejected_out_of_bounds
        MoveRejectionReason.PIECE_ALREADY_USED -> R.string.move_rejected_piece_used
        MoveRejectionReason.GAME_OVER,
        MoveRejectionReason.NOT_PLAYERS_TURN,
        MoveRejectionReason.INVALID_PLAYER,
        MoveRejectionReason.PLAYER_HAS_PASSED,
        MoveRejectionReason.UNKNOWN_PIECE,
        MoveRejectionReason.UNKNOWN_ORIENTATION,
        -> R.string.accessibility_move_rejected
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
        is AccessibilityAnnouncement.MoveRejected -> stringResource(reason.messageRes())
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
    var showChallenges by remember { mutableStateOf(false) }
    if (showChallenges) {
        ChallengeDialog(
            challengeStars = state.challengeStars,
            onStartLevel = screenActions.onStartChallengeLevel,
            onStartDaily = screenActions.onStartDailyChallenge,
            onDismiss = { showChallenges = false },
            dailyBestScore = state.dailyBestScore,
        )
    }
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
            unlockedAchievements = state.unlockedAchievements,
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
            onDeleteProfile = profileActions.onDeleteProfile,
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
            onPlayAgain = {
                val challengeLevel = state.activeChallengeLevel
                if (challengeLevel != null) {
                    screenActions.onStartChallengeLevel(challengeLevel)
                } else {
                    screenActions.onModeSelected(state.gameMode)
                }
            },
            onShowStats = screenActions.onShowHistoryStats,
            challengeResult = state.challengeResult,
            isNewBestScore = state.isNewBestScore,
            newAchievements = state.newAchievements,
            dailyBestScore = if (state.isDailyChallenge) state.dailyBestScore else null,
        )
    }
    val dragController = remember { BoardDragController() }
    dragController.boardCellCount = state.board.size
    dragController.onPlaceCell = pieceActions.onPlaceCell
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .candyBackground()
                .safeDrawingPadding()
                .onGloballyPositioned { dragController.rootCoordinates = it },
    ) {
        val scrollState = rememberScrollState()
        val layoutMode = GameLayoutPolicy.modeForWidthDp(maxWidth.value.toInt())
        val layoutContent =
            GameLayoutContent(
                state = state,
                accessibilityAnnouncement = dialogState.accessibilityAnnouncement,
                statusNotice = dialogState.statusNotice,
                scoreNotice = dialogState.scoreNotice,
                screenActions = screenActions,
                pieceActions = pieceActions,
                dragController = dragController,
                onShowSettings = { showSettings = true },
                onShowProfiles = { showProfiles = true },
                onShowHelp = { showHelp = true },
                onShowChallenges = { showChallenges = true },
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
        DragGhostOverlay(
            dragController = dragController,
            colorIndex = state.currentPlayer.colorIndex,
        )
    }
}

@Composable
private fun DragGhostOverlay(
    dragController: BoardDragController,
    colorIndex: Int,
) {
    val dragCells = dragController.dragCells ?: return
    val finger = dragController.fingerInRoot ?: return
    // Laudan päällä laudan oma esikatselu näyttää palan — kelluva
    // haamupala vain peittäisi sen.
    if (dragController.previewAnchor != null) return
    val previewSizePx = with(LocalDensity.current) { CornersApartSpacing.PiecePreviewSize.toPx() }
    val appear = remember { Animatable(DRAG_GHOST_START_SCALE) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = DRAG_GHOST_SCALE,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        )
    }
    PieceShape(
        cells = dragCells,
        colorIndex = colorIndex,
        modifier =
            Modifier
                .offset {
                    IntOffset(
                        x = (finger.x - previewSizePx / 2f).roundToInt(),
                        y = (finger.y - previewSizePx * DRAG_GHOST_LIFT_FACTOR).roundToInt(),
                    )
                }.size(CornersApartSpacing.PiecePreviewSize)
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                },
        alpha = DRAG_GHOST_ALPHA,
    )
}

private const val DRAG_GHOST_LIFT_FACTOR = 1.2f
private const val DRAG_GHOST_ALPHA = 0.9f
private const val DRAG_GHOST_START_SCALE = 0.7f
private const val DRAG_GHOST_SCALE = 1.1f
private const val PIECE_CARD_SELECTED_SCALE = 1.05f

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
        GameBoard(
            state = content.state,
            onPlaceCell = content.pieceActions.onPlaceCell,
            externalPreviewAnchor = content.dragController.previewAnchor,
            onCanvasPositioned = { coordinates -> content.dragController.boardCoordinates = coordinates },
            isPlacementLegal = content.pieceActions.isPlacementLegal,
        )
        AccessibilityAnnouncementNode(content.accessibilityAnnouncement)
        StatusLine(state = content.state, notice = content.statusNotice, scoreNotice = content.scoreNotice)
        ControlBar(content.pieceActions)
        SelectedPiecePreview(content.state)
        PiecePanel(
            pieces = content.state.pieces,
            colorIndex = content.state.currentPlayer.colorIndex,
            selectedCells = content.state.selectedCells,
            onSelectPiece = content.pieceActions.onSelectPiece,
            dragController = content.dragController,
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
            StatusLine(state = content.state, notice = content.statusNotice, scoreNotice = content.scoreNotice)
            ControlBar(content.pieceActions)
            SelectedPiecePreview(content.state)
        }
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        ) {
            GameBoard(
                state = content.state,
                onPlaceCell = content.pieceActions.onPlaceCell,
                externalPreviewAnchor = content.dragController.previewAnchor,
                onCanvasPositioned = { coordinates -> content.dragController.boardCoordinates = coordinates },
                isPlacementLegal = content.pieceActions.isPlacementLegal,
            )
            PiecePanel(
                pieces = content.state.pieces,
                colorIndex = content.state.currentPlayer.colorIndex,
                selectedCells = content.state.selectedCells,
                onSelectPiece = content.pieceActions.onSelectPiece,
                dragController = content.dragController,
            )
        }
    }
}

@Composable
private fun GameHeaderActions(content: GameLayoutContent) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Header(state = content.state, onModeSelected = content.screenActions.onModeSelected)
        UtilityActions(
            onShowHistoryStats = content.screenActions.onShowHistoryStats,
            onShowSettings = content.onShowSettings,
            onShowProfiles = content.onShowProfiles,
            onShowHelp = content.onShowHelp,
        )
        NearbyActions(
            sessionType = content.state.sessionType,
            nearbyState = content.state.nearbyState,
            onShowChallenges = content.onShowChallenges,
            onCreateNearbyGame = content.screenActions.onCreateNearbyGame,
            onFindNearbyGame = content.screenActions.onFindNearbyGame,
            onConnectToNearbyEndpoint = content.screenActions.onConnectToNearbyEndpoint,
            onAcceptPendingNearbyConnection = content.screenActions.onAcceptPendingNearbyConnection,
            onRejectPendingNearbyConnection = content.screenActions.onRejectPendingNearbyConnection,
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
    var showModePicker by rememberSaveable { mutableStateOf(false) }
    if (showModePicker) {
        GameModePickerDialog(
            currentMode = state.gameMode,
            onModeSelected = { mode ->
                showModePicker = false
                onModeSelected(mode)
            },
            onDismiss = { showModePicker = false },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium.withCandyShadow(),
                color = CornersApartColors.TextOnDarkPrimary,
            )
            Box(
                modifier =
                    Modifier
                        .size(
                            width = CornersApartSpacing.TitleAccentBarWidth,
                            height = CornersApartSpacing.TitleAccentBarHeight,
                        ).background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            CornersApartColors.PlayerPink,
                                            CornersApartColors.PlayerMango,
                                            CornersApartColors.PlayerLime,
                                            CornersApartColors.PlayerCyan,
                                        ),
                                ),
                            shape = CircleShape,
                        ),
            )
        }
        CandyChip(
            label = stringResource(state.gameMode.labelRes()),
            selected = true,
            onClick = { showModePicker = true },
        )
    }
}

@Composable
private fun GameModePickerDialog(
    currentMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    onDismiss: () -> Unit,
) {
    CandyDialog(
        title = stringResource(R.string.game_mode_picker_title),
        onDismiss = onDismiss,
    ) {
        GameModeUiOptions.modes.forEach { mode ->
            CandyChip(
                label = stringResource(mode.labelRes()),
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NearbyActions(
    sessionType: SessionType,
    nearbyState: NearbyUiState,
    onShowChallenges: () -> Unit,
    onCreateNearbyGame: () -> Unit,
    onFindNearbyGame: () -> Unit,
    onConnectToNearbyEndpoint: (String) -> Unit,
    onAcceptPendingNearbyConnection: (String) -> Unit,
    onRejectPendingNearbyConnection: (String) -> Unit,
) {
    var expandedByUser by rememberSaveable { mutableStateOf(false) }
    val mustShow =
        sessionType == SessionType.NEARBY ||
            nearbyState.pendingConnection != null ||
            nearbyState.discoveredEndpoints.isNotEmpty() ||
            nearbyState.errorMessage != null
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            CandyButton(
                text = stringResource(R.string.challenge_title),
                onClick = onShowChallenges,
                modifier = Modifier.weight(1f),
                style = CandyButtonStyle.Positive,
            )
            CandyButton(
                text = stringResource(R.string.nearby_game),
                onClick = { expandedByUser = !expandedByUser },
                modifier = Modifier.weight(1f),
                style = CandyButtonStyle.Neutral,
            )
        }
        AnimatedVisibility(visible = expandedByUser || mustShow) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(CornersApartSpacing.SectionGap),
                    verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
                ) {
                    if (sessionType == SessionType.NEARBY) {
                        Text(
                            text = stringResource(nearbyState.connectionState.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    nearbyState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                        CandyButton(
                            text = stringResource(R.string.create_nearby_game),
                            onClick = onCreateNearbyGame,
                            modifier = Modifier.weight(1f),
                            style = CandyButtonStyle.Primary,
                        )
                        CandyButton(
                            text = stringResource(R.string.find_nearby_game),
                            onClick = onFindNearbyGame,
                            modifier = Modifier.weight(1f),
                            style = CandyButtonStyle.Positive,
                        )
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
            CandyButton(
                text = stringResource(R.string.nearby_accept_connection),
                onClick = { onAccept(pendingConnection.endpointId) },
                style = CandyButtonStyle.Positive,
            )
            CandyButton(
                text = stringResource(R.string.nearby_reject_connection),
                onClick = { onReject(pendingConnection.endpointId) },
                style = CandyButtonStyle.Warn,
            )
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
            CandyButton(
                text = stringResource(R.string.nearby_connect_endpoint, endpoint.endpointName),
                onClick = { onConnect(endpoint.endpointId) },
                style = CandyButtonStyle.Positive,
            )
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
        CandyIconButton(
            contentDescription = stringResource(R.string.history_stats_title),
            onClick = onShowHistoryStats,
        ) {
            Icon(painter = painterResource(R.drawable.ic_history_24), contentDescription = null)
        }
        CandyIconButton(
            contentDescription = stringResource(R.string.profiles_title),
            onClick = onShowProfiles,
        ) {
            Icon(painter = painterResource(R.drawable.ic_person_24), contentDescription = null)
        }
        CandyIconButton(
            contentDescription = stringResource(R.string.settings_title),
            onClick = onShowSettings,
        ) {
            Icon(painter = painterResource(R.drawable.ic_settings_24), contentDescription = null)
        }
        CandyIconButton(
            contentDescription = stringResource(R.string.help_title),
            onClick = onShowHelp,
        ) {
            Icon(painter = painterResource(R.drawable.ic_help_24), contentDescription = null)
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
private fun StatusLine(
    state: GameUiState,
    notice: String?,
    scoreNotice: String?,
) {
    val text =
        if (state.isGameOver) {
            stringResource(R.string.game_status_game_over)
        } else {
            stringResource(R.string.game_status_turn, state.currentPlayer.name)
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                AnimatedContent(targetState = text, label = "turnText") { turnText ->
                    Text(
                        text = turnText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                var lastScoreNotice by remember { mutableStateOf("") }
                if (scoreNotice != null) lastScoreNotice = scoreNotice
                AnimatedVisibility(visible = scoreNotice != null) {
                    Text(
                        text = lastScoreNotice,
                        style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                        color = CornersApartColors.PlayerLime,
                    )
                }
            }
            var lastNotice by remember { mutableStateOf("") }
            if (notice != null) lastNotice = notice
            AnimatedVisibility(visible = notice != null) {
                Text(
                    text = lastNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = CornersApartColors.ButtonWarnFace,
                )
            }
        }
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
        CandyIconButton(
            contentDescription = stringResource(R.string.control_rotate_counterclockwise),
            onClick = onRotateCounterClockwise,
        ) {
            Icon(painter = painterResource(R.drawable.ic_rotate_left_24), contentDescription = null)
        }
        CandyIconButton(
            contentDescription = stringResource(R.string.control_rotate_clockwise),
            onClick = onRotateClockwise,
        ) {
            Icon(painter = painterResource(R.drawable.ic_rotate_right_24), contentDescription = null)
        }
        CandyIconButton(
            contentDescription = stringResource(R.string.control_flip),
            onClick = onFlip,
        ) {
            Icon(painter = painterResource(R.drawable.ic_flip_24), contentDescription = null)
        }
        PassButton(onPass)
    }
}

@Composable
private fun RowScope.PassButton(onPass: () -> Unit) {
    val description = stringResource(R.string.control_pass)
    CandyButton(
        text = description,
        onClick = onPass,
        modifier =
            Modifier
                .weight(1f)
                .semantics { contentDescription = description },
        style = CandyButtonStyle.Warn,
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_skip_next_24), contentDescription = null)
        },
    )
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
    selectedCells: List<CellOffset>,
    onSelectPiece: (String) -> Unit,
    dragController: BoardDragController,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
        val usedCount = pieces.count { item -> item.isUsed }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.piece_panel_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "$usedCount / ${pieces.size}",
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (usedCount == pieces.size) {
                        CornersApartColors.PlayerLime
                    } else {
                        CornersApartColors.TextOnDarkSecondary
                    },
            )
        }
        PieceProgressBar(
            fraction = if (pieces.isEmpty()) 0f else usedCount.toFloat() / pieces.size,
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
                    selectedCells = selectedCells,
                    onSelectPiece = onSelectPiece,
                    dragController = dragController,
                )
            }
        }
    }
}

@Composable
private fun PieceProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "pieceProgress")
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(CornersApartSpacing.PieceMeterHeight)
                .background(CornersApartColors.PanelSurfaceRaised, CircleShape),
    ) {
        if (animatedFraction > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(CornersApartSpacing.PieceMeterHeight)
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            CornersApartColors.PlayerLimeHighlight,
                                            CornersApartColors.PlayerLime,
                                        ),
                                ),
                            shape = CircleShape,
                        ),
            )
        }
    }
}

@Composable
private fun PieceCard(
    item: PiecePanelItem,
    colorIndex: Int,
    selectedCells: List<CellOffset>,
    onSelectPiece: (String) -> Unit,
    dragController: BoardDragController,
) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    val description =
        if (item.isUsed) {
            stringResource(R.string.piece_used_content_description, item.piece.displayName)
        } else {
            stringResource(R.string.piece_content_description, item.piece.displayName)
        }
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dragCells =
        if (item.isSelected && selectedCells.isNotEmpty()) {
            PieceTransforms.normalize(selectedCells)
        } else {
            PieceTransforms.normalize(item.piece.cells)
        }
    val selectionScale by
        animateFloatAsState(
            targetValue = if (item.isSelected) PIECE_CARD_SELECTED_SCALE else 1f,
            label = "pieceCardScale",
        )
    Surface(
        modifier =
            Modifier
                .size(CornersApartSpacing.PieceCardSize)
                .graphicsLayer {
                    scaleX = selectionScale
                    scaleY = selectionScale
                }.alpha(if (item.isUsed) CornersApartAlpha.UsedPiece else 1f)
                .semantics { contentDescription = description }
                .clickable(enabled = !item.isUsed) { onSelectPiece(item.piece.id) }
                .onGloballyPositioned { coordinates -> cardCoordinates = coordinates }
                .pointerInput(item.piece.id, item.isUsed, dragCells) {
                    if (item.isUsed) return@pointerInput
                    var position = Offset.Zero
                    detectDragGestures(
                        onDragStart = { startPosition ->
                            position = startPosition
                            onSelectPiece(item.piece.id)
                            dragController.startDrag(dragCells)
                            cardCoordinates?.let { dragController.updateFinger(it, position) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            position = change.position
                            cardCoordinates?.let { dragController.updateFinger(it, position) }
                        },
                        onDragEnd = {
                            val coordinates = cardCoordinates
                            if (coordinates != null) {
                                dragController.drop(coordinates, position)
                            } else {
                                dragController.clear()
                            }
                        },
                        onDragCancel = { dragController.clear() },
                    )
                },
        shape = MaterialTheme.shapes.small,
        color =
            if (item.isSelected) {
                CornersApartColors.PanelSurfaceRaised
            } else {
                MaterialTheme.colorScheme.surface
            },
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
