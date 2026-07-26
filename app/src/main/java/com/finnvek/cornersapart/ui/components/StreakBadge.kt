package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.finnvek.cornersapart.R
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow

/**
 * Liekkipilleri päivähaasteputkelle: lämmin liukuväri, liekki ja putken
 * pituus. Näytetään vain kun putkea on (tai paras putki on olemassa).
 */
@Composable
fun StreakBadge(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier,
) {
    if (currentStreak <= 0 && bestStreak <= 0) return
    val shape = RoundedCornerShape(CornersApartSpacing.StreakBadgeRadius)
    Row(
        modifier =
            modifier
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    CornersApartColors.StreakFlameFace,
                                    CornersApartColors.StreakFlameDeep,
                                ),
                        ),
                    shape = shape,
                ).padding(
                    horizontal = CornersApartSpacing.SectionGap,
                    vertical = CornersApartSpacing.TinyGap,
                ),
        horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = FLAME_GLYPH,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                if (currentStreak > 0) {
                    stringResource(R.string.streak_days_label, currentStreak)
                } else {
                    stringResource(R.string.streak_best_label, bestStreak)
                },
            style = MaterialTheme.typography.titleMedium.withCandyShadow(),
            fontWeight = FontWeight.Bold,
            color = CornersApartColors.TextOnDarkPrimary,
        )
        if (currentStreak > 0 && bestStreak > currentStreak) {
            Text(
                text = stringResource(R.string.streak_best_label, bestStreak),
                style = MaterialTheme.typography.labelLarge,
                color = CornersApartColors.TextOnDarkPrimary,
            )
        }
    }
}

private const val FLAME_GLYPH = "🔥"
