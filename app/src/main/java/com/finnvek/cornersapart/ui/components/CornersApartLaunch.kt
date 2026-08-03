package com.finnvek.cornersapart.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.theme.candyBackground
import kotlinx.coroutines.delay

@Composable
fun CornersApartLaunch(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var splashVisible by rememberSaveable { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (splashVisible) {
            LaunchSplashOverlay(onFinish = { splashVisible = false })
        }
    }
}

@Composable
private fun LaunchSplashOverlay(onFinish: () -> Unit) {
    val assemblyProgress = remember { Animatable(0f) }
    val logoScale = remember { Animatable(LOGO_INITIAL_SCALE) }
    val overlayAlpha = remember { Animatable(1f) }
    val currentOnFinish by rememberUpdatedState(onFinish)

    LaunchedEffect(Unit) {
        assemblyProgress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = ASSEMBLY_TIMELINE_MILLIS,
                    easing = LinearEasing,
                ),
        )
        logoScale.animateTo(
            targetValue = LOGO_SETTLE_SCALE,
            animationSpec = tween(LOGO_SETTLE_IN_MILLIS, easing = FastOutLinearInEasing),
        )
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(LOGO_SETTLE_OUT_MILLIS, easing = FastOutSlowInEasing),
        )
        delay(LOGO_HOLD_MILLIS.toLong())
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(OVERLAY_FADE_MILLIS),
        )
        currentOnFinish()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overlayAlpha.value }
                .candyBackground()
                .clearAndSetSemantics {}
                .pointerInput(Unit) {
                    while (true) {
                        awaitPointerEventScope {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val logoSize =
                minOf(
                    maxWidth * LOGO_WIDTH_FRACTION,
                    maxHeight * LOGO_HEIGHT_FRACTION,
                    LOGO_MAX_SIZE,
                )
            Box(
                modifier =
                    Modifier
                        .size(logoSize)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        },
            ) {
                splashPieceSpecs.forEach { spec ->
                    SplashPiece(
                        spec = spec,
                        timelineProgress = assemblyProgress.value,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashPiece(
    spec: SplashPieceSpec,
    timelineProgress: Float,
) {
    val timelineMillis = timelineProgress * ASSEMBLY_TIMELINE_MILLIS
    val rawProgress =
        ((timelineMillis - spec.delayMillis) / PIECE_TRAVEL_MILLIS)
            .coerceIn(0f, 1f)
    val progress = FastOutSlowInEasing.transform(rawProgress)
    val remaining = 1f - progress

    Image(
        painter = painterResource(spec.imageRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = size.width * spec.startXFraction * remaining
                    translationY = size.height * spec.startYFraction * remaining
                    rotationZ = spec.startRotationDegrees * remaining
                    scaleX = PIECE_INITIAL_SCALE + (1f - PIECE_INITIAL_SCALE) * progress
                    scaleY = PIECE_INITIAL_SCALE + (1f - PIECE_INITIAL_SCALE) * progress
                    alpha = progress
                },
    )
}

private data class SplashPieceSpec(
    val imageRes: Int,
    val startXFraction: Float,
    val startYFraction: Float,
    val startRotationDegrees: Float,
    val delayMillis: Float,
)

private val splashPieceSpecs =
    listOf(
        SplashPieceSpec(R.drawable.splash_piece_cyan, -0.72f, -0.82f, -11f, 0f),
        SplashPieceSpec(R.drawable.splash_piece_orange, 0.76f, -0.78f, 12f, 50f),
        SplashPieceSpec(R.drawable.splash_piece_pink, -0.78f, 0.78f, 10f, 100f),
        SplashPieceSpec(R.drawable.splash_piece_green, 0.74f, 0.82f, -10f, 150f),
    )

private const val ASSEMBLY_TIMELINE_MILLIS = 720
private const val PIECE_TRAVEL_MILLIS = 570f
private const val PIECE_INITIAL_SCALE = 0.80f
private const val LOGO_INITIAL_SCALE = 0.98f
private const val LOGO_SETTLE_SCALE = 1.035f
private const val LOGO_SETTLE_IN_MILLIS = 80
private const val LOGO_SETTLE_OUT_MILLIS = 120
private const val LOGO_HOLD_MILLIS = 60
private const val OVERLAY_FADE_MILLIS = 180
private const val LOGO_WIDTH_FRACTION = 0.76f
private const val LOGO_HEIGHT_FRACTION = 0.76f
private val LOGO_MAX_SIZE = 320.dp
