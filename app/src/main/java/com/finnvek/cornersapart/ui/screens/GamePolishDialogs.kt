package com.finnvek.cornersapart.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.model.Achievement
import com.finnvek.cornersapart.model.GameConstants
import com.finnvek.cornersapart.model.GameMode
import com.finnvek.cornersapart.model.LocalAvatarStyle
import com.finnvek.cornersapart.model.PieceCatalog
import com.finnvek.cornersapart.model.PlayerScore
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyChip
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.components.CandySwitch
import com.finnvek.cornersapart.ui.components.ConfettiBurst
import com.finnvek.cornersapart.ui.components.StreakBadge
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartPlayerPalette
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow
import com.finnvek.cornersapart.viewmodel.ChallengeResult
import com.finnvek.cornersapart.viewmodel.ProfileUiState
import com.finnvek.cornersapart.viewmodel.ResumeGameSummary
import com.finnvek.cornersapart.viewmodel.RivalMatchResult

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
    modifier: Modifier = Modifier,
) {
    CandyDialog(
        title = stringResource(R.string.resume_game_title),
        onDismiss = {},
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.resume_game_new_game),
                onClick = onNewGame,
                style = CandyButtonStyle.Neutral,
            )
            CandyButton(
                text = stringResource(R.string.resume_game_continue),
                onClick = onContinue,
                style = CandyButtonStyle.Positive,
            )
        },
    ) {
        Text(text = stringResource(R.string.resume_game_saved_at, summary.savedAtEpochMillis))
        Text(text = stringResource(R.string.resume_game_mode, stringResource(summary.gameMode.labelRes())))
        Text(
            text = stringResource(R.string.resume_game_leader, summary.leadingPlayerName, summary.leadingScore),
        )
        Text(text = stringResource(R.string.resume_game_bonus_tiles, summary.claimedBonusTiles))
        Text(text = stringResource(R.string.resume_game_difficulty, summary.difficulty))
    }
}

@Composable
fun GameSettingsDialog(
    settings: GameSettingsDialogState,
    actions: GameSettingsActions,
    onDismiss: () -> Unit,
) {
    CandyDialog(
        title = stringResource(R.string.settings_title),
        onDismiss = onDismiss,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Primary,
            )
        },
    ) {
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
}

@Composable
private fun DifficultySelector(
    selectedDifficulty: Int,
    onDifficultySelected: (Int) -> Unit,
) {
    SelectorSection(title = stringResource(R.string.settings_difficulty)) {
        for (level in 1..GameConstants.DIFFICULTY_LEVELS) {
            CandyChip(
                label = stringResource(R.string.settings_difficulty_level, level),
                selected = selectedDifficulty == level,
                onClick = { onDifficultySelected(level) },
            )
        }
    }
}

@Composable
private fun ModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        SelectorTitle(stringResource(R.string.settings_preferred_mode))
        GameModeUiOptions.modes.forEach { mode ->
            CandyChip(
                label = stringResource(mode.labelRes()),
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectorTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = CornersApartColors.TextOnDarkSecondary,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileChipRow(chips: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
    ) {
        chips()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorSection(
    title: String,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        SelectorTitle(title)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            chips()
        }
    }
}

@Composable
fun ProfilesDialog(
    profiles: List<ProfileUiState>,
    onSetActiveProfile: (String) -> Unit,
    onAddProfile: (name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit,
    onUpdateProfile: (profileId: String, name: String, colorIndex: Int, avatarStyle: LocalAvatarStyle) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeProfile = profiles.firstOrNull { profile -> profile.active } ?: profiles.firstOrNull()
    var selectedProfileId by remember(profiles) { mutableStateOf(activeProfile?.id) }
    var draftName by remember(profiles) { mutableStateOf(activeProfile?.name.orEmpty()) }
    var draftColorIndex by remember(profiles) { mutableIntStateOf(activeProfile?.colorIndex ?: 0) }
    var draftAvatarStyle by remember(
        profiles,
    ) { mutableStateOf(activeProfile?.avatarStyle ?: LocalAvatarStyle.INITIALS) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    confirmDeleteId?.let { deleteId ->
        val deleteName = profiles.firstOrNull { profile -> profile.id == deleteId }?.name.orEmpty()
        CandyDialog(
            title = stringResource(R.string.profile_delete_confirm_title),
            onDismiss = { confirmDeleteId = null },
            buttons = {
                CandyButton(
                    text = stringResource(R.string.dialog_cancel),
                    onClick = { confirmDeleteId = null },
                    style = CandyButtonStyle.Neutral,
                )
                CandyButton(
                    text = stringResource(R.string.profile_delete),
                    onClick = {
                        confirmDeleteId = null
                        onDeleteProfile(deleteId)
                    },
                    style = CandyButtonStyle.Warn,
                )
            },
        ) {
            Text(
                text = stringResource(R.string.profile_delete_confirm_body, deleteName),
                style = MaterialTheme.typography.bodyLarge,
                color = CornersApartColors.TextOnDarkSecondary,
            )
        }
    }
    CandyDialog(
        title = stringResource(R.string.profiles_title),
        onDismiss = onDismiss,
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Neutral,
            )
            CandyButton(
                text = stringResource(R.string.profile_save),
                onClick = {
                    val selectedId = selectedProfileId
                    if (selectedId == null) {
                        onAddProfile(draftName, draftColorIndex, draftAvatarStyle)
                    } else {
                        onUpdateProfile(selectedId, draftName, draftColorIndex, draftAvatarStyle)
                    }
                    onDismiss()
                },
                style = CandyButtonStyle.Positive,
            )
        },
    ) {
        ProfileChipRow {
            profiles.forEach { profile ->
                CandyChip(
                    label =
                        if (profile.active) {
                            stringResource(R.string.profile_active_label, profile.name)
                        } else {
                            profile.name
                        },
                    selected = selectedProfileId == profile.id,
                    onClick = {
                        selectedProfileId = profile.id
                        draftName = profile.name
                        draftColorIndex = profile.colorIndex
                        draftAvatarStyle = profile.avatarStyle
                        onSetActiveProfile(profile.id)
                    },
                )
            }
            if (selectedProfileId == null) {
                CandyChip(
                    label = stringResource(R.string.profile_new_chip),
                    selected = true,
                    onClick = {},
                )
            }
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
        Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            CandyButton(
                text = stringResource(R.string.profile_add),
                onClick = {
                    selectedProfileId = null
                    draftName = ""
                    draftColorIndex = 0
                    draftAvatarStyle = LocalAvatarStyle.INITIALS
                },
                style = CandyButtonStyle.Primary,
            )
            val deletableId = selectedProfileId
            if (deletableId != null && profiles.size > 1) {
                CandyButton(
                    text = stringResource(R.string.profile_delete),
                    onClick = { confirmDeleteId = deletableId },
                    style = CandyButtonStyle.Warn,
                )
            }
        }
    }
}

@Composable
private fun ProfileColorSelector(
    selectedColorIndex: Int,
    onColorSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        SelectorTitle(stringResource(R.string.profile_color_label))
        Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap)) {
            GameConstants.PLAYER_COLORS.indices.forEach { colorIndex ->
                ColorSwatch(
                    colorIndex = colorIndex,
                    selected = selectedColorIndex == colorIndex,
                    onClick = { onColorSelected(colorIndex) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    colorIndex: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CornersApartPlayerPalette.colorsFor(colorIndex)
    val description = stringResource(R.string.profile_color_option, colorIndex + 1)
    Box(
        modifier =
            Modifier
                .size(CornersApartSpacing.ColorSwatchSize)
                .semantics { contentDescription = description }
                .then(
                    if (selected) {
                        Modifier.border(
                            width = CornersApartSpacing.ColorSwatchRingWidth,
                            color = CornersApartColors.TextOnDarkPrimary,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ).padding(CornersApartSpacing.TinyGap)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(colors.highlight, colors.base),
                        ),
                    shape = CircleShape,
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun ProfileAvatarStyleSelector(
    selectedStyle: LocalAvatarStyle,
    onStyleSelected: (LocalAvatarStyle) -> Unit,
) {
    SelectorSection(title = stringResource(R.string.profile_avatar_label)) {
        LocalAvatarStyle.entries.forEach { style ->
            CandyChip(
                label = stringResource(style.labelRes()),
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) },
            )
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
        CandySwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun GameHelpDialog(onDismiss: () -> Unit) {
    val placedCellPoints =
        pluralStringResource(
            R.plurals.points_count,
            GameConstants.PLACED_CELL_POINTS,
            GameConstants.PLACED_CELL_POINTS,
        )
    val bonusTilePoints =
        pluralStringResource(
            R.plurals.points_count,
            GameConstants.BONUS_TILE_POINTS,
            GameConstants.BONUS_TILE_POINTS,
        )
    CandyDialog(
        title = stringResource(R.string.help_title),
        onDismiss = onDismiss,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Primary,
            )
        },
    ) {
        HelpRuleSection(
            R.string.help_goal_title,
            stringResource(R.string.help_goal_body, placedCellPoints),
        )
        HelpRuleSection(R.string.help_start_title, stringResource(R.string.help_start_body))
        HelpRuleSection(R.string.help_contact_title, stringResource(R.string.help_contact_body))
        HelpRuleSection(
            R.string.help_scoring_title,
            pluralStringResource(
                R.plurals.help_scoring_body,
                PieceCatalog.all.size,
                placedCellPoints,
                PieceCatalog.all.size,
            ),
        )
        HelpRuleSection(
            R.string.help_bonus_title,
            stringResource(R.string.help_bonus_body, bonusTilePoints),
        )
        HelpRuleSection(R.string.help_passing_title, stringResource(R.string.help_passing_body))
        HelpRuleSection(R.string.help_controls_title, stringResource(R.string.help_controls_body))
        HelpRuleSection(R.string.help_nearby_title, stringResource(R.string.help_nearby_body))
    }
}

@Composable
private fun HelpRuleSection(
    @StringRes title: Int,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = CornersApartColors.TextOnDarkSecondary,
        )
    }
}

@Composable
fun GameOverDialog(
    rankedScores: List<PlayerScore>,
    durationSeconds: Int,
    onPlayAgain: () -> Unit,
    onShowStats: () -> Unit,
    modifier: Modifier = Modifier,
    challengeResult: ChallengeResult? = null,
    isNewBestScore: Boolean = false,
    newAchievements: List<String> = emptyList(),
    dailyBestScore: Int? = null,
    rivalResult: RivalMatchResult? = null,
    allTimeRank: Int? = null,
    allTimeRankModeLabel: String = "",
    dailyStreak: Int = 0,
) {
    val duration = pluralStringResource(R.plurals.seconds_count, durationSeconds, durationSeconds)
    val winner = rankedScores.firstOrNull()
    CandyDialog(
        title = stringResource(R.string.game_over_title),
        onDismiss = {},
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.stats_tab),
                onClick = onShowStats,
                style = CandyButtonStyle.Neutral,
            )
            CandyButton(
                text = stringResource(R.string.game_over_play_again),
                onClick = onPlayAgain,
                style = CandyButtonStyle.Positive,
            )
        },
    ) {
        if (challengeResult != null) {
            Text(
                text =
                    if (challengeResult.stars > 0) {
                        stringResource(
                            R.string.game_over_challenge_cleared,
                            challengeResult.level,
                            starsLabel(challengeResult.stars),
                        )
                    } else {
                        stringResource(R.string.game_over_challenge_failed, challengeResult.level)
                    },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium.withCandyShadow(),
                color = CornersApartColors.BonusAccentBright,
            )
        }
        if (rivalResult != null) {
            Text(
                text =
                    if (rivalResult.won) {
                        stringResource(R.string.game_over_rival_won, rivalResult.rivalName)
                    } else {
                        stringResource(R.string.game_over_rival_lost, rivalResult.rivalName)
                    },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium.withCandyShadow(),
                color =
                    if (rivalResult.won) {
                        CornersApartColors.PlayerLime
                    } else {
                        CornersApartColors.TextOnDarkSecondary
                    },
            )
            rivalResult.unlockedRivalName?.let { unlockedName ->
                Text(
                    text = stringResource(R.string.game_over_rival_unlocked, unlockedName),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                    color = CornersApartColors.BonusAccentBright,
                )
            }
        }
        if (winner != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = CornersApartSpacing.ConfettiHeight),
            ) {
                ConfettiBurst(modifier = Modifier.matchParentSize())
                Text(
                    text = stringResource(R.string.game_over_winner, winner.name),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.displayMedium.withCandyShadow(),
                    color = CornersApartColors.TextOnDarkPrimary,
                )
            }
        }
        if (isNewBestScore) {
            Text(
                text = stringResource(R.string.game_over_new_best_score),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                color = CornersApartColors.PlayerLime,
            )
        }
        if (allTimeRank != null) {
            Text(
                text = stringResource(R.string.game_over_all_time_rank, allTimeRankModeLabel, allTimeRank),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                color = CornersApartColors.BonusAccentBright,
            )
        }
        if (dailyBestScore != null) {
            Text(
                text = stringResource(R.string.game_over_daily_result, dailyBestScore),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium.withCandyShadow(),
                color = CornersApartColors.BonusAccentBright,
            )
            StreakBadge(
                currentStreak = dailyStreak,
                bestStreak = dailyStreak,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        newAchievements.mapNotNull(Achievement::fromId).forEach { achievement ->
            Text(
                text = stringResource(R.string.achievement_unlocked, stringResource(achievement.labelRes())),
                style = MaterialTheme.typography.bodyLarge,
                color = CornersApartColors.BonusAccentBright,
            )
        }
        Text(
            text = stringResource(R.string.game_over_duration, duration),
            style = MaterialTheme.typography.bodyLarge,
            color = CornersApartColors.TextOnDarkSecondary,
        )
        ScoreBreakdownLabels()
        rankedScores.forEachIndexed { index, player ->
            PlayerScoreBreakdown(
                rank = index + 1,
                player = player,
            )
        }
    }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = CornersApartColors.PanelSurface,
    ) {
        Column(
            modifier = Modifier.padding(CornersApartSpacing.CompactGap),
            verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap),
        ) {
            Text(
                text = stringResource(R.string.game_over_ranked_player, rank, player.name, score),
                style = MaterialTheme.typography.titleMedium,
            )
            HorizontalDivider(color = CornersApartColors.PanelSurfaceRaised)
            Text(
                text =
                    stringResource(
                        R.string.game_over_ranked_score_breakdown,
                        player.scoreBreakdown.placedCellPoints,
                        player.scoreBreakdown.bonusTilePoints,
                        player.scoreBreakdown.completionBonus,
                        player.claimedBonusTiles,
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = CornersApartColors.TextOnDarkSecondary,
            )
        }
    }
}

@StringRes
fun Achievement.labelRes(): Int =
    when (this) {
        Achievement.FIRST_WIN -> R.string.achievement_first_win
        Achievement.BONUS_HUNTER -> R.string.achievement_bonus_hunter
        Achievement.ALL_PIECES -> R.string.achievement_all_pieces
        Achievement.EXPERT_WIN -> R.string.achievement_expert_win
        Achievement.WIN_STREAK_3 -> R.string.achievement_win_streak_3
        Achievement.PERFECT_LEVEL -> R.string.achievement_perfect_level
        Achievement.CHALLENGE_CHAMP -> R.string.achievement_challenge_champ
    }

private fun LocalAvatarStyle.labelRes(): Int =
    when (this) {
        LocalAvatarStyle.INITIALS -> R.string.profile_avatar_initials
        LocalAvatarStyle.GEOMETRIC -> R.string.profile_avatar_geometric
        LocalAvatarStyle.MOSAIC -> R.string.profile_avatar_mosaic
        LocalAvatarStyle.RINGS -> R.string.profile_avatar_rings
    }
