package com.softhome.feature.iconpack.domain

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppInfo
import org.junit.Test

class IconMaskerTest {

    @Test
    fun `radius is 30 percent of tile size`() {
        // Design spec docs/08 ?3: 62->19, 104->30, 512->154
        assertThat(IconMasker.radiusPx(62f)).isWithin(0.01f).of(18.6f)
        assertThat(IconMasker.radiusPx(104f)).isWithin(0.01f).of(31.2f)
        assertThat(IconMasker.radiusPx(512f)).isWithin(0.01f).of(153.6f)
    }

    @Test
    fun `category heuristic picks camera for camera packages`() {
        assertThat(IconMasker.symbolFor(app("com.google.android.GoogleCamera", "Camera")))
            .isEqualTo("Camera")
    }

    @Test
    fun `category heuristic picks mail for gmail`() {
        assertThat(IconMasker.symbolFor(app("com.google.android.gm", "Gmail")))
            .isEqualTo("Mail")
    }

    @Test
    fun `category heuristic picks message for sms`() {
        assertThat(IconMasker.symbolFor(app("com.android.mms", "Messages")))
            .isEqualTo("MessageCircle")
    }

    @Test
    fun `unknown app falls back to generic window glyph`() {
        assertThat(IconMasker.symbolFor(app("com.acme.zorp", "Zorp")))
            .isEqualTo("AppWindow")
    }

    private fun app(pkg: String, label: String) =
        AppInfo(packageName = pkg, className = "$pkg.Main", label = label)
}
