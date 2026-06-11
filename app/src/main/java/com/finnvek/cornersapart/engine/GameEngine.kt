package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameModeConfig
import com.finnvek.cornersapart.model.GameModeConfigs
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.MutableBoard
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PieceTransforms
import com.finnvek.cornersapart.model.Player

class GameEngine {
    fun newGame(config: GameConfig): GameState {
        val modeConfig = GameModeConfigs.forMode(config.mode, config.boardSize)
        val players = createPlayers(modeConfig)
        val generatedLayout =
            if (config.bonusTiles == null) {
                BonusTileGenerator.generate(
                    mode = config.mode,
                    boardSize = config.boardSize,
                    randomSeed = config.randomSeed,
                    requestedCount = config.bonusTileCount ?: modeConfig.bonusTileCount,
                )
            } else {
                null
            }
        return GameState(
            board = BoardSnapshot.empty(config.boardSize),
            players = players,
            currentPlayerIndex = 0,
            turnNumber = 0,
            ruleset = config.ruleset,
            gameMode = config.mode,
            randomSeed = config.randomSeed,
            bonusTiles = config.bonusTiles ?: generatedLayout.orEmptyBonusTiles(),
            bonusLayoutId = generatedLayout?.id,
        )
    }

    fun applyMove(
        state: GameState,
        move: Move,
    ): MoveResult {
        val validation = PlacementValidator.validate(state, move)
        if (!validation.isValid) {
            return MoveResult.Rejected(checkNotNull(validation.reason))
        }

        val piece = PieceCatalog.require(move.pieceId)
        val player = state.players[move.playerIndex]
        val completedPieceSet = player.usedPieceIds.size + 1 == PieceCatalog.all.size
        val scoreDelta =
            Scoring.scoreMove(
                placedCellCount = piece.cells.size,
                claimedBonusTileCount = validation.claimedBonusTiles.size,
                completesPieceSet = completedPieceSet,
            )
        val movedState = state.withAcceptedMove(move, validation, scoreDelta)
        val stateWithEndStatus = movedState.copy(isGameOver = shouldEndGame(movedState))
        return MoveResult.Accepted(
            state = stateWithEndStatus.copy(currentPlayerIndex = nextPlayerIndex(stateWithEndStatus)),
            scoreDelta = scoreDelta,
        )
    }

    fun pass(
        state: GameState,
        playerIndex: Int,
    ): GameState {
        require(playerIndex == state.currentPlayerIndex) { "Only the current player can pass." }
        val players =
            state.players.map { player ->
                if (player.index == playerIndex) player.copy(passed = true) else player
            }
        val passedState = state.copy(players = players, turnNumber = state.turnNumber + 1)
        val stateWithEndStatus = passedState.copy(isGameOver = shouldEndGame(passedState))
        return stateWithEndStatus.copy(currentPlayerIndex = nextPlayerIndex(stateWithEndStatus))
    }

    fun getValidMoves(
        state: GameState,
        playerIndex: Int,
    ): List<Move> {
        val player = state.players.getOrNull(playerIndex) ?: return emptyList()
        if (player.passed || state.isGameOver) return emptyList()
        return player.availablePieces().flatMap { pieceId ->
            validMovesForPiece(state, playerIndex, pieceId)
        }
    }

    fun hasValidMove(
        state: GameState,
        playerIndex: Int,
    ): Boolean = getValidMoves(state, playerIndex).isNotEmpty()

    fun previewPlacement(
        state: GameState,
        move: Move,
    ): PlacementPreview {
        val validation = PlacementValidator.validate(state, move, enforceTurn = false)
        val scoreDelta =
            if (validation.isValid) {
                previewScoreDelta(state, move, validation)
            } else {
                ScoreDelta()
            }
        return PlacementPreview(
            isValid = validation.isValid,
            targetCells = validation.targetCells,
            rejectionReason = validation.reason,
            scoreDelta = scoreDelta,
        )
    }

    private fun createPlayers(config: GameModeConfig): List<Player> =
        config.playerSlots.map { slot ->
            Player(
                index = slot.index,
                name = slot.name,
                colorIndex = slot.colorIndex,
                startCorner = slot.startCorner,
                isActiveScoring = slot.isActiveScoring,
                isComputerControlled = slot.isComputerControlled,
                ownerIndex = slot.ownerIndex,
            )
        }

    private fun GameState.withAcceptedMove(
        move: Move,
        validation: PlacementValidation,
        scoreDelta: ScoreDelta,
    ): GameState {
        val board = MutableBoard(board)
        validation.targetCells.forEach { target ->
            board.set(target, move.playerIndex)
        }
        return copy(
            board = board.toSnapshot(),
            players = players.withUpdatedPlayer(move, scoreDelta),
            bonusTiles = bonusTiles.withClaimedTiles(validation, move.playerIndex, turnNumber),
            turnNumber = turnNumber + 1,
            moveHistory = moveHistory + move,
        )
    }

    private fun List<Player>.withUpdatedPlayer(
        move: Move,
        scoreDelta: ScoreDelta,
    ): List<Player> =
        map { player ->
            if (player.index == move.playerIndex) {
                player.copy(
                    usedPieceIds = player.usedPieceIds + move.pieceId,
                    scoreBreakdown = player.scoreBreakdown.plus(scoreDelta),
                    passed = false,
                )
            } else {
                player
            }
        }

    private fun List<BonusTile>.withClaimedTiles(
        validation: PlacementValidation,
        playerIndex: Int,
        turnNumber: Int,
    ): List<BonusTile> {
        val claimedPositions = validation.claimedBonusTiles.map { bonusTile -> bonusTile.position }.toSet()
        return map { bonusTile ->
            if (bonusTile.position in claimedPositions && bonusTile.claimedByPlayerIndex == null) {
                bonusTile.copy(claimedByPlayerIndex = playerIndex, claimedOnTurn = turnNumber)
            } else {
                bonusTile
            }
        }
    }

    private fun Player.availablePieces(): List<String> =
        PieceCatalog.all
            .map { piece -> piece.id }
            .filterNot { pieceId -> pieceId in usedPieceIds }

    private fun validMovesForPiece(
        state: GameState,
        playerIndex: Int,
        pieceId: String,
    ): List<Move> {
        val piece = PieceCatalog.require(pieceId)
        return PieceTransforms.getAllOrientations(piece).flatMapIndexed { orientationIndex, _ ->
            validMovesForOrientation(state, playerIndex, pieceId, orientationIndex)
        }
    }

    private fun validMovesForOrientation(
        state: GameState,
        playerIndex: Int,
        pieceId: String,
        orientationIndex: Int,
    ): List<Move> {
        val piece = PieceCatalog.require(pieceId)
        val orientation = PieceTransforms.getOrientation(piece, orientationIndex) ?: return emptyList()
        return CornerCache.candidateAnchors(state, playerIndex, orientation).mapNotNull { anchor ->
            val move = Move(playerIndex, pieceId, anchor.row, anchor.col, orientationIndex)
            if (PlacementValidator.validate(state, move, enforceTurn = false).isValid) {
                move
            } else {
                null
            }
        }
    }

    private fun previewScoreDelta(
        state: GameState,
        move: Move,
        validation: PlacementValidation,
    ): ScoreDelta {
        val player = state.players[move.playerIndex]
        val piece = PieceCatalog.require(move.pieceId)
        return Scoring.scoreMove(
            placedCellCount = piece.cells.size,
            claimedBonusTileCount = validation.claimedBonusTiles.size,
            completesPieceSet = player.usedPieceIds.size + 1 == PieceCatalog.all.size,
        )
    }

    private fun shouldEndGame(state: GameState): Boolean {
        val activePlayers = state.players.filter { player -> player.isActiveScoring }
        return activePlayers.all { player ->
            player.passed || !hasValidMove(state, player.index)
        }
    }

    private fun nextPlayerIndex(state: GameState): Int {
        if (state.isGameOver) return state.currentPlayerIndex
        val playerCount = state.players.size
        for (offset in 1..playerCount) {
            val candidate = (state.currentPlayerIndex + offset) % playerCount
            val player = state.players[candidate]
            if (player.isActiveScoring && !player.passed && hasValidMove(state, candidate)) {
                return candidate
            }
        }
        return state.currentPlayerIndex
    }

    private fun com.finnvek.cornersapart.model.BonusTileLayout?.orEmptyBonusTiles(): List<BonusTile> =
        checkNotNull(this).positions.map { position ->
            BonusTile(row = position.row, col = position.col)
        }
}
