# P4d — Backup & Restore — Design Spec

> **Date:** 2026-09-25
> **Type:** Persistence + settings UI. The final P4 sub-phase (P4a–P4d).
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Parent spec:** [P4 scope & phase split](2026-09-25-p4-scope-and-phase-split-design.md) (§2, P4d)
> **Predecessors:** [P4a](2026-09-25-p4a-drag-and-drop-design.md) · [P4b](2026-09-25-p4b-edit-icon-rich-editor-design.md) · P4c (dark mode) — all done
> **Status:** Spec written — **awaiting user review before implementation.**

This document is the single source of truth for **P4d**. It contains: (0) the `.pen`
re-audit, (1) the **complete current persisted-state inventory** (what a backup must
capture), (2) the scope decision, (3) the locked decisions, (4) the technical design
(format + repository + UI), (5) files to touch, (6) the test plan, (7) the phase plan,
and (8) the questions that need the user's answer before code.

**No code is written under this spec. It stops for user review, same as P1–P4c.**

---

## 0. `.pen` re-audit (2026-09-25, pre-spec)

**File:** `design/homeApp.pen`, **176,128 bytes**, SHA256
`08DB2A81909EF7F71B351EB23D7F1AB993DC50053D82BD7AA8609A5D30CA6201`.

**Result: byte-identical to every audit since P3.5** (P3.5, P4a, P4b, P4c, now P4d).
The user has not edited it. Top-level frames are unchanged (6): `znb90` (light home),
`L7ZAp` (spec board), `TpzL1` (drawer), `KkPN3` (dark home), `QBhow` + `ciHU3`
(glyph libraries).

**Keyword sweep for a backup/restore design — all ABSENT:**

| Keyword | Hits | Meaning |
|---|---|---|
| `backup` / `restore` / `export` / `import` | 0 | **Nothing.** No backup mock, no settings entry design. |
| `cloud` / `sync` / `account` | 0 | Nothing. |
| `file` / `share` | 2 / 0 | Only lucide glyph/drawable names (`file`-ish), no flow. |

**Consequence:** like P3/P4a/P4b, P4d has **no design source**. The Backup/Restore
surface is authored from the existing Warm Right Rail vocabulary — the `SettingsRow`
list pattern (P3) plus the `PrimaryAction`/`SecondaryAction` pill buttons already used by
`IconPackImportSheet` (P1.5) — as documented assumptions (§7). The `.pen` stays
**read-only** (verified byte-identical after the build).

---

## 1. Current persisted-state inventory (what a backup must capture)

P4d's job is to snapshot and restore **all launcher user state**. Audited 2026-09-25.

### 1.1 DataStores (3 separate files)

| DataStore file | Owner | Keys (types) |
|---|---|---|
| `soft_home_prefs` | `PrefsRepositoryImpl` (`core:data`) | `grid_columns`(Int), `grid_rows`(Int), `grid_icon_scale_x100`(Int), `grid_show_labels`(Bool), `active_icon_pack`(String?), `mask_unsupported`(Bool), `theme_mode`(String), `show_badges`(Bool), `home_rows`(String — `HomeRowsCodec`), `spacing_scale`(String), `hidden_apps`(StringSet), `icon_overrides_json`(String — `IconOverridesCodec` P4b) |
| `soft_home_folders` | `FolderRepositoryImpl` (`core:data`) | `folders_json`(String — `FoldersCodec`, list of `Folder{id,name,apps}`) |
| `soft_home_notes` | `NotesRepositoryImpl` (`core:data`) | `quick_notes`(String — the notes body) |

**No Room.** Everything is DataStore-backed. `AppRepository`/`DeviceStatusRepository`/
`AppActionsRepository` hold **no** user state (installed apps, battery, intents are
re-derived from the device), so they are **not** backed up.

### 1.2 Derived / device state (NOT backed up)

| Thing | Why not backed up |
|---|---|
| Installed app list | Derived from `PackageManager` on every cold start. |
| Icon pack **contents** (zip bytes / installed pack APK) | Lives outside our prefs (in `filesDir/iconpacks/` or another app's APK). We back up only the **reference** (id) — see §3 P4d-5. |
| Wallpaper | Owned by the OS (`WallpaperManager`), not our prefs. |
| Notification badges | Live from the OS. |

### 1.3 The key finding (why P4d is mostly "one codec + one apply")

Unlike P4b (which had to **build a data path from scratch**), every persisted field
already has a **typed model + a pure codec** and a **typed repository setter**:

- `PrefsRepository` exposes one setter per pref (`setGrid`, `setHomeRows`, `setSpacing`,
  `setHiddenApps`, `setIconOverride`, `setDarkTheme`, …).
- `FolderRepository.save(list)` and `NotesRepository.setBody(String)` are whole-value setters.

So P4d = **serialize the union of those values to one JSON document, then apply it back
through the existing setters**. No new storage engine, no Room, no per-key plumbing.

**One gap to close:** there is currently **no single "apply all prefs atomically"**
entry point — a restore that called `prefs`'s setters one-by-one would emit N DataStore
edits (and N recompositions). P4d adds **one** bulk method (§4.2) so a restore is a
single write.

---

## 2. Scope decision

> **Q-A. What does a "backup" contain?**
>
> **Recommendation: a single JSON file** with a versioned envelope + three sections
> (`prefs`, `folders`, `notes`). Human-readable, forward-migratable (a `version` int),
> and self-describing. **Not** a zip (no binary payload to carry: §1.2 shows we back up
> only the pack *reference*, not its bytes).

> **Q-B. Transport?**
>
> **Recommendation: the Storage Access Framework (SAF)** via `ActivityResultContracts.
> CreateDocument` (export) and `OpenDocument` (import). This is exactly the pattern P1.5
> already uses for the icon-pack `.zip` (`IconPackImportSheet` → `rememberLauncherFor-
> ActivityResult`). No storage permission is needed on any API level, the user controls
> the file, and it works on both the emulator and a real device. The file is MIME
> `application/json` with a suggested name `softhome-backup-<yyyy-MM-dd>.json`.

> **Q-C. Restore semantics — merge or replace?**
>
> **Recommendation: REPLACE the launcher's persisted state wholesale** (the whole point
> of a backup is to reproduce a known state). The user is warned with a confirmation step
> (§4.4) because restore is destructive to the current state. The installed-app
> references (hidden apps, folder members, icon overrides) that no longer resolve are
> **kept as-is** (they are just keys; harmless if the app is uninstalled) — no pruning,
> so a restore is loss-free and reversible by restoring an older file.

> **Q-D. Icon-pack reference depth.**
>
> **Recommendation: back up the active pack *id* only** (`activeIconPackId`). If that pack
> is still present on the device after restore, `IconPackRepositoryImpl.restoreActivePack`
> rehydrates it exactly as it does on cold start (P3-5). If it is gone, the id simply
> fails to resolve and the launcher falls back to mask — the same graceful path as today.
> **Backing up the zip bytes is OUT** (D-038 spirit: no new binary-payload subsystem).

### 2.1 The resulting scope (recommended)

**IN (P4d):**
1. A versioned **`BackupDocument`** model + pure **`BackupCodec`** (encode/decode total,
   never throws) in `core:data`.
2. A **`BackupRepository`** (`export(out) -> Result`, `import(in) -> Result`) that reads
   the current state from the three DataStores, encodes it, and on import applies it
   through the existing typed setters + a new bulk `PrefsRepository.applyAll`.
3. A **"Backup & restore"** section in the P3 settings panel with two rows
   (**Export backup**, **Import backup**) wired to SAF launchers.
4. A **confirmation** step before an import overwrites state, and a **result message**
   (success / "not a SOFT / HOME backup" / read error).
5. **Unit tests**: codec round-trip, version handling, malformed/partial input, unknown
   fields, empty state; an end-to-end repository test on a Robolectric/in-memory store.

**OUT (P4d):**
- Backing up the icon-pack **bytes** / installed pack APKs (§2 Q-D).
- Any **cloud / account / auto-sync** (no design source; out of scope).
- Scheduled / automatic backups.
- Per-section (partial) backup — it's the whole state or nothing.

---

## 3. Locked decisions (carried in)

| # | Decision | Source |
|---|---|---|
| **P4d-1** | The backup is a **single JSON document** with a `version` int envelope. | §2 Q-A; forward-compat house rule |
| **P4d-2** | Transport is **SAF** (`CreateDocument` / `OpenDocument`), MIME `application/json`. | §2 Q-B; mirrors P1.5 |
| **P4d-3** | Restore **replaces** all persisted launcher state; a confirmation step precedes it. | §2 Q-C |
| **P4d-4** | Decode + apply are **total and graceful**: a malformed / foreign / partial file returns a typed failure and **leaves current state untouched**. | D-028 spirit (never crash on bad input) |
| **P4d-5** | The active icon pack is backed up as an **id reference only**; rehydration reuses P3-5's cold-start path. | §2 Q-D |
| **P4d-6** | The `.pen` stays **read-only**; the surface is authored from the P3 `SettingsRow` + P1.5 action-pill vocabulary as a documented assumption (§7). | D-002/D-005 |
| **P4d-7** | No new persistence engine: **DataStore only**, applied through the existing typed setters + one new bulk method. | §1.3 |

**New decisions proposed for P4d (need confirmation — §8):**

| # | Decision | Proposal |
|---|---|---|
| **P4d-8** | **Format** | JSON object: `{ "app":"SOFT_HOME", "version":1, "exportedAt":<epochMs>, "prefs":{…}, "folders":[…], "notes":"…" }`. |
| **P4d-9** | **Restore atomicity** | `PrefsRepository.applyAll(LauncherPrefs)` writes all `soft_home_prefs` keys in **one** `DataStore.edit`; folders + notes are written in one edit each. |
| **P4d-10** | **Foreign-file guard** | Import validates the `app` marker + `version ≤ CURRENT`; otherwise `ImportResult.NotABackup` / `UnsupportedVersion`. |

---

## 4. Technical design

### 4.1 The document + codec (`core:data`, pure, JVM-tested)

```kotlin
/** P4d: the whole launcher user state in one versioned, portable document. */
data class BackupDocument(
    val schemaVersion: Int = CURRENT_VERSION,
    val exportedAtEpochMs: Long,
    val prefs: LauncherPrefs,       // the full typed prefs (core:model)
    val folders: List<Folder>,      // core:model
    val notes: String,              // the quick-notes body
) {
    companion object { const val CURRENT_VERSION = 1; const val APP_MARKER = "SOFT_HOME" }
}

object BackupCodec {
    fun encode(doc: BackupDocument): String            // deterministic JSON
    fun decode(json: String?): BackupDecodeResult      // total: Ok | NotABackup | Unsupported(v) | Malformed
}
```

- Reuses the **existing item codecs** (`HomeRowsCodec`, `IconOverridesCodec`,
  `FoldersCodec`) where a value is itself structured, so there is one source of truth per
  shape (no parallel encodings to drift).
- `decode` is **total**: a wrong/missing `app` marker → `NotABackup`; `version` newer than
  `CURRENT_VERSION` → `Unsupported`; malformed JSON or missing required sections →
  `Malformed`. It never throws.

### 4.2 Bulk apply (`core:data`)

`PrefsRepository` gains:

```kotlin
/** P4d: replace ALL persisted prefs atomically (one DataStore.edit). */
suspend fun applyAll(prefs: LauncherPrefs)
```

It writes every `Keys.*` entry (grid, pack id, mask, theme, badges, home rows, spacing,
hidden apps, icon overrides) in a **single** `edit { }` block — so a restore is one write
and one reactive emission, not twelve.

### 4.3 The repository (`core:data`)

```kotlin
sealed interface BackupResult {
    data object Success : BackupResult
    data class Failure(val reason: String) : BackupResult
}

interface BackupRepository {
    /** Snapshot current state and write it to [out] (SAF stream). */
    suspend fun export(out: OutputStream): BackupResult
    /** Read [in], validate, and replace all state. */
    suspend fun import(input: InputStream): BackupResult
}
```

- `export`: `first()` the current `prefs`, `folders`, `notes`; `BackupCodec.encode`; write.
- `import`: read fully → `BackupCodec.decode` → on `Ok`, `prefsRepository.applyAll(doc.prefs)`,
  `folderRepository.save(doc.folders)`, `notesRepository.setBody(doc.notes)`. On any other
  decode result, return `Failure` **without touching state**.
- Runs on `DispatcherProvider.io` (reuses the existing `core:common` abstraction).

### 4.4 The settings surface (P3 panel, new section)

A new **"BACKUP & RESTORE"** section (the 5th, after Gestures) with two rows:

```
BACKUP & RESTORE
  Export backup        Export launcher settings to a file      [chevron]
  Import backup        Restore from a backup file              [chevron]
```

- **Export** → `CreateDocument("application/json")` with a suggested filename; on URI,
  `repository.export(contentResolver.openOutputStream(uri))`; show a transient result
  ("Backup saved" / failure reason).
- **Import** → `OpenDocument(arrayOf("application/json","*/*"))`; on URI, **first decode
  the file**, and only if it is a valid SOFT / HOME backup show an **inline confirm**
  ("Replace all settings with this backup?") before calling the destructive `import`.
  This matches the P4a-10 "predictable, non-destructive until confirmed" principle.
- Both call `SettingsViewModel` methods; the VM owns the SAF-independent logic so it is
  testable; the Activity/Composable owns only the `ActivityResultLauncher` plumbing (the
  same split `IconPackImportSheet` uses).

Reuses the existing atoms (`SettingsRow`) + the `PrimaryAction`/`SecondaryAction` pill
style from `IconPackImportSheet` for the confirm/message surfaces. **No new visual
language.**

### 4.5 Messaging

A single `MaterialSnapshot`-free state string in the VM (`backupMessage: String?`),
consumed on tap (like `IconPackImportViewModel.consumeMessage`). Text is exact and
actionable:

| Outcome | Message |
|---|---|
| Export ok | "Backup saved." |
| Export fail | "Could not write the backup file." |
| Import ok | "Backup restored." |
| Not a backup | "That file is not a SOFT / HOME backup." |
| Newer version | "This backup was made by a newer version." |
| Read fail | "Could not read that file." |

---

## 5. Files to touch (module map)

| Module | Change |
|---|---|
| `core:model` | *(none expected — `LauncherPrefs`/`Folder` already model everything; add nothing unless a field is missing)* |
| `core:data` | New `BackupDocument` + `BackupCodec` (pure, total); new `BackupRepository` + impl; `PrefsRepository.applyAll(LauncherPrefs)`; bind the repo in `DataModule`. |
| `app` | `SettingsPanel` gains the "Backup & restore" section; `SettingsViewModel` gains export/import + message state; SAF launchers live in the panel. |
| `core:designsystem` | *(none — reuse `SettingsRow` + the action-pill style; no new atom expected)* |
| `docs` | `00` (P4d note), `03` (feature map: G4 done), `04` (new section N), `05` (session log + final report), `06` (test plan), `09` (D-040 format, D-041 scope). |

No new Gradle module.

---

## 6. Test plan (real evidence, per prior phases)

**Unit (JVM, no device):**
- `BackupCodecTest` — round-trip a fully-populated document; empty state; **total**
  decode (malformed JSON / wrong app marker / newer version / missing sections) returns
  the right typed result and never throws; unknown extra fields are ignored; a v1 file
  decodes to the same model.
- `BackupRepositoryTest` — export writes a document that decodes back to the same
  `prefs`/`folders`/`notes`; import applies to the (fake) repositories; a non-backup
  stream returns `Failure` and leaves state untouched.
- `PrefsRepositoryTest` (+cases) — `applyAll` writes every key and is observable as a
  single `prefs` emission with the expected values.
- **Regression guard:** the **7 icon-pipeline tests** + all P4a/P4b/P4c suites stay green.
  If a P4b codec test needs an edit, the boundary leaked — stop and re-scope.

**Instrumented (Compose, AVD + real device):**
- `BackupRestoreTest` — the settings panel shows the section; the two rows exist and are
  enabled; (a Robolectric/instrumented round-trip through the repository is acceptable if
  driving the SAF picker is not automatable).

**On-device verification (emulator API 35 `soft_home_pixel` + real device Tecno Camon 50 Pro)** — real screenshots:
- [ ] Settings shows the "Backup & restore" section.
- [ ] Export writes a JSON file (dump it; it contains rows/folders/notes/theme).
- [ ] **Mutate state** (hide an app, add a folder, write a note, change the theme), then
      **Import** the file → all of it comes back.
- [ ] The restored state **survives `am force-stop`**.
- [ ] Importing a **non-backup** file → the graceful message, state unchanged.
- [ ] Screenshots from **both** the emulator and the physical device.

---

## 7. Documented assumptions (to record in `04` section N)

| # | Gap | Assumption |
|---|---|---|
| N1 | `.pen` has no backup mock | A new "Backup & restore" `SettingsRow` section in the P3 panel. |
| N2 | No format spec | One JSON document `{app,version,exportedAt,prefs,folders,notes}` (P4d-8). |
| N3 | No transport spec | SAF `CreateDocument` / `OpenDocument` (MIME `application/json`), like P1.5's zip flow. |
| N4 | Restore UX unspecified | Destructive, behind an inline confirm; result shown as a transient message. |
| N5 | Icon-pack bytes | Backed up as an **id reference** only; rehydrated via the P3-5 path (P4d-5). |
| N6 | Foreign/corrupt files | Guarded by the `app` marker + `version`; failure leaves state untouched (P4d-4/P4d-10). |

---

## 8. Decisions needed from the user before implementation

1. **Scope of the backup (§2 Q-A):** one JSON file of all launcher prefs (rows, spacing,
   hidden apps, icon overrides, folders, notes, theme, active pack **id**) — confirm.
2. **Transport (§2 Q-B):** SAF picker (recommended) — confirm, or prefer a fixed
   app-private path / share-sheet?
3. **Restore semantics (§2 Q-C):** **replace** wholesale behind a confirm (recommended) —
   confirm.
4. **Icon-pack depth (§2 Q-D):** back up the **id only** (recommended) — confirm.
5. **Format approval (P4d-8):** OK with the JSON envelope + `version` int?

**STOP — no implementation until these are answered.**

---

## 9. Phase plan (P4d)

| Phase | Work | Exit |
|---|---|---|
| **0** | Audit: confirm `.pen` byte-identical; inventory every DataStore key (§1); confirm the setters exist; identify the missing bulk-apply. | Findings recorded; scope locked. |
| **1** | Pure model: `BackupDocument` + `BackupCodec` (+ reuse the item codecs). | `BackupCodecTest` green. |
| **2** | `PrefsRepository.applyAll` (single edit) + `BackupRepository`/impl + DI. | `BackupRepositoryTest` + `PrefsRepositoryTest` green. |
| **3** | Settings surface: the "Backup & restore" section + SAF launchers + VM methods + confirm/message. | Rows render; VM logic unit-tested. |
| **4** | Instrumented test + on-device verification (emulator **and** real device): export → mutate → import → force-stop; foreign-file guard. | Evidence captured. |
| **5** | Docs (`00/03/04/05/06/07/09`) + cleanup + the **final project report**. | Docs consistent; suites green; git committed. |
