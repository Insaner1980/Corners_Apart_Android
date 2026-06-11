package com.finnvek.cornersapart.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.cornersapart.R

private val QuicksandFontFamily =
    FontFamily(
        Font(R.font.quicksand, FontWeight.Normal),
        Font(R.font.quicksand, FontWeight.Medium),
        Font(R.font.quicksand, FontWeight.SemiBold),
        Font(R.font.quicksand, FontWeight.Bold),
    )

val CornersApartTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = QuicksandFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
            ),
    )
