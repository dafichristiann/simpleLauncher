package com.softhome.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softhome.launcher.R
import com.softhome.core.designsystem.atom.DragPreviewLayer
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.atom.SettingsRow
import com.softhome.core.designsystem.atom.dragSource
import com.softhome.core.designsystem.atom.dropTarget
import com.softhome.core.designsystem.atom.rememberDragController
import com.softhome.core.designsystem.atom.warmPress
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.AppInfo
import com.softhome.core.model.RailConfigLogic
import com.softhome.core.model.RailItemId
import com.softhome.core.model.RailShortcutId
import com.softhome.feature.iconpack.domain.IconMasker

@Composable
fun CustomizeSidebarScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAddApp: (String) -> Unit,
    onAddShortcut: (RailShortcutId) -> Unit,
    onRemove: (RailItemId) -> Unit,
    onMove: (String, Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    val active = state.railItems
    val activeIds = active.map { it.storageId }.toSet()
    val availableApps = state.installedApps.filterNot { it.componentKey.let { key -> "app:$key" in activeIds } }
    val availableShortcuts = RailShortcutId.entries.filter {
        it != RailShortcutId.PanelLeft && "system:${it.name}" !in activeIds
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val backA11y = stringResource(R.string.customize_sidebar_back_a11y)
            Text(
                text = stringResource(R.string.customize_sidebar_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.customize_sidebar_back),
                color = colors.accent,
                modifier = Modifier
                    .semantics { contentDescription = backA11y }
                    .clickable(onClick = onBack)
                    .padding(Spacing.sm),
            )
        }
        Text(
            text = stringResource(R.string.customize_sidebar_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )

        SectionTitle(stringResource(R.string.customize_sidebar_current_rail))
        CurrentRailEditor(
            items = active,
            apps = state.installedApps.associateBy { it.componentKey },
            onRemove = onRemove,
            onMove = onMove,
        )
        Text(
            text = stringResource(
                R.string.customize_sidebar_slots_used,
                active.count { it != RailConfigLogic.LOCKED_SETTINGS },
                RailConfigLogic.MAX_CONFIGURABLE_ITEMS,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
        )

        SectionTitle(stringResource(R.string.customize_sidebar_available_apps))
        if (availableApps.isEmpty()) {
            Text(stringResource(R.string.customize_sidebar_no_apps), color = colors.textMuted)
        } else {
            availableApps.forEach { app ->
                AddableRow(
                    label = app.label,
                    icon = LineIcon.fromName(IconMasker.symbolFor(app)),
                    actionLabel = stringResource(R.string.customize_sidebar_add_app_a11y, app.label),
                    enabled = active.count { it != RailConfigLogic.LOCKED_SETTINGS } <
                        RailConfigLogic.MAX_CONFIGURABLE_ITEMS,
                    onAdd = { onAddApp(app.componentKey) },
                )
            }
        }

        SectionTitle(stringResource(R.string.customize_sidebar_available_shortcuts))
        availableShortcuts.forEach { shortcut ->
            val shortcutLabel = shortcutLabel(shortcut)
            AddableRow(
                label = shortcutLabel,
                icon = shortcutIcon(shortcut),
                actionLabel = stringResource(R.string.customize_sidebar_add_shortcut_a11y, shortcutLabel),
                enabled = active.count { it != RailConfigLogic.LOCKED_SETTINGS } <
                    RailConfigLogic.MAX_CONFIGURABLE_ITEMS,
                onAdd = { onAddShortcut(shortcut) },
            )
        }

        SettingsRow(
            label = stringResource(R.string.customize_sidebar_reset),
            supporting = stringResource(R.string.customize_sidebar_reset_support),
            onClick = onReset,
            onClickLabel = stringResource(R.string.customize_sidebar_reset_a11y),
        )
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun CurrentRailEditor(
    items: List<RailItemId>,
    apps: Map<String, AppInfo>,
    onRemove: (RailItemId) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val controller = rememberDragController()
    val dragState = controller.state
    val dragging = dragState.draggingId?.removePrefix("settings-rail:")
    val draggingIndex = items.indexOfFirst { it.storageId == dragging }
    Box {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items.forEachIndexed { index, item ->
            val title = railItemLabel(item, apps)
            val locked = item == RailConfigLogic.LOCKED_SETTINGS
            val sourceIndex = index
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .dropTarget("settings-rail:$index", controller)
                    .dragSource(
                        id = "settings-rail:${item.storageId}",
                        controller = controller,
                        interactionSource = interactionSource,
                        onTap = null,
                        onLongPress = {},
                        onDrop = { target, _ ->
                            val targetIndex = target?.removePrefix("settings-rail:")?.toIntOrNull()
                                ?: return@dragSource
                            val destination = if (sourceIndex < targetIndex) targetIndex - 1 else targetIndex
                            onMove(item.storageId, destination)
                        },
                    ),
            ) {
                SettingsRow(
                    label = title,
                    supporting = if (locked) {
                        stringResource(R.string.customize_sidebar_always_available)
                    } else {
                        stringResource(R.string.customize_sidebar_in_sidebar)
                    },
                    enabled = !locked,
                    onClick = if (locked) null else ({ onRemove(item) }),
                    onClickLabel = if (locked) {
                        null
                    } else {
                        stringResource(R.string.customize_sidebar_remove_a11y, title)
                    },
                    trailing = {
                        if (locked) {
                            Text(
                                stringResource(R.string.customize_sidebar_locked),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        } else {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                    },
                    onMoveUp = if (locked) null else ({ onMove(item.storageId, (index - 1).coerceAtLeast(0)) }),
                    onMoveDown = if (locked) null else ({ onMove(item.storageId, (index + 1).coerceAtMost(items.lastIndex)) }),
                    leadingContent = {
                        LineIconImage(
                            icon = railItemIcon(item, apps),
                            size = 20.dp,
                            tint = MaterialTheme.softColors.textPrimary,
                            contentDescription = null,
                        )
                    },
                )
            }
            }
        }
        DragPreviewLayer(controller = controller, windowOrigin = androidx.compose.ui.geometry.Offset.Zero) { id ->
            val item = items.firstOrNull { it.storageId == id.removePrefix("settings-rail:") }
            if (item != null) {
                Text(railItemLabel(item, apps), color = MaterialTheme.softColors.textPrimary)
            }
        }
    }
}

@Composable
private fun AddableRow(
    label: String,
    icon: LineIcon,
    actionLabel: String,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    SettingsRow(
        label = label,
        leadingContent = { LineIconImage(icon, 20.dp, contentDescription = null) },
        trailing = {
            Text(
                text = "+",
                color = if (enabled) MaterialTheme.softColors.accent else MaterialTheme.softColors.textMuted,
                modifier = Modifier
                    .semantics { contentDescription = actionLabel }
                    .warmPress(source, enabled = enabled)
                    .clickable(enabled = enabled, onClick = onAdd)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.softColors.textMuted,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
    )
}

@Composable
private fun railItemLabel(item: RailItemId, apps: Map<String, AppInfo>): String = when (item) {
    is RailItemId.App -> apps[item.componentKey]?.label
        ?: stringResource(R.string.customize_sidebar_unavailable_app)
    is RailItemId.System -> shortcutLabel(item.shortcut)
}

private fun railItemIcon(item: RailItemId, apps: Map<String, AppInfo>): LineIcon = when (item) {
    is RailItemId.App -> apps[item.componentKey]?.let { LineIcon.fromName(IconMasker.symbolFor(it)) }
        ?: LineIcon.AppWindow
    is RailItemId.System -> shortcutIcon(item.shortcut)
}

@Composable
private fun shortcutLabel(shortcut: RailShortcutId): String = stringResource(
    when (shortcut) {
        RailShortcutId.Sparkles -> R.string.rail_label_more
        RailShortcutId.CircleDot -> R.string.rail_label_web
        RailShortcutId.MessageCircle -> R.string.rail_label_messages
        RailShortcutId.Send -> R.string.rail_label_mail
        RailShortcutId.Camera -> R.string.rail_label_camera
        RailShortcutId.Wind -> R.string.rail_label_weather
        RailShortcutId.PanelLeft -> R.string.rail_label_settings
        RailShortcutId.Phone -> R.string.rail_label_phone
    },
)

private fun shortcutIcon(shortcut: RailShortcutId): LineIcon = when (shortcut) {
    RailShortcutId.Sparkles -> LineIcon.Sparkles
    RailShortcutId.CircleDot -> LineIcon.CircleDot
    RailShortcutId.MessageCircle -> LineIcon.MessageCircle
    RailShortcutId.Send -> LineIcon.Mail
    RailShortcutId.Camera -> LineIcon.Camera
    RailShortcutId.Wind -> LineIcon.Wind
    RailShortcutId.PanelLeft -> LineIcon.PanelLeft
    RailShortcutId.Phone -> LineIcon.Phone
}
