package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing

/** Iso kiiltävä kytkin candy-tyyliin. */
@Composable
fun CandySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier =
            modifier.graphicsLayer {
                scaleX = SWITCH_SCALE
                scaleY = SWITCH_SCALE
            },
        colors =
            SwitchDefaults.colors(
                checkedTrackColor = CornersApartColors.ButtonPositiveFace,
                checkedThumbColor = CornersApartColors.TextOnDarkPrimary,
                uncheckedTrackColor = CornersApartColors.PanelSurfaceRaised,
                uncheckedThumbColor = CornersApartColors.TextOnDarkSecondary,
                uncheckedBorderColor = CornersApartColors.TextOnDarkMuted,
            ),
    )
}

/** Pillerinmuotoinen valintachip candy-tyyliin. */
@Composable
fun CandyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val face = if (selected) CornersApartColors.ButtonPrimaryFace else CornersApartColors.PanelSurface
    val bevel = if (selected) CornersApartColors.ButtonPrimaryBevel else CornersApartColors.ButtonNeutralBevel
    CandyChipBody(
        label = label,
        face = face,
        bevel = bevel,
        textColor =
            if (selected) {
                CornersApartColors.TextOnDarkPrimary
            } else {
                CornersApartColors.TextOnDarkSecondary
            },
        modifier = modifier.selectable(selected = selected, role = Role.Button, onClick = onClick),
    )
}

@Composable
fun CandyStatusChip(
    label: String,
    face: androidx.compose.ui.graphics.Color,
    bevel: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    CandyChipBody(
        label = label,
        face = face,
        bevel = bevel,
        textColor = CornersApartColors.TextOnDarkPrimary,
        modifier = modifier,
    )
}

@Composable
private fun CandyChipBody(
    label: String,
    face: androidx.compose.ui.graphics.Color,
    bevel: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = CornersApartSpacing.TouchTargetMin)
                .background(bevel, CircleShape)
                .padding(bottom = CornersApartSpacing.CandyButtonBevel / 2)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(lerp(face, CornersApartColors.GlossTint, CHIP_TOP_LIGHTEN), face),
                        ),
                    shape = CircleShape,
                ).padding(
                    horizontal = CornersApartSpacing.SectionGap,
                    vertical = CornersApartSpacing.CompactGap,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

private const val SWITCH_SCALE = 1.15f
private const val CHIP_TOP_LIGHTEN = 0.25f
