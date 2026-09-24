package com.softhome.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.softhome.core.designsystem.R

/**
 * Typography tokens.
 *
 * Source: design/homeApp.pen -- family "DM Sans" (headings/clock/body) + "IBM Plex Mono"
 * (code/caption). Sizes/weights/letterSpacing taken from the `.pen` text nodes:
 *   Clock  KQeDm  60 bold ls -2
 *   Hero   OHvvM  72 bold, lineHeight 0.98
 *   Head*.  lasU6 / upFsv  30-42 bold
 *   Body   uRoyh  11-16
 *   Eyebrow ttKBz 12 bold ls 2
 */
val DmSans = FontFamily(
    Font(R.font.dmsans_variable, FontWeight.Normal),
    Font(R.font.dmsans_variable, FontWeight.Medium),
    Font(R.font.dmsans_variable, FontWeight.Bold),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibmplexmono_regular, FontWeight.Normal),
)

private val trimBoth = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** App type scale. Screens read these via MaterialTheme.typography. */
val SoftTypography = Typography(
    // Clock -- big display numerals
    displayLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 60.sp, letterSpacing = (-2).sp,
        lineHeight = 60.sp, lineHeightStyle = trimBoth,
    ),
    // Hero headline
    displayMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 42.sp, lineHeight = 42.sp,
    ),
    // Drawer title / section heading
    headlineLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 42.sp, lineHeight = 44.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 34.sp,
    ),
    // Card title
    titleLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 22.sp,
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    // Weather condition / date / meta
    labelLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 14.sp,
    ),
    // Eyebrow (uppercase, tracked) + alphabet index
    labelSmall = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, letterSpacing = 2.sp, lineHeight = 14.sp,
    ),
)

/** Code / caption face (KWGT values, notes). */
val SoftMono = TextStyle(
    fontFamily = IbmPlexMono, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 22.sp,
)

// --- Warm Right Rail type styles -- from .pen znb90 / L7ZAp ---
// The new design uses NORMAL weight for the clock/date/row display (was bold).

/** Home clock "04:35" -- node IDSBb (58 normal, ls -2). */
val ClockLarge = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Normal,
    fontSize = 58.sp, letterSpacing = (-2).sp, lineHeight = 62.sp,
)

/** Home date number "22" -- node ApuhU (60 normal, ls -2). */
val DateNumber = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Normal,
    fontSize = 60.sp, letterSpacing = (-2).sp, lineHeight = 64.sp,
)

/** Row display (weather "Current 8C" / music title) -- 28 normal, ls -0.5. */
val RowDisplay = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Normal,
    fontSize = 28.sp, letterSpacing = (-0.5).sp, lineHeight = 34.sp,
)

/** Search row placeholder "f i n d  s o m e t h i n g" -- 13 normal, ls 2. */
val RowMeta = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Normal,
    fontSize = 13.sp, letterSpacing = 2.sp, lineHeight = 18.sp,
)

/** Date day "t u e s d a y" -- 9 bold, ls 1.5. */
val TweakLabel = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Bold,
    fontSize = 9.sp, letterSpacing = 1.5.sp, lineHeight = 12.sp,
)

/** Date month / music track -- 9 normal, ls 1.2 (month) or normal. */
val TweakLabelLean = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.Normal,
    fontSize = 9.sp, letterSpacing = 1.2.sp, lineHeight = 12.sp,
)
