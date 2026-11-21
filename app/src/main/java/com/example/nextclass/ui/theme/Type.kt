package com.example.nextclass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Editorial scale: tight display figures, tabular clocks, tracked uppercase micro-labels. */
object NcText {
    val Display = TextStyle(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    )
    val Headline = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    )
    val Title = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    )
    val Body = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    )
    val Time = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )
    val Micro = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        fontFeatureSettings = "tnum",
    )
}

val NcTypography = Typography(
    displayLarge = NcText.Display,
    displayMedium = NcText.Display,
    displaySmall = NcText.Display,
    headlineLarge = NcText.Headline,
    headlineMedium = NcText.Headline,
    headlineSmall = NcText.Headline,
    titleLarge = NcText.Title,
    titleMedium = NcText.Title,
    titleSmall = NcText.Title,
    bodyLarge = NcText.Body,
    bodyMedium = NcText.Body,
    bodySmall = NcText.Time,
    labelLarge = NcText.Time,
    labelMedium = NcText.Micro,
    labelSmall = NcText.Micro,
)
