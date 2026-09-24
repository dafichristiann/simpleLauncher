package com.softhome.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.BackupDocument
import com.softhome.core.model.DrawerIconTokenName
import com.softhome.core.model.Folder
import com.softhome.core.model.GridConfig
import com.softhome.core.model.HomeRowKind
import com.softhome.core.model.HomeRowLogic
import com.softhome.core.model.HomeRowPref
import com.softhome.core.model.IconOverride
import com.softhome.core.model.LauncherPrefs
import com.softhome.core.model.SpacingScale
import com.softhome.core.model.ThemeMode
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P4d: pure codec tests. Runs under Robolectric for the real `org.json` implementation
 * (the Android stub throws on plain JVM tests -- same reason as [IconOverridesCodecTest]).
 * Focus: round-trip fidelity and the **total** decode contract.
 */
@RunWith(RobolectricTestRunner::class)
class BackupCodecTest {

    private fun richPrefs() = LauncherPrefs(
        grid = GridConfig(columns = 5, rows = 6, iconScale = 1.2f, showLabels = true),
        activeIconPackId = "whicons",
        maskUnsupportedApps = false,
        darkTheme = ThemeMode.Dark,
        showNotificationBadges = false,
        iconOverrides = mapOf(
            "com.a/Main" to IconOverride.Pack("a_drawable"),
            "com.b/Main" to IconOverride.Glyph("Phone", DrawerIconTokenName.Communication),
        ),
        homeRows = HomeRowLogic.moveUp(HomeRowLogic.default(), HomeRowKind.Notes),
        spacing = SpacingScale.Roomy,
        hiddenApps = setOf("com.hidden/One", "com.hidden/Two"),
    )

    private fun richDoc() = BackupDocument(
        exportedAtEpochMs = 1_758_800_000_000L,
        prefs = richPrefs(),
        folders = listOf(
            Folder("f1", "Games", listOf("com.g/A", "com.g/B")),
            Folder("f2", "Tools", emptyList()),
        ),
        notes = "buy milk\nand call mom",
    )

    @Test
    fun `round-trips a fully populated document`() {
        val doc = richDoc()
        val result = BackupCodec.decode(BackupCodec.encode(doc))

        assertThat(result).isInstanceOf(BackupDecodeResult.Ok::class.java)
        val got = (result as BackupDecodeResult.Ok).document

        assertThat(got.schemaVersion).isEqualTo(BackupDocument.CURRENT_VERSION)
        assertThat(got.exportedAtEpochMs).isEqualTo(doc.exportedAtEpochMs)
        assertThat(got.notes).isEqualTo(doc.notes)
        assertThat(got.folders).isEqualTo(doc.folders)

        val p = got.prefs
        assertThat(p.grid).isEqualTo(doc.prefs.grid)
        assertThat(p.activeIconPackId).isEqualTo("whicons")
        assertThat(p.maskUnsupportedApps).isFalse()
        assertThat(p.darkTheme).isEqualTo(ThemeMode.Dark)
        assertThat(p.showNotificationBadges).isFalse()
        assertThat(p.spacing).isEqualTo(SpacingScale.Roomy)
        assertThat(p.hiddenApps).containsExactly("com.hidden/One", "com.hidden/Two")
        assertThat(p.iconOverrides).isEqualTo(doc.prefs.iconOverrides)
        assertThat(p.homeRows).isEqualTo(doc.prefs.homeRows)
    }

    @Test
    fun `round-trips an empty state`() {
        val doc = BackupDocument(
            exportedAtEpochMs = 0L,
            prefs = LauncherPrefs(),
            folders = emptyList(),
            notes = "",
        )
        val got = (BackupCodec.decode(BackupCodec.encode(doc)) as BackupDecodeResult.Ok).document
        assertThat(got.prefs.grid).isEqualTo(GridConfig.Default)
        assertThat(got.prefs.activeIconPackId).isNull()
        assertThat(got.prefs.iconOverrides).isEmpty()
        assertThat(got.prefs.homeRows).isEqualTo(HomeRowLogic.default())
        assertThat(got.folders).isEmpty()
        assertThat(got.notes).isEmpty()
    }

    @Test
    fun `null pack id round-trips as null`() {
        val doc = BackupDocument(exportedAtEpochMs = 1L, prefs = LauncherPrefs(activeIconPackId = null))
        val got = (BackupCodec.decode(BackupCodec.encode(doc)) as BackupDecodeResult.Ok).document
        assertThat(got.prefs.activeIconPackId).isNull()
    }

    // --- total decode (never throws) ---

    @Test
    fun `blank or unparseable input is malformed`() {
        assertThat(BackupCodec.decode(null)).isEqualTo(BackupDecodeResult.Malformed)
        assertThat(BackupCodec.decode("")).isEqualTo(BackupDecodeResult.Malformed)
        assertThat(BackupCodec.decode("not json at all {{{")).isEqualTo(BackupDecodeResult.Malformed)
        assertThat(BackupCodec.decode("42")).isEqualTo(BackupDecodeResult.Malformed)
    }

    @Test
    fun `a foreign json object is not a backup`() {
        val foreign = JSONObject().put("hello", "world").toString()
        assertThat(BackupCodec.decode(foreign)).isEqualTo(BackupDecodeResult.NotABackup)
    }

    @Test
    fun `a backup missing the prefs section is malformed`() {
        val json = JSONObject()
            .put("app", BackupDocument.APP_MARKER)
            .put("version", 1)
            .toString()
        assertThat(BackupCodec.decode(json)).isEqualTo(BackupDecodeResult.Malformed)
    }

    @Test
    fun `a newer schema version is refused`() {
        val doc = richDoc()
        val bumped = JSONObject(BackupCodec.encode(doc))
            .put("version", BackupDocument.CURRENT_VERSION + 5)
            .toString()
        assertThat(BackupCodec.decode(bumped))
            .isEqualTo(BackupDecodeResult.UnsupportedVersion(BackupDocument.CURRENT_VERSION + 5))
    }

    @Test
    fun `unknown extra fields are ignored`() {
        val doc = richDoc()
        val augmented = JSONObject(BackupCodec.encode(doc))
            .put("somethingNew", "ignored")
            .toString()
        val got = (BackupCodec.decode(augmented) as BackupDecodeResult.Ok).document
        assertThat(got.prefs.activeIconPackId).isEqualTo("whicons")
    }

    @Test
    fun `partial prefs fall back to defaults without failing`() {
        // A v1 file whose prefs object only carries the theme: every other field defaults.
        val json = JSONObject()
            .put("app", BackupDocument.APP_MARKER)
            .put("version", 1)
            .put("prefs", JSONObject().put("darkTheme", "Light"))
            .toString()
        val got = (BackupCodec.decode(json) as BackupDecodeResult.Ok).document
        assertThat(got.prefs.darkTheme).isEqualTo(ThemeMode.Light)
        assertThat(got.prefs.grid).isEqualTo(GridConfig.Default)
        assertThat(got.prefs.homeRows).isEqualTo(HomeRowLogic.default())
        assertThat(got.prefs.spacing).isEqualTo(SpacingScale.Normal)
        assertThat(got.folders).isEmpty()
    }

    @Test
    fun `an unknown enum name falls back to the default`() {
        val json = JSONObject()
            .put("app", BackupDocument.APP_MARKER)
            .put("version", 1)
            .put("prefs", JSONObject().put("darkTheme", "Ultraviolet").put("spacing", "Enormous"))
            .toString()
        val got = (BackupCodec.decode(json) as BackupDecodeResult.Ok).document
        assertThat(got.prefs.darkTheme).isEqualTo(ThemeMode.System)
        assertThat(got.prefs.spacing).isEqualTo(SpacingScale.Normal)
    }

    @Test
    fun `a home-rows string that hides a locked row is sanitized on decode`() {
        val json = JSONObject()
            .put("app", BackupDocument.APP_MARKER)
            .put("version", 1)
            .put(
                "prefs",
                JSONObject().put(
                    "homeRows",
                    HomeRowsCodec.encode(listOf(HomeRowPref(HomeRowKind.Time, visible = false))),
                ),
            )
            .toString()
        val got = (BackupCodec.decode(json) as BackupDecodeResult.Ok).document
        // Time is locked -> forced visible; all other rows appended.
        assertThat(got.prefs.homeRows.first { it.kind == HomeRowKind.Time }.visible).isTrue()
        assertThat(got.prefs.homeRows.map { it.kind }).containsExactlyElementsIn(HomeRowLogic.DEFAULT_ORDER)
    }
}
