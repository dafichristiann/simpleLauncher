package com.softhome.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * QW1: the dead-reference prune. Locks the "uninstall self-heals" contract:
 * keys not present in the installed set are dropped from every per-app store, and a
 * no-op prune returns the *same instance* so callers can skip the write.
 */
class LauncherPrefsCleanupTest {

    private val a = "com.a/A"
    private val b = "com.b/B"
    private val c = "com.c/C"

    @Test
    fun pruneHiddenApps_drops_missing_keeps_present() {
        val result = LauncherPrefsCleanup.pruneHiddenApps(setOf(a, b), present = setOf(a))
        assertThat(result).containsExactly(a)
    }

    @Test
    fun pruneHiddenApps_returns_same_instance_when_nothing_to_do() {
        val input = setOf(a, b)
        val result = LauncherPrefsCleanup.pruneHiddenApps(input, present = setOf(a, b, c))
        assertThat(result).isSameInstanceAs(input)
    }

    @Test
    fun pruneIconOverrides_drops_missing() {
        val overrides = mapOf(
            a to IconOverride.Glyph("Phone", DrawerIconTokenName.Communication),
            b to IconOverride.Pack("pack_mail"),
        )
        val result = LauncherPrefsCleanup.pruneIconOverrides(overrides, present = setOf(a))
        assertThat(result.keys).containsExactly(a)
    }

    @Test
    fun pruneIconOverrides_returns_same_instance_when_all_present() {
        val overrides = mapOf(a to IconOverride.Pack("pack_x"))
        val result = LauncherPrefsCleanup.pruneIconOverrides(overrides, present = setOf(a))
        assertThat(result).isSameInstanceAs(overrides)
    }

    @Test
    fun pruneRailItems_drops_missing_apps_but_keeps_system_items() {
        val items = listOf(
            RailItemId.System(RailShortcutId.Sparkles),
            RailItemId.App(a),
            RailItemId.App(b),
            RailItemId.System(RailShortcutId.Phone),
        )
        val result = LauncherPrefsCleanup.pruneRailItems(items, present = setOf(a))
        assertThat(result).containsExactly(
            RailItemId.System(RailShortcutId.Sparkles),
            RailItemId.App(a),
            RailItemId.System(RailShortcutId.Phone),
        ).inOrder()
    }

    @Test
    fun pruneRailItems_returns_same_instance_when_nothing_dropped() {
        val items = listOf(RailItemId.App(a), RailItemId.System(RailShortcutId.Phone))
        val result = LauncherPrefsCleanup.pruneRailItems(items, present = setOf(a))
        assertThat(result).isSameInstanceAs(items)
    }

    @Test
    fun pruneFolders_strips_dead_members_and_keeps_the_folder() {
        val folders = listOf(
            Folder(id = "f1", name = "Games", apps = listOf(a, b)),
            Folder(id = "f2", name = "Empty", apps = listOf(c)),
        )
        val result = LauncherPrefsCleanup.pruneFolders(folders, present = setOf(a))
        assertThat(result).hasSize(2)
        assertThat(result[0].apps).containsExactly(a)
        assertThat(result[1].apps).isEmpty()
    }

    @Test
    fun pruneFolders_returns_same_instance_when_nothing_dropped() {
        val folders = listOf(Folder(id = "f1", name = "Games", apps = listOf(a)))
        val result = LauncherPrefsCleanup.pruneFolders(folders, present = setOf(a, b))
        assertThat(result).isSameInstanceAs(folders)
    }

    @Test
    fun prunePrefs_reports_change_and_applies_all_three_fields() {
        val prefs = LauncherPrefs(
            hiddenApps = setOf(a, b),
            iconOverrides = mapOf(b to IconOverride.Pack("p")),
            railItems = listOf(RailItemId.App(b), RailItemId.System(RailShortcutId.Phone)),
        )
        val (pruned, changed) = LauncherPrefsCleanup.prunePrefs(prefs, present = setOf(a))
        assertThat(changed).isTrue()
        assertThat(pruned.hiddenApps).containsExactly(a)
        assertThat(pruned.iconOverrides).isEmpty()
        assertThat(pruned.railItems).containsExactly(RailItemId.System(RailShortcutId.Phone))
    }

    @Test
    fun prunePrefs_reports_no_change_when_everything_present() {
        val prefs = LauncherPrefs(
            hiddenApps = setOf(a),
            iconOverrides = mapOf(a to IconOverride.Pack("p")),
            railItems = listOf(RailItemId.App(a)),
        )
        val (pruned, changed) = LauncherPrefsCleanup.prunePrefs(prefs, present = setOf(a))
        assertThat(changed).isFalse()
        assertThat(pruned).isSameInstanceAs(prefs)
    }
}
