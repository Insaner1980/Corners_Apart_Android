package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.Achievement
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.HallOfFameEntry
import com.finnvek.cornersapart.model.HistoryEntry
import com.finnvek.cornersapart.model.HistoryStats
import com.finnvek.cornersapart.model.HistoryStatsCalculator
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing

@Composable
fun HistoryStatsDialog(
    history: List<HistoryEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    stats: HistoryStats = HistoryStatsCalculator.calculate(history),
    unlockedAchievements: Set<String> = emptySet(),
    hallOfFameByMode: Map<GameMode?, List<HallOfFameEntry>> = emptyMap(),
    activeProfileName: String = "",
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    CandyDialog(
        title = stringResource(R.string.history_stats_title),
        onDismiss = onDismiss,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Primary,
            )
        },
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(text = stringResource(R.string.history_tab)) },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(text = stringResource(R.string.stats_tab)) },
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(text = stringResource(R.string.hall_of_fame_tab)) },
                )
            }
            when (selectedTabIndex) {
                0 -> {
                    HistoryTab(history)
                }

                1 -> {
                    StatsTab(stats)
                    AchievementsSection(unlockedAchievements)
                }

                else -> {
                    HallOfFameTab(
                        hallOfFameByMode = hallOfFameByMode,
                        activeProfileName = activeProfileName,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<HistoryEntry>) {
    if (history.isEmpty()) {
        Text(text = stringResource(R.string.history_empty))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
            history.takeLast(HISTORY_ROW_LIMIT).forEach { entry ->
                val points = pluralStringResource(R.plurals.points_count, entry.totalScore, entry.totalScore)
                val seconds = pluralStringResource(R.plurals.seconds_count, entry.timeSeconds, entry.timeSeconds)
                Text(
                    text =
                        stringResource(
                            R.string.history_row,
                            entry.date,
                            entry.rank,
                            points,
                            entry.difficulty,
                            seconds,
                        ),
                )
            }
        }
    }
}

@Composable
private fun StatsTab(stats: HistoryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        StatRow(label = stringResource(R.string.stats_total_games), value = stats.totalGamesPlayed.toString())
        StatRow(label = stringResource(R.string.stats_wins), value = stats.winCount.toString())
        StatRow(label = stringResource(R.string.stats_average_score), value = stats.averageScore.formatOneDecimal())
        StatRow(label = stringResource(R.string.stats_best_score), value = stats.bestScore.toString())
        StatRow(label = stringResource(R.string.stats_average_rank), value = stats.averageRank.formatOneDecimal())
        StatRow(
            label = stringResource(R.string.stats_average_bonus_tiles),
            value = stats.averageClaimedBonusTiles.formatOneDecimal(),
        )
    }
}

@Composable
private fun AchievementsSection(unlockedAchievements: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(
            text = stringResource(R.string.achievements_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Achievement.entries.forEach { achievement ->
            val unlocked = achievement.id in unlockedAchievements
            Text(
                text = "${if (unlocked) "★" else "☆"} ${stringResource(achievement.labelRes())}",
                modifier = Modifier.alpha(if (unlocked) 1f else CornersApartAlpha.PassedPlayer),
                color =
                    if (unlocked) {
                        CornersApartColors.BonusAccentBright
                    } else {
                        CornersApartColors.TextOnDarkSecondary
                    },
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = CornersApartSpacing.TinyGap),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label)
        Text(text = value)
    }
}

private fun Double.formatOneDecimal(): String = "%1.1f".format(this)

private const val HISTORY_ROW_LIMIT = 20
