package com.finnvek.cornersapart.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/** Koko näytön pystysuuntainen indigoliukuväri, candy-tyylin pohja. */
fun Modifier.candyBackground(): Modifier =
    background(
        Brush.verticalGradient(
            colors =
                listOf(
                    CornersApartColors.BackgroundGradientTop,
                    CornersApartColors.BackgroundGradientBottom,
                ),
        ),
    )
