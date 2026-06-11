package com.finnvek.cornersapart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameSettings
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.multiplayer.LocalSession
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
class GameViewModel : ViewModel {
    private val session: LocalSession
    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
    private var selectedPieceId: String = PieceCatalog.SINGLE_CELL_ID
    private var selectedOrientationIndex: Int = 0
    private var settings: GameSettings = GameSettings()
    private var gameStartedAtMillis: Long = System.currentTimeMillis()
    private val _uiState: MutableStateFlow<GameUiState>

    val uiState: StateFlow<GameUiState>
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    @Inject
    constructor() : this(LocalSession())

    internal constructor(session: LocalSession) : super() {
        this.session = session
        _uiState = MutableStateFlow(session.gameState.value.toUiState())
        uiState = _uiState.asStateFlow()
    }

    fun startFourPlayerGame() {
        startGame(GameMode.FOUR_PLAYER)
    }

    fun startSoloGame() {
        startGame(GameMode.SOLO)
    }

    fun startGame(mode: GameMode) {
        selectedPieceId = PieceCatalog.SINGLE_CELL_ID
        selectedOrientationIndex = 0
        gameStartedAtMillis = System.currentTimeMillis()
        session.startNewGame(LocalSession.defaultConfigFor(mode))
        refreshUiState()
    }

    fun setSoundEnabled(enabled: Boolean) {
        settings = settings.copy(soundEnabled = enabled)
        refreshUiState()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settings = settings.copy(hapticsEnabled = enabled)
        refreshUiState()
    }

    fun setReducedMotionEnabled(enabled: Boolean) {
        settings = settings.copy(reducedMotionEnabled = enabled)
        refreshUiState()
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
            } else {
                _effects.tryEmit(GameEffect.MoveRejected(result.moveRejectionReason()))
            }
        }
    }

    fun passCurrentPlayer() {
        viewModelScope.launch {
            val playerIndex = session.gameState.value.currentPlayerIndex
            session.sendPass(playerIndex)
            refreshUiState()
        }
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
            gameDurationSeconds = ((System.currentTimeMillis() - gameStartedAtMillis) / MILLIS_PER_SECOND).toInt(),
        )
    }

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
}

private const val MILLIS_PER_SECOND = 1_000L
