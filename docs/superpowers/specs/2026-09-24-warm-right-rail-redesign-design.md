# Warm Right Rail Redesign — Design Spec & Phase Plan

> **Date:** 2026-09-24
> **Type:** Architectural redesign (replaces part of P1/P1.5 home + drawer UI)
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18
> **Status:** Spec written — awaiting user review before implementation plan.

This document is the single source of truth for the **Warm Right Rail** redesign.
It contains the `.pen` audit, the agreed design, exact tokens, and an
**end-to-end phase plan** (Phase 0 → Phase 8).

---

## 1. Why this redesign

The `.pen` file was updated. The old home layout (4-column squircle icon grid,
mic-only search pill) is **superseded** by a new system: a vertical list of
full-width rows (clock → date → weather → search → music player) with a
**right icon rail** and a **motion system** (3 interaction states).

The old docs (`00`–`04`) reference node IDs that **no longer exist** in the
file (`bi8Au`, `lk7jo`, `IwpZU`, `HhwVC`, `o0Fi0`, `ws286`, `u0a8RP`, `ECmUn`,
`uwKsz`, `bxlfq`, `ha6OA`, …). All prior traceability is stale and is rewritten
by this spec.

This is **not** a new feature (P2/P3/P4). It is a **redesign of existing P1
screens** (Home + App Drawer).

---

## 2. `.pen` Audit — source of truth

### 2.1 Top-level frames (new)

| # | Node ID | Name | Size | Role |
|---|---|---|---|---|
| 1 | `znb90` | Home Screen Mockup — Warm Right Rail | 390×720, r44, bg `#E8DFD0` | **Primary home mockup** |
| 2 | `L7ZAp` | Warm Home Screen — Full System Board | 1440×1180, r32, bg `#F1EBE1` | **Spec sheet** (4 panels) |
| 3 | `TpzL1` | Warm App Drawer — Unique Icon Grid | 430×860, r36, bg `#DCCDBA` | **New drawer** |
| 4 | `KkPN3` | Home Screen Mockup — Dark Editorial | 390×720, r44, bg `#18191A` | Dark reference only (out of scope) |

### 2.2 `L7ZAp` board structure (4 panels = the attached reference)

| Panel | Label node | Content |
|---|---|---|
| Final mockup | `RwVPc` (390×720 @ 72,300) | full Warm Right Rail home |
| 01 · Icon Language | `QeMXD` + `u3sUQ` + `X26hv` + panel `QBhow` | "Quiet symbols, consistent weight." + 8 tiles |
| 02 · Motion System | `JNFYC` + `pBKHX` + `X3DMZL` + card `Z5V7Y9` | "Small movement, clear purpose." + motion token card |
| 03 · Interaction Storyboard | `YzfzQ` + `hew89` + cards `Az7qs` / `m9OlxQ` / `T8AA1` | IDLE / SEARCH / MUSIC |

### 2.3 Traceability table — component → design node

| App component | Design node(s) | Values from `.pen` |
|---|---|---|
| Home root | `znb90` / `RwVPc` | 390×720 r44 `#E8DFD0`, `layout: none` |
| Right rail container | `znb90/hrsLU`, `RwVPc/B6633e` | x=318 y=0, **72×720**, fill `#D8C8B6`, pad `[58,0,26,0]`, gap **22**, align center |
| Rail icons (8) | `vsygX`,`Ws2pl`,`rU7nM`,`ibX83`,`q2W46j`,`RKZW7`,`o3McO3`,`o1ALlt` | **20×20**, lucide, fill `#2B2B2B`; icons: sparkles, circle-dot, message-circle, send, camera, wind, panel-left, phone |
| Time display | `IDSBb` (in `znb90`) / `cFKcs` (in `RwVPc`) | x=42 y=52, `"04:35"`, DM Sans **58 normal**, ls −2, `#2B2B2B` |
| Divider (×4) | `MrrnQ`,`RDJW1`,`LKAGP`,`FH9n4` | **318×1**, fill `#D0C2B1`; y=128/226/322/424 |
| Date number | `ApuhU` / `xFSYv` | `"22"`, **60 normal**, ls −2, `#2B2B2B` |
| Date day | `KkRQg` / `rDwbD` | `"t u e s d a y"`, 9 bold, ls 1.5, `#625B52` |
| Date month | `ftENm` / `HEjoM` | `"D E C E M B E R"`, 9 normal, ls 1.2, `#81796D` |
| Weather label | `lLPZS` / `b3lhF` | `"Current 8°C"`, **28 normal**, ls −0.5, `#3A3A3A` |
| Search placeholder | `Q1cGYj` / `U2xeN` | `"f i n d  s o m e t h i n g"`, 13 normal, ls 2, `#81796D` |
| Search icon | `N7ICqS` / `HbNbu` | **24×24**, `#2B2B2B` |
| Search dots | `obDAF` / `islo1` + children | 92×8, gap 8; middle dot 5 `#F2EEE7`, others 3 `#777873` |
| Music title | `QTwqr` / `U6lDr` | `"play music."`, **28 normal**, ls −0.5, `#2B2B2B` |
| Music artist | `juzvC` / `Y2uMpb` | `"Djo"`, 10 bold, **`#8A5F43`** (accent) |
| Music track | `E7w0Vk` / `yvDmK` | `"End of Beginning · Live from Chicago"`, 9 normal, `#81796D` |
| Album artwork | `mdsQP` / `yJjQ2` | ellipse **64×64**, fill `#F5EFE6`, stroke `#8A5F43` w1 |
| Album mark | `lAfW2` / `x38RS` | disc-3, 20, `#625B52` |
| Music controls | `x5SL5` / `cTvUn` | frame 108×24, gap 18; skip-back 16, play 18, skip-forward 16, `#2B2B2B` |
| Progress track | `pvQO0` / `HZ2B1` | 104×5 r3, `#B9AA98` |
| Progress fill | `d41sbp` / `zuUxr` | 32×5 r3, `#8A5F43` |
| Signature | `X5tG2G` / `nLNR7` | `"by SOFT / HOME"`, 9 bold, ls 0.8, `#2B2B2B` |
| Icon library tile | `E9T8qF`…`y0DAnQ` (panel `QBhow`) | tile **68×68 r20** `#F5EFE6`, glyph 24 `#2B2B2B`; panel bg `#E4D7C7` r24 |
| Motion token card | `Z5V7Y9` + text `Cvy3V` | fill `#2B2B2B` r24, pad 24, gap 14; title `#CBB39D`; body IBM Plex Mono 13 `#F7F0E5` |
| Storyboard cards | `Az7qs`,`m9OlxQ`,`T8AA1` | 250×300 `#E4D7C7` r24; number `#8A5F43`; label 20 bold; preview 210×76 `#F5EFE6` r18 |
| **Drawer root** | `TpzL1` | 430×860 r36 `#DCCDBA` |
| Drawer time/status | `bqgp2`, `YHLVj` | 14 bold `#2B2B2B` / 12 bold `#625B52` |
| Category nav | `B6gGM` | frame 372×46, gap 28; "All" 16 bold `#2B2B2B`; others 14 normal `#81796D`; active underline `n0TeI` 34×3 r2 `#8A5F43` |
| Drawer tile | `dBQmH`…`cbYbn` | **68×68 r21**; fill varies `#F5EFE6` / `#E8DFD0` / `#EFE3D3` / `#D19B62`; glyph 24 colored |
| Drawer label | `J3Lb4`…`R9mZMI` | 11 normal `#3A3A3A`, centered, under tile |
| Alphabet index | `czxh4` + children | 16 wide, gap 4; active `#B06F52`, idle `#81796D` |
| Drawer search | `V7udUl` | **390×56 r28**, fill `#E8DFD0`, stroke `#C8B8A6` w1; search icon 24 `#625B52`; label 16 `#625B52`; ellipsis-vertical 22 |
| Gesture bar | `OGsws` | 98×4 r2 `#81796D` |

### 2.4 Drawer accent palette (icon tint examples in `TpzL1`)

`#B06F52`, `#D19B62`, `#5F7A72`, `#6E8B86`, and `#2B2B2B`. These are treated as
**example pack colors**, not a required tint (see §4.7 and decision Q4).

---

## 3. Visual comparison — what changes vs P1

### Changed (must refactor)
1. **Home layout** — 4-column squircle grid → **full-width vertical rows**,
   separated by 1px `#D0C2B1` dividers (not cream cards).
2. **Right rail** — brand new. 72dp wide, `#D8C8B6`, 8 line icons @ 20dp.
3. **Music player** — brand new; absent in P1.
4. **Search** — mic-only pill → text row `"f i n d  s o m e t h i n g"` + search icon.
5. **Drawer redesign** — tile 104 r30 → **68 r21**, labels added, search
   h52 r26 → **h56 r28**, category nav added, alphabet active `#B06F52`.
6. **Icon language** — rail icons are outline, no solid squircle background.
7. **Typography** — clock 60 bold → **58 normal**; weather 30 bold → **28 normal**.

### Preserved (unchanged)
- DM Sans (+ IBM Plex Mono).
- Core colors: `#E8DFD0` surface, `#2B2B2B` charcoal, `#F6F0E7`/`#F5EFE6` card,
  `#8A5F43` accent, `#625B52`/`#81796D` text.
- Compose + MVVM/Clean architecture, Hilt, DataStore.
- **Icon pack pipeline (B1–B5):** decode, auto-mask, importer, resolver,
  parser — logic used as-is; only tile rendering/size/colors change.
- Swipe-up → drawer.
- HomeActivity / LauncherRole / HOME intent target.

---

## 4. Agreed design

### 4.1 Architecture — in-place refactor

| Module | Change |
|---|---|
| `core:designsystem` | **+ `MotionTokens.kt`** (new). **+ `HomeRows.kt`** (new atoms: `HomeRow`, `RailIcon`, `MusicPlayerRow`). Extend `Dimens`, `Spacing`, `Color.kt`. Add new icon drawables. New type styles (clock/weather). |
| `feature:home` | **Refactor `HomeScreen.kt`** (grid → rows). Add `HomeState` enum + state in `HomeViewModel`. Add `HomeRightRail`. Add `MusicPlayerRow` usage. |
| `feature:appdrawer` | **Refactor `AppDrawerScreen.kt`** (tile 68, label, category nav, search h56, colors). Add category logic to `AppDrawerViewModel`. |
| `core:model` | + `AppInfo.category` (new field). |
| `feature:iconpack` | **No logic change.** Pipeline used as-is. |

No new module. No rewrite. This keeps risk low and traceability clear.

### 4.2 Color tokens (new, verbatim from `.pen`)

Add to `core/designsystem/theme/Color.kt`:

```kotlin
// --- Warm Right Rail tokens (from homeApp.pen) ---
val SoftRailBg        = Color(0xFFD8C8B6) // right rail background (hrsLU)
val SoftDivider       = Color(0xFFD0C2B1) // 1px row divider (MrrnQ..)
val SoftTileWarm      = Color(0xFFF5EFE6) // icon tile / album circle (mdsQP, drawer tiles)
val SoftCategoryWash  = Color(0xFFE4D7C7) // icon library + storyboard card bg (QBhow, Az7qs)
val SoftDrawerBg      = Color(0xFFDCCDBA) // drawer screen bg (TpzL1)
val SoftDrawerStroke  = Color(0xFFC8B8A6) // drawer search pill border (V7udUl)
val SoftProgressTrack = Color(0xFFB9AA98) // music progress track (pvQO0)
val SoftIndexActive   = Color(0xFFB06F52) // alphabet rail active letter (fZMbk)
val SoftRailBgDark    = Color(0xFF2E3134) // dark editorial rail (q0n2Wd) — dark ref only
```

Drawer accent palette (example/fallback tints only):

```kotlin
val DrawerTintClay   = Color(0xFFB06F52)
val DrawerTintAmber  = Color(0xFFD19B62)
val DrawerTintPine   = Color(0xFF5F7A72)
val DrawerTintSage   = Color(0xFF6E8B86)
```

Extend `SoftColors` data class with: `railBg`, `divider`, `tileWarm`,
`categoryWash`, `drawerBg`, `drawerStroke`, `progressTrack`, `indexActive`.
Provide light + dark values (dark derived, warm adaptation).

### 4.3 Dimens tokens (new)

Add to `Dimens.kt`:

```kotlin
val railWidth        = 72.dp   // hrsLU
val railIcon         = 20.dp   // rail icons
val railPadTop       = 58.dp   // hrsLU
val railPadBottom    = 26.dp
val homeRowPaddingX  = 42.dp   // row content x (znb90)
val dividerHeight    = 1.dp
val searchRowIcon    = 24.dp
val albumArt         = 64.dp
val musicControl     = 18.dp   // play (largest)
val musicControlSmall= 16.dp   // skip-back / skip-forward
val progressHeight   = 5.dp
val iconLibTile      = 68.dp   // QBhow tile
val drawerTileNew    = 68.dp   // dBQmH (replaces drawerIconTile 104)
val drawerTileSymbol = 24.dp
```

Keep legacy `drawerIconTile`/`drawerIconSymbol` only if still referenced; else remove.

### 4.4 Spacing tokens (new)

```kotlin
val railGap   = 22.dp   // hrsLU gap
val rowBand   = 164.dp  // divider-to-divider distance (y 128→226→322→424)
```

### 4.5 Typography (new styles; follow `.pen` exactly — decision Q12)

Clock/date use **normal weight** in the new design (was bold):

```kotlin
// Type.kt additions
val clockLarge   = TextStyle(DmSans, Normal, 58.sp, letterSpacing = (-2).sp) // Time (IDSBb)
val dateNumber   = TextStyle(DmSans, Normal, 60.sp, letterSpacing = (-2).sp) // Date (ApuhU)
val rowDisplay   = TextStyle(DmSans, Normal, 28.sp, letterSpacing = (-0.5).sp) // weather + music title
val rowMeta      = TextStyle(DmSans, Normal, 13.sp, letterSpacing = 2.sp)     // search placeholder
val tweakLabel   = TextStyle(DmSans, Bold,   9.sp, letterSpacing = 1.5.sp)    // day
val tweakLabelLean = TextStyle(DmSans, Normal, 9.sp, letterSpacing = 1.2.sp)  // month / track
```

### 4.6 Motion tokens — `core/designsystem/theme/MotionTokens.kt` (new)

Source: node `Cvy3V` inside card `Z5V7Y9`.

```kotlin
package com.softhome.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/**
 * Motion system — "Small movement, clear purpose."
 * Source: homeApp.pen board L7ZAp, node Z5V7Y9 / Cvy3V.
 *
 * RAIL SLIDE  220ms
 * SEARCH FADE 160ms
 * MUSIC RISE  280ms
 * EASE  cubic-bezier(0.2, 0.8, 0.2, 1)
 */
object MotionTokens {
    const val RAIL_SLIDE_MS  = 220
    const val SEARCH_FADE_MS = 160
    const val MUSIC_RISE_MS  = 280

    val WarmEase: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    fun <T> railSlide(): TweenSpec<T>  = tween(RAIL_SLIDE_MS, easing = WarmEase)
    fun <T> searchFade(): TweenSpec<T> = tween(SEARCH_FADE_MS, easing = WarmEase)
    fun <T> musicRise(): TweenSpec<T>  = tween(MUSIC_RISE_MS, easing = WarmEase)
}
```

**Rule:** no hardcoded durations/easings in animations — everything goes through
these tokens. Reduced motion: when Compose's `LocalMotionDurationScale` scale
is 0 (system animator scale off), tokens resolve to 0ms automatically via the
Compose animation host; verify and document.

### 4.7 Home screen structure

```
Box(bg = surface #E8DFD0)
└─ Row(fillMaxSize)
   ├─ Column(weight 1, horizontal padding = homeRowPaddingX = 42dp)
   │   ├─ TimeRow        "04:35"          clockLarge, #2B2B2B
   │   ├─ Divider        (SoftDivider 1px, full row width)
   │   ├─ DateRow        "22" + day + month
   │   ├─ Divider
   │   ├─ WeatherRow     "Current 8°C"    rowDisplay
   │   ├─ Divider
   │   ├─ SearchRow      "f i n d  s o m e t h i n g" + search icon + dots  [tap -> SEARCH]
   │   ├─ Divider
   │   └─ MusicPlayerRow title/artist/track/album/controls/progress         [tap -> MUSIC]
   └─ HomeRightRail(width 72, bg SoftRailBg, pad 58/26, gap 22)
```

- Rows are **full-width, divider-separated** (decision Q2 — not cream cards).
- Rail sits at the far right, full height.

### 4.8 Right rail — `HomeRightRail` (new)

- 72dp wide, fill `SoftRailBg`, vertical, gap 22, pad top 58 / bottom 26,
  centered.
- 8 icons @ 20dp `#2B2B2B`, lucide: sparkles, circle-dot, message-circle,
  send, camera, wind, panel-left, phone.
- **Shortcuts** (decision Q3/Q10):
  | Icon | Target |
  |---|---|
  | sparkles | no-op (decorative) |
  | circle-dot | default browser |
  | message-circle | default messaging app |
  | send | default email app |
  | camera | default camera app |
  | wind | default weather app |
  | panel-left | system settings |
  | phone | default dialer |
  Icons without a resolvable target render normally but are no-op.
- SEARCH state: rail "expands" using `RAIL_SLIDE` (220ms).

### 4.9 Music player — `MusicPlayerRow` (new)

- **Static UI first** (decision Q1): renders the design's dummy values
  (`"play music."` / `"Djo"` / `"End of Beginning · Live from Chicago"`).
- Antenna/controls: skip-back 16, play 18, skip-forward 16 — play is no-op.
- Progress: track 104×5 r3 `#B9AA98`, fill 32×5 r3 `#8A5F43` (static 31%).
- Album: 64×64 circle `#F5EFE6`, stroke `#8A5F43` w1, disc-3 mark 20 `#625B52`.
- **MUSIC state:** the row **grows in-place** (`MUSIC_RISE` 280ms); rows above
  stay put (decision Q7).
- MediaSession integration deferred to a later phase (§8 Future).

### 4.10 Interaction states — Idle / Search / Music

Model in `feature:home`:

```kotlin
enum class HomeState { Idle, Search, Music }
```

| State | Visual | Motion token | Storyboard node |
|---|---|---|---|
| IDLE | home at rest; clock + weather + rows normal; rail minimal | — | `Az7qs` "Home at rest / clock + weather" |
| SEARCH | rail expands; search row focused (cursor, placeholder hidden) | `SEARCH_FADE` 160ms + `RAIL_SLIDE` 220ms | `m9OlxQ` "rail expands" |
| MUSIC | music row rises/expands, more prominent | `MUSIC_RISE` 280ms | `T8AA1` "player rises" |

**Triggers (decision Q5):** tap search row → SEARCH; tap music row → MUSIC;
tap outside / Back → IDLE.

### 4.11 App drawer redesign

- Root bg `SoftDrawerBg` `#DCCDBA` (not surface).
- **Category nav** (new): All / Communication / Entertainment / Tools.
  Mapping (decision Q8) from `ApplicationInfo.category`:
  - `CATEGORY_SOCIAL`, `CATEGORY_MESSAGE` → **Communication**
  - `CATEGORY_AUDIO`, `CATEGORY_VIDEO`, `CATEGORY_GAME` → **Entertainment**
  - `CATEGORY_PRODUCTIVITY`, `CATEGORY_MAPS`, `CATEGORY_NEWS`,
    `CATEGORY_IMAGE`, others → **Tools**
  - ungategorized / null → shown under **All** only
  - "All" = default tab (no filter).
- Grid tile **68dp r21** (was 104 r30) + **11pt label under tile** (new).
- Search pill **h56 r28** `#E8DFD0`, stroke `#C8B8A6` (was h52 r26 `#F6F0E7`).
- Alphabet rail: active `#B06F52`, idle `#81796D`.
- **Icon source unchanged** (decision Q9/Q4): renders whatever the icon pack
  pipeline produces. The colored glyphs in `TpzL1` are example pack art, not a
  required tint. The `DrawerTint*` tokens exist only as an optional fallback.
- Drawer opens via **swipe-up** (decision Q7-prev, unchanged from P1).

### 4.12 Error handling

- Missing shortcut targets → icon renders, tap is no-op (no crash).
- Empty category → empty state text (reuse existing "No apps match that search."
  pattern / a "No apps in this category." variant).
- Static music data → no failure path. Future MediaSession errors handled later.

---

## 5. Testing plan

### Unit (new)
| Test | Asserts |
|---|---|
| `MotionTokensTest` | durations == 220 / 160 / 280; easing control points == (0.2,0.8,0.2,1) |
| `HomeStateTest` | Idle → Search → Music → Idle transitions via ViewModel intents |
| `DrawerCategoryTest` | `ApplicationInfo.category` → tab mapping (Social/Message→Communication, Audio/Video/Game→Entertainment, Productivity/…→Tools, null→All-only) |
| `RailShortcutTest` | 8 rail icons resolve to expected target app / no-op |

### Compose UI
| Test | Asserts |
|---|---|
| `HomeScreenTest` | renders TimeRow, DateRow, WeatherRow, SearchRow, MusicPlayerRow + rail with 8 icons; tap search → SEARCH; tap music → MUSIC |
| `AppDrawerScreenTest` | category nav switches filter; tile is 68; label present; search pill height 56 |
| `HomeWidgetsTest.kt` (existing) | **must be updated** — old `HomeScreen` contract changed |

### Manual
- Screenshot compare vs `znb90`, `L7ZAp`, `TpzL1`.
- TalkBack: rail icons, rows, music controls, category tabs announce.
- Contrast: rail `#2B2B2B` on `#D8C8B6`; drawer label `#3A3A3A` on tiles; dividers.
- Reduced-motion: state transitions respect animator scale.

---

## 6. Documentation updates (follow prior pattern)

| Doc | Update |
|---|---|
| `00-DESIGN-SOURCE.md` | **Rewrite** traceability with new nodes (`znb90`/`L7ZAp`/`TpzL1`/`KkPN3`); drop stale node list |
| `02-DESIGN-SYSTEM.md` | Add new color/dimens/spacing/typography tokens + motion tokens |
| `03-FEATURE-MAP.md` | Add A9 (right rail), A10 (music player), A11 (3 states); update A3 (search row); update C1–C4 (drawer redesign) |
| `04-ASSUMPTIONS.md` | New section H: static music, rail shortcut mapping, drawer categories, drawer icon colors |
| `05-PROGRESS.md` | Session 5 log (Warm Right Rail) |
| `09-DECISIONS-LOG.md` | D-017 home list redesign, D-018 motion tokens, D-019 drawer redesign, D-020 rail shortcuts, D-021 static music |

---

## 7. Phase plan (end-to-end)

Each phase = one focused PR-sized unit with its own exit criteria. Do not start a
phase until the previous phase's exit criteria pass.

### Phase 0 — Foundations: tokens + assets
**Goal:** all new design tokens and icons exist; nothing rendered yet.
- Add color tokens (§4.2), dimens (§4.3), spacing (§4.4), typography (§4.5).
- Create `MotionTokens.kt` (§4.6).
- Add/convert icon drawables: sparkles, circle-dot, send, wind, panel-left,
  skip-back, skip-forward, disc-3, tent, images, book-open, wallet-cards,
  shield, bot, globe, contact, download, box, folder-symlink, folder-open,
  graduation-cap, heart-handshake, shopping-bag, messages-square, users,
  utensils, wallet (as needed by rail + drawer).
- Extend `SoftColors` with new semantic fields (light + dark).
- **Tests:** `MotionTokensTest`.
- **Exit:** module compiles; `MotionTokensTest` green; no screen changed yet.

### Phase 1 — Model + domain
**Goal:** carry the data the new UI needs.
- Add `AppInfo.category` (nullable `Int?` mirroring `ApplicationInfo.category`);
  map in `AppRepository`.
- Add drawer category model + mapping (pure logic in `core:model` or drawer domain).
- Add rail shortcut target map (package-independent: resolve "default dialer",
  "default camera", etc. via `Intent` resolution — or a small resolver).
- **Tests:** `DrawerCategoryTest`, `RailShortcutTest` (pure parts).
- **Exit:** logic tested; no UI changed.

### Phase 2 — Design-system components
**Goal:** reusable atoms for the new home.
- New file `core/designsystem/atom/HomeRows.kt`:
  - `HomeRow` (full-width + trailing divider wrapper).
  - `HomeDivider` (318×1 `SoftDivider`).
  - `RailIcon` (20dp line icon, tint, click target).
  - `MusicPlayerRow` (static UI per §4.9).
- Keep old atoms where reused; mark unused ones for removal in Phase 7.
- **Exit:** atoms compile + a preview/test renders each.

### Phase 3 — Home screen refactor (list + rail)
**Goal:** home renders the new list layout + rail (IDLE only).
- Rewrite `HomeScreen.kt`: Box → Row { rows + `HomeRightRail` }.
- Implement `HomeRightRail` using `RailIcon` × 8 with shortcut targets.
- Wire MusicPlayerRow (static).
- Remove old grid usage from home (grid stays in drawer only).
- **Tests:** update `HomeWidgetsTest`; add `HomeScreenTest` (renders rows + rail).
- **Exit:** home matches `znb90` at IDLE; UI tests green.

### Phase 4 — Interaction states + motion
**Goal:** IDLE / SEARCH / MUSIC with tokenized animation.
- Add `HomeState` to `HomeViewModel` + intents.
- Wire tap-row triggers + reset (tap outside/Back).
- Animate rail expand (`RAIL_SLIDE`), search focus (`SEARCH_FADE`),
  music grow in-place (`MUSIC_RISE`). Rows above stay put.
- Respect reduced motion.
- **Tests:** `HomeStateTest` + UI state-transition assertions.
- **Exit:** all three states reachable + animated per tokens.

### Phase 5 — App drawer redesign
**Goal:** drawer matches `TpzL1`.
- Tile 68 r21 + labels; category nav; search h56 r28; alphabet active color;
  drawer bg `SoftDrawerBg`.
- Wire category filtering to ViewModel (Phase 1 logic).
- **Tests:** `AppDrawerScreenTest`; update `AlphabetIndexTest` if needed.
- **Exit:** drawer matches `TpzL1`; tests green.

### Phase 6 — Verification on device
**Goal:** prove it works like P1.5 did.
- Build debug APK, install on emulator (Android 15 / API 35).
- Set as default home; screenshot Home (all 3 states) + Drawer.
- Compare vs `znb90` / `L7ZAp` / `TpzL1`.
- TalkBack pass; contrast check; reduced-motion check.
- Save screenshots to `docs/screenshots/`.
- **Exit:** evidence captured; no crashes; regressions none.

### Phase 7 — Cleanup + docs
**Goal:** remove dead code; update docs.
- Remove unused legacy atoms/sizes now confirmed dead (e.g. old mic pill if
  fully replaced, old `drawerIconTile`/`drawerIconSymbol` if unused).
- Update docs `00`, `02`, `03`, `04`, `05`, `09` per §6.
- **Exit:** docs consistent; build clean; all tests green.

### Phase 8 — Final review / handoff
**Goal:** checkpoint.
- Summarize what changed/kept, evidence, and open items.
- **STOP for user confirmation** before any further step (per instruction).

---

## 8. Out of scope / deferred

- **Real music playback / MediaSession** — static UI now; integration later.
- **Dark Editorial home (`KkPN3`)** — reference only; not implemented now.
- **Real voice search** — still a stub (unchanged).
- **Folder, widgets, settings, onboarding (P2–P4)** — untouched.
- **Custom wallpaper** — unchanged.

---

## 9. Open risks

| Risk | Mitigation |
|---|---|
| Rail icon → app resolution varies by OEM | Resolve via standard `Intent` (dialer/camera/browser/messaging/email/settings); no-op on failure |
| Drawer category coverage incomplete | Unknown categories fall under "All"; "Tools" is the catch-all for mapped-but-unsorted |
| Static music looks "dead" | Label clearly in code as placeholder; keep the component ready for MediaSession |
| Motion over-animates on low-end | All durations < 300ms + reduced-motion aware |
| Large `HomeScreen` file | Keep atoms in designsystem; home stays orchestration-only |
