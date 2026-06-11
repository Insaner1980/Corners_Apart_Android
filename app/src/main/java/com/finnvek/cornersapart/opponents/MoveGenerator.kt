package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog

class MoveGenerator(
    private val engine: GameEngine = GameEngine(),
) {
    fun generateMoves(
        state: GameState,
        playerIndex: Int,
        difficulty: OpponentDifficulty,
    ): List<MoveCandidate> =
        engine
            .getValidMoves(state, playerIndex)
            .asSequence()
            .sortedWith(candidateOrdering())
            .take(difficulty.candidateSoftCap)
            .map { move -> move.toCandidate(state) }
            .toList()

    private fun candidateOrdering(): Comparator<Move> =
        compareByDescending<Move> { move -> PieceCatalog.require(move.pieceId).cells.size }
            .thenBy { move -> move.pieceId }
            .thenBy { move -> move.orientationIndex }
            .thenBy { move -> move.anchorRow }
            .thenBy { move -> move.anchorCol }

    private fun Move.toCandidate(state: GameState): MoveCandidate {
        val preview = engine.previewPlacement(state, this)
        return MoveCandidate(
            move = this,
            placedCellCount = PieceCatalog.require(pieceId).cells.size,
            claimedBonusTileCount = preview.scoreDelta.bonusTilePoints / GameConstants.BONUS_TILE_POINTS,
        )
    }
}
