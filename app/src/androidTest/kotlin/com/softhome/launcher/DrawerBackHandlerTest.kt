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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso
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
 * Regression test for the drawer BACK bug (pre-P3.5 patch).
 *
 * With the drawer overlay open, the system BACK button must close the drawer and
 * return to home -- not be swallowed by the launcher's intentional no-op
 * `onBackPressed`. Before the fix, `AppDrawerScreen` had no top-level `BackHandler`,
 * so BACK did nothing while the drawer was open.
 *
 * Mirrors the real `HomeActivity` wiring: a `drawerOpen` flag + `AnimatedVisibility
 * { AppDrawerScreen(onClose = ...) }`, driven through the real back dispatcher via
 * `Espresso.pressBack()`.
 */
class DrawerBackHandlerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setDrawerHost() {
        composeRule.setContent {
            var drawerOpen by remember { mutableStateOf(true) }
            SoftHomeTheme {
                AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
                    val context = LocalContext.current
                    val viewModel = remember {
                        AppDrawerViewModel(
                            appRepository = FakeAppRepository,
                            prefsRepository = FakePrefsRepository,
                            appActionsRepository = FakeAppActionsRepository,
                            folderRepository = FakeFolderRepository,
                            iconPackRepository = FakeIconPackRepository,
                            drawableLoader = IconPackDrawableLoader(context),
                            bitmapProvider = IconBitmapProvider(context),
                            iconResolver = IconResolver(),
                        )
                    }
                    AppDrawerScreen(
                        onAppLaunched = { drawerOpen = false },
                        onClose = { drawerOpen = false },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    @Test
    fun back_closes_the_drawer_overlay() {
        setDrawerHost()
        composeRule.waitForIdle()
        // Drawer visible: the current UI has no legacy "All apps" header; the pager is
        // the stable drawer surface marker.
        composeRule.onNodeWithTag("drawer_pager").assertIsDisplayed()

        // System BACK must close the drawer (was a no-op before this fix).
        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("drawer_pager").assertDoesNotExist()
    }

    @Test
    fun drawer_stays_closed_after_back() {
        setDrawerHost()
        composeRule.waitForIdle()
        assertTrue("precondition: drawer should be open", drawerHeaderExists())

        Espresso.pressBack()
        composeRule.waitForIdle()

        // No re-open, no residual overlay.
        assertFalse("drawer must stay closed after BACK", drawerHeaderExists())
    }

    private fun drawerHeaderExists(): Boolean = try {
        composeRule.onNodeWithTag("drawer_pager").assertExists()
        true
    } catch (_: AssertionError) {
        false
    }
}

// --- Lightweight fakes (the drawer renders an empty grid; no icon decode needed) ---

private object FakeAppRepository : AppRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = emptyList()
    override fun launchApp(app: AppInfo) = Unit
}

private object FakePrefsRepository : PrefsRepository {
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

private object FakeAppActionsRepository : AppActionsRepository {
    override suspend fun shortcutsFor(app: AppInfo): List<ShortcutInfo> = emptyList()
    override fun appInfoIntent(app: AppInfo): Intent? = null
    override fun uninstallIntent(app: AppInfo): Intent? = null
    override fun isRemovable(app: AppInfo): Boolean = false
    override fun requestUninstall(app: AppInfo): Boolean = false
    override fun openAppInfo(app: AppInfo): Boolean = false
}

private object FakeFolderRepository : FolderRepository {
    override val folders: Flow<List<Folder>> = flowOf(emptyList())
    override suspend fun save(folders: List<Folder>) = Unit
}

private object FakeIconPackRepository : IconPackRepository {
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
