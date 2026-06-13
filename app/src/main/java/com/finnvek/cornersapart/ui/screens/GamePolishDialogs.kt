package com.finnvek.cornersapart.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.PlayerUiState

@Composable
fun GameSettingsDialog(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    reducedMotionEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onReducedMotionEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                SettingSwitchRow(
                    label = stringResource(R.string.settings_sound),
                    checked = soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_haptics),
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsEnabledChange,
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_reduced_motion),
                    checked = reducedMotionEnabled,
                    onCheckedChange = onReducedMotionEnabledChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        },
    )
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CornersApartSpacing.TouchTargetMin),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun GameHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.help_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
            ) {
                HelpRuleSection(R.string.help_goal_title, R.string.help_goal_body)
                HelpRuleSection(R.string.help_start_title, R.string.help_start_body)
                HelpRuleSection(R.string.help_contact_title, R.string.help_contact_body)
                HelpRuleSection(R.string.help_scoring_title, R.string.help_scoring_body)
                HelpRuleSection(R.string.help_bonus_title, R.string.help_bonus_body)
                HelpRuleSection(R.string.help_passing_title, R.string.help_passing_body)
                HelpRuleSection(R.string.help_controls_title, R.string.help_controls_body)
                HelpRuleSection(R.string.help_nearby_title, R.string.help_nearby_body)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        },
    )
}

@Composable
private fun HelpRuleSection(
    @StringRes title: Int,
    @StringRes body: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun GameOverDialog(
    players: List<PlayerUiState>,
    durationSeconds: Int,
    onPlayAgain: () -> Unit,
    onShowStats: () -> Unit,
) {
    val duration = pluralStringResource(R.plurals.seconds_count, durationSeconds, durationSeconds)
    val rankedPlayers =
        players.sortedWith(
            compareByDescending<PlayerUiState> { player ->
                player.totalScore
            }.thenBy { player ->
                player.index
            },
        )
    val winner = rankedPlayers.firstOrNull()
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.game_over_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
            ) {
                if (winner != null) {
                    Text(
                        text = stringResource(R.string.game_over_winner, winner.name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.game_over_duration, duration),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ScoreBreakdownLabels()
                rankedPlayers.forEachIndexed { index, player ->
                    PlayerScoreBreakdown(
                        rank = index + 1,
                        player = player,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text(text = stringResource(R.string.game_over_play_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onShowStats) {
                Text(text = stringResource(R.string.stats_tab))
            }
        },
    )
}

@Composable
private fun ScoreBreakdownLabels() {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.game_over_placed_cells))
        Text(text = stringResource(R.string.game_over_bonus_tiles))
        Text(text = stringResource(R.string.game_over_completion_bonus))
        Text(text = stringResource(R.string.game_over_claimed_bonus_tiles))
    }
}

@Composable
private fun PlayerScoreBreakdown(
    rank: Int,
    player: PlayerUiState,
) {
    val score = pluralStringResource(R.plurals.points_count, player.totalScore, player.totalScore)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = CornersApartSpacing.TinyGap),
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
    ) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.game_over_ranked_player, rank, player.name, score),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text =
                stringResource(
                    R.string.game_over_breakdown_values,
                    player.placedCellPoints,
                    player.bonusTilePoints,
                    player.completionBonus,
                    player.claimedBonusTiles,
                    player.piecesPlaced,
                    player.piecesRemaining,
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
