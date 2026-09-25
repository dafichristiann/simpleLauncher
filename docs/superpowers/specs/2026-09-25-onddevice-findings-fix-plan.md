# On-Device Findings Fix — Plan & Spec (P1–P5)

> **Status:** COMPLETE — P1–P5 implemented; STOP #3 final review (2026-09-25).
> **Scope:** fixes/tasks reported after using the launcher on a real **Tecno Camon 50 Pro
> (TECNO CN7c, Android 16 / API 36)** plus the `soft_home_pixel` emulator (API 35).
> **Extends:** P1–P4d. **Supersedes nothing.**
> **`.pen` is read-only** (D-002). All prior decisions (D-032/D-033/D-037, etc.) remain.
> **Baseline (verified):** git HEAD `e51226c`, clean tree; **236 JVM unit + 38 instrumented**
> tests green on both devices.

This document is both the **execution plan** ("alur dari awal sampai akhir") and, for the
architectural priorities (P3, P4, P5), the **short spec** the user asked for.

---

## 0. Intent & success criteria

Make the launcher feel correct on the user's real Tecno:

1. **Symmetric drawer gesture** — swipe-up opens, swipe-down closes.
2. **Smooth drawer scroll** — no "stiff / choppy" stutter.
3. **Leaner home** — only Time, Date, Weather, Search, Music visible (Calendar /
   Battery-Storage / Quick-Notes default-off, re-enablable in Settings).
4. **Tap-rows open the right app** — graceful fallback, never crash.
5. **Honest per-app drawer icons** — no two different apps look identical.
6. **Richer categories** — the 8 design categories, swipeable horizontally.

Every phase is verified with **real screenshots/video on Tecno + emulator**, docs are
updated, and work **STOPs for review** at the marked checkpoints.

---

## Repository facts (audit baseline)

| Fact | Value |
|---|---|
| Project root | `D:\Perkuliahan\semester 7\menu` |
| git HEAD / tree | `e51226c` / clean |
| Design source | `design/homeApp.pen` (byte-stable since P3.5; SHA256 `08DB2A81…CA6201`, 176,128 B) |
| Drawer frame | `.pen` `TpzL1` = "Warm App Drawer — Unique Icon Grid" |
| Category/icon library | `.pen` `ciHU3` = "Warm Right Rail — Icon Language Library" (8 groups) |
| Devices (adb) | Tecno `169402562R001782` (API 36); AVD `soft_home_pixel` (API 35) |
| Tests (baseline) | 236 unit + 38 instrumented, green both devices |
| Module layout | `:core:{designsystem,common,model,data}`, `:feature:{home,iconpack,appdrawer}`, `:app` |

---

## 1. Root-cause audit (evidence collected BEFORE any fix)

### 1.1 — Swipe-down does NOT close the drawer  *(Prioritas 1)*

**Where:** `app/src/main/kotlin/com/softhome/launcher/HomeActivity.kt` (`LauncherRoot`).

```kotlin
// Swipe-up layer (BEHIND the home content): opening the drawer.
Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onVerticalDrag = { _, delta -> total += delta },
        onDragEnd = { if (total < -60f) drawerOpen = true },
    )
})
HomeScreen(...)
AnimatedVisibility(visible = drawerOpen, ...) { AppDrawerScreen(onClose = { drawerOpen = false }) }
```

`AppDrawerScreen.kt` has **no** gesture handler — only a `BackHandler` that pops the
drawer's layer stack (menu → folder → close) and `onClose`.

**Root cause:** there is literally **no swipe-down handler anywhere**. Only Back closes
the drawer. The open/close gesture is **asymmetric by omission** (swipe-up exists behind
the home content; the drawer overlay has no downward-drag path).

---

### 1.2 — Drawer scroll feels "stiff / choppy"  *(Prioritas 1)*

**Where:** `feature/appdrawer/.../AppDrawerScreen.kt` → `AppGridContent`.

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(4),
    verticalArrangement = Arrangement.spacedBy(Spacing.xxl), // 32dp FIXED
    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
)
```

Each `AppCell` calls `DrawerAppIcon`, which runs `remember(activePack?.id,
resolved.drawableName, resolved.source, sizePx) { … toBitmap … }`.

`AppDrawerViewModel.buildState` computes, **per emission**:
`IconMasker.symbolFor(app)` (allocates `(pkg + class + label).lowercase()` and runs up to
~18 `contains` scans) and `resolveIcon(...)`.

**Suspected causes (to confirm by measurement, not assumption):**
- (a) the fixed **32 dp** vertical row gap is unusually large → matches the "patah-patah"
  feel vs. normal grid spacing;
- (b) bitmap decode/composite for pack apps on scroll frames;
- (c) per-item work (string heuristic + color token) re-run on every recomposition.

**Also confirmed:** the P3 **"Spacing" preset (`SpacingScale`) is persisted but never
applied** to the drawer grid (nothing consumes `spacing.factor`) — a latent bug.

**Fix approach:** measure first (`adb shell dumpsys gfxinfo` + Perfetto trace on the
Tecno with the real app list), then apply only what the data shows:
1. memoize `(symbolName, colorToken)` once per app in `buildState` (store on `DrawerEntry`),
   so the composable stops recomputing;
2. apply the `SpacingScale` preset to the drawer's vertical gap (and/or reduce the fixed
   32 dp to a warmer default);
3. if decode is the hot path, verify `DrawerAppIcon`'s `remember` keys are stable and the
   work is not re-triggered per frame.

---

### 1.3 — Home rows to remove  *(Prioritas 2)*

**Where:** `core/model/.../HomeRowPref.kt`.

```kotlin
enum class HomeRowKind { Time, Date, Weather, Search, Music, Calendar, BatteryStorage, Notes }

object HomeRowLogic {
    val LOCKED = setOf(Time, Date, Weather)
    val DEFAULT_ORDER = listOf(Time, Date, Weather, Search, Music, Calendar, BatteryStorage, Notes)
    fun default() = DEFAULT_ORDER.map { HomeRowPref(it, visible = true) }  // ALL visible today
    fun sanitize(stored) { … missing kinds appended with visible = true … }
}
```

`SettingsPanel.WidgetsSection` already renders a toggle + up/down per row via
`HomeRowLogic.toggle/moveUp/moveDown`.

**Conclusion (answers the user's question):** the user's preferred approach — **hide via
settings, do NOT delete code** — **matches the existing architecture exactly**. Calendar /
Battery-Storage / Notes are `HomeRowKind`s; the correct change is data-only (defaults +
sanitize), no code removal, and Settings keeps the ability to re-enable them.

---

### 1.4 — Deep-link row → app  *(Prioritas 3)*

**Pattern already proven in the codebase:** `feature/home/.../RailShortcut.kt`
(`RailShortcutResolver`) resolves implicit intents with `FLAG_ACTIVITY_NEW_TASK` and caches
a null-sentinel. P3 generalizes this for home rows.

**Real device probe (Tecno CN7c) — `cmd package resolve-activity`:**

| Row | Intent tried | Result on Tecno |
|---|---|---|
| Time | `ACTION_SET_ALARM` | ✅ `com.transsion.deskclock/.HandleSetAlarmApiCalls` |
| Time | `CATEGORY_APP_ALARM` (MAIN) | (available path; `SET_ALARM` already resolves) |
| Date | `CATEGORY_APP_CALENDAR` (MAIN) | ❌ **`No activity found`** → fallback required |
| Date | launcher activity | ✅ `com.transsion.calendar/com.android.calendar.AllInOneActivity` exists |
| Weather | `CATEGORY_APP_WEATHER` (MAIN) | ❌ **`No activity found`** → fallback required |
| Weather | launcher activity | ✅ `com.rlk.weathers/.ui.main.MainActivity` exists |
| Search | `ACTION_VIEW https://www.google.com` | ✅ → user default browser `com.brave.browser` |
| Music | Spotify (`com.spotify.music`) | ✅ `com.spotify.music/.MainActivity` installed |

**Conclusion:** implicit category intents are the correct primary path but are **not
guaranteed on OEM ROMs** (Tecno doesn't register calendar/weather categories). A
**graceful fallback chain** is mandatory: *implicit category intent → known-launcher-app
package → chooser → no-crash message*. (Matches the user's explicit request.)

---

### 1.5 — Duplicate / generic drawer icons  *(Prioritas 4)*

**Design source of truth.** `TpzL1` draws 24 tiles; `ciHU3` defines a per-app glyph library
across **8 groups**. Every tile uses a **lucide line glyph** tinted a **category color**
(e.g. WhatsApp → `message-circle` tinted `$icon-communication`), **not** a real brand logo.

`TpzL1` tiles (all Tecno-specific — proves the design was authored for this device):
WhatsApp, Instagram, Gmail, Chrome, Messages, Phone, Contacts, Calendar, Calculator,
Camera, Clock, Settings, Files, Photos, YouTube, Spotify, Maps, **DANA**, **Alkitab**,
**Brave**, **Agoda**, **AI Gallery**, **Claude**, **Discord**.

`ciHU3` groups (the 8 categories requested in Prioritas 5):
Communication · Social & Entertainment · Productivity & Tools · Browser & Search ·
Camera & Media · Maps & Travel · Finance & Shopping · Food & Lifestyle.

**Runtime reality.**
- `IconMasker.symbolFor(app)` is a **string heuristic** over `(pkg + class + label)`
  reaching ~20 `LineIcon` names.
- `LineIcon.fromName()` only matches enum names **exactly** — design glyph names such as
  `disc-3`, `banknote`, `utensils`, `shopping-basket`, `joystick`, `car-front`, `bike`,
  `tent`, `coffee`, `graduation-cap` have **no** drawable.
- Bundled `appfilter.xml` maps only Google package names (absent/renamed on Tecno); with
  no active icon pack the pack branch never wins.

**Root cause:** there is **no per-package → glyph mapping table**. The design's per-app
glyphs live only in the `.pen`; they were never ported. So DANA, GoPay, Shopee,
Tokopedia, Gojek, Grab, Agoda, KFC, Discord, Claude, ChatGPT, Fortnite, Tandem, Pinterest,
Threads and every uncategorized app collapse to **`AppWindow` + `Neutral`** → the
"duplikat / generic" complaint.

---

### 1.6 — Categories + horizontal swipe  *(Prioritas 5)*

**Where:** `core/model/.../DrawerCategory.kt` — only **4** values
(`All / Communication / Entertainment / Tools`), mapped from the raw
`ApplicationInfo.category` Int via `DrawerCategoryMapper`.

`AppDrawerScreen.CategoryNav` is a plain tap-only `Row` — **no** horizontal gesture.

**Root cause:** the 4 categories are the older `ApplicationInfo.category` mapping. The
richer 8 (the `.pen` `ciHU3` groups) are **app-specific**, not derivable from
`ApplicationInfo.category` — they need a **package→category resolver** (the same data the
P4 icon table provides). Horizontal swipe needs a scoping decision (whole-category pager
vs. tab switch).

---

## 2. Prioritas 1 — Gesture / scroll fixes  *(bounded bug-fix — do first)*

> **Scope:** `feature:appdrawer/AppDrawerScreen.kt` (+ `AppDrawerViewModel.kt` for 1.2).
> Bug fix; no spec required. **STOP #1 for review afterwards.**

### 2.1 Swipe-down closes the drawer

- Add a vertical-drag handler on the drawer **surface** in `AppDrawerScreen`
  (`Modifier.pointerInput` on the top-level `Box`), downward drag beyond a threshold →
  `onClose()` (reuse the `total < Xf` technique from `HomeActivity`; upward not needed here).
- **Coexistence with the inner `LazyVerticalGrid`:** the grid consumes its own vertical
  scroll. The drawer-close drag is attached to **non-scrolling chrome + background**
  (header area / background `Box`), so an over-scroll-down gesture on the grid can instead
  be routed through a `nestedScroll` connection — chosen approach: intercept in
  header/background; if the grid is at scroll-top and the user drags down, close.
- Threshold chosen to match the swipe-up (≈60dp), direction-symmetric.
- **Files:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerScreen.kt`.
- **Tests:** `DrawerSwipeDownTest` (instrumented Compose swipe → asserts `onClose`),
  plus real on-device video.

### 2.2 Smooth drawer scroll

1. **Measure** on the Tecno (real app list): `adb shell dumpsys gfxinfo
   com.softhome.launcher.debug framestats` + a Perfetto/systrace capture while scrolling.
2. Apply the indicated fix(es):
   - **Memoize** `(symbolName, colorToken)` per app once in `AppDrawerViewModel.buildState`,
     carried on `DrawerEntry` — composables read fields, not recompute.
   - **Apply the spacing preset** (`SpacingScale.factor`) to the drawer's vertical gap,
     replacing the fixed 32 dp (and pick a warmer default). Also fixes the latent
     "persisted-but-unconsumed spacing" bug.
   - If decode-bound: confirm `DrawerAppIcon`'s `remember` keys are stable and bitmap work
     is not re-run per frame.
3. **Files:** `AppDrawerViewModel.kt`, `AppDrawerScreen.kt` (+ possibly
   `IconMasker`/`DrawerIconColor` gain a pure combined `resolve(app)`).
4. **Tests:** unit test asserting the symbol/color is computed once per app; on-device
   before/after jank numbers + video.

### 2.3 P1 exit criteria
- [ ] Swipe-down from the drawer closes it and returns to Home (video).
- [ ] Scroll jank measurably improved (before/after `gfxinfo`); no regression to swipe-up.
- [ ] Existing suites green; new tests added.
- **STOP #1 → report root cause + evidence to the user.**

---

## 3. Prioritas 2 — Home rows simplification  *(architectural-lite)*

> **Scope:** `core:model/HomeRowPref.kt` + tests + docs. Show a short design to the user
> (in-chat) before executing; here it is.

**Design (chosen — matches existing architecture):**

- Add `val DEFAULT_HIDDEN: Set<HomeRowKind> = setOf(Calendar, BatteryStorage, Notes)`.
- `default()` → `DEFAULT_ORDER.map { HomeRowPref(it, visible = it !in DEFAULT_HIDDEN) }`.
- `sanitize(stored)` → when appending a **missing** kind, if it is in `DEFAULT_HIDDEN`,
  append it **hidden**; otherwise visible (as today).
- `LOCKED` unchanged (Time/Date/Weather). Home then renders exactly
  **Time → Date → Weather → Search → Music**.
- **No code deletion.** Calendar/Battery/Notes stay toggleable in Settings → Widgets.

**Migration for existing installs** (already stored all-visible `homeRows`):
- One-time migration in `sanitize` / prefs read: detect the **legacy all-visible default**
  (all 8 kinds present & visible) and apply the new default once. Documented in
  `docs/09` as a new decision.
- **Decision needed (Q1):** migration mode (apply-once default vs. reset vs. code-only).

**Files:** `core/model/.../HomeRowPref.kt`, `HomeRowLogicTest.kt`, `docs/03`, `docs/05`,
`docs/09`.

---

## 4. Prioritas 3 — Deep-link rows  *(architectural — spec)*

> **Scope:** new `feature/home/RowLaunchResolver.kt` + `HomeScreen.kt` wiring + tests.

### 4.1 Design

New pure-ish resolver, mirroring `RailShortcutResolver`:

```kotlin
enum class HomeRowAction { Clock, Calendar, Weather, Search, Music }

class RowLaunchResolver(private val resolve: (Intent) -> ComponentName?, context) {
    fun intentFor(kind: HomeRowKind): Intent?
}
```

**Resolution chain (per row), uniform fallback rule:**

1. Build the **primary implicit intent**; `resolveActivity` → if found, use it.
2. Else try **known package launcher activities** (below); if found, use it.
3. Else `Intent.createChooser(bestRemaining, …)`.
4. Else **no-op + Toast "No app found"** (never crash).

| Row | Primary intent | Known-package fallbacks (Tecno first) |
|---|---|---|
| Time | `ACTION_SET_ALARM` (fallback `CATEGORY_APP_ALARM`) | `com.transsion.deskclock`, `com.google.android.deskclock` |
| Date | `CATEGORY_APP_CALENDAR` (MAIN) | `com.transsion.calendar`, `com.google.android.calendar` |
| Weather | `CATEGORY_APP_WEATHER` (MAIN) | `com.rlk.weathers`, `com.google.android.apps.weather`; else browser weather URL *(Q3)* |
| Search | `ACTION_VIEW https://www.google.com` | — (honors the user's default browser/search app; **no hardcoded app**) |
| Music | Spotify deep link `ACTION_VIEW` `spotify:` → `com.spotify.music` | Play Store listing if absent; else chooser |

All intents get `FLAG_ACTIVITY_NEW_TASK` (launcher context).

### 4.2 Wiring

- `HomeScreen` passes `onTapRow` for Time/Date/Weather/Search/Music to the resolver.
- **Preserve / decide existing states (Q2):** today Search-row tap → SEARCH state,
  Music-row tap → MUSIC expand. Recommended: **tap = launch the app**, **long-press =
  keep the in-place expand** (Search/Music), so no behavior is silently lost.

### 4.3 Files & tests
- New: `feature/home/.../RowLaunchResolver.kt`.
- Edit: `feature/home/.../HomeScreen.kt` (`HomeRowSlot` tap routing).
- Tests: `RowLaunchResolverTest` (pure intent selection with a fake `PackageManager`);
  instrumented tap test per row on device.

---

## 5. Prioritas 4 — Drawer icon correctness  *(architectural — spec)*

> **Scope:** new pure icon map in `core:model`, new drawables in `core:designsystem`,
> wiring in `feature:iconpack`, tests.

### 5.1 Design

- **Port `TpzL1`/`ciHU3`** into a new pure table:

  ```kotlin
  // core:model/DrawerIconMap.kt
  data class DrawerIconEntry(val packageName: String, val glyph: String, val category: DrawerCategory8)
  object DrawerIconMap { fun forPackage(pkg: String): DrawerIconEntry? ; fun allIn(category): List<…> }
  ```

  Seed from the `.pen` (WhatsApp→`message-circle`/Communication, Discord→…, DANA→`wallet`/
  Finance, Brave→`globe`/Browser, Claude→`bot`/Browser, Alkitab→`book-open`, Agoda→`tent`/
  Travel, Grab/Gojek→`car-front`, Pinterest→`pin`, TikTok→`music-2`, Threads→`at-sign`,
  Fortnite→`joystick`, Tandem→`users-round`, KFC→`utensils`, etc.).

- **Re-audit real installed apps** via `PackageManager` (Tecno list already captured: 76
  launcher activities) and cross-check each against the table.

- **Resolver precedence (new):**
  `user override → active icon pack drawable → DrawerIconMap → heuristic IconMasker →
  category glyph`.

- **Guarantee uniqueness:** a deterministic **de-duplication pass** assigns a stable
  distinct `(glyph, color)` per package when two collide. Test
  `DrawerIconUniquenessTest` asserts unique `(glyph,color)` across the **real Tecno
  package list**.

- **Add missing glyph drawables** for design names with no `LineIcon` yet (`disc-3`,
  `banknote`, `utensils`, `shopping-basket`, `joystick`, `car-front`, `bike`, `tent`,
  `coffee`, `graduation-cap`, `pin`, `at-sign`, `palette`, `mic`, `store`, …) and extend
  `LineIcon`.

### 5.2 Files & tests
- New: `core/model/DrawerIconMap.kt`; new vector drawables; `LineIcon` additions.
- Edit: `feature/iconpack/domain/IconMasker.kt` (fallback retained),
  `feature/iconpack/ui/DrawerAppIcon.kt` / `AppDrawerViewModel.resolveIcon`.
- Tests: `DrawerIconUniquenessTest` (pure, seeded with the real Tecno list),
  `DrawerIconMapTest`.

---

## 6. Prioritas 5 — Categories + horizontal swipe  *(architectural)*

> **Scope:** `core/model/DrawerCategory.kt`, `feature/appdrawer/*`.

### 6.1 Design

- Expand to **8 categories** (names verbatim from `ciHU3`):
  `All, Communication, Social & Entertainment, Productivity & Tools, Browser & Search,
  Camera & Media, Maps & Travel, Finance & Shopping, Food & Lifestyle` (+ `Other` fallback).
- New `DrawerCategoryResolver` maps package→category using the **same `DrawerIconMap`**
  package data (not `ApplicationInfo.category`), with `Other` fallback for unknowns.
- Replace the tap-only `CategoryNav` with a **`HorizontalPager`** (one page per category)
  + a tab row; **tap and horizontal swipe both switch**.
- Search + alphabet rail scope: **Decision needed (Q6)** — recommended **global search,
  per-category pager**.
- Category is runtime-only (not persisted) → no storage migration.

### 6.2 Files & tests
- Edit: `core/model/DrawerCategory.kt`, `feature/appdrawer/AppDrawerScreen.kt`,
  `AppDrawerViewModel.kt`.
- Tests: `DrawerCategoryTest` (extended), `DrawerCategoryResolverTest`,
  `DrawerSwipeCategoryTest` (instrumented).

**P5 result:** complete. The pager exposes the eight design tabs; tab taps and horizontal
swipes select the same page. Search/alphabet filtering remains global and category state is
runtime-only. `DrawerSwipeCategoryTest` and `DrawerSwipeDownTest` cover the gesture paths.

---

## 7. Verification (every phase)

- **Build:** `.\gradlew.bat :app:assembleDebug` → install to **Tecno + emulator**.
- **Suites:** `.\gradlew.bat test` and `.\gradlew.bat :app:connectedDebugAndroidTest` green
  on both devices; new tests per phase added.
- **Real evidence** (save under `docs/screenshots/`):
  - swipe-down closes drawer (video/gif + before/after stills),
  - scroll jank before/after numbers + video,
  - trimmed home (5 rows only),
  - each row deep-link on Tecno (Clock/Calendar/Weather/Search/Spotify),
  - non-duplicate icon grid (Tecno + emulator),
  - horizontal category swipe.
- **Docs:** update `docs/03-FEATURE-MAP.md`, `docs/05-PROGRESS.md`,
  `docs/09-DECISIONS-LOG.md` (new D-043+), `README.md`.

**Final evidence:** Tecno and emulator screenshots are stored as `docs/screenshots/p5-*`;
the debug APK was installed/launched on both connected targets.

---

## 8. Sequencing & STOP points

```
P1 (audit → fix gesture + scroll) ──► STOP #1  (report root cause + evidence)
P2 (home rows default-off)        ──► quick review
P3 (deep-link rows)               ──► STOP #2  (most decisions land here)
P4 (icon mapping + uniqueness)
P5 (8 categories + horizontal swipe)
Docs update                        ──► STOP #3  (final review)
```

- **P1 may start immediately** (pure bug fix, per user instruction) — **after** the audit
  report, which is this document.
- **P2–P5** each get their short design reviewed before execution (P2–P3 specs above).

---

## 9. Open questions (answers required before P2–P5 implementation)

1. **Row default migration:** existing install already stored all rows visible. Apply the
   new default once via migration **(a, recommended)**, reset on update **(b)**, or change
   only the code default and leave current prefs **(c)**?
2. **Row tap semantics:** Search row tap → open search/browser, Music row tap → open
   Spotify, **long-press = existing expand** **(recommended)** — or keep tap = expand and
   add a separate affordance?
3. **Weather fallback:** if no weather app resolves, open the browser to a weather URL
   **(recommended)** or show a "no app" toast?
4. **Icon style:** follow the `.pen` (lucide glyph + category color) **(recommended, design
   faithful)** — or pull real app icons via `PackageManager` (contradicts the Warm system)?
5. **Category assignment:** drive categories from the ported `TpzL1` package→category table
   with heuristic fallback **(recommended)**?
6. **Swipe scope:** horizontal swipe switches whole category pages (pager); should the
   alphabet rail and search be **global** (recommended: global search, per-category pager)
   or per-category?

---

## 10. Risks & non-goals

- **OEM variability:** category intents for calendar/weather are absent on Tecno → the
  fallback chain is load-bearing (covered by tests).
- **Icon de-dup:** the uniqueness guarantee must be deterministic and stable across
  launches (no random assignment) so tiles don't shuffle.
- **Gesture conflict:** the drawer-close drag must not steal the grid's scroll — verified by
  an instrumented scroll test, not only by eye.
- **Non-goals:** no `.pen` edits (D-002); no MediaSession playback (D-021); no real brand
  logo extraction; no redesign of the home/rail beyond the row list above.

---

## 11. P1 EXECUTION REPORT — STOP #1 (2026-09-25)

**Status: P1 COMPLETE. Stopped for review as agreed (P1 alone → STOP #1).**

### 11.1 What the audit found (beyond the plan)

| Item | Plan said | Reality (verified at source) |
|---|---|---|
| Swipe-down close | Missing | ✅ Missing (confirmed) |
| Swipe-up open | Works | ❌ **Also broken**: `HomeScreen.onOpenDrawer` was **never invoked**; the swipe-up layer sat behind the content and the rows' `dragSource` turns a quick swipe into a **tap**. Neither direction worked on the Tecno. |
| Fixed grid gap | "32dp" | Actually `Spacing.xxl` = **24dp**; the design token is `Spacing.drawerGap` = 26dp. |
| Spacing preset unused | Persisted, unconsumed | ✅ Confirmed — and the enum KDoc *claimed* it was applied to drawer gaps (aspirational comment). |

### 11.2 What was changed

- `core:designsystem/.../atom/VerticalSwipeObserver.kt` — **new** pass-through
  `Modifier.verticalSwipe(key, direction, threshold, onSwipe)`: observes on
  `PointerEventPass.Initial`, **never consumes**, ignores horizontal-dominant gestures.
- `app/.../HomeActivity.kt` — the broken behind-content swipe layer is replaced by
  `.verticalSwipe(direction = -1f)` on the **home surface**; swipe-up opens the drawer.
- `feature/appdrawer/.../AppDrawerScreen.kt` — `.verticalSwipe(direction = +1f)` on the
  drawer root closes it **only when the grid is at the top** and no overlay (menu/folder/
  editor) is open; also passes the precomputed `colorToken` into `DrawerAppIcon`; grid
  vertical gap now `Spacing.drawerGap * state.spacingFactor`.
- `feature/appdrawer/.../AppDrawerViewModel.kt` — `DrawerEntry` gains `colorToken`;
  `buildState` computes `(symbolName, colorToken)` **once per app**; `DrawerUiState` gains
  `spacingFactor` (from `prefs.spacing.factor`).
- `feature/iconpack/.../ui/DrawerAppIcon.kt` — accepts an optional precomputed
  `colorToken`; skips the per-frame heuristic when present.
- Tests: `app/.../androidTest/DrawerSwipeDownTest.kt` (new, 2 tests),
  `feature/appdrawer/src/test/.../DrawerEntryResolveTest.kt` (new, 4 tests);
  `FolderCellsTest` updated for the new `DrawerEntry` field.

### 11.3 Verification (real devices)

- **Suites green:** JVM `test` ✅; instrumented `connectedDebugAndroidTest` **40/40 on BOTH
  the Tecno (API 36) and the emulator (API 35)** — including Back-handler + home-row-drag
  regression tests.
- **Swipe sequence on Tecno:** swipe-up opens drawer ✅; swipe-down closes it ✅; sequence
  captured in `docs/screenshots/p1-drawer-open-via-swipeup-after.png`,
  `p1-drawer-swipedown-midtransition-after.png`,
  `p1-drawer-closed-via-swipedown-after.png`.
- **Tighter grid:** `docs/screenshots/p1-drawer-tighter-grid-after.png` (spacing preset now
  consumed; ~7.5 rows fit vs ~6.5).

### 11.4 Honest gaps / new findings (not hidden)

1. **Scroll jank could not be measured numerically on the Tecno** — the Transsion ROM blocks
   `dumpsys gfxinfo` for the process ("Failure while dumping the app"); `SurfaceFlinger
   --latency` also returns no buffer. Emulator before/after was **within noise**
   (19/80 = 23.75% → 24/90 = 26.67% janky), because the small emulator grid is
   layout-bound, not heuristic-bound. The memoization is therefore justified
   **structurally** (per-frame heuristic removed, unit-locked) rather than by a jank delta.
   Recommend a follow-up Perfetto trace on the Tecno if a hard number is required.
2. **Pre-existing UX bug (out of P1 scope):** in the drawer, a *fast/short* swipe over an
   app tile fires `onTap` (because `dragSource` treats a release-before-long-press as a tap)
   → accidental app launches. Not caused by this change; flag for a future fix.
3. **Flaky test (pre-existing):** `IconPackPersistenceTest > stored id with no matching pack
   is cleared…` failed once in a full `test` run, then passed on isolation and on rerun —
   test-order/shared-storage sensitive. Not caused by P1; flag for a future fix.
4. **Launcher not persistently the device default** on the Tecno: closing our drawer can
   fall through to the OEM home (our app only becomes HOME when explicitly started / chosen).

**Awaiting review before P2 (home rows default-off, Q1 = apply-once migration).**

---

## 12. P2 EXECUTION REPORT (2026-09-25)

**Status: P2 COMPLETE (review checkpoint).** Q1 = **apply-once migration** (as chosen).

### 12.1 What was changed
- `core:model/HomeRowPref.kt`
  - new `DEFAULT_HIDDEN = {Calendar, BatteryStorage, Notes}`;
  - `default()` → 5 visible / 3 hidden;
  - `sanitize()` appends a missing kind **hidden** when it is in `DEFAULT_HIDDEN` (else visible);
  - new pure `migrateLegacy(stored): List<HomeRowPref>?` — returns the new default **only**
    when `stored` is exactly the legacy all-visible shape, else `null`.
- `core:data/PrefsRepository.kt` — the `prefs` flow runs `migrateLegacyHomeRowsOnce()` in
  `onStart` (guarded by an `AtomicBoolean`), writing the migrated list back **once per
  process**. No code path deletes/hides any row permanently; Settings still toggles them.

### 12.2 Verification
- **JVM suites green** — including new `HomeRowLogicTest` cases (`DEFAULT_HIDDEN`,
  `migrateLegacy` positive/negative) and `PrefsRepositoryTest`
  "legacy all-visible … migrated once" (asserts idempotence on a second read).
- **Instrumented green: 41/41 on BOTH Tecno (API 36) and emulator (API 35)** — including a
  new `default_home_hides_calendar_battery_and_notes` and updated row-visibility tests.
- **On-device (Tecno):** the home now renders exactly **Time → Date → Weather → Search →
  Music**. This install previously had all 8 rows visible, so the screenshot
  `docs/screenshots/p2-home-5-rows-after-migration.png` is **direct proof the migration
  fired** on an existing install.
- No `.pen` edits; no row removed from the model (locked set = Time/Date/Weather unchanged).

**Awaiting review before P3 (deep-link rows) — STOP #2 lands there.**

---

## 13. P3 EXECUTION REPORT — STOP #2 (2026-09-25)

**Status: P3 COMPLETE. STOP #2.** Decisions Q2 (tap launches / long-press expands) and
Q3 (weather → browser fallback) implemented as approved.

### 13.1 What was changed
- New `feature/home/RowLaunchResolver.kt` — ordered fallback chain
  *primary implicit intent → known launcher package → chooser → null*, testable with a
  fake resolver.
- `HomeScreen.kt` — row **tap launches**; the in-place expand of Search/Music/Notes moved to
  **long-press** (Q2). New optional `onLaunchRow` param so tests can assert the split.
- `core:designsystem/atom/HomeRows.kt` — `HomeRow` gained `onLongClick` (combinedClickable),
  backward compatible (a tap-only row is unchanged).
- `AndroidManifest.xml` — added the `<queries>` entries the rows need for package visibility
  on Android 11+ (SET_ALARM, APP_CALENDAR, APP_WEATHER, https, spotify).

### 13.2 On-device results (Tecno `169402562R001782`)
| Row | Launches | Path used |
|---|---|---|
| Time | `com.transsion.deskclock/.DeskClock` | known-package (SET_ALARM was **permission-denied**) |
| Date | `com.transsion.calendar/.AllInOneActivity` | **fallback** (category = No activity found) |
| Weather | `com.rlk.weathers/.ui.main.MainActivity` | **fallback** (category = No activity found) |
| Search | `com.brave.browser` | primary VIEW (user's default) |
| Music | `com.spotify.music/.SpotifyMainActivity` | spotify: deep link |

### 13.3 New root cause found (the important one)
The Time row silently did nothing at first. Logcat showed:
`Permission Denial: starting Intent { act=android.intent.action.SET_ALARM … } requires
com.android.alarm.permission.SET_ALARM`. The intent **resolves** (so the resolver picked it)
but `startActivity` is denied → swallowed by `runCatching`. Also, **Android has no clock
category** (`Intent.CATEGORY_APP_CLOCK` does not exist). Fixed by opening the clock app via
its known launcher package. This confirms OEM category intents are load-bearing to the
fallback design, exactly as §1.4/§10 predicted.

### 13.4 Tests
- `RowLaunchResolverTest` (9 cases): primary/fallback/chooser/null per row, NEW_TASK flag,
  Spotify deep link, Q3 weather browser fallback.
- Instrumented: `tapping_search_row_does_not_expand_it_launches_instead` +
  `long_pressing_search_row_expands_in_place` (Q2).
- **JVM suites green; 43/43 instrumented green on BOTH Tecno and emulator.**

**STOP #2 reached. P4 + P5 are now complete; proceed to STOP #3 final review.**

---

## 14. P4 EXECUTION REPORT — drawer icon correctness (2026-09-25)

**Status: P4 COMPLETE.** Stopped for review before P5 (as agreed: "do P4 fully, then
STOP for review before P5").

### 14.1 Design re-audit (the two `.pen` sources)
The `.pen` was re-read node-by-node (byte-stable: 176,128 B, SHA256 `08DB2A81…CA6201`).
Two frames drive the drawer, and they **disagree** in places:

| Frame | What it gives | Example |
|---|---|---|
| `TpzL1` "Warm App Drawer — Unique Icon Grid" | 24 real tiles, each an exact lucide glyph **+** a `$icon-*` color | WhatsApp → `message-circle` / `$icon-communication`; Chrome → `globe` / **`$icon-finance` (blue)**; Agoda → `tent`/travel; DANA → `wallet-minimal`/finance |
| `ciHU3` "Warm Right Rail — Icon Language Library" | the **8 categories** + ~72 apps (glyph + category) | Telegram → `send`/Communication; GoPay → `wallet-cards`/Finance; Agoda → **`tag`**/Travel |

**Decisions (user-approved):**
1. **The tiles win** for the 24 apps `TpzL1` draws; `ciHU3` fills the rest.
2. The design has **8 categories but only 7 color variables**. `Browser & Search` had no
   slot; the mock tints Chrome with the blue `$icon-finance`. → a dedicated **Browser**
   token reusing that blue (D-050).

### 14.2 The gap this closed
`DrawerIconMap` (a previous partial attempt) **existed but was never consumed** — the
drawer still rendered `IconMasker.symbolFor(app)`, a ~20-name string heuristic. So DANA,
GoPay, Shopee, Tokopedia, Gojek, Grab, Agoda, KFC, Discord, Claude, ChatGPT, Fortnite,
Tandem, Pinterest, Threads and every uncategorised app collapsed to `AppWindow` +
`Neutral` — the "duplikat / generic" complaint, reproduced on the Tecno.

### 14.3 What was changed
- **`core:model`**
  - `DrawerIconMap.kt` — rebuilt from the `.pen`: `DrawerIconEntry(fragment, glyph,
    category, colorToken)`; **TpzL1's 24 tiles** verbatim first, then the **ciHU3 8
    groups**; fragment order fixed for specificity (GoPay `gojek.gopay` before `gojek`;
    Threads `instagram.barcelona` before `instagram`; `gallery20` before `gallery`;
    `google.android.videos`/`apps.bard` before generic `google`; `youtube.music` before
    `youtube`; ShopeePay before Shopee). Added the remaining Tecno apps (Roaming `gotii`,
    SIM Toolkit `android.stk`, TECNO dialer `smart.caller`, Welife `smartlife.nebula`).
  - `DrawerIconAssignment.kt` (new, pure) — deterministic per-cell glyph+color with a
    **uniqueness guarantee**: cells are keyed by `componentKey` (so a dialer's Phone and
    Contacts activities — one package, two cells — get distinct pairs), sorted so the
    result never shuffles between launches, with a collision "nudge" that rotates the
    color then falls to a resolvable glyph.
  - `DrawerIconTokenName` gains **`Browser`**; `DrawerCategory.tabs` now excludes
    `Other` (the 8 real tabs; a latent off-by-one the P5 test already expected).
- **`core:designsystem`**
  - `Color.kt` / `SoftColors` — new `drawerIconBrowser` (light `#4D7C8A`, dark
    `#7FA6B2`), the `.pen`'s Chrome blue.
  - **7 broken vector drawables fixed** (incomplete lucide paths that rendered as dots /
    fragments): `banknote`, `credit_card`, `tv`, `music_2`, `settings_2`,
    `contact_round`, `bike` — replaced with the full official lucide 24×24 geometry.
- **`feature:iconpack`** — `DrawerIconColor.Token` gains `Browser`; new
  `tokenForName(DrawerIconTokenName)` bridge; `DrawerAppIcon` resolves the glyph via
  `LineIcon.fromLucide` (kebab **or** PascalCase) and paints the Browser color.
- **`feature:appdrawer`** — `AppDrawerViewModel.buildState` now computes the per-cell
  assignment from `DrawerIconMap` (precedence: **user override → active pack → map →
  IconMasker heuristic → category glyph**) and passes it into `resolveIcon` so the
  **rendered** `ResolvedIcon.symbolName` is the design glyph (the map's glyph was
  previously computed but ignored by the renderer — the core wiring bug).

### 14.4 Tests
- New `DrawerIconMapTest` (6): TpzL1 tiles' exact glyph+color; the 8 groups populated;
  representative library apps; specificity (`gojek.gopay`≠`gojek`, Threads≠Instagram,
  Google TV≠Google search, ShopeePay≠Shopee); unknown→null; no duplicate fragments.
- New `DrawerIconUniquenessTest` (5): unique `(glyph,color)` across the **real Tecno
  launcher package list** (77 activities, captured on-device); determinism /
  order-independence; design apps keep their glyph; one-package-two-activities split.
- Extended `DrawerIconColorTest` (name bridge round-trip incl. Browser),
  `DrawerAppIconColorTest` / `DrawerColorsTest` (Browser token coverage).
- **JVM unit: 265, 0 failures** (was 236 at P4d baseline; +29).

### 14.5 On-device verification (real screenshots)
- **Tecno `169402562R001782` (API 36):** `docs/screenshots/p4-drawer-tecno.png` (top),
  `p4-drawer-tecno-scrolled.png` (mid). Every visible tile is **distinct and
  design-faithful**: Agoda=tent, AI Gallery=image, Brave=shield-check,
  Calculator=calculator, Calendar=calendar-days, Camera=camera, **Chrome=globe (blue)**,
  Clock=alarm-clock, Contacts≠Phone, Drive=cloud-upload, edLink=graduation-cap,
  Game Space=joystick, Gmail=mail, **Google TV=tv**, Maps=map, Meet=video,
  Messages=message-square, Notes=notebook-pen, Photos=image, Pinterest=pin,
  Play Store=store, Recorder=mic, **SeaBank=banknote**, **Settings=sliders**,
  Spotify=disc-3, Weather=cloud-sun, **Welife=heart-handshake**, YouTube=play,
  YT Music=music-2.
- **Emulator `soft_home_pixel` (API 35, light theme):**
  `docs/screenshots/p4-drawer-emulator.png` — same distinct grid; YT Music (`music-2`)
  ≠ YouTube (`play`); Contacts (`contact-round`) ≠ Phone.

### 14.6 Honest notes
- The **exact tiles win** rule means the shipped map intentionally differs from `ciHU3`
  for a few apps (Agoda `tent` not `tag`; DANA `wallet-minimal` not `wallet`) — this is
  the approved decision, not drift; `DrawerIconMapTest` pins it.
- Claude/Discord tiles show `$selected-icon` in the mock (a selected-state leak); their
  color follows the ciHU3 category instead (documented assumption).
- `bitpit.launcher` (Niagara) and SOFT/HOME itself are genuinely unmapped → `AppWindow`
  (the design has no entry); they are de-duplicated so they still differ from each other.

**STOP #3 — P5 complete. Awaiting final review.**
