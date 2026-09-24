package com.softhome.launcher

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression guard for the drawer Back bug (pre-P3.5 patch).
 *
 * Root cause: [HomeActivity] overrode `onBackPressed()` without delegating to
 * `super`, which bypasses the Activity's `OnBackPressedDispatcher` and therefore
 * breaks every `BackHandler` in the Compose tree (the drawer's overlay handler never
 * ran, so BACK left the drawer open).
 *
 * The fix removes that override and instead keeps the launcher from exiting via a
 * last-resort `BackHandler` in `LauncherRoot`. This test locks the root cause in:
 * `HomeActivity` must NOT shadow the dispatcher-backed back handling. The behavior
 * itself is covered by the instrumented `DrawerBackHandlerTest` and the on-device
 * before/after screenshots (`docs/screenshots/backfix-*`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeActivityBackContractTest {

    @Test
    fun `HomeActivity does not override onBackPressed`() {
        val declared = HomeActivity::class.java.declaredMethods.map { it.name }
        assertThat(declared).doesNotContain("onBackPressed")
    }
}
