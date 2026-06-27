package com.finnvek.cornersapart.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.ProfileUiState
import com.finnvek.cornersapart.viewmodel.ResumeGameSummary

data class GameSettingsDialogState(
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val preferredDifficulty: Int,
    val preferredMode: GameMode,
)

@Composable
fun ResumeGameDialog(
    summary: ResumeGameSummary,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.resume_game_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                Text(text = stringResource(R.string.resume_game_saved_at, summary.savedAtEpochMillis))
                Text(text = stringResource(R.string.resume_game_mode, stringResource(summary.gameMode.labelRes())))
                Text(
                    text = stringResource(R.string.resume_game_leader, summary.leadingPlayerName, summary.leadingScore),
                )
                Text(text = stringResource(R.string.resume_game_bonus_tiles, summary.claimedBonusTiles))
                Text(text = stringResource(R.string.resume_game_difficulty, summary.difficulty))
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(text = stringResource(R.string.resume_game_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onNewGame) {
                Text(text = stringResource(R.string.resume_game_new_game))
            }
        },
    )
}

@Composable
fun GameSettingsDialog(
    settings: GameSettingsDialogState,
    actions: GameSettingsActions,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
                DifficultySelector(
                    selectedDifficulty = settings.preferredDifficulty,
                    onDifficultySelected = actions.onPreferredDifficultyChange,
                )
                ModeSelector(
                    selectedMode = settings.preferredMode,
                    onModeSelected = actions.onPreferredModeChange,
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_sound),
                    checked = settings.soundEnabled,
                    onCheckedChange = actions.onSoundEnabledChange,
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_haptics),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = actions.onHapticsEnabledChange,
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
private fun DifficultySelector(
    selectedDifficulty: Int,
    onDifficultySelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.settings_difficulty), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            for (level in 1..GameConstants.DIFFICULTY_LEVELS) {
                FilterChip(
                    selected = selectedDifficulty == level,
                    onClick = { onDifficultySelected(level) },
                    label = { Text(text = stringResource(R.string.settings_difficulty_level, level)) },
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.settings_preferred_mode), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            GameModeUiOptions.modes.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(text = stringResource(mode.labelRes())) },
                )
            }
        }
    }
}

@Composable
fun ProfilesDialog(
    profiles: List<ProfileUiState>,
    onSetActiveProfile: (String) -> Unit,
    onAddProfile: (name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit,
    onUpdateProfile: (profileId: String, name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val activeProfile = profiles.firstOrNull { profile -> profile.active } ?: profiles.firstOrNull()
    var selectedProfileId by remember(profiles) { mutableStateOf(activeProfile?.id) }
    var draftName by remember(profiles) { mutableStateOf(activeProfile?.name.orEmpty()) }
    var draftColorIndex by remember(profiles) { mutableIntStateOf(activeProfile?.colorIndex ?: 0) }
    var draftAvatarStyle by remember(
        profiles,
    ) { mutableStateOf(activeProfile?.avatarStyle ?: LocalAvatarStyle.INITIALS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.profiles_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
            ) {
                profiles.forEach { profile ->
                    FilterChip(
                        selected = selectedProfileId == profile.id,
                        onClick = {
                            selectedProfileId = profile.id
                            draftName = profile.name
                            draftColorIndex = profile.colorIndex
                            draftAvatarStyle = profile.avatarStyle
                            onSetActiveProfile(profile.id)
                        },
                        label = {
                            Text(
                                text =
                                    if (profile.active) {
                                        stringResource(R.string.profile_active_label, profile.name)
                                    } else {
                                        profile.name
                                    },
                            )
                        },
                    )
                }
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text(text = stringResource(R.string.profile_name_label)) },
                    singleLine = true,
                )
                ProfileColorSelector(
                    selectedColorIndex = draftColorIndex,
                    onColorSelected = { draftColorIndex = it },
                )
                ProfileAvatarStyleSelector(
                    selectedStyle = draftAvatarStyle,
                    onStyleSelected = { draftAvatarStyle = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedId = selectedProfileId
                    if (selectedId == null) {
                        onAddProfile(draftName, draftColorIndex, draftAvatarStyle)
                    } else {
                        onUpdateProfile(selectedId, draftName, draftColorIndex, draftAvatarStyle)
                    }
                },
            ) {
                Text(text = stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAddProfile(draftName, draftColorIndex, draftAvatarStyle) }) {
                Text(text = stringResource(R.string.profile_add))
            }
        },
    )
}

@Composable
private fun ProfileColorSelector(
    selectedColorIndex: Int,
    onColorSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.profile_color_label), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            GameConstants.PLAYER_COLORS.indices.forEach { colorIndex ->
                FilterChip(
                    selected = selectedColorIndex == colorIndex,
                    onClick = { onColorSelected(colorIndex) },
                    label = { Text(text = stringResource(R.string.profile_color_option, colorIndex + 1)) },
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatarStyleSelector(
    selectedStyle: LocalAvatarStyle,
    onStyleSelected: (LocalAvatarStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(text = stringResource(R.string.profile_avatar_label), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            LocalAvatarStyle.entries.forEach { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    label = { Text(text = stringResource(style.labelRes())) },
                )
            }
        }
    }
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
    rankedScores: List<PlayerScore>,
    durationSeconds: Int,
    onPlayAgain: () -> Unit,
    onShowStats: () -> Unit,
) {
    val duration = pluralStringResource(R.plurals.seconds_count, durationSeconds, durationSeconds)
    val winner = rankedScores.firstOrNull()
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
                rankedScores.forEachIndexed { index, player ->
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
    player: PlayerScore,
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
                    R.string.game_over_ranked_score_breakdown,
                    player.scoreBreakdown.placedCellPoints,
                    player.scoreBreakdown.bonusTilePoints,
                    player.scoreBreakdown.completionBonus,
                    player.claimedBonusTiles,
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun LocalAvatarStyle.labelRes(): Int =
    when (this) {
        LocalAvatarStyle.INITIALS -> R.string.profile_avatar_initials
        LocalAvatarStyle.GEOMETRIC -> R.string.profile_avatar_geometric
        LocalAvatarStyle.MOSAIC -> R.string.profile_avatar_mosaic
        LocalAvatarStyle.RINGS -> R.string.profile_avatar_rings
    }
