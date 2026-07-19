package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.engine.MoveResult
import com.finnvek.cornersapart.engine.SeedMixer
import com.finnvek.cornersapart.model.GameState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

class ComputerOpponentEngine(
    private val gameEngine: GameEngine = GameEngine(),
    private val moveGenerator: MoveGenerator = MoveGenerator(gameEngine),
    private val moveEvaluator: MoveEvaluator = MoveEvaluator(gameEngine),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun chooseAction(
        state: GameState,
        playerIndex: Int,
        style: OpponentStyle = defaultStyleFor(playerIndex),
        difficulty: OpponentDifficulty = OpponentDifficulty.MEDIUM,
    ): OpponentAction =
        withContext(dispatcher) {
            chooseActionOnDispatcher(state, playerIndex, style, difficulty)
        }

    private fun chooseActionOnDispatcher(
        state: GameState,
        playerIndex: Int,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): OpponentAction {
        val candidates = moveGenerator.generateMoves(state, playerIndex, difficulty)
        if (candidates.isEmpty()) return OpponentAction.Pass(playerIndex)
        val evaluated =
            evaluateCandidates(state, candidates, style, difficulty)
                .let { items -> applyLookahead(state, items, playerIndex, difficulty) }
        val chosen = chooseByTemperature(evaluated, seededSelection(state, playerIndex, difficulty, style), difficulty)
        val preview = gameEngine.previewPlacement(state, chosen.candidate.move)
        return if (preview.isValid) {
            OpponentAction.PlaceMove(chosen.candidate.move)
        } else {
            evaluated
                .firstOrNull { item -> gameEngine.previewPlacement(state, item.candidate.move).isValid }
                ?.candidate
                ?.move
                ?.let(OpponentAction::PlaceMove)
                ?: OpponentAction.Pass(playerIndex)
        }
    }

    private fun evaluateCandidates(
        state: GameState,
        candidates: List<MoveCandidate>,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
    ): List<EvaluatedMove> =
        candidates
            .map { candidate ->
                EvaluatedMove(
                    candidate = candidate,
                    evaluation = moveEvaluator.evaluate(state, candidate, style, difficulty),
                )
            }.sortedByDescending { item -> item.evaluation.total }

    /**
     * MASTER-tason 2-ply-haku: parhaille kandidaateille arvioidaan seuraavan
     * pelaajan paras vastasiirto ja kandidaatit järjestetään erotuksen mukaan.
     */
    private fun applyLookahead(
        state: GameState,
        evaluated: List<EvaluatedMove>,
        playerIndex: Int,
        difficulty: OpponentDifficulty,
    ): List<EvaluatedMove> {
        if (difficulty.lookaheadCandidates <= 0) return evaluated
        val head = evaluated.take(difficulty.lookaheadCandidates)
        val tail = evaluated.drop(difficulty.lookaheadCandidates)
        val reordered =
            head
                .map { item ->
                    item to (item.evaluation.total - OPPONENT_REPLY_WEIGHT * bestReplyScore(state, item, playerIndex))
                }.sortedByDescending { (_, adjusted) -> adjusted }
                .map { (item, _) -> item }
        return reordered + tail
    }

    private fun bestReplyScore(
        state: GameState,
        item: EvaluatedMove,
        playerIndex: Int,
    ): Double {
        val result = gameEngine.applyMove(state, item.candidate.move) as? MoveResult.Accepted ?: return 0.0
        val nextState = result.state
        if (nextState.isGameOver) return 0.0
        val replyPlayerIndex = nextState.currentPlayerIndex
        if (replyPlayerIndex == playerIndex) return 0.0
        val replyCandidates = moveGenerator.generateMoves(nextState, replyPlayerIndex, LOOKAHEAD_REPLY_DIFFICULTY)
        if (replyCandidates.isEmpty()) return 0.0
        return replyCandidates.maxOf { candidate ->
            moveEvaluator
                .evaluate(
                    nextState,
                    candidate,
                    defaultStyleFor(replyPlayerIndex),
                    LOOKAHEAD_REPLY_DIFFICULTY,
                ).total
        }
    }

    private fun chooseByTemperature(
        evaluated: List<EvaluatedMove>,
        selection: Double,
        difficulty: OpponentDifficulty,
    ): EvaluatedMove {
        if (evaluated.size == 1 || difficulty.temperature <= LOW_TEMPERATURE_THRESHOLD) return evaluated.first()
        val maxScore = evaluated.maxOf { item -> item.evaluation.total }
        val weighted =
            evaluated.map { item ->
                val weight = exp((item.evaluation.total - maxScore) / difficulty.temperature).coerceAtLeast(MIN_WEIGHT)
                item to weight
            }
        val totalWeight = weighted.sumOf { item -> item.second }
        var cursor = selection * totalWeight
        weighted.forEach { item ->
            cursor -= item.second
            if (cursor <= 0.0) return item.first
        }
        return weighted.last().first
    }

    private fun seededSelection(
        state: GameState,
        playerIndex: Int,
        difficulty: OpponentDifficulty,
        style: OpponentStyle,
    ): Double =
        (
            state.randomSeed xor
                (state.turnNumber.toLong() shl TURN_SHIFT) xor
                (playerIndex.toLong() shl PLAYER_SHIFT) xor
                difficulty.ordinal.toLong() xor
                (style.ordinal.toLong() shl STYLE_SHIFT)
        ).let(SeedMixer::unitInterval)

    private data class EvaluatedMove(
        val candidate: MoveCandidate,
        val evaluation: MoveEvaluation,
    )

    companion object {
        fun defaultStyleFor(playerIndex: Int): OpponentStyle =
            when (playerIndex.mod(STYLE_COUNT)) {
                1 -> OpponentStyle.EXPANSIONIST
                2 -> OpponentStyle.OPPORTUNIST
                else -> OpponentStyle.BLOCKER
            }

        private const val STYLE_COUNT = 3
        private const val TURN_SHIFT = 16
        private const val PLAYER_SHIFT = 8
        private const val STYLE_SHIFT = 4
        private const val LOW_TEMPERATURE_THRESHOLD = 0.25
        private const val MIN_WEIGHT = 0.000_001
        private const val OPPONENT_REPLY_WEIGHT = 0.6
        private val LOOKAHEAD_REPLY_DIFFICULTY = OpponentDifficulty.EASY
    }
}
