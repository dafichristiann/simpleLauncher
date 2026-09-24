package com.softhome.launcher

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.softhome.core.data.repository.PrefsRepositoryImpl
import com.softhome.core.model.AppInfo
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.IconOverride
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource
import com.softhome.feature.iconpack.domain.IconResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P4b end-to-end (on-device, real DataStore): a saved override survives a round-trip
 * through [PrefsRepositoryImpl] and drives [IconResolver] to the expected source.
 *
 * This is the full data path the editor writes to and the drawer reads from -- proven
 * with the real Android DataStore on the device, not a fake.
 */
class IconOverrideEndToEndTest {

    @Test
    fun glyph_override_persists_and_resolves_to_a_glyph_source() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = PrefsRepositoryImpl(context)
        // Key must match AppInfo.componentKey = "packageName/className".
        val app = AppInfo("com.softhome.p4b.probe", "com.softhome.p4b.probe.Main", "Probe")
        val key = app.componentKey

        // Save a glyph override through the real repository.
        repo.setIconOverride(key, IconOverride.Glyph("Camera", DrawerIconTokenName.Travel))
        val persisted = repo.prefs.first().iconOverrides
        assertEquals(
            IconOverride.Glyph("Camera", DrawerIconTokenName.Travel),
            persisted[key],
        )

        // The resolver honours it, even with no pack active.
        val source = IconResolver().resolve(app, null, persisted, maskUnsupported = true)
        assertTrue(source is IconSource.Glyph)
        assertEquals("Camera", (source as IconSource.Glyph).symbolName)

        // Reset clears it -> back to the normal pipeline (mask).
        repo.setIconOverride(key, null)
        val afterReset = repo.prefs.first().iconOverrides
        assertFalse(afterReset.containsKey(key))
        assertEquals(
            IconSource.AutoMask,
            IconResolver().resolve(app, null, afterReset, maskUnsupported = true),
        )
    }

    @Test
    fun pack_override_persists_and_resolves_to_that_drawable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = PrefsRepositoryImpl(context)
        val app = AppInfo("com.softhome.p4b.pack", "com.softhome.p4b.pack.Main", "PackApp")
        val key = app.componentKey
        repo.setIconOverride(key, IconOverride.Pack("pack_alt_icon"))
        val persisted = repo.prefs.first().iconOverrides

        val pack = IconPack(
            id = "pack", name = "Pack", sourcePath = "/p",
            entries = mapOf(key to "pack_alt_icon"), hasAppFilter = true,
        )
        val source = IconResolver().resolve(app, pack, persisted, maskUnsupported = true)
        assertTrue(source is IconSource.Override)
        assertEquals("pack_alt_icon", (source as IconSource.Override).drawableName)

        repo.setIconOverride(key, null)
    }
}
