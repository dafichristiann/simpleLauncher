package com.softhome.launcher

import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.softhome.core.data.repository.AppActionsRepository
import com.softhome.core.data.repository.AppRepository
import com.softhome.core.data.repository.FolderRepository
import com.softhome.core.data.repository.PrefsRepository
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DiscoveredIconPack
import com.softhome.core.model.Folder
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import com.softhome.feature.appdrawer.AppDrawerScreen
import com.softhome.feature.appdrawer.AppDrawerViewModel
import com.softhome.feature.iconpack.data.IconPackDrawableLoader
import com.softhome.feature.iconpack.data.IconPackRepository
import com.softhome.feature.iconpack.data.ImportProgress
import com.softhome.feature.iconpack.domain.IconBitmapProvider
import com.softhome.feature.iconpack.domain.IconResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream

/**
 * P1.1 regression test: a **swipe-down on the drawer surface** closes it (returns to
 * home), symmetric with the swipe-up that opens it from home.
 *
 * The drawer root carries a pass-through [com.softhome.core.designsystem.atom.verticalSwipe]
 * observer that never consumes events, so a downward drag past the threshold at the top of
 * the grid calls `onClose`. This test drives that gesture on the real [AppDrawerScreen]
 * (mirroring the `HomeActivity` wiring: a `drawerOpen` flag + `AnimatedVisibility`).
 */
class DrawerSwipeDownTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var closeCount = 0

    private fun setDrawerHost() {
        closeCount = 0
        composeRule.setContent {
            var drawerOpen by remember { mutableStateOf(true) }
            SoftHomeTheme {
                AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
                    val context = LocalContext.current
                    val viewModel = remember {
                        AppDrawerViewModel(
                            appRepository = SwipeFakeAppRepository,
                            prefsRepository = SwipeFakePrefsRepository,
                            appActionsRepository = SwipeFakeAppActionsRepository,
                            folderRepository = SwipeFakeFolderRepository,
                            iconPackRepository = SwipeFakeIconPackRepository,
                            drawableLoader = IconPackDrawableLoader(context),
                            bitmapProvider = IconBitmapProvider(context),
                            iconResolver = IconResolver(),
                        )
                    }
                    AppDrawerScreen(
                        onAppLaunched = { drawerOpen = false },
                        onClose = {
                            closeCount++
                            drawerOpen = false
                        },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    @Test
    fun swipe_down_closes_the_drawer() {
        setDrawerHost()
        composeRule.waitForIdle()
        // Precondition: the drawer is showing. The legacy "All apps" header was removed;
        // the pager tag is the stable drawer surface marker.
        composeRule.onNodeWithTag("drawer_pager").assertIsDisplayed()

        // A clear downward swipe on the drawer surface (well past the 60dp threshold).
        composeRule.onNodeWithTag("drawer_pager").performTouchInput {
            swipe(start = Offset(centerX, top + 40f), end = Offset(centerX, top + 900f), durationMillis = 220)
        }
        composeRule.waitForIdle()

        assertTrue("swipe-down should have invoked onClose", closeCount >= 1)
        composeRule.onNodeWithTag("drawer_pager").assertDoesNotExist()
    }

    @Test
    fun short_drag_does_not_close_the_drawer() {
        setDrawerHost()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_pager").assertIsDisplayed()

        // A small drag (below threshold) must NOT close the drawer.
        composeRule.onNodeWithTag("drawer_pager").performTouchInput {
            swipe(start = Offset(centerX, top + 40f), end = Offset(centerX, top + 120f), durationMillis = 200)
        }
        composeRule.waitForIdle()

        assertFalse("a sub-threshold drag must not close the drawer", closeCount >= 1)
        composeRule.onNodeWithTag("drawer_pager").assertIsDisplayed()
    }
}

// --- Lightweight fakes (the drawer renders an empty grid; no icon decode needed) ---

private object SwipeFakeAppRepository : AppRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = emptyList()
    override fun launchApp(app: AppInfo) = Unit
}

private object SwipeFakePrefsRepository : PrefsRepository {
    override val prefs: Flow<LauncherPrefs> = flowOf(LauncherPrefs())
    override suspend fun setGrid(grid: GridConfig) = Unit
    override suspend fun setActiveIconPack(packId: String?) = Unit
    override suspend fun setMaskUnsupported(mask: Boolean) = Unit
    override suspend fun setDarkTheme(mode: ThemeMode) = Unit
    override suspend fun setShowBadges(show: Boolean) = Unit
    override suspend fun setHomeRows(rows: List<HomeRowPref>) = Unit
    override suspend fun setSpacing(scale: SpacingScale) = Unit
    override suspend fun setHiddenApps(keys: Set<String>) = Unit
    override suspend fun hideApp(componentKey: String) = Unit
    override suspend fun unhideApp(componentKey: String) = Unit
    override suspend fun setIconOverride(
        componentKey: String,
        override: com.softhome.core.model.IconOverride?,
    ) = Unit
    override suspend fun applyAll(prefs: LauncherPrefs) = Unit
}

private object SwipeFakeAppActionsRepository : AppActionsRepository {
    override suspend fun shortcutsFor(app: AppInfo): List<ShortcutInfo> = emptyList()
    override fun appInfoIntent(app: AppInfo): Intent? = null
    override fun uninstallIntent(app: AppInfo): Intent? = null
    override fun isRemovable(app: AppInfo): Boolean = false
    override fun requestUninstall(app: AppInfo): Boolean = false
    override fun openAppInfo(app: AppInfo): Boolean = false
}

private object SwipeFakeFolderRepository : FolderRepository {
    override val folders: Flow<List<Folder>> = flowOf(emptyList())
    override suspend fun save(folders: List<Folder>) = Unit
}

private object SwipeFakeIconPackRepository : IconPackRepository {
    override val activePack = MutableStateFlow<IconPack?>(null)
    override fun importZipStream(stream: InputStream, displayName: String): Flow<ImportProgress> =
        flowOf(ImportProgress.Failed("not supported in test"))
    override fun importZip(file: File): Flow<ImportProgress> =
        flowOf(ImportProgress.Failed("not supported in test"))
    override suspend fun scanInstalled(): List<DiscoveredIconPack> = emptyList()
    override suspend fun applyInstalled(discovered: DiscoveredIconPack): IconPackParseResult =
        IconPackParseResult.Invalid("not supported in test")
    override suspend fun loadFromZip(file: File): IconPackParseResult =
        IconPackParseResult.Invalid("not supported in test")
    override suspend fun setActive(pack: IconPack?) = Unit
    override suspend fun clearActive() = Unit
}

