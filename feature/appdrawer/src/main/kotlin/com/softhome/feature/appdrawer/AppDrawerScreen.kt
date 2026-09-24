package com.softhome.feature.appdrawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softhome.core.designsystem.atom.FolderPopupBody
import com.softhome.core.designsystem.atom.FolderTile
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.DrawerCategory
import com.softhome.feature.iconpack.ui.AppIcon
import com.softhome.feature.iconpack.ui.IconPackImportSheet
import kotlinx.coroutines.launch

/**
 * The app drawer -- "Warm App Drawer" (design/homeApp.pen frame `TpzL1`).
 *
 * 430x860, r36, bg #DCCDBA. Contains a category nav ("All / Communication /
 * Entertainment / Tools" -- B6gGM), a 4-column grid of 68 r21 tiles with
 * labels (dBQmH + J3Lb4), an alphabet index (czxh4) and a 56 r28 search pill
 * (V7udUl). Icons render through the icon-pack pipeline (unchanged).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    onAppLaunched: () -> Unit,
    viewModel: AppDrawerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.softColors
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var iconPackSheetOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.drawerBg),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(start = Spacing.xxl, top = Spacing.xxl, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DrawerHeader(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onOpenIconPack = { iconPackSheetOpen = true },
                    onNewFolder = viewModel::createFolder,
                )

                CategoryNav(
                    categories = state.availableCategories,
                    selected = state.category,
                    onSelect = viewModel::onCategoryChange,
                )

                if (state.cells.isEmpty() && !state.loading) {
                    Text(
                        text = if (state.query.isNotBlank()) "No apps match that search."
                        else "No apps in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = Spacing.xxl),
                    )
                } else {
                    AppGridContent(
                        state = state,
                        gridState = gridState,
                        activePack = state.activePack,
                        drawableLoader = viewModel.drawableLoader,
                        bitmapProvider = viewModel.bitmapProvider,
                        onLaunch = {
                            viewModel.launchApp(it.app)
                            onAppLaunched()
                        },
                        onOpenFolder = viewModel::openFolder,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Alphabet index rail (czxh4).
            AlphabetRail(
                letters = state.indexLetters,
                onLetter = { letter ->
                    val idx = AlphabetIndex.firstIndexFor(
                        state.cells.filterIsInstance<DrawerCell.AppEntry>()
                            .map { it.entry.app.label }, letter,
                    )
                    scope.launch { gridState.scrollToItem(idx) }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = Spacing.md),
            )
        }

        if (iconPackSheetOpen) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { iconPackSheetOpen = false },
                containerColor = colors.drawerBg,
            ) {
                IconPackImportSheet(onApplied = { iconPackSheetOpen = false })
            }
        }

        // Folder popup (P2 / D2): cream r24 card over a dim scrim.
        state.openFolder?.let { folder ->
            FolderPopup(
                folder = folder,
                allApps = state.allApps,
                activePack = state.activePack,
                drawableLoader = viewModel.drawableLoader,
                bitmapProvider = viewModel.bitmapProvider,
                onClose = viewModel::closeFolder,
                onRename = { viewModel.renameFolder(folder.id, it) },
                onAddApp = { viewModel.addAppToFolder(folder.id, it) },
                onRemoveApp = { viewModel.removeAppFromFolder(folder.id, it) },
            )
        }
    }
}

@Composable
private fun DrawerHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenIconPack: () -> Unit,
    onNewFolder: () -> Unit,
) {
    val colors = MaterialTheme.softColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "All apps",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "New folder",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNewFolder)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
            Text(
                text = "Icon pack",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textBody,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenIconPack)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }
        DrawerSearchPill(query = query, onQueryChange = onQueryChange)
    }
}

/**
 * Search pill (V7udUl): h56, r28, fill #E8DFD0, stroke #C8B8A6, search icon 24
 * + vertical ellipsis.
 */
@Composable
private fun DrawerSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.drawerSearchNewHeight)
            .clip(RoundedCornerShape(Dimens.drawerSearchNewRadius))
            .background(colors.surface)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        LineIconImage(
            icon = LineIcon.Search,
            size = Dimens.searchRowIcon,
            tint = colors.textBody,
            contentDescription = null,
        )
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Search apps" },
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search for apps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textBody,
                    )
                }
                inner()
            },
        )
        LineIconImage(
            icon = LineIcon.MoreVertical,
            size = 22.dp,
            tint = colors.textBody,
            contentDescription = null,
        )
    }
}

/**
 * Category nav (B6gGM): All / Communication / Entertainment / Tools.
 * Active tab bold charcoal + accent underline (n0TeI); others muted.
 */
@Composable
private fun CategoryNav(
    categories: List<DrawerCategory>,
    selected: DrawerCategory,
    onSelect: (DrawerCategory) -> Unit,
) {
    val colors = MaterialTheme.softColors
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Categories" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            val active = category == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = category.label,
                    style = if (active) MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium,
                    color = if (active) colors.textPrimary else colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(category) }
                        .padding(vertical = Spacing.xs),
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun AppGridContent(
    state: DrawerUiState,
    gridState: LazyGridState,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: com.softhome.feature.iconpack.data.IconPackDrawableLoader,
    bitmapProvider: com.softhome.feature.iconpack.domain.IconBitmapProvider,
    onLaunch: (DrawerEntry) -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        itemsIndexed(
            state.cells,
            key = { _, cell ->
                when (cell) {
                    is DrawerCell.AppEntry -> "app:${cell.entry.app.componentKey}"
                    is DrawerCell.FolderCell -> "folder:${cell.folder.id}"
                }
            },
        ) { _, cell ->
            when (cell) {
                is DrawerCell.AppEntry -> AppCell(
                    item = cell.entry,
                    activePack = activePack,
                    drawableLoader = drawableLoader,
                    bitmapProvider = bitmapProvider,
                    onLaunch = onLaunch,
                )

                is DrawerCell.FolderCell -> FolderTile(
                    name = cell.folder.name,
                    onClick = { onOpenFolder(cell.folder.id) },
                ) {
                    FolderMiniGrid(
                        entries = cell.preview,
                        activePack = activePack,
                        drawableLoader = drawableLoader,
                        bitmapProvider = bitmapProvider,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCell(
    item: DrawerEntry,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: com.softhome.feature.iconpack.data.IconPackDrawableLoader,
    bitmapProvider: com.softhome.feature.iconpack.domain.IconBitmapProvider,
    onLaunch: (DrawerEntry) -> Unit,
) {
    val colors = MaterialTheme.softColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.app.label }
            .clickable { onLaunch(item) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val tileSide = minOf(maxWidth, Dimens.drawerTileNew)
            AppIcon(
                resolved = item.resolved,
                size = tileSide,
                activePack = activePack,
                drawableLoader = drawableLoader,
                bitmapProvider = bitmapProvider,
                contentDescription = null,
            )
        }
        Text(
            text = item.app.label,
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

/** 2x2 mini icon grid inside a folder tile (up to 4 member icons). */
@Composable
private fun FolderMiniGrid(
    entries: List<DrawerEntry>,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: com.softhome.feature.iconpack.data.IconPackDrawableLoader,
    bitmapProvider: com.softhome.feature.iconpack.domain.IconBitmapProvider,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { entry ->
                    AppIcon(
                        resolved = entry.resolved,
                        size = Dimens.folderMiniIcon,
                        activePack = activePack,
                        drawableLoader = drawableLoader,
                        bitmapProvider = bitmapProvider,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

/**
 * Alphabet index rail (czxh4): vertical, gap, centered. Active letter is the
 * accent (#B06F52), idle letters muted (#81796D).
 */
@Composable
private fun AlphabetRail(
    letters: List<Char>,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    Column(
        modifier = modifier.width(20.dp).semantics { contentDescription = "Alphabet index" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.indexLetter,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onLetter(letter) }
                    .padding(horizontal = 4.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * Folder popup (P2 / D2): dim scrim + cream r24 card. Editable title, the folder's
 * app grid, and an "Add app" list of apps not yet inside the folder. Back / scrim
 * tap / Close closes it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FolderPopup(
    folder: com.softhome.core.model.Folder,
    allApps: List<DrawerEntry>,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: com.softhome.feature.iconpack.data.IconPackDrawableLoader,
    bitmapProvider: com.softhome.feature.iconpack.domain.IconBitmapProvider,
    onClose: () -> Unit,
    onRename: (String) -> Unit,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
) {
    val colors = MaterialTheme.softColors
    BackHandler { onClose() }
    val members = folder.apps.mapNotNull { key -> allApps.firstOrNull { it.app.componentKey == key } }
    val addable = allApps.filterNot { it.app.componentKey in folder.apps }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0x88000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.xxl)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            FolderPopupBody(
                title = folder.name,
                onTitleChange = onRename,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    if (members.isEmpty()) {
                        Text(
                            text = "Empty folder \u2014 add an app below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    } else {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            items(members, key = { it.app.componentKey }) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onRemoveApp(item.app.componentKey) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AppIcon(
                                        resolved = item.resolved,
                                        size = Dimens.drawerTileNew,
                                        activePack = activePack,
                                        drawableLoader = drawableLoader,
                                        bitmapProvider = bitmapProvider,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = item.app.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.statusText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    if (addable.isNotEmpty()) {
                        Text(
                            text = "Add app",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            addable.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAddApp(item.app.componentKey) }
                                        .padding(vertical = Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AppIcon(
                                        resolved = item.resolved,
                                        size = 32.dp,
                                        activePack = activePack,
                                        drawableLoader = drawableLoader,
                                        bitmapProvider = bitmapProvider,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(Spacing.md))
                                    Text(
                                        text = item.app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textBody,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onClose)
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}
