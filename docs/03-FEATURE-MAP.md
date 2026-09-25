# 03 - Feature Map

Master table of every requested feature -> priority -> backing design node -> status.

**Status legend:** [ ] todo - [~] in progress - [x] done - **stub** = skeleton only

---

## Priority 1 - MVP (Home + Icon Pack + App Drawer)  ·  DONE (incl. P1.5)

> **P1.5** (icon-pack real decode) completed after the P1 checkpoint. B1-B3 and B5
> are now fully real; P1's placeholder glyph rendering was retired.

### A. Home Screen Core

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| A1 | Home screen replacement (default launcher) | `launcher/HomeActivity` (manifest `HOME`/`DEFAULT`) | [x] | verified `set-home-activity` on emulator |
| A2 | Customizable grid (columns, spacing, icon size) | *(legacy)* | [~] | `GridConfig` model kept; home is now a row list; UI editor in P3 |
| A3 | Minimal search bar + voice search | `Q1cGYj` `N7ICqS` | [~] | **REDESIGNED**: row with "find something" text + search icon; tapping enters SEARCH state; voice action still a stub |
| A4 | Multiple pages + custom page indicator (pill lines) | *(assumption)* | [x] | removed in favour of the single Warm Right Rail page |
| A5 | Gestures (swipe up -> drawer, swipe L/R -> page, double-tap -> lock) | *(behavior)* | [~] | swipe-up -> drawer done; others P3 |
| A6 | Clock on home | `IDSBb` / `cFKcs` | [x] | **REDESIGNED**: 58 normal, ls −2; **P8**: local minute ticker + per-digit transition (D-061) |
| A7 | Date on home | `ApuhU` `KkRQg` `ftENm` | [x] | **REDESIGNED**: number 60 normal + spaced day/month |
| A8 | Weather row on home | `lLPZS` | [x] | **REDESIGNED**: "Current 8°C", 28 normal; **P8**: abstract fallback `condition = Clear` + condition cross-fade + ambient icon (D-060) |
| A9 | **Right icon rail (8 shortcuts)** | `hrsLU` / `B6633e` | [x] | **NEW**: 72dp, `#D8C8B6`, line icons → default device apps; **P8**: press/drag/drop feedback on the single existing drag engine |
| A10 | ~~Music player row~~ | `QTwqr`… `pvQO0` `d41sbp` | [x] | **REMOVED** (Session 24 / D-062): the home no longer renders a music row (P8 music reverted); the `.pen` node remains the design source |
| A11 | **3 interaction states (Idle/Search/Music)** | `Az7qs` `m9OlxQ` `T8AA1` | [x] | **NEW**: tap-row trigger, tokenized motion (`MotionTokens`) — now Idle/Search/Notes (the Music state was removed) |

### B. Custom Icon Pack System

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| B1 | Import/apply icon pack (`appfilter.xml` + drawable) | `lk7jo`, card `X7SHLQ` | [x] | **P1.5 DONE**: real drawable decode (`IconPackDrawableLoader`); zip import flow + installed-pack query (`IconPackImporter`, `InstalledIconPackScanner`, `IconPackImportSheet`); progress + graceful failure |
| B2 | Preview icon before apply | `lk7jo` grid | [x] | **P1.5 DONE**: the live drawer/home grid *is* the preview - decoded drawables render in place |
| B3 | Auto-mask unknown apps (mask real icon) | `lk7jo` tile spec | [x] | **P1.5 DONE**: real icon composited into the squircle as a cream monochrome mark (`AutoMask`, `IconCompositor`, `IconBitmapProvider`) |
| B4 | Minimalist notification badge (cream dot, no count) | `lQywT` / `ha6OA` | [~] | `CreamBadgeDot` built; listener P3 |
| B5 | Icon glyph set (lucide line-art) | `lk7jo` symbols | [x] | 26 vector drawables; glyphs are now the *fallback* only, not the pack rendering |

### C. App Drawer

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| C1 | Separate drawer, swipe-up trigger, 4-column grid | `TpzL1` | [x] | **REDESIGNED**: tile 68 r21 (was 104 r30), bg `#DCCDBA`. **P3.5**: cream tile + per-category colored glyphs (was charcoal mono) |
| C2 | Alphabetical index on right | `czxh4` | [x] | active `#B06F52`, idle `#81796D` |
| C3 | Search/filter apps inside drawer | `V7udUl` | [x] | **REDESIGNED**: h56 r28 `#E8DFD0` stroke `#C8B8A6` |
| C4 | Drawer header / search placement | `lasU6` | [x] | polished grid-first layout; redundant "All apps" header removed and search pill anchored below the icon grid |
| C5 | **Category nav** | `B6gGM` | [x] | **P5**: 8 `ciHU3` groups in a horizontal pager; both tab tap and left/right swipe switch pages. Search + alphabet rail remain global. |
| C6 | **App labels under tiles** | `J3Lb4`… | [x] | **NEW**: 11pt `#3A3A3A` |
| C7 | **Color icon system** (P3.5, extended P4) | `TpzL1` | [x] | **P3.5**: cream tile + pack artwork or category-colored glyph + amber selected state. **P4**: per-package glyph from `DrawerIconMap` + a dedicated **Browser** color token |
| C8 | **Per-app drawer icons (no duplicates)** (P4) | `TpzL1` + `ciHU3` | [x] | **P4**: `DrawerIconMap` (TpzL1 tiles + ciHU3 library) + `DrawerIconAssignment` uniqueness; 7 broken lucide drawables fixed |

---

## Priority 2 - Widgets (E) + Folders (D)  ·  DONE (2026-09-25)

> **P2 is implemented under the Warm Right Rail design system** (not the retired P1
> grid-squircle/card language). The `.pen` has **no** widget/folder mock, so these are
> documented assumptions — see [`04` section I](04-ASSUMPTIONS.md) and the
> [P2 spec](superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md). The old
> node IDs previously listed here (`u0a8RP`, `o0Fi0`, `nFk4u`) no longer exist.

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| E1 | Clock & date on home | `IDSBb`/`ApuhU` | [x] | (row, from the redesign) |
| E2 | Weather row on home | `lLPZS` | [x] | (static values, from the redesign) |
| E3 | **Calendar row** (big date + upcoming events) | *(assumption)* | [x] | `CalendarRowContent`; static/empty events — live `CalendarContract` **deferred** |
| E4 | **Battery/storage row** (thin progress bar) | *(assumption)* | [x] | `BatteryStorageRowContent`; **REAL** `BatteryManager` + `StatFs` values |
| E5 | **Quick notes row** (tap-to-expand editor) | *(assumption)* | [x] | `NotesRowContent`; **persisted in DataStore** (survives restart) |
| D1 | **Folder in the app drawer** (68 r21 + 2×2 preview) | *(assumption)* | [x] | `FolderTile`; folders live in the **drawer** (not the row home) |
| D2 | **Folder popup** (cream r24, editable title, add/remove apps) | *(assumption)* | [x] | `FolderPopupBody`; "New folder" + assign-from-popup |
| D3 | Live system widgets (AppWidgetHost) | - | **stub** | P2+ |

### P2 deferred (recorded, not removed from scope)

| Item | Status |
|---|---|
| Drag-and-drop app → folder | ⛔ deferred to **P3** (gesture subsystem) |
| Live calendar events (`CalendarContract` + `READ_CALENDAR`) | ⛔ deferred |
| Battery change callback stream (vs re-read on resume) | ⛔ deferred to **P3** |
| Real music playback / MediaSession | ⛔ deferred (the home music row itself was removed in Session 24 / D-062) |
| KkPN3 dark editorial mode | ⛔ deferred (still) |

---

## Priority 3 - System UI (F) + Settings (G)  ·  DONE (2026-09-25)

> **P3 is implemented under the Warm Right Rail design system.** The `.pen` has **no**
> status/nav/long-press/settings/toggle mock; the node IDs previously listed here
> (`ogMkZ`, `Fzobx`/`pu2gg`, `nFk4u`, `UPa9N`) are from the **retired P1 file** and do
> not exist. P3 is authored as documented assumptions — see
> [`04` section J](04-ASSUMPTIONS.md) and the
> [P3 spec](superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md).

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| F1 | Status bar icon color follows theme (charcoal, transparent) | *(assumption)* | [x] | `SystemBarAppearance`: dark icons on cream, light on warm-dark; transparent bars. Verified light + dark |
| F2 | Nav bar: hide on gesture nav / minimal on button nav | *(assumption)* | [x] | hidden with transient reveal under gesture nav; minimal transparent bar otherwise |
| F3 | Long-press menu near icon (App Info, Uninstall, Edit Icon, Shortcuts) | *(assumption)* | [x] | row-style r24 `AppContextMenu` on **rail icons + drawer tiles** (P3-3); Uninstall greyed for system apps (P3-4) |
| G1 | Settings sections: Appearance / Widgets / Wallpaper / Gestures | *(assumption)* | [x] | `SettingsPanel` (4 sections); `SettingsRow` + `SoftToggle`; opened from the rail settings icon (Q1) |
| G2 | Custom pill toggle (charcoal, not system default) | *(assumption)* | [x] | `SoftToggle` atom (42×22, charcoal, `Role.Switch`) |
| G3 | Dark/light mode (warm dark palette) | *(assumption)* | [x] | `ThemeMode` persisted + applied app-wide (Theme setting cycles Light/Dark/System) |
| G4 | Backup & restore settings | *(assumption)* | [x] | **P4d DONE** (Session 13): export/import all launcher state as one JSON file via SAF; versioned + total decode. See [P4d spec](superpowers/specs/2026-09-25-p4d-backup-restore-design.md) |

### Home Widgets (P3 addition to G1/Widgets)

| ID | Feature | Status | Notes |
|---|---|---|---|
| G-W | Row visibility + order (locked set Time/Date/Weather) | [x] | `HomeRowLogic` (P3-1); toggle + inline ↑/↓ (P3-2 ships move buttons; drag → P4) |
| G-H | Hidden apps manager ("Remove" from drawer, reversible) | [x] | `hiddenApps` set persisted; restore from the Appearance section (Q2) |
| G-S | Spacing preset (Compact/Normal/Roomy ×0.88/1.0/1.12) | [x] | `SpacingScale` (Q3) |
| G-IP | Icon pack persists + rehydrates on cold start | [x] | closes `04` #36 (P3-5) |

### P3 deferred (recorded, not removed from scope)

| Item | Status |
|---|---|
| Drag-and-drop row reorder | ✅ **done in P4a** |
| Drag-and-drop app → folder (from P2) | ✅ **done in P4a** |
| Rich icon editor (upload/crop custom icon) | ⛔ deferred to **P4b** (P3 ships pick-from-pack, Q4) |
| Live-wallpaper engine | ⚪ **dropped from roadmap** (flat default + system picker in P3, P3-6) |
| Backup & restore settings (G4) | ⛔ deferred to **P4d** |
| Real gesture actions (swipe-down / double-tap) | ⛔ deferred (settings rows show "Coming soon") |

---

## Priority 4a - Drag & Drop  ·  DONE (2026-09-25)

One gesture engine, two targets. `.pen` has **no** drag/drop mock — authored from Warm
tokens. Full spec:
[P4a spec](superpowers/specs/2026-09-25-p4a-drag-and-drop-design.md). See also
[04 section K](04-ASSUMPTIONS.md).

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| P4a-1 | Drag-reorder home rows | *(assumption)* | [x] | Long-press-lift + accent insertion line; `HomeRowDropResolver`; composes with the P3 ↑/↓ buttons |
| P4a-2 | Drag app → existing folder | *(assumption)* | [x] | Folder tile = drop target (accent hover); `FolderDropResolver.assign` |
| P4a-3 | Drag app → "New folder" chip (drop-to-create) | *(assumption)* | [x] | `FolderDropResolver.createWith` |
| P4a-4 | Long-press split: menu vs. drag | *(assumption)* | [x] | one gesture owner: no-move → menu, move → drag (D-036) |
| P4a-5 | Persistence (order + folder membership) | — | [x] | verified across `am force-stop` |
| — | Drag-out of a folder | *(assumption)* | [~] | **descoped** (popup clickable fights the drag; tap-to-remove exists); resolver kept + tested |

### P4 (remaining) — planned

| Sub-phase | Item | Status |
|---|---|---|
| P4b | Rich icon editor (pack drawable or category glyph; recolor; reset) | [x] **done** (Session 11) |
| P4c | Dark mode rendered from the `KkPN3` palette | [x] **done** (Session 12) |
| P4d | Backup & restore settings (G4) | [x] **done** (Session 13) |

> **P4 is complete.** All four sub-phases (P4a–P4d) are done. See the P4d section below.

---

## Priority 4d - Backup & Restore  ·  DONE (2026-09-25)

`.pen` has **no** backup/restore mock (byte-identical re-audit; sweep 0) — authored from
the P3 `SettingsRow` + P1.5 action-pill vocabulary. Full spec:
[P4d spec](superpowers/specs/2026-09-25-p4d-backup-restore-design.md). See also
[04 section N](04-ASSUMPTIONS.md) and [D-040/D-041/D-042](09-DECISIONS-LOG.md).

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| P4d-1 | Export all launcher state to one file | *(assumption)* | [x] | One versioned JSON doc (rows, spacing, hidden, overrides, folders, notes, theme, pack id); SAF `CreateDocument` |
| P4d-2 | Import/restore, destructively, behind a confirm | *(assumption)* | [x] | Decodes first; only a valid `SOFT_HOME` file offers the inline confirm; SAF `OpenDocument` |
| P4d-3 | Total, non-destructive failure handling | — | [x] | `NotABackup` / `UnsupportedVersion` / `Malformed` leave state untouched; never throws |
| P4d-4 | Atomic apply | — | [x] | `PrefsRepository.applyAll` writes all prefs in one DataStore edit |
| P4d-5 | Round-trip verified on device | — | [x] | `BackupRestoreEndToEndTest` on emulator (API 35) + real device (API 36); file dump 421 B |

---

## Priority 4c - Dark Mode (KkPN3)  ·  DONE (2026-09-25)

`.pen` now has a **named dark palette** — frame `KkPN3` "Home Screen Mockup — Dark
Editorial". P4c renders it faithfully (replacing the derived warm-dark guess). See
[04 section M](04-ASSUMPTIONS.md) and [D-039](09-DECISIONS-LOG.md).

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| P4c-1 | Dark palette = verbatim `KkPN3` values | `KkPN3` | [x] | bg `#18191A`, rail `#2E3134`, divider `#343638`, text `#F2EEE7`/`#D2CBC1`/`#918F8B` |
| P4c-2 | Home + rail render `KkPN3` in dark | `KkPN3` | [x] | near-black canvas + `#2E3134` rail (screenshot) |
| P4c-3 | Drawer + category glyphs legible in dark | *(assumption)* | [x] | dark tiles + the P3.5 color glyphs' dark adaptations |
| P4c-4 | Settings panel follows `ThemeMode` | *(assumption)* | [x] | `SettingsStubActivity` fix; status/nav bar matches |
| P4c-5 | Context menu / folder popup / icon editor in dark | *(assumption)* | [x] | all read `SoftColors`, so they follow the palette |
| P4c-6 | Token lock | — | [x] | `DarkPaletteTest` pins the `KkPN3` hexes |

---

## Priority 4b - Edit Icon (rich editor)  ·  DONE (2026-09-25)

`.pen` has **no** editor mock (byte-identical re-audit; sweep 0) — authored from Warm
tokens. Full spec:
[P4b spec](superpowers/specs/2026-09-25-p4b-edit-icon-rich-editor-design.md). See also
[04 section L](04-ASSUMPTIONS.md).

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| P4b-1 | "Edit Icon" opens a real editor | *(assumption)* | [x] | cream r24 card over a dim scrim, opened from the long-press menu row (`IconEditorSheet`) |
| P4b-2 | Pick a pack drawable | *(assumption)* | [x] | grid of the active pack's **mapped distinct drawables**; `IconOverride.Pack` |
| P4b-3 | Pick a glyph + color (non-pack apps) | *(assumption)* | [x] | glyph grid + 7 `TpzL1` color swatches; `IconOverride.Glyph` |
| P4b-4 | Reset to automatic | *(assumption)* | [x] | clears the override → derived (pack/category) rendering |
| P4b-5 | Live preview | *(assumption)* | [x] | uses the **same** `DrawerIconTile` path as the grid (cannot drift) |
| P4b-6 | Persistence | — | [x] | DataStore (`IconOverridesCodec`); verified across `am force-stop` |
| P4b-7 | Applies everywhere via the resolver | — | [x] | drawer + folder previews + home rail (one resolver path) |
| — | Crop / resize / upload a custom image | — | ⛔ | **out of P4b** (D-038); recorded as a future phase |
| — | Raw pack drawable enumeration | — | ⛔ | **out of P4b**; picker uses the mapped set |

> **Roadmap note:** the earlier "P4 = Onboarding (H) + polish" framing was re-split into
> P4a–P4d by the [P4 scope spec](superpowers/specs/2026-09-25-p4-scope-and-phase-split-design.md).
> **Onboarding (H) is OUT** — the `.pen` has no onboarding mock (`onboard`/`welcome`/`setup`
> = 0 hits). MediaSession + live calendar stay parked past P4. Live-wallpaper engine +
> battery callback stream are dropped.

---

## Cross-cutting requirements

| Area | Requirement | Status |
|---|---|---|
| Performance | startup <1s, 60fps, no jank | [~] | app list load off main thread; to measure |
| Battery | minimal background work, no leaks | [~] | no background services in P1 |
| Compatibility | API 26 -> latest | [x] | compileSdk 35 / minSdk 26 |
| Responsive | phone + tablet | [~] | drawer tiles adaptive; home grid column-driven |
| Testing | unit + Compose UI | [~] | 54 unit tests pass; 3 Compose UI smoke tests written |
| Error handling | graceful fallback on corrupt icon pack | [x] | `IconPackParseResult.Invalid` -> mask/system; **P1.5**: corrupt zip / missing drawable folder / unreadable icons all fail as `ImportProgress.Failed` without crashing |
| Accessibility | TalkBack + WCAG contrast | [~] | content descriptions added; contrast verified in docs/06 |
