package com.softhome.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.designsystem.theme.softColors

/**
 * Settings shell.
 *
 * P1 (docs/03 G1/G2): read-only shell. Real Appearance / Widgets / Wallpaper /
 * Gestures controls arrive in P3.
 */
class SettingsStubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoftHomeTheme { SettingsStub() }
        }
    }
}

@Composable
private fun SettingsStub() {
    val colors = MaterialTheme.softColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.labelSmall, color = colors.accent)
        Text("Make it yours.", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        Text(
            "Appearance, widgets, wallpaper and gestures will be editable here. " +
                "This screen is a shell in P1 and becomes functional in P3.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textBody,
        )
    }
}
