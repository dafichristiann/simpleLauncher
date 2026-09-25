package com.softhome.feature.appdrawer

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.AppCategory
import com.softhome.core.model.AppInfo
import com.softhome.feature.iconpack.domain.DrawerIconColor
import com.softhome.feature.iconpack.domain.IconMasker
import org.junit.Test

/**
 * P1.2: the drawer glyph **name + color token** are computed once per app in
 * `AppDrawerViewModel.buildState` and carried on [DrawerEntry] (see `colorToken`).
 * Before this, the composable re-ran these heuristics on every recomposition, which was
 * a chunk of the "stiff scroll" UI-thread work.
 *
 * These tests lock the *pure* values that buildState stores, so the ViewModel and the
 * UI can never drift: whatever `tokenFor(category, symbolFor(app))` returns is exactly
 * what the entry must carry.
 */
class DrawerEntryResolveTest {

    private fun app(pkg: String, label: String, category: Int? = null) =
        AppInfo(
            packageName = pkg,
            className = "$pkg.Main",
            label = label,
            category = category,
            isSystem = false,
        )

    /** The exact combined resolve `buildState` performs per app. */
    private fun resolveToken(a: AppInfo): DrawerIconColor.Token =
        DrawerIconColor.tokenFor(a.category, IconMasker.symbolFor(a))

    @Test
    fun `known category produces its category token`() {
        val a = app("com.example.social", "Chatter", AppCategory.SOCIAL)
        assertThat(resolveToken(a)).isEqualTo(DrawerIconColor.Token.Social)
    }

    @Test
    fun `unknown category uses the glyph heuristic`() {
        // No category, but the label reads as a mail app -> Communication via the glyph.
        val a = app("com.example.mailer", "Mail", category = null)
        assertThat(resolveToken(a)).isEqualTo(DrawerIconColor.Token.Communication)
    }

    @Test
    fun `unknown category with a generic glyph is neutral`() {
        val a = app("com.example.thing", "Thing", category = null)
        assertThat(resolveToken(a)).isEqualTo(DrawerIconColor.Token.Neutral)
    }

    @Test
    fun `resolve is deterministic for the same app`() {
        // A memoized value must equal a fresh computation (no per-call drift).
        val a = app("com.example.maps", "Maps", AppCategory.MAPS)
        assertThat(resolveToken(a)).isEqualTo(resolveToken(a))
        assertThat(resolveToken(a)).isEqualTo(DrawerIconColor.Token.Travel)
    }
}
