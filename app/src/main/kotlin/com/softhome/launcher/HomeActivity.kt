package com.softhome.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softhome.core.designsystem.theme.SoftHomeTheme
import com.softhome.feature.appdrawer.AppDrawerScreen
import com.softhome.feature.home.HomeScreen
import com.softhome.feature.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * HOME intent target -- the real launcher entry point.
 *
 * Behavior in P1:
 *  - renders the home screen (feature:home)
 *  - swipe up opens the app drawer (feature:appdrawer) as an overlay
 *  - "set as default launcher" is wired via [LauncherRole] (stub-friendly)
 */
@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftHomeTheme {
                LauncherRoot()
            }
        }
    }

    override fun onBackPressed() {
        // A launcher must not exit on Back; consume it (stay on Home).
        // (Left as a no-op intentionally -- see docs/04 #11.)
    }
}

@Composable
private fun LauncherRoot(viewModel: HomeViewModel = hiltViewModel()) {
    var drawerOpen by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-read real battery/storage whenever home resumes (decision P2-3, #55).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDeviceStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            onVoiceSearch = { /* TODO: STUB - wire real voice search later */ },
            state = state,
            onNotesChange = viewModel::setNotes,
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
        ) {
            AppDrawerScreen(onAppLaunched = { drawerOpen = false })
        }
    }
}
