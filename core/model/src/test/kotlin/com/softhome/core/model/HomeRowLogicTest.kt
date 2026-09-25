package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeRowLogicTest {

    @Test
    fun `default is the lean set visible, the rest hidden, in default order`() {
        val prefs = HomeRowLogic.default()
        // Order is unchanged; only visibility differs from the pre-P2 default.
        assertThat(prefs.map { it.kind }).isEqualTo(HomeRowLogic.DEFAULT_ORDER)
        // P2: only Time/Date/Weather/Search/Music are visible on a fresh install.
        assertThat(prefs.filter { it.visible }.map { it.kind }).containsExactly(
            HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather,
            HomeRowKind.Search, HomeRowKind.Music,
        ).inOrder()
        assertThat(prefs.filterNot { it.visible }.map { it.kind })
            .containsExactlyElementsIn(HomeRowLogic.DEFAULT_HIDDEN)
    }

    @Test
    fun `default hidden rows are still re-enablable (not locked)`() {
        HomeRowLogic.DEFAULT_HIDDEN.forEach { kind ->
            assertThat(HomeRowLogic.canHide(kind)).isTrue()
        }
        // Toggling a default-hidden row ON makes it visible.
        val after = HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Calendar)
        assertThat(after.first { it.kind == HomeRowKind.Calendar }.visible).isTrue()
    }

    @Test
    fun `locked rows cannot be hidden`() {
        assertThat(HomeRowLogic.canHide(HomeRowKind.Time)).isFalse()
        assertThat(HomeRowLogic.canHide(HomeRowKind.Date)).isFalse()
        assertThat(HomeRowLogic.canHide(HomeRowKind.Weather)).isFalse()
        assertThat(HomeRowLogic.canHide(HomeRowKind.Notes)).isTrue()

        val after = HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Time)
        assertThat(after.first { it.kind == HomeRowKind.Time }.visible).isTrue()
    }

    @Test
    fun `toggle flips a toggleable row only`() {
        // Notes starts hidden by default (P2); toggling flips it back on.
        val after = HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Notes)
        assertThat(after.first { it.kind == HomeRowKind.Notes }.visible).isTrue()
        assertThat(after.first { it.kind == HomeRowKind.Music }.visible).isTrue()
    }

    @Test
    fun `moveUp swaps with previous and clamps at top`() {
        val prefs = HomeRowLogic.default()
        val moved = HomeRowLogic.moveUp(prefs, HomeRowKind.Weather)
        assertThat(moved.map { it.kind }.take(3))
            .containsExactly(HomeRowKind.Time, HomeRowKind.Weather, HomeRowKind.Date).inOrder()

        // Time is already first: no-op.
        assertThat(HomeRowLogic.moveUp(prefs, HomeRowKind.Time).map { it.kind })
            .isEqualTo(prefs.map { it.kind })
    }

    @Test
    fun `moveDown swaps with next and clamps at bottom`() {
        val prefs = HomeRowLogic.default()
        val moved = HomeRowLogic.moveDown(prefs, HomeRowKind.Time)
        assertThat(moved.map { it.kind }.take(3))
            .containsExactly(HomeRowKind.Date, HomeRowKind.Time, HomeRowKind.Weather).inOrder()

        // Notes is last: no-op.
        assertThat(HomeRowLogic.moveDown(prefs, HomeRowKind.Notes).map { it.kind })
            .isEqualTo(prefs.map { it.kind })
    }

    @Test
    fun `visibleInOrder skips hidden rows but keeps locked ones`() {
        var prefs = HomeRowLogic.default()
        prefs = HomeRowLogic.toggle(prefs, HomeRowKind.Search)
        prefs = HomeRowLogic.toggle(prefs, HomeRowKind.Music)
        val visible = HomeRowLogic.visibleInOrder(prefs)
        assertThat(visible).doesNotContain(HomeRowKind.Search)
        assertThat(visible).doesNotContain(HomeRowKind.Music)
        assertThat(visible).containsAtLeast(
            HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather,
        ).inOrder()
    }

    @Test
    fun `hiding every non-locked row leaves the locked set rendered`() {
        // Force every hideable row hidden, whether it started visible or hidden.
        var prefs = HomeRowLogic.default()
        val hideable = HomeRowLogic.DEFAULT_ORDER.filter { HomeRowLogic.canHide(it) }
        hideable.forEach { kind ->
            if (prefs.first { it.kind == kind }.visible) {
                prefs = HomeRowLogic.toggle(prefs, kind)
            }
        }
        assertThat(HomeRowLogic.visibleInOrder(prefs))
            .containsExactly(HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather).inOrder()
    }

    @Test
    fun `sanitize falls back to default on null or empty`() {
        assertThat(HomeRowLogic.sanitize(null).map { it.kind }).isEqualTo(HomeRowLogic.DEFAULT_ORDER)
        assertThat(HomeRowLogic.sanitize(emptyList()).map { it.kind }).isEqualTo(HomeRowLogic.DEFAULT_ORDER)
    }

    @Test
    fun `sanitize forces locked visible and appends missing kinds`() {
        // Stored list hides Weather (locked) and omits most rows.
        val stored = listOf(
            HomeRowPref(HomeRowKind.Weather, visible = false),
            HomeRowPref(HomeRowKind.Notes, visible = true),
        )
        val cleaned = HomeRowLogic.sanitize(stored)
        assertThat(cleaned.first { it.kind == HomeRowKind.Weather }.visible).isTrue()
        assertThat(cleaned.map { it.kind }).containsExactlyElementsIn(HomeRowLogic.DEFAULT_ORDER)
        // P2: a re-appended default-hidden row stays hidden; a re-appended normal row is visible.
        assertThat(cleaned.first { it.kind == HomeRowKind.Calendar }.visible).isFalse()
        assertThat(cleaned.first { it.kind == HomeRowKind.Music }.visible).isTrue()
    }

    @Test
    fun `migrateLegacy maps the old all-visible default to the new lean default`() {
        // Exactly what a pre-P2 install has persisted: all 8 kinds, visible, default order.
        val legacy = HomeRowLogic.DEFAULT_ORDER.map { HomeRowPref(it, visible = true) }
        val migrated = HomeRowLogic.migrateLegacy(legacy)
        assertThat(migrated).isEqualTo(HomeRowLogic.default())
        assertThat(migrated!!.filter { it.visible }.map { it.kind }).containsExactly(
            HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather,
            HomeRowKind.Search, HomeRowKind.Music,
        ).inOrder()
    }

    @Test
    fun `migrateLegacy leaves a real user choice untouched`() {
        // Any hidden row -> not the legacy shape -> no migration (null).
        val userChoice = HomeRowLogic.DEFAULT_ORDER.map {
            HomeRowPref(it, visible = it != HomeRowKind.Music)
        }
        assertThat(HomeRowLogic.migrateLegacy(userChoice)).isNull()

        // Already the new default -> not legacy either (Calendar hidden) -> no re-migration.
        assertThat(HomeRowLogic.migrateLegacy(HomeRowLogic.default())).isNull()

        // Empty / null -> nothing to migrate.
        assertThat(HomeRowLogic.migrateLegacy(null)).isNull()
        assertThat(HomeRowLogic.migrateLegacy(emptyList())).isNull()
    }

    @Test
    fun `sanitize drops unknown or duplicate kinds`() {
        // A stored duplicate is de-duped; the set is still complete.
        val stored = listOf(
            HomeRowPref(HomeRowKind.Notes, visible = true),
            HomeRowPref(HomeRowKind.Notes, visible = false),
        )
        val cleaned = HomeRowLogic.sanitize(stored)
        assertThat(cleaned.count { it.kind == HomeRowKind.Notes }).isEqualTo(1)
    }

    @Test
    fun `spacing scale factors`() {
        assertThat(SpacingScale.Compact.factor).isWithin(0.0001f).of(0.88f)
        assertThat(SpacingScale.Normal.factor).isWithin(0.0001f).of(1.00f)
        assertThat(SpacingScale.Roomy.factor).isWithin(0.0001f).of(1.12f)
    }
}
