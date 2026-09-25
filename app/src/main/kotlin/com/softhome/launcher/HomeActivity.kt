package com.softhome.launcher

import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.atom.verticalSwipe
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.core.designsystem.theme.MotionTokens
import com.softhome.core.designsystem.theme.Dimens
import com.softhome.core.model.ThemeMode
import com.softhome.feature.appdrawer.AppDrawerScreen
import com.softhome.feature.home.HomeScreen
import com.softhome.feature.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * HOME intent target -- the real launcher entry point.
 *
 * Behavior:
 *  - renders the home screen (feature:home)
 *  - swipe up opens the app drawer (feature:appdrawer) as an overlay
 *  - "set as default launcher" is wired via [LauncherRole] (stub-friendly)
 *  - P3 (F1/F2): status-bar icons follow the theme; the nav bar hides under gesture
 *    navigation. The app theme itself follows [com.softhome.core.model.ThemeMode]
 *    from settings (wired in Phase 6).
 *  - Back is routed through the Activity's `OnBackPressedDispatcher` (see
 *    [LauncherRoot]): the drawer's overlays and the home states register their own
 *    handlers, and a last-resort root handler keeps a Back press from ever exiting
 *    the launcher. We deliberately do NOT override `onBackPressed()` -- overriding it
 *    without delegating to `super` bypasses the dispatcher and breaks every
 *    `BackHandler` in the tree (this was the drawer Back regression).
 */
@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // P8: Request 120Hz refresh rate for smooth scrolling
        // Use Surface.setFrameRate() API (Android 10+) for more reliable refresh rate request
        requestHighRefreshRate()
        
        setContent {
            LauncherRoot()
        }
    }
    
    private fun requestHighRefreshRate() {
        try {
            // API 31+ (Android 12+): Use window.attributes to set preferred display mode
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val display = display ?: return
                val modes = display.supportedModes
                // Find 120Hz mode
                val mode120Hz = modes.firstOrNull { it.refreshRate >= 120f } ?: return
                window?.attributes = window?.attributes?.apply {
                    preferredDisplayModeId = mode120Hz.modeId
                }
            }
        } catch (e: Exception) {
            // Silently fail if not supported
        }
    }
}

@Composable
private fun LauncherRoot(viewModel: HomeViewModel = hiltViewModel()) {
    var drawerOpen by remember { mutableStateOf(false) }
    val appsState by viewModel.appsState.collectAsStateWithLifecycle()
    val prefsState by viewModel.prefsState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val railState by viewModel.railState.collectAsStateWithLifecycle()
    val deviceStatusState by viewModel.deviceStatusState.collectAsStateWithLifecycle()
    val homeRowsState by viewModel.homeRowsState.collectAsStateWithLifecycle()
    
    val systemDark = isSystemInDarkTheme()
    // P3 (G): the app theme follows ThemeMode from settings (Light/Dark/System).
    val darkTheme = when (prefsState.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val rightRailSwipeStartPx = with(density) {
        (configuration.screenWidthDp.dp - Dimens.railWidth).toPx()
    }

    // F1/F2: adapt the status/nav bar icon color to the theme and hide the nav bar
    // under gesture navigation. Re-applied on every recomposition of this host.
    SideEffect {
        val activity = context as? ComponentActivity ?: return@SideEffect
        SystemBarAppearance.makeBarsTransparent(activity)
        SystemBarAppearance.apply(
            activity = activity,
            darkTheme = darkTheme,
            hideNavBar = SystemBarAppearance.isGestureNavigation(activity),
        )
    }

    // Re-read real battery/storage whenever home resumes (decision P2-3, #55).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDeviceStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SoftHomeTheme(darkTheme = darkTheme) {
        // Last-resort Back handler: a launcher must never exit on Back. This is
        // registered *first* (outermost), so the drawer's overlay handler -- composed
        // later, deeper in the tree -- takes precedence while the drawer is open, and
        // this one only fires when nothing else consumes Back (docs/04 #11).
        BackHandler(enabled = true) { /* consume: stay on Home */ }

        Box(modifier = Modifier.fillMaxSize()) {
            // P1.1 fix: the old swipe-up layer sat BEHIND the home content and therefore
            // almost never received a drag -- the row list's `dragSource` treats a quick
            // swipe as a tap and never lets the gesture through to a layer behind it. The
            // gesture now lives ON the home surface itself as a pass-through observer
            // ([verticalSwipe]) that never consumes, so row tap / long-press reorder /
            // notes scroll are unaffected. A clear upward swipe opens the drawer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalSwipe(
                        key = drawerOpen,
                        direction = -1f,
                        // A rail long-press+drag belongs to the rail, never to the
                        // home-to-drawer observer behind it.
                        ignoreStart = { it.x >= rightRailSwipeStartPx },
                    ) {
                        if (!drawerOpen) drawerOpen = true
                    },
            ) {
                HomeScreen(
                    onOpenDrawer = { drawerOpen = true },
                    onVoiceSearch = { /* STUB - wire real voice search later */ },
                    appsState = appsState,
                    prefsState = prefsState,
                    notesState = notesState,
                    railState = railState,
                    deviceStatusState = deviceStatusState,
                    homeRowsState = homeRowsState,
                    onNotesChange = viewModel::setNotes,
                    onOpenSettings = { context.startActivity(SettingsIntents.settings(context)) },
                    onReorderRow = viewModel::reorderHomeRow,
                    onReorderRail = viewModel::reorderRail,
                    onLaunchApp = viewModel::launchApp,
                    onSetThemeMode = viewModel::setThemeMode,
                    drawerDrawableLoader = viewModel.drawableLoader,
                )
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInVertically(MotionTokens.overlayEnter()) { it } +
                    fadeIn(MotionTokens.overlayEnter()),
                exit = slideOutVertically(MotionTokens.overlayExit()) { it } +
                    fadeOut(MotionTokens.overlayExit()),
            ) {
                AppDrawerScreen(
                    onAppLaunched = { drawerOpen = false },
                    onClose = { drawerOpen = false },
                )
            }
        }
    }
}
