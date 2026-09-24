# P2 — Custom Widgets + Folder System (under Warm Right Rail) — Design Spec

> **Date:** 2026-09-25
> **Type:** Architectural (new subsystem: widgets E + folders D), built on the
> Warm Right Rail design system shipped in Session 5.
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Status:** Spec written — awaiting user review before implementation.

This document is the single source of truth for **P2**. It contains the `.pen`
re-audit, the three user-confirmed decisions, the agreed design, exact tokens,
and an end-to-end phase plan (Phase 0 → Phase 8).

---

## 0. User-confirmed decisions (locked)

These three answers were given explicitly and are not open for re-litigation:

| # | Topic | Decision |
|---|---|---|
| **P2-1** | **Folder location** | **Option B — folders live in the App Drawer** (grid-based). The home row list gets **no** folder element. Folders are purely an app-organization feature inside the drawer. |
| **P2-2** | **Quick notes shape + persistence** | **Tap-to-expand row** (progressive disclosure, consistent with Search/Music). Persistence is **DataStore-backed** (survives process death / restart), implemented in `core:data` reusing the P1 DataStore pattern. **Not** in-memory. |
| **P2-3** | **Battery / Storage data** | **Real device values** — `BatteryManager` (battery %) and `StatFs` (storage used/available). Pure Android APIs, no extra permission. **Not** static design values. |

**Deferrals carried in (also locked):**

| Item | Status | Note |
|---|---|---|
| MediaSession (#39) | **Deferred** (not deleted) | Music row stays static UI. Recorded as deferred in `04` section I. |
| KkPN3 dark editorial mode | **Deferred** | Remains a visual reference only; not executed in P2. |

---

## 1. `.pen` re-audit (2026-09-25) — no P2 nodes exist

The current `.pen` (103 KB, 288 named nodes, unchanged since 2026-09-24) was
parsed node-by-node. The only top-level frames remain those from the Warm Right
Rail redesign:

| Node ID | Name | Role |
|---|---|---|
| `znb90` | Home Screen Mockup — Warm Right Rail | Primary home mockup |
| `L7ZAp` | Warm Home Screen — Full System Board | Spec sheet (4 panels) |
| `TpzL1` | Warm App Drawer — Unique Icon Grid | App drawer |
| `KkPN3` | Home Screen Mockup — Dark Editorial | Dark reference only |

**Keyword sweep for P2 concepts:**

| Keyword | Hits | Actual meaning in the file |
|---|---|---|
| `calendar` | 5 | Only `bE2bO` "App Tile Calendar" + `M4W7DK` glyph inside the **drawer grid** — an app *tile*, not a widget |
| `battery` / `storage` / `notes` / `widget` / `quick` | 0 | Nothing |
| `folder` | 2 | Only the lucide icon names `folder-symlink` / `folder-open` on drawer app tiles |
| `expand` | 1 | Only the motion note "rail expands" (SEARCH state) |

**Conclusion:** there is **no** calendar / battery / storage / quick-notes widget
mock and **no** folder mock. The `.pen` stays **read-only**. All P2 visuals are
derived from the **existing Warm Right Rail patterns** (`HomeRow`, `HomeDivider`,
`RowDisplay` / `TweakLabel` typography, the Session-5 token set) — **not** from the
retired P1 grid-squircle / cream-card language.

> **Traceability note:** `docs/03` lists P2 design nodes `u0a8RP`, `o0Fi0`, `nFk4u`
> for E1/E2/E5. Those node IDs **no longer exist** in the `.pen` (they were part of
> the retired P1 file). P2 is therefore authored entirely as documented assumptions.

---

## 2. Stale P2 scaffolding found (and why it is replaced)

P2 code was pre-scaffolded (unwired) during P1 against the **retired** design
language. Confirmed by grep: **nothing in `feature:home`, `feature:appdrawer`, or
`app` references any of it** — it is dead code.

| File / symbol | Old-language trait (conflicts with Warm Right Rail) | Action |
|---|---|---|
| `SoftWidgets.SoftWidgetCard` | cream `#F6F0E7` card, **r24 + soft shadow** — the retired card style (Warm system uses flat divider-separated rows, no cards, no shadow) | **Remove** (superseded) |
| `SoftWidgets.CalendarWidget` / `BatteryWidget` / `NotesWidget` | built on `SoftWidgetCard`; bold `displayLarge`/`titleSmall` | **Remove**; rebuild as rows |
| `SoftWidgets.ThinProgressBar` | 4px bar, track = `surface`, fill = `tile` (#2B2B2B) — old vocabulary | **Remove**; rebuild with Warm progress tokens (`progressTrack` + `accent`) |
| `SoftWidgets.FolderPreviewTile` | **62dp squircle** — retired grid tile | **Remove**; rebuild as drawer folder tile (68 r21) |
| `FolderLogic` (`homeRootKeys`, `seedToolsFolder`, `HomeRootItem`) | builds a **home-grid slot list** ("folders lead the grid…") | **Rework** to a drawer-grid slot model (§4.7) |

`Folder` data class itself (id, name, apps) is a fine domain model and is
**reused**. `HomePage` (unused) is **removed** — home is no longer a paged grid.

---

## 3. Visual comparison — what changes vs the P1 scaffold

### Widgets: cards → rows

| Aspect | Retired P1 scaffold | P2 (Warm Right Rail) |
|---|---|---|
| Container | cream card r24 + shadow, `18dp` inner pad | `HomeRow` + `HomeDivider` (flat, 1px `#D0C2B1`), no card, no shadow |
| Row content x | card is inset | `homeRowPaddingX` = 42dp (same as every home row) |
| Typography | `displayLarge` bold / `titleSmall` | `ClockLarge`/`DateNumber` normal (big number), `TweakLabel`/`TweakLabelLean` (meta), `RowDisplay` (labels) |
| Progress bar | 4px, track `#E8DFD0`, fill `#2B2B2B` | 5px (`progressHeight`), track `progressTrack #B9AA98`, fill `accent #8A5F43` (matches the music row) |

### Folders: home-grid squircle → drawer tile

| Aspect | Retired P1 scaffold | P2 |
|---|---|---|
| Surface | home grid cell | app drawer grid (`TpzL1`) |
| Preview | 62dp squircle `FolderPreviewTile` | 68dp r21 tile `#F5EFE6` (`drawerTileNew`/`drawerTileRadius`) with a 2×2 mini icon grid inside |
| Label | — | drawer label 11pt `#3A3A3A` under the tile (`J3Lb4`) |
| Interaction | — | tap → open folder popup (cream r24 card, editable title) |

---

## 4. Agreed design

### 4.1 Module / file plan

| Module | Change |
|---|---|
| `core:model` | **Rework** `FolderLogic` → drawer-grid slot model. Add `FolderSlot` / `DrawerGridItem` types. **Keep** `Folder`. **Remove** `HomePage`, `HomeRootItem`. |
| `core:designsystem` | **New** `atom/HomeWidgetRows.kt` (`CalendarRow`, `BatteryStorageRow`, `NotesRow`). **New** `atom/FolderTile.kt` (drawer folder tile + mini-grid). **Extend** `Dimens` / `Color` if new tokens are needed. **Remove** stale `SoftWidgets.kt`. |
| `core:data` | **New** `NotesRepository` (+ impl) over DataStore. **New** `DeviceStatusRepository` (+ impl: `BatteryManager`, `StatFs`). **New** `FolderRepository` (+ impl: persist folders in DataStore). Bind in `RepositoryModule`. |
| `core:model` | Notes model (`Notes`), device-status model (`DeviceStatusSnapshot`), folder-grid model. |
| `feature:home` | **New** rows: Calendar, Battery/Storage, Notes. `HomeState` gains `Notes` (tap-to-expand). `HomeViewModel` gains a minimal state (rows are otherwise static/real-read). |
| `feature:appdrawer` | Folder support: build drawer grid items (app + folder refs), folder tile in grid, folder popup (open/close/rename/fill), persistence via `FolderRepository`. |

No new Gradle module. Follows the D-008 multi-module rule.

### 4.2 Row order on home

```
Time → Date → Weather → Search → Music → [NEW] Calendar → Battery/Storage → Notes
```

All rows are full-width, `HomeDivider`-separated, content x = `homeRowPaddingX`
(42dp). Rows are **tappable only where interaction is meaningful**:
- Weather / Calendar / Battery rows: informational (no tap) in P2.
- Search row: tap → SEARCH state (existing).
- Music row: tap → MUSIC state (existing).
- Notes row: tap → **NOTES** state (tap-to-expand; edits persist).

### 4.3 Calendar row (E3)

- Big day number in **`DateNumber`-style normal weight** (reuse `ClockLarge`/`DateNumber`
  token; a 48–60sp number reads as the row's hero) at left.
- Right column: weekday (`TweakLabel`) + month (`TweakLabelLean`), muted.
- Up to **2 static upcoming-event lines** (`TweakLabelLean`, `textMuted`), or
  "No upcoming events" when empty.
- **No `READ_CALENDAR`** in P2 — events come from the home layer as a static/empty
  list (same "static-first, live-data-later" precedent as music #39). Live
  `CalendarContract` is a later phase (P3 or a dedicated task).

### 4.4 Battery / Storage row (E4) — REAL values

Source: `BatteryManager` + `StatFs`. Pure Android, no permission.

- Battery: `BatteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)` → 0..100.
  Fall back to `ACTION_BATTERY_CHANGED` sticky intent `EXTRA_LEVEL`/`EXTRA_SCALE`
  when the property returns negative (some OEMs).
- Storage: `StatFs(Environment.getDataDirectory().path)` → used/total via
  `blockCount`/`availableBlocks` × `blockSize`. Report **used %**.
- Display: label (`RowDisplay`, e.g. "Battery") + big `%` + a thin **Warm**
  progress bar (5px, `progressTrack` track + `accent` fill). A second compact
  line shows storage used (`TweakLabelLean`, e.g. "Storage 62% · 78 GB free").
- **Live-ish refresh:** read once on composition + refresh on `ON_RESUME`
  (battery changes often; a `BatteryManager` callback is a P3 nicety, noted but
  not built here — P2 keeps it a cheap resume re-read to avoid over-engineering).

### 4.5 Quick notes row (E5) — tap-to-expand + DataStore

- **Collapsed (Idle):** one `HomeRow` showing a notes icon + `RowDisplay` label
  ("Quick notes") + a one-line muted preview (`TweakLabelLean`, ellipsized) of the
  saved text ("Nothing yet" when blank).
- **Expanded (NOTES state):** the row **grows in-place** (same in-place-grow
  animation family as MUSIC — reuse `MotionTokens.musicRise()` or add a
  `notesExpand()` alias; see §4.9) and reveals an inline `BasicTextField` over a
  flat area (no card), `RowDisplay`-scale label + multi-line editor styled with
  `bodyMedium`-family body text at `textBody` color, cursor `accent`.
- **Persistence:** every committed change writes to `NotesRepository` (DataStore).
  Commit on IME done / focus loss / state collapse; debounce is a P3 nicety
  (P2 commits on change with `rememberUpdatedState` + a single writer coroutine).
- **Survives process death / restart:** guaranteed by DataStore (tested).

### 4.6 HomeState change (feature:home)

```kotlin
enum class HomeState { Idle, Search, Music, Notes }
```

- `onTapRow(Notes)` → `Notes` (toggle back to `Idle` if already Notes).
- `isNotesOpen` helper.
- `HomeRowId` gains `Notes`, `Calendar`, `BatteryStorage`.
- Back / tap-outside → `Idle` (unchanged mechanism). Collapsing the Notes row
  commits the current text before reset.

### 4.7 Folders in the app drawer (D1/D2) — Option B

**Model (rework `FolderLogic`, `core:model`):**

```kotlin
// Drawer grid is a flat ordered list of cells.
sealed interface DrawerGridItem {
    data class App(val componentKey: String) : DrawerGridItem
    data class FolderItem(val folderId: String) : DrawerGridItem
}

object FolderLogic {
    const val PREVIEW_CAPACITY = 4
    fun previewKeys(folder: Folder): List<String>          // first 4 for the mini-grid
    fun isInsideFolder(folders: List<Folder>, key: String): Boolean
    fun drawerGridItems(allApps: List<String>, folders: List<Folder>): List<DrawerGridItem>
    // folders lead the list (so the folded preview is visible first), then apps not in a folder
    fun rename(folder: Folder, name: String): Folder
    fun addApp(folder: Folder, key: String): Folder
    fun removeApp(folder: Folder, key: String): Folder
    fun createFolder(id: String, name: String, keys: List<String>): Folder
}
```

**Behavior:**
- The drawer grid (`LazyVerticalGrid`, 4 cols) is built from
  `FolderLogic.drawerGridItems(...)` instead of the flat app list. Folder cells
  render a `FolderTile` (68 r21 `#F5EFE6` with a 2×2 mini icon grid) + label.
- **Tap a folder** → open a **folder popup** (cream `card` r24, dim scrim behind):
  editable title (centered bold, tap to rename), a grid of the folder's apps
  (reuse the drawer tile), and a close affordance. Back / tap-outside closes.
- **Fill a folder (P2 minimal flow):** folders are **created and named from the
  drawer** via a seed/empty-folder entry point. To keep P2 bounded and testable:
  - P2 ships **one entry point**: a "New folder" action in the drawer header menu
    that creates an empty folder and opens its popup; the user assigns apps from
    within the popup by choosing from a simple in-popup list.
  - Full drag-and-drop app→folder (Nova-style) is **explicitly deferred to P3**;
    recorded in `04`. Rationale: drag-and-drop reordering/gestures are their own
    subsystem and the redesign has no gesture spec for them.
- **Persistence:** folders persist via `FolderRepository` (DataStore, JSON-encoded
  list). Survive restart (tested).

**Filtering interaction:** category + search filtering operate on apps; a folder
cell is shown when at least one of its apps is in the current filter, and inside an
open folder the search applies to the folder's apps. (Kept simple; documented.)

### 4.8 Error handling

| Case | Behavior |
|---|---|
| `BATTERY_PROPERTY_CAPACITY` < 0 (OEM) | Fall back to sticky `ACTION_BATTERY_CHANGED`; if that fails too, hide the battery % (never crash) |
| `StatFs` throws | Hide the storage line; battery still renders |
| Notes text extremely long | TextField scrolls; stored as-is; preview line ellipsizes |
| Corrupt folders JSON in DataStore | Parse failure → empty folder list (never crash); previous file kept |
| Folder with 0 apps | Popup shows "Empty folder" hint; grid cell still renders |
| Folder apps no longer installed | Filtered out at grid-build time; folder may become empty (handled above) |
| Missing calendar events | "No upcoming events" line |

### 4.9 Tokens (extend, don't invent)

P2 **reuses** Session-5 tokens wherever possible. Expected additions (kept minimal):

| Token | Value / source | Why |
|---|---|---|
| `Dimens.folderTile` | 68.dp (reuse `drawerTileNew`) | drawer folder tile matches app tile |
| `Dimens.folderTileRadius` | 21.dp (reuse `drawerTileRadius`) | same squircle |
| `Dimens.folderPopupRadius` | 24.dp (reuse `Shape large`) | popup card |
| `Dimens.widgetProgressHeight` | 5.dp (reuse `progressHeight`) | row progress bar |
| `Spacing` | reuse `xl` (16), `xxl` (24), `sm`, `md` | row paddings |
| `MotionTokens.notesExpand()` | alias → `musicRise()` (280ms, WarmEase) | in-place grow, same family |
| Colors | reuse `progressTrack`, `accent`, `divider`, `card`, `tileWarm` | no new colors planned |

Rule unchanged: **no screen hardcodes a color / duration / size**; everything goes
through tokens. If a value is genuinely new, it is added to `Dimens`/`Color` and
recorded in `04` (not inlined).

### 4.10 New icons

Reuse existing drawables where possible: `calendar_days`, `pen_line`, `folder`
already exist. **Confirmed missing** (audited 2026-09-25): `battery` and a
storage glyph — add `battery.xml` and `hard_drive.xml` lucide glyphs in the same
ASCII-safe converter style (D-010) during Phase 0.

---

## 5. Testing plan

### Unit (JVM)
| Test | Asserts |
|---|---|
| `FolderLogicTest` (rework of grid logic) | `drawerGridItems` = folders-first + non-nested apps; preview takes first 4; add/remove/rename/create; app-inside-folder detection; empty-folder handling |
| `NotesRepositoryTest` (Robolectric + real DataStore) | write → read round-trip; blank/cleared text persists as blank; value survives a fresh repository instance over the same file (restart proxy) |
| `FolderRepositoryTest` (Robolectric) | save → load round-trip; corrupt JSON → empty list, no throw |
| `DeviceStatusTest` (Robolectric + fakes) | battery % parsing incl. fallback path; storage used% math from fake block counts; negative/absent values degrade gracefully |
| `HomeStateTest` (extend) | Idle→Notes→Idle; Search/Music unaffected |
| `MotionTokensTest` (extend) | `notesExpand()` == 280ms + (0.2,0.8,0.2,1) |

### Compose UI
| Test | Asserts |
|---|---|
| `HomeWidgetRowsTest` | Calendar/Battery/Notes rows render with expected labels; Notes collapsed vs expanded; tap toggles |
| `AppDrawerFolderTest` | folder tile renders 2×2 preview; tap opens popup; popup title + close; empty folder hint |
| `HomeScreenTest` (extend) | new rows present in order |

### Manual (Phase 7 — real screenshots, not prose)
- Home **Idle** with all 8 rows (screenshot).
- **Battery/storage row verified against the device**: screenshot the row AND
  `adb shell dumpsys battery` / `adb shell df` output side-by-side to prove the
  numbers match the actual emulator state.
- **Notes tap-to-expand**: screenshot collapsed, then expanded with typed text;
  then **kill the app** (`adb shell am force-stop`) and relaunch — screenshot
  proving the notes text survived.
- **Folder in drawer**: create folder → screenshot; open folder → screenshot;
  add app / close → screenshot; relaunch → folder persisted.
- TalkBack pass on new rows + folder popup; contrast check; reduced-motion check.

---

## 6. Documentation updates (follow prior pattern)

| Doc | Update |
|---|---|
| `00-DESIGN-SOURCE.md` | Note P2 has **no** design nodes; P2 visuals derive from Warm Right Rail patterns |
| `02-DESIGN-SYSTEM.md` | Add P2 atom rows + folder tile to the atoms table; note reused tokens |
| `03-FEATURE-MAP.md` | Update E3/E4/E5 (widgets as rows), D1/D2 (drawer folders), D3 stays stub; fix stale node IDs |
| `04-ASSUMPTIONS.md` | **New section I**: P2 decisions (folders in drawer, notes tap-expand + DataStore, real battery/storage), retirement of `SoftWidgets.kt`, staleness of `00/03` nodes, deferrals (MediaSession #39, KkPN3) |
| `05-PROGRESS.md` | New Session 6 log (P2) with evidence |
| `09-DECISIONS-LOG.md` | D-022..D-0xx: drawers-folder decision, notes persistence, real device status, retirement of card widgets |

---

## 7. Phase plan (end-to-end)

Each phase is one focused unit with its own exit criteria. Do not start a phase
until the previous phase's exit criteria pass.

### Phase 0 — Model + tokens + assets
**Goal:** data shapes and any new tokens/icons exist; nothing rendered.
- Rework `FolderLogic` → `DrawerGridItem` model; remove `HomePage`/`HomeRootItem`.
- Add `Notes`, `DeviceStatusSnapshot` models in `core:model`.
- Add any needed `Dimens`/`Color` tokens (likely reuse-only) + `MotionTokens.notesExpand()`.
- Add `battery.xml` / `hard-drive.xml` glyphs if missing.
- **Tests:** `FolderLogicTest`, `MotionTokensTest` (extend).
- **Exit:** module compiles; new pure-logic tests green; no UI changed.

### Phase 1 — Repositories (`core:data`)
**Goal:** persistence + device reads exist and are testable.
- `NotesRepository` (DataStore) + impl + binding.
- `FolderRepository` (DataStore JSON) + impl + binding.
- `DeviceStatusRepository` (`BatteryManager` + `StatFs`) + impl + binding.
- **Tests:** `NotesRepositoryTest`, `FolderRepositoryTest`, `DeviceStatusTest` (Robolectric).
- **Exit:** repositories green; no UI changed.

### Phase 2 — Design-system atoms
**Goal:** reusable rows + folder tile.
- New `HomeWidgetRows.kt`: `CalendarRow`, `BatteryStorageRow`, `NotesRow`.
- New `FolderTile.kt`: drawer folder tile (68 r21) + 2×2 mini grid + reuse `AppIcon`.
- **Remove** `SoftWidgets.kt`.
- **Exit:** atoms compile; a preview renders each.

### Phase 3 — Home integration
**Goal:** home shows calendar / battery-storage / notes rows.
- Wire rows into `HomeScreen` row list (order per §4.2).
- `HomeState` + `HomeRowId` gain `Notes`; tap-to-expand for notes.
- Wire `HomeViewModel` → `NotesRepository` (read + write), `DeviceStatusRepository`
  (read on resume).
- **Tests:** `HomeStateTest` (extend), `HomeWidgetRowsTest`, `HomeScreenTest` extend.
- **Exit:** home renders all rows; notes expand + persist; battery/storage show real values.

### Phase 4 — Drawer folders
**Goal:** folders live in the drawer.
- Build drawer grid from `FolderLogic.drawerGridItems`.
- `FolderTile` in the grid; folder popup (open/close/rename/fill).
- "New folder" entry point in the drawer header.
- Wire `FolderRepository` (load/save).
- **Tests:** `AppDrawerFolderTest`, `FolderLogicTest` already covers logic.
- **Exit:** folder tiles render, popup works, folders persist.

### Phase 5 — Verification on device (real screenshots)
**Goal:** prove it works like P1.5/Session 5 did.
- Build debug APK; install on emulator (Android 15 / API 35, `soft_home_pixel`); set as home.
- Capture every state listed in §5 Manual, including the **battery/storage vs
  `dumpsys battery`/`df` comparison** and the **notes-survive-restart** proof.
- TalkBack / contrast / reduced-motion checks.
- Save screenshots to `docs/screenshots/`.
- **Exit:** evidence captured; no crashes; no regressions; all unit tests green.

### Phase 6 — Cleanup + docs
**Goal:** docs consistent; dead code gone.
- Update `00`, `02`, `03`, `04`, `05`, `09` per §6.
- Confirm `SoftWidgets.kt` fully removed and unreferenced.
- **Exit:** build clean; all tests green; docs consistent.

### Phase 7 — Final review / handoff
**Goal:** checkpoint.
- Summarize what shipped, evidence, open items; list deferrals.
- **STOP for user review before P3.**

---

## 8. Out of scope / deferred (explicit)

- **MediaSession (#39)** — music row stays static UI. Deferred, not deleted.
- **KkPN3 dark editorial home** — reference only; not implemented.
- **Drag-and-drop app→folder** (Nova-style) — deferred to P3.
- **Live weather provider** — unchanged (static).
- **Live calendar events (`CalendarContract`)** — static/empty list in P2.
- **Battery change callback stream** — P2 re-reads on resume; push stream is P3.
- **Multi-page home / widgets drag-placement** — not part of P2.
- **System widgets (AppWidgetHost, D3)** — remains a stub (P2+).

---

## 9. Open risks

| Risk | Mitigation |
|---|---|
| `BATTERY_PROPERTY_CAPACITY` unreliable on some OEMs | Sticky-intent fallback; hide on total failure |
| `StatFs` semantics differ (data vs external) | Use `getDataDirectory()` (app-visible storage); document the choice in `04` |
| Folder-in-drawer vs category/search filter interplay | Simple rule: folder shown when ≥1 member passes filter; documented + tested |
| Notes commit timing (lost edits) | Commit on IME-done / focus-loss / collapse; value also held in VM state |
| Drawer grid key stability with folders | Stable keys: `app:<componentKey>` / `folder:<id>` |
| Large surface for one P2 | Phase gating; each phase independently verifiable; drag-and-drop deferred |
