# 02 — Design System

All values here come from [`design/homeApp.pen`](../design/homeApp.pen) (see
[00-DESIGN-SOURCE](00-DESIGN-SOURCE.md)). **Nothing is hardcoded per screen** —
every screen consumes these tokens.

Target files (created in P1):

```
core/designsystem/src/main/kotlin/.../theme/
├─ Color.kt        # palettes (light + the KkPN3 dark)
├─ Type.kt         # DM Sans + IBM Plex Mono text styles
├─ Shape.kt        # squircle radii + pills
├─ Spacing.kt      # spacing scale
├─ Elevation.kt    # soft-shadow tokens
├─ Dimens.kt       # icon sizes, card paddings
└─ Theme.kt        # SoftHomeTheme { colors, typography, shapes }
```

---

## Color tokens → Kotlin

```kotlin
// Color.kt  — light
val SoftBackground      = Color(0xFFEDE6D8)
val SoftSurface         = Color(0xFFE8DFD0)   // screen bg + cream icon stroke
val SoftCard            = Color(0xFFF6F0E7)
val SoftCardAlt         = Color(0xFFF5EFE6)
val SoftAccentWash      = Color(0xFFD8C8B6)
val SoftTile            = Color(0xFF2B2B2B)
val SoftTileAlt         = Color(0xFF2F2F2F)
val SoftTextTitle       = Color(0xFF1A1A1A)
val SoftTextBody        = Color(0xFF625B52)
val SoftTextMuted       = Color(0xFF81796D)
val SoftIndexLetter     = Color(0xFF6E675E)
val SoftOnDark          = Color(0xFFF7F0E5)
val SoftIconStroke      = Color(0xFFE8DFD0)
val SoftAccent          = Color(0xFF8A5F43)
val SoftAccentDeep      = Color(0xFF7A553D)
val SoftCodeAccent      = Color(0xFFCBB39D)
```

### Dark palette — the `KkPN3` "Dark Editorial" named palette (P4c)

The `.pen` now **names** a dark palette in frame `KkPN3` ("Home Screen Mockup — Dark
Editorial"). P4c renders it verbatim, replacing the earlier derived warm-dark guess.
Values are pinned by `DarkPaletteTest` and documented in
[04 section M](04-ASSUMPTIONS.md).

```kotlin
// Color.kt — dark (verbatim KkPN3, 2026-09-25)
val DarkBackground  = Color(0xFF18191A)   // KkPN3 screen bg
val DarkSurface     = Color(0xFF18191A)
val DarkRailBg      = Color(0xFF2E3134)   // KkPN3 dark rail
val DarkDivider     = Color(0xFF343638)   // KkPN3 divider
val DarkTextTitle   = Color(0xFFF2EEE7)   // KkPN3 primary text
val DarkTextBody    = Color(0xFFD2CBC1)   // KkPN3 "day"
val DarkTextMuted   = Color(0xFF918F8B)   // KkPN3 "month"
val DarkStatusText  = Color(0xFFE2DDD5)   // KkPN3 soft text
val DarkProgressTrack = Color(0xFF676866) // KkPN3 progress track
// Derived (frame shows no card/menu/popup surface) — same neutral family:
val DarkCard        = Color(0xFF24262A)
val DarkCardAlt     = Color(0xFF2A2C30)
val DarkDrawerBg    = Color(0xFF1F2124)
val DarkAccent      = Color(0xFFC98B6E)   // interactive emphasis on the neutral dark
```

### Semantic mapping (what screens use)

| Semantic | Light | Dark |
|---|---|---|
| `background` | SoftBackground | DarkBackground |
| `surface` | SoftSurface | DarkSurface |
| `card` | SoftCard | DarkCard |
| `tile` (icon bg) | SoftTile | DarkTile |
| `onTile` (icon stroke) | SoftIconStroke | DarkIconStroke |
| `textPrimary` | SoftTextTitle | DarkTextTitle |
| `textBody` | SoftTextBody | DarkTextBody |
| `textMuted` | SoftTextMuted | DarkTextMuted |
| `accent` | SoftAccent | DarkAccent |
| `primaryAction` bg | SoftTile | DarkTile |
| `onPrimaryAction` | SoftOnDark | SoftTile |

> Accessibility: title/body/muted pairings on cream were sized to meet **WCAG AA**
> (≥4.5:1). `SoftTextMuted` on `SoftCard` is the lowest-contrast pair — verify at
> implementation (target ≥4.5:1 for body, ≥3:1 for large text). See
> [06-TESTING](06-TESTING.md).

---

## Typography → Kotlin

Family: **DM Sans** (bundled in `res/font/`), mono: **IBM Plex Mono**.

```kotlin
// Type.kt (excerpt)
val SoftTypography = Typography(
  displayLarge = TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 60.sp, letterSpacing = (-2).sp), // clock
  headlineLarge= TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 42.sp, lineHeight = 44.sp),
  headlineMedium=TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 30.sp), // weather temp / titles
  titleLarge   = TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 24.sp),
  bodyLarge    = TextStyle(fontFamily = DmSans, fontWeight = Normal, fontSize = 16.sp, lineHeight = 23.sp),
  bodyMedium   = TextStyle(fontFamily = DmSans, fontWeight = Normal, fontSize = 14.sp, lineHeight = 20.sp),
  labelSmall   = TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 11.sp, letterSpacing = 1.5.sp), // eyebrows
  labelMedium  = TextStyle(fontFamily = DmSans, fontWeight = Bold,   fontSize = 13.sp),
)
val SoftMono = TextStyle(fontFamily = IbmPlexMono, fontSize = 14.sp, lineHeight = 22.sp) // KWGT code block
```

---

## Shape tokens → Kotlin

```kotlin
// Shape.kt
val SoftShapes = Shapes(
  small        = RoundedCornerShape(12.dp),   // tags
  medium       = RoundedCornerShape(18.dp),   // settings rows
  large        = RoundedCornerShape(24.dp),   // cards
  extraLarge   = RoundedCornerShape(32.dp),   // sheets / mockup panels
)
// Icons: squircle ≈ 30% of tile size
fun iconShape(size: Dp) = RoundedCornerShape(size * 0.30f) // 62→19, 104→30
// Pills: full
val PillShape = RoundedCornerShape(percent = 50)
```

---

## Spacing scale → Kotlin

```kotlin
// Spacing.kt
object Spacing {
  val xs = 4.dp;  val sm = 8.dp;  val md = 12.dp
  val lg = 16.dp; val xl = 18.dp; val xxl = 24.dp
  val xxxl = 28.dp; val huge = 36.dp
}
```

Design-specified spacings: card padding `24`, weather inner `18`, icon grid gap
`18`, drawer grid gap `26`/`18`, home screen padding `28/24/24/24`, mockup gap `18`.

---

## Elevation → Kotlin (soft shadows, **no borders**)

Compose `Modifier.shadow` uses a single `ambientColor`/`spotColor`. Map each design
shadow to a token:

```kotlin
// Elevation.kt
data class SoftShadow(val elevation: Dp, val color: Color, val dy: Dp)
val TileShadow   = SoftShadow(8.dp,  Color(0xFFC7B9A8), 4.dp)
val CardShadow   = SoftShadow(14.dp, Color(0xFFCFC2B2), 7.dp)
val SearchShadow = SoftShadow(12.dp, Color(0xFFCFC2B2), 6.dp)
val RecCardShadow= SoftShadow(18.dp, Color(0xFFD0C2B1), 8.dp)
val MenuShadow   = SoftShadow(18.dp, Color(0xFFBFAF9E), 8.dp)
```

Applied via a `Modifier.softShadow(SoftShadow)` helper (custom draw behind, since
Compose shadow color control is limited — implementation detail noted in code).

---

## Reusable atoms (`core:designsystem`)

| Atom | Backed by design node | Props |
|---|---|---|
| `SoftCard` | `o0Fi0` / `X7SHLQ` | fill=card, radius=large, softShadow=CardShadow/RecCard |
| `PillSearchBar` | `ws286` / `kcUeA` | icon slot only (mic / search), h=50/52 |
| `LoupeIcon` / `MicIcon` | lucide `mic`, `search` | stroke cream/charcoal by context |
| `SoftToggle` | `UPa9N` (42×22 r11) | checked state, knob 16 cream |
| `PageIndicator` | *(assumption)* | active pill = tall line, inactive = short |
| `CreamBadgeDot` | `ha6OA` | 15% size, cream, soft glow, no count |
| `LineIcon` | all lucide symbols | `icon: LucideIcon`, size, tint |
| `AppIconTile` | `S1eRI` / drawer tile | size, radius≈30%, tile color, symbol |
| `EyebrowLabel` | `gHHFV` | uppercase, accent, letterSpacing 2 |
| `SettingsRow` | `IrKIT` | label + value/toggle, h42 r18 |

Icon symbols are **lucide** (per the design: `mail`, `globe`, `image`, `music`,
`phone`, `message-circle`, `file-text`, `camera`, `calendar-days`, `map-pin`,
`settings`, `calculator`, `clock-3`, `folder`, `cloud-sun`, `compass`,
`alarm-clock`, `play`, `pen-line`, `phone-call`, `sun`, `arrow-up-right`,
`info`, `trash-2`, `x`, `pencil`, `grid-2x2`, `mic`, `search`). Use a
`lucide-android`-style source or bundled vector drawables with a consistent
stroke weight.

---

## Warm Right Rail tokens (2026-09-24)

Source: `.pen` frames `znb90` (home), `L7ZAp` (system board), `TpzL1` (drawer).
These replace the P1 home grid tokens. Full detail:
[`superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md`](superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md).

### Colors → Kotlin (`Color.kt`)

```kotlin
val SoftRailBg        = Color(0xFFD8C8B6) // right rail background
val SoftDivider       = Color(0xFFD0C2B1) // 1px row divider
val SoftTileWarm      = Color(0xFFF5EFE6) // icon tile / album circle
val SoftCategoryWash  = Color(0xFFE4D7C7) // icon library / storyboard card
val SoftDrawerBg      = Color(0xFFDCCDBA) // drawer screen bg
val SoftDrawerStroke  = Color(0xFFC8B8A6) // drawer search pill border
val SoftProgressTrack = Color(0xFFB9AA98) // music progress track
val SoftIndexActive   = Color(0xFFB06F52) // alphabet active letter
val SoftRailBgDark    = Color(0xFF2E3134) // dark editorial rail (reference only)
// Drawer tint fallbacks (optional; icons use the icon-pack pipeline):
// REMOVED in P3.5 — replaced by the drawer color system below.
```

### P3.5: Drawer icon color system (2026-09-25)

Source: `.pen` frame `TpzL1` "Warm App Drawer — Unique Icon Grid". The drawer
renders **cream tiles + per-category colored glyphs** (not monochrome charcoal).
Full detail: [`superpowers/specs/2026-09-24-p35-drawer-icon-redesign-design.md`](superpowers/specs/2026-09-24-p35-drawer-icon-redesign-design.md).

```kotlin
// Tile backgrounds
val DrawerTileCream    = Color(0xFFF6F0E7) // $tile-cream (default)
val DrawerTileSelected = Color(0xFFD19B62) // $selected-tile (active/selected)
val DrawerIconOnSelected = Color(0xFFF5EFE6) // $selected-icon

// Per-category glyph colors
val DrawerIconCommunication = Color(0xFF5F7A72) // $icon-communication
val DrawerIconSocial        = Color(0xFF6E8B86) // $icon-social
val DrawerIconProductivity  = Color(0xFF8A5F43) // $icon-productivity
val DrawerIconMedia         = Color(0xFFD19B62) // $icon-media
val DrawerIconTravel        = Color(0xFFB06F52) // $icon-travel
val DrawerIconFinance       = Color(0xFF4D7C8A) // $icon-finance
val DrawerIconNeutral       = Color(0xFF625B52) // $icon-neutral (fallback)
```

Semantic fields added to `SoftColors`: `drawerTileCream`, `drawerTileSelected`,
`drawerIconOnSelected`, `drawerIconCommunication`, `drawerIconSocial`,
`drawerIconProductivity`, `drawerIconMedia`, `drawerIconTravel`,
`drawerIconFinance`, `drawerIconNeutral` (light + warm-dark values).

Category→color mapping (`DrawerIconColor`, pure):
`SOCIAL`/`IMAGE`→Social, `PRODUCTIVITY`→Productivity, `GAME`/`AUDIO`/`VIDEO`→Media,
`NEWS`→Finance, `MAPS`→Travel, unknown→Neutral (with a communication-glyph
heuristic: `Phone`/`Mail`/`CalendarDays` + unknown category → Communication).

Semantic fields added to `SoftColors`: `railBg`, `divider`, `tileWarm`,
`categoryWash`, `drawerBg`, `drawerStroke`, `progressTrack`, `indexActive`
(light + warm-dark values).

### Dimens → Kotlin (`Dimens.kt`)

| Token | Value | Node |
|---|---|---|
| `railWidth` | 72 | `hrsLU` |
| `railIcon` | 20 | rail icons |
| `railPadTop` / `railPadBottom` | 58 / 26 | `hrsLU` |
| `homeRowPaddingX` | 42 | row content |
| `dividerHeight` | 1 | dividers |
| `searchRowIcon` | 24 | `N7ICqS` |
| `albumArt` | 64 | `mdsQP` |
| `musicControl` / `musicControlSmall` | 18 / 16 | `x5SL5` |
| `progressHeight` | 5 | `pvQO0` |
| `drawerTileNew` / `drawerTileRadius` / `drawerTileSymbol` | 68 / 21 / 24 | `dBQmH` |
| `drawerSearchNewHeight` / `drawerSearchNewRadius` | 56 / 28 | `V7udUl` |

### Spacing (`Spacing.kt`)

`railGap = 22` (rail icon gap), `rowBand = 164` (divider-to-divider distance).

### Typography (`Type.kt`)

The redesign uses **normal** weight for the clock / date / row display:

| Style | Family | Size | Weight | ls |
|---|---|---|---|---|
| `ClockLarge` | DM Sans | 58 | normal | −2 |
| `DateNumber` | DM Sans | 60 | normal | −2 |
| `RowDisplay` | DM Sans | 28 | normal | −0.5 |
| `RowMeta` | DM Sans | 13 | normal | 2 |
| `TweakLabel` | DM Sans | 9 | bold | 1.5 |
| `TweakLabelLean` | DM Sans | 9 | normal | 1.2 |

### Motion → Kotlin (`MotionTokens.kt`)

```kotlin
object MotionTokens {
    const val RAIL_SLIDE_MS = 220
    const val SEARCH_FADE_MS = 160
    const val MUSIC_RISE_MS = 280
    val WarmEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    fun <T> railSlide(): TweenSpec<T> = tween(RAIL_SLIDE_MS, easing = WarmEase)
    fun <T> searchFade(): TweenSpec<T> = tween(SEARCH_FADE_MS, easing = WarmEase)
    fun <T> musicRise(): TweenSpec<T> = tween(MUSIC_RISE_MS, easing = WarmEase)
}
```

Rule: no screen hardcodes a duration/easing — every animation uses these helpers.

### Warm Right Rail atoms (`core:designsystem/atom/HomeRows.kt`)

| Atom | Backed by node | Props |
|---|---|---|
| `HomeRow` | `MrrnQ`… | full-width row + optional trailing divider / click |
| `HomeDivider` | `MrrnQ`… | 1px `SoftDivider` |
| `RailIcon` | `vsygX`… | line icon (20), tint, click |
| `MusicPlayerRow` | `QTwqr`… | title/artist/track, album (64), controls, progress |

### P2 atoms (2026-09-25) — `HomeWidgetRows.kt` / `FolderTile.kt`

No `.pen` mock exists for P2; these reuse the Warm Right Rail row vocabulary
(flat rows, `RowDisplay`/`TweakLabel` type, Warm progress tokens). `SoftWidgets.kt`
(the old cream-card widgets + 62dp folder squircle) was **removed**.

| Atom | Backed by | Props |
|---|---|---|
| `CalendarRowContent` | *(assumption)* | big day number + weekday/month + up to 2 static event lines |
| `BatteryStorageRowContent` | *(assumption)* | real battery % + storage %, thin Warm progress bar |
| `NotesRowContent` | *(assumption)* | collapsed preview / expanded `BasicTextField` |
| `ThinWarmProgressBar` | `pvQO0` (reused) | 5px `progressTrack` track + `accent` fill |
| `FolderTile` | `dBQmH` (reused) | 68 r21 `tileWarm` tile + 2×2 mini icon slot + label |
| `FolderPopupBody` | `Shape.large` | cream r24 card, editable title + app-grid slot |

New tokens: `Dimens.folderTile` (68), `folderTileRadius` (21), `folderMiniIcon` (22),
`folderPopupRadius` (24), `widgetProgressHeight` (5); `MotionTokens.notesExpand()`
(280ms, WarmEase); icons `LineIcon.Battery`, `LineIcon.HardDrive`.

### P3 atoms (2026-09-25) — `SoftToggle.kt` / `SettingsRow.kt` / `AppContextMenu.kt`

No `.pen` mock exists for P3; these reuse the Warm Right Rail vocabulary (cream
surfaces, r18/r24 radii, `RowDisplay`/`labelMedium` type, warm accents).

| Atom | Backed by | Props |
|---|---|---|
| `SoftToggle` | `UPa9N` (retired) — built from tokens | 42×22 pill, charcoal track on / warm track off, 16dp knob; `Role.Switch`; reuses `MotionTokens.railSlide()` |
| `SettingsRow` | `Shape.medium` (r18) | label + supporting, click, chevron, `toggle` slot, inline **up/down** reorder buttons |
| `AppContextMenu` | `Shape.large` (r24) | row-style long-press menu over a dim scrim; `ContextMenuItem(label, icon, enabled, destructive, onClick)` |

New P3 tokens: colors `destructive` (warm clay `#9A4B33`) + `disabled` (`#AFA493`)
light/dark; `Dimens.menuRadius` (24), `menuRowMinHeight` (48), `menuIcon` (20),
`menuScrim` (0.53), `settingsRowRadius` (18), `settingsRowMinHeight` (52),
`settingsChevron` (18), `moveButton` (28); icons `info`, `trash_2`, `chevron_right`,
`arrow_up`, `arrow_down`, `x`, `external_link`.

### System UI (P3 / F1–F2)

`SystemBarAppearance` (`:app`) sets `isAppearanceLightStatusBars` /
`isAppearanceLightNavigationBars` from the app theme (dark icons on cream, light on
warm-dark) over transparent bars, and hides the nav bar under gesture navigation.
`SoftHomeTheme(darkTheme)` follows `ThemeMode` (Light/Dark/System) from settings.

