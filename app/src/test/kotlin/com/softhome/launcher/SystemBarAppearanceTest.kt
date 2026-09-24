package com.softhome.launcher

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P3 (F1/F2): system-bar appearance helper. Best-effort assertions under Robolectric
 * -- the real visual proof is the on-device screenshot in Phase 7.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemBarAppearanceTest {

    private fun activity(): Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    @Test
    fun `isGestureNavigation defaults to false when the secure setting is unset`() {
        // Robolectric's Settings.Secure has no navigation_mode by default -> not gesture.
        assertThat(SystemBarAppearance.isGestureNavigation(activity())).isFalse()
    }

    @Test
    fun `apply does not throw for light and dark themes`() {
        val a = activity()
        SystemBarAppearance.makeBarsTransparent(a)
        SystemBarAppearance.apply(a, darkTheme = false, hideNavBar = false)
        SystemBarAppearance.apply(a, darkTheme = true, hideNavBar = true)
        // No assertion beyond "did not throw": visual state is device-verified.
        assertThat(activity()).isNotNull()
    }
}
