package com.finnvek.cornersapart.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.cornersapart.R

private val NunitoFontFamily =
    FontFamily(
        Font(R.font.nunito_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
        Font(R.font.nunito_black, FontWeight.Black),
    )

/** Pehmeä pudotusvarjo isoille otsikoille ja pistenumeroille. */
fun TextStyle.withCandyShadow(): TextStyle =
    copy(
        shadow =
            Shadow(
                color = CornersApartColors.TextShadow.copy(alpha = CornersApartAlpha.TextShadow),
                offset = Offset(0f, 2f),
                blurRadius = 4f,
            ),
    )

val CornersApartTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            ),
    )
