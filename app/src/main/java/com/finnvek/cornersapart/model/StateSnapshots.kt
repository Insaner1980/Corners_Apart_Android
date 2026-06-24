package com.finnvek.cornersapart.model

import java.util.Collections

internal fun <T> List<T>.toSnapshotList(): List<T> = Collections.unmodifiableList(toList())

internal fun <T> Set<T>.toSnapshotSet(): Set<T> = Collections.unmodifiableSet(toSet())

internal fun <K, V> Map<K, V>.toSnapshotMap(): Map<K, V> = Collections.unmodifiableMap(toMap())

internal fun BoardSnapshot.toSnapshotCopy(): BoardSnapshot =
    BoardSnapshot(
        size = size,
        cells = cells.toSnapshotList(),
    )

internal fun Player.toSnapshotCopy(): Player =
    copy(
        usedPieceIds = usedPieceIds.toSnapshotSet(),
    )

internal fun GameState.toSnapshotCopy(): GameState =
    copy(
        board = board.toSnapshotCopy(),
        players = players.map { player -> player.toSnapshotCopy() }.toSnapshotList(),
        bonusTiles = bonusTiles.toSnapshotList(),
        moveHistory = moveHistory.toSnapshotList(),
    )

internal fun HistoryEntry.toSnapshotCopy(): HistoryEntry =
    copy(
        scores = scores.toSnapshotList(),
    )

internal fun Profile.toSnapshotCopy(): Profile =
    copy(
        history = history.map { entry -> entry.toSnapshotCopy() }.toSnapshotList(),
    )

internal fun ProfilesData.toSnapshotCopy(): ProfilesData =
    copy(
        profiles = profiles.map { profile -> profile.toSnapshotCopy() }.toSnapshotList(),
    )

internal fun SavedGameData.toSnapshotCopy(): SavedGameData =
    copy(
        gameState = gameState?.toSnapshotCopy(),
    )
