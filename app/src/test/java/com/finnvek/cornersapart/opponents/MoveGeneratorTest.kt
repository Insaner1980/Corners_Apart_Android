package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.GameState
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveGeneratorTest {
    private val engine = GameEngine()
    private val generator = MoveGenerator(engine)

    @Test
    fun generatedMovesAreLegalForEveryDifficultyLevel() {
        val state = soloState(seed = 31L).copy(currentPlayerIndex = 1)

        OpponentDifficulty.entries.forEach { difficulty ->
            val moves =
                generator.generateMoves(
                    state = state,
                    playerIndex = 1,
                    difficulty = difficulty,
                )

            assertTrue(moves.isNotEmpty())
            assertTrue(moves.size <= difficulty.candidateSoftCap)
            moves.forEach { candidate ->
                assertTrue(engine.previewPlacement(state, candidate.move).isValid)
            }
        }
    }

    @Test
    fun generatorReturnsNoMovesWhenPlayerHasNoLegalMove() {
        val state =
            soloState(seed = 37L).copy(
                players =
                    soloState(seed = 37L).players.map { player ->
                        if (player.index == 1) {
                            player.copy(usedPieceIds = PieceCatalog.all.map { piece -> piece.id }.toSet())
                        } else {
                            player
                        }
                    },
                currentPlayerIndex = 1,
            )

        val moves =
            generator.generateMoves(
                state = state,
                playerIndex = 1,
                difficulty = OpponentDifficulty.EXPERT,
            )

        assertEquals(emptyList<MoveCandidate>(), moves)
    }

    @Test
    fun candidateMoveCanBeAppliedByEngine() {
        val state = soloState(seed = 41L).copy(currentPlayerIndex = 1)

        val candidate =
            generator
                .generateMoves(
                    state = state,
                    playerIndex = 1,
                    difficulty = OpponentDifficulty.MEDIUM,
                ).first()

        assertTrue(engine.applyMove(state, candidate.move) is MoveResult.Accepted)
    }

    private fun soloState(seed: Long): GameState =
        engine.newGame(
            GameConfig(
                mode = GameMode.SOLO,
                boardSize = GameConstants.STANDARD_BOARD_SIZE,
                randomSeed = seed,
                bonusTiles = emptyList(),
            ),
        )
}
