package com.finnvek.cornersapart.ui.screens

import androidx.annotation.StringRes
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.GameMode

internal object GameModeUiOptions {
    val modes: List<GameMode> = GameMode.entries
}

@StringRes
internal fun GameMode.labelRes(): Int =
    when (this) {
        GameMode.SOLO -> R.string.game_mode_solo
        GameMode.TWO_COLOR_DUEL -> R.string.game_mode_two_color_duel
        GameMode.COMPACT_DUEL -> R.string.game_mode_compact_duel
        GameMode.THREE_PLAYER -> R.string.game_mode_three_player
        GameMode.FOUR_PLAYER -> R.string.game_mode_four_player
    }
