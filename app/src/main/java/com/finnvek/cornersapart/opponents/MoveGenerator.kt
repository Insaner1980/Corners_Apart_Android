package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog

class MoveGenerator(
    private val engine: GameEngine = GameEngine(),
) {
    /**
     * Tuottaa edustavan kandidaattipoolin: tasainen otanta jokaisen palan
     * sijoituksista ja palat lomitettuna suurin/pienin-vuorotellen, jotta
     * pienikin pooli sisältää sekä isoja että pieniä paloja laudan eri osista.
     */
    fun generateMoves(
        state: GameState,
        playerIndex: Int,
        difficulty: OpponentDifficulty,
    ): List<MoveCandidate> {
        val validMoves = engine.getValidMoves(state, playerIndex)
        if (validMoves.isEmpty()) return emptyList()
        val movesByPiece = validMoves.groupBy { move -> move.pieceId }
        val perPieceCap =
            maxOf(MIN_MOVES_PER_PIECE, difficulty.candidateSoftCap / movesByPiece.size)
        val sampledByPiece =
            movesByPiece.mapValues { (_, moves) ->
                sampleEvenly(moves.sortedWith(withinPieceOrdering()), perPieceCap)
            }
        return interleaveBySize(sampledByPiece, difficulty.candidateSoftCap)
            .map { move -> move.toCandidate(state) }
    }

    private fun withinPieceOrdering(): Comparator<Move> =
        compareBy<Move> { move -> move.orientationIndex }
            .thenBy { move -> move.anchorRow }
            .thenBy { move -> move.anchorCol }

    private fun sampleEvenly(
        moves: List<Move>,
        cap: Int,
    ): List<Move> =
        if (moves.size <= cap) {
            moves
        } else {
            List(cap) { index -> moves[index * moves.size / cap] }
        }

    private fun interleaveBySize(
        sampledByPiece: Map<String, List<Move>>,
        cap: Int,
    ): List<Move> {
        val bySizeDescending =
            sampledByPiece.keys.sortedWith(
                compareByDescending<String> { pieceId -> PieceCatalog.require(pieceId).cells.size }
                    .thenBy { pieceId -> pieceId },
            )
        val pieceOrder = alternatingEnds(bySizeDescending)
        val result = mutableListOf<Move>()
        var round = 0
        while (result.size < cap) {
            var addedAny = false
            for (pieceId in pieceOrder) {
                val moves = sampledByPiece.getValue(pieceId)
                if (round < moves.size) {
                    result += moves[round]
                    addedAny = true
                    if (result.size == cap) break
                }
            }
            if (!addedAny) break
            round++
        }
        return result
    }

    /** Järjestää listan päistä vuorotellen: suurin, pienin, toiseksi suurin, ... */
    private fun <T> alternatingEnds(items: List<T>): List<T> {
        val result = ArrayList<T>(items.size)
        var start = 0
        var end = items.lastIndex
        while (start <= end) {
            result += items[start]
            if (start != end) result += items[end]
            start++
            end--
        }
        return result
    }

    private fun Move.toCandidate(state: GameState): MoveCandidate {
        val preview = engine.previewPlacement(state, this)
        return MoveCandidate(
            move = this,
            placedCellCount = PieceCatalog.require(pieceId).cells.size,
            claimedBonusTileCount = preview.claimedBonusTileCount,
        )
    }

    private companion object {
        const val MIN_MOVES_PER_PIECE = 2
    }
}
