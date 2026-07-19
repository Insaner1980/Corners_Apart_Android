package com.finnvek.cornersapart.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.finnvek.cornersapart.R
import kotlin.random.Random

/** Soittaa lyhyet peliäänet SoundPoolilla res/raw-sampleista. */
class GameSoundPlayer(
    context: Context,
) {
    private val soundPool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()

    private val soundIds: Map<GameSoundEvent, Int> =
        mapOf(
            GameSoundEvent.PLACEMENT to soundPool.load(context, R.raw.snd_place, DEFAULT_PRIORITY),
            GameSoundEvent.BONUS_CLAIM to soundPool.load(context, R.raw.snd_bonus, DEFAULT_PRIORITY),
            GameSoundEvent.GAME_OVER to soundPool.load(context, R.raw.snd_game_over, DEFAULT_PRIORITY),
            GameSoundEvent.REJECT to soundPool.load(context, R.raw.snd_reject, DEFAULT_PRIORITY),
        )

    fun play(event: GameSoundEvent) {
        val soundId = soundIds[event] ?: return
        // Pieni satunnainen sävelkorkeusvaihtelu palan asetukseen,
        // ettei toistuva klik kuulosta konemaiselta.
        val rate =
            if (event == GameSoundEvent.PLACEMENT) {
                Random.nextDouble(RATE_VARIATION_MIN, RATE_VARIATION_MAX).toFloat()
            } else {
                NORMAL_RATE
            }
        soundPool.play(soundId, VOLUME, VOLUME, DEFAULT_PRIORITY, NO_LOOP, rate)
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val MAX_STREAMS = 2
        const val DEFAULT_PRIORITY = 1
        const val VOLUME = 1f
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f
        const val RATE_VARIATION_MIN = 0.94
        const val RATE_VARIATION_MAX = 1.06
    }
}

@Composable
fun rememberGameSoundPlayer(): GameSoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { GameSoundPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
