# 09 - Decisions Log

Short, ADR-style entries. Newest on top. Each entry: **what**, **why**, **impact**.
Append-only.

---

### D-042 - Backup/restore is SAF + one versioned JSON document (P4d)
- **What:** Backup & Restore (the last P4 sub-phase, G4) exports/imports the whole
  launcher user state as **one JSON file** with a schema envelope
  (`{ "app":"SOFT_HOME", "version":1, "exportedAt":…, "prefs":{…}, "folders":"…", "notes":"…" }`),
  transported through the **Storage Access Framework** (`CreateDocument` / `OpenDocument`,
  MIME `application/json`). A restore **replaces** state wholesale, behind an inline
  confirm. Failure is total + non-destructive: a foreign/corrupt/newer file returns a
  typed result and changes nothing.
- **Why:** every persisted field already has a typed model + a pure codec + a typed
  setter, so P4d is "one serializer + one apply" -- no new storage engine. SAF (the same
  pattern as the P1.5 icon-pack zip) needs no storage permission on any API level and
  works on both emulator and a physical device.
- **Impact:** new `BackupDocument` (`core:model`), `BackupCodec` +
  `BackupRepository`/`Impl` (`core:data`), `PrefsRepository.applyAll` (one atomic write),
  and a "Backup & restore" section in the P3 settings panel. The active icon pack is
  backed up as an **id reference** only (rehydrated via the P3-5 cold-start path); the
  pack's bytes, the wallpaper, and the installed-app list are **not** backed up.

---

### D-041 - Restore replaces state and is gated behind a confirm (P4d)
- **What:** an import **replaces** all persisted launcher state (rows, spacing, hidden
  apps, icon overrides, folders, notes, theme, active pack id). It is preceded by a
  confirmation step (the file is decoded first; only a valid SOFT / HOME backup offers
  "Replace all current settings with this backup?"). A foreign file shows a message and
  changes nothing.
- **Why:** the point of a backup is to reproduce a known state; a partial merge would be
  surprising. But restore is destructive to the current state, so it must be explicit and
  reversible (restore an older file). Mirrors the P4a-10 "predictable, non-destructive
  until confirmed" principle.
- **Impact:** `SettingsViewModel.confirmRestore/cancelRestore` + `pendingRestore` state;
  the settings panel renders the confirm row + Restore/Cancel pills.

---

### D-040 - Backup format is a versioned JSON document (P4d)
- **What:** the on-disk backup is a single JSON object carrying an `app` marker
  (`"SOFT_HOME"`), an integer `version` (`CURRENT_VERSION = 1`), and the three state
  sections. Nested structured values (`homeRows`, `iconOverrides`, `folders`) are stored
  as the **strings their own codecs already produce**, so each sub-shape has exactly one
  encoder.
- **Why:** a marker + version make a foreign or future file *refuseable* instead of
  silently mis-decoded (P4d-4/P4d-10). Reusing the item codecs prevents a second, drifting
  encoding of the same data.
- **Impact:** `BackupCodec.decode` is total -- `Ok` | `NotABackup` | `UnsupportedVersion`
  | `Malformed` -- and never throws. `BackupCodecTest` pins round-trip + every failure
  path.

---

### D-039 - Dark mode is the `KkPN3` "Dark Editorial" named palette (P4c)
- **What:** the dark palette is replaced by the **verbatim `KkPN3` frame values**
  (screen bg `#18191A`, rail `#2E3134`, divider `#343638`, primary text `#F2EEE7`,
  soft text `#E2DDD5`, muted `#918F8B`, progress track `#676866`, …). A few surfaces the
  frame does not show (menu/popup/drawer cards) are derived from the same neutral family
  and documented. `SettingsStubActivity` now follows `ThemeMode` (Light/Dark/System) and
  matches its status/nav bar appearance to the resolved theme (was hardcoded light).
- **Why:** P1–P3 shipped a DERIVED warm-dark guess (assumption #5, bg `#1F1D1A`). The
  `.pen` actually names a concrete, cooler/neutral dark palette in `KkPN3`; P4c renders
  it faithfully instead of the guess.
- **Impact:** the `Dark*` tokens in `Color.kt` are re-pointed to the `KkPN3` values; a
  new `DarkPaletteTest` pins them; dark surfaces (home, drawer, settings, context menu,
  folder popup, editor) were re-checked on device. `ThemeMode` plumbing unchanged.

---

### D-038 - P4b "rich editor" is pick-from-what-exists (no crop/upload)
- **What:** the P4b icon editor offers **pack drawable** (from the active pack's mapped
  set) or **glyph + color** (for non-pack apps), plus **Reset**. It does **NOT** crop,
  resize, or upload a custom image.
- **Why:** the `.pen` has **no** editor mock (byte-identical re-audit; keyword sweep 0),
  so a crop/upload flow would be unbounded UI with no source. A custom-image path is a
  real subsystem (SAF/decode/downscale/adaptive-icon safety/storage format). The P3 Q4
  promise ("pick from the active pack's drawables") and the P4 scope text ("pack drawable
  **or** category glyph; recolor; reset") are fully satisfied without it.
- **Impact:** crop/upload is recorded as an explicit future phase, not lost. The pack
  picker lists the pack's **mapped distinct drawables** (not raw zip/APK enumeration).

### D-037 - Per-app icon override is a typed choice (P4b)
- **What:** `LauncherPrefs.iconOverrides` changes from `Map<componentKey, packId>` to
  `Map<componentKey, IconOverride>`, where `IconOverride` = `Pack(drawableName)` |
  `Glyph(symbolName, colorToken)`. Absence = automatic (the P3.5 hybrid). `IconSource`
  gains a `Glyph` variant; `ResolvedIcon` gains `overrideColorToken`. `IconResolver`
  consults the override **first**; the drawer renderer honors a chosen glyph color.
- **Why:** the old shape **could not express a choice** -- the resolver re-derived the
  pack's default entry, so an "override" was visually a no-op, and there was no UI or
  setter at all (the "Edit Icon" row closed the menu). This is the minimal honest model
  that makes "Edit Icon" mean something.
- **Impact:** new `IconOverridesCodec` (pure, total decode + legacy migration);
  `PrefsRepository.setIconOverride`; `IconEditor` (pure state machine); `IconEditorSheet`
  + `IconEditorCard`/`ChoiceTile`/`ColorSwatchRow` atoms; the override applies in the
  drawer **and** the home rail (one resolver path). +20 unit tests, +6 instrumented.

---
- **What:** `Modifier.dragSource` / `Modifier.dropTarget` / `DragPreviewLayer` /
  `DragInsertionLine` (in `core:designsystem`) form **one** gesture engine. Two adapters
  sit on top: home-row reorder (`feature:home`) and drawer app→folder
  (`feature:appdrawer`). The pure drop math lives in `core:model`
  (`HomeRowDropResolver`, `FolderDropResolver`).
- **Why:** the two surfaces (a `Column` of unequal rows vs. a mixed-cell
  `LazyVerticalGrid`) cannot share a layout, but they share gesture math, the lift
  animation, and the preview visual. Splitting the layout keeps hit-testing correct
  while keeping one interaction language.
- **Impact:** +18 JVM tests (`DragAndDropStateTest`, `DragDropResolverTest`); the engine
  is Compose-only and covered by instrumented tests.

### D-036 - Long-press splits into menu vs. drag by movement (P4a)
- **What:** a node that is both tappable and draggable uses a **single** gesture owner
  (`Modifier.dragSource`) instead of stacking `combinedClickable` on top of a drag
  detector (which fight over the press). Behaviour: quick press → `onTap`; long-press
  with **no movement** → `onLongPress` (context menu); long-press **with movement** →
  drag, `onDrop` on release.
- **Why:** the P3 context menu already used long-press on drawer tiles while P4a needs
  long-press to drag. Two detectors on one node produced a grey press-ripple with no
  drag. One owner removes the ambiguity.
- **Impact:** `DrawerAppScreen.AppCell` uses `dragSource(onTap, onLongPress, onDrop)`;
  the P3 long-press menu is preserved (verified on device).

### D-033 - Drawer icons use a hybrid color source (P3.5)
- **What:** in the app drawer, an app with an active-pack entry renders the **real
  drawable untinted** (its own colors); an app **not** in the pack renders a
  **category-colored lucide glyph** (`DrawerIconColor` → `SoftColors` drawer token).
  The charcoal auto-mask is **not used in the drawer** (kept for other surfaces).
- **Why:** the `TpzL1` frame specifies a colorful grid; auto-mask charcoal would
  clash with cream tiles. The hybrid keeps pack artwork faithful while giving every
  app a colored fallback.
- **Impact:** `DrawerAppIcon` (new), `DrawerIconColor` (pure, tested); 7 existing
  pipeline tests untouched.

### D-032 - Drawer icon color system (P3.5)
- **What:** the drawer renders **cream `#F6F0E7` tiles** with **per-category colored
  glyphs** (7 color tokens from `TpzL1`), replacing the monochrome charcoal squircle.
  A **selected/active tile** uses **amber `#D19B62`** + cream glyph. The placeholder
  `DrawerTint*` tokens are removed.
- **Why:** `TpzL1` is titled "Unique Icon Grid" — it was always meant to be colorful;
  P1–P3 deferred the tint. P3.5 renders the frame faithfully before P4's Edit Icon.
- **Impact:** 10 new `SoftColors` fields (light + dark), `DrawerIconTile` atom,
  `DrawerColorsTest` (token coverage), `DrawerIconColorTest` (mapping).

---

### D-031 - Home rows are user-configurable with a locked minimum (P3 / Widgets)
- **What:** `HomeRowLogic` + `LauncherPrefs.homeRows` (visibility + order). Locked set =
  Time / Date / Weather (P3-1); the rest toggleable. Reorder via **up/down buttons** in
  the settings Widgets section (P3-2); drag-and-drop deferred to P4.
- **Why:** after the redesign the home is a row list; users want to control which rows
  show. A locked set guarantees the home never renders empty.
- **Impact:** `HomeStateTest`/`HomeRowLogicTest`; `HomeScreen` renders `visibleRows`.

---

### D-030 - Long-press menus are row-style cards on rail icons + drawer tiles (P3 / F3)
- **What:** long-press a rail icon or a drawer tile → `AppContextMenu` (cream r24 card
  over a dim scrim, 1px divider rows): Open / App Info / Edit Icon / Remove / Uninstall /
  Shortcuts. **Uninstall is greyed** for system/non-removable apps (P3-4). Not the
  platform `PopupMenu`.
- **Why:** the Warm Right Rail system has no card/shadow language; a row-style card keeps
  one visual vocabulary. Greyed uninstall avoids throwing `ACTION_DELETE` on system apps.
- **Impact:** `AppContextMenu` atom; `AppActionLogic` + `AppActionsRepository`;
  verified on device (greyed Uninstall for the system "Settings" app).

---

### D-029 - Settings panel + custom pill toggle (P3 / G)
- **What:** sectioned settings (Appearance / Widgets / Wallpaper / Gestures) built from
  `SettingsRow` + the `SoftToggle` pill (42×22, charcoal, `Role.Switch`); the rail
  `panel-left` icon opens it (Q1); a "System settings" row opens the OS screen.
  `ThemeMode` persists and is applied app-wide.
- **Why:** the `UPa9N` toggle/settings mocks are from the retired P1 file; the panel is
  built from the current token set. Custom toggle keeps the charcoal language.
- **Impact:** `SettingsViewModel`/`SettingsPanel`; `SettingsCyclesTest`.

---

### D-028 - Active icon pack persists and rehydrates on cold start (P3-5, closes #36)
- **What:** `IconPackRepositoryImpl` persists the chosen pack id and rehydrates it on
  cold start (imported zip → installed pack → newest-zip fallback); a dead id is cleared
  and the launcher falls back to auto-mask. Never crashes.
- **Why:** P1.5 kept the active pack in memory only; the choice must survive process death.
- **Impact:** `IconPackPersistenceTest`; `IconPackRef` is the single zip-id derivation.

---

### D-027 - Status/nav bar follow the app theme; nav bar hides under gesture nav (P3 / F1/F2)
- **What:** `SystemBarAppearance` sets `isAppearanceLightStatusBars`/
  `isAppearanceLightNavigationBars` from `ThemeMode` (dark icons on cream, light on
  warm-dark) over transparent bars, and hides the nav bar with transient reveal under
  gesture navigation (best-effort detection; degrades to "show").
- **Why:** the design's status content is a mockup; the real bar must read as "quiet" on
  the cream surface and must not fight the home.
- **Impact:** `SystemBarAppearanceTest`; verified light + dark on device.

---

### D-026 - "Remove" hides an app from the drawer; wallpapers use the system picker (P3 / Q2, P3-6)
- **What:** the drawer long-press "Remove" adds the component key to a persisted
  `hiddenApps` set (restorable from the settings "Hidden apps" manager); the Wallpaper
  section opens the **system** picker (`ACTION_CHANGE_LIVE_WALLPAPER` →
  `ACTION_SET_WALLPAPER`) over a flat warm default. The live-wallpaper engine stays
  deferred.
- **Why:** Q2 chose reversible hide-from-drawer (folders already cover
  remove-from-folder); P3-6 keeps wallpaper selection without a wallpaper engine.
- **Impact:** `PrefsRepository.hideApp/unhideApp`; `WallpaperIntents`.

---

### D-025 - P2 widgets are Warm Right Rail rows (old cream-card widgets removed)
- **What:** calendar / battery-storage / quick-notes are full-width `HomeRow`s
  (`HomeWidgetRows.kt`), not cards. Removed `SoftWidgets.kt` (`SoftWidgetCard`,
  card `CalendarWidget`/`BatteryWidget`/`NotesWidget`, `ThinProgressBar`,
  `FolderPreviewTile`).
- **Why:** the `.pen` has no widget mock; the Warm Right Rail redesign retired the
  cream-card / grid-squircle language, so the P1 widget scaffold was dead-but-wrong.
- **Impact:** `docs/04` section I #48-49; `docs/03` E1-E5 updated.

---

### D-024 - Battery/storage read REAL device values
- **What:** `DeviceStatusRepository` reads `BatteryManager.BATTERY_PROPERTY_CAPACITY`
  (with a sticky `ACTION_BATTERY_CHANGED` fallback) and `StatFs(getDataDirectory())`;
  the row re-reads on `ON_RESUME`.
- **Why:** pure Android APIs, no permission; same effort as static values but useful.
- **Impact:** `CoreDataTest` covers the storage math + battery read; verified on
  device against `dumpsys battery` (100%) and `df` (19%). A live battery callback
  stream is deferred to P3.

---

### D-023 - Quick notes are tap-to-expand and DataStore-backed
- **What:** the notes row grows in-place (`MotionTokens.notesExpand()`, 280ms) into
  an editor; the body persists via `NotesRepository` (DataStore key `quick_notes`).
- **Why:** progressive disclosure like Search/Music; notes are **user data** and must
  survive process death (unlike the static music row).
- **Impact:** `HomeState.Notes` added; verified persisted after `am force-stop`.

---

### D-022 - Folders live in the App Drawer, not the row home
- **What:** folders are a drawer concept (`FolderLogic` builds drawer grid items;
  `FolderTile` = 68 r21 tile + 2×2 preview; `FolderPopupBody` = cream r24 popup).
  The row-based home has **no** folder element.
- **Why:** after the redesign the drawer is the only grid surface; folders are a
  grid-native concept and forcing them into the row home would invent a layout.
  Drag-and-drop is deferred to P3 ("New folder" + assign-from-popup ships now).
- **Impact:** `FolderLogicTest`, `FolderRepositoryTest`, `FolderCellsTest`; verified
  create/fill/persist on the emulator.

---

### D-021 - Music player is a static UI component (MediaSession deferred)
- **What:** `MusicPlayerRow` renders the design's dummy values ("play music." /
  Djo / End of Beginning · Live from Chicago) with no playback wiring; the play
  button is a no-op.
- **Why:** the `.pen` specifies the player UI but no MediaSession/playback behaviour.
- **Impact:** real media integration is a later task; the component is structured to
  accept it (see `04` section H #39).

---

### D-020 - Right rail icons are shortcuts to default device apps
- **What:** the 8 rail icons (`hrsLU`) map to the device's default app per role
  (browser / messaging / email / camera / weather / settings / dialer); `sparkles`
  is decorative (no-op). Unresolvable icons render but do nothing.
- **Why:** the design shows a quick-access rail but names no actions.
- **Impact:** `RailShortcut` + `RailShortcutResolver` (cached, pure-resolvable);
  no crash when an app is missing.

---

### D-019 - App drawer redesigned (kept, not replaced by the rail)
- **What:** the drawer (`TpzL1`) is kept as a 4-column grid + alphabet rail but
  redesigned: bg `#DCCDBA`, tile 68 r21, labels under tiles, a category nav
  (All/Communication/Entertainment/Tools), search pill h56 r28.
- **Why:** the new design still includes the drawer; the right rail is a separate
  home element, not a drawer replacement. Categories come from `ApplicationInfo.category`.
- **Impact:** `AppDrawerViewModel` gained category state; `DrawerCategory` +
  `DrawerCategoryMapper` in `core:model`. Icons still use the icon-pack pipeline
  (the colored glyphs in `TpzL1` are example pack art).

---

### D-018 - Motion system as tokens (`MotionTokens.kt`)
- **What:** RAIL SLIDE 220ms, SEARCH FADE 160ms, MUSIC RISE 280ms, ease
  cubic-bezier(0.2, 0.8, 0.2, 1) — from `.pen` card `Z5V7Y9`.
- **Why:** the design introduces a motion system with the principle "small movement,
  clear purpose"; tokenizing it keeps animations consistent and reviewable.
- **Impact:** no screen hardcodes durations/easings; `MotionTokensTest` guards values.

---

### D-017 - Home screen is a row list + right rail (grid squircle retired)
- **What:** `HomeScreen` rewritten from the 4-column squircle grid to a vertical
  list of full-width rows (time → date → weather → search → music) separated by 1px
  `#D0C2B1` dividers, plus a 72dp right icon rail. Old atoms (`ClockBlock`,
  `WeatherCard`, `MicSearchPill`, `PageIndicator`, `HintText`) removed.
- **Why:** the `.pen` was updated (frames `znb90` / `L7ZAp`); the previous grid layout
  and its node IDs no longer exist.
- **Impact:** 3 interaction states (Idle/Search/Music) via tap-row triggers; the
  swipe-up layer moved **behind** the home content so taps reach the rows. Home
  typography uses normal weight (clock 58, date 60). Grid tokens are legacy.

---

### D-016 - Icon glyph uniqueness hardening (P1.5 correction)
- **What:** Fixed AutoMask / compositor so per-app glyphs cannot collapse into
  identical charcoal (or cream-blob) tiles. Changes:
  1. `IconCompositor` rejects unusable AdaptiveIcon **monochrome** layers
     (alpha coverage outside 4%-72%) and falls back to drawing the **full**
     `AdaptiveIconDrawable` + luminance tint instead of `SRC_IN` on a blob.
  2. `IconBitmapProvider` rejects composites with no visible non-tile pixels
     (charcoal-only / full-bleed cream) so `AppIcon` falls through to the
     category glyph instead of painting a blank ring.
  3. `AppIcon` includes `sizePx` in `remember` keys (prevents locking a 1px /
     empty pack bitmap forever) and falls through FromPack → AutoMask → glyph
     when decode fails.
  4. SoftHome launcher adaptive assets fixed: separate black-on-transparent
     `ic_launcher_monochrome` (old mono reused a full-cream-fill foreground,
     which `SRC_IN` flattened into a solid cream square).
  5. Regression suite `IconCompositorGlyphUniquenessTest` asserts different
     source glyphs → different `fingerprint()` values, and rejects blank/blob.
- **Why:** P1.5 verification checked squircle/radius/colour and claimed per-app
  glyphs matched `.pen`, but did **not** assert bitmap fingerprints differ across
  packages. SoftHome's bad mono layer made at least one tile a cream square; a
  charcoal-only composite would have looked like an identical "donut" for every
  failed mask. The prior "verified" claim in `05-PROGRESS` is corrected (not
  deleted) under Session 4.
- **Impact:** AutoMask is stricter; some apps with near-full mono blobs now use
  full-adaptive tint or the category glyph. Pack path unchanged when drawables
  load. Pipeline logs (`SOFTHOME_PIPELINE`) emit package → source → fingerprint
  for device cross-checks. Screenshots:
  `docs/screenshots/drawer-after-pack.png`,
  `docs/screenshots/comparison-grid-pack.png`,
  `docs/screenshots/home-after-pack.png`.
- **Honesty note:** On current `soft_home_pixel` with SoftMonoTest active, all 15
  visible drawer tiles show distinct pack glyphs and 15 unique log fingerprints.
  The reporter's "16 identical charcoal rings" state was **not** reproduced after
  the SoftMonoTest zip with 15 unique PNGs was present; the SoftHome cream-square
  AutoMask failure **was** reproduced and fixed. Remaining uncertainty: AutoMask
  on some OEM icons can still leave residual brand colour (tint strength) — not
  claimed as pixel-perfect vs `.pen`.

---

### D-015 - Icon pack source modelled explicitly (Zip / InstalledPack / Assets)
- **What:** `IconPackSource` is a sealed type; `IconPack` carries `source`,
  `sourceKind`, `drawableCount`. `IconPackDrawableLoader` decodes per kind.
- **Why:** A pack can arrive as a user-picked zip, an installed pack APK, or bundled
  assets - each needs a different resolution path. Encoding the source makes the
  decode explicit and testable, and lets the UI show "Imported .zip" vs "Installed app".
- **Impact:** Adding a source = one branch in the loader; the resolver stays source-blind.

---

### D-014 - Auto-mask composites the app's REAL icon (cream monochrome)
- **What:** Apps without a pack entry render their real launcher icon, inset at 0.62,
  composited into the charcoal squircle and tinted to cream (`AutoMask` +
  `IconCompositor` + `IconBitmapProvider`). The category glyph is now fallback-only.
- **Why:** The design's premise is that *every* app looks like one family. A leaf
  glyph for unknown apps broke that; masking the real icon keeps recognisability while
  matching the monochrome treatment.
- **Impact:** `IconBitmapProvider` caches masked bitmaps (LRU 128); decode always
  off the main thread; failure degrades to the glyph, never a blank tile.

---

### D-013 - Zip import is streamed, validated, and progress-emitting
- **What:** `IconPackImporter` validates structure, copies the zip into app-private
  `filesDir/iconpacks/`, parses, verifies drawables, and emits
  `Flow<ImportProgress>` (Validating / Indexing / Done / Failed).
- **Why:** Packs hold hundreds of drawables; parsing must not block the UI thread, and
  the copy into app storage protects against losing the picker URI permission later.
- **Impact:** `ImportProgress.Failed` is the single graceful failure channel; the
  previous active pack is only replaced on `Done`.

---

### D-012 - `ComponentInfo{...}` parsed with string ops, not regex
- **What:** `AppFilterParser.normalizeComponent` strips the `ComponentInfo{...}`
  wrapper with `indexOf('{')` / `lastIndexOf('}')` and plain splitting.
- **Why:** The P1 regex threw `PatternSyntaxException` on-device for otherwise-valid
  input (brace escaping differs across engines). A brace-heavy input is now a
  regression test.
- **Impact:** Parser is engine-agnostic; malformed inputs are rejected, never crash.

---

### D-011 - Real symbol ratio 0.49 for decoded drawables (glyph ratio stays 0.42)
- **What:** `AppIconTile` renders decoded pack drawables at the icon-set spec ratio
  **250/512 = 0.49**; the lucide category glyph keeps the smaller 0.42.
- **Why:** `lk7jo` places the symbol at 0.49 of the tile; matching it makes a decoded
  icon sit in the tile exactly as the design shows.
- **Impact:** `AppIconTile` gained a `fullBleedPainter` path for pre-composited masks;
  the painter path (decoded drawable) uses the 0.49 inset.

---

### D-010 - Source files kept ASCII-only; non-ASCII via escapes/entities
- **What:** Kotlin/XML source uses `\u00B0` (degree), `\u00B7` (middle dot) and XML
  entities (`&#176;`, `&#183;`) instead of literal non-ASCII glyphs.
- **Why:** The authoring pipeline mangled literal unicode punctuation (mojibake /
  lost chars). ASCII-only sources are encoding-proof across editors and toolchains.
- **Impact:** New strings follow the same rule; visual result is identical.

---

### D-009 - Toolchain installed on D: (IDE not required to build)
- **What:** JDK 17 on C:; Android SDK `D:\Android\Sdk`; Gradle 8.11.1
  `D:\Android\gradle-8.11.1`; `GRADLE_USER_HOME=D:\gradle-cache`; AVD on `D:\Android\AVD`;
  temp downloads on `D:\Android\downloads`. Android Studio IDE itself not installed.
- **Why:** C: had only ~8 GB free. cmdline-tools + the Gradle wrapper are sufficient to
  build/install/run; the IDE is only a convenience.
- **Impact:** Build commands use the SDK/AVD paths above; see docs/07.

---

### D-008 - Multi-module Gradle project (not single :app)
- **What:** Shipped `:core:{designsystem,common,model,data}` and
  `:feature:{home,iconpack,appdrawer}` per docs/01.
- **Why:** Enforces the dependency rule (Compose never touches PackageManager) from day
  one, and keeps P2-P4 feature work isolated.
- **Impact:** New features add a module; build is still fast (<1 min incremental).

---

### D-007 - Icon glyphs bundled as vector drawables, not a runtime icon lib
- **What:** 26 lucide glyphs converted to Android vector drawables in
  `core/designsystem/res/drawable`.
- **Why:** No runtime dependency, full control of stroke weight (2), matches the
  `lk7jo` icon-set style.
- **Impact:** Glyphs tint via `ColorFilter`; add more by dropping in a drawable.

---

### D-006 - Editor vs real launcher: `HomeActivity` is a real HOME target
- **What:** `HomeActivity` registered with HOME + DEFAULT; `MainActivity` was dropped in
  favour of the single HOME entry. `LauncherRole` fires the system picker.
- **Why:** The app must be genuinely selectable as the default launcher in P1.
- **Impact:** Verified on emulator with `cmd package set-home-activity`.

---

### D-005 - Design gaps resolved as ASSUMPTIONS, never by editing the `.pen`
- **What:** Home grid default (4-col), page indicator, badge placement, drawer tile
  sizing, dark palette, icon-pack decode placeholder - all documented in
  `04-ASSUMPTIONS.md` sections E/F.
- **Why:** Per the audit instruction and D-002.
- **Impact:** The `.pen` stays pristine; every inferred value is traceable.

---

### D-004 - Documentation set lives in `docs/`
- **What:** All progress/analysis notes stored under `docs/` + a top-level `README.md`.
- **Why:** Keeps the design file (`design/homeApp.pen`) pristine and gives one
  place for progress tracking across sessions/priorities.
- **Impact:** Every checkpoint updates `docs/05-PROGRESS.md` + `docs/03-FEATURE-MAP.md`.

---

### D-003 - SDK / Gradle caches on `D:`
- **What:** Install Android SDK to `D:\Android\Sdk`, set `GRADLE_USER_HOME=D:\gradle-cache`.
- **Why:** C: has only ~8.4 GB free; Android SDK + emulator are multi-GB.
- **Impact:** Env vars `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME`, `TEMP`.

---

### D-002 - Treat `homeApp.pen` as read-only source of truth
- **What:** Do not edit the `.pen`; implement it in the app.
- **Why:** Per user instruction. The file is a design spec sheet, not a work target.
- **Impact:** Gaps (page indicator, folders, calendar widget...) are resolved as
  documented **assumptions** in `docs/04-ASSUMPTIONS.md`, not by editing the design.

---

### D-001 - P1 = installable MVP; OS-deep parts stubbed
- **What:** Build A+B+C fully (UI matching the design ~100%) while stubbing
  default-launcher picker edge cases, live wallpaper engine, and KWGT-style system
  widgets (AppWidgetHost).
- **Why:** A real from-scratch launcher is large; an installable, set-as-default
  MVP proves the design system + core flows first. Heavy OS integration is deferred.
- **Impact:** P1 delivers a working Home app, icon pack system, and drawer; P2-P4
  add widgets, folders, system UI, settings, onboarding.

---

### D-000 - Native Kotlin + Compose over cross-platform
- **What:** Kotlin, Jetpack Compose, MVVM + Clean, Hilt, DataStore, Room, Coil.
- **Why:** A launcher (replace-home intent, system icon replacement, widgets)
  needs deep, stable Android API access that is more reliable/performant natively
  than via Flutter/React Native bridges.
- **Impact:** Stack fixed; see `docs/01-ARCHITECTURE.md`.
