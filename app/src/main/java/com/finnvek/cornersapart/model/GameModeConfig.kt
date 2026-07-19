package com.finnvek.cornersapart.model

import kotlinx.serialization.Serializable

@Serializable
data class GameModeConfig(
    val mode: GameMode,
    val boardSize: Int,
    val bonusTileCount: Int,
    val playerSlots: List<PlayerSlotConfig>,
    val requiresPlayTesting: Boolean = false,
) {
    fun toGameConfig(
        randomSeed: Long = 0L,
        bonusTiles: List<BonusTile>? = null,
    ): GameConfig =
        GameConfig(
            mode = mode,
            boardSize = boardSize,
            randomSeed = randomSeed,
            bonusTiles = bonusTiles?.toSnapshotList(),
            bonusTileCount = bonusTileCount,
        )
}

@Serializable
data class PlayerSlotConfig(
    val index: Int,
    val name: String,
    val colorIndex: Int,
    val ownerIndex: Int,
    val startCorner: CellPosition,
    val isActiveScoring: Boolean = true,
    val isComputerControlled: Boolean = false,
)

object GameModeConfigs {
    val defaultMode: GameMode = GameMode.FOUR_PLAYER

    fun defaultBoardSizeFor(mode: GameMode): Int =
        when (mode) {
            GameMode.COMPACT_DUEL -> GameConstants.COMPACT_BOARD_SIZE
            GameMode.SOLO,
            GameMode.TWO_COLOR_DUEL,
            GameMode.THREE_PLAYER,
            GameMode.FOUR_PLAYER,
            -> GameConstants.STANDARD_BOARD_SIZE
        }

    fun forMode(
        mode: GameMode,
        boardSize: Int = defaultBoardSizeFor(mode),
    ): GameModeConfig =
        when (mode) {
            GameMode.SOLO ->
                GameModeConfig(
                    mode = mode,
                    boardSize = boardSize,
                    bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
                    playerSlots = soloSlots(boardSize),
                )
            GameMode.TWO_COLOR_DUEL ->
                GameModeConfig(
                    mode = mode,
                    boardSize = boardSize,
                    bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
                    playerSlots = twoColorDuelSlots(boardSize),
                )
            GameMode.COMPACT_DUEL ->
                GameModeConfig(
                    mode = mode,
                    boardSize = boardSize,
                    bonusTileCount = GameConstants.COMPACT_BONUS_TILE_COUNT,
                    playerSlots = compactDuelSlots(boardSize),
                    requiresPlayTesting = true,
                )
            GameMode.THREE_PLAYER ->
                GameModeConfig(
                    mode = mode,
                    boardSize = boardSize,
                    bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
                    playerSlots =
                        standardSlots(boardSize)
                            .take(3)
                            .toSnapshotList(),
                )
            GameMode.FOUR_PLAYER ->
                GameModeConfig(
                    mode = mode,
                    boardSize = boardSize,
                    bonusTileCount = GameConstants.STANDARD_BONUS_TILE_COUNT,
                    playerSlots = standardSlots(boardSize),
                )
        }

    fun defaultGameConfig(
        mode: GameMode,
        randomSeed: Long = 0L,
        bonusTiles: List<BonusTile>? = null,
    ): GameConfig =
        forMode(mode).toGameConfig(
            randomSeed = randomSeed,
            bonusTiles = bonusTiles,
        )

    private fun soloSlots(boardSize: Int): List<PlayerSlotConfig> =
        listOf(
            slot(
                index = 0,
                startCorner = CellPosition(row = boardSize - 1, col = boardSize - 1),
            ),
            slot(
                index = 1,
                startCorner = CellPosition(row = 0, col = 0),
                isComputerControlled = true,
            ),
            slot(
                index = 2,
                startCorner = CellPosition(row = 0, col = boardSize - 1),
                isComputerControlled = true,
            ),
            slot(
                index = 3,
                startCorner = CellPosition(row = boardSize - 1, col = 0),
                isComputerControlled = true,
            ),
        ).toSnapshotList()

    private fun twoColorDuelSlots(boardSize: Int): List<PlayerSlotConfig> =
        standardSlots(boardSize)
            .map { slot ->
                slot.copy(ownerIndex = slot.index % 2)
            }.toSnapshotList()

    private fun compactDuelSlots(boardSize: Int): List<PlayerSlotConfig> =
        listOf(
            slot(
                index = 0,
                startCorner = CellPosition(row = 0, col = 0),
            ),
            slot(
                index = 1,
                startCorner = CellPosition(row = boardSize - 1, col = boardSize - 1),
            ),
        ).toSnapshotList()

    private fun standardSlots(boardSize: Int): List<PlayerSlotConfig> =
        standardCorners(boardSize)
            .mapIndexed { index, corner ->
                slot(
                    index = index,
                    startCorner = corner,
                )
            }.toSnapshotList()

    private fun slot(
        index: Int,
        startCorner: CellPosition,
        ownerIndex: Int = index,
        isComputerControlled: Boolean = false,
    ): PlayerSlotConfig =
        PlayerSlotConfig(
            index = index,
            name = GameConstants.PLAYER_NAMES[index],
            colorIndex = index,
            ownerIndex = ownerIndex,
            startCorner = startCorner,
            isComputerControlled = isComputerControlled,
        )

    private fun standardCorners(boardSize: Int): List<CellPosition> =
        listOf(
            CellPosition(row = 0, col = 0),
            CellPosition(row = 0, col = boardSize - 1),
            CellPosition(row = boardSize - 1, col = boardSize - 1),
            CellPosition(row = boardSize - 1, col = 0),
        ).toSnapshotList()
}
