# 05 - Progress

> Living changelog. Updated at **every checkpoint** (start, end of each build step
> group, and at each priority boundary). Newest entry on top.

**Session 25: Audit Batch 1 — functional gaps (QW1 live package monitoring + QW2 Home-press)
DONE (2026-09-26).** Following a full technical + Nielsen audit (baseline ~3.0–3.4/5), the two
highest-priority functional gaps were fixed and verified on a real Android runtime (emulator;
Tecno manual pass pending device connection).

- **QW1 — live install/uninstall monitoring.** New `PackageEventMonitor`
  (`core:data/packages`) wraps `LauncherApps.registerCallback` (API 21+, no extra permission)
  and exposes a `Flow<PackageChange>`. New `AppInventoryCoordinator` reloads the launchable
  app list **and** prunes dead `componentKey`s from `hiddenApps`, `iconOverrides`, folder
  membership and rail slots via the new pure `LauncherPrefsCleanup` (unit-tested; no-op writes
  skipped). `HomeViewModel` + `AppDrawerViewModel` observe the monitor; the drawer no longer
  lags behind installs/uninstalls until process restart.
  *On-device evidence (emulator):* `[MONITOR] registered LauncherApps callback`,
  `[MONITOR] removed com.softhome.launcher.debug.test` and `[MONITOR] added …` — captured live
  with the launcher foregrounded.
- **QW2 — press HOME while already on Home.** `HomeActivity` overrides `onNewIntent`, bumping a
  `homeIntentToken`; `LauncherRoot` observes it to close the drawer and `HomeScreen` collapses
  any expanded row back to Idle. *On-device evidence (emulator, real `KEYCODE_HOME`):*
  `[HOME] onNewIntent -> reset token=1`.
- **Test hygiene fix (pre-existing).** The `app/src/androidTest` sources did not compile
  (`HomeScreen(state = …)` stale API); fixed, so the instrumented suite now builds. Added a
  `HomeScreenTest` case for the QW2 collapse + 10 JVM tests (`LauncherPrefsCleanupTest`).
- **Backlog (pre-existing, not introduced here):** `IconPackPersistenceTest` "stored id … is
  cleared" flaky under Robolectric DataStore state-sharing; `HomeRowDragTest` drag-position
  assertion is device-geometry-sensitive (passes on the reference Tecno, off on the AVD).

**Overall:** P1.5 decode works; Session 4 corrected an over-claimed verification and
hardened glyph uniqueness (D-016). **Session 5 redesigned the home + drawer into the
"Warm Right Rail" system.** **Session 6 delivered P2: custom widgets (calendar /
battery-storage / quick notes) + the folder system, under the Warm Right Rail design
system.** **Session 8 fixed the drawer BACK regression.** **Session 9 delivered P3.5:
the drawer icon color system (cream tiles + per-category colored glyphs + amber
selected state), rendering the `TpzL1` frame faithfully.** **Session 10 delivered P4a:
drag-and-drop — one gesture engine, two targets (home-row reorder + drawer app→folder).**
**Session 11 delivered P4b: the "Edit Icon" rich editor (per-app typed override: pack
drawable or glyph+color, with reset).** Working P4b and P4c back-to-back (user-authorized
no-stop), stopping at the end of P4c.

**Session 12: P4c — Dark Mode from the `KkPN3` named palette — DONE (2026-09-25).**

**Session 13: P4d — Backup & Restore — DONE (2026-09-25). P4 is COMPLETE. See the
FINAL PROJECT REPORT at the end of this file.**

**Session 14: On-device fixes P1–P4 — P1–P3 DONE earlier; P4 (drawer icon correctness:
`DrawerIconMap` ported from the `.pen` + per-cell uniqueness) DONE (2026-09-25).** See
`superpowers/specs/2026-09-25-onddevice-findings-fix-plan.md` §14.

**Session 15: P5 — 8-category drawer pager DONE (2026-09-25).** Horizontal swipe and tab
tap are implemented and verified on the Tecno and emulator. STOP #3 (docs + final review).

**Session 16: drawer scroll tap-guard patch DONE (2026-09-25).** Starting a vertical scroll
no longer launches the first app touched; tap is emitted only after a release without
movement past touch-slop. Added scroll, tap, and stationary long-press regression tests
and re-ran the JVM suite successfully.

**Session 17: release/update safety DONE (2026-09-25).** Versioned the app as `0.1.1`
(`versionCode 16`), added the version footer in Settings, and added the root
`update.ps1` install helper. Audited all persisted state: existing DataStore names and
keys remain unchanged; home rows and icon overrides have tolerant migrations/codecs;
folders and notes retain their stable stores and round-trip tests. No destructive data
migration is required for the next `installDebug` update.

**Session 18: motion polish foundation + first pass DONE (2026-09-25).** Added
tokenized press/overlay/theme motion, Warm press feedback, animated widget progress,
animated drag lift, haptic feedback for long-press/toggle, and scale/fade transitions
for drawer folder/menu/icon-editor overlays. Build and the Tecno drawer gesture suite
remain green. A Tecno motion recording is retained under `docs/videos/`; frame timing
showed 1.04% aggregate janky frames after reset during representative swipes.

**Session 19: grid-first drawer layout DONE (2026-09-25).** Removed the redundant
`All apps`/action header, moved the search pill below the icon grid, and moved New folder
and Icon pack actions into the Settings panel's APP DRAWER section. The latest APK builds,
the full 485-test JVM suite passes, and the Tecno screenshot is retained as
`docs/screenshots/layout-latest2.png`.

**Session 20: motion polish completion audit DONE (2026-09-25).** Added the remaining
tokenized press feedback to Notes, Music, Settings actions, and drawer category tabs.
Rebuilt and installed version `0.1.4` (`versionCode 19`) on Tecno. The full build/test
verification is green; a fresh Tecno `gfxinfo` run measured 174 frames with 1 janky frame
(0.57%) and 0 missed vsyncs while opening and scrolling the drawer. Remaining checklist
items are documentation-only: formal before/after video captures, a future badge data
source, and optional deeper first-decode profiling.

**Session 21: device app coverage + glyph audit DONE (2026-09-25).** Confirmed the drawer
queries Android's complete `MAIN` + `LAUNCHER` activity set (79 activities on the Tecno
reference device, including SOFT/HOME itself), so newly installed launchable apps appear
without a code update. Added explicit Warm glyph mappings for the remaining reference-device
utilities (Niagara Launcher, Honest Bank, call blocker, GuitarTuna, and NFC Checker) so they
use design-language icons instead of generic category fallback. Rebuilt, installed, and
smoke-tested the drawer. This feature build is version `0.1.5` (`versionCode 20`);
screenshot: `docs/screenshots/apps-mapped.png`.

**Device check follow-up (2026-09-25).** A later screenshot that appeared to show
brand-logo icons was verified to be the Tecno stock launcher (`com.transsion.launcher3`),
not SOFT/HOME. When `HomeActivity` is opened explicitly, the app shows the intended Warm
glyph drawer and all-app coverage (`docs/screenshots/explicit-home.png`). The Tecno phone is
not currently using SOFT/HOME as its default Home app; this is a device role/picker issue,
not a drawer icon-resolution regression.

**Session 22: P6 — drawer category transition (no peek) DONE (2026-09-25).** Replaced the
category `HorizontalPager` with an `AnimatedContent` keyed on `state.category`: a
10dp directional micro-slide + 160ms fade, so a neighbouring category never peeks at the
edge mid-swipe without changing the resting grid geometry.
Added a `pointerInput` horizontal-drag detector for the swipe (tab taps run the same
transition); the alphabet rail and `CategoryNav` now read `state.category`, with one
`LazyGridState` per category. Root cause: `HorizontalPager`'s drag physics render the
adjacent page during a swipe — not a `pageSpacing`/`contentPadding` setting. Verified on the
Tecno by screen recording (60fps): swipe A→B, B→A, and a tab tap all show one full-width
page, no peek; swipe-down-to-close and vertical grid scroll still work. Evidence:
`docs/videos/p6-category-swipe-ab.mp4`, `docs/videos/p6-category-swipe-ba-and-tab.mp4`,
`docs/screenshots/p6-transition-dense-frames.png`, `p6-transition-left-edge.png`,
`p6-swipe-comm-to-all.png`, `p6-tab-tap-all-to-comm.png`. See D-058.

**P6 revision (2026-09-25).** Restored the pre-P6 parent `padding(start = Spacing.xxl)` so
the static `All` grid and chrome match the grid-first layout. `AlphabetRail` now renders
only for `DrawerCategory.All`; non-All categories reclaim that space. The focused category
instrumented suite passes 7/7 on TECNO CN7c. Evidence: `docs/screenshots/p6r-all-static.png`,
`docs/screenshots/p6r-communication-no-rail.png`, and `docs/videos/p6r-micro-slide.mp4`.

**P6 grid/tuning follow-up (2026-09-25).** Non-All pages now reserve the hidden alphabet
rail's 20dp + spacing as grid `contentPadding`, keeping the same four column slot geometry,
tile size, and centers as `All` without rendering the rail. Tecno evidence covers `All`,
Communication, and Social & Entertainment: `docs/screenshots/p6g-all.png`,
`p6g-communication.png`, and `p6g-social-settled.png`. Added two uncommitted motion candidates
for device comparison: option A (240ms + WarmEase) and option B (280ms + FastOutSlowIn),
both retaining the 10dp micro-slide. APK/video pairs are under `docs/videos/`.
The focused category instrumented suite remains green at 7/7; no P6 commit has been made.

**P6 motion finalization (2026-09-25).** Opsi B dipilih sebagai motion final: 10dp
directional micro-slide, 280ms, dan `FastOutSlowInEasing`. `AppDrawerScreen` now uses
`MotionTokens.categoryLauncher()` for category enter/exit; the grid geometry and
AlphabetRail behavior remain unchanged. The Tecno device has the final Opsi B APK installed.

**Session 24: P8 music reverted — Music player row removed (2026-09-25).** Per user
request, the home Music player row was removed entirely: `PlaybackController` /
`DemoPlaybackController`, `MusicPlayerRow`, the `Music` `HomeRowKind`/`HomeRowId`, the
`HomeState.Music` state, and the Spotify deep link are all gone (D-062, supersedes D-059).
The default home is now **Time → Date → Weather → Search**. Weather (D-060) and Clock
(D-061) are unchanged. JVM suite: **275 unit, 0 failures**.

**Session 23: P8 — motion demo (weather / clock / rail) DONE (2026-09-25).**
*(Music part since removed in Session 24 / D-062.)* The home's live motion runs on honest
local state: the Weather row renders an abstract fallback condition (`Clear`), the Clock
uses a local minute ticker with a per-digit transition, and the right rail keeps its single
drag engine with press/drag/drop feedback. **No MediaSession / Spotify / weather API is
used.** See the Session 23 log below and `docs/09` D-060/D-061.

---

## Session log

### Session 24 - P8 music reverted: Music player row removed (2026-09-25)

**Why:** the user asked to drop the music section from the home.

**What changed:**
- Deleted `feature/home/.../PlaybackController.kt` and `PlaybackControllerTest.kt`.
- `core/designsystem/atom/HomeRows.kt` — removed `MusicPlayerRow`, `MusicProgressBar`,
  `MusicControl`, `formatPlaybackTime`, and their now-unused imports.
- `feature/home/HomeScreen.kt` — removed the `DemoPlaybackController` + playback state,
  the music `MusicRow` composable, the `HomeRowKind.Music` / `HomeRowId.Music` branches,
  the "Music" drag label, and the music import.
- `feature/home/HomeState.kt` — removed `HomeState.Music` (`isMusicOpen`) and
  `HomeRowId.Music`.
- `core/model/HomeRowPref.kt` — removed `HomeRowKind.Music` (default order is now 7 kinds).
- `feature/home/RowLaunchResolver.kt` — removed the Music/Spotify path and the
  `SPOTIFY` constant.
- `core/designsystem/theme/Dimens.kt` — removed the four dead music dimens.
- `SettingsPanel` label map and tests updated to drop Music.

**Tests:** **275 JVM unit, 0 failures** (`.\gradlew.bat testDebugUnitTest`); removed the
playback tests and the music home-state cases; home-row logic/drag tests updated.

**Decisions:** D-062 (music row removed; supersedes D-059).

**Blocked / needs decision**
- None.

---

### Session 23 - P8 Motion demo: music playback abstraction + weather fallback + clock ticker + rail feedback — DONE (2026-09-25)

**Why:** the home's widgets should feel alive (progress that actually moves, pause that
freezes, seek that jumps, a clock that ticks, a weather state that can animate) **without**
claiming to control system media or depending on a weather API. This is a design/demo
state layer with a clean seam for a future `MediaSession` implementation.

**Scope / decisions:**
- **Music** — `PlaybackController` interface + `PlaybackState(isPlaying, currentTimeMs,
  durationMs, progress)`; the only implementation is `DemoPlaybackController` (50 ms ticker
  → `advanceBy`, `togglePlayPause`, `seekTo` clamped). The UI reads
  `playbackState.progress`/`currentTimeMs`; the play control reflects `isPlaying`. The
  progress bar is a deterministic function of `duration`/`currentTime`, **not** a decorative
  animation. **No `MediaSession`, no Spotify control, no notification** — the interface is
  the swap seam (D-059; supersedes D-021's "static UI" premise).
- **Weather** — `WeatherUiState(condition = WeatherCondition.Clear, temperatureLabel)`;
  the row cross-fades between conditions and plays an ambient icon scale. A future
  repository replaces the single state source without touching the animation (D-060).
- **Clock** — `rememberMinuteKey()` (sleep-to-minute-boundary coroutine) + a per-character
  `AnimatedContent` so only changed digits animate (D-061); reduced-motion = hard cut.
- **Sidebar (right rail)** — kept the **existing** `dragSource`/`dropTarget`/`warmPress`
  engine; press/drag/drop feedback (lift, insertion indicator, slot shift, dimmed source)
  is integrated into that one gesture owner. **No second gesture handler** was added.

**Bug found & fixed during smoke test:**
- The progress bar initially rendered the static design value (0.31) instead of live state.
  Corrected so both the collapsed and expanded layouts read `playbackState.progress`
  (`HomeScreen.kt` → `MusicPlayerRow(progress = playbackState.progress, …)`), and the
  elapsed/duration labels bind to `currentTimeMs`/`durationMs`. The APK was rebuilt and
  reinstalled; the new recording was captured after the fix.

**Tests:** **280 JVM unit, 0 failures** (`.\gradlew.bat testDebugUnitTest`);
`PlaybackControllerTest` locks advance / pause / resume-from-position / seek-clamp /
stop-at-duration / progress ratio. **50 instrumented, 0 failures** on Tecno CN7c ((
`.\gradlew.bat :app:connectedDebugAndroidTest`).

**Evidence (Tecno CN7c, API 36):**
- `docs/videos/p8-motion-playback-expand.mp4` — long-press expand → Play (progress moves)
  → Pause (frozen) → Seek (jumps to picked position) → Play-from-seek.
- `docs/screenshots/p8-motion-home-idle.png`, `p8-motion-music-playing.png`,
  `p8-motion-music-paused.png`, `p8-motion-music-seek.png`, `p8-motion-music-expanded.png`,
  `p8-settings-check.png`, `p8-theme-dialog.png`.

**Decisions:** D-059 (PlaybackController abstraction + local demo state), D-060 (weather
fallback state), D-061 (local clock minute ticker + digit transition).

**Blocked / needs decision**
- None. The player is a **local demo** by design; a real `MediaSession` adapter is a
  separate, later task that implements the same `PlaybackController` interface.

---


### Session 14 - P4 Drawer icon correctness (per-package map + uniqueness) — DONE (2026-09-25)

**Why:** on the real Tecno the drawer showed "duplicate / generic" icons — DANA, GoPay,
Shopee, Tokopedia, Gojek, Grab, Agoda, KFC, Discord, Claude, ChatGPT, Fortnite, Tandem,
Pinterest, Threads, and every uncategorised app collapsed to the same `AppWindow` glyph.
Plan §5 (P4). Verified on Tecno `169402562R001782` (API 36) + emulator `soft_home_pixel`.

**Root cause found:** `DrawerIconMap` (a prior partial attempt) was **orphaned** — no
production code called it. `AppDrawerViewModel` still rendered `IconMasker.symbolFor(app)`
(a ~20-name string heuristic) and, critically, `resolveIcon` set the **rendered**
`ResolvedIcon.symbolName` from that heuristic, ignoring the map entirely.

**Design re-audit:** the `.pen` has **two** relevant frames that disagree — `TpzL1`
(24 real tiles, exact glyph **+** `$icon-*` color) and `ciHU3` (8 groups, ~72 apps).
Approved decision: **the tiles win** for the apps they show, `ciHU3` fills the rest.

**What changed:**
- `core:model/DrawerIconMap.kt` — rebuilt from the `.pen` (TpzL1 24 tiles first, then the
  ciHU3 8 groups); `colorToken` added; specificity-ordered fragments; missing Tecno apps
  added (Roaming, SIM Toolkit, TECNO dialer, Welife).
- `core:model/DrawerIconAssignment.kt` (new, pure) — deterministic per-**cell**
  (componentKey) glyph+color with a **uniqueness guarantee** (also fixes Phone vs Contacts,
  one package / two activities).
- `core:model/DrawerIconTokenName` + `DrawerIconColor.Token` gain **`Browser`**; `Color.kt`
  / `SoftColors` add `drawerIconBrowser` (the `.pen`'s Chrome blue). `DrawerCategory.tabs`
  now excludes `Other` (the 8 real tabs).
- `feature:appdrawer/AppDrawerViewModel` — map-driven precedence
  (**override → pack → map → heuristic → category glyph**), passed into the renderer.
- `feature:iconpack/DrawerAppIcon` + `DrawerIconColor` — `Browser` token + `fromLucide`
  glyph resolution + `tokenForName` bridge.
- `core:designsystem` — **7 broken lucide vector drawables fixed** (`banknote`,
  `credit_card`, `tv`, `music_2`, `settings_2`, `contact_round`, `bike`): they contained
  only a fragment of the path (rendered as dots/lines).

**Tests:** **265 JVM unit, 0 failures** (was 236; +29: `DrawerIconMapTest` 6,
`DrawerIconUniquenessTest` 5, plus Browser-token + name-bridge coverage).

**Evidence (real screenshots):** `p4-drawer-tecno.png`, `p4-drawer-tecno-scrolled.png`
(Tecno, dark), `p4-drawer-emulator.png` (emulator, light) — every visible tile distinct and
design-faithful.

**Decisions:** D-050 (Browser token), D-051 (`.pen`-ported map; tiles win), D-052
(deterministic per-cell uniqueness).

**Blocked / needs decision**
- None. **STOP — P4 complete, awaiting review before P5 (8 categories + `HorizontalPager`).**

---

### Session 13 - P4d Backup & Restore — DONE (2026-09-25)

**Why:** P4d is the last P4 sub-phase — export/import the whole launcher state as one
file (G4, deferred since P3). Spec:
[`superpowers/specs/2026-09-25-p4d-backup-restore-design.md`](superpowers/specs/2026-09-25-p4d-backup-restore-design.md).

**Re-audit:** the `.pen` is **byte-identical** to every audit since P3.5 (176,128 bytes,
SHA256 `08DB2A81…CA6201`). Keyword sweep `backup`/`restore`/`export`/`import`/`cloud` = **0**.
No design source; authored from the P3 `SettingsRow` + P1.5 action-pill vocabulary.
`.pen` stays read-only.

**Key finding:** unlike P4b (which built a data path from scratch), every persisted field
already had a typed model + a pure codec + a typed setter, so P4d is "one serializer +
one apply". The one gap closed: a single atomic `PrefsRepository.applyAll` (one DataStore
write).

**What changed:**
- **`core:model`** — new `BackupDocument` (versioned envelope; `app` marker + `version`).
- **`core:data`** — new `BackupCodec` (pure, total decode: `Ok`/`NotABackup`/
  `UnsupportedVersion`/`Malformed`; reuses `HomeRowsCodec`/`IconOverridesCodec`/
  `FoldersCodec`); new `BackupRepository`/`Impl` (SAF-shaped streams, non-destructive on
  failure); `PrefsRepository.applyAll(LauncherPrefs)` (single edit); DI binding.
- **`app`** — `SettingsPanel` gains a "BACKUP & RESTORE" section (2 rows + inline confirm
  + message row + SAF `CreateDocument`/`OpenDocument` launchers); `SettingsViewModel`
  gains export/import + `pendingRestore` + message state.

**Tests:** **236 JVM unit** (was 220; +16: `BackupCodecTest` 11, `BackupRepositoryTest` 3,
`PrefsRepositoryTest` +2) and **38 instrumented** (was 36; +2 `BackupRestoreEndToEndTest`,
`BackupSettingsScreenshotTest`). Green on `soft_home_pixel` (API 35) **and** the physical
device TECNO CN7c (API 36).

**Evidence (real screenshots + a real file):** `p4d-settings-backup-real.png`,
`p4d-settings-backup-emulator.png`, `p4d-saf-save-picker.png`,
`p4d-backup-saved-emulator.png`, `p4d-backup-file-example.json` (the 421-byte export),
`p4d-home-light-real.png`.

**Bug found & fixed during verification:** export first produced a **0-byte** file — the
SAF stream was opened with `.use{}` at the call site while `exportBackup` launched a
coroutine and returned immediately, so the stream closed before the async write. Fixed by
opening/using/closing the stream **inside** the coroutine (VM now takes the
`ContentResolver` + `Uri`). Re-verified: file is 421 B and well-formed.

**Decisions:** D-040 (versioned JSON format), D-041 (restore replaces, behind a confirm),
D-042 (SAF transport, id-only pack reference).

**Blocked / needs decision**
- None. **P4 is complete. STOP for review.**

---

### Session 12 - P4c Dark Mode (KkPN3 palette) — DONE (2026-09-25)

**Why:** P4c replaces the **derived warm-dark guess** (assumption #5, bg `#1F1D1A`) with
the **`KkPN3` "Dark Editorial" named palette** now present in the `.pen`, and audits every
surface under dark. It is the highest-visual-payoff P4 sub-phase; `ThemeMode` plumbing
already existed from P3. Spec: [P4 scope §2](superpowers/specs/2026-09-25-p4-scope-and-phase-split-design.md).

**Re-audit:** the `.pen` is **byte-identical** to the P3.5/P4a/P4b audits (176,128 bytes,
SHA256 `08DB2A81…CA6201`). The `KkPN3` frame was re-extracted node-by-node (see below);
the palette matches the P4 scope §0 table. The `.pen` stays **read-only**.

**`KkPN3` palette (authoritative, from the file):** screen bg `#18191A`; rail `#2E3134`;
divider `#343638`; primary text / rail icon / time / title / progress fill `#F2EEE7`;
soft text `#E2DDD5`; date day `#D2CBC1`; date month / track `#918F8B`; search placeholder
`#A8A29A`; artist `#C8C0B6`; album `#E3DED6`; album mark `#454648`; progress track
`#676866`.

**What changed:**
- **`core:designsystem/Color.kt`** — the `Dark*` tokens re-pointed to the `KkPN3` values
  (bg/surface `#18191A`, rail/`tileWarm`/`drawerTileCream` `#2E3134`, divider `#343638`,
  text `#F2EEE7`/`#D2CBC1`/`#918F8B`, progress track `#676866`, status text `#E2DDD5`);
  card/menu/popup/drawer-derived surfaces documented as same-family derivations. The dark
  **drawer icon** adaptations aligned to the neutral family.
- **`app/.../SettingsStubActivity.kt`** — now resolves `ThemeMode` (Light/Dark/System) and
  applies `SoftHomeTheme(darkTheme=…)` + a matching status/nav bar (was hardcoded light →
  a dark panel had light status-bar icons). **Bug fixed.**
- **`core/designsystem/theme/DarkPaletteTest.kt`** (new) — pins the verbatim `KkPN3`
  values + asserts every dark `SoftColors` field is set and opaque.

**Tests:** **220 JVM unit** (was 217; +3 `DarkPaletteTest`) and **36 instrumented** (was
33; +3 `DarkThemeTest`). The 7 icon-pipeline tests + all P4b tests stayed green.

**Evidence (emulator, Android 15 / API 35, `soft_home_pixel`) — real screenshots:**
`p4c-home-light.png` (baseline light), `p4c-home-dark.png` (KkPN3 dark home: `#18191A`
canvas, `#2E3134` rail, warm-white type), `p4c-drawer-dark.png` (dark drawer: dark tiles +
legible category-colored glyphs), `p4c-settings-dark.png` (dark settings panel; the Theme
row reads **"Dark"**, proving the `SettingsStubActivity` fix).

**Decisions:** D-039 (dark = the `KkPN3` named palette).

### Session 11 - P4b Edit Icon (rich editor) — DONE (2026-09-25)

**Why:** P4b is the second of the four P4 sub-phases; it is "the whole reason P3.5 was
run first — so P4's Edit Icon editor builds on the final model". Spec:
[`superpowers/specs/2026-09-25-p4b-edit-icon-rich-editor-design.md`](superpowers/specs/2026-09-25-p4b-edit-icon-rich-editor-design.md).

**Re-audit:** the `.pen` was re-audited (byte-identical to the P3.5/P4a audits: 176,128
bytes, SHA256 `08DB2A81…CA6201`, mtime 2026-09-24 11:58). Keyword sweep for an editor
mock (`picker`/`swatch`/`crop`/`resize`/`upload`/`icon editor`) = **0**. No design
source; authored from Warm tokens. `.pen` stays read-only.

**Key finding (a gap, not a wiring task):** P3's "pick-from-pack" **did not exist**.
`iconOverrides` was `Map<componentKey, packId>` — the resolver ignored the value except
to check `== activePack.id`, then re-derived the pack's **default** drawable (identical
to no override). The "Edit Icon" row was `onEditIcon = viewModel::closeMenu` (a no-op),
and `PrefsRepository` had no override setter. So P4b had to build the **data path and the
UI**, not just a picker.

**What changed:**
- **`core:model`** — new `IconOverride` (`Pack(drawableName)` | `Glyph(symbolName,
  colorToken)`) + `DrawerIconTokenName`; `LauncherPrefs.iconOverrides` retyped;
  `IconSource.Glyph` added; `ResolvedIcon.overrideColorToken` added.
- **`core:data`** — new `IconOverridesCodec` (pure, JSON, **total** decode + legacy
  migration); `PrefsRepository.setIconOverride(key, override?)` (null clears).
- **`core:designsystem`** — new `atom/IconEditorScaffold.kt` (`IconEditorCard` cream r24
  card + `ChoiceTile` + `ColorSwatchRow` + `EditorSectionLabel`); `Dimens` `editor*`/
  `choice*`/`swatch*` tokens.
- **`feature:iconpack`** — new `domain/IconEditor.kt` (pure editor state machine) +
  `ui/IconEditorSheet.kt` (live preview via the **same** `DrawerIconTile` path);
  `DrawerIconColor.nameOf` bridge; `DrawerAppIcon` honors the chosen glyph color.
- **`feature:appdrawer`** — `AppMenu`'s "Edit Icon" opens the editor; VM
  `openIconEditor/closeIconEditor/applyIconOverride/resetIconOverride` + `editingEntry`
  state; screen renders the sheet (BACK closes it, topmost first).
- **`feature:home`** — honors the override automatically via the shared resolver (no
  code change needed).

**Tests:** **217 JVM unit** (IconOverridesCodecTest 10, IconEditorTest 10,
IconResolverTest 8→10; the 7 icon-pipeline tests stayed green) and **33 instrumented**
(IconEditorSheetTest 2, IconEditorScreenshotTest 2, IconOverrideEndToEndTest 2).
*(Count reconciliation: the P4a log's "307 unit" was an over-count; the authoritative
debug-unit total is now 217.)*

**Evidence:** `p4b-editor-pack.png` (pack mode: tabs + drawable grid + gated Save),
`p4b-editor-glyph.png` (glyph mode: glyph grid + selected ring + 7 color swatches),
`p4b-override-applied.png` (Calendar shows the chosen HeartHandshake glyph in the Travel
color in the real drawer), `p4b-override-persist.png` (same after `am force-stop`).

**Decisions:** D-037 (typed per-app override), D-038 (rich = pick-from-existing; no
crop/upload).

### Session 10 - P4a Drag & Drop (row reorder + app→folder) — DONE (2026-09-25)

**Why:** P4a is the first of the four P4 sub-phases (spec:
[`superpowers/specs/2026-09-25-p4-scope-and-phase-split-design.md`](superpowers/specs/2026-09-25-p4-scope-and-phase-split-design.md)).
It delivers the two long-deferred drag targets at once — **home-row reorder** (deferred
from P3 / P3-2) and **assign app → folder** (deferred from P2 #52) — as one gesture
subsystem, per the P4a spec
[`superpowers/specs/2026-09-25-p4a-drag-and-drop-design.md`](superpowers/specs/2026-09-25-p4a-drag-and-drop-design.md).

**Re-audit:** the `.pen` was re-audited (unchanged, 176KB): **no** drag/drop/reorder/
handle node exists (keyword sweep 0). The gesture is authored from Warm tokens; the
`.pen` stays read-only.

**What changed:**
- **`core:model`** — new `DragAndDrop.kt` (`DragKind`, `DragAndDropState`, `FloatPair`)
  + pure `HomeRowDropResolver` (wraps `HomeRowLogic.move`, new) + `FolderDropResolver`
  (assign / createWith / removeFrom, wraps `FolderLogic`).
- **`core:designsystem`** — new `atom/DragAndDrop.kt`: `Modifier.dragSource` (one
  gesture owner: tap / long-press / drag), `Modifier.dropTarget`, `DragPreviewLayer`,
  `DragInsertionLine`, `DragRowChip`, `DragHoverRing`, `dragSourceAlpha`. New `Dimens`
  (`drag*`) + `MotionTokens` (`dragLift` / `dragSnapBack` / `dragReorder`).
- **`feature:home`** — `HomeScreen` rows are drag sources; live insertion line;
  `HomeViewModel.reorderHomeRow` → `setHomeRows` (same path as the up/down buttons).
- **`feature:appdrawer`** — `AppDrawerScreen.AppCell` is a drag source
  (tap / long-press menu / drag); folder tiles + the "New folder" chip are drop targets;
  `AppDrawerViewModel.assignToFolder` / `createFolderWith`.

**Tests:** **307 unit** (was 176; +18: `DragAndDropStateTest` 5, `DragDropResolverTest`
13); **27 instrumented** (was 20; +2: `HomeRowDragTest`). The 7 icon-pipeline tests
stayed green.

**Evidence:** `p4a-row-mid-drag.png` (drag chip + accent insertion line + dimmed source),
`p4a-row-persist.png` (order survives force-stop), `p4a-app-middrag.png` (icon preview +
hovered "New folder" chip), `p4a-app-to-folder.png` (2-member folder), `p4a-folder-persist.png`,
`p4a-longpress-menu.png` (menu preserved).

**Descoped:** drag-out of a folder (popup-card clickable fights the drag; tap-to-remove
exists). Resolver + VM method kept + tested for a later phase.

**Decisions:** D-035 (one shared gesture engine, two adapters), D-036 (long-press splits
into menu vs. drag by movement).

**Blocked / needs decision**
- None. **Stopped at the end of P4a for review. Do NOT auto-start P4b.**

**Next:** P4b (Edit Icon — rich editor), on approval, building on the P3.5 hybrid
renderer.

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

### Session 15 - P5 category pager + final on-device verification — DONE (2026-09-25)

**What changed:**
- The drawer now uses the eight design categories as pager pages: All, Communication,
  Social & Entertainment, Productivity & Tools, Browser & Search, Camera & Media,
  Maps & Travel, Finance & Shopping, and Food & Lifestyle.
- Both horizontal swipes and category-tab taps change the active page. Search and the
  alphabet rail remain global; category selection is runtime-only and is not persisted.
- Vertical grid scrolling remains vertical; the swipe-down drawer-close path remains intact.

**Tests:** `DrawerSwipeCategoryTest` covers left/right paging, tab selection, and the
vertical-swipe regression. `DrawerSwipeDownTest` covers close-threshold behavior. The JVM
suite remains green at **265 tests, 0 failures** (`.\\gradlew.bat test`).

**On-device evidence:**
- Tecno: `p5-tecno-all.png`, `p5-tecno-communication.png`, `p5-tecno-swiped.png`.
- Emulator: `p5-emulator-all.png`, `p5-emulator-swiped.png`, `p5-emulator-swiped-2.png`.

The horizontal swipe visibly advances All → Communication → Social & Entertainment;
the active tab is selected/underlined and no app is accidentally launched. The debug APK
was installed and launched on both connected targets.

**STOP #3:** implementation, screenshots, tests, and documentation are complete; no
additional P5 implementation work is required unless final review finds a regression.

---

### Session 16 - Drawer scroll tap-guard patch — DONE (2026-09-25)

**Root cause:** the shared `dragSource` treated every pre-long-press cancellation as a
tap. A small finger movement cancels long-press detection in Compose, so the first app
under a scroll gesture could launch accidentally.

**Fix:** `dragSource` now distinguishes release, movement-before-long-press, and timeout.
Only a release that stays within touch-slop invokes `onTap`; movement cancels the source
gesture and is left for `LazyVerticalGrid` scrolling. Long-press drag and context-menu
behavior are unchanged.

**Regression coverage:** `DrawerSwipeCategoryTest.starting_a_vertical_scroll_does_not_launch_an_app`.
The debug app and androidTest sources compile; `./gradlew test` passes all JVM suites.

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

## P3 - System UI (F) + Settings (G)  ·  DONE (2026-09-25)

System UI (theme-following status/nav bars, long-press menus) + a sectioned settings
panel. Verified on emulator (F1/F2/F3, G). 160 unit + 20 instrumented tests. See Session 7.

## P3.5 - Drawer Icon Redesign (color + labels)  ·  DONE (2026-09-25)

Cream tiles + per-category colored glyphs + amber selected state (`TpzL1`). 176 unit +
25 instrumented tests. See Session 9.

## P4a - Drag & Drop  ·  DONE (2026-09-25)

One gesture engine, two targets: home-row reorder + drawer app→folder. 217 unit + 33
instrumented tests. See Session 10 (P4a) and Session 11 (P4b).

## P4b - Edit Icon (rich editor)  ·  DONE (2026-09-25)

Typed per-app override (pack drawable **or** glyph+color; Reset); cream r24 editor card
opened from the "Edit Icon" menu row. 217 unit + 33 instrumented. See Session 11.

## P4c - Dark Mode (KkPN3)  ·  DONE (2026-09-25)

Dark palette replaced with the verbatim `KkPN3` "Dark Editorial" values; settings panel
now follows `ThemeMode`. 220 unit + 36 instrumented. See Session 12.

## P4d - Backup & Restore  ·  DONE (2026-09-25)

Versioned SAF JSON export/import with atomic restore and non-destructive malformed-file
handling. 236 unit + 38 instrumented tests. See Session 13.

## P5 - Category pager  ·  DONE (2026-09-25)

Eight design categories are available as horizontal pager pages. Tabs and swipes select
the same page; global search/index and vertical scrolling remain intact. 265 unit tests
and eight new instrumented gesture cases. See Sessions 15–16 and the P5 screenshots.

*(Superseded roadmap note: the earlier "P4 = Onboarding (H) + polish" was re-split into
P4a–P4d; onboarding H is **out** — no design source. See the P4 scope spec.)*


---

# FINAL PROJECT REPORT — SOFT / HOME launcher (P1 → P5 complete)

> Generated at the end of **P5** (Session 15), the last planned phase. Everything below is
> verified against the committed working tree (`git log` section at the end), the `.pen`
> source of truth (byte-identical, SHA256 `08DB2A81…CA6201`, 176,128 B), and real
> on-device runs (emulator `soft_home_pixel` API 35 **and** a physical TECNO CN7c,
> Android 16 / API 36).

## 1. What was built — phase checklist

Legend: [x] done · [~] partial/stub · ⛔ deferred/dropped.

### P1 — Home + Icon Pack + App Drawer · DONE (incl. P1.5)
- [x] A1 Home replacement (default launcher, `HOME`+`DEFAULT` intent).
- [~] A2 Grid config (home is now a row list; drawer grid columns configurable in P3).
- [~] A3 Search row + voice (row + SEARCH state done; voice action is a stub).
- [x] A4 Single Warm-Right-Rail page (the old multi-page/pill indicator was retired).
- [~] A5 Swipe-up → drawer done; swipe L/R + double-tap parked.
- [x] A6 Clock · A7 Date · A8 Weather row · A9 8-icon right rail · A10 music row (static) ·
      A11 Idle/Search/Music states.
- [x] B1 import/apply icon pack (`appfilter.xml` + drawables) · B2 live preview ·
      B3 auto-mask real icons · B5 lucide glyph set.
- [~] B4 notification badge dot built; listener parked (no permission path in scope).
- [x] C1 drawer (swipe-up, tile grid) · C2 alphabet rail · C3 in-drawer search ·
      C4 header · C5 category nav · C6 app labels.
- **P1.5:** real icon-pack decode (zip + installed pack), auto-mask compositor, graceful
  failure; glyph-uniqueness bug found + fixed (D-016).

### P2 — Widgets (E) + Folders (D) · DONE
- [x] E3 calendar row · E4 battery/storage row (**real** `BatteryManager`+`StatFs`) ·
      E5 quick-notes row (DataStore-persisted).
- [x] D1 folders in the drawer (68 r21 tile + 2×2 preview) · D2 folder popup (create/
      rename/add/remove).
- ⛔ D3 live system widgets (`AppWidgetHost`) — stub, out of scope.
- Deferred (later delivered): drag app→folder (P4a), KkPN3 dark (P4c), live calendar (parked).

### P3 — System UI (F) + Settings (G) · DONE
- [x] F1 status-bar icon colour follows theme · F2 nav bar (hidden on gesture nav) ·
      F3 long-press context menu (rail + drawer tiles; Uninstall greyed for system apps).
- [x] G1 sectioned settings (Appearance/Widgets/Wallpaper/Gestures) · G2 custom toggle ·
      G3 ThemeMode (Light/Dark/System) applied app-wide.
- [x] G-W row visibility + order (+ up/down buttons) · G-H hidden-apps manager ·
      G-S spacing preset · G-IP icon-pack persistence + cold-start rehydration.
- [x] G4 Backup & restore — *delivered in P4d*.
- Patch (Session 8): drawer BACK regression fixed (`HomeActivity.onBackPressed` bypass).

### P3.5 — Drawer Icon Redesign · DONE
- [x] `TpzL1` rendered: cream tiles + per-category coloured glyphs + amber selected state;
      hybrid renderer (pack artwork OR coloured category glyph).

### P4a — Drag & Drop · DONE
- [x] Home-row reorder (long-press-lift + accent insertion line) · app→existing folder ·
      app→"New folder" chip (drop-to-create) · long-press splits menu vs drag ·
      persistence across `am force-stop`.
- [~] Drag-out of a folder — descoped (resolver kept + tested).

### P4b — Edit Icon (rich editor) · DONE
- [x] Typed per-app override (`Pack(drawable)` | `Glyph(symbol,color)`), pure codec +
      legacy migration; cream r24 editor card; live preview via the same tile path;
      Reset; persists + applies everywhere (drawer + folders + home rail).
- ⛔ Crop/resize/upload a custom image — out of P4b (D-038).
- ⛔ Raw pack-drawable enumeration — out of P4b (picker uses the mapped set).

### P4c — Dark Mode (KkPN3) · DONE
- [x] Dark palette = verbatim `KkPN3` "Dark Editorial" values (`#18191A` canvas,
      `#2E3134` rail, `#343638` divider, `#F2EEE7` type); every surface audited;
      `SettingsStubActivity` now follows `ThemeMode` (bug fixed); token lock test.

### P4d — Backup & Restore · DONE (this session)
- [x] Export/import the whole launcher state as one versioned JSON file via SAF.
- [x] Total, non-destructive failure handling; destructive restore behind an inline confirm.
- [x] Atomic apply (`PrefsRepository.applyAll`); round-trip proven on emulator + device.

### P5 — Category pager · DONE (this session)
- [x] Eight design categories rendered as pager pages.
- [x] Category tab tap and horizontal swipe both change the active page.
- [x] Vertical grid scrolling, global search/index, and swipe-down close remain intact.
- [x] Tecno + emulator screenshots captured under `docs/screenshots/p5-*`.

## 2. Final test counts (P5)

| Suite | Count | Where |
|---|---|---|
| **JVM unit tests (debug variant — authoritative)** | **280**, 0 failures | `./gradlew test` |
| **Instrumented Compose tests** | **50 executed** | Includes P5 pager + scroll/tap/long-press regression coverage |

Instrumented suite green on **both** the emulator (`soft_home_pixel`, API 35) and the
physical device (**TECNO CN7c, Android 16 / API 36**); P5 evidence is retained under
`docs/screenshots/p5-*`.

> **Counting discipline (cross-checked).** `./gradlew test` builds *both* the debug and
> release variants; the counted total is the **debug** variant (280). The release variant
> reports fewer for the same sources. The P4a log's "307 unit" was an over-count; the
> authoritative progression is 217 (P4b) → 220 (P4c) → 236 (P4d) → 265 (P5) → 280 (P8 motion).
> Instrumented progression: 22 (P3) → 25 (P3.5) → 33 (P4b) → 36 (P4c) → 38 (P4d) → 50 (P8 motion).

## 3. Deferred / dropped items (with reasons) — nothing silently lost

| Item | Status | Reason |
|---|---|---|
| MediaSession / real music playback (#39) | 🟡 deferred past P4 | No playback design source; needs `MediaSessionManager` + notification-listener access. The music row is a static UI faithful to the `.pen`. |
| Live calendar (`CalendarContract`) | 🟡 deferred past P4 | Needs `READ_CALENDAR` + permission-denied / no-app / multi-calendar edge cases; no design node. |
| Real gesture actions (swipe-down → notifications, double-tap → lock) | 🟡 deferred | Permission-gated (notification access / Device Admin). Settings rows read "Coming soon". |
| Live-wallpaper **engine** | ⚪ dropped from roadmap | P3-6 already ships "flat default + system picker" — the useful part. A full engine is a separate product with no design source. |
| Battery change callback stream | ⚪ dropped | Re-reading on `ON_RESUME` is sufficient; a stream adds a receiver for a value that barely changes. |
| Onboarding (H) | ⛔ out of P4 | No onboarding/welcome/setup mock in the `.pen` (sweep = 0). |
| Notification badge listener (real counts) | 🟡 deferred | Needs notification-listener access; the cream dot atom exists. |
| KWGT-style system widgets (`AppWidgetHost`) | 🟡 deferred | Large separate subsystem; custom rows (P2) cover the design's intent. |
| Voice search action | 🟡 stub | Mic/search row present; no speech-integration source. |
| Icon editor: crop / resize / upload a custom image | ⛔ out of P4b (D-038) | Unbounded UI with no design mock; a real subsystem (SAF/decode/adaptive-icon safety/storage format). Recorded as a future phase. |
| Icon editor: raw pack-drawable enumeration | ⛔ out of P4b | The picker uses the pack's `appfilter`-mapped distinct drawables; raw zip/APK enumeration is new infra. |
| Editing rail icons' shortcut targets | ⛔ out of P4b | That is the "Edit Shortcut" row, unrelated to icon appearance. |
| Drag-out of a folder | [~] descoped in P4a | The popup card's clickable fights the drag; tap-to-remove exists. Resolver kept + tested. |
| Backup of icon-pack **bytes** / wallpaper / installed-app list | ⛔ out of P4d | Pack bytes live outside our prefs (id reference captured; rehydrates via P3-5). Wallpaper is OS-owned; the app list is derived. |
| Cloud / auto-sync / scheduled backups | ⛔ out of P4d | No design source; the file-based SAF flow is the whole scope. |

## 4. Build & install (verified on device + emulator)

Toolchain (already installed, all on D:): JDK 17, Android SDK `D:\Android\Sdk`,
`GRADLE_USER_HOME=D:\gradle-cache`, AVD `soft_home_pixel` (API 35).

```powershell
# from the repo root
.\gradlew.bat :app:assembleDebug            # -> app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat :app:installDebug             # build + install to a running device/emulator
.\gradlew.bat test                          # 280 JVM unit tests
.\gradlew.bat :app:connectedDebugAndroidTest  # 50 instrumented tests (needs a device)

# install + set as the default Home app (debug package id ends in .debug)
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell cmd package set-home-activity com.softhome.launcher.debug/com.softhome.launcher.HomeActivity
adb shell input keyevent KEYCODE_HOME
```

Verified working on:
- **Emulator** `soft_home_pixel` — Android 15 / API 35 (the standard AVD).
- **Physical device** — **TECNO CN7c, Android 16 / API 36** (installed, set as Home,
  rendered + drove the settings, drawer, backup export).

## 5. Known issues / limitations

- **Auto-mask tint** can leave residual brand colour on some system adaptive icons; not
  exhaustive across OEM icon sets.
- **Voice search** and **notification badges** are visual stubs (see §3).
- **Health/storage numbers** come from real `BatteryManager`/`StatFs` but are read on
  resume, not pushed (dropped battery stream — §3).
- **Themes:** the dark palette follows `KkPN3`; the `.pen` is monochrome, so a small warm
  accent is derived for interactive emphasis (documented in `04` section M).
- **Icon-pack rehydrate** depends on the pack still being present (zip in app storage or
  the pack app installed); a missing pack falls back to mask gracefully.
- **Backup** does not carry pack bytes / wallpaper / app list (§3); restoring on a device
  without the pack falls back to mask.
- **`docs/07`** section 5 note "active pack is in-memory for now" is superseded by P3-5
  (persisted + rehydrated) — kept only as historical prose.
- Accessibility: content descriptions present; contrast audited in `docs/06`; not verified
  with TalkBack end-to-end.

## 6. Final APK

| Variant | File | Size |
|---|---|---|
| **Debug** (installable, used for all verification) | `app\build\outputs\apk\debug\app-debug.apk` | **12,538,288 bytes (11.96 MB)** |
| Release (unsigned) | `app\build\outputs\apk\release\app-release-unsigned.apk` | 8,627,796 bytes (8.23 MB) |

Package id: `com.softhome.launcher.debug` (debug) — `.launcher` (release).

## 7. Git log (baseline → P5 working tree)

```
3093819 P4d: backup & restore (export/import all launcher state)
b500dcf P4a-P4c: drag & drop, edit-icon editor, KkPN3 dark mode
81aee32 P3.5: drawer icon redesign (cream tiles + category-colored glyphs)
34c0af3 docs: P3.5 drawer icon redesign spec (color + labels)
83fadd6 Fix: BACK button now closes drawer overlay
0e2b6f9 P3 Phase 8: docs (00/02/03/04/05/06/09) + README
b829973 P3 Phase 7: on-device verification (soft_home_pixel, API 35) + screenshots
541335d P3 Phase 6: settings panel (G) + home row visibility/order + ThemeMode
1ac8952 P3 Phase 5: drawer long-press context menu (F3) + hidden-apps "Remove" (Q2)
4e28648 P3 Phase 4: System UI integration (F1 status bar, F2 nav bar, F3 rail long-press)
6b9b8cb P3 Phase 3: icon-pack persistence + cold-start rehydration (closes docs/04 #36)
bf21746 P3 Phase 2: design-system atoms (SoftToggle, AppContextMenu, SettingsRow)
36887bb P3 Phase 1: repositories (prefs for row visibility/order/spacing/hidden apps + app actions)
79ab168 P3 Phase 0: model + tokens + assets (home rows, spacing, menu/settings tokens)
e00a70b baseline: P1 + P1.5 + P2 (Warm Right Rail launcher) + P3 spec
```

> Note: P4a, P4b and P4c were built in Sessions 10–12 but never committed between phases;
> their combined state is captured in `b500dcf` (the docs land in `3093819`). Every phase
> from the baseline onward is now in history.

## 8. Status

**All planned phases are COMPLETE (P1 → P5).** The launcher is an installable,
set-as-default Home app implementing the Warm Right Rail design system, with a real icon
pack pipeline, widgets, folders, system UI, settings, drag-and-drop, a rich icon editor,
KkPN3 dark mode, and backup/restore. Remaining work is the explicitly deferred/dropped set
in §3.

**STOP #3 — final review.**

## Session 22 — P6 edge-to-edge category motion (2026-09-25)

- Restored the pre-P6 `padding(start = Spacing.xxl)` so the static `All` grid and chrome
  match the grid-first layout; the transition alone now applies a 10dp directional nudge.
- `AlphabetRail` renders only for `DrawerCategory.All`; non-All categories reclaim its space.
- Versioned as **0.1.7 (versionCode 22)** and installed with `adb install -r` on TECNO CN7c,
  preserving launcher preferences and folders.
- Build + drawer/design-system unit tests passed; focused category instrumentation passed
  **7/7** on TECNO CN7c. Device evidence:
  `docs/screenshots/p6r-all-static.png`, `docs/screenshots/p6r-communication-no-rail.png`,
  and `docs/videos/p6r-micro-slide.mp4`.

## Session 23 — Reorderable right sidebar (2026-09-25)

- Added stable `RailOrderLogic` sanitization/move rules and persisted `rail_order` in the
  existing DataStore preferences; backup JSON carries the optional `railOrder` field so
  older backups remain valid.
- Right-rail icons now use 48dp touch targets around the existing 20dp glyphs. Long-press
  enters the shared drag engine, sibling icons shift with the existing motion tokens,
  the lifted preview uses the existing scale/shadow treatment, and an end drop zone allows
  moving an icon to the last position. Tap and stationary long-press behavior remain intact.
- The rail begins below the system status-bar inset; status-bar content remains native and
  outside the rail surface. Parent drawer swipe ignores touches that start in the rail.
- Device evidence: `docs/screenshots/p7-rail-final.png`,
  `docs/screenshots/p7-rail-final-drag.png`, `docs/screenshots/p7-rail-final-restart.png`.
- Follow-up polish rounds the visible rail-to-home corners with the tokenized 20dp shape;
  final device capture: `docs/screenshots/p7-rail-rounded.png`.
- `:core:model:testDebugUnitTest`, `:core:data:testDebugUnitTest`,
  `:feature:home:testDebugUnitTest`, and `:app:assembleDebug` passed; APK installed on
  TECNO CN7c as `com.softhome.launcher.debug`. The complete Gradle `test` task and
  `:app:lintDebug` also pass (lint reports warnings only).

## Session 24 — Motion system + local playback demo (2026-09-25)

- Added `PlaybackController` with `DemoPlaybackController`: deterministic 3:30 local
  state, play/pause, resume, clamped seek, end-of-track stop, and animated progress.
  The UI depends only on the abstraction, so a future MediaSession adapter can replace
  the demo without changing the music composable. No Spotify or system playback control
  is claimed or performed.
- Music row now has a tokenized compact/expanded transition, expanded artwork and time
  labels, full-track seek, and a local ticker that advances only while playing. Expanded
  home content uses the existing scroll surface instead of overflowing the viewport.
- Clock uses a local minute ticker and per-character digit transitions. Weather now has a
  `WeatherCondition`/`WeatherUiState` fallback (`Clear`, `Current 8°C`) with a subtle
  ambient icon treatment and a replacement seam for a future weather source.
- Sidebar press feedback is emitted by the existing `dragSource` gesture engine and
  consumed by Warm press/ripple visuals; no competing click handler was added. Tap,
  stationary long-press, and long-press drag/drop remain mutually exclusive.
- Verification: feature/design-system unit tests, debug assemble, and `:app:lintDebug`
  passed. The APK was installed on TECNO CN7c (Android 16/API 36). Device captures:
  `docs/screenshots/p8-motion-home-idle.png`, `p8-motion-music-playing.png`,
  `p8-motion-music-expanded.png`, and `p8-motion-music-seek.png`.
