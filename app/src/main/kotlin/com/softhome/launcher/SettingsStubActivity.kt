package com.softhome.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.launcher.settings.SettingsPanel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings host (P1 shell -> P3 panel).
 *
 * P3 (docs/03 G1/G2): real Appearance / Widgets / Wallpaper / Gestures sections
 * (see [SettingsPanel]). Opened from the home rail's settings shortcut (Q1).
 */
@AndroidEntryPoint
class SettingsStubActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftHomeTheme {
                val context = LocalContext.current
                SideEffect {
                    val activity = context as? ComponentActivity ?: return@SideEffect
                    SystemBarAppearance.apply(
                        activity = activity,
                        darkTheme = false, // panel is always the light cream surface in P3
                        hideNavBar = false,
                    )
                }
                SettingsPanel(
                    onClose = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
