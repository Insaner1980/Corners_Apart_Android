package com.finnvek.cornersapart.ui.screens

enum class GameLayoutMode {
    COMPACT,
    EXPANDED,
}

object GameLayoutPolicy {
    fun modeForWidthDp(widthDp: Int): GameLayoutMode =
        if (widthDp >= EXPANDED_WIDTH_DP) {
            GameLayoutMode.EXPANDED
        } else {
            GameLayoutMode.COMPACT
        }

    private const val EXPANDED_WIDTH_DP = 840
}

object MotionPolicy {
    fun durationMillis(
        defaultMillis: Int,
        reducedMotionEnabled: Boolean,
    ): Int =
        if (reducedMotionEnabled) {
            0
        } else {
            defaultMillis
        }
}
