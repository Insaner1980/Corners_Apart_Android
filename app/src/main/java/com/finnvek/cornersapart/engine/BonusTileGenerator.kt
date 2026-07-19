package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BonusTileLayout
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.toSnapshotList

object BonusTileGenerator {
    private val standardTemplates =
        listOf(
            BonusTileLayout(
                id = "standard-cross-01",
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                positions =
                    listOf(
                        CellPosition(3, 5),
                        CellPosition(5, 14),
                        CellPosition(5, 5),
                        CellPosition(8, 8),
                        CellPosition(8, 11),
                        CellPosition(11, 8),
                        CellPosition(11, 11),
                        CellPosition(14, 5),
                        CellPosition(14, 14),
                        CellPosition(16, 14),
                    ),
            ),
        )

    private val compactTemplates =
        listOf(
            BonusTileLayout(
                id = "compact-balance-01",
                boardSize = GameConstants.COMPACT_BOARD_SIZE,
                positions =
                    listOf(
                        CellPosition(3, 3),
                        CellPosition(3, 10),
                        CellPosition(6, 5),
                        CellPosition(7, 8),
                        CellPosition(10, 3),
                        CellPosition(10, 10),
                    ),
            ),
        )

    fun generate(
        mode: GameMode,
        boardSize: Int,
        randomSeed: Long,
        requestedCount: Int = GameRules.bonusTileCountFor(mode),
    ): BonusTileLayout {
        val templates = templatesFor(boardSize)
        val template = templates[SeedMixer.index(seed = randomSeed, salt = SALT_INDEX, bound = templates.size)]
        val transform = SeedMixer.index(seed = randomSeed, salt = SALT_TRANSFORM, bound = TRANSFORM_COUNT)
        val positions =
            transformPositions(
                positions = template.positions,
                boardSize = boardSize,
                transform = transform,
            ).take(requestedCount)
                .toSnapshotList()
        return BonusTileLayout(
            id = "${template.id}:$transform",
            boardSize = boardSize,
            positions = positions,
        )
    }

    private fun templatesFor(boardSize: Int): List<BonusTileLayout> =
        when (boardSize) {
            GameConstants.COMPACT_BOARD_SIZE -> compactTemplates
            else -> standardTemplates
        }

    private fun transformPositions(
        positions: List<CellPosition>,
        boardSize: Int,
        transform: Int,
    ): List<CellPosition> =
        positions
            .map { position ->
                when (transform) {
                    ROTATE_180 -> CellPosition(row = boardSize - 1 - position.row, col = boardSize - 1 - position.col)
                    MIRROR_VERTICAL -> CellPosition(row = position.row, col = boardSize - 1 - position.col)
                    MIRROR_HORIZONTAL -> CellPosition(row = boardSize - 1 - position.row, col = position.col)
                    else -> position
                }
            }.toSnapshotList()

    private const val TRANSFORM_COUNT = 4
    private const val ROTATE_180 = 1
    private const val MIRROR_VERTICAL = 2
    private const val MIRROR_HORIZONTAL = 3
    private const val SALT_INDEX = 0x6A09E667F3BCC909L
    private const val SALT_TRANSFORM = 0x3C6EF372FE94F82BL
}
