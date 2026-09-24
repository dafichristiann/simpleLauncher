# 05 - Progress

> Living changelog. Updated at **every checkpoint** (start, end of each build step
> group, and at each priority boundary). Newest entry on top.

**Overall:** P1.5 decode works; Session 4 corrected an over-claimed verification and
hardened glyph uniqueness (D-016). **Session 5 redesigned the home + drawer into the
"Warm Right Rail" system.** **Session 6 delivered P2: custom widgets (calendar /
battery-storage / quick notes) + the folder system, under the Warm Right Rail design
system.** **Session 8 fixed the drawer BACK regression.** **Session 9 delivered P3.5:
the drawer icon color system (cream tiles + per-category colored glyphs + amber
selected state), rendering the `TpzL1` frame faithfully.** Stopped for review.

---

## Session log

### Session 9 - P3.5 Drawer Icon Redesign (color + labels) — DONE (2026-09-25)

**Why:** the `TpzL1` frame "Warm App Drawer — Unique Icon Grid" specifies cream tiles
with per-category colored glyphs and labels, but P1–P3 rendered the drawer
monochrome (charcoal squircle + cream). P3.5 renders the frame faithfully, as a
**separate phase before P4** so P4's Edit Icon editor builds on the final model.

**Spec:** [`superpowers/specs/2026-09-24-p35-drawer-icon-redesign-design.md`](superpowers/specs/2026-09-24-p35-drawer-icon-redesign-design.md).

**Re-audit:** the `.pen` was edited by the user after the spec was drafted (file grew
to 176KB). Re-audited before Step 0: `TpzL1` is byte-identical (same tiles, labels,
color variables). A **new frame `ciHU3` "Warm Right Rail — Icon Language Library"**
(144 monochrome glyphs, 8 groups) was added — confirmed **out of scope** (rail asset
library, not a drawer color spec). No tokens invented.

**What changed:**
- `Color.kt` + `SoftColors`: 10 new drawer color fields (light + dark), verbatim from
  `TpzL1`'s 7 icon variables + `tile-cream`/`selected-tile`/`selected-icon`. Removed
  the placeholder `DrawerTintClay/Amber/Pine/Sage` (dead after the port).
- `DrawerIconColor` (pure, `feature:iconpack/domain`): category→color-token mapper
  with the communication-glyph heuristic.
- `DrawerIconTile` (designsystem atom): color-parameterized tile (background + optional
  painter tint + symbol tint). The monochrome `AppIconTile` path is unchanged.
- `DrawerAppIcon` (iconpack UI): the drawer icon composable — pack apps render
  untinted real artwork; non-pack apps render a category-colored glyph; selected
  tiles use the amber background.
- `AppDrawerScreen`: `AppCell`/`FolderMiniGrid`/folder popup switched from `AppIcon`
  to `DrawerAppIcon`; folder members are `selected = true`. Dead `bitmapProvider`
  parameter removed from the drawer render chain.

**Tests:** 176 unit (was 161; +15 new), 25 instrumented (was 22; +3 new). The 7
existing icon-pipeline tests (AutoMask, IconResolver, IconMasker, GlyphUniqueness,
Importer, Persistence, AppFilterParser) stayed green throughout.

**Evidence:** `docs/screenshots/p35-drawer-all.png` (cream tiles + colored glyphs,
no charcoal remaining), `p35-folder-amber-selected.png` (amber selected state),
`p35-back-still-works.png` (Session 8 BACK fix intact).

**Decisions:** D-032 (drawer color system), D-033 (pack-vs-glyph hybrid).

### Session 8 - Patch: drawer BACK regression fix (2026-09-25)

**Why:** A small regression from before P3 -- with the app drawer open, the system
BACK button did nothing (the drawer stayed open) instead of closing it and returning
to home. Fixed as an isolated patch ahead of P3.5, per user instruction.

**Root cause:** `HomeActivity` overrode `onBackPressed()` with an empty body (to stop
a launcher exiting on Back) **without delegating to `super`**. That bypasses the
Activity's `OnBackPressedDispatcher`, so every `BackHandler` in the Compose tree was
dead -- including the drawer's. Compounding it, `AppDrawerScreen` had no top-level
`BackHandler` at all (only the folder popup had one).

**Fix:**
- `HomeActivity`: removed the `onBackPressed()` override. Added a last-resort
  `BackHandler` in `LauncherRoot` (registered outermost) so a Back press still never
  exits the launcher, while the drawer/home handlers take precedence when active.
- `AppDrawerScreen`: added a top-level `BackHandler` and an explicit `onClose` param.
  It pops the drawer's layer stack in order: long-press menu -> folder popup -> close
  drawer. Removed the now-redundant inner `BackHandler` in `FolderPopup`.
- `HomeActivity` passes `onClose = { drawerOpen = false }`.

**Tests / verification:**
- `DrawerBackHandlerTest` (instrumented, 2 cases): renders the drawer overlay through
  the real back dispatcher and asserts BACK closes it (`Espresso.pressBack`).
- `HomeActivityBackContractTest` (Robolectric): asserts `HomeActivity` does not
  declare `onBackPressed()` -- locks the root cause in.
- Full suites green: **161 unit tests**, **22 instrumented tests**.
- On-device before/after: `docs/screenshots/backfix-BEFORE-back-stuck.png` (BACK left
  the drawer open) vs `backfix-AFTER-back-home.png` (BACK returns to home);
  `backfix-drawer-open-context.png` shows the open drawer.

### Session 7 - P3 System UI + Settings — DONE (2026-09-25)

**Why:** P3 = System UI (F) + Settings (G), built on the Warm Right Rail design system
(not the retired P1 card/grid language). Spec:
[`superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md`](superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md).
First session under **git** (repo initialised; baseline commit e00a70b, then one commit
per phase).

**Audit result (Session 7)**
- Re-audited `.pen`: still **no** status/nav/long-press/settings/toggle nodes; the P3
  node IDs in `docs/03` (`ogMkZ`, `Fzobx`/`pu2gg`, `nFk4u`, `UPa9N`) are stale. `.pen`
  stays read-only (verified byte-identical after the build).
- Reused **live** scaffolding: `themes.xml` (transparent bars), `SettingsStubActivity`
  shell, `PrefsRepository` (DataStore), the `FolderPopup` row-card pattern.

**Locked decisions**
- **P3-1** locked rows Time/Date/Weather; **P3-2** up/down buttons (drag → P4);
  **P3-3** long-press on rail + drawer tiles; **P3-4** Uninstall greyed for system apps;
  **P3-5** icon pack persists + rehydrates (closes #36); **P3-6** flat wallpaper +
  system picker. Q1–Q5 resolved (settings entry via rail; Remove = hide-from-drawer;
  spacing multiplier; pick-from-pack icon; inline move buttons).

**Done (phases 0-8)**
- **Phase 0:** `HomeRowKind`/`HomeRowPref`/`HomeRowLogic`; `LauncherPrefs` + `homeRows`,
  `spacing`, `hiddenApps`; `SpacingScale`; P3 color/dimens tokens; glyphs info/trash_2/
  chevron_right/arrow_up/arrow_down/x/external_link.
- **Phase 1:** `PrefsRepository` persists rows/spacing/hidden-apps (`HomeRowsCodec`);
  `AppActionsRepository` (+ pure `AppIntentFactory`) and `AppActionLogic`.
- **Phase 2:** atoms `SoftToggle`, `SettingsRow`, `AppContextMenu`.
- **Phase 3:** `IconPackRepositoryImpl` persists + rehydrates the active pack
  (`IconPackRef`).
- **Phase 4:** `SystemBarAppearance` (F1/F2) + rail long-press menu (F3) + `SettingsIntents`.
- **Phase 5:** drawer long-press menu (F3) + hidden-apps "Remove" (Q2).
- **Phase 6:** `SettingsViewModel` + `SettingsPanel` (4 sections); home rows data-driven
  from prefs; ThemeMode applied app-wide; `WallpaperIntents`.
- **Phase 7:** verified on `soft_home_pixel` (API 35) with real screenshots.
- **Phase 8:** docs updated.

**Verified on emulator (Android 15 / API 35, `soft_home_pixel`)** — real screenshots:
- [x] F1 light: status icons **dark** on cream (`p3-home-idle.png`).
- [x] F1 dark: ThemeMode=Dark → status icons **light** on warm-dark (`p3-home-dark.png`).
- [x] F2: nav bar hidden under gesture nav (edge-to-edge home).
- [x] F3 rail long-press → row-style r24 menu (`p3-rail-longpress-menu.png`).
- [x] F3 drawer long-press → Open/App Info/Edit Icon/Remove/**Uninstall greyed**
      (system app)/Shortcuts (`p3-drawer-longpress-menu.png`).
- [x] G: four settings sections (`p3-settings-appearance.png`,
      `p3-settings-widgets-wallpaper-gestures.png`).
- [x] G: "Quick notes" toggled off → home row gone; survives `am force-stop`
      (`p3-home-notes-hidden.png`).
- [x] Locked rows show "Always shown" + disabled toggle.
- **Tests:** 160 JVM unit tests pass; 20 instrumented Compose tests pass on the AVD.

**Blocked / needs decision**
- None. **Stopped at the end of P3 for review. Do NOT auto-start P4.**

**Next:** P4 (onboarding H + polish) — likely absorbs the deferred drag-and-drop
(row + app→folder), the rich icon editor, and backup/restore.

---

## Session log (previous)

### Session 6 - P2 Widgets + Folders — DONE (2026-09-25)

**Why:** P2 = custom widgets (E) + folder system (D), built on the **Warm Right
Rail** design system (not the retired P1 card/grid language). Spec:
[`superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md`](superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md).

**Audit result (Session 6)**
- Re-audited `.pen`: still **no** widget/folder nodes (only drawer app tiles + the
  "rail expands" note). `.pen` remains read-only; P2 is authored from Warm patterns.
- Found **stale P2 scaffolding** (dead code) built on the retired language:
  `SoftWidgets.kt` (cream cards + 62dp folder squircle) and `FolderLogic`'s
  home-grid model. Replacing both.

**Locked decisions**
- **P2-1** Folders live in the **App Drawer** (grid), not on the row-home.
- **P2-2** Quick notes = **tap-to-expand row**, persisted in **DataStore**.
- **P2-3** Battery/storage = **real** `BatteryManager` + `StatFs` values.
- Deferred (recorded in `04` section I): MediaSession #39, KkPN3 dark mode, live
  calendar (`CalendarContract`), drag-and-drop → folder (P3), battery callback (P3).

**Done (phases 0-7)**
- **Phase 0 (model + tokens + assets):** added lucide `battery` + `hard_drive`
  glyphs (`LineIcon.Battery`/`HardDrive`); reworked `FolderLogic` to a drawer-grid
  model (`DrawerGridItem`); added `Notes` + `DeviceStatusSnapshot` models; removed
  unused `LauncherPrefs.quickNotes`; added `MotionTokens.notesExpand()` + P2 dimens
  (`folderTile` 68 r21, `folderMiniIcon`, `folderPopupRadius`, `widgetProgressHeight`).
- **Phase 1 (repositories, `core:data`):** `NotesRepository` (DataStore),
  `FolderRepository` (DataStore, JSON via `FoldersCodec`), `DeviceStatusRepository`
  (`BatteryManager` + `StatFs`, reflection-safe charging + sticky-intent fallback,
  pure `storageFromBlocks` math). Bound all three in `DataModule`.
- **Phase 2 (design system):** new `atom/HomeWidgetRows.kt`
  (`CalendarRowContent`, `BatteryStorageRowContent`, `NotesRowContent`,
  `ThinWarmProgressBar`) and `atom/FolderTile.kt` (`FolderTile`, `FolderPopupBody`).
  **Removed** the stale `SoftWidgets.kt`.
- **Phase 3 (home):** wired calendar / battery-storage / notes rows into the home
  list; `HomeState.Notes` + `HomeRowId` additions; `HomeViewModel` gained notes +
  real device-status state (re-read on `ON_RESUME`); `HomeActivity` injects the VM.
- **Phase 4 (drawer folders):** `AppDrawerViewModel` builds drawer grid cells
  (folders-first) + open/rename/add/remove + persistence; `AppDrawerScreen` renders
  `FolderTile`s and the `FolderPopup`; "New folder" header action.
- **Phase 5 (tests):** +`FolderLogicTest`(11), `NotesTest`(3), `NotesRepositoryTest`(4),
  `FolderRepositoryTest`(6), `DeviceStatusTest`(5), `FolderCellsTest`(4),
  `HomeStateTest`(+4), `MotionTokensTest`(+1). **124 unit tests, 0 failures.**

**Verified on emulator (Android 15 / API 35, `soft_home_pixel`)** — real screenshots:
- [x] Home renders calendar + battery/storage + notes rows (`p2-home-idle.png`).
- [x] Battery/storage **match the device**: 100% (`dumpsys battery` level 100);
      Storage 19% · 4.6 GB free (`df` ≈ 4.65 GB available).
- [x] Folder create → fill (Calendar/Camera/Clock) → 2×2 tile (`p2-folder-empty/filled/tile.png`).
- [x] **Folder persists after `am force-stop`** (`p2-folder-persisted.png`).
- [x] Notes tap-to-expand + editor + typing (`p2-notes-editor/typed.png`).
- [x] **Notes persist after `am force-stop`** — DataStore had
      `quick_notes → "y milk and call mom"`; home preview showed it (`p2-notes-persisted.png`).
- [x] Swipe-up → drawer still works; taps land on rows.

**Bug found & fixed during verification**
- The expanded notes editor clipped off-screen (notes is the last row). Fixed by
  making the home column scrollable **only while NOTES is expanded** and trimming
  the home bottom padding — this keeps the swipe-up-to-drawer layer behind the
  content working at rest (see `04` section I #59, D-note).

**Blocked / needs decision**
- None. **Stopped at the end of P2 for review. Do NOT auto-start P3.**

**Next**
- P3 on approval: system UI (F) + settings (G). Deferred items to revisit: media
  session, live calendar, drag-and-drop, battery callback stream, KkPN3 dark mode.

---

## Session log (continued)

**Why:** the `.pen` was updated — the old grid-squircle home is superseded by a
vertical row list + a right icon rail + a motion system. All prior node IDs
(`bi8Au`, `lk7jo`, `IwpZU`, `HhwVC`, `o0Fi0`, `ws286`, `uwKsz`, …) no longer exist.

**Audit result** (spec: `docs/superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md`)
- New frames: `znb90` (home), `L7ZAp` (system board), `TpzL1` (drawer), `KkPN3` (dark ref).
- **Key finding:** the app drawer is **kept** (grid + alphabet rail) but **redesigned**
  — it is *not* replaced by the right rail. The rail is a separate home element.

**Done (phases 0-7)**
- **Tokens:** new colors (`railBg #D8C8B6`, `divider #D0C2B1`, `tileWarm #F5EFE6`,
  `categoryWash #E4D7C7`, `drawerBg #DCCDBA`, `drawerStroke #C8B8A6`,
  `progressTrack #B9AA98`, `indexActive #B06F52`, + drawer tints), dimens
  (rail 72, drawer tile 68 r21, album 64, …), spacing (`railGap 22`), and type
  (`ClockLarge` 58 normal, `DateNumber` 60 normal, `RowDisplay` 28 normal, …).
- **`MotionTokens.kt`** (new): RAIL SLIDE 220 / SEARCH FADE 160 / MUSIC RISE 280 /
  ease cubic-bezier(0.2,0.8,0.2,1).
- **25 new icon drawables** (rail + music + drawer examples) + `LineIcon` enum additions.
- **Model:** `AppInfo.category`; `DrawerCategory` + `DrawerCategoryMapper`;
  `RailShortcut` + `RailShortcutResolver` (8 shortcuts → default device apps).
- **Atoms** (`HomeRows.kt`): `HomeRow`, `HomeDivider`, `RailIcon`, `MusicPlayerRow`.
- **Home** (`HomeScreen.kt`): rewritten as a row list (time → date → weather →
  search → music) + `HomeRightRail`; `HomeState {Idle, Search, Music}` with
  tap-row triggers and tokenized animation (rail nudge, music grow in-place).
- **Drawer** (`AppDrawerScreen.kt`): bg `#DCCDBA`, tile 68 r21 + labels, category
  nav (mapped from `ApplicationInfo.category`), search h56 r28, alphabet colors.
- Removed legacy home atoms (`ClockBlock`, `WeatherCard`, `MicSearchPill`,
  `PageIndicator`, `HintText`) and dead dimens.

**Verified on emulator (Android 15 / API 35, `soft_home_pixel`)**
- [x] Debug APK builds (11.85 MB), installs, and is the default HOME.
- [x] Home renders the row list + 8-icon rail (`docs/screenshots/warmrp-idle.png`).
- [x] SEARCH state: row shows "searching" + rail nudges in (`warmrp-search.png`).
- [x] MUSIC state: music row grows in-place (bounds 387→471px) (`warmrp-music.png`).
- [x] Drawer matches `TpzL1` with tile labels + category nav (`warmrp-drawer.png`).
- [x] Category filter works — Entertainment shows only YouTube + YT Music
      (`warmrp-drawer-entertainment.png`).
- [x] **87 unit tests, 0 failures** (was 63; +24: MotionTokens, DrawerCategory,
      HomeState, RailShortcut).
- [x] No fatal exceptions in logcat after the two launch-crash fixes.

**Bugs found & fixed during verification**
1. Negative-padding crash in `HomeRightRail` → use `Modifier.offset` instead.
2. Swipe-up layer swallowed all taps → moved it behind `HomeScreen`.

**Blocked / needs decision**
- Awaiting go-ahead before P2. Do NOT auto-start.

**Next**
- P2 on approval (widgets + folders), or address open items (`04` section H #39).

---

## Session log (continued)

### Session 4 - P1.5 glyph-uniqueness bug found & fixed (2026-09-24)

**Correction to Session 3 verification**
- Session 3 marked P1.5 "verified" with "each mapped app shows its own glyph" and
  "screenshot-compare vs `.pen` lk7jo — match". That check validated **container**
  shape (squircle, radius, charcoal/cream) more than **per-app glyph identity**.
  Treat Session 3's "glyph uniqueness verified" claim as **untrusted**; this
  session replaces it with fingerprint logs + comparison-grid screenshots.

**Root cause (what we actually found on device)**
1. SoftHome AdaptiveIcon **monochrome** reused a full-cream-fill foreground →
   `PorterDuff.SRC_IN` cream tint → solid cream square (reproduced under AutoMask).
2. `IconCompositor` preferred any non-null `monochrome` layer without checking
   whether it was a usable silhouette vs a blob / empty layer.
3. `AppIcon` `remember` for pack drawables omitted `sizePx` (risk of locking a
   tiny/empty bitmap) and did not fall through FromPack decode failure → AutoMask.
4. SoftMonoTest pack itself is fine: **15 unique PNG hashes** in `SoftMonoTest.zip`.

**Fix (D-016)**
- Mono coverage gate + full-adaptive fallback; reject charcoal-only/blob masks;
  `sizePx` in remember keys; pack→mask→glyph fallthrough; proper SoftHome mono asset;
  `IconCompositorGlyphUniquenessTest` (5 cases).

**Re-verification evidence (not just prose)**
- Logs: 15 FromPack tiles, **15 distinct** `fingerprint=` values
  (Calendar / Camera / Chrome / … / YT Music).
- Screenshots: `docs/screenshots/drawer-after-pack.png`,
  `docs/screenshots/home-after-pack.png`,
  `docs/screenshots/comparison-grid-pack.png`.
- Unit: iconpack suite green including new uniqueness tests.
- **Not claiming "fully verified forever":** AutoMask tint can leave residual brand
  colour on some system icons; OEM icons not exhaustively tested.

**Next**
- Await confirmation before P2.

---

### Session 3 - P1.5 real icon-pack decode built + verified on device (2026)

> **Superseded in part by Session 4.** Keep for history. The "each mapped app shows
> its own glyph / screenshot-compare match" bullets below are **not** sufficient
> proof of glyph uniqueness; see Session 4 + D-016.

**Done**
- Real icon-pack drawable decode (retired the P1 placeholder glyph):
  - `IconPackSource` model (`Zip` / `InstalledPack` / `Assets`) + richer `IconPack`
    (`source`, `sourceKind`, `drawableCount`, `displaySource`).
  - `IconPackDrawableLoader` - decodes the pack entry for each source kind, with an
    LRU cache; zip decode prefers the highest drawable density present.
  - `AppIconTile` gained a full-bleed painter path; decoded drawables render at the
    design's **0.49** symbol ratio; the category glyph is now fallback-only.
- `InstalledIconPackScanner` - queries the standard icon-pack intents (ADW / Apex /
  Nova / Go / TeslaCoil / Lawnchair / Pixel) and parses the chosen pack's appfilter.
- Zip-import flow: `IconPackImporter` (validate + copy into app storage + parse +
  verify drawables) emitting a `Flow<ImportProgress>` (Validating / Indexing / Done /
  Failed) - never blocks the UI thread, never throws on bad input.
- `IconPackImportSheet` (ModalBottomSheet on the drawer): pick a `.zip` or an
  installed pack, thin charcoal progress bar, exact-state messages.
- Real auto-mask: `AutoMask` (pure rules) + `IconCompositor` (Android paint) +
  `IconBitmapProvider` (Icon -> squircle cream mark, cached). Unknown apps now mask
  their **real** icon instead of showing a leaf glyph.
- Reactive fix: both ViewModels now `combine` the `activePack` flow (previously read
  `.value`), so applying a pack updates the grid live.
- Parser hardening: replaced the `ComponentInfo{...}` regex (threw on-device) with
  plain string parsing.
- Tests: **58 unit tests, 0 failures** (was 54). New: `AutoMaskTest` (9),
  `IconPackImporterTest` (8, Robolectric + real zip), `IconPackTest` model (5),
  plus parser/resolver regressions (brace-heavy input, malformed XML, no-appfilter,
  real drawable name propagation).

**Verified on emulator (Android 15 / API 35, `soft_home_pixel`)**
- [x] Debug APK builds (`app-debug.apk`, 11.96 MB) and installs.
- [x] App set as default Home (`set-home-activity`) and HOME returns to SOFT / HOME.
- [x] Built a real icon pack `.zip` (appfilter.xml + `res/drawable-xxhdpi/*.png` for
      14 installed apps), pushed to the device, imported via the in-app flow.
- [~] **Decoded drawables render per entry** - claimed Calendar=C, Camera=dot, etc.
      **Session 4: this was not fingerprint-proven; re-verified with fingerprints +
      comparison grid.**
- [x] **Auto-mask** covers apps absent from the pack.
- [x] Corrupt/mismatched pack -> graceful message, no crash, previous state kept.
- [~] Screenshot-compared vs `.pen` `lk7jo` (shape tokens). **Session 4: shape-only
      compare is insufficient for glyph uniqueness.**
- [x] Unit tests pass (count grew again in Session 4).

**Blocked / needs decision**
- Awaiting go-ahead for P2 (widgets + folders). Do NOT auto-start.

**Next**
- P2 on approval: calendar/battery/notes widgets + folder system.

---

## Session log (continued)

### Session 2 - P1 built + verified on device (2026)

**Done**
- Toolchain installed entirely on D: (JDK 17 on C:, SDK `D:\Android\Sdk`, Gradle
  8.11.1 `D:\Android\gradle-8.11.1`, caches `D:\gradle-cache`, temp `D:\Android\downloads`).
  Android Studio IDE NOT installed (not needed for Gradle builds); used cmdline-tools.
- Multi-module Gradle project scaffolded (see repo layout below).
- Design system tokens created (`core:designsystem`) - every value from `homeApp.pen`.
- 26 lucide vector drawables generated from the `lk7jo` icon set (ASCII-normalised).
- Domain models + repositories (AppRepository, PrefsRepository via DataStore).
- Icon pack system: `AppFilterParser`, `IconMasker`, `IconResolver`, `IconPackRepository`.
- Home screen UI (home grid, clock, weather card, mic pill, page indicator, swipe-up).
- App drawer UI (4-col grid, alphabet index rail, in-drawer search).
- `HomeActivity` as the real HOME intent target; `LauncherRole` for the default picker.
- Unit tests (54 across 12 suites) + Compose UI smoke tests.
- Verified on emulator: installed, **set as default launcher**, HOME returns to SOFT/HOME.
- Screenshot-compared home + drawer against the `.pen` mockups.

**Verified on emulator (Android 15 / API 35, Pixel 7 AVD)**
- [x] Debug APK builds (`app-debug.apk`, 11.47 MB) and installs.
- [x] App registered as a HOME handler (`cmd package query-activities -c HOME`).
- [x] `cmd package set-home-activity` succeeds; pressing HOME shows SOFT / HOME.
- [x] Home screen renders clock, date, weather card, 4-col icon grid, mic pill.
- [x] Swipe up opens the drawer with alphabet index + search; tiles are square squircles.
- [x] All 54 unit tests pass.

**Blocked / needs decision**
- Awaiting go-ahead for P2 (widgets + folders). Do NOT auto-start.

**Next**
- P2 on approval: calendar/battery/notes widgets + folder system.

---

## Repo layout (as built)

```
menu/
├─ design/homeApp.pen            # read-only design source of truth
├─ docs/                         # this documentation set
├─ gradle/libs.versions.toml     # version catalog
├─ app/                          # Application, HomeActivity, LauncherRole, stubs
├─ core/
│  ├─ designsystem/              # tokens, theme, atoms, 26 vector drawables
│  ├─ common/                    # dispatchers, SoftResult
│  ├─ model/                     # AppInfo, IconPack, GridConfig, LauncherPrefs
│  └─ data/                      # AppRepository, PrefsRepository (DataStore), DI
└─ feature/
   ├─ home/                      # HomeScreen + HomeViewModel
   ├─ iconpack/                  # parser, loader, importer, scanner, masker,
   │                             # compositor, resolver, repository, AppIcon,
   │                             # IconPackImportSheet + ViewModel
   └─ appdrawer/                 # AppDrawerScreen + ViewModel + AlphabetIndex
```

---

## P1 - Home (A) + Icon Pack (B) + App Drawer (C)  ·  DONE

### Verification (P1 exit criteria)
- [x] Debug APK builds and installs.
- [x] App can be selected as default Home app (verified via `set-home-activity`).
- [x] Icon pack parser/resolver/masker implemented + unit-tested; corrupt pack -> mask.
      (Real drawable decode landed in **P1.5** - see the Session 3 log above.)
- [x] Drawer opens via swipe-up with alphabet index + search.
- [x] Home screen visually matches `homeApp.pen` tokens (screenshot-compared).
- [x] Unit tests for icon masking, pref saving, drawer logic pass (54/54).

### P1 stubs (skeleton only)
- Default-launcher picker flow: real HOME target + basic picker intent.
- Live wallpaper engine: flat cream background only.
- KWGT-style system widgets: none (P2).
- Settings panel: read-only shell (`SettingsStubActivity`).
- Voice search: mic pill present, action is a no-op stub.
- ~~Icon pack real drawable decode~~ **DONE in P1.5** (see Session 3).

---

## P1.5 - Real Icon Pack Decode  ·  DONE (with Session 4 uniqueness correction)

### Verification (P1.5 exit criteria)
- [x] Real icon-pack drawable decode replaces the P1 placeholder glyph.
- [x] `appfilter.xml` parse + package -> drawable resolution (zip & installed pack).
- [x] Zip-import flow with structure validation + non-blocking progress.
- [x] Installed-pack picker via the standard icon-pack intent query.
- [x] Real auto-mask: app's real icon composited into the squircle (cream mono mark).
- [x] Graceful error handling: corrupt pack / malformed appfilter / unreadable icons
      -> message + fallback to mask; no crash.
- [x] Unit tests for parsing (valid & invalid), resolution, auto-mask, corrupt pack.
- [x] **Glyph uniqueness** (Session 4): distinct pack fingerprints on device +
      `IconCompositorGlyphUniquenessTest` + comparison-grid screenshot.
      Session 3's shape-only "verified" claim is corrected in the Session 4 log.

### P1.5 known limitations
- Active pack auto-restores from latest zip in `filesDir/iconpacks/` (in-memory
  StateFlow + folder scan); full prefs-backed pack id still P3.
- Badge listener, live wallpaper, system widgets, settings, voice search: still P2/P3.
- AutoMask tint can leave residual brand colour on some system adaptive icons.

---

## P2 - Widgets (E) + Folders (D)  -  DONE (2026-09-25)

Calendar row + battery/storage row (real values) + quick-notes row (DataStore) on the
home (Warm Right Rail rows); folder system in the app drawer (tile + popup). 124 unit
tests; verified on emulator with real screenshots. See the Session 6 log above.

## P3 - System UI (F) + Settings (G)  ·  NOT STARTED

## P4 - Onboarding (H) + Polish  ·  NOT STARTED
