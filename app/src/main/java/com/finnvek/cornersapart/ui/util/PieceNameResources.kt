package com.finnvek.cornersapart.ui.util

import androidx.annotation.StringRes
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.PieceDef

@StringRes
fun PieceDef.displayNameResource(): Int = PIECE_NAME_RESOURCES[id] ?: R.string.piece_name_unknown

private val PIECE_NAME_RESOURCES: Map<String, Int> =
    mapOf(
        "one-dot" to R.string.piece_name_one_dot,
        "two-bar" to R.string.piece_name_two_bar,
        "three-bar" to R.string.piece_name_three_bar,
        "three-corner" to R.string.piece_name_three_corner,
        "four-bar" to R.string.piece_name_four_bar,
        "four-block" to R.string.piece_name_four_block,
        "four-tee" to R.string.piece_name_four_tee,
        "four-corner" to R.string.piece_name_four_corner,
        "four-step" to R.string.piece_name_four_step,
        "five-bar" to R.string.piece_name_five_bar,
        "five-block-tail" to R.string.piece_name_five_block_tail,
        "five-tee" to R.string.piece_name_five_tee,
        "five-cross" to R.string.piece_name_five_cross,
        "five-long-corner" to R.string.piece_name_five_long_corner,
        "five-shift" to R.string.piece_name_five_shift,
        "five-stair" to R.string.piece_name_five_stair,
        "five-cup" to R.string.piece_name_five_cup,
        "five-wide-corner" to R.string.piece_name_five_wide_corner,
        "five-hook" to R.string.piece_name_five_hook,
        "five-zag" to R.string.piece_name_five_zag,
        "five-offset" to R.string.piece_name_five_offset,
    )
