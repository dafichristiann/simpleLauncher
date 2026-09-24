# P3 — System UI (F) + Settings (G) — Design Spec

> **Date:** 2026-09-25
> **Type:** Architectural (two subsystems: F system-UI adaptation, G settings panel),
> built on the Warm Right Rail design system (Session 5) and the P2 widgets/folders
> layer (Session 6).
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Status:** Spec written — awaiting user review before implementation.

This document is the single source of truth for **P3**. It contains the `.pen`
re-audit, the five user-confirmed decisions, the agreed design, exact tokens, and an
end-to-end phase plan (Phase 0 → Phase 8).

---

## 0. User-confirmed decisions (locked)

These five answers were given explicitly and are not open for re-litigation:

| # | Topic | Decision |
|---|---|---|
| **P3-1** | **Minimum always-visible home rows** | Locked set = **Time, Date, Weather** (always on). **Search / Music / Calendar / Battery-Storage / Notes** are user-toggleable. The bottom row (Notes) is **not** special-cased; it is toggleable like the others. The locked set guarantees the home never renders empty. |
| **P3-2** | **Widget row reorder** | Ship **up/down move buttons** in the settings **Widgets** section (reorder works, testable, no gesture subsystem). **Drag-and-drop reorder is deferred to P4** — together with the P2-deferred app→folder drag — as one gesture subsystem. |
| **P3-3** | **Long-press menu targets** | **App drawer tiles + right-rail icons.** Long-press a drawer tile → row-style menu; long-press a rail icon → row-style menu (App Info / Edit Shortcut / Remove). Covers the brief's *App Info, Uninstall, Remove, Edit Icon, Shortcuts*. |
| **P3-4** | **Uninstall of system/protected apps** | The menu shows **Uninstall visible-but-disabled (greyed)** for `isSystem` or otherwise unremovable apps, with **App Info always available**. Never attempt an uninstall that will throw. |
| **P3-5** | **Active icon-pack persistence (resolves `04` #36)** | P3 **persists** `activeIconPackId` (already a `LauncherPrefs` field / DataStore key) and **re-parses / rehydrates** the pack on cold start so the chosen pack survives process death. Closes the P1.5 open item. |
| **P3-6** | **Wallpaper section** | Ship a **flat warm default** + a **system-wallpaper picker** (`WallpaperManager` `ACTION_CHANGE_LIVE_WALLPAPER` fallback to `ACTION_SET_WALLPAPER`). The **live-wallpaper *engine* stays deferred** (`04` #12). |

**Deferrals carried in (also locked):**

| Item | Status | Note |
|---|---|---|
| MediaSession (#39) | **Deferred** (not deleted) | Music row stays static UI. |
| KkPN3 dark editorial mode | **Deferred** | Reference only; not executed. |
| Live calendar events (`CalendarContract`) | **Deferred** | Static/empty list. |
| Drag-and-drop — app→folder (from P2) | **Deferred to P4** | Gesture subsystem. |
| Drag-and-drop — row reorder (new) | **Deferred to P4** | Per **P3-2**. |
| Live-wallpaper engine | **Deferred** | Flat default + system picker only (**P3-6**). |
| Backup & restore settings (G4) | **Deferred to P4** | Not part of P3. |

---

## 1. `.pen` re-audit (2026-09-25) — no P3 nodes exist

The current `.pen` (103,422 bytes, 288 named nodes, **unchanged since 2026-09-24**)
was parsed node-by-node. The only top-level frames remain those from the Warm Right
Rail redesign:

| Node ID | Name | Role |
|---|---|---|
| `znb90` | Home Screen Mockup — Warm Right Rail | Primary home mockup |
| `L7ZAp` | Warm Home Screen — Full System Board | Spec sheet (4 panels) |
| `TpzL1` | Warm App Drawer — Unique Icon Grid | App drawer |
| `KkPN3` | Home Screen Mockup — Dark Editorial | Dark reference only |

**Keyword sweep for P3 concepts:**

| Keyword | Hits | Actual meaning in the file |
|---|---|---|
| `status` | 1 | Only the node name **"Drawer Status"** — a `9:41 ◦ ◦ ▪` mock snippet on the drawer frame. **Not** a status-bar spec. |
| `nav` / `navigation` | 0 / 0 | Nothing |
| `long` / `press` / `menu` / `uninstall` / `remove` / `shortcut` | 0 | Nothing |
| `setting` / `toggle` / `switch` / `appearance` / `wallpaper` / `gesture` / `grid size` | 0 | Nothing (the **Gesture Bar** `OGsws` node exists only as a *drawable* — a 98×4 pill — not as a gesture spec) |
| `system` | 7 | Only "Full System Board" titles (the spec-sheet frame). Not system-UI. |

**Stale design-node IDs cited in `docs/03` — verified absent from the `.pen`:**

| ID | Cited for | Present in `.pen`? |
|---|---|---|
| `ogMkZ` | F1 status bar | ❌ **no** |
| `Fzobx` / `pu2gg` | F3 long-press menu | ❌ **no** |
| `nFk4u` | G1 settings sections | ❌ **no** |
| `UPa9N` | G2 custom toggle | ❌ **no** |

These are from the **retired P1 file** — same class of staleness P2 documented for
`u0a8RP` / `o0Fi0` / `nFk4u`.

**Conclusion:** there is **no** status-bar treatment, nav-bar handling, long-press menu,
settings panel, or toggle mock. The `.pen` stays **read-only**. All P3 visuals are
derived from the **existing Warm Right Rail vocabulary**:

- **Colors** — `accent #8A5F43`, `tileWarm #F5EFE6`, `divider #D0C2B1`, `drawerBg`,
  `textPrimary #1A1A1A`, `textBody #625B52`, `textMuted #81796D`.
- **Type** — `RowDisplay` (28 normal), `TweakLabel` (9 bold ls1.5), `TweakLabelLean`
  (9 normal ls1.2), `labelMedium` (13 bold) for menu rows / section labels.
- **Shape** — settings rows **r18** (`Shape.medium`), popups/menus **r24**
  (`Shape.large`), pills = height/2.
- **Motion** — `MotionTokens` only; no new durations.

---

## 2. Existing scaffolding (real, reused — not dead code)

Unlike P2 (which found dead scaffolding on the retired language), P3 finds **live**
scaffolding it extends:

| File / symbol | Current state | P3 action |
|---|---|---|
| `app/.../themes.xml` `Theme.SoftHome` | Already transparent status + nav bar, `windowLightStatusBar`/`windowLightNavigationBar = true`, `windowLayoutInDisplayCutoutMode = shortEdges` | **Extend**: add a theme-driven dark variant and a runtime `WindowInsetsController` pass so bar icon color follows the **app theme**, not just the OS default (**F1**) |
| `app/.../SettingsStubActivity.kt` | Real shell (eyebrow + headline + body), not dead | **Replace** body with the real settings panel (**G1**); keep the activity + manifest entry |
| `PrefsRepository` (`core:data`) | DataStore-backed; persists `grid`, `activeIconPackId`, `maskUnsupportedApps`, `ThemeMode`, `showNotificationBadges` | **Extend**: add home **row visibility/order**, **grid spacing**, and wire the existing `setActiveIconPack` (**G1–G3, P3-5**) |
| `LauncherPrefs` (`core:model`) | Already has `activeIconPackId`, `iconOverrides`, `darkTheme`, `grid` | **Extend**: add `homeRows: List<HomeRowPref>` (visible + order) |
| `IconPackRepositoryImpl` | Keeps active pack **in memory only** (`_activePack`) | **Extend**: persist + rehydrate on cold start (**P3-5**) |
| `FolderPopupBody` / `FolderPopup` (`feature:appdrawer`) | Proven **row-card popup** over a dim scrim | **Reuse the pattern** for the long-press menu (**F3**) — same scrim, r24 card, 1px divider rows |
| `AppRepository` | Reads `AppInfo` (`isSystem`, `category`) | **Reuse** for uninstall eligibility (P3-4) |
| `LineIcon` set | 55+ lucide glyphs incl. `Settings`, `Info`, `Trash2`? | **Check**: `info` / `trash-2` / `pencil` / `grid-2x2` are named in `docs/02` but need a **drawable check** in Phase 0 |

> **Note:** `docs/02` line 180 lists `info`, `trash-2`, `pencil`, `grid-2x2`, `sun`,
> `arrow-up-right` among the intended symbols. Phase 0 audits which drawables actually
> exist and adds any missing glyphs in the same ASCII-safe converter style (D-010).

---

## 3. Visual comparison — what P3 adds vs existing surfaces

### Long-press menu: system default → row-style card

| Aspect | Android default (`PopupMenu`) | P3 (Warm Right Rail) |
|---|---|---|
| Container | platform rounded rect, default elevation, default padding | cream `card` **r24** (`Shape.large`), flat rows, dim scrim behind |
| Rows | platform 48dp list items | **row-style rows**: label (`labelMedium`/`bodyMedium`) left, leading `LineIcon` 20dp, 1px `divider` between rows |
| Destructive row | platform text color | **"Uninstall"** in a muted/destructive treatment, **greyed when not removable** (P3-4) |
| Anchoring | near the icon | anchored near the pressed element, clamped to screen |

### Custom toggle: system `Switch` → pill atom

| Aspect | Material3 `Switch` | P3 `SoftToggle` (Warm) |
|---|---|---|
| Shape | platform | **pill** (`height/2`), 42×22 (`Dimens.toggleWidth/Height`) |
| Track on | theme primary | **charcoal** `tile` (`#2B2B2B` light / `#EDE6D8` dark) |
| Track off | theme surfaceVariant | `progressTrack` / `#B9AA98`-adjacent warm grey |
| Knob | platform | 16dp (`Dimens.toggleKnob`) cream `onTile`; animates with `MotionTokens` |

### Settings panel: stub text → sectioned panel

| Aspect | P1 stub | P3 |
|---|---|---|
| Content | eyebrow + "Make it yours." + paragraph | **4 sections** (Appearance / Widgets / Wallpaper / Gestures), each a label + r18 rows |
| Rows | — | `SettingsRow` atom (`SoftCard`-family, r18) with label + value/toggle/chevron |
| Background | `surface` | `background` (`#EDE6D8`) with `card` rows |

---

## 4. Agreed design

### 4.1 Module / file plan

| Module | Change |
|---|---|
| `core:model` | **Extend** `LauncherPrefs` with `homeRows: List<HomeRowPref>` (id + visible + order). New `HomeRowPref` + `HomeRowKind` enum (Time, Date, Weather, Search, Music, Calendar, BatteryStorage, Notes). New pure `HomeRowLogic` (default order, locked set, move up/down, toggle guards). |
| `core:designsystem` | **New** `atom/SoftToggle.kt` (pill charcoal toggle). **New** `atom/AppContextMenu.kt` (row-style long-press menu card). **New** `atom/SettingsRow.kt` if not already present (label + value/toggle/chevron, r18). **Extend** `Dimens` (menu/settings sizes), `Color` (destructive/disabled row colors — reuse existing where possible). |
| `core:data` | **Extend** `PrefsRepository`: persist row visibility/order + grid spacing; wire `setActiveIconPack` (exists). **Extend** `FolderRepository` only if rail-shortcut edits persist (see F3). New `AppActionsRepository` (uninstall / app-info / shortcut intents; pure, testable, never throws). |
| `feature:home` | Read row prefs → render only visible rows in user order. Rail long-press → context menu. Wire theme-following system-bar appearance (F1 host). |
| `feature:appdrawer` | Drawer tile long-press → context menu (reuse `AppContextMenu`). |
| `feature:iconpack` | **Extend** `IconPackRepositoryImpl` to persist + rehydrate the active pack (P3-5). |
| `app` | **Rewrite** `SettingsStubActivity` body → real settings panel; add `SettingsActivity`(or keep name) routes + a "Settings" entry. Status/nav-bar appearance controller. Manifest: `SET_WALLPAPER`/query additions as needed. |

No new Gradle module for P3 UI (settings lives in `:app` as the existing stub does),
**except** if the settings panel grows past a thin shell, in which case an
`feature:settings` module is added per the D-008 multi-module rule. **Decision:** keep
settings in `:app` for P3 (it is a shell + rows); record the option in `09`.

### 4.2 Home row visibility + order (G / Widgets section)

```
Default order: Time → Date → Weather → Search → Music → Calendar → BatteryStorage → Notes
Locked (always visible): Time, Date, Weather
Toggleable: Search, Music, Calendar, BatteryStorage, Notes
```

- **Model:**

  ```kotlin
  enum class HomeRowKind { Time, Date, Weather, Search, Music, Calendar, BatteryStorage, Notes }
  data class HomeRowPref(val kind: HomeRowKind, val visible: Boolean)

  object HomeRowLogic {
      val LOCKED = setOf(HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather)
      val DEFAULT_ORDER = listOf(Time, Date, Weather, Search, Music, Calendar, BatteryStorage, Notes)
      fun default(): List<HomeRowPref>                       // all visible, DEFAULT_ORDER
      fun canHide(kind): Boolean                             // kind !in LOCKED
      fun toggle(prefs, kind): List<HomeRowPref>             // no-op if locked hidden
      fun moveUp(prefs, kind): List<HomeRowPref>             // swap with previous, clamped
      fun moveDown(prefs, kind): List<HomeRowPref>           // swap with next, clamped
      fun visibleInOrder(prefs): List<HomeRowKind>           // what HomeScreen renders
      fun sanitize(stored): List<HomeRowPref>                // unknown kinds dropped; missing kinds appended; locked forced visible
  }
  ```

- **Home rendering:** `HomeScreen` builds its row list from
  `HomeRowLogic.visibleInOrder(state.homeRows)`, mapping each `HomeRowKind` to its
  existing composable. Locked rows ignore their `visible=false` value (sanitize forces
  them true). Dividers are rendered **between visible rows only** (no leading/trailing
  orphan divider).

- **Settings UI (Widgets section):** one row per `HomeRowKind` showing the label + a
  `SoftToggle` (locked rows show the toggle **on + disabled**). Each toggleable row
  also shows **↑ / ↓** move buttons (P3-2). Order edits apply immediately.

### 4.3 Appearance section (G)

| Row | Control | Backing |
|---|---|---|
| Theme | 3-way (Light / Dark / System) | `PrefsRepository.setDarkTheme(ThemeMode)` (exists) |
| Icon pack | chevron → opens the existing `IconPackImportSheet` | `IconPackRepository` + `setActiveIconPack` (P3-5: now persisted) |
| Grid size | 3-way / stepper (columns 4/5/6) applied to the **drawer** grid | `PrefsRepository.setGrid(GridConfig)` (exists; home is row-based, so grid applies to drawer) |
| Spacing | stepper (Compact / Normal / Roomy → maps to a spacing multiplier on drawer + rows) | **new** persisted value; default Normal = current values |

> **Scope note:** "grid size" and "spacing" now affect the **drawer** (the only grid
> surface) plus row vertical rhythm. Home row *layout* is unchanged (rows stay
> full-width). This is stated explicitly to avoid implying a home grid returns.

### 4.4 Wallpaper section (G, P3-6)

| Row | Control | Behavior |
|---|---|---|
| Wallpaper | "Choose wallpaper" button | Fires `WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER` when resolvable, else `ACTION_SET_WALLPAPER` (system picker). |
| Background style | toggle: Flat warm (default) vs System wallpaper | Flat warm = the launcher paints `background` `#EDE6D8`; System = `windowShowWallpaper = true` already set, hide the flat fill. |

- **Live-wallpaper engine** (drawing a live wallpaper ourselves) stays **deferred**.
- Flat default is the shipped look; the system picker is the only live path.

### 4.5 Gestures section (G)

| Row | Control | Backing / status |
|---|---|---|
| Swipe up | (already) open app drawer | **Implemented** (Session 5) — shown read-only/label here |
| Swipe down | (new) open notifications | **Option stub** — attempts `expandNotifications` via status-bar reflection or is left **deferred** with a "coming soon" muted row |
| Double-tap | (new) lock screen | **Best-effort** (needs Device Admin / accessibility) — **deferred** with a muted row and a note (matches `04` #15) |
| Long-press home | (new) choose action | **Deferred** to P4 (gesture subsystem) |

> **Scope guard:** P3's Gestures section is a **settings surface for gestures that
> already exist** (swipe-up → drawer) plus clearly-labeled **muted rows** for ones still
> deferred. It does **not** build the gesture subsystem (that is P4 with drag-and-drop).

### 4.6 Status bar icon color adaptation (F1)

- **Baseline (already present):** transparent status bar, `windowLightStatusBar = true`,
  cutout `shortEdges`.
- **P3 hardening:** a `SystemBarAppearance` helper (using `WindowInsetsControllerCompat`)
  sets `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` from the
  **current app theme** (`SoftColors.isLight`):
  - **Light theme** → dark (charcoal) icons (`isAppearanceLight*Bars = true`).
  - **Dark theme** → light icons (`false`).
- **Transparent bars:** bar background stays transparent over the warm background; the
  home/drawer content already insets via `windowInsetsPadding(systemBars.only(Top))`.
- Applied on `HomeActivity`, the drawer overlay, and `SettingsActivity`.
- **Icon color itself** is system-owned (we do not draw status icons); "charcoal" is
  achieved by requesting **light bars (dark icons)** on the cream theme.

### 4.7 Navigation bar handling (F2)

- **Gesture nav:** hide the nav bar by requesting `WindowInsetsCompat.Type.navigationBars`
  be hidden with **transient bars** on the home surface (immersive-ish), so the home
  reads edge-to-edge; swipe brings it back.
- **Button nav:** draw a **minimal** nav-bar background = `background` color
  (`navigationBarColor = transparent` + `isAppearanceLightNavigationBars` per theme), so
  the 3-button bar blends with the cream instead of showing the platform black/white.
- Detection: `WindowInsetsCompat` + `WindowManager` gesture-inset heuristics; degrade
  gracefully (never crash) when undetectable.

### 4.8 Long-press context menu (F3) — row-style card

**Targets (P3-3):** drawer tile + right-rail icon.

**Trigger:** `Modifier.combinedClickable(onLongClick = …)` on the tile / rail icon.
Vibration/haptic optional (`HapticFeedbackType.LongPress`).

**Menu contents:**

| Row | Drawer tile | Rail icon |
|---|---|---|
| Open (launch / fire) | ✔ | ✔ (if resolvable) |
| App Info | ✔ | ✔ |
| Edit Icon | ✔ (→ icon override flow; persistence via `iconOverrides`) | — |
| Remove (from drawer/home) | ✔ (hide app) | ✔ (remove shortcut) |
| Uninstall | ✔ **greyed if `isSystem`/unremovable** (P3-4) | — |
| Shortcuts | ✔ (long-press app shortcuts; list `ShortcutInfo`) | ✔ (rail icon shortcut target) |

- **Style:** dim scrim + cream **r24** card anchored near the pressed element (reuse the
  `FolderPopup` pattern), rows separated by 1px `divider`, leading 20dp `LineIcon`,
  labels `bodyMedium`. Destructive/unavailable rows use a muted/disabled color.
- **Anchoring:** positioned at the press point, clamped inside the screen bounds.
  (If anchoring proves fragile in Compose, fall back to a centered card — the FolderPopup
  precedent — and document the deviation; see §9 risks.)

### 4.9 Custom toggle atom (G2) — `SoftToggle`

```kotlin
@Composable fun SoftToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
)
```

- **Size** 42×22 (`Dimens.toggleWidth/Height`), pill (`ToggleShape`), knob 16
  (`Dimens.toggleKnob`).
- **On:** track `tile` (charcoal light / cream dark); knob `onTile` (cream light /
  charcoal dark).
- **Off:** track `progressTrack`; knob `onTile` muted.
- **Disabled:** 40% alpha; used for locked rows (Time/Date/Weather toggles).
- **Motion:** knob X animates with `MotionTokens.railSlide()` (220ms, WarmEase) —
  reuses the established duration; no new token.
- Accessible: `Role.Switch`, `stateDescription` "On"/"Off".

### 4.10 Icon-pack persistence (P3-5) — closes `04` #36

- `IconPackRepositoryImpl` persists the chosen pack id **via `PrefsRepository.setActiveIconPack`**
  (or its own DataStore key, whichever is already wired) and **rehydrates on cold
  start**: read the id → re-run the importer/scanner → restore `activePack` (or fall
  back to auto-mask when the pack is gone/corrupt).
- **Failure:** missing/corrupt pack → clear the stored id, fall back to auto-mask, no
  crash (mirrors D-013's "previous pack kept on `Failed`" for imports).

### 4.11 Error handling

| Case | Behavior |
|---|---|
| All toggleable rows hidden | Locked rows (Time/Date/Weather) still render — home never empty |
| Corrupt/unknown row-pref JSON in DataStore | `HomeRowLogic.sanitize` → default order, no crash |
| Uninstall not permitted (system app / policy) | Row greyed; tap is a no-op; App Info still works |
| `PackageManager` uninstall intent unresolvable | Hide/grey the Uninstall row; never throw |
| App Info intent unresolvable | Grey the row |
| Shortcuts empty for an app | "Shortcuts" row hidden (or shows "No shortcuts") |
| `WallpaperManager` picker unresolvable | Toast/"unavailable" message; never crash |
| Status/nav-bar insets undetectable | Skip hiding; degrade to minimal style; no crash |
| Active icon pack missing at cold start | Clear id, fall back to auto-mask (no crash) |
| Rail icon has no resolvable target | Long-press menu hides "Open"; App Info/Remove still available |

### 4.12 Tokens (extend, don't invent)

Reuse Session-5 / P2 tokens wherever possible. Expected additions (minimal):

| Token | Value / source | Why |
|---|---|---|
| `Dimens.toggleWidth` / `toggleHeight` / `toggleKnob` | 42 / 22 / 16 (**existing**) | toggle — already present |
| `Dimens.menuRadius` | 24 (reuse `Shape.large`) | context menu card |
| `Dimens.menuRowHeight` | 44–48 | menu row tap target |
| `Dimens.settingsRowRadius` | 18 (reuse `Shape.medium`) | settings rows |
| `Dimens.settingsRowMinHeight` | 52 | settings row |
| `Color.destructive` | reuse `accent`/`SoftTextMuted` or add one warm-red-free muted destructive | uninstall row (no red in the warm palette → use muted + strikeout label) |
| `Color.disabledContent` | alpha of `textMuted` | greyed rows |
| `Spacing` | reuse `md`/`lg`/`xl`/`xxl` | section + row paddings |

Rule unchanged: **no screen hardcodes a color / duration / size**; everything goes
through tokens. Any genuinely new value is added to `Dimens`/`Color` and recorded in `04`.

### 4.13 New icons

Reuse existing drawables where possible. `docs/02` names `info`, `trash-2`, `pencil`,
`grid-2x2`, `sun`, `arrow-up-right`. **Phase 0 must audit which exist**
(`_designsystem/res/drawable` currently has: `pen_line`, `settings`, `arrow_up_right`,
`square`, `sun`, … but **no `info`, no `trash_2`, no `grid_2x2`, no `chevron`/`x`** —
verify). Missing glyphs are added in the same ASCII-safe converter style (D-010):
likely `info.xml`, `trash_2.xml`, `chevron_right.xml`, `arrow_up.xml`, `arrow_down.xml`,
and `x.xml` (if absent).

---

## 5. Testing plan

### Unit (JVM)
| Test | Asserts |
|---|---|
| `HomeRowLogicTest` | default order/all-visible; locked rows cannot hide; toggle; moveUp/moveDown clamped; `visibleInOrder` respects visibility + order; `sanitize` drops unknown, appends missing, forces locked visible; all-hidden → locked remain |
| `AppActionsTest` (pure) | uninstall eligibility = `!isSystem`; intent builders produce expected actions; unresolvable → null/hidden (no throw) |
| `PrefsRepositoryTest` (extend, Robolectric) | row prefs round-trip; spacing round-trip; `activeIconPackId` round-trip; corrupt row JSON → default |
| `LauncherPrefsTest` (extend) | new fields default correctly |
| `IconPackPersistenceTest` (Robolectric) | set pack id → new repo instance rehydrates `activePack`; missing pack → null + id cleared |
| `MotionTokensTest` (extend) | toggle uses `railSlide()` (220ms) — guard only if a new helper is added |

### Compose UI
| Test | Asserts |
|---|---|
| `SoftToggleTest` | on/off render; disabled state; click toggles; locked toggle non-interactive |
| `AppContextMenuTest` | menu shows expected rows; Uninstall disabled for a system app; tap App Info fires; scrim/back dismisses |
| `SettingsPanelTest` | 4 sections render; theme change emits; row toggle emits; move up/down reorders |
| `HomeScreenTest` (extend) | hidden row not rendered; locked row always rendered; order follows prefs |
| `SystemBarAppearanceTest` (Robolectric, best-effort) | light theme → `isAppearanceLightStatusBars = true`; dark → false |

### Manual (Phase 7 — real screenshots, not prose)
- **Status bar:** screenshot home in Light vs Dark theme; show bar icons dark-on-cream
  and light-on-charcoal.
- **Nav bar:** emulator with gesture nav → nav bar hidden/edge-to-edge; switch to button
  nav → bar blends with cream (screenshot both).
- **Long-press menu:** long-press a drawer tile → menu screenshot; long-press a **system**
  app → Uninstall greyed (screenshot); long-press a rail icon → menu screenshot.
- **Settings panel:** screenshot each section (Appearance / Widgets / Wallpaper /
  Gestures).
- **Row visibility/order:** toggle off a row + move a row up → screenshot home before/after;
  prove locked rows can't be hidden.
- **Icon-pack persistence:** apply a pack → `am force-stop` → relaunch → screenshot the
  drawer still using the pack.
- **Wallpaper:** open the system picker (screenshot); set Flat warm (screenshot).
- TalkBack pass on menu, toggles, settings rows; contrast check; reduced-motion check.

---

## 6. Documentation updates (follow prior pattern)

| Doc | Update |
|---|---|
| `00-DESIGN-SOURCE.md` | Note P3 has **no** design nodes; P3 visuals derive from Warm Right Rail patterns |
| `02-DESIGN-SYSTEM.md` | Add `SoftToggle`, `AppContextMenu`, `SettingsRow` atoms; note reused + new tokens |
| `03-FEATURE-MAP.md` | Update F1/F2/F3 + G1–G4 statuses; **fix stale node IDs** (`ogMkZ`, `Fzobx`/`pu2gg`, `nFk4u`, `UPa9N` → *(assumption)*) |
| `04-ASSUMPTIONS.md` | **New section J**: P3 decisions, status/nav-bar handling, long-press row menu, settings sections, toggle, icon-pack persistence (closes #36), wallpaper flat+picker; new deferrals (drag-reorder → P4, backup/restore → P4) |
| `05-PROGRESS.md` | New Session 7 log (P3) with evidence |
| `09-DECISIONS-LOG.md` | D-026…D-0xx: row visibility/order model, row-style long-press menu, custom toggle, icon-pack persistence, wallpaper approach, status/nav-bar appearance |
| `06-TESTING.md` | Tick the "long-press menu appears (P3)" manual row |

---

## 7. Phase plan (end-to-end)

Each phase is one focused unit with its own exit criteria. Do not start a phase until
the previous phase's exit criteria pass.

### Phase 0 — Model + tokens + assets
**Goal:** data shapes and tokens exist; nothing rendered.
- Add `HomeRowKind`, `HomeRowPref`, `HomeRowLogic` in `core:model`; extend `LauncherPrefs`.
- Add `Dimens`/`Color` tokens (§4.12); audit + add missing icons (§4.13).
- **Tests:** `HomeRowLogicTest`, `LauncherPrefsTest` (extend).
- **Exit:** module compiles; new pure-logic tests green; no UI changed.

### Phase 1 — Repositories (`core:data`)
**Goal:** persistence + app actions exist and are testable.
- Extend `PrefsRepository` (row prefs, spacing, active pack); extend impl + keys.
- New `AppActionsRepository` (`uninstallInfo`, `appInfoIntent`, `shortcutList`,
  `removeFromDrawer`; pure parts testable).
- **Tests:** `PrefsRepositoryTest` (extend), `AppActionsTest`.
- **Exit:** repositories green; no UI changed.

### Phase 2 — Design-system atoms
**Goal:** reusable toggle + menu + settings row.
- New `SoftToggle.kt`, `AppContextMenu.kt`, `SettingsRow.kt`.
- **Tests:** `SoftToggleTest`, `AppContextMenuTest` (Compose).
- **Exit:** atoms compile; previews render each.

### Phase 3 — Icon-pack persistence (P3-5)
**Goal:** chosen pack survives cold start.
- Persist + rehydrate in `IconPackRepositoryImpl`; fall back to auto-mask on failure.
- **Tests:** `IconPackPersistenceTest`.
- **Exit:** pack rehydrates; no UI regression.

### Phase 4 — System UI integration (F)
**Goal:** status/nav bar follow theme; long-press menus live.
- `SystemBarAppearance` controller wired into `HomeActivity` + drawer + settings (F1/F2).
- Rail long-press → `AppContextMenu` (F3); wire App Info / Remove / Shortcuts.
- **Tests:** `SystemBarAppearanceTest` (best-effort), update `HomeScreenTest`.
- **Exit:** bars adapt per theme; rail menu works; no crashes.

### Phase 5 — Drawer long-press menu (F3)
**Goal:** drawer tiles open the row-style menu.
- Wrap drawer tile in `combinedClickable`; build menu items (Open/App Info/Edit Icon/
  Remove/Uninstall[greyed for system]/Shortcuts); wire actions.
- **Tests:** extend drawer Compose tests; `AppContextMenuTest` system-app case.
- **Exit:** menu opens on tiles; uninstall greyed for system apps; actions fire.

### Phase 6 — Settings panel (G)
**Goal:** real settings in place of the stub.
- Rewrite `SettingsStubActivity` body into the 4 sections; wire `PrefsRepository`.
- Home reads row prefs → renders visible rows in order (Widgets section live).
- **Tests:** `SettingsPanelTest`; `HomeScreenTest` (extend for visibility/order).
- **Exit:** all four sections functional; row visibility/order persist; theme applies.

### Phase 7 — Verification on device (real screenshots)
**Goal:** prove it works like P1.5/Session 5/P2.
- Build debug APK; install on emulator (Android 15 / API 35, `soft_home_pixel`); set as home.
- Capture every state in §5 Manual (status bar light/dark, nav bar both modes, long-press
  menus incl. greyed uninstall, each settings section, row toggle+reorder, **icon-pack
  survives `am force-stop`**, wallpaper picker + flat).
- TalkBack / contrast / reduced-motion checks; save screenshots to `docs/screenshots/`.
- **Exit:** evidence captured; no crashes; no regressions; all unit tests green.

### Phase 8 — Cleanup + docs
**Goal:** docs consistent; no dead code.
- Update `00`, `02`, `03`, `04`, `05`, `06`, `09` per §6.
- Confirm the stub body is fully replaced and unreferenced.
- **Exit:** build clean; all tests green; docs consistent.

### Phase 9 — Final review / handoff
**Goal:** checkpoint.
- Summarize what shipped, evidence, open items; list deferrals.
- **STOP for user review before P4.**

---

## 8. Out of scope / deferred (explicit)

- **Drag-and-drop row reorder** — deferred to P4 (P3-2 ships up/down buttons).
- **Drag-and-drop app→folder** (P2-deferred) — deferred to P4 (gesture subsystem).
- **Live-wallpaper engine** — deferred; flat default + system picker only (P3-6).
- **Backup & restore settings (G4)** — deferred to P4.
- **Real gesture actions** (swipe-down notifications, double-tap lock, long-press home)
  — settings surface only; behavior deferred (mirrors `04` #15).
- **MediaSession / real playback (#39)** — still deferred.
- **KkPN3 dark editorial** — reference only.
- **Live calendar events** — static/empty.
- **Notification listener / badge counts** — still deferred.
- **System widgets (AppWidgetHost, D3)** — remains a stub.

---

## 9. Open risks

| Risk | Mitigation |
|---|---|
| Anchored long-press popup positioning is fragile in Compose | Fall back to a centered card (FolderPopup precedent); clamp to screen bounds; document the deviation |
| Uninstall availability varies (device policy, system app) | Grey the row; never attempt a throwing uninstall; App Info always present (P3-4) |
| Hiding the nav bar on gesture nav can break some OEM ROMs | Best-effort; degrade to minimal-style bar; never crash |
| "Spacing"/"grid size" now touch only the drawer — user may expect home grid | Explicit scope note (§4.3); settings labels say "drawer" |
| Row-pref schema drift (new rows later) | `HomeRowLogic.sanitize` appends unknown/missing kinds safely; covered by tests |
| Icon-pack rehydrate cost on cold start | Off-main-thread parse; cache; fall back to auto-mask while loading |
| Charcoal status icons depend on OS light-bar support | `WindowInsetsControllerCompat` handles API 26+; degrade to default on failure |
| Large surface for one phase | Phase gating; each phase independently verifiable; drag-and-drop + backup deferred |

---

## 10. Open questions (need user confirmation)

1. **Settings entry point.** The rail already has a `panel-left` → *Settings* shortcut
   (D-020) that currently fires the **system** Settings intent. Should P3 **repoint** the
   rail's `panel-left` icon to open **our** settings panel instead? My recommendation:
   **yes** — repoint it to our panel (the brief's G1 implies an in-app settings surface),
   and keep a "System settings" row inside our panel for the OS screen.
   - Alt: keep the rail firing the OS settings and add a separate in-app entry (drawer
     header or a "Settings" row).

2. **"Remove" semantics (drawer).** Does "Remove" mean **hide the app from the drawer**
   (reversible via a settings "hidden apps" list) or **only remove from a folder**? My
   recommendation: **hide from drawer** (persist a hidden-apps set in `LauncherPrefs`),
   since folders already handle removal-from-folder. Confirm — this adds a
   `hiddenApps: Set<String>` field + a tiny hidden-apps management row.

3. **Spacing values.** Proposed stepper: **Compact / Normal / Roomy** mapping to a
   multiplier (×0.88 / ×1.0 / ×1.12) on drawer grid gaps + home row vertical padding.
   Confirm the three labels and that a simple multiplier is acceptable (vs exact dp
   presets).

4. **Edit Icon flow depth.** "Edit Icon" for a tile: P3 minimal = **pick from the active
   pack's drawables** for that app (writes `iconOverrides`). A richer editor (crop/tint/
   import single icon) is **P4**. Confirm the minimal scope.

5. **Move-buttons placement.** Up/down move buttons sit **inline on each Widgets row**
   (right side, next to the toggle) — compact, but busy. Alternative: a **separate
   "Reorder" sub-panel** listing rows with drag handles (but drag is deferred, so it'd be
   up/down buttons there too). Recommendation: **inline** for P3. Confirm.

---

*End of P3 spec. Awaiting user review before implementation.*
