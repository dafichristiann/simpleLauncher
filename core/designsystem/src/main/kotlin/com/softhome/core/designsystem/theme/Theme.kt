package com.softhome.core.designsystem.theme

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SOFT / HOME theme.
 *
 * Wraps Material3 with the SOFT token system. Screens read colors through
 * [MaterialTheme.softColors] (semantic) rather than raw tokens.
 */
@Composable
fun SoftHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Launcher is edge-to-edge; status/nav bars transparent over the cream bg.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    AnimatedContent(
        targetState = darkTheme,
        transitionSpec = {
            fadeIn(MotionTokens.themeTransition()) togetherWith
                fadeOut(MotionTokens.themeTransition())
        },
        label = "themeTransition",
    ) { animatedDarkTheme ->
        val softColors = if (animatedDarkTheme) DarkSoftColors else LightSoftColors
        val scheme = if (animatedDarkTheme) {
            darkColorScheme(
                background = DarkBackground,
                surface = DarkSurface,
                surfaceVariant = DarkCard,
                primary = DarkTile,
                onPrimary = DarkOnDark,
                onBackground = DarkTextTitle,
                onSurface = DarkTextTitle,
                secondary = DarkAccent,
            )
        } else {
            lightColorScheme(
                background = SoftBackground,
                surface = SoftSurface,
                surfaceVariant = SoftCard,
                primary = SoftTile,
                onPrimary = SoftOnDark,
                onBackground = SoftTextTitle,
                onSurface = SoftTextTitle,
                secondary = SoftAccent,
            )
        }

        CompositionLocalProvider(LocalSoftColors provides softColors) {
            MaterialTheme(
                colorScheme = scheme,
                typography = SoftTypography,
                shapes = SoftShapes,
                content = content,
            )
        }
    }
}

/** Accessor: `MaterialTheme.softColors`. */
val MaterialTheme.softColors: SoftColors
    @Composable get() = LocalSoftColors.current
