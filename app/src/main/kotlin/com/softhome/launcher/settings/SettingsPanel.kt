package com.softhome.launcher.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.atom.SettingsRow
import com.softhome.core.designsystem.atom.SoftToggle
import com.softhome.core.designsystem.atom.warmPress
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.launcher.SettingsIntents
import com.softhome.launcher.WallpaperIntents
import com.softhome.launcher.BuildConfig
import com.softhome.launcher.LauncherRole
import com.softhome.launcher.R

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
    onOpenIconPack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.softColors
    val context = LocalContext.current
    var customizeSidebar by remember { mutableStateOf(false) }

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

    if (customizeSidebar) {
        CustomizeSidebarScreen(
            state = state,
            onBack = { customizeSidebar = false },
            onAddApp = viewModel::addRailApp,
            onAddShortcut = viewModel::addRailShortcut,
            onRemove = viewModel::removeRailItem,
            onMove = viewModel::moveRailItem,
            onReset = viewModel::resetRailItems,
            modifier = modifier.fillMaxSize(),
        )
        return
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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = Spacing.md),
            )

            AppearanceSection(
                themeMode = state.themeMode,
                gridColumns = state.grid.columns,
                spacing = state.spacing,
                hiddenApps = state.hiddenApps,
                isDefaultLauncher = LauncherRole.isDefaultLauncher(context),
                onSetDefaultLauncher = { LauncherRole.requestDefaultLauncher(context) },
                onTheme = viewModel::setThemeMode,
                onColumns = viewModel::setGridColumns,
                onSpacing = viewModel::setSpacing,
                onUnhide = viewModel::unhideApp,
                onCustomizeSidebar = { customizeSidebar = true },
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

            AppDrawerSection(
                onOpenIconPack = onOpenIconPack,
                onCreateFolder = viewModel::createFolder,
            )

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

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg, bottom = Spacing.md),
            )
        }
    }
}

@Composable
private fun AppDrawerSection(
    onOpenIconPack: () -> Unit,
    onCreateFolder: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel(stringResource(R.string.settings_section_app_drawer))
        SettingsRow(
            label = stringResource(R.string.settings_new_folder),
            supporting = stringResource(R.string.settings_new_folder_support),
            showChevron = true,
            onClick = onCreateFolder,
        )
        SettingsRow(
            label = stringResource(R.string.settings_icon_pack),
            supporting = stringResource(R.string.settings_icon_pack_support),
            showChevron = true,
            onClick = onOpenIconPack,
        )
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
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onColumns: (Int) -> Unit,
    onSpacing: (SpacingScale) -> Unit,
    onUnhide: (String) -> Unit,
    onCustomizeSidebar: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel(stringResource(R.string.settings_section_appearance))

        // Launcher role: only offered while SOFT / HOME is not the default Home app.
        // A launcher that is not the default cannot launch on HOME, so this is the
        // highest-value action when the row is shown.
        if (!isDefaultLauncher) {
            SettingsRow(
                label = stringResource(R.string.set_default_launcher),
                supporting = stringResource(R.string.default_launcher_body),
                showChevron = true,
                onClick = onSetDefaultLauncher,
            )
        }

        // Theme: cycle Light -> Dark -> System on tap (3-way control).
        SettingsRow(
            label = stringResource(R.string.settings_theme),
            supporting = themeMode.name,
            onClick = { onTheme(SettingsCycles.nextTheme(themeMode)) },
        )

        // Grid size: applies to the drawer grid (home is row-based).
        SettingsRow(
            label = stringResource(R.string.settings_drawer_grid_size),
            supporting = stringResource(R.string.settings_grid_columns, gridColumns),
            onClick = { onColumns(SettingsCycles.nextColumns(gridColumns)) },
        )

        // Spacing preset (Q3): Compact / Normal / Roomy multiplier.
        SettingsRow(
            label = stringResource(R.string.settings_spacing),
            supporting = spacing.name,
            onClick = { onSpacing(SettingsCycles.nextSpacing(spacing)) },
        )

        SettingsRow(
            label = stringResource(R.string.settings_customize_sidebar),
            supporting = stringResource(R.string.settings_customize_sidebar_support),
            showChevron = true,
            onClick = onCustomizeSidebar,
            onClickLabel = stringResource(
                R.string.settings_generic_open,
                stringResource(R.string.settings_customize_sidebar),
            ),
        )

        // System settings entry (Q1).
        SettingsRow(
            label = stringResource(R.string.settings_system_settings),
            supporting = stringResource(R.string.settings_system_settings_support),
            showChevron = true,
            onClick = onOpenSystemSettings,
        )

        // Hidden apps manager (Q2).
        SettingsRow(
            label = stringResource(R.string.settings_hidden_apps),
            supporting = if (hiddenApps.isEmpty()) {
                stringResource(R.string.settings_hidden_apps_none)
            } else {
                stringResource(R.string.settings_hidden_apps_count, hiddenApps.size)
            },
        )
        hiddenApps.forEach { app ->
            SettingsRow(
                label = app.label,
                supporting = stringResource(R.string.settings_hidden_app_tap_restore),
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
        SectionLabel(stringResource(R.string.settings_section_widgets))
        Text(
            text = stringResource(R.string.settings_widgets_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.softColors.textMuted,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        rows.forEach { pref ->
            val locked = !HomeRowLogic.canHide(pref.kind)
            SettingsRow(
                label = pref.kind.displayLabel(),
                supporting = if (locked) stringResource(R.string.settings_always_shown) else null,
                toggle = {
                    SoftToggle(
                        checked = pref.visible || locked,
                        onCheckedChange = { onToggle(pref.kind) },
                        enabled = !locked,
                        contentDescription = stringResource(
                            R.string.settings_row_visible_a11y,
                            pref.kind.displayLabel(),
                        ),
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
        SectionLabel(stringResource(R.string.settings_section_wallpaper))
        SettingsRow(
            label = stringResource(R.string.settings_choose_wallpaper),
            supporting = stringResource(R.string.settings_choose_wallpaper_support),
            showChevron = true,
            onClick = { WallpaperIntents.openPicker(context) },
        )
    }
}

@Composable
private fun GesturesSection() {
    val comingSoon = stringResource(R.string.settings_gesture_coming_soon)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionLabel(stringResource(R.string.settings_section_gestures))
        SettingsRow(
            label = stringResource(R.string.settings_gesture_swipe_up),
            supporting = stringResource(R.string.settings_gesture_swipe_up_support),
        )
        SettingsRow(
            label = stringResource(R.string.settings_gesture_swipe_down),
            supporting = comingSoon,
            enabled = false,
        )
        SettingsRow(
            label = stringResource(R.string.settings_gesture_double_tap),
            supporting = comingSoon,
            enabled = false,
        )
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
        SectionLabel(stringResource(R.string.settings_section_backup))
        Text(
            text = stringResource(R.string.settings_backup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.softColors.textMuted,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        SettingsRow(
            label = stringResource(R.string.settings_export_backup),
            supporting = stringResource(R.string.settings_export_backup_support),
            showChevron = true,
            onClick = { onExport(suggestedName) },
        )
        SettingsRow(
            label = stringResource(R.string.settings_import_backup),
            supporting = stringResource(R.string.settings_import_backup_support),
            showChevron = true,
            onClick = onImport,
        )

        message?.let { msg ->
            MessageRow(text = msg, onClick = onConsumeMessage)
        }

        if (pendingRestore) {
            Text(
                text = stringResource(R.string.settings_restore_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.softColors.textPrimary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActionPill(
                    label = stringResource(R.string.settings_restore),
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm,
                )
                ActionPill(
                    label = stringResource(R.string.settings_cancel),
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
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardAlt)
            .warmPress(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (primary) colors.onPrimaryAction else colors.textBody,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) colors.primaryAction else colors.card)
            .warmPress(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    )
}

@Composable
private fun HomeRowKind.displayLabel(): String = stringResource(
    when (this) {
        HomeRowKind.Time -> R.string.row_label_clock
        HomeRowKind.Date -> R.string.row_label_date
        HomeRowKind.Weather -> R.string.row_label_weather
        HomeRowKind.Search -> R.string.row_label_search
        HomeRowKind.Calendar -> R.string.row_label_calendar
        HomeRowKind.BatteryStorage -> R.string.row_label_battery
        HomeRowKind.Notes -> R.string.row_label_notes
    },
)

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
