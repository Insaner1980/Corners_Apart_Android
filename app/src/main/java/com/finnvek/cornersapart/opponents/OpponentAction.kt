package com.finnvek.cornersapart.opponents

import com.finnvek.cornersapart.model.Move

sealed interface OpponentAction {
    data class PlaceMove(
        val move: Move,
    ) : OpponentAction

    data class Pass(
        val playerIndex: Int,
    ) : OpponentAction
}
