package com.softhome.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.theme.SoftHomeTheme
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
        setContent {
            LauncherRoot()
        }
    }
}

@Composable
private fun LauncherRoot(viewModel: HomeViewModel = hiltViewModel()) {
    var drawerOpen by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    // P3 (G): the app theme follows ThemeMode from settings (Light/Dark/System).
    val darkTheme = when (state.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
    val context = LocalContext.current

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
            // Swipe-up layer (BEHIND the home content): opening the drawer. The home row
            // list scrolls; this layer still receives drags in the area the content does
            // not consume (and the row list's own scroll region forwards leftover drags).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onVerticalDrag = { _, delta -> total += delta },
                            onDragEnd = {
                                if (total < -60f) drawerOpen = true
                            },
                        )
                    },
            )

            HomeScreen(
                onOpenDrawer = { drawerOpen = true },
                onVoiceSearch = { /* STUB - wire real voice search later */ },
                state = state,
                onNotesChange = viewModel::setNotes,
                onOpenSettings = { context.startActivity(SettingsIntents.settings(context)) },
                onReorderRow = viewModel::reorderHomeRow,
            )

            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                AppDrawerScreen(
                    onAppLaunched = { drawerOpen = false },
                    onClose = { drawerOpen = false },
                )
            }
        }
    }
}
