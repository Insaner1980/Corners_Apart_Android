package com.finnvek.cornersapart.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import kotlin.random.Random

private data class ConfettiParticle(
    val color: Color,
    val startXFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val sizeFraction: Float,
    val rotationDegrees: Float,
)

/** Kertaluonteinen konfettisade pelaajaväreissä — voittajan juhlistus. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = CONFETTI_DURATION_MS, easing = LinearEasing),
        )
    }
    val particles =
        remember {
            val random = Random(RANDOM_SEED)
            List(CONFETTI_COUNT) { index ->
                ConfettiParticle(
                    color = CornersApartPlayerPalette.colorsFor(index % PLAYER_COLOR_COUNT).base,
                    startXFraction = random.nextFloat(),
                    velocityX = (random.nextFloat() - 0.5f) * HORIZONTAL_SPREAD,
                    velocityY = -(MIN_LAUNCH_SPEED + random.nextFloat() * LAUNCH_SPEED_RANGE),
                    sizeFraction = MIN_SIZE_FRACTION + random.nextFloat() * SIZE_FRACTION_RANGE,
                    rotationDegrees = (random.nextFloat() - 0.5f) * MAX_SPIN_DEGREES,
                )
            }
        }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        particles.forEach { particle ->
            val x = size.width * (particle.startXFraction + particle.velocityX * t)
            val y = size.height * (LAUNCH_BASELINE + particle.velocityY * t + GRAVITY * t * t)
            val particleSize = size.height * particle.sizeFraction
            rotate(degrees = particle.rotationDegrees * t, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = particle.color.copy(alpha = 1f - t * t),
                    topLeft = Offset(x, y),
                    size = Size(particleSize, particleSize * PARTICLE_ASPECT),
                    cornerRadius = CornerRadius(particleSize * PARTICLE_CORNER_FRACTION),
                )
            }
        }
    }
}

private const val CONFETTI_DURATION_MS = 1500
private const val CONFETTI_COUNT = 48
private const val PLAYER_COLOR_COUNT = 4
private const val RANDOM_SEED = 20260719
private const val HORIZONTAL_SPREAD = 0.7f
private const val MIN_LAUNCH_SPEED = 0.8f
private const val LAUNCH_SPEED_RANGE = 1.2f
private const val LAUNCH_BASELINE = 0.9f
private const val GRAVITY = 1.4f
private const val MIN_SIZE_FRACTION = 0.05f
private const val SIZE_FRACTION_RANGE = 0.06f
private const val MAX_SPIN_DEGREES = 720f
private const val PARTICLE_ASPECT = 0.6f
private const val PARTICLE_CORNER_FRACTION = 0.3f
