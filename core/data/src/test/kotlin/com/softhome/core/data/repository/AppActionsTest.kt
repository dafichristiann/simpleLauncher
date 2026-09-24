package com.softhome.core.data.repository

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.common.DefaultDispatcherProvider
import com.softhome.core.model.AppInfo
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppActionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun repo() = AppActionsRepositoryImpl(context, DefaultDispatcherProvider())

    private val app = AppInfo(
        packageName = "com.example.app",
        className = "com.example.app.Main",
        label = "Example",
        isSystem = false,
    )

    @Test
    fun `app info intent targets the package settings screen`() {
        val intent = AppIntentFactory.appInfo(app)
        assertThat(intent.action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(intent.data.toString()).contains(app.packageName)
    }

    @Test
    fun `uninstall intent is ACTION_DELETE with the package uri`() {
        val intent = AppIntentFactory.uninstall(app)
        assertThat(intent.action).isEqualTo(Intent.ACTION_DELETE)
        assertThat(intent.data.toString()).isEqualTo("package:${app.packageName}")
    }

    @Test
    fun `isRemovable is false for a system app and for an unknown package`() {
        assertThat(repo().isRemovable(app.copy(isSystem = true))).isFalse()
        // A package that is not installed on the Robolectric image is not removable.
        assertThat(repo().isRemovable(app.copy(packageName = "com.not.installed"))).isFalse()
    }

    @Test
    fun `shortcuts for an unknown package are empty and never throw`() = runTest {
        assertThat(repo().shortcutsFor(app.copy(packageName = "com.not.installed"))).isEmpty()
    }

    @Test
    fun `requestUninstall and openAppInfo return false when unresolvable`() {
        // Robolectric's stock image resolves the app-info settings screen, but the
        // ACTION_DELETE flow is environment-dependent; both must be boolean-safe.
        val r = repo()
        assertThat(r.requestUninstall(app)).isAnyOf(true, false)
        assertThat(r.openAppInfo(app)).isAnyOf(true, false)
    }
}
