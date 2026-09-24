package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeRowLogicTest {

    @Test
    fun `default is all rows visible in default order`() {
        val prefs = HomeRowLogic.default()
        assertThat(prefs.map { it.kind }).isEqualTo(HomeRowLogic.DEFAULT_ORDER)
        assertThat(prefs.all { it.visible }).isTrue()
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
        val after = HomeRowLogic.toggle(HomeRowLogic.default(), HomeRowKind.Notes)
        assertThat(after.first { it.kind == HomeRowKind.Notes }.visible).isFalse()
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
    fun `hiding everything leaves the locked set rendered`() {
        var prefs = HomeRowLogic.default()
        listOf(
            HomeRowKind.Search, HomeRowKind.Music,
            HomeRowKind.Calendar, HomeRowKind.BatteryStorage, HomeRowKind.Notes,
        ).forEach { prefs = HomeRowLogic.toggle(prefs, it) }
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
        // Appended missing rows default to visible.
        assertThat(cleaned.first { it.kind == HomeRowKind.Music }.visible).isTrue()
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
