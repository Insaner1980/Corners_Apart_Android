package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.finnvek.cornersapart.opponents.OpponentStyle
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette

/**
 * Piirretty Rivals-hahmokasvo: candy-laatta, jonka päällä on silmät ja tyylin
 * mukainen ilme (Expansionist: leveä hymy, Opportunist: virnistys + kohotettu
 * kulma, Blocker: päättäväinen suora suu ja kulmakarvat). Master-tason hahmot
 * saavat kultaisen kruunun.
 */
@Composable
fun RivalAvatar(
    style: OpponentStyle,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    showCrown: Boolean = false,
    alpha: Float = 1f,
) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    Canvas(modifier = modifier) {
        val side = size.minDimension
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val tileTop = if (showCrown) side * CROWN_TILE_OFFSET_FRACTION else 0f
        val tileSize = side - tileTop
        drawCandyCell(
            topLeft = topLeft + Offset((side - tileSize) / 2f, tileTop),
            cellSize = tileSize,
            colors = colors,
            alpha = alpha,
        )
        val faceOrigin = topLeft + Offset((side - tileSize) / 2f, tileTop)
        drawFace(origin = faceOrigin, side = tileSize, style = style, alpha = alpha)
        if (showCrown) {
            drawCrown(origin = faceOrigin, side = tileSize, alpha = alpha)
        }
    }
}

private fun DrawScope.drawFace(
    origin: Offset,
    side: Float,
    style: OpponentStyle,
    alpha: Float,
) {
    val stroke = side * FEATURE_STROKE_FRACTION
    val eyeRadius = side * EYE_RADIUS_FRACTION
    val pupilRadius = side * PUPIL_RADIUS_FRACTION
    val eyeY = side * EYE_Y_FRACTION
    val leftEye = origin + Offset(side * LEFT_EYE_X_FRACTION, eyeY)
    val rightEye = origin + Offset(side * RIGHT_EYE_X_FRACTION, eyeY)
    val pupilShift =
        when (style) {
            OpponentStyle.OPPORTUNIST -> Offset(pupilRadius * PUPIL_SIDE_GLANCE, 0f)
            else -> Offset(0f, pupilRadius * PUPIL_DOWN_REST)
        }
    listOf(leftEye, rightEye).forEach { center ->
        drawCircle(
            color = CornersApartColors.TextOnDarkPrimary.copy(alpha = alpha),
            radius = eyeRadius,
            center = center,
        )
        drawCircle(
            color = CornersApartColors.TextShadow.copy(alpha = alpha),
            radius = pupilRadius,
            center = center + pupilShift,
        )
        drawCircle(
            color = CornersApartColors.TextOnDarkPrimary.copy(alpha = alpha),
            radius = pupilRadius * PUPIL_GLINT_FRACTION,
            center = center + pupilShift + Offset(-pupilRadius * PUPIL_GLINT_OFFSET, -pupilRadius * PUPIL_GLINT_OFFSET),
        )
    }
    when (style) {
        OpponentStyle.EXPANSIONIST -> drawSmile(origin, side, stroke, alpha)
        OpponentStyle.OPPORTUNIST -> drawSmirk(origin, side, stroke, alpha, leftEye, eyeRadius)
        OpponentStyle.BLOCKER -> drawDeterminedFace(origin, side, stroke, alpha, leftEye, rightEye, eyeRadius)
    }
}

private fun DrawScope.drawSmile(
    origin: Offset,
    side: Float,
    stroke: Float,
    alpha: Float,
) {
    drawArc(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        startAngle = SMILE_START_ANGLE,
        sweepAngle = SMILE_SWEEP_ANGLE,
        useCenter = false,
        topLeft = origin + Offset(side * SMILE_LEFT_FRACTION, side * SMILE_TOP_FRACTION),
        size = Size(side * SMILE_WIDTH_FRACTION, side * SMILE_HEIGHT_FRACTION),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawSmirk(
    origin: Offset,
    side: Float,
    stroke: Float,
    alpha: Float,
    leftEye: Offset,
    eyeRadius: Float,
) {
    drawArc(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        startAngle = SMIRK_START_ANGLE,
        sweepAngle = SMIRK_SWEEP_ANGLE,
        useCenter = false,
        topLeft = origin + Offset(side * SMIRK_LEFT_FRACTION, side * SMIRK_TOP_FRACTION),
        size = Size(side * SMIRK_WIDTH_FRACTION, side * SMIRK_HEIGHT_FRACTION),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawLine(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        start = leftEye + Offset(-eyeRadius, -eyeRadius * BROW_RAISE_HIGH),
        end = leftEye + Offset(eyeRadius, -eyeRadius * BROW_RAISE_LOW),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawDeterminedFace(
    origin: Offset,
    side: Float,
    stroke: Float,
    alpha: Float,
    leftEye: Offset,
    rightEye: Offset,
    eyeRadius: Float,
) {
    val mouthY = origin.y + side * MOUTH_FLAT_Y_FRACTION
    drawLine(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        start = Offset(origin.x + side * MOUTH_FLAT_LEFT_FRACTION, mouthY),
        end = Offset(origin.x + side * MOUTH_FLAT_RIGHT_FRACTION, mouthY),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        start = leftEye + Offset(-eyeRadius, -eyeRadius * BROW_RAISE_HIGH),
        end = leftEye + Offset(eyeRadius, -eyeRadius * BROW_ANGRY_INNER),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = CornersApartColors.TextShadow.copy(alpha = alpha * MOUTH_ALPHA),
        start = rightEye + Offset(-eyeRadius, -eyeRadius * BROW_ANGRY_INNER),
        end = rightEye + Offset(eyeRadius, -eyeRadius * BROW_RAISE_HIGH),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCrown(
    origin: Offset,
    side: Float,
    alpha: Float,
) {
    val crownRect =
        Rect(
            offset = origin + Offset(side * CROWN_LEFT_FRACTION, -side * CROWN_HEIGHT_FRACTION * CROWN_OVERLAP),
            size = Size(side * CROWN_WIDTH_FRACTION, side * CROWN_HEIGHT_FRACTION),
        )
    val path =
        Path().apply {
            moveTo(crownRect.left, crownRect.bottom)
            lineTo(crownRect.left, crownRect.top + crownRect.height * CROWN_NOTCH_FRACTION)
            lineTo(crownRect.left + crownRect.width * CROWN_QUARTER, crownRect.top + crownRect.height * CROWN_DIP_FRACTION)
            lineTo(crownRect.center.x, crownRect.top)
            lineTo(crownRect.right - crownRect.width * CROWN_QUARTER, crownRect.top + crownRect.height * CROWN_DIP_FRACTION)
            lineTo(crownRect.right, crownRect.top + crownRect.height * CROWN_NOTCH_FRACTION)
            lineTo(crownRect.right, crownRect.bottom)
            close()
        }
    drawPath(path = path, color = CornersApartColors.BonusAccent.copy(alpha = alpha))
    drawPath(
        path = path,
        color = CornersApartColors.BonusAccentBright.copy(alpha = alpha * CROWN_FACE_ALPHA),
    )
}

private const val FEATURE_STROKE_FRACTION = 0.045f
private const val EYE_RADIUS_FRACTION = 0.13f
private const val PUPIL_RADIUS_FRACTION = 0.062f
private const val EYE_Y_FRACTION = 0.42f
private const val LEFT_EYE_X_FRACTION = 0.33f
private const val RIGHT_EYE_X_FRACTION = 0.67f
private const val PUPIL_SIDE_GLANCE = 0.55f
private const val PUPIL_DOWN_REST = 0.20f
private const val PUPIL_GLINT_FRACTION = 0.38f
private const val PUPIL_GLINT_OFFSET = 0.35f
private const val MOUTH_ALPHA = 0.78f

private const val SMILE_START_ANGLE = 25f
private const val SMILE_SWEEP_ANGLE = 130f
private const val SMILE_LEFT_FRACTION = 0.28f
private const val SMILE_TOP_FRACTION = 0.38f
private const val SMILE_WIDTH_FRACTION = 0.44f
private const val SMILE_HEIGHT_FRACTION = 0.36f

private const val SMIRK_START_ANGLE = 40f
private const val SMIRK_SWEEP_ANGLE = 85f
private const val SMIRK_LEFT_FRACTION = 0.38f
private const val SMIRK_TOP_FRACTION = 0.42f
private const val SMIRK_WIDTH_FRACTION = 0.34f
private const val SMIRK_HEIGHT_FRACTION = 0.30f

private const val MOUTH_FLAT_Y_FRACTION = 0.70f
private const val MOUTH_FLAT_LEFT_FRACTION = 0.36f
private const val MOUTH_FLAT_RIGHT_FRACTION = 0.64f

private const val BROW_RAISE_HIGH = 2.1f
private const val BROW_RAISE_LOW = 1.5f
private const val BROW_ANGRY_INNER = 1.4f

private const val CROWN_TILE_OFFSET_FRACTION = 0.14f
private const val CROWN_LEFT_FRACTION = 0.30f
private const val CROWN_WIDTH_FRACTION = 0.40f
private const val CROWN_HEIGHT_FRACTION = 0.20f
private const val CROWN_OVERLAP = 0.55f
private const val CROWN_NOTCH_FRACTION = 0.25f
private const val CROWN_DIP_FRACTION = 0.45f
private const val CROWN_QUARTER = 0.25f
private const val CROWN_FACE_ALPHA = 0.55f
