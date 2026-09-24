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
| UI | home renders clock + icons | Compose UI test | ✅ (smoke) |
| UI | home renders P2 rows (calendar/battery/notes) | Compose UI test | ✅ (P2, androidTest) |
| UI | drawer opens & filters by search | Compose UI test | ⬜ |
| UI | long-press menu appears (P3) | Compose UI test | ⬜ |
| Manual | set as default launcher, gestures | on device | ✅ |
| Manual | icon pack import (zip) end-to-end | on device | ✅ (P1.5) |
| Manual | **glyph uniqueness on device** (log fingerprints + comparison grid screenshot) | on device | ✅ (Session 4) |
| Manual | **P2 widgets + folders end-to-end** (real battery/storage vs `dumpsys`/`df`; notes & folders survive `am force-stop`) | on device | ✅ (P2) |
| Manual | TalkBack navigation | on device | ⬜ |
| Manual | WCAG contrast (title/body/muted on cream) | tooling | ⬜ |

**Current total: 124 unit tests, 0 failures (P2, Session 6).** Was 87 after the Warm
Right Rail redesign. P2 added: `FolderLogicTest` (11), `NotesTest` (3),
`NotesRepositoryTest` (4), `FolderRepositoryTest` (6), `DeviceStatusTest` (5),
`FolderCellsTest` (4), `HomeStateTest` (+4), `MotionTokensTest` (+1).

---

## Test file layout (as built)

```
feature/iconpack/src/test/kotlin/.../
├─ AppFilterParserTest.kt        # appfilter variants + brace-heavy regression
├─ IconMaskerTest.kt             # radius, category heuristics
├─ IconResolverTest.kt           # pipeline order + fallback + drawable name
├─ AutoMaskTest.kt               # P1.5 auto-mask rules (pure)
├─ IconPackImporterTest.kt       # P1.5 zip import (Robolectric + real zips)
└─ IconCompositorGlyphUniquenessTest.kt  # Session 4: fingerprints differ; blank/blob rejected
core/model/src/test/kotlin/.../
├─ GridConfigTest.kt
└─ IconPackTest.kt               # P1.5 source kinds + counts
core/data/src/test/kotlin/.../
└─ PrefsRepositoryTest.kt
feature/appdrawer/src/test/kotlin/.../
└─ AlphabetIndexTest.kt
app/src/androidTest/kotlin/.../
└─ HomeWidgetsTest.kt            # Compose UI smoke
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
