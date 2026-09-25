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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
            Text(
                text = "Customize Sidebar",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "Back",
                color = colors.accent,
                modifier = Modifier
                    .semantics { contentDescription = "Back to Settings" }
                    .clickable(onClick = onBack)
                    .padding(Spacing.sm),
            )
        }
        Text(
            text = "Choose which apps and shortcuts appear in your sidebar.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )

        SectionTitle("CURRENT RAIL")
        CurrentRailEditor(
            items = active,
            apps = state.installedApps.associateBy { it.componentKey },
            onRemove = onRemove,
            onMove = onMove,
        )
        Text(
            text = "${active.count { it != RailConfigLogic.LOCKED_SETTINGS }} / " +
                "${RailConfigLogic.MAX_CONFIGURABLE_ITEMS} configurable slots used",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
        )

        SectionTitle("AVAILABLE APPS")
        if (availableApps.isEmpty()) {
            Text("All installed apps are already configured.", color = colors.textMuted)
        } else {
            availableApps.forEach { app ->
                AddableRow(
                    label = app.label,
                    icon = LineIcon.fromName(IconMasker.symbolFor(app)),
                    actionLabel = "Add ${app.label} to sidebar",
                    enabled = active.count { it != RailConfigLogic.LOCKED_SETTINGS } <
                        RailConfigLogic.MAX_CONFIGURABLE_ITEMS,
                    onAdd = { onAddApp(app.componentKey) },
                )
            }
        }

        SectionTitle("AVAILABLE SYSTEM SHORTCUTS")
        availableShortcuts.forEach { shortcut ->
            AddableRow(
                label = shortcutLabel(shortcut),
                icon = shortcutIcon(shortcut),
                actionLabel = "Add ${shortcutLabel(shortcut)} to sidebar",
                enabled = active.count { it != RailConfigLogic.LOCKED_SETTINGS } <
                    RailConfigLogic.MAX_CONFIGURABLE_ITEMS,
                onAdd = { onAddShortcut(shortcut) },
            )
        }

        SettingsRow(
            label = "Reset to default",
            supporting = "Restore the original system shortcut order.",
            onClick = onReset,
            onClickLabel = "Reset sidebar to default",
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
                    supporting = if (locked) "Always available" else "In sidebar",
                    enabled = !locked,
                    onClick = if (locked) null else ({ onRemove(item) }),
                    onClickLabel = if (locked) null else "Remove $title from sidebar",
                    trailing = {
                        if (locked) {
                            Text("LOCKED", style = MaterialTheme.typography.labelSmall)
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

private fun railItemLabel(item: RailItemId, apps: Map<String, AppInfo>): String = when (item) {
    is RailItemId.App -> apps[item.componentKey]?.label ?: "Unavailable app"
    is RailItemId.System -> shortcutLabel(item.shortcut)
}

private fun railItemIcon(item: RailItemId, apps: Map<String, AppInfo>): LineIcon = when (item) {
    is RailItemId.App -> apps[item.componentKey]?.let { LineIcon.fromName(IconMasker.symbolFor(it)) }
        ?: LineIcon.AppWindow
    is RailItemId.System -> shortcutIcon(item.shortcut)
}

private fun shortcutLabel(shortcut: RailShortcutId): String = when (shortcut) {
    RailShortcutId.Sparkles -> "More"
    RailShortcutId.CircleDot -> "Web"
    RailShortcutId.MessageCircle -> "Messages"
    RailShortcutId.Send -> "Mail"
    RailShortcutId.Camera -> "Camera"
    RailShortcutId.Wind -> "Weather"
    RailShortcutId.PanelLeft -> "Settings"
    RailShortcutId.Phone -> "Phone"
}

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
