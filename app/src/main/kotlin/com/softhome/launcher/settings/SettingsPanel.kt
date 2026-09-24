package com.softhome.launcher.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.atom.SettingsRow
import com.softhome.core.designsystem.atom.SoftToggle
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.launcher.SettingsIntents
import com.softhome.launcher.WallpaperIntents

/**
 * The SOFT / HOME settings panel (P3 / G). Sections: Appearance, Widgets, Wallpaper,
 * Gestures. No `.pen` mock exists, so this is built from the Warm Right Rail vocabulary
 * (cream r18 [SettingsRow]s on the warm background, [SoftToggle] pills).
 */
@Composable
fun SettingsPanel(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.softColors
    val context = LocalContext.current

    // P4d: SAF launchers. Export -> CreateDocument; Import -> OpenDocument. Both mirror
    // the P1.5 icon-pack flow (no storage permission needed on any API level).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportBackup(context.contentResolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.loadBackupForRestore(context.contentResolver, uri)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
            Text(
                text = "Make it yours.",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = Spacing.md),
            )

            AppearanceSection(
                themeMode = state.themeMode,
                gridColumns = state.grid.columns,
                spacing = state.spacing,
                hiddenApps = state.hiddenApps,
                onTheme = viewModel::setThemeMode,
                onColumns = viewModel::setGridColumns,
                onSpacing = viewModel::setSpacing,
                onUnhide = viewModel::unhideApp,
                onOpenSystemSettings = { context.startActivity(SettingsIntents.systemSettings(context)) },
            )

            WidgetsSection(
                rows = state.homeRows,
                onToggle = viewModel::toggleHomeRow,
                onUp = viewModel::moveHomeRowUp,
                onDown = viewModel::moveHomeRowDown,
            )

            WallpaperSection(context = context)

            GesturesSection()

            BackupSection(
                message = state.backupMessage,
                pendingRestore = state.pendingRestore,
                suggestedName = viewModel.suggestedBackupName(),
                onExport = { name -> exportLauncher.launch(name) },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onConfirm = viewModel::confirmRestore,
                onCancel = viewModel::cancelRestore,
                onConsumeMessage = viewModel::consumeBackupMessage,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.softColors.textMuted,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
    )
}

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    gridColumns: Int,
    spacing: SpacingScale,
    hiddenApps: List<HiddenApp>,
    onTheme: (ThemeMode) -> Unit,
    onColumns: (Int) -> Unit,
    onSpacing: (SpacingScale) -> Unit,
    onUnhide: (String) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("APPEARANCE")

        // Theme: cycle Light -> Dark -> System on tap (3-way control).
        SettingsRow(
            label = "Theme",
            supporting = themeMode.name,
            onClick = { onTheme(SettingsCycles.nextTheme(themeMode)) },
        )

        // Grid size: applies to the drawer grid (home is row-based).
        SettingsRow(
            label = "Drawer grid size",
            supporting = "$gridColumns columns",
            onClick = { onColumns(SettingsCycles.nextColumns(gridColumns)) },
        )

        // Spacing preset (Q3): Compact / Normal / Roomy multiplier.
        SettingsRow(
            label = "Spacing",
            supporting = spacing.name,
            onClick = { onSpacing(SettingsCycles.nextSpacing(spacing)) },
        )

        // System settings entry (Q1).
        SettingsRow(
            label = "System settings",
            supporting = "Open Android settings",
            showChevron = true,
            onClick = onOpenSystemSettings,
        )

        // Hidden apps manager (Q2).
        SettingsRow(
            label = "Hidden apps",
            supporting = if (hiddenApps.isEmpty()) "None" else "${hiddenApps.size} hidden",
        )
        hiddenApps.forEach { app ->
            SettingsRow(
                label = app.label,
                supporting = "Tap to restore",
                onClick = { onUnhide(app.componentKey) },
            )
        }
    }
}

@Composable
private fun WidgetsSection(
    rows: List<HomeRowPref>,
    onToggle: (HomeRowKind) -> Unit,
    onUp: (HomeRowKind) -> Unit,
    onDown: (HomeRowKind) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("WIDGETS")
        Text(
            text = "Choose which home rows show and their order.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.softColors.textMuted,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        rows.forEach { pref ->
            val locked = !HomeRowLogic.canHide(pref.kind)
            SettingsRow(
                label = pref.kind.displayLabel(),
                supporting = if (locked) "Always shown" else null,
                toggle = {
                    SoftToggle(
                        checked = pref.visible || locked,
                        onCheckedChange = { onToggle(pref.kind) },
                        enabled = !locked,
                        contentDescription = "${pref.kind.displayLabel()} visible",
                    )
                },
                onMoveUp = { onUp(pref.kind) },
                onMoveDown = { onDown(pref.kind) },
            )
        }
    }
}

@Composable
private fun WallpaperSection(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("WALLPAPER")
        SettingsRow(
            label = "Choose wallpaper",
            supporting = "Opens the system picker",
            showChevron = true,
            onClick = { WallpaperIntents.openPicker(context) },
        )
    }
}

@Composable
private fun GesturesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("GESTURES")
        SettingsRow(label = "Swipe up", supporting = "Opens the app drawer")
        SettingsRow(label = "Swipe down", supporting = "Coming soon", enabled = false)
        SettingsRow(label = "Double-tap", supporting = "Coming soon", enabled = false)
    }
}

/**
 * P4d: Backup & restore section. Two `SettingsRow`s drive SAF pickers (via the caller).
 * A destructive restore is gated behind an inline confirm (P4d-3), and every outcome
 * surfaces as a tappable message row.
 */
@Composable
private fun BackupSection(
    message: String?,
    pendingRestore: Boolean,
    suggestedName: String,
    onExport: (String) -> Unit,
    onImport: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onConsumeMessage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel("BACKUP & RESTORE")
        Text(
            text = "Export your rows, folders, notes, theme and icon choices to a file, or restore them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.softColors.textMuted,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        SettingsRow(
            label = "Export backup",
            supporting = "Save a backup file",
            showChevron = true,
            onClick = { onExport(suggestedName) },
        )
        SettingsRow(
            label = "Import backup",
            supporting = "Restore from a backup file",
            showChevron = true,
            onClick = onImport,
        )

        message?.let { msg ->
            MessageRow(text = msg, onClick = onConsumeMessage)
        }

        if (pendingRestore) {
            Text(
                text = "Replace all current settings with this backup?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.softColors.textPrimary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActionPill(
                    label = "Restore",
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm,
                )
                ActionPill(
                    label = "Cancel",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
            }
        }
    }
}

/** A tappable result/message row (cream card, accent text) -- mirrors the P1.5 sheet. */
@Composable
private fun MessageRow(text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.softColors
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardAlt)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
    )
}

/** Full-width action pill; reuses the P1.5 `PrimaryAction`/`SecondaryAction` vocabulary. */
@Composable
private fun ActionPill(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.softColors
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (primary) colors.onPrimaryAction else colors.textBody,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) colors.primaryAction else colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    )
}

private fun HomeRowKind.displayLabel(): String = when (this) {
    HomeRowKind.Time -> "Clock"
    HomeRowKind.Date -> "Date"
    HomeRowKind.Weather -> "Weather"
    HomeRowKind.Search -> "Search"
    HomeRowKind.Music -> "Music"
    HomeRowKind.Calendar -> "Calendar"
    HomeRowKind.BatteryStorage -> "Battery & storage"
    HomeRowKind.Notes -> "Quick notes"
}

/** Pure 3-way cycling helpers (unit-tested) used by the Appearance section. */
object SettingsCycles {
    fun nextTheme(mode: ThemeMode): ThemeMode = when (mode) {
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
        ThemeMode.System -> ThemeMode.Light
    }

    fun nextSpacing(scale: SpacingScale): SpacingScale = when (scale) {
        SpacingScale.Compact -> SpacingScale.Normal
        SpacingScale.Normal -> SpacingScale.Roomy
        SpacingScale.Roomy -> SpacingScale.Compact
    }

    /** 4 -> 5 -> 6 -> 4 (drawer columns). */
    fun nextColumns(columns: Int): Int = if (columns >= 6) 4 else columns + 1
}
