package com.softhome.feature.home

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RailShortcutTest {

    @Test
    fun rail_has_eight_icons_in_design_order() {
        assertThat(RailShortcut.ordered).hasSize(8)
        assertThat(RailShortcut.ordered.map { it.drawableName }).containsExactly(
            "sparkles", "circle_dot", "message_circle", "send",
            "camera", "wind", "panel_left", "phone",
        ).inOrder()
    }

    @Test
    fun sparkles_is_decorative_with_no_target() {
        assertThat(RailShortcut.Sparkles.isDecorative).isTrue()
        val resolver = RailShortcutResolver { RailShortcutResolver.defaultIntent(it) }
        assertThat(resolver.intentFor(RailShortcut.Sparkles)).isNull()
    }

    @Test
    fun each_role_has_a_default_intent() {
        RailShortcut.ordered.filterNot { it.isDecorative }.forEach { shortcut ->
            assertThat(RailShortcutResolver.defaultIntent(shortcut)).isNotNull()
        }
    }

    @Test
    fun phone_uses_dial_action() {
        assertThat(RailShortcutResolver.defaultIntent(RailShortcut.Phone)?.action)
            .isEqualTo(Intent.ACTION_DIAL)
    }

    @Test
    fun panel_left_opens_settings() {
        assertThat(RailShortcutResolver.defaultIntent(RailShortcut.PanelLeft)?.action)
            .isEqualTo(android.provider.Settings.ACTION_SETTINGS)
    }

    @Test
    fun resolver_caches_and_returns_same_intent() {
        var calls = 0
        val resolver = RailShortcutResolver { calls++; Intent(Intent.ACTION_DIAL) }
        val first = resolver.intentFor(RailShortcut.Phone)
        val second = resolver.intentFor(RailShortcut.Phone)
        assertThat(first).isSameInstanceAs(second)
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun resolver_returns_null_when_nothing_installed() {
        val resolver = RailShortcutResolver { null }
        assertThat(resolver.intentFor(RailShortcut.Camera)).isNull()
    }
}
