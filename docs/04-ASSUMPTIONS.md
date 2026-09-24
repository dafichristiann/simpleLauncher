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
| 5 | **Dark mode** requested, but no dark palette in the file | Derived **warm dark** palette (charcoal family): bg `#1F1D1A`, card `#2E2A25`, cream tile inverted. See [02](02-DESIGN-SYSTEM.md) | Dark tone may need tuning | 🔒 |
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
