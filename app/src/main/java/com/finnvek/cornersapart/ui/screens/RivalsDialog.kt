package com.finnvek.cornersapart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.components.CandyButton
import com.finnvek.cornersapart.ui.components.CandyButtonStyle
import com.finnvek.cornersapart.ui.components.CandyDialog
import com.finnvek.cornersapart.ui.components.RivalAvatar
import com.finnvek.cornersapart.ui.theme.CornersApartAlpha
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.viewmodel.RivalUiState

/**
 * Rivals-galleria: tikapuulista nimettyjä konevastustajia. Seuraava haastaja
 * saa hehkuvan värikehyksen; lukitut kortit ovat himmennettyjä.
 */
@Composable
fun RivalsDialog(
    rivals: List<RivalUiState>,
    onChallengeRival: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CandyDialog(
        title = stringResource(R.string.rivals_title),
        onDismiss = onDismiss,
        modifier = modifier,
        buttons = {
            CandyButton(
                text = stringResource(R.string.dialog_close),
                onClick = onDismiss,
                style = CandyButtonStyle.Neutral,
            )
        },
    ) {
        val defeatedCount = rivals.count { rival -> rival.defeated }
        Text(
            text = stringResource(R.string.rivals_subtitle),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyLarge,
            color = CornersApartColors.TextOnDarkSecondary,
        )
        Text(
            text =
                pluralStringResource(
                    R.plurals.rivals_defeated_count,
                    defeatedCount,
                    defeatedCount,
                    rivals.size,
                ),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium,
            color = CornersApartColors.BonusAccentBright,
        )
        rivals.forEachIndexed { index, rival ->
            RivalCard(
                rival = rival,
                previousRivalName = rivals.getOrNull(index - 1)?.name,
                onChallenge = {
                    onChallengeRival(rival.id)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun RivalCard(
    rival: RivalUiState,
    previousRivalName: String?,
    onChallenge: () -> Unit,
) {
    val shape = RoundedCornerShape(CornersApartSpacing.RivalCardRadius)
    val challengerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    CornersApartColors.PlayerPink,
                    CornersApartColors.PlayerMango,
                    CornersApartColors.PlayerLime,
                    CornersApartColors.PlayerCyan,
                ),
        )
    val cardDescription =
        if (rival.unlocked) {
            val wins = pluralStringResource(R.plurals.rival_wins_count, rival.wins, rival.wins)
            val losses = pluralStringResource(R.plurals.rival_losses_count, rival.losses, rival.losses)
            stringResource(R.string.rival_card_content_description, rival.name, rival.tier, wins, losses)
        } else {
            stringResource(R.string.rival_locked_content_description, rival.name)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (rival.isNextChallenger) {
                        Modifier.border(CornersApartSpacing.RivalChallengerBorderWidth, challengerBrush, shape)
                    } else {
                        Modifier
                    },
                ).clip(shape)
                .background(CornersApartColors.PanelSurface)
                .clickable(enabled = rival.unlocked, onClick = onChallenge)
                .alpha(if (rival.unlocked) 1f else CornersApartAlpha.DisabledCandy)
                .padding(CornersApartSpacing.SectionGap)
                .semantics { contentDescription = cardDescription },
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            RivalAvatar(
                style = rival.style,
                colorIndex = rival.colorIndex,
                modifier = Modifier.size(CornersApartSpacing.RivalAvatarSize),
                showCrown = rival.tier == MASTER_TIER,
            )
            if (!rival.unlocked) {
                Text(
                    text = LOCK_GLYPH,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rival.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CornersApartColors.TextOnDarkPrimary,
                )
                if (rival.defeated) {
                    Text(
                        text = "$CROWN_GLYPH ${stringResource(R.string.rival_defeated_badge)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CornersApartColors.PlayerLime,
                    )
                } else if (rival.isNextChallenger) {
                    Text(
                        text = stringResource(R.string.rival_next_challenger),
                        style = MaterialTheme.typography.labelLarge,
                        color = CornersApartColors.BonusAccentBright,
                    )
                }
            }
            Text(
                text =
                    if (rival.unlocked || previousRivalName == null) {
                        stringResource(rival.taglineRes())
                    } else {
                        stringResource(R.string.rival_locked_hint, previousRivalName)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = CornersApartColors.TextOnDarkSecondary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DifficultyPips(tier = rival.tier)
                if (rival.wins > 0 || rival.losses > 0) {
                    Text(
                        text = stringResource(R.string.rival_record, rival.wins, rival.losses),
                        style = MaterialTheme.typography.labelLarge,
                        color = CornersApartColors.TextOnDarkMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyPips(tier: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.TinyGap)) {
        repeat(MASTER_TIER) { index ->
            Box(
                modifier =
                    Modifier
                        .size(CornersApartSpacing.RivalPipSize)
                        .background(
                            color =
                                if (index < tier) {
                                    CornersApartColors.BonusAccentBright
                                } else {
                                    CornersApartColors.TextOnDarkMuted
                                },
                            shape = CircleShape,
                        ),
            )
        }
    }
}

private fun RivalUiState.taglineRes(): Int =
    when (id) {
        "jelly" -> R.string.rival_tagline_jelly
        "pip" -> R.string.rival_tagline_pip
        "sprout" -> R.string.rival_tagline_sprout
        "coco" -> R.string.rival_tagline_coco
        "dash" -> R.string.rival_tagline_dash
        "fig" -> R.string.rival_tagline_fig
        "blaze" -> R.string.rival_tagline_blaze
        "luna" -> R.string.rival_tagline_luna
        "onyx" -> R.string.rival_tagline_onyx
        "nova" -> R.string.rival_tagline_nova
        "vex" -> R.string.rival_tagline_vex
        else -> R.string.rival_tagline_sol
    }

internal const val MASTER_TIER = 6
private const val LOCK_GLYPH = "🔒"
private const val CROWN_GLYPH = "♛"
