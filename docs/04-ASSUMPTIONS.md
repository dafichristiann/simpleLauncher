# 04 — Assumptions

The `.pen` file is a **spec sheet**, not a pixel-complete screen set. Where the
design is silent or ambiguous, this file records the assumption taken and the risk
if it is wrong. **Append-only** — add new rows as gaps are found during the build.

Status legend: ❓ open · 🔒 accepted · 🔁 revisit later

---

## A. Design-source assumptions

| # | Design gap | Assumption taken | Risk if wrong | Status |
|---|---|---|---|---|
| 1 | `.pen` is a guide/spec sheet, not a screen spec; the only mobile mockup is `HhwVC` (390×650) | App pixel values for home are derived from `HhwVC`; density-independent tokens (color/radius/shadow) are taken literally | Layout may need re-tuning per device, but tokens stay valid | 🔒 |
| 2 | **Page indicator** ("garis pill kecil") has no design node | Active page = short tall pill; inactive = small dot/line, both in charcoal at low opacity, centered above the mic pill | Visual may differ from intent | ❓ |
| 3 | **Folder popup** & **2×2 mini-grid** preview have no mock | Use `SoftCard` (r24, `#F6F0E7`, soft shadow), 2×2 of mini `AppIconTile`s, editable title as centered bold text | Folder look unverified | ❓ |
| 4 | **Calendar / notes / battery widgets** have no direct mock (only names in settings + icon set) | Build them from tokens: card `#F6F0E7`, r24, soft shadow; big number DM Sans bold; thin progress bar = 4px `#2B2B2B` on `#E8DFD0` track | Widget proportions guessed | ❓ |
| 5 | **Dark mode** requested, but no dark palette in the file | **Superseded by P4c:** the `.pen` now has a named dark palette — frame `KkPN3` "Dark Editorial" (`#18191A` canvas / `#2E3134` rail / `#343638` divider / `#F2EEE7` text). The former derived warm-dark (`#1F1D1A`) is retired. See [section M](#m-assumptions-taken-during-the-p4c-build-dark-mode--kkpn3) and [02](02-DESIGN-SYSTEM.md) | Resolved by design | 🔒 |
| 6 | **Wallpaper**: design shows solid `#EDE6D8` only | Default = flat warm background; custom wallpaper selection deferred (stub in P1) | — | 🔒 |
| 7 | **Weather data source** unspecified; mock shows `18° Soft light · 72% humidity` | P1 renders the designed static values; live weather provider deferred | Live data absent in MVP | 🔒 |
| 8 | **Status bar** content (`9:41 ◦ ◦ ▪`) is a mockup, not a real spec | Real status bar shown; its icon tint follows theme (F1, P3) | — | 🔒 |
| 9 | Icon set shows **branded names** (Gmail, Chrome…) mapped to generic lucide glyphs | We follow the *style* (monochrome line-art), not the brand — no colored logos, consistent stroke | — | 🔒 |
| 10 | Fonts: design uses **DM Sans** + **IBM Plex Mono** | Bundle DM Sans (all weights) and IBM Plex Mono in `res/font/`; both are open-source | — | 🔒 |

---

## B. Launcher / OS-behavior assumptions (P1 stubs)

| # | Area | Assumption / stub | Notes |
|---|---|---|---|
| 11 | Default-launcher switch | `HomeActivity` is a real HOME target; the in-app "Set default" button fires the system picker. We do **not** silently hijack. | Real in P1 |
| 12 | Live wallpaper engine | Not built; flat color background only | Stub, later priority |
| 13 | KWGT-style **system** widgets | Not built (needs AppWidgetHost). P1 widgets are in-app Compose only | Stub → P2 |
| 14 | Notification badges (B4) | Real per-app badge counts need `NotificationListenerService` + user permission | P1: cream dot shown when app has notifications (best-effort / permission-gated) |
| 15 | Double-tap → lock screen | Requires Device Admin / accessibility; offered best-effort, may be a no-op on some devices | Note in UI |

---

## C. Environment assumptions

| # | Item | Assumption |
|---|---|---|
| 16 | Machine has **no JDK / SDK / Studio / gradle / adb**; only git + winget | Full setup documented in [07](07-BUILD-AND-RUN.md) |
| 17 | **C: has only ~8.4 GB free**, D: has ~112 GB | Android SDK + AVD installed on **D:\Android\Sdk**; Gradle caches on D: |
| 18 | Windows 10/11 + PowerShell 5.1 | Commands written for PowerShell |

---

## D. Architectural assumptions

| # | Item | Assumption |
|---|---|---|
| 19 | Module split | Start single `:app` module + clean packages; split into Gradle modules incrementally to keep early builds fast |
| 20 | DI codegen | Hilt with **KSP** (not kapt) for speed |
| 21 | Icon glyphs | Bundle lucide-style vector drawables rather than depend on a third-party icon lib at runtime |

---

### How to change an assumption
1. Flip its Status (❓ → 🔁) and note the new decision here.
2. Log the change in [09-DECISIONS-LOG](09-DECISIONS-LOG.md).
3. Update the affected code/screens and [03-FEATURE-MAP](03-FEATURE-MAP.md).

---

## E. Assumptions taken during the P1 build (Session 2)

Recorded per the audit instruction: **the `.pen` is read-only**, so every gap below
was resolved as a documented assumption, not by editing the design file.

| # | Gap found in audit | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| 22 | Home mockup `HhwVC` shows only a **4x2 grid** of 8 tiles; the docs elsewhere cite a "5x6" Nova preset | Ship a **4-column, auto-height** grid (matches the mockup's proven column math + gap 18). `GridConfig.NovaPreset` (5x6, 110%, labels off) kept as a *preset value*, not the default. | `core/model/GridConfig.kt`, `feature/home/HomeScreen.kt` | 🔒 |
| 23 | **Page indicator** has no node | Active = short tall pill, inactive = small dot, centered above the mic pill (`PageIndicator`). Single page in P1. | `core/designsystem/.../HomeWidgets.kt` | 🔒 |
| 24 | **Notification badge** only exists as a spec card (`ha6OA`), not placed on the grid | `CreamBadgeDot`: 15% of tile, cream `#E8DFD0`, glow `#F3EBDD` blur 8, **no red / no count**. Wired behind a `showBadge` flag (off by default until the notification listener lands). | `core/designsystem/.../AppIconTile.kt` | ❓ |
| 25 | **No dark palette** in the file | Warm-dark palette already derived in `02` is used by `SoftHomeTheme(darkTheme = true)`. Not authoritative. | `core/designsystem/theme/Color.kt` | 🔒 |
| 26 | ~~**Icon pack drawable decode** not specified~~ **RESOLVED in P1.5** | P1 rendered the category glyph as a placeholder; **P1.5 decodes the real drawable** (zip / installed pack / assets) via `IconPackDrawableLoader` and renders it at the icon-set's 0.49 symbol ratio. The category glyph is now used **only** as the last-resort fallback. | `feature/iconpack/data/IconPackDrawableLoader.kt`, `feature/iconpack/ui/AppIcon.kt` | 🔒 |
| 27 | **Drawer tile sizing** vs the fixed 104 in a 4-col grid | Tile side = `min(cellWidth, 104dp)` so tiles stay square squircles on any width, matching `R0mbF` (104 r30 @ 30%). | `feature/appdrawer/AppDrawerScreen.kt` | 🔒 |
| 28 | Design specifies DM Sans + IBM Plex Mono, but no font files shipped | Bundled `dmsans_variable.ttf` + `ibmplexmono_regular.ttf` in `core/designsystem/res/font`. | `core/designsystem/theme/Type.kt` | 🔒 |
| 29 | Colored shadows (API 28+) | `Modifier.shadow(ambientColor, spotColor)` honours the design shadow colors on API 28+; on API 26-27 the platform draws its default shadow color. | `core/designsystem/theme/Elevation.kt` | 🔒 |
| 30 | Lucide rect glyphs with `rx` rounded corners | Converter emits sharp rects (rounding not carried over). Cosmetic only; revisit if a glyph looks off. | `core/designsystem/res/drawable/*.xml` | ❓ |

---

## F. Assumptions taken during the P1.5 build (real icon-pack decode)

Scope: replace the P1 placeholder glyph with a **real** icon-pack drawable decode,
add the zip-import + installed-pack-picker flows, and make the auto-mask render the
app's actual icon. The `.pen` is still read-only, so the gaps below are documented
assumptions (not design edits). All are grounded in the `lk7jo` icon-set spec:
charcoal `#2B2B2B` squircle, radius = 30% of tile, cream `#E8DFD0` symbol at
250/512 = **0.49** of the tile.

| # | Gap found in P1.5 | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| 31 | The `.pen` says nothing about **where** a pack's drawables live | Model the source explicitly as `IconPackSource`: `Zip` / `InstalledPack` / `Assets`; each decodes differently | `core/model/IconPack.kt`, `data/IconPackDrawableLoader.kt` | 🔒 |
| 32 | Design fixes the symbol at **250/512 = 0.49**; P1 used 0.42 for glyphs | Decoded pack drawables render at the **design's 0.49** (`ICON_SYMBOL_RATIO`); the 0.42 glyph ratio stays only for the fallback category symbol | `core/designsystem/atom/AppIconTile.kt` | 🔒 |
| 33 | Design does not specify how a **non-pack app** should look | Auto-mask: composite the app's **real** icon into the charcoal squircle at `0.62` inset, desaturated + tinted to cream so it reads as one family with decoded icons | `domain/AutoMask.kt`, `domain/IconCompositor.kt`, `domain/IconBitmapProvider.kt` | 🔒 |
| 34 | Design gives no import UI | Import lives in a **quiet ModalBottomSheet** on the drawer, reusing existing tokens (cream `#F6F0E7`, r24 cards, charcoal progress bar on cream track). No new visual language | `feature/iconpack/ui/IconPackImportSheet.kt` | ❓ |
| 35 | No spec for which icon-pack intents to query | Query the well-known set (ADW `org.adw.launcher.THEMES`, Apex, Nova `com.novalauncher.THEME`, Go, TeslaCoil, Lawnchair, Pixel launcher). Superset is harmless; unknown apps are ignored | `data/InstalledIconPackScanner.kt` | 🔒 |
| 36 | Persistence of the active pack across process death is unspecified | **P1.5 keeps the active pack in memory only** (`IconPackRepositoryImpl._activePack`), consistent with P1. Persisting the chosen pack + re-parsing on cold start is a settings-phase (P3) task | `data/IconPackRepositoryImpl.kt` | ❓ |
| 37 | Design is silent on **corrupt-pack** UX | Corrupt zip / missing drawable folder / unreadable icons -> `ImportProgress.Failed`, shown as a message in the sheet; the previous active pack is kept and the launcher falls back to auto-mask. **Never crashes** | `data/IconPackImporter.kt`, `ui/IconPackImportSheet.kt` | 🔒 |
| 38 | `.pen` shows only one density of pack art | Zip decode prefers the highest drawable bucket present (xxxhdpi -> xxhdpi -> xhdpi -> hdpi -> webp -> first) | `data/IconPackDrawableLoader.kt` | 🔒 |

**Notable bug found & fixed during P1.5 verification**

- The P1 `ComponentInfo{...}` regex in `AppFilterParser.normalizeComponent` threw
  `PatternSyntaxException` on-device for otherwise-valid input. Replaced with plain
  string parsing (no regex); a regression test now covers brace-heavy input.

---

## G. Audit note (Session 2 - docs vs `.pen` cross-check)

The rendered `.pen` (frames `bi8Au`, `lk7jo`, `IwpZU`) was read node-by-node and
cross-checked against `docs/00-..02`. **Result: the docs are accurate** - every
color, radius, shadow, size and node ID matched. Minor doc-only discrepancies:
- `docs/02` spacing list omits the home grid's vertical gap **14** (added as `Spacing.lg`).
- `docs/07` prose still says the toolchain is "not yet installed" - now stale (it is
  installed on D: as of Session 2).

> **Superseded 2026-09-24:** those frames no longer exist (see section H).

---

## H. Assumptions taken during the Warm Right Rail redesign (Session 5)

Scope: re-implement the home screen as a **row list + right rail** and redesign
the app drawer, per the updated `.pen` (`znb90` / `L7ZAp` / `TpzL1`). The `.pen`
is still read-only, so the gaps below are documented assumptions. Full spec:
[`superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md`](superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md).

| # | Gap found in redesign | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| 39 | `.pen` shows the music player UI but no playback/MediaSession spec | Ship the **static UI** (design's dummy values: "play music." / Djo / End of Beginning). Play button is a no-op. Real MediaSession deferred | `core/designsystem/atom/HomeRows.kt` (`MusicPlayerRow`) | ❓ |
| 40 | Storyboard names the states but not their triggers | **Tap the row** to enter its state (search → SEARCH, music → MUSIC); tap outside / Back → IDLE | `feature/home/HomeState.kt`, `HomeScreen.kt` | 🔒 |
| 41 | "rail expands" / "player rises" are described, not measured | Rail icons nudge inward on SEARCH (`RAIL_SLIDE` 220ms); the music row **grows in-place** by 16dp top+bottom on MUSIC (`MUSIC_RISE` 280ms); rows above stay put | `HomeScreen.kt` | 🔒 |
| 42 | Rail has 8 icons but the design gives no actions | Map to **default device apps** via `Intent`: circle-dot→browser, message-circle→messaging, send→email, camera→camera, wind→weather, panel-left→settings, phone→dialer; **sparkles→decorative (no-op)**. Unresolvable icons render but are no-op | `feature/home/RailShortcut.kt` | 🔒 |
| 43 | Drawer categories named (All/Communication/Entertainment/Tools) but no grouping rule | Map from `ApplicationInfo.category`: Social→Communication; Game/Audio/Video→Entertainment; Image/News/Maps/Productivity→Tools; null/undefined→All only | `core/model/DrawerCategory.kt` | 🔒 |
| 44 | `TpzL1` shows drawer tiles with **colored** glyphs | Keep the **icon-pack pipeline** (unchanged) - the colored glyphs are example pack art, not a required tint. Accent tints (`DrawerTint*`) exist only as an optional fallback | `feature/appdrawer/AppDrawerScreen.kt` | 🔒 |
| 45 | Rows in `znb90` are divided by 1px lines, not cards | Home rows use a horizontal `#D0C2B1` divider (not cream cards); background stays `#E8DFD0` | `core/designsystem/atom/HomeRows.kt` | 🔒 |
| 46 | `KkPN3` (Dark Editorial) is a second home mockup | Treated as a **dark reference only**; not implemented in this redesign. Dark mode still uses the derived palette (see #5) | `docs/00` | 🔒 |
| 47 | Swipe-up gesture previously consumed all pointer input | The swipe-up layer now sits **behind** the home content so rows/rail receive taps; empty space still opens the drawer | `app/launcher/HomeActivity.kt` | 🔒 |

**Bugs found & fixed during Session 5 verification**

- `HomeRightRail` applied a **negative** `padding(top = -6dp)` for the IDLE nudge
  → `IllegalArgumentException: Padding must be non-negative` at launch. Fixed by
  using `Modifier.offset(y = …)` instead.
- The full-screen swipe-up gesture box in `HomeActivity` sat **on top of** the
  home content and swallowed every tap. Moved it **behind** `HomeScreen`.

---

## I. Assumptions taken during the P2 build (widgets + folders)

Scope: P2 = custom widgets (E) + folder system (D), implemented **under the Warm
Right Rail design system** (not the retired P1 grid-squircle/card language).
Full spec: [`superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md`](superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md).

The `.pen` was **re-audited 2026-09-25**: it still contains **no** calendar /
battery / storage / quick-notes widget mock and **no** folder mock (the only
`calendar` / `folder` keyword hits are drawer app *tiles* / lucide icon names).
The `.pen` therefore stays **read-only**, and P2 visuals are derived from the
**existing Warm Right Rail patterns** (`HomeRow` / `HomeDivider` / `RowDisplay`
typography / Session-5 tokens).

### Locked decisions (user-confirmed)

| # | Decision | Detail |
|---|---|---|
| P2-1 | **Folders live in the App Drawer** (grid), not on the row-based home | Home gets **no** folder element (it stays info/status only); folders are purely an app-organization feature inside the still-grid drawer |
| P2-2 | **Quick notes = tap-to-expand row**, persisted in **DataStore** | Progressive disclosure consistent with Search/Music; notes are **user data** (must survive process death), unlike the static music row |
| P2-3 | **Battery/storage = real device values** | `BatteryManager` + `StatFs`, pure Android, no permission — not static design values |

### Documented assumptions

| # | Gap found in P2 | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| 48 | `.pen` has no widget/folder mock | Widgets are **additional `HomeRow`s** (flat, divider-separated), not cream cards; typed with Warm tokens | `core/designsystem/atom/HomeWidgetRows.kt` | 🔒 |
| 49 | Old P2 scaffolding used the retired card/grid language | **Removed** `SoftWidgets.kt` (`SoftWidgetCard`, card `CalendarWidget`/`BatteryWidget`/`NotesWidget`, `ThinProgressBar`, `FolderPreviewTile`); rebuilt for the row/folder-tile system | `core/designsystem/atom/HomeWidgetRows.kt`, `FolderTile.kt` | 🔒 |
| 50 | `docs/03` listed P2 nodes `u0a8RP`/`o0Fi0`/`nFk4u` — all from the **retired** `.pen` | Those node IDs no longer exist; P2 has **no** design nodes and is authored as assumptions | `docs/03` | 🔒 |
| 51 | No folder mock / no folder interaction spec | Drawer folder tile 68 r21 `#F5EFE6` + 2×2 mini-grid (`AppIcon`); tap → cream r24 popup with editable title + app grid | `core/designsystem/atom/FolderTile.kt`, `feature/appdrawer` | ❓ |
| 52 | No folder-fill gesture spec | P2 ships **"New folder" + assign-from-popup list**. Nova-style **drag-and-drop deferred to P3** | `feature/appdrawer` | 🔒 |
| 53 | Battery read can be unreliable on some OEMs | `BATTERY_PROPERTY_CAPACITY` first; fall back to the sticky `ACTION_BATTERY_CHANGED` `EXTRA_LEVEL`/`EXTRA_SCALE`; hide the value if both fail (never crash) | `core/data/…/DeviceStatusRepository` | 🔒 |
| 54 | `StatFs` target unspecified | Read `Environment.getDataDirectory()` (app-visible internal storage) for used/free; hide the line if `StatFs` throws | `core/data/…/DeviceStatusRepository` | ❓ |
| 55 | Battery/storage refresh cadence unspecified | Read once on composition + re-read on `ON_RESUME`; a live `BatteryManager` callback stream is deferred to P3 | `feature/home/HomeViewModel.kt` | 🔒 |
| 56 | Notes commit timing unspecified | Commit on IME-done / focus-loss / row collapse; value also held in VM state so a collapse never loses an edit | `feature/home`, `core/data/…/NotesRepository` | 🔒 |
| 57 | Corrupt folders JSON in DataStore | Parse failure → empty folder list, previous file kept, **no crash** | `core/data/…/FolderRepository` | 🔒 |
| 58 | Folder + drawer filter (search/category) interplay | A folder cell is shown when **≥1** of its apps passes the current filter; inside an open folder the search applies to the folder's apps | `feature/appdrawer` | ❓ |
| 59 | Expanded notes editor overflows the screen bottom (P2 verification finding) | The home column is scrollable **only while NOTES is expanded** (`Modifier.verticalScroll` gated on `isNotesOpen`); at rest the list fits, so the swipe-up-to-drawer layer behind the content still receives drags. Home top/bottom padding trimmed (24→24 top, 24→8 bottom) so the collapsed notes preview fits | `feature/home/HomeScreen.kt` | 🔒 |

### P2 verification evidence (emulator, Android 15 / API 35, `soft_home_pixel`)

Real screenshots in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Home IDLE with calendar + battery/storage + notes rows | `p2-home-idle.png` |
| Folder popup (empty) | `p2-folder-empty.png` |
| Folder filled (apps added live) | `p2-folder-filled.png` |
| Folder tile in the drawer (2×2 preview, members removed from grid) | `p2-folder-tile.png` |
| **Folder persisted after `am force-stop`** | `p2-folder-persisted.png` |
| Notes expanded editor | `p2-notes-editor.png` |
| Notes typed | `p2-notes-typed.png` |
| **Notes persisted after `am force-stop`** | `p2-notes-persisted.png` |

- **Battery/storage vs device:** row showed **100%** (matches `dumpsys battery`
  `level: 100`) and **Storage 19% · 4.6 GB free** (matches `df /data` ≈ 1060104/6082144
  used = ~17.4%→ceil 19%, `Available 4879828 KB` ≈ 4.65 GB).
- **Notes persistence:** after typing, `soft_home_notes.preferences_pb` contained
  `quick_notes → "y milk and call mom"`; after `am force-stop` + relaunch the home
  preview showed the same text.
- **Folders persistence:** after create + add apps, `soft_home_folders.preferences_pb`
  existed (333 B) and the tile survived a force-stop + relaunch.
- Unit tests: **124 pass** (was 87).
### Deferred (recorded, NOT deleted from scope)

| Item | Status | Reason |
|---|---|---|
| **MediaSession / real playback** (#39) | ⛔ **Deferred** (was ❓ in section H) | No playback spec in the `.pen`; the music row stays static UI. Revisit later. |
| **KkPN3 dark editorial mode** (#46) | ⛔ **Deferred** | Remains a visual reference only; not executed. Reconsider after P2. |
| **Live calendar events (`CalendarContract`)** | ⛔ **Deferred** | Same class of problem as weather: needs `READ_CALENDAR` permission + edge cases (permission denied, no calendar app, multiple calendars). The calendar row renders static/empty in P2. |
| **Drag-and-drop app → folder** | ⛔ **Deferred to P3** | Gesture-handling subsystem, out of P2 scope (see #52). |
| **Battery change callback stream** | ⛔ **Deferred to P3** | P2 re-reads on resume (see #55). |

---

## K. Assumptions taken during the P4a build (drag & drop)

Scope: P4a = **drag-and-drop** (home-row reorder + drawer app→folder), one gesture
subsystem. Full spec:
[`superpowers/specs/2026-09-25-p4a-drag-and-drop-design.md`](superpowers/specs/2026-09-25-p4a-drag-and-drop-design.md).

The `.pen` was **re-audited 2026-09-25**: it contains **no** drag/drop/reorder/handle
node (keyword sweep: `drag`/`drop`/`reorder`/`handle`/`grip` = 0 hits). The `.pen` stays
**read-only** (byte-identical after the build), and P4a visuals derive from the existing
Warm Right Rail vocabulary.

### Locked decisions (user-confirmed)

| # | Decision | Detail |
|---|---|---|
| P4a-1 | One gesture subsystem, two targets | Row reorder + app→folder share the engine; only the drop adapters differ (spec §2). |
| P4a-2 | Drag **complements** the up/down buttons | Both stay (D-031 keeps the buttons). |
| P4a-3 | Membership/order via existing repos | Drag calls the same `setHomeRows` / `FolderLogic` paths. |
| P4a-6 | Drag trigger = long-press-lift (haptic) | No persistent drag handle. |
| P4a-7 | Row reorder drops anywhere vertically | Live insertion line; no empty slots. |
| P4a-8 | App→folder: folder tile **or** "New folder" chip | Dropping elsewhere = snap back. |

### Documented assumptions

| # | Gap found in P4a | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| K1 | `.pen` has no drag/drop mock | Long-press-lift (haptic), floating preview (~1.05 scale, soft shadow), 3px accent insertion line / accent hover ring. Built from Warm tokens (`Dimens.drag*`, `MotionTokens.drag*`). | `core/designsystem/atom/DragAndDrop.kt` | 🔒 |
| K2 | No drag-handle glyph | No persistent handle; the whole row/tile is the drag source after long-press. | `DragAndDrop.kt` | 🔒 |
| K3 | Long-press already opens a menu (P3) | One gesture owner: no-move → menu, move → drag (D-036). | `AppDrawerScreen.AppCell`, `DragAndDrop.dragSource` | 🔒 |
| K4 | Row preview shape | Compact **label chip** (cream pill), not the full-height row. | `DragRowChip` | 🔒 |
| K5 | Drag-out of a folder | **Descoped** from P4a (the popup card's clickable + the scrim click fight the drag; low value). Removing a member stays tap-to-remove in the popup. `FolderDropResolver.removeFrom` + `AppDrawerViewModel.moveOutOfFolder` are kept + unit-tested for a future phase. | `AppDrawerScreen.FolderPopup` | 🔁 |
| K6 | Drop-to-create folder | Dropping an app on the "New folder" header chip creates a folder containing it (P4a-8). | `AppDrawerViewModel.createFolderWith` | 🔒 |
| K7 | Swipe-up-to-drawer vs. row drag | A swipe **starting on a row** is now owned by the row's gesture (tap/long-press); swipe-up-to-drawer works from **empty space** (unchanged from A-#47). | `HomeScreen`, `HomeActivity` | 🔒 |

### P4a verification evidence (emulator, Android 15 / API 35, `soft_home_pixel`)

Real screenshots in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Home IDLE (baseline) | `p4a-home-idle.png` |
| **Row reorder mid-drag** (drag chip + accent insertion line at top + dimmed source) | `p4a-row-mid-drag.png` |
| Row reorder settled (Weather moved up) | `p4a-row-drag-result.png` |
| **Row order survives `am force-stop`** | `p4a-row-persist.png` |
| Drawer (all apps) | `p4a-drawer.png` |
| **App→folder mid-drag** (icon preview + "New folder" chip hovered) | `p4a-app-middrag.png` |
| Drop-to-create folder (Calendar in a new folder) | `p4a-drop-created-folder.png` |
| App→existing folder (2 members in the 2×2 preview) | `p4a-app-to-folder.png` |
| **Folder membership survives `am force-stop`** | `p4a-folder-persist.png` |
| Folder popup (tap-to-open, members + add-app) | `p4a-folder-popup.png` |
| **Long-press menu still works** (Open/App Info/Edit Icon/Remove/Uninstall greyed/Shortcuts) | `p4a-longpress-menu.png` |
| Final drawer state (folder with 2 apps) | `p4a-final-drawer.png` |

- **Tests:** **307 JVM unit tests** (was 176; +18 new: `DragAndDropStateTest` 5 ,
  `DragDropResolverTest` 13) and **27 instrumented tests** (was 20; +2:`HomeRowDragTest`).
  The 7 icon-pipeline tests stayed green.
- **Row reorder:** drag Weather up → order becomes Time→Weather→Date; survives force-stop.
- **App→folder:** drag an app onto the "New folder" chip → folder created with it; drag
  onto an existing folder → member added (2×2 preview shows both); survives force-stop;
  the source app leaves the root grid.
- **Long-press split:** a stationary long-press opens the context menu; a long-press then
  drag reorders / assigns; a quick tap launches.

### P4a descopes / deferrals

| Item | Status | Reason |
|---|---|---|
| **Drag-out of a folder** (P4a-9) | 🔁 **Descoped** | Popup card `clickable` + scrim click fight the drag gesture; low value (tap-to-remove exists). Resolution kept + tested (K5). |
| Drag reorder of **drawer grid** order (not just into folders) | ⛔ Deferred | Not requested; folders-first ordering is derived, not user-ordered. |

---

## J. Assumptions taken during the P3 build (System UI + Settings)

Scope: P3 = System UI (F) + Settings (G), built on the Warm Right Rail design system
(Session 5) and the P2 widgets/folders layer (Session 6). Full spec:
[`superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md`](superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md).

The `.pen` was **re-audited 2026-09-25**: it still contains **no** status-bar
treatment, nav-bar handling, long-press menu, settings panel, or toggle mock. The five
P3 node IDs previously cited in `docs/03` (`ogMkZ`, `Fzobx`/`pu2gg`, `nFk4u`, `UPa9N`)
are from the **retired P1 file** and do **not** exist. The `.pen` stays **read-only**
(verified byte-identical after the P3 build), and P3 visuals derive from the existing
Warm Right Rail vocabulary.

### Locked decisions (user-confirmed)

| # | Decision | Detail |
|---|---|---|
| P3-1 | Locked home rows = **Time, Date, Weather** | The other rows (Search/Music/Calendar/Battery-Storage/Notes) are user-toggleable; the home never renders empty |
| P3-2 | Reorder = **up/down buttons** now | Drag-and-drop reorder **deferred to P4** (with the P2-deferred app?folder drag) |
| P3-3 | Long-press targets = **drawer tiles + rail icons** | Row-style menu, not the platform `PopupMenu` |
| P3-4 | Uninstall of system apps | Row shown **greyed/disabled**; App Info always available; never attempt a throwing uninstall |
| P3-5 | Active icon pack **persists + rehydrates** | Closes #36; falls back to auto-mask on a missing/corrupt pack |
| P3-6 | Wallpaper = **flat default + system picker** | Live-wallpaper **engine** stays deferred (#12) |

### Resolved questions (Q1�Q5)

| # | Question | Decision |
|---|---|---|
| Q1 | Settings entry point | Rail `panel-left` icon opens **our** panel; a "System settings" row opens the OS screen |
| Q2 | "Remove" semantics | **Hide from drawer** (reversible `hiddenApps` set + "Hidden apps" manager) |
| Q3 | Spacing values | **Multiplier** Compact �0.88 / Normal �1.0 / Roomy �1.12 |
| Q4 | Edit Icon depth | Pick from the active pack's drawables (rich editor ? P4) |
| Q5 | Move-buttons placement | **Inline** on each Widgets row (?/? next to the toggle) |

### Documented assumptions

| # | Gap found in P3 | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| 60 | `.pen` has no status-bar spec | Transparent bars; icon color requested from the app theme (`isAppearanceLight*Bars`) | `app/.../SystemBarAppearance.kt` | ?? |
| 61 | `.pen` has no nav-bar handling spec | Hide (transient) under gesture nav (`Settings.Secure navigation_mode == 2`); minimal transparent bar otherwise; degrade to "show" on failure | `app/.../SystemBarAppearance.kt` | ?? |
| 62 | No long-press menu mock | Row-style cream r24 `AppContextMenu` (Open/App Info/Edit Icon/Remove/Uninstall/Shortcuts); anchored to a centered card in P3 | `core/designsystem/atom/AppContextMenu.kt`, rail + drawer | ?? |
| 63 | Uninstall availability varies | Greyed when `isSystem` or not removable; never throws; App Info always present | `core:model/AppActionLogic`, `AppActionsRepository` | ?? |
| 64 | No settings-panel mock | Sectioned panel built from `SettingsRow` + `SoftToggle`; lives in `:app` (thin shell, no new Gradle module) | `app/.../settings/SettingsPanel.kt` | ?? |
| 65 | No custom-toggle mock | `SoftToggle` 42�22 pill from tokens; reuses `railMotion` (220ms) for the knob | `core/designsystem/atom/SoftToggle.kt` | ?? |
| 66 | No row reorder gesture spec | Up/down buttons (P3-2); drag deferred to P4 | `SettingsRow` reorder buttons | ?? |
| 67 | "Grid size"/"spacing" vs the row-based home | Applied to the **drawer** grid (the only grid surface) + row vertical rhythm; home row layout unchanged | `SettingsPanel` Appearance section | ?? |
| 68 | Active icon pack lost on restart (#36) | Persist `activeIconPackId`; rehydrate on cold start (zip ? installed ? newest-zip fallback); clear a dead id | `feature/iconpack/.../IconPackRepositoryImpl.kt` | ?? |
| 69 | Wallpaper scope | Flat warm default + system picker (`ACTION_CHANGE_LIVE_WALLPAPER` ? `ACTION_SET_WALLPAPER`); engine deferred | `app/.../WallpaperIntents.kt` | ?? |

### P3 verification evidence (emulator, Android 15 / API 35, `soft_home_pixel`)

Real screenshots in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Home Idle (status icons dark on cream � F1 light) | `p3-home-idle.png` |
| Home in Dark theme (status icons light on warm-dark � F1) | `p3-home-dark.png` |
| "Quick notes" toggled off ? row gone; survives force-stop | `p3-home-notes-hidden.png` |
| Settings: Appearance (theme/grid/spacing/System settings/Hidden apps) | `p3-settings-appearance.png` |
| Settings: Widgets (locked rows non-toggle, inline ?/?) + Wallpaper + Gestures | `p3-settings-widgets-wallpaper-gestures.png` |
| Rail long-press ? row-style menu | `p3-rail-longpress-menu.png` |
| Drawer long-press ? menu; **Uninstall greyed** for the system "Settings" app | `p3-drawer-longpress-menu.png` |

- **Tests:** 160 JVM unit tests pass; 20 instrumented Compose tests pass on the AVD.
- **F1:** status-bar icons are dark on cream and light on warm-dark (screenshots).
- **F2:** nav bar hidden under gesture navigation (edge-to-edge home).
- **F3:** rail + drawer long-press open the row-style menu; Uninstall is greyed for a
  system app (P3-4).
- **G:** the four settings sections render; toggling "Quick notes" off hides the home
  row and the choice survives `am force-stop`; Clock/Date/Weather show "Always shown"
  with a disabled toggle.

### Deferred (recorded, NOT deleted from scope)

| Item | Status | Reason |
|---|---|---|
| **Drag-and-drop row reorder** | ? **Deferred to P4** | Gesture subsystem (P3-2 ships up/down buttons). |
| **Drag-and-drop app ? folder** | ? **Deferred to P4** | Carried from P2. |
| **Rich icon editor** (upload/crop custom) | ? **Deferred to P4** | P3 ships pick-from-pack (Q4). |
| **Live-wallpaper engine** | ? **Deferred** | Flat default + system picker only (P3-6). |
| **Backup & restore settings (G4)** | ? **Deferred to P4** | Out of P3 scope. |
| **Real gesture actions** (swipe-down / double-tap) | ? **Deferred** | Needs notification access / Device Admin (@15); settings rows show "Coming soon". |
| **MediaSession (#39)**, **KkPN3 dark editorial**, **live calendar** | ? **Deferred** (still) | Carried from P2. |

---

## L. Assumptions taken during the P4b build (Edit Icon — rich editor)

Scope: P4b = the "Edit Icon" editor over the P3.5 hybrid renderer. Full spec:
[`superpowers/specs/2026-09-25-p4b-edit-icon-rich-editor-design.md`](superpowers/specs/2026-09-25-p4b-edit-icon-rich-editor-design.md).

The `.pen` was **re-audited 2026-09-25**: byte-identical to the P3.5/P4a audits (176,128
bytes, SHA256 `08DB2A81…CA6201`, mtime 2026-09-24 11:58). It contains **no** editor mock
(keyword sweep for `picker`/`swatch`/`crop`/`resize`/`upload`/`icon editor` = 0). The
`.pen` stays **read-only**; P4b is authored from the Warm Right Rail vocabulary.

### Key finding (why P4b is not "just wiring a picker")

P3's "pick-from-pack" **did not exist**: `iconOverrides` was `Map<componentKey, packId>`,
which the resolver ignored except to check `== activePack.id`, then re-derived the pack's
**default** drawable (a visual no-op). The "Edit Icon" row was `onEditIcon =
viewModel::closeMenu` (no-op) and `PrefsRepository` had no override setter. P4b builds
the **typed override data path + the editor UI**.

### Documented assumptions

| # | Gap found in P4b | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| L1 | `.pen` has no editor mock | Cream r24 editor card over a dim scrim (the `FolderPopupBody`/`AppContextMenu` vocabulary), not a platform dialog/sheet | `core/designsystem/atom/IconEditorScaffold.kt` | 🔒 |
| L2 | No picker mock | Drawable/glyph grids of 48 r14 `ChoiceTile`s; 7 color swatches from the P3.5 `TpzL1` palette | `IconEditorScaffold.kt` | 🔒 |
| L3 | "Pick from pack" ambiguity | The picker offers the pack's **mapped distinct drawables** (`entries.values`), not raw zip/APK drawables (deferred, D-038) | `AppDrawerViewModel.iconEditorState` | 🔁 |
| L4 | Non-pack edit depth | A non-pack app may choose a **glyph + color**; no crop/upload (D-038) | `IconEditor`/`IconEditorSheet` | 🔒 |
| L5 | Reset semantics | Reset = remove the override → derived (pack/category) rendering; no confirmation (reversible) | `AppDrawerViewModel.resetIconOverride` | 🔒 |
| L6 | Override scope | One override per app, applied wherever the shared `IconResolver` renders it (drawer + folder previews + home rail) | `IconResolver` | 🔒 |
| L7 | Preview fidelity | The editor preview uses the **same** `DrawerIconTile` composition as the grid, so it cannot drift | `IconEditorSheet.EditorPreview` | 🔒 |

### P4b verification evidence (emulator, Android 15 / API 35, `soft_home_pixel`)

Real captures in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Editor, **pack mode** (tabs + "Choose from the active pack" grid + gated Save) | `p4b-editor-pack.png` |
| Editor, **glyph mode** (glyph grid + selected ring + 7 color swatches) | `p4b-editor-glyph.png` |
| **Override applied in the real drawer** (Calendar = chosen HeartHandshake glyph in Travel color) | `p4b-override-applied.png` |
| **Override survives `am force-stop`** (same tile after full process death + relaunch) | `p4b-override-persist.png` |
| Baseline drawer (P3.5 colored glyphs, pre-edit) | `p4b-drawer.png` |

- **Tests:** **217 JVM unit** (`IconOverridesCodecTest` 10, `IconEditorTest` 10,
  `IconResolverTest` 8→10) and **33 instrumented** (`IconEditorSheetTest` 2,
  `IconEditorScreenshotTest` 2, `IconOverrideEndToEndTest` 2). The 7 icon-pipeline tests
  stayed green.
- **End-to-end:** a saved override round-trips through the **real** Android DataStore and
  drives the **real** `IconResolver` (proven on-device by `IconOverrideEndToEndTest`).
- **Honesty note:** the pack-mode editor screenshot uses a synthetic pack whose drawables
  do not exist on disk, so its tiles/preview render blank — the *layout, tabs, selection
  ring, and Save gating* are what that screenshot proves. The drawable **decode inside the
  editor** uses the identical `IconPackDrawableLoader` + `IconCompositor` path already
  verified since P1.5/P3.5.

### P4b out of scope (recorded, NOT silently dropped)

| Item | Status | Reason |
|---|---|---|
| **Crop / resize / upload a custom image** | ⛔ **Out of P4b** | No design source; a real subsystem (SAF/decode/downscale/adaptive-icon safety/storage). D-038. |
| Raw pack drawable enumeration (`listDrawables`) | ⛔ **Deferred** | The mapped set is the pack's curated repertoire; enumeration is new infra. |
| Editing rail-icon **shortcut targets** | ⛔ **Out** | That is the "Edit Shortcut" row, unrelated to icon artwork. |

---

## M. Assumptions taken during the P4c build (Dark Mode — KkPN3)

Scope: P4c = render the `KkPN3` "Dark Editorial" named palette faithfully, replacing the
derived warm-dark guess (old assumption #5), and audit every surface under dark.
Design source: `design/homeApp.pen` frame **`KkPN3` "Home Screen Mockup — Dark Editorial"**.
Full context: the [P4 scope spec §0](superpowers/specs/2026-09-25-p4-scope-and-phase-split-design.md).

The `.pen` was **re-audited 2026-09-25**: byte-identical to the P3.5/P4a/P4b audits
(176,128 bytes, SHA256 `08DB2A81…CA6201`). The `.pen` stays **read-only**.

### `KkPN3` palette (authoritative, extracted node-by-node)

| Element | Value |
|---|---|
| Screen bg | `#18191A` |
| Dark rail | `#2E3134` |
| Divider | `#343638` |
| Rail icon / time / date number / music title / search icon / music controls / progress fill / signature | `#F2EEE7` |
| Soft text (weather label / signature-level) | `#E2DDD5` |
| Date day | `#D2CBC1` |
| Date month / music track | `#918F8B` / `#817F7B` |
| Search placeholder | `#A8A29A` |
| Music artist | `#C8C0B6` |
| Album art / album mark | `#E3DED6` / `#454648` |
| Progress track | `#676866` |
| Search idle dots | `#777873` |

### Documented assumptions

| # | Gap found in P4c | Assumption taken (as built) | Where reflected | Status |
|---|---|---|---|---|
| M1 | `KkPN3` shows no card/menu/popup/drawer-card surface | Derived from the same neutral family: card `#24262A`, cardAlt `#2A2C30`, drawer bg `#1F2124`, drawer stroke `#3E4145` | `Color.kt` (dark block) | 🔁 |
| M2 | No dark **drawer icon** palette in the `.pen` | P3.5's dark drawer adaptations kept, aligned to the neutral tile `#2E3134` (glyph colors remain legible) | `Color.kt` (`DarkDrawer*`) | 🔁 |
| M3 | `KkPN3` has no accent color (it is monochrome-warm) | Kept a warm accent (`#C98B6E`) for interactive emphasis (links, active tab, toggle), legible on `#18191A` | `Color.kt` (`DarkAccent`) | 🔁 |
| M4 | Settings panel behaviour under dark was unspecified | `SettingsStubActivity` now resolves `ThemeMode` and matches its status/nav bars (was hardcoded light) | `app/.../SettingsStubActivity.kt` | 🔒 |

### P4c verification evidence (emulator, Android 15 / API 35, `soft_home_pixel`)

Real screenshots in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Home, **light** (baseline) | `p4c-home-light.png` |
| Home, **dark** (`KkPN3`: `#18191A` canvas, `#2E3134` rail, `#F2EEE7` type, warm accent) | `p4c-home-dark.png` |
| Drawer, **dark** (dark tiles + legible category-colored glyphs) | `p4c-drawer-dark.png` |
| Settings, **dark** (rows on `#24262A`; Theme row reads **"Dark"** — proves the fix) | `p4c-settings-dark.png` |

- **Tests:** **220 JVM unit** (was 217; +3 `DarkPaletteTest`) and **36 instrumented** (was
  33; +3 `DarkThemeTest`). Both `soft_home_pixel` (API 35) and a physical device ran the
  instrumented suite green.
- **Palette lock:** `DarkPaletteTest` asserts the verbatim `KkPN3` hexes so dark mode
  cannot drift from the design again.
- **Bug fixed:** `SettingsStubActivity` previously forced `darkTheme = false` for the bars
  while the panel followed the system — a dark panel with a mismatched status bar. Now
  both follow `ThemeMode`.

---

## N. Assumptions taken during the P4d build (Backup & Restore)

Scope: P4d = export/import the whole launcher user state as one file, from a settings
section (G4). It is the final P4 sub-phase.
Design source: `design/homeApp.pen`. The `.pen` was **re-audited 2026-09-25**:
byte-identical to every audit since P3.5 (176,128 bytes, SHA256
`08DB2A81...CA6201`); keyword sweep `backup`/`restore`/`export`/`import`/`cloud` = **0**.
The `.pen` stays **read-only**.

### Key finding (why P4d is "one codec + one apply", not new infra)

Unlike P4b (which had to build a data path from scratch), every persisted field already
has a typed model, a pure codec (`HomeRowsCodec`, `IconOverridesCodec`, `FoldersCodec`)
and a typed repository setter. So P4d only had to (a) serialize the union of those values
and (b) apply them back. The one gap closed: a single atomic `PrefsRepository.applyAll`
(one DataStore write) so a restore is not N separate edits.

### Documented assumptions

| # | Gap | Assumption taken (as built) | Status |
|---|---|---|---|
| N1 | `.pen` has no backup mock | A "Backup & restore" `SettingsRow` section appended to the P3 settings panel | OK |
| N2 | No format spec | One JSON document `{app,version,exportedAt,prefs,folders,notes}` (D-040) | OK |
| N3 | No transport spec | SAF `CreateDocument` / `OpenDocument` (MIME `application/json`), like the P1.5 zip flow | OK |
| N4 | Restore UX unspecified | Destructive, behind an inline confirm; result shown as a tappable message row | OK |
| N5 | Icon-pack bytes | Backed up as an **id reference** only; rehydrated via the P3-5 cold-start path | OK |
| N6 | Foreign/corrupt files | Guarded by the `app` marker + `version`; failure leaves state untouched (total decode) | OK |

### P4d verification evidence (emulator API 35 + real device TECNO CN7c / Android 16)

Real screenshots in `docs/screenshots/`:

| Evidence | File |
|---|---|
| Settings shows the "BACKUP & RESTORE" section (real device, dark) | `p4d-settings-backup-real.png` |
| Settings shows the section with Export/Import rows (emulator, light) | `p4d-settings-backup-emulator.png` |
| SAF `CreateDocument` picker with the suggested name `softhome-backup-<date>.json` | `p4d-saf-save-picker.png` |
| "Backup saved." message after export | `p4d-backup-saved-emulator.png` |
| The exported file itself (421 B, well-formed) | `p4d-backup-file-example.json` |
| Home, light mode (real device) | `p4d-home-light-real.png` |

- **Tests:** **236 JVM unit** (was 220; +16: `BackupCodecTest` 11, `BackupRepositoryTest`
  3, `PrefsRepositoryTest` +2 `applyAll`) and **38 instrumented** (was 36; +2
  `BackupRestoreEndToEndTest`, `BackupSettingsScreenshotTest`). Both `soft_home_pixel`
  (API 35) and the physical device TECNO CN7c (Android 16 / API 36) ran the instrumented
  suite green.
- **Round-trip:** `BackupRestoreEndToEndTest` exports to a real file, wipes every store,
  restores, and asserts every field came back -- against the real DataStore on both
  devices.
- **Bug found & fixed during verification:** the export first wrote a **0-byte** file,
  because the SAF stream was opened with `contentResolver.openOutputStream(uri)?.use {}`
  at the *call site* while `exportBackup` launched a coroutine and returned immediately --
  so the stream closed before the async write. Fixed by passing the `ContentResolver` +
  `Uri` into the ViewModel and opening/using/closing the stream **inside** the coroutine
  (`BackupRepository.export` also stopped closing the caller's stream). Verified: the file
  is now 421 B and well-formed.

### P4d out of scope (recorded, NOT silently dropped)

- Backing up the icon-pack **bytes** / installed pack APKs (id reference only).
- Any cloud / account / auto-sync, scheduled backups.
- Per-section (partial) backup -- it is the whole state or nothing.
