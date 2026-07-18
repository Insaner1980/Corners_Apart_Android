package com.finnvek.cornersapart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.finnvek.cornersapart.ui.theme.CornersApartColors
import com.finnvek.cornersapart.ui.theme.CornersApartSpacing
import com.finnvek.cornersapart.ui.theme.withCandyShadow

/**
 * Candy-tyylinen dialogi: pyöristetty indigopaneeli, jonka alareunasta näkyy
 * tummempi pohjaviiste, iso keskitetty otsikko ja nappirivi alhaalla.
 */
@Composable
fun CandyDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(CornersApartSpacing.DialogRadius)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier =
                modifier
                    .background(CornersApartColors.DialogSurfaceEdge, shape)
                    .padding(bottom = CornersApartSpacing.CandyButtonBevel)
                    .background(CornersApartColors.DialogSurface, shape),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(CornersApartSpacing.ScreenPadding)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CornersApartSpacing.SectionGap),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.headlineMedium.withCandyShadow(),
                    color = CornersApartColors.TextOnDarkPrimary,
                )
                content()
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(CornersApartSpacing.CompactGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    buttons()
                }
            }
        }
    }
}

