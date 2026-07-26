package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HallOfFameEntry
import com.finnvek.cornersapart.ui.components.CandyChip
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow

/**
 * Laitteen Top 20 -välilehti: pelimuotosuodatin, mitalikoroke kolmelle
 * parhaalle ja loput listana. Aktiivisen profiilin rivit korostetaan.
 */
@Composable
fun HallOfFameTab(
    hallOfFameByMode: Map<GameMode?, List<HallOfFameEntry>>,
    activeProfileName: String,
    modifier: Modifier = Modifier,
) {
    var selectedMode by remember { mutableStateOf<GameMode?>(null) }
    val entries = hallOfFameByMode[selectedMode].orEmpty()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            CandyChip(
                label = stringResource(R.string.hall_of_fame_filter_all),
                selected = selectedMode == null,
                onClick = { selectedMode = null },
            )
            GameModeUiOptions.modes.forEach { mode ->
                CandyChip(
                    label = stringResource(mode.labelRes()),
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.hall_of_fame_empty),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyLarge,
                color = CornersApartColors.TextOnDarkSecondary,
            )
        } else {
            Podium(entries = entries.take(PODIUM_SIZE), activeProfileName = activeProfileName)
            entries.drop(PODIUM_SIZE).forEachIndexed { index, item ->
                HallOfFameRow(
                    rank = PODIUM_SIZE + index + 1,
                    item = item,
                    highlighted = item.profileName == activeProfileName,
                )
            }
        }
    }
}

/** Koroke: hopea vasemmalla, kulta keskellä korkeimmalla, pronssi oikealla. */
@Composable
private fun Podium(
    entries: List<HallOfFameEntry>,
    activeProfileName: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumColumn(
            entry = entries.getOrNull(1),
            rank = 2,
            height = CornersApartSpacing.PodiumHeightSecond,
            medalColors = CornersApartColors.MedalSilver to CornersApartColors.MedalSilverDark,
            activeProfileName = activeProfileName,
            modifier = Modifier.weight(1f),
        )
        PodiumColumn(
            entry = entries.getOrNull(0),
            rank = 1,
            height = CornersApartSpacing.PodiumHeightFirst,
            medalColors = CornersApartColors.BonusAccentBright to CornersApartColors.BonusAccent,
            activeProfileName = activeProfileName,
            modifier = Modifier.weight(1f),
        )
        PodiumColumn(
            entry = entries.getOrNull(2),
            rank = 3,
            height = CornersApartSpacing.PodiumHeightThird,
            medalColors = CornersApartColors.MedalBronze to CornersApartColors.MedalBronzeDark,
            activeProfileName = activeProfileName,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PodiumColumn(
    entry: HallOfFameEntry?,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    medalColors: Pair<Color, Color>,
    activeProfileName: String,
    modifier: Modifier = Modifier,
) {
    if (entry == null) {
        Box(modifier = modifier)
        return
    }
    val (medalBright, medalDeep) = medalColors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
    ) {
        Box(
            modifier =
                Modifier
                    .size(CornersApartSpacing.PodiumMedalSize)
                    .background(
                        brush = Brush.verticalGradient(colors = listOf(medalBright, medalDeep)),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleLarge.withCandyShadow(),
                fontWeight = FontWeight.Black,
                color = CornersApartColors.TextOnDarkPrimary,
            )
        }
        Text(
            text = entry.profileName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (entry.profileName == activeProfileName) FontWeight.Bold else FontWeight.Normal,
            color =
                if (entry.profileName == activeProfileName) {
                    CornersApartColors.TextOnDarkPrimary
                } else {
                    CornersApartColors.TextOnDarkSecondary
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        medalDeep,
                                        CornersApartColors.PanelSurface,
                                    ),
                            ),
                        shape =
                            RoundedCornerShape(
                                topStart = CornersApartSpacing.PodiumRadius,
                                topEnd = CornersApartSpacing.PodiumRadius,
                            ),
                    ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = stringResource(R.string.hall_of_fame_points, entry.entry.totalScore),
                modifier = Modifier.padding(top = CornersApartSpacing.CompactGap),
                style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                fontWeight = FontWeight.Bold,
                color = CornersApartColors.TextOnDarkPrimary,
            )
        }
    }
}

@Composable
private fun HallOfFameRow(
    rank: Int,
    item: HallOfFameEntry,
    highlighted: Boolean,
) {
    val description =
        pluralStringResource(
            R.plurals.hall_of_fame_row_content_description,
            item.entry.totalScore,
            rank,
            item.profileName,
            item.entry.totalScore,
            item.entry.date,
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (highlighted) {
                            CornersApartColors.PanelSurfaceRaised
                        } else {
                            CornersApartColors.PanelSurface
                        },
                    shape = RoundedCornerShape(CornersApartSpacing.PodiumRadius),
                ).padding(
                    horizontal = CornersApartSpacing.SectionGap,
                    vertical = CornersApartSpacing.CompactGap,
                ).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#$rank",
            modifier = Modifier.width(CornersApartSpacing.HallOfFameRankWidth),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CornersApartColors.TextOnDarkMuted,
        )
        Box(
            modifier =
                Modifier
                    .size(CornersApartSpacing.HallOfFameSwatchSize)
                    .background(
                        color = CornersApartPlayerPalette.colorsFor(item.profileColorIndex).base,
                        shape = CircleShape,
                    ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.profileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                color = CornersApartColors.TextOnDarkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.entry.date} · ${stringResource(item.entry.gameMode.labelRes())}",
                style = MaterialTheme.typography.labelMedium,
                color = CornersApartColors.TextOnDarkMuted,
            )
        }
        Text(
            text = stringResource(R.string.hall_of_fame_points, item.entry.totalScore),
            style = MaterialTheme.typography.titleMedium.withCandyShadow(),
            fontWeight = FontWeight.Bold,
            color = CornersApartColors.BonusAccentBright,
        )
    }
}

private const val PODIUM_SIZE = 3
