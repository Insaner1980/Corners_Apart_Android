package com.finnvek.cornersapart.engine

import com.finnvek.cornersapart.model.BonusTileLayout
import com.finnvek.cornersapart.model.CellPosition
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode

object BonusTileGenerator {
    const val MIN_BONUS_DISTANCE = 2

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
        val template = templates[randomSeed.toIndex(templates.size)]
        val transform = randomSeed.mixedWith(SALT_TRANSFORM).toIndex(TRANSFORM_COUNT)
        val positions = transformPositions(template.positions, boardSize, transform).take(requestedCount)
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
        positions.map { position ->
            when (transform) {
                ROTATE_180 -> CellPosition(row = boardSize - 1 - position.row, col = boardSize - 1 - position.col)
                MIRROR_VERTICAL -> CellPosition(row = position.row, col = boardSize - 1 - position.col)
                MIRROR_HORIZONTAL -> CellPosition(row = boardSize - 1 - position.row, col = position.col)
                else -> position
            }
        }

    private fun Long.toIndex(bound: Int): Int = (mixedWith(SALT_INDEX).floorMod(bound.toLong())).toInt()

    private fun Long.mixedWith(salt: Long): Long =
        (this xor salt)
            .let { value -> value xor (value ushr FIRST_MIX_SHIFT) }
            .let { value -> value * FIRST_MIX_MULTIPLIER }
            .let { value -> value xor (value ushr SECOND_MIX_SHIFT) }
            .let { value -> value * SECOND_MIX_MULTIPLIER }
            .let { value -> value xor (value ushr THIRD_MIX_SHIFT) }

    private fun Long.floorMod(divisor: Long): Long = ((this % divisor) + divisor) % divisor

    private const val TRANSFORM_COUNT = 4
    private const val ROTATE_180 = 1
    private const val MIRROR_VERTICAL = 2
    private const val MIRROR_HORIZONTAL = 3
    private const val SALT_INDEX = 0x6A09E667F3BCC909L
    private const val SALT_TRANSFORM = 0x3C6EF372FE94F82BL
    private const val FIRST_MIX_SHIFT = 33
    private const val SECOND_MIX_SHIFT = 29
    private const val THIRD_MIX_SHIFT = 32
    private const val FIRST_MIX_MULTIPLIER = 6_364_136_223_846_793_005L
    private const val SECOND_MIX_MULTIPLIER = 1_442_695_040_888_963_407L
}
