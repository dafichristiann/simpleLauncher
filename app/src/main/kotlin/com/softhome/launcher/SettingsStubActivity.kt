package com.softhome.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.ThemeMode
import com.softhome.launcher.settings.SettingsPanel
import com.softhome.launcher.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings host (P1 shell -> P3 panel).
 *
 * P3 (docs/03 G1/G2): real Appearance / Widgets / Wallpaper / Gestures sections
 * (see [SettingsPanel]). Opened from the home rail's settings shortcut (Q1).
 *
 * P4c: the panel follows the persisted [ThemeMode] (Light/Dark/System) and its
 * status/nav bar appearance matches the resolved theme -- previously it was hardcoded
 * light, which left a dark panel with light status-bar icons.
 */
@AndroidEntryPoint
class SettingsStubActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (state.themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> systemDark
            }
            SoftHomeTheme(darkTheme = darkTheme) {
                val context = LocalContext.current
                SideEffect {
                    val activity = context as? ComponentActivity ?: return@SideEffect
                    SystemBarAppearance.apply(
                        activity = activity,
                        darkTheme = darkTheme,
                        hideNavBar = false,
                    )
                }
                SettingsPanel(
                    viewModel = viewModel,
                    onClose = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
