package com.hazender.tropimonlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hazender.tropimonlauncher.R

// Déclaration de la police Righteous
val Righteous = FontFamily(
    Font(R.font.righteous_regular, FontWeight.Normal)
)

// Typographie complète avec Righteous comme police principale
val RighteousTypography = Typography(

    displayLarge = TextStyle(
        fontFamily = Righteous,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(fontFamily = Righteous, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = Righteous, fontSize = 36.sp),

    headlineLarge = TextStyle(fontFamily = Righteous, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Righteous, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = Righteous, fontSize = 24.sp),

    titleLarge = TextStyle(fontFamily = Righteous, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Righteous, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Righteous, fontSize = 14.sp),

    bodyLarge = TextStyle(fontFamily = Righteous, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Righteous, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Righteous, fontSize = 12.sp),

    labelLarge = TextStyle(fontFamily = Righteous, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = Righteous, fontSize = 12.sp),   // ← celui que tu utilises
    labelSmall = TextStyle(fontFamily = Righteous, fontSize = 11.sp)
)