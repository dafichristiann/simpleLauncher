package com.softhome.core.designsystem.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors

/**
 * Drawer folder tile (P2 / D1) -- decision P2-1: folders live in the app drawer
 * (the grid surface that survived the Warm Right Rail redesign).
 *
 * Matches a drawer app tile exactly: 68 x 68, radius 21, fill `tileWarm`, with a
 * 2x2 mini icon-grid inside. [miniIcons] is supplied by the caller (typically four
 * [com.softhome.feature.iconpack.ui.AppIcon]s), keeping this atom free of any
 * iconpack dependency (core:designsystem cannot import feature:iconpack).
 */
@Composable
fun FolderTile(
    name: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    miniIcons: @Composable () -> Unit,
) {
    val colors = MaterialTheme.softColors
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .warmPress(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = name,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = "Folder $name" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.folderTile)
                .clip(RoundedCornerShape(Dimens.folderTileRadius))
                .background(colors.tileWarm)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            miniIcons()
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = colors.statusText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
        )
    }
}

/**
 * Open-folder popup body (P2 / D2): a cream r24 card, editable title + a grid of
 * the folder's apps. The [appGrid] slot is supplied by the drawer so it can render
 * app icons + labels with the icon-pack pipeline.
 */
@Composable
fun FolderPopupBody(
    title: String,
    modifier: Modifier = Modifier,
    onTitleChange: ((String) -> Unit)? = null,
    appGrid: @Composable () -> Unit,
) {
    val colors = MaterialTheme.softColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.folderPopupRadius))
            .background(colors.card)
            .padding(Spacing.xxl)
            .semantics { contentDescription = "Folder $title" },
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        if (onTitleChange != null) {
            BasicFolderTitle(title = title, onTitleChange = onTitleChange)
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        appGrid()
    }
}

@Composable
private fun BasicFolderTitle(title: String, onTitleChange: (String) -> Unit) {
    val colors = MaterialTheme.softColors
    androidx.compose.foundation.text.BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Folder title" },
        decorationBox = { inner -> inner() },
    )
}
