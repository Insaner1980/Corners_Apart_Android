package com.finnvek.cornersapart.viewmodel

import com.finnvek.cornersapart.review.MatchReviewFailure
import com.finnvek.cornersapart.review.MoveAssessment
import com.finnvek.cornersapart.review.MoveClassification
import com.finnvek.cornersapart.review.ReviewTimelineStep

enum class MatchReviewPhase {
    ANALYZING,
    COMPLETE,
    FAILED,
}

data class MatchReviewPlayerUiState(
    val index: Int,
    val name: String,
    val colorIndex: Int,
)

data class MatchReviewUiState(
    val phase: MatchReviewPhase,
    val players: List<MatchReviewPlayerUiState>,
    val timeline: List<ReviewTimelineStep>,
    val assessmentsByStepIndex: Map<Int, MoveAssessment>,
    val analyzedCount: Int,
    val totalCount: Int,
    val currentStepIndex: Int,
    val accuracy: Double?,
    val classificationCounts: Map<MoveClassification, Int>,
    val failure: MatchReviewFailure? = null,
)
