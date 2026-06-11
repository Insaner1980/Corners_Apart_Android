package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.engine.GameEngine
import com.finnvek.cornersapart.model.GameState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.random.Random
import kotlin.time.TimeSource

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
        val deadline = TimeSource.Monotonic.markNow() + difficulty.timeBudget
        val candidates = moveGenerator.generateMoves(state, playerIndex, difficulty)
        if (candidates.isEmpty()) return OpponentAction.Pass(playerIndex)
        val evaluated = evaluateUntilDeadline(state, candidates, style, difficulty, deadline)
        val chosen = chooseByTemperature(evaluated, seededRandom(state, playerIndex, difficulty, style), difficulty)
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

    private fun evaluateUntilDeadline(
        state: GameState,
        candidates: List<MoveCandidate>,
        style: OpponentStyle,
        difficulty: OpponentDifficulty,
        deadline: TimeSource.Monotonic.ValueTimeMark,
    ): List<EvaluatedMove> {
        val evaluated = mutableListOf<EvaluatedMove>()
        for (candidate in candidates) {
            evaluated +=
                EvaluatedMove(
                    candidate = candidate,
                    evaluation = moveEvaluator.evaluate(state, candidate, style, difficulty),
                )
            if (deadline.hasPassedNow()) break
        }
        return evaluated.sortedByDescending { item -> item.evaluation.total }
    }

    private fun chooseByTemperature(
        evaluated: List<EvaluatedMove>,
        random: Random,
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
        var cursor = random.nextDouble() * totalWeight
        weighted.forEach { item ->
            cursor -= item.second
            if (cursor <= 0.0) return item.first
        }
        return weighted.last().first
    }

    private fun seededRandom(
        state: GameState,
        playerIndex: Int,
        difficulty: OpponentDifficulty,
        style: OpponentStyle,
    ): Random =
        Random(
            state.randomSeed xor
                (state.turnNumber.toLong() shl TURN_SHIFT) xor
                (playerIndex.toLong() shl PLAYER_SHIFT) xor
                difficulty.ordinal.toLong() xor
                (style.ordinal.toLong() shl STYLE_SHIFT),
        )

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
    }
}
