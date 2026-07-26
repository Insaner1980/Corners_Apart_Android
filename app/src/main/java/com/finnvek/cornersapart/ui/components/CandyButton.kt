package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow

enum class CandyButtonStyle(
    val face: Color,
    val bevel: Color,
) {
    Primary(CornersApartColors.ButtonPrimaryFace, CornersApartColors.ButtonPrimaryBevel),
    Positive(CornersApartColors.ButtonPositiveFace, CornersApartColors.ButtonPositiveBevel),
    Warn(CornersApartColors.ButtonWarnFace, CornersApartColors.ButtonWarnBevel),
    Neutral(CornersApartColors.ButtonNeutralFace, CornersApartColors.ButtonNeutralBevel),
}

/**
 * Candy-tyylinen 3D-nappi: kirkas liukuvärikansi, jonka alta näkyy tummempi
 * pohjaviiste. Painettaessa kansi painuu alas viisteen päälle.
 */
@Composable
fun CandyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CandyButtonStyle = CandyButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    CandyPressable(
        onClick = onClick,
        modifier = modifier.heightIn(min = CornersApartSpacing.CandyButtonHeight),
        style = style,
        enabled = enabled,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = CornersApartSpacing.SectionGap,
                    vertical = CornersApartSpacing.CompactGap,
                ),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.withCandyShadow(),
                color = CornersApartColors.TextOnDarkPrimary,
            )
        }
    }
}

/** Neliömäinen candy-ikoninappi. */
@Composable
fun CandyIconButton(
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CandyButtonStyle = CandyButtonStyle.Neutral,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    val semanticsModifier =
        if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        }
    CandyPressable(
        onClick = onClick,
        modifier =
            semanticsModifier.sizeIn(
                minWidth = CornersApartSpacing.TouchTargetMin,
                minHeight = CornersApartSpacing.TouchTargetMin,
            ),
        style = style,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.size(CornersApartSpacing.TouchTargetMin),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun CandyPressable(
    onClick: () -> Unit,
    style: CandyButtonStyle,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(CornersApartSpacing.CandyButtonRadius)
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else CornersApartAlpha.DisabledCandy),
        enabled = enabled,
        shape = shape,
        color = style.bevel,
        interactionSource = interactionSource,
        contentColor = CornersApartColors.TextOnDarkPrimary,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(
                        top = if (pressed) CornersApartSpacing.CandyButtonBevel else 0.dp,
                        bottom = if (pressed) 0.dp else CornersApartSpacing.CandyButtonBevel,
                    ).background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        lerp(style.face, CornersApartColors.GlossTint, FACE_TOP_LIGHTEN),
                                        style.face,
                                    ),
                            ),
                        shape = shape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private const val FACE_TOP_LIGHTEN = 0.25f
