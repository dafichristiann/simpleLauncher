# 06 — Testing

Test strategy for the launcher: unit tests for core logic + Compose UI tests for
the main flows. Not all tests exist yet — this file is the target and the tracker.

---

## Coverage plan

| Layer | What | Tool | Status |
|---|---|---|---|
| Unit | icon masking math (squircle radius ≈30%) | JUnit | ✅ |
| Unit | auto-mask rules (radius, inset, tint, luminance) | JUnit | ✅ (P1.5) |
| Unit | `appfilter.xml` parser (valid / malformed / missing / braces) | JUnit | ✅ |
| Unit | icon resolution order (override → pack → mask) | JUnit | ✅ |
| Unit | icon pack model (sources, counts) | JUnit | ✅ (P1.5) |
| Unit | zip import: valid / corrupt / no drawable / no readable icons | JUnit + Robolectric | ✅ (P1.5) |
| Unit | preference saving/reading (DataStore) | JUnit | ✅ |
| Unit | grid config validation (columns, spacing, size) | JUnit | ✅ |
| Unit | folder grouping logic (drawer grid items, preview, add/remove) | JUnit | ✅ (P2) |
| Unit | alphabetical index bucketing | JUnit | ✅ |
| Unit | graceful fallback on corrupt icon pack | JUnit | ✅ (P1.5) |
| Unit | **glyph uniqueness**: different sources → different `fingerprint()`; blank/blob rejected | JUnit + Robolectric | ✅ (Session 4 / D-016) |
| Unit | **notes persistence** (DataStore round-trip, blank/clear, survives new instance) | JUnit + Robolectric | ✅ (P2) |
| Unit | **folder persistence** (DataStore JSON round-trip, corrupt → empty) | JUnit + Robolectric | ✅ (P2) |
| Unit | **device status** (battery read + storage math from block counts) | JUnit + Robolectric | ✅ (P2) |
| Unit | home interaction states (Idle/Search/Music/**Notes**) | JUnit | ✅ (P2) |
| Unit | **folder drawer cells** (preview order/cap, skip-missing) | JUnit | ✅ (P2) |
| Unit | **home row visibility/order** (locked set, toggle, move, sanitize) | JUnit | ✅ (P3) |
| Unit | **prefs round-trip** (rows/spacing/hidden-apps + `HomeRowsCodec` junk tolerance) | JUnit + Robolectric | ✅ (P3) |
| Unit | **app action eligibility** (uninstall greyed for system; remove/edit need drawer) | JUnit | ✅ (P3) |
| Unit | **icon pack persistence** (rehydrate + ghost-id cleared) | JUnit + Robolectric | ✅ (P3) |
| Unit | **settings cycles** (theme/spacing/columns) | JUnit | ✅ (P3) |
| Unit | **drawer menu labels** (order) | JUnit | ✅ (P3) |
| UI | home renders clock + icons | Compose UI test | ✅ (smoke) |
| UI | home renders P2 rows (calendar/battery/notes) | Compose UI test | ✅ (P2, androidTest) |
| UI | **P3 row visibility/order** (hidden row not rendered; locked rows always) | Compose UI test | ✅ (P3, androidTest) |
| UI | **SoftToggle** (on/off, disabled non-interactive) | Compose UI test | ✅ (P3, androidTest) |
| UI | **AppContextMenu** (rows, tap fires, uninstall disabled for system) | Compose UI test | ✅ (P3, androidTest) |
| UI | **SettingsRow** (label, click, reorder buttons, toggle slot) | Compose UI test | ✅ (P3, androidTest) |
| UI | drawer opens & filters by search | Compose UI test | ⬜ |
| UI | **category pager (tab tap + horizontal swipe + vertical-scroll regression)** | Compose UI test | ✅ (P5, `DrawerSwipeCategoryTest`) |
| UI | **long-press menu appears** (rail + drawer) | Compose UI test | ✅ (P3, androidTest) |
| Manual | set as default launcher, gestures | on device | ✅ |
| Manual | icon pack import (zip) end-to-end | on device | ✅ (P1.5) |
| Manual | **glyph uniqueness on device** (log fingerprints + comparison grid screenshot) | on device | ✅ (Session 4) |
| Manual | **P2 widgets + folders end-to-end** (real battery/storage vs `dumpsys`/`df`; notes & folders survive `am force-stop`) | on device | ✅ (P2) |
| Manual | **P3 system UI + settings** (status bar light/dark, nav bar hidden, rail+drawer long-press, settings sections, row toggle survives `am force-stop`) | on device | ✅ (P3) |
| Manual | **P4a drag & drop** (row reorder + app→folder on device; mid-drag preview + insertion line; order & folder membership survive `am force-stop`; long-press menu still opens) | on device | ✅ (P4a) |
| Manual | **P4b Edit Icon** (editor pack/glyph modes; a saved override shows in the drawer + survives `am force-stop`; Reset returns the derived icon) | on device | ✅ (P4b) |
| Manual | TalkBack navigation | on device | ⬜ |
| Manual | WCAG contrast (title/body/muted on cream) | tooling | ⬜ |

**Current total: 280 unit tests, 0 failures (P5 + motion demo); 50 instrumented Compose tests execute
for the app, including the P5 pager/gesture coverage.** P4b added: `IconOverridesCodecTest` (10),
`IconEditorTest` (10) as unit; `IconEditorSheetTest` (2), `IconEditorScreenshotTest` (2),
`IconOverrideEndToEndTest` (2) as instrumented. `IconResolverTest` grew 8→10. P5 adds
`DrawerSwipeCategoryTest` (6) and `DrawerSwipeDownTest` (2). The 7
icon-pipeline tests (AutoMask, IconResolver, IconMasker, GlyphUniqueness, Importer,
Persistence, AppFilterParser) stayed green throughout. *(P8 re-verified 2026-09-25 on
TECNO CN7c: 280 unit + 50 instrumented, 0 failures — see "Motion system verification".)*

*(History: 124 after P2; 160 + 20 instrumented after P3; 176 + 25 after P3.5; P4a added
`DragAndDropStateTest` 5 + `DragDropResolverTest` 13 + `HomeRowDragTest` 2. The P4a log's
"307 unit" was an over-count; the authoritative debug-unit total after P4b is 217.)*

---

## Test file layout (as built)

```
feature/iconpack/src/test/kotlin/.../
├─ AppFilterParserTest.kt        # appfilter variants + brace-heavy regression
├─ IconMaskerTest.kt             # radius, category heuristics
├─ IconResolverTest.kt           # pipeline order + fallback + drawable name + P4b overrides
├─ AutoMaskTest.kt               # P1.5 auto-mask rules (pure)
├─ IconPackImporterTest.kt       # P1.5 zip import (Robolectric + real zips)
├─ IconEditorTest.kt             # P4b: editor state machine (pure)
└─ IconCompositorGlyphUniquenessTest.kt  # Session 4: fingerprints differ; blank/blob rejected
core/model/src/test/kotlin/.../
├─ GridConfigTest.kt
├─ IconPackTest.kt               # P1.5 source kinds + counts
├─ DragAndDropStateTest.kt       # P4a: begin/move/hover/end + gating (pure)
└─ DragDropResolverTest.kt       # P4a: row reorder index math + folder membership (pure)
core/data/src/test/kotlin/.../
├─ PrefsRepositoryTest.kt        # incl. P4b icon-override persist/clear
└─ IconOverridesCodecTest.kt     # P4b: override codec + legacy migration (Robolectric)
feature/appdrawer/src/test/kotlin/.../
└─ AlphabetIndexTest.kt
app/src/androidTest/kotlin/.../
├─ HomeWidgetsTest.kt            # Compose UI smoke
├─ HomeRowDragTest.kt            # P4a: drag reorders; stationary long-press does not
├─ IconEditorSheetTest.kt        # P4b: editor modes + Save gating
├─ IconEditorScreenshotTest.kt   # P4b: renders the editor and writes PNG evidence
└─ IconOverrideEndToEndTest.kt   # P4b: real DataStore round-trip + resolver
```

### Glyph-uniqueness regression (Session 4 / D-016)

`IconCompositorGlyphUniquenessTest` must stay green. It catches the failure mode
where every drawer tile collapses to the same charcoal (or cream) shape:

1. **Different silhouettes → different `IconCompositor.fingerprint()`** after `mask()`.
2. **Charcoal-only bitmap → `hasVisibleGlyph() == false`** (UI must fall back).
3. **Full-bleed cream blob → `hasVisibleGlyph() == false`**.
4. **`toBitmap` on two different BitmapDrawables → different fingerprints**
   (guards loader/cache key mistakes that would return one shared bitmap).

On-device check (manual): open drawer with SoftMonoTest active, filter logcat
`SOFTHOME_PIPELINE` for `Pack bitmap` / `fingerprint=` — all packages in view must
have **distinct** fingerprint values. Evidence screenshot:
`docs/screenshots/comparison-grid-pack.png`.

---

## Commands

```powershell
# Unit tests (all modules)
./gradlew test

# Unit tests for one module
./gradlew :feature:iconpack:testDebugUnitTest

# Instrumented (Compose) tests — needs device/emulator
./gradlew connectedDebugAndroidTest

# Coverage report (optional, add kover if needed)
./gradlew koverHtmlReport
```

---

## Accessibility checks

- **TalkBack:** every interactive composable has `contentDescription` or
  `semantics { }`; icon tiles announce the app label; page indicator announces
  "Page x of y"; mic pill announces "Voice search".
- **Contrast (WCAG):**
  - `#1A1A1A` on `#EDE6D8` → high (passes AA/AAA for large text).
  - `#625B52` on `#F6F0E7` → verify ≥ 4.5:1 (body).
  - `#81796D` on `#F6F0E7` → lowest pair; only for small non-essential meta,
    verify ≥ 3:1 and prefer ≥ 4.5:1 where possible.
  - `#E8DFD0` (icon stroke) on `#2B2B2B` (tile) → high contrast, passes.
- **Touch targets:** min 48dp (icon tiles ≥ 62dp in design, fine).

---

## Definition of "tested" for P1

- [ ] Icon masking + parser + resolution order have unit tests and pass.
- [ ] Preference saving round-trips (DataStore) and passes.
- [ ] Drawer search/filter + alphabet bucketing pass.
- [ ] At least one Compose UI smoke test for Home and one for Drawer.
- [ ] Manual: set-as-default + swipe-up drawer verified on emulator.

## P4a exit criteria (verified)

- [x] **Pure drag model unit-tested** (`DragAndDropStateTest`, `DragDropResolverTest`).
- [x] **Home-row reorder** works on device and survives `am force-stop`.
- [x] **App→folder** (existing + drop-to-create) works on device and survives `am force-stop`.
- [x] **Long-press menu still opens** on a stationary press (P3 regression guarded).
- [x] A stationary long-press does **not** reorder (`HomeRowDragTest`).
- [x] Mid-drag evidence captured (preview + insertion line / hover).
- [x] The 7 icon-pipeline tests stayed green.

## P4d exit criteria (verified)

- [x] **Pure codec unit-tested** (`BackupCodecTest`): round-trip (populated + empty),
  null pack id, and the **total** decode contract — `NotABackup` for a foreign JSON,
  `UnsupportedVersion` for a newer schema, `Malformed` for garbage / a missing `prefs`
  section; unknown fields ignored; partial prefs default; unknown enum names default;
  a locked home row in the string is sanitized.
- [x] **Repository round-trip on the real DataStore** (`BackupRepositoryTest`,
  Robolectric): export → wipe → import restores grid, theme, spacing, pack id, hidden
  apps, icon overrides, home rows, folders, notes.
- [x] **Non-destructive failure**: a non-backup / garbage file returns `NotABackup` /
  `Malformed` and leaves state untouched.
- [x] **Atomic apply**: `PrefsRepository.applyAll` writes every field (PrefsRepositoryTest).
- [x] **On-device round-trip** (`BackupRestoreEndToEndTest`): export to a real file, wipe,
  import, assert — green on **both** `soft_home_pixel` (API 35) and the physical TECNO
  CN7c (Android 16 / API 36).
- [x] **UI**: the "Backup & restore" section renders (real screenshots, emulator + device);
  the SAF `CreateDocument` picker offers `softhome-backup-<date>.json`; the exported file
  is well-formed JSON (421 B).
- [x] The 7 icon-pipeline tests + all P4a/P4b/P4c suites stayed green.

## Final test counts (P5)

| Suite | Count | Notes |
|---|---|---|
| **JVM unit (debug variant, authoritative)** | **280** | `./gradlew test` builds both variants; the **debug** variant is the counted total |
| Instrumented (Compose, `:app`) | **50 executed** | Includes P5 pager + scroll/tap/long-press regression coverage |

> Reconciliation note (continuing the honest-counting rule from P4a): the P4a log's
> "307 unit" was an over-count; P4b's "217", P4c's "220", and P4d's "236" are the
> authoritative **debug-variant** totals. The release variant reports a smaller number
> for the same sources (Robolectric-only suites vary), which is why the number must always
> be read per-variant and per-command.

## Motion system verification (Session 23 — P8)

- `:feature:home:testDebugUnitTest` covers deterministic demo playback advance, pause,
  resume, seek clamping, progress ratio, and end-of-track stop (`PlaybackControllerTest`).
- `:core:designsystem:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug`
  passed after integrating the shared drag/press interaction source.
- **Verified on Tecno CN7c (API 36, 2026-09-25):** `.\gradlew.bat testDebugUnitTest` →
  **280 unit, 0 failures**; `.\gradlew.bat :app:connectedDebugAndroidTest` →
  **50 instrumented, 0 failures**.
- TECNO CN7c visual smoke: idle home, play/pause progress, long-press expansion,
  full-track seek, clock digit update, weather fallback, and rounded rail capture are
  retained in `docs/screenshots/p8-motion-*.png`; the recorded playback/expand sequence is
  `docs/videos/p8-motion-playback-expand.mp4` (play → progress moves → pause → seek →
  resume-from-seek).
