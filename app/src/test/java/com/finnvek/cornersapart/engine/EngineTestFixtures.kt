package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BoardSnapshot
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.Move

object EngineTestFixtures {
    data class MutableGameStateInput(
        val state: GameState,
        val boardCells: MutableList<Int>,
        val usedPieceIds: MutableSet<String>,
        val bonusTiles: MutableList<BonusTile>,
        val moveHistory: MutableList<Move>,
    )

    fun standardState(
        engine: GameEngine,
        randomSeed: Long = 7L,
    ): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.FOUR_PLAYER,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = randomSeed,
                bonusTiles = emptyList(),
            ),
        )

    fun mutableSnapshotInput(
        engine: GameEngine,
        randomSeed: Long,
    ): MutableGameStateInput {
        val baseState = standardState(engine, randomSeed)
        val boardCells = baseState.board.cells.toMutableList()
        val usedPieceIds = mutableSetOf<String>()
        val bonusTiles = mutableListOf(BonusTile(row = 4, col = 4))
        val moveHistory = mutableListOf<Move>()
        val state =
            baseState.copy(
                board = BoardSnapshot(size = baseState.board.size, cells = boardCells),
                players =
                    baseState.players.map { player ->
                        if (player.index == 0) player.copy(usedPieceIds = usedPieceIds) else player
                    },
                bonusTiles = bonusTiles,
                moveHistory = moveHistory,
            )
        return MutableGameStateInput(
            state = state,
            boardCells = boardCells,
            usedPieceIds = usedPieceIds,
            bonusTiles = bonusTiles,
            moveHistory = moveHistory,
        )
    }
}
