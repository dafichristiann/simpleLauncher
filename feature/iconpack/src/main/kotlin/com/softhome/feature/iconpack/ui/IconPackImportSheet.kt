package com.softhome.feature.iconpack.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors

/**
 * The icon-pack import sheet (P1.5). Deliberately a *quiet utility* surface: it reuses
 * the drawer's card language (cream card, r24, charcoal text) and does not compete with
 * the icon tiles. Two ways in, per the brief: pick a `.zip`, or choose a pack app that
 * is already installed.
 */
@Composable
fun IconPackImportSheet(
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IconPackImportViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.softColors

    val pickZip = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importZip(uri)
            onApplied()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = "Icon pack",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Text(
            text = state.activePack?.let { "Using ${it.name} - ${it.iconCount} icons (${it.displaySource})" }
                ?: "No pack applied. Every app is masked to the soft monochrome tile.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textBody,
        )

        // Progress bar for an in-flight import (thin charcoal bar on cream track).
        state.progress?.let { progress ->
            ImportProgressBar(fraction = progress.fraction, label = progressLabel(progress))
        }

        state.message?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardAlt)
                    .clickable { viewModel.consumeMessage() }
                    .padding(Spacing.md),
            )
        }

        PrimaryAction(
            label = "Import a .zip file",
            enabled = !state.importing,
            onClick = { pickZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        )

        if (state.activePack != null) {
            SecondaryAction(label = "Remove icon pack", onClick = { viewModel.clearPack(); onApplied() })
        }

        Text(
            text = "Pack apps on this device",
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = Spacing.sm),
        )

        if (state.scanning) {
            Text(
                text = "Looking for installed packs...",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        } else if (state.installed.isEmpty()) {
            Text(
                text = "No icon pack apps found. Import a .zip above, or install a pack from the store and reopen this sheet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.installed, key = { it.id }) { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.card)
                            .clickable(enabled = !state.importing) { viewModel.applyInstalled(pack) }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = pack.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (pack.appFilterLocation != null) "appfilter found" else "no appfilter",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

@Composable
private fun ImportProgressBar(fraction: Float?, label: String) {
    val colors = MaterialTheme.softColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textBody)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.cardAlt),
        ) {
            if (fraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.textPrimary),
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.softColors
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onPrimaryAction,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) colors.primaryAction else colors.textMuted)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
    )
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.softColors
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textBody,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
    )
}

private fun progressLabel(progress: com.softhome.feature.iconpack.data.ImportProgress): String = when (progress) {
    is com.softhome.feature.iconpack.data.ImportProgress.Validating -> "Checking ${progress.fileName}..."
    is com.softhome.feature.iconpack.data.ImportProgress.Indexing -> "Reading icons ${progress.current} of ${progress.total}"
    is com.softhome.feature.iconpack.data.ImportProgress.Done -> "Imported ${progress.pack.name}"
    is com.softhome.feature.iconpack.data.ImportProgress.Failed -> "Import failed: ${progress.reason}"
}
