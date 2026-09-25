package com.softhome.feature.appdrawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softhome.feature.appdrawer.R
import com.softhome.core.designsystem.atom.AppContextMenu
import com.softhome.core.designsystem.atom.ContextMenuItem
import com.softhome.core.designsystem.atom.DragController
import com.softhome.core.designsystem.atom.DragHoverRing
import com.softhome.core.designsystem.atom.DragPreviewLayer
import com.softhome.core.designsystem.atom.FolderPopupBody
import com.softhome.core.designsystem.atom.FolderTile
import com.softhome.core.designsystem.atom.LineIcon
import com.softhome.core.designsystem.atom.LineIconImage
import com.softhome.core.designsystem.atom.dragSource
import com.softhome.core.designsystem.atom.dragSourceAlpha
import com.softhome.core.designsystem.atom.dropTarget
import com.softhome.core.designsystem.atom.rememberDragController
import com.softhome.core.designsystem.atom.verticalSwipe
import com.softhome.core.designsystem.atom.warmPress
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.Spacing
import com.softhome.core.designsystem.theme.softColors
import com.softhome.core.model.DrawerCategory
import com.softhome.feature.iconpack.ui.DrawerAppIcon
import com.softhome.feature.iconpack.domain.IconEditor
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
    onClose: () -> Unit,
    viewModel: AppDrawerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.softColors
    val scope = rememberCoroutineScope()
    // P6: category switching is a full-width slide+fade (AnimatedContent), NOT a
    // HorizontalPager. The pager's drag physics rendered the neighbouring page during a
    // swipe, so a sliver of the next/previous category "peeked" at the edge mid-drag.
    // The user wants no peek: each category fills the width and the old one leaves fully
    // as the new one enters fully. We keep one LazyGridState per category so scroll
    // position is remembered, and the swipe-down-to-close reads the current category.
    val tabListState = rememberLazyListState()
    val gridStates = remember { mutableMapOf<DrawerCategory, LazyGridState>() }
    // Direction of the last category change, so the slide reads with the swipe:
    // tabbing right slides the new page in from the right (+1), left from the left (-1).
    var categorySlideDirection by remember { mutableIntStateOf(1) }
    // The "current" grid drives the swipe-down-to-close at-top check.
    val currentGridState = gridStates.getOrPut(state.category) { LazyGridState() }
    val categoryMicroSlidePx = with(LocalDensity.current) {
        MotionTokens.CATEGORY_MICRO_SLIDE_DP.dp.roundToPx()
    }
    // P4b: the editor's live state, keyed by the app being edited so reopening re-seeds.
    var editorState by remember { mutableStateOf<com.softhome.feature.iconpack.domain.IconEditor.State?>(null) }
    val editingEntry = state.editingEntry
    // Re-seed the editor whenever the edited app changes (open/close/switch).
    androidx.compose.runtime.LaunchedEffect(editingEntry?.app?.componentKey) {
        editorState = editingEntry?.let { viewModel.iconEditorState(it) }
    }

    // P4a: one drag controller for the drawer surface (app -> folder + drag-out).
    val dragController = rememberDragController()
    val dragState = dragController.state
    var surfaceOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    // The app currently being dragged (for the icon preview), or null.
    val draggedApp: DrawerEntry? = dragState.draggingId
        ?.removePrefix("app:")
        ?.let { key -> state.allApps.firstOrNull { it.app.componentKey == key } }

    // BACK pops the drawer's own layer stack, topmost first: an open long-press
    // menu, then an open folder popup, and finally the drawer overlay itself
    // (returning to home). Without this the drawer had no top-level handler and BACK
    // fell through to the launcher's intentional no-op onBackPressed, so the drawer
    // stayed open. One place owns the stack, so precedence is unambiguous.
    BackHandler {
        when {
            state.editingEntry != null -> viewModel.closeIconEditor()
            state.menuEntry != null -> viewModel.closeMenu()
            state.openFolder != null -> viewModel.closeFolder()
            else -> onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.drawerBg)
            .onGloballyPositioned { surfaceOriginInWindow = it.boundsInWindow().topLeft }
            // P1.1: symmetric swipe-down closes the drawer. Pass-through observer (never
            // consumes) so the grid's own scroll and every tap/long-press still work. It
            // only closes when the grid is already at the top -- a downward drag while
            // scrolled into the list stays a normal scroll, and only an over-scroll swipe
            // at the top returns to home (mirrors the swipe-up that opened it).
            .verticalSwipe(
                key = currentGridState.firstVisibleItemIndex to currentGridState.firstVisibleItemScrollOffset,
                direction = 1f,
            ) {
                val atTop = currentGridState.firstVisibleItemIndex == 0 &&
                    currentGridState.firstVisibleItemScrollOffset == 0
                // While an overlay (menu / folder / editor) is open, Back owns dismissal;
                // only a top-of-grid swipe closes the drawer surface itself.
                val overlayOpen = state.menuEntry != null || state.openFolder != null ||
                    state.editingEntry != null
                if (atTop && !overlayOpen) onClose()
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    // Preserve the grid-first static geometry from before P6. The category
                    // transition is allowed to move its content, but never changes the
                    // resting position of tabs, grid, or search chrome.
                    .padding(start = Spacing.xxl, top = Spacing.xxl, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // P6: the tabs are a scrollable row; they drive (and are driven by) the
                // selected category, so tap and horizontal swipe both switch category
                // through the same full-width slide transition (no pager peek).
                CategoryNav(
                    categories = state.availableCategories,
                    selected = state.category,
                    onSelect = { tab ->
                        if (tab != state.category) {
                            categorySlideDirection = if (
                                state.availableCategories.indexOf(tab) >=
                                state.availableCategories.indexOf(state.category)
                            ) 1 else -1
                            viewModel.onCategoryChange(tab)
                        }
                    },
                    tabListState = tabListState,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = state.category,
                        transitionSpec = {
                            // P6 final tuning B: preserve directional feedback with a 10dp
                            // nudge, 280ms duration, and FastOutSlowIn deceleration. The
                            // resting layout is unchanged.
                            val dir = categorySlideDirection
                            (
                                slideInHorizontally(
                                    animationSpec = MotionTokens.categoryLauncher(),
                                    initialOffsetX = { categoryMicroSlidePx * dir },
                                ) + fadeIn(animationSpec = MotionTokens.categoryLauncher())
                                )
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = MotionTokens.categoryLauncher(),
                                        targetOffsetX = { -categoryMicroSlidePx * dir },
                                    ) + fadeOut(animationSpec = MotionTokens.categoryLauncher()),
                                )
                        },
                        label = "drawerCategoryTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("drawer_pager")
                            // P6: horizontal swipe switches category without revealing the
                            // neighbouring page. A fling left/right past the threshold steps
                            // one category; vertical drags are untouched (they fall through
                            // to the grid's own scroll and the surface's swipe-down-to-close).
                            .pointerInput(state.availableCategories, state.category) {
                                var dragX = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { dragX = 0f },
                                    onDragEnd = {
                                        val threshold = 64.dp.toPx()
                                        if (dragX <= -threshold) {
                                            val idx = state.availableCategories.indexOf(state.category)
                                            state.availableCategories.getOrNull(idx + 1)?.let { next ->
                                                categorySlideDirection = 1
                                                viewModel.onCategoryChange(next)
                                            }
                                        } else if (dragX >= threshold) {
                                            val idx = state.availableCategories.indexOf(state.category)
                                            state.availableCategories.getOrNull(idx - 1)?.let { prev ->
                                                categorySlideDirection = -1
                                                viewModel.onCategoryChange(prev)
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { _, amount -> dragX += amount },
                                )
                            },
                    ) { category ->
                        val page = state.pages.firstOrNull { it.category == category }
                        val gridStateForPage = gridStates.getOrPut(category) { LazyGridState() }
                        // Non-All pages do not render the alphabet rail, but retain its
                        // slot width so the four grid columns stay geometrically identical
                        // to All (only the page data changes).
                        val gridEndInset = if (category == DrawerCategory.All) {
                            0.dp
                        } else {
                            Dimens.alphabetRailWidth + Spacing.md
                        }
                        if (page == null || (page.cells.isEmpty() && !state.loading)) {
                            Text(
                                text = if (state.query.isNotBlank()) stringResource(R.string.no_apps_found)
                                else stringResource(R.string.drawer_no_apps_category),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                                modifier = Modifier.padding(top = Spacing.xxl),
                            )
                        } else {
                            AppGridContent(
                                page = page,
                                gridState = gridStateForPage,
                                spacingFactor = state.spacingFactor,
                                activePack = state.activePack,
                                drawableLoader = viewModel.drawableLoader,
                                dragController = dragController,
                                onLaunch = {
                                    viewModel.launchApp(it.app)
                                    onAppLaunched()
                                },
                                onLongPress = viewModel::openMenu,
                                onOpenFolder = viewModel::openFolder,
                                onDropOnFolder = viewModel::assignToFolder,
                                onDropOnNewFolder = viewModel::createFolderWith,
                                endInset = gridEndInset,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                DrawerSearchPill(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Alphabet index rail (czxh4). P6: scoped to the selected category (which now
            // owns the visible grid; the previous pager page index is gone).
            if (state.category == DrawerCategory.All) {
                AlphabetRail(
                    letters = state.indexLetters,
                    onLetter = { letter ->
                        val idx = AlphabetIndex.firstIndexFor(
                            state.cells.filterIsInstance<DrawerCell.AppEntry>()
                                .map { it.entry.app.label }, letter,
                        )
                        scope.launch {
                            gridStates.getOrPut(state.category) { LazyGridState() }
                                .scrollToItem(idx)
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = Spacing.md),
                )
            }
        }

        // Folder popup (P2 / D2): cream r24 card over a dim scrim.
        AnimatedContent(
            targetState = state.openFolder,
            transitionSpec = {
                (fadeIn(MotionTokens.overlayEnter()) + scaleIn(MotionTokens.overlayEnter(), initialScale = 0.94f))
                    .togetherWith(fadeOut(MotionTokens.overlayExit()) + scaleOut(MotionTokens.overlayExit(), targetScale = 0.94f))
            },
            label = "folderPopupTransition",
        ) { folder ->
            folder?.let { folderValue ->
                FolderPopup(
                    folder = folderValue,
                    allApps = state.allApps,
                    activePack = state.activePack,
                    drawableLoader = viewModel.drawableLoader,
                    onClose = viewModel::closeFolder,
                    onRename = { name -> viewModel.renameFolder(folderValue.id, name) },
                     onAddApp = { key -> viewModel.addAppToFolder(folderValue.id, key) },
                     onRemoveApp = { key -> viewModel.removeAppFromFolder(folderValue.id, key) },
                 )
             }
         }

        // Long-press context menu (P3 / F3): row-style cream r24 card.
        AnimatedContent(
            targetState = state.menuEntry,
            transitionSpec = {
                (fadeIn(MotionTokens.overlayEnter()) + scaleIn(MotionTokens.overlayEnter(), initialScale = 0.94f))
                    .togetherWith(fadeOut(MotionTokens.overlayExit()) + scaleOut(MotionTokens.overlayExit(), targetScale = 0.94f))
            },
            label = "appMenuTransition",
        ) { entry ->
            entry?.let {
                AppMenu(
                    canUninstall = viewModel.canUninstall(it.app),
                    onOpen = {
                        viewModel.launchApp(it.app)
                        viewModel.closeMenu()
                        onAppLaunched()
                    },
                    onAppInfo = {
                        viewModel.openAppInfo(it.app)
                        viewModel.closeMenu()
                    },
                    onEditIcon = { viewModel.openIconEditor(it) },
                    onRemove = { viewModel.hideApp(it.app) },
                    onUninstall = {
                        viewModel.requestUninstall(it.app)
                        viewModel.closeMenu()
                    },
                    onDismiss = viewModel::closeMenu,
                )
            }
        }

        // P4b: the "Edit Icon" editor over the drawer (cream r24 card + dim scrim).
        AnimatedContent(
            targetState = editingEntry to editorState,
            transitionSpec = {
                (fadeIn(MotionTokens.overlayEnter()) + scaleIn(MotionTokens.overlayEnter(), initialScale = 0.94f))
                    .togetherWith(fadeOut(MotionTokens.overlayExit()) + scaleOut(MotionTokens.overlayExit(), targetScale = 0.94f))
            },
            label = "iconEditorTransition",
        ) { (entry, editor) ->
            if (entry != null && editor != null) {
                com.softhome.feature.iconpack.ui.IconEditorSheet(
                    appLabel = entry.app.label,
                    state = editor,
                    activePack = state.activePack,
                    drawableLoader = viewModel.drawableLoader,
                    category = entry.app.category,
                    onSelectMode = { editorState = IconEditor.selectMode(editor, it) },
                    onSelectDrawable = { editorState = IconEditor.selectDrawable(editor, it) },
                    onSelectGlyph = { editorState = IconEditor.selectGlyph(editor, it) },
                    onSelectColor = { editorState = IconEditor.selectColor(editor, it) },
                    onReset = { viewModel.resetIconOverride(entry.app.componentKey) },
                    onSave = {
                        editor.currentOverride?.let { viewModel.applyIconOverride(entry.app.componentKey, it) }
                            ?: viewModel.resetIconOverride(entry.app.componentKey)
                    },
                    onDismiss = viewModel::closeIconEditor,
                )
            }
        }

        // P4a: floating drag preview (the real app icon tile under the finger).
        DragPreviewLayer(controller = dragController, windowOrigin = surfaceOriginInWindow) {            draggedApp?.let { entry ->
                DrawerAppIcon(
                    resolved = entry.resolved,
                    size = Dimens.drawerTileNew,
                    activePack = state.activePack,
                    drawableLoader = viewModel.drawableLoader,
                    category = entry.app.category,
                    contentDescription = null,
                )
            }
        }
    }
}

/**
 * Long-press context menu body (P3 / F3). Row-style items over the shared
 * [AppContextMenu] atom: Open / App Info / Edit Icon / Remove / Uninstall (greyed for
 * system apps, P3-4) / Shortcuts. "Edit Icon" opens the icon flow (deferred to P4 for
 * the rich editor; the row is present) -- see the P3 spec section 4.8.
 */
@Composable
private fun AppMenu(
    canUninstall: Boolean,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onEditIcon: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val items = buildList {
        add(ContextMenuItem(DrawerMenuLabels.OPEN, LineIcon.ArrowUpRight, onClick = onOpen))
        add(ContextMenuItem(DrawerMenuLabels.APP_INFO, LineIcon.Info, onClick = onAppInfo))
        add(ContextMenuItem(DrawerMenuLabels.EDIT_ICON, LineIcon.PenLine, onClick = onEditIcon))
        add(ContextMenuItem(DrawerMenuLabels.REMOVE, LineIcon.X, onClick = onRemove))
        add(
            ContextMenuItem(
                DrawerMenuLabels.UNINSTALL,
                LineIcon.Trash2,
                enabled = canUninstall,
                destructive = true,
                onClick = onUninstall,
            ),
        )
        add(ContextMenuItem(DrawerMenuLabels.SHORTCUTS, LineIcon.AppWindow, onClick = onOpen))
    }
    AppContextMenu(items = items, onDismiss = onDismiss)
}

@Composable
private fun DrawerHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenIconPack: () -> Unit,
    onNewFolder: () -> Unit,
    dragController: DragController,
) {
    val colors = MaterialTheme.softColors
    val newFolderLabel = stringResource(R.string.drawer_new_folder)
    val iconPackLabel = stringResource(R.string.drawer_icon_pack)
    val newFolderHovered = dragController.state.hoveredTargetId == "newfolder"
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
                text = newFolderLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (newFolderHovered) colors.accent else colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .dropTarget(targetId = "newfolder", controller = dragController)
                    .background(if (newFolderHovered) colors.accent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(role = Role.Button, onClickLabel = newFolderLabel, onClick = onNewFolder)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
            Text(
                text = iconPackLabel,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textBody,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = iconPackLabel, onClick = onOpenIconPack)
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
    val searchHint = stringResource(R.string.drawer_search_hint)
    val searchA11y = stringResource(R.string.search_apps)
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
                .semantics { contentDescription = searchA11y },
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = searchHint,
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
 * P5/P6: the category tab row (design/homeApp.pen `B6gGM`). A horizontally scrollable row of
 * tabs; the selected tab is bold + accent-underlined, others muted. Tapping a tab selects
 * it (the caller runs the same full-width slide transition as a swipe); the row auto-scrolls
 * the active tab into view when the category changes by swipe.
 */
@Composable
private fun CategoryNav(
    categories: List<DrawerCategory>,
    selected: DrawerCategory,
    onSelect: (DrawerCategory) -> Unit,
    tabListState: androidx.compose.foundation.lazy.LazyListState,
) {
    val colors = MaterialTheme.softColors
    val categoriesA11y = stringResource(R.string.drawer_categories_a11y)
    val selectedIndex = categories.indexOf(selected).coerceAtLeast(0)
    // Follow the selection: scroll the active tab into view.
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        tabListState.animateScrollToItem(selectedIndex)
    }
    androidx.compose.foundation.lazy.LazyRow(
        state = tabListState,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = categoriesA11y },
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(categories.size) { i ->
            val category = categories[i]
            val active = category == selected
            val interactionSource = remember(category) { MutableInteractionSource() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = category.label,
                    style = if (active) MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium,
                    color = if (active) colors.textPrimary else colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .warmPress(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(category) },
                        )
                        .semantics {
                            contentDescription = category.label
                            this.selected = active
                        }
                        .padding(vertical = Spacing.xs)
                        .testTag("drawer_tab_${category.name}"),
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
    page: DrawerPage,
    gridState: LazyGridState,
    spacingFactor: Float,
    activePack: com.softhome.core.model.IconPack?,
    drawableLoader: com.softhome.feature.iconpack.data.IconPackDrawableLoader,
    dragController: DragController,
    onLaunch: (DrawerEntry) -> Unit,
    onLongPress: (DrawerEntry) -> Unit,
    onOpenFolder: (String) -> Unit,
    onDropOnFolder: (folderId: String, componentKey: String) -> Unit,
    onDropOnNewFolder: (componentKey: String) -> Unit,
    endInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.softColors
    // P1.2: the vertical gap honours the P3 spacing preset (previously persisted but
    // never applied). The design drawer gap token is [Spacing.drawerGap]; the factor
    // scales it (Compact 0.88 / Normal 1.00 / Roomy 1.12). Kept below the old fixed
    // value so the grid reads tighter and warmer, matching the .pen.
    val verticalGap = Spacing.drawerGap * spacingFactor
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        // P6: no horizontal content padding; the animated category page must flow to
        // both edges of its drawer surface. Tile spacing remains internal to the grid.
        modifier = modifier
            .fillMaxWidth()
            .testTag("drawer_grid"),
        contentPadding = PaddingValues(end = endInset, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        itemsIndexed(
            page.cells,
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
                    dragController = dragController,
                    onLaunch = onLaunch,
                    onLongPress = onLongPress,
                    onDropFolder = onDropOnFolder,
                    onDropNewFolder = { onDropOnNewFolder(it.app.componentKey) },
                )

                is DrawerCell.FolderCell -> {
                    val hovered = dragController.state.hoveredTargetId == "folder:${cell.folder.id}"
                    DragHoverRing(
                        hovered = hovered,
                        shape = RoundedCornerShape(Dimens.folderTileRadius),
                        modifier = Modifier
                            .fillMaxWidth()
                            .dropTarget(targetId = "folder:${cell.folder.id}", controller = dragController),
                    ) {
                        FolderTile(
                            name = cell.folder.name,
                            onClick = { onOpenFolder(cell.folder.id) },
                        ) {
                            FolderMiniGrid(
                                entries = cell.preview,
                                activePack = activePack,
                                drawableLoader = drawableLoader,
                            )
                        }
                    }
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
    dragController: DragController,
    onLaunch: (DrawerEntry) -> Unit,
    onLongPress: (DrawerEntry) -> Unit,
    onDropFolder: (folderId: String, componentKey: String) -> Unit,
    onDropNewFolder: (entry: DrawerEntry) -> Unit,
) {
    val colors = MaterialTheme.softColors
    val beingDragged = dragController.state.draggingId == "app:${item.app.componentKey}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.app.label }
            .dragSourceAlpha(beingDragged)
            .dragSource(
                id = "app:${item.app.componentKey}",
                controller = dragController,
                onTap = { onLaunch(item) },
                onLongPress = { onLongPress(item) },
                onDrop = { target, _ ->
                    // Only folder tiles / the new-folder chip accept an app; anything
                    // else (or a null target) = snap back (no-op).
                    when {
                        target == "newfolder" -> onDropNewFolder(item)
                        target != null && target.startsWith("folder:") ->
                            onDropFolder(target.removePrefix("folder:"), item.app.componentKey)
                        else -> Unit
                    }
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val tileSide = minOf(maxWidth, Dimens.drawerTileNew)
            // P3.5: color system (cream tile + pack artwork or category-colored glyph).
            // P1.2: pass the precomputed color token so no heuristic runs during scroll.
            DrawerAppIcon(
                resolved = item.resolved,
                size = tileSide,
                activePack = activePack,
                drawableLoader = drawableLoader,
                category = item.app.category,
                colorToken = item.colorToken,
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
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { entry ->
                    DrawerAppIcon(
                        resolved = entry.resolved,
                        size = Dimens.folderMiniIcon,
                        activePack = activePack,
                        drawableLoader = drawableLoader,
                        category = entry.app.category,
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
    val railA11y = stringResource(R.string.drawer_alphabet_a11y)
    Column(
        modifier = modifier
            .width(Dimens.alphabetRailWidth)
            .testTag("drawer_alphabet_rail")
            .semantics { contentDescription = railA11y },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.indexLetter,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.drawer_jump_to_letter, letter.toString()),
                        onClick = { onLetter(letter) },
                    )
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
    onClose: () -> Unit,
    onRename: (String) -> Unit,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
) {
    val colors = MaterialTheme.softColors
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
                                    DrawerAppIcon(
                                        resolved = item.resolved,
                                        size = Dimens.drawerTileNew,
                                        activePack = activePack,
                                        drawableLoader = drawableLoader,
                                        category = item.app.category,
                                        selected = true,
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
                                    DrawerAppIcon(
                                        resolved = item.resolved,
                                        size = 32.dp,
                                        activePack = activePack,
                                        drawableLoader = drawableLoader,
                                        category = item.app.category,
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

/**
 * Drawer long-press menu labels (P3 / F3). Single source so the menu rows and the
 * tests cannot drift.
 */
internal object DrawerMenuLabels {
    const val OPEN = "Open"
    const val APP_INFO = "App Info"
    const val EDIT_ICON = "Edit Icon"
    const val REMOVE = "Remove"
    const val UNINSTALL = "Uninstall"
    const val SHORTCUTS = "Shortcuts"
    val ALL = listOf(OPEN, APP_INFO, EDIT_ICON, REMOVE, UNINSTALL, SHORTCUTS)
}
