package com.finnvek.cornersapart.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

class GameSoundPlayer {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)

    fun play(event: GameSoundEvent) {
        val tone =
            when (event) {
                GameSoundEvent.PLACEMENT -> ToneGenerator.TONE_PROP_ACK
                GameSoundEvent.BONUS_CLAIM -> ToneGenerator.TONE_PROP_BEEP2
                GameSoundEvent.GAME_OVER -> ToneGenerator.TONE_PROP_PROMPT
            }
        toneGenerator.startTone(tone, TONE_DURATION_MS)
    }

    fun release() {
        toneGenerator.release()
    }

    private companion object {
        const val TONE_VOLUME = 40
        const val TONE_DURATION_MS = 90
    }
}

@Composable
fun rememberGameSoundPlayer(): GameSoundPlayer {
    val player = remember { GameSoundPlayer() }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
