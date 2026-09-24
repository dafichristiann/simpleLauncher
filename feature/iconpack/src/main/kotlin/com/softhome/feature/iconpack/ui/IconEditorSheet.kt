package com.softhome.feature.iconpack.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.atom.ChoiceTile
import com.softhome.core.designsystem.atom.ColorSwatchRow
import com.softhome.core.designsystem.atom.DrawerIconTile
import com.softhome.core.designsystem.atom.EditorSectionLabel
import com.softhome.core.designsystem.atom.IconEditorCard
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconPack
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.domain.IconCompositor
import com.softhome.feature.iconpack.domain.IconEditor

/**
 * P4b: the "Edit Icon" editor body (open over a dim scrim by the caller, inside the
 * [IconEditorCard]). Pure `(state, callbacks)` -- it decides nothing itself; the state
 * machine is [IconEditor] (unit-tested) and persistence is the ViewModel's job.
 *
 * The **live preview** uses the same [DrawerAppIcon]/[DrawerIconTile] render path as the
 * real grid, so the preview cannot drift from how the icon will actually look.
 */
@Composable
fun IconEditorSheet(
    appLabel: String,
    state: IconEditor.State,
    activePack: IconPack?,
    drawableLoader: IconPackDrawableLoader,
    category: Int?,
    onSelectMode: (IconEditor.Mode) -> Unit,
    onSelectDrawable: (String) -> Unit,
    onSelectGlyph: (String) -> Unit,
    onSelectColor: (DrawerIconTokenName) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.softColors
    val glyphs = remember { LineIcon.entries.toList() }
    val tokens = remember { DrawerIconTokenName.entries.toList() }

    IconEditorCard(title = "Edit icon \u00B7 $appLabel", onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // --- Live preview ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorPreview(state = state, activePack = activePack, drawableLoader = drawableLoader)
            }

            // --- Mode tabs (only when the app is in the pack; otherwise glyph-only) ---
            if (state.canPickPack) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    ModeTab(
                        label = "Pack icon",
                        active = state.mode == IconEditor.Mode.Pack,
                        onClick = { onSelectMode(IconEditor.Mode.Pack) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeTab(
                        label = "Glyph",
                        active = state.mode == IconEditor.Mode.Glyph,
                        onClick = { onSelectMode(IconEditor.Mode.Glyph) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (state.mode) {
                IconEditor.Mode.Pack -> {
                    EditorSectionLabel("Choose from the active pack")
                    if (state.packDrawables.isEmpty()) {
                        Text(
                            text = "This app has no pack artwork to choose from.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .semantics { contentDescription = "Pack drawables" },
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            items(state.packDrawables, key = { it }) { name ->
                                val painter = rememberPackPainter(activePack, drawableLoader, name)
                                ChoiceTile(
                                    selected = state.selectedDrawable == name,
                                    onClick = { onSelectDrawable(name) },
                                    painter = painter,
                                    contentDescription = name,
                                )
                            }
                        }
                    }
                }

                IconEditor.Mode.Glyph -> {
                    EditorSectionLabel("Choose a glyph")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp)
                            .semantics { contentDescription = "Glyph choices" },
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(glyphs, key = { it.name }) { glyph ->
                            ChoiceTile(
                                selected = state.selectedGlyph == glyph.name,
                                onClick = { onSelectGlyph(glyph.name) },
                                symbol = glyph,
                                symbolTint = colors.textPrimary,
                                contentDescription = glyph.name,
                            )
                        }
                    }

                    EditorSectionLabel("Color")
                    ColorSwatchRow(
                        swatches = tokens.map { tokenColor(colors, it) },
                        selectedIndex = tokens.indexOf(state.selectedColor),
                        onSelect = { onSelectColor(tokens[it]) },
                    )
                }
            }

            // --- Actions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill(
                    label = "Reset",
                    enabled = true,
                    onClick = onReset,
                )
                Box(Modifier.weight(1f))
                ActionPill(
                    label = "Save",
                    enabled = state.changed,
                    onClick = onSave,
                )
            }
        }
    }
}

/**
 * The live preview tile: renders the editor's *current selection* the same way the
 * drawer will -- a chosen pack drawable (real colors) or a chosen glyph in the chosen
 * color, on a cream tile.
 */
@Composable
private fun EditorPreview(
    state: IconEditor.State,
    activePack: IconPack?,
    drawableLoader: IconPackDrawableLoader,
) {
    val colors = MaterialTheme.softColors
    val showPack = state.mode == IconEditor.Mode.Pack && state.selectedDrawable != null
    val painter = if (showPack) {
        rememberPackPainter(activePack, drawableLoader, state.selectedDrawable!!)
    } else {
        null
    }
    DrawerIconTile(
        size = Dimens.editorPreviewTile,
        background = colors.drawerTileCream,
        symbol = if (showPack) null else LineIcon.fromName(state.selectedGlyph),
        symbolTint = tokenColor(colors, state.selectedColor),
        painter = painter,
        painterTint = null,
        contentDescription = "Preview",
    )
}

@Composable
private fun ModeTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.choiceTileRadius))
            .then(
                if (active) Modifier.background(colors.accent.copy(alpha = 0.16f))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) colors.accent else colors.textBody,
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.softColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.choiceTileRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) colors.accent else colors.disabled,
        )
    }
}

/** Decode a pack drawable once per (pack, name, size) into a Compose [Painter]. */
@Composable
private fun rememberPackPainter(
    activePack: IconPack?,
    loader: IconPackDrawableLoader,
    name: String,
): Painter? {
    val sizePx = with(LocalDensity.current) { Dimens.choiceTile.roundToPx() }
    return remember(activePack?.id, name, sizePx) {
        if (activePack == null || sizePx < 8) return@remember null
        val drawable = loader.load(activePack, name) ?: return@remember null
        val bmp = IconCompositor.toBitmap(drawable, sizePx)
        if (bmp != null) BitmapPainter(bmp.asImageBitmap())
        else (drawable as? BitmapDrawable)?.bitmap?.let { BitmapPainter(it.asImageBitmap()) }
    }
}
