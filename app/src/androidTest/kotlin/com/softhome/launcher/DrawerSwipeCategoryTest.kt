package com.softhome.launcher

import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream

/**
 * P5: the category pager. The drawer shows one page per category (All + the 8 design
 * groups); **tap** on a tab and **horizontal swipe** both switch the visible category.
 *
 * The fixture seeds two apps that the design table places in different categories
 * (WhatsApp -> Communication, Spotify -> Social & Entertainment) so a page change is
 * observable by the app labels that are present.
 */
class DrawerSwipeCategoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setDrawer(onAppLaunched: () -> Unit = {}) {
        composeRule.setContent {
            SoftHomeTheme {
                val context = LocalContext.current
                val viewModel = remember {
                    AppDrawerViewModel(
                        appRepository = CategoryFakeAppRepository,
                        prefsRepository = CategoryFakePrefsRepository,
                        appActionsRepository = CategoryFakeAppActionsRepository,
                        folderRepository = CategoryFakeFolderRepository,
                        iconPackRepository = CategoryFakeIconPackRepository,
                        drawableLoader = IconPackDrawableLoader(context),
                        bitmapProvider = IconBitmapProvider(context),
                        iconResolver = IconResolver(),
                        packageEventMonitor = com.softhome.core.data.packages.PackageEventMonitor(context),
                        appInventory = com.softhome.core.data.packages.AppInventoryCoordinator(
                            CategoryFakeAppRepository,
                            CategoryFakePrefsRepository,
                            CategoryFakeFolderRepository,
                            com.softhome.core.common.DefaultDispatcherProvider(),
                        ),
                        context = context,
                    )
                }
                AppDrawerScreen(
                    onAppLaunched = onAppLaunched,
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun short_tap_still_launches_the_selected_app() {
        var launched = 0
        setDrawer { launched++ }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").performTouchInput {
            down(center)
            up()
        }
        composeRule.waitForIdle()
        assertEquals("a stationary short tap should launch exactly once", 1, launched)
    }

    @Test
    fun stationary_long_press_opens_menu_without_launching() {
        var launched = 0
        setDrawer { launched++ }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Open").assertIsDisplayed()
        assertEquals("a stationary long-press must not launch", 0, launched)
    }

    @Test
    fun starting_a_vertical_scroll_does_not_launch_an_app() {
        var launched = 0
        composeRule.setContent {
            SoftHomeTheme {
                val context = LocalContext.current
                val viewModel = remember {
                    AppDrawerViewModel(
                        appRepository = CategoryFakeAppRepository,
                        prefsRepository = CategoryFakePrefsRepository,
                        appActionsRepository = CategoryFakeAppActionsRepository,
                        folderRepository = CategoryFakeFolderRepository,
                        iconPackRepository = CategoryFakeIconPackRepository,
                        drawableLoader = IconPackDrawableLoader(context),
                        bitmapProvider = IconBitmapProvider(context),
                        iconResolver = IconResolver(),
                        packageEventMonitor = com.softhome.core.data.packages.PackageEventMonitor(context),
                        appInventory = com.softhome.core.data.packages.AppInventoryCoordinator(
                            CategoryFakeAppRepository,
                            CategoryFakePrefsRepository,
                            CategoryFakeFolderRepository,
                            com.softhome.core.common.DefaultDispatcherProvider(),
                        ),
                        context = context,
                    )
                }
                AppDrawerScreen(
                    onAppLaunched = { launched++ },
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").performTouchInput {
            swipe(start = Offset(centerX, centerY), end = Offset(centerX, centerY - 260f), durationMillis = 220)
        }
        composeRule.waitForIdle()
        org.junit.Assert.assertEquals("scroll start must not launch the app", 0, launched)
    }

    @Test
    fun swiping_left_advances_to_the_next_category_page() {
        setDrawer()
        composeRule.waitForIdle()

        // "All": both apps are visible.
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("Spotify").assertIsDisplayed()

        // Swipe the pager left -> Communication page (WhatsApp only). The pager keeps the
        // neighbouring page composed off-screen, so assert *displayed* (on-screen), not
        // existence.
        composeRule.onNodeWithTag("drawer_pager").performTouchInput {
            swipe(start = Offset(right - 20f, centerY), end = Offset(left + 20f, centerY), durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("Spotify").assertIsNotDisplayed()

        // Swipe left again -> Social & Entertainment page (Spotify only).
        composeRule.onNodeWithTag("drawer_pager").performTouchInput {
            swipe(start = Offset(right - 20f, centerY), end = Offset(left + 20f, centerY), durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Spotify").assertIsDisplayed()
        composeRule.onNodeWithText("WhatsApp").assertIsNotDisplayed()
    }

    @Test
    fun tapping_a_tab_switches_the_page() {
        setDrawer()
        composeRule.waitForIdle()

        // Tap the "Communication" tab -> only WhatsApp.
        composeRule.onNodeWithTag("drawer_tab_Communication").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("Spotify").assertIsNotDisplayed()

        // Tapping a tab also scrolls the tab row (the active tab stays visible), so go
        // back via a right-swipe on the pager rather than tapping the now-virtualized "All".
        composeRule.onNodeWithTag("drawer_pager").performTouchInput {
            swipe(start = Offset(left + 20f, centerY), end = Offset(right - 20f, centerY), durationMillis = 300)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Spotify").assertIsDisplayed()
    }

    @Test
    fun alphabet_rail_is_only_visible_on_all_category() {
        setDrawer()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("drawer_alphabet_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_tab_Communication").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_alphabet_rail").assertDoesNotExist()
    }

    @Test
    fun vertical_swipe_still_scrolls_and_does_not_change_category() {
        setDrawer()
        composeRule.waitForIdle()

        // A downward drag on the grid must not page sideways (stays on All).
        composeRule.onNodeWithText("WhatsApp").performTouchInput {
            swipe(start = Offset(centerX, centerY), end = Offset(centerX, centerY - 300f), durationMillis = 200)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("Spotify").assertIsDisplayed()
    }
}

// --- Fakes: two apps in different design categories -------------------------------

private object CategoryFakeAppRepository : AppRepository {
    override suspend fun getInstalledApps(): List<AppInfo> = listOf(
        AppInfo("com.whatsapp", "com.whatsapp.Main", "WhatsApp"),
        AppInfo("com.spotify.music", "com.spotify.music.MainActivity", "Spotify"),
    )
    override fun launchApp(app: AppInfo) = Unit
}

private object CategoryFakePrefsRepository : PrefsRepository {
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
    override suspend fun applyPruned(prefs: LauncherPrefs) = Unit
}

private object CategoryFakeAppActionsRepository : AppActionsRepository {
    override suspend fun shortcutsFor(app: AppInfo): List<ShortcutInfo> = emptyList()
    override fun appInfoIntent(app: AppInfo): Intent? = null
    override fun uninstallIntent(app: AppInfo): Intent? = null
    override fun isRemovable(app: AppInfo): Boolean = false
    override fun requestUninstall(app: AppInfo): Boolean = false
    override fun openAppInfo(app: AppInfo): Boolean = false
}

private object CategoryFakeFolderRepository : FolderRepository {
    override val folders: Flow<List<Folder>> = flowOf(emptyList())
    override suspend fun save(folders: List<Folder>) = Unit
}

private object CategoryFakeIconPackRepository : IconPackRepository {
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
