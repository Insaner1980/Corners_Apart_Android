package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.BonusTile
import com.finnvek.cornersapart.model.GameConfig
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.Move
import com.finnvek.cornersapart.model.PieceCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveEvaluatorTest {
    private val engine = GameEngine()
    private val evaluator = MoveEvaluator(engine)

    @Test
    fun opportunistScoresBonusClaimAboveSameSizedNonBonusMove() {
        val state =
            engine
                .newGame(
                    GameConfig(
                        mode = GameMode.FOUR_PLAYER,
                        boardSize = GameConstants.STANDARD_BOARD_SIZE,
                        randomSeed = 43L,
                        bonusTiles = listOf(BonusTile(row = 1, col = 18)),
                    ),
                ).copy(currentPlayerIndex = 1)
        val bonusMove =
            Move(
                playerIndex = 1,
                pieceId = PieceCatalog.THREE_BEND_ID,
                anchorRow = 0,
                anchorCol = 17,
                orientationIndex = 0,
            )
        val plainMove =
            Move(
                playerIndex = 1,
                pieceId = PieceCatalog.THREE_BEND_ID,
                anchorRow = 0,
                anchorCol = 17,
                orientationIndex = 1,
            )

        val bonusScore =
            evaluator.evaluate(
                state = state,
                move = bonusMove,
                style = OpponentStyle.OPPORTUNIST,
                difficulty = OpponentDifficulty.HARD,
            )
        val plainScore =
            evaluator.evaluate(
                state = state,
                move = plainMove,
                style = OpponentStyle.OPPORTUNIST,
                difficulty = OpponentDifficulty.HARD,
            )

        assertTrue(bonusScore.total > plainScore.total)
    }
}
