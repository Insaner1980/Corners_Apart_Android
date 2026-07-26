package com.finnvek.cornersapart.multiplayer

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveRejectionReason
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.ScoreDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostGameCoordinatorTest {
    private val engine = GameEngine()

    @Test
    fun hostAcceptsLegalMoveAndBroadcastsFullAuthoritativeState() {
        val coordinator = coordinator()
        val move =
            Move(
                playerIndex = 0,
                pieceId = PieceCatalog.SINGLE_CELL_ID,
                anchorRow = 0,
                anchorCol = 0,
                orientationIndex = 0,
            )

        val result = coordinator.handle("client-1", GameMessage.PlaceMove(move))

        assertEquals(1, coordinator.state.currentPlayerIndex)
        assertEquals(0, coordinator.state.board.get(row = 0, col = 0))
        assertEquals(
            GameMessage.MoveAccepted(
                move = move,
                state = coordinator.state,
                scoreDelta = ScoreDelta(placedCellPoints = 1),
            ),
            result.single().message,
        )
        assertEquals(MessageTarget.Broadcast, result.single().target)
    }

    @Test
    fun hostRejectsIllegalMoveWithoutMutatingState() {
        val coordinator = coordinator()
        val originalState = coordinator.state
        val move =
            Move(
                playerIndex = 0,
                pieceId = PieceCatalog.SINGLE_CELL_ID,
                anchorRow = 0,
                anchorCol = 1,
                orientationIndex = 0,
            )

        val result = coordinator.handle("client-1", GameMessage.PlaceMove(move))

        assertEquals(originalState, coordinator.state)
        assertEquals(MessageTarget.Endpoint("client-1"), result.single().target)
        assertTrue(result.single().message is GameMessage.MoveRejected)
    }

    @Test
    fun hostPassBroadcastsSameStateAsEnginePass() {
        val coordinator = coordinator()
        val expectedState = engine.pass(coordinator.state, playerIndex = 0)

        val result = coordinator.handle("client-1", GameMessage.Pass(playerIndex = 0))

        assertEquals(expectedState, coordinator.state)
        assertEquals(
            HostMessage(MessageTarget.Broadcast, GameMessage.FullSync(expectedState)),
            result.single(),
        )
    }

    @Test
    fun hostRejectsPassAfterGameOverWithoutMutatingState() {
        val coordinator = coordinator()
        val endedState =
            coordinator.state.copy(
                currentPlayerIndex = 0,
                turnNumber = 12,
                isGameOver = true,
            )
        coordinator.replaceState(endedState)

        val result = coordinator.handle("client-1", GameMessage.Pass(playerIndex = 0))

        assertEquals(endedState, coordinator.state)
        assertEquals(MessageTarget.Endpoint("client-1"), result.single().target)
        val rejection = result.single().message as GameMessage.MoveRejected
        assertEquals(MoveRejectionReason.GAME_OVER, rejection.reason)
    }

    @Test
    fun hostFullSyncsNewlyJoinedPlayers() {
        val coordinator = coordinator()
        val player =
            SessionPlayer(
                index = 1,
                name = "Guest",
                isLocal = false,
                isComputerControlled = false,
                colorIndex = 1,
                ownerIndex = 1,
                usedPieceCount = 0,
            )

        val result = coordinator.handle("client-2", GameMessage.PlayerJoined(player))

        assertEquals(
            listOf(
                HostMessage(MessageTarget.Broadcast, GameMessage.PlayerJoined(player)),
                HostMessage(MessageTarget.Endpoint("client-2"), GameMessage.FullSync(coordinator.state)),
            ),
            result,
        )
    }

    private fun coordinator(): HostGameCoordinator =
        HostGameCoordinator(
            engine = engine,
            initialState =
                engine.newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        randomSeed = 41L,
                        bonusTiles = emptyList(),
                    ),
                ),
        )
}
