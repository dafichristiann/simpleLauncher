# P3.5 — Drawer Icon Redesign (Color + Labels) — Design Spec

> **Date:** 2026-09-24
> **Type:** Architectural (re-renders the app-drawer icon grid; touches the design
> system, the icon pipeline's presentation layer, and the drawer screen).
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — frame
> `TpzL1` "Warm App Drawer — Unique Icon Grid" (already present; Pen v2.18).
> **Status:** Spec written — awaiting user review before implementation.
> **Predecessor:** Session 8 patch (`83fadd6`, drawer BACK fix). **Successor:** P4.

This document is the single source of truth for **P3.5**. It turns the `TpzL1`
frame — which the P1/P1.5/P2/P3 work rendered **monochrome** (charcoal squircle +
cream glyph) and, for labels, only partially — into the **colorful, labeled** grid
the frame actually specifies. It records the four user-confirmed decisions, the
`TpzL1` audit, the exact color tokens, the category→color mapping, the files
touched, and the test plan that keeps the existing icon pipeline green.

---

## 0. User-confirmed decisions (locked)

These four answers were given explicitly and are **not** open for re-litigation:

| # | Topic | Decision |
|---|---|---|
| **P3.5-1** | **Icon color source** | **Hybrid (Option C).** An app that has an active-pack drawable renders the **real pack drawable in its own colors** (no monochrome tint). An app **not** in the pack renders a **category-colored lucide glyph** (color chosen from `AppInfo.category`). |
| **P3.5-2** | **Auto-mask in the drawer** | **Disabled for the drawer grid.** The charcoal monochrome auto-mask is **not** used in the drawer. It **remains** the fallback for other surfaces that still want monochrome (kept, not deleted). Drawer = always the color system. |
| **P3.5-3** | **Tile fill** | **Not flat cream.** Tile background varies to differentiate state: default tile = `tile-cream #F6F0E7`; **active/selected tile** (e.g. an open folder, or a temporarily-highlighted newly-installed app) = **`selected-tile #D19B62`** with `selected-icon #F5EFE6` glyph. |
| **P3.5-4** | **Phase placement** | A **separate phase, "P3.5", completed and verified *before* P4.** Rationale: P4's *Edit Icon* rich editor builds directly on the final drawer rendering model, so that model must be frozen first. |

**Carried-in facts (locked):**
- The frame `TpzL1` is **already** named "Warm App Drawer — Unique Icon Grid"; P3.5
  is **not** a new design, it renders the frame faithfully.
- The `DrawerTint*` tokens added in the Warm Right Rail spec (`Color.kt`) were an
  *optional fallback*; P3.5 replaces that placeholder with the real per-category
  palette from `TpzL1` (see §3).
- Icon-pack **logic** (`IconResolver`, `AutoMask`, `IconMasker`, `IconPackImporter`,
  `IconPackRepositoryImpl`, `AppFilterParser`) is **untouched** — P3.5 is a rendering
  change only (same boundary the Warm Right Rail spec drew in §4.1/§3).

---

## 1. `.pen` audit — frame `TpzL1` (source of truth)

Frame `TpzL1`, 430×860, r36, fill `#DCCDBA`. Grid tiles are **68×68 r21**, glyph
24×24 centered, **11pt label** centered below each tile. Representative tiles:

| Tile node | App | Tile fill | Glyph | Glyph color var | Hex |
|---|---|---|---|---|---|
| `dBQmH` | WhatsApp | `$tile-cream` | `message-circle` | `$icon-communication` | `#5F7A72` |
| `axOkn` | Instagram | `$tile-cream` | `image` | `$icon-social` | `#6E8B86` |
| `HGHVv` | Gmail | `$tile-cream` | `mail` | `$icon-communication` | `#5F7A72` |
| `vLsee` | Chrome | `$tile-cream` | `globe` | `$icon-finance` | `#4D7C8A` |
| `gKl9I` | Messages | `$tile-cream` | `message-square` | `$icon-communication` | `#5F7A72` |
| `E3xTsy` | Phone | `$tile-cream` | `phone` | `$icon-communication` | `#5F7A72` |
| `bE2bO` | Contacts | `$tile-cream` | `contact-round` | `$icon-communication` | `#5F7A72` |
| `BbIsy` | Calendar | `$tile-cream` | `calendar-days` | `$icon-communication` | `#5F7A72` |
| `xjkkS` | Calculator | `$tile-cream` | `calculator` | `$icon-media` | `#D19B62` |

### 1.1 `TpzL1` color variables (verbatim from the file)

| Variable | Hex | Intended domain |
|---|---|---|
| `$tile-cream` | `#F6F0E7` | default tile background |
| `$selected-tile` | `#D19B62` | active/selected tile background |
| `$selected-icon` | `#F5EFE6` | glyph on a selected tile |
| `$icon-communication` | `#5F7A72` | messaging / phone / contacts / calendar |
| `$icon-social` | `#6E8B86` | social / photos |
| `$icon-productivity` | `#8A5F43` | productivity / docs |
| `$icon-media` | `#D19B62` | media / entertainment (calc example) |
| `$icon-travel` | `#B06F52` | maps / travel |
| `$icon-finance` | `#4D7C8A` | browser / finance / tools |
| `$icon-neutral` | `#625B52` | uncategorized / fallback |

All seven icon colors are already present (or near-identical) in `Color.kt` as the
`DrawerTint*` tokens and the Warm Right Rail palette — see §3 for the exact mapping.

---

## 2. What changes vs the current drawer

| Aspect | Current (monochrome) | P3.5 (color + labels) |
|---|---|---|
| Tile background | charcoal `#2B2B2B` (squircle) | **cream `#F6F0E7`** (default) / **amber `#D19B62`** (selected) |
| Glyph color | cream `#E8DFD0` (monochrome tint) | **per-category color** (§3), or **real pack colors** |
| Pack apps | drawable tinted cream | **real drawable, untinted** |
| Non-pack apps | auto-mask (charcoal + cream) | **category-colored lucide glyph** (no auto-mask) |
| Label | present (`labelMedium`, `#3A3A3A`) | **11pt `#3A3A3A` centered** (spec-exact; verify/tighten) |
| Corpus radius | `iconShape(size)` (30% of side) | **r21 @ 68px** (≈0.31; keep `iconShape`, matches) |

Preserved unchanged: grid columns (4), spacing, category nav, search pill, alphabet
rail, folder cells (their mini-grid **inherits** the new coloring), the whole icon
pipeline's resolution logic, and the Session 8 Back behavior.

---

## 3. Color tokens & category→color mapping

### 3.1 New color tokens (add to `Color.kt`, extend `SoftColors`)

Add named tokens so no screen hardcodes a hex (house rule):

```kotlin
// --- P3.5: drawer icon palette (from homeApp.pen frame TpzL1) ---
val DrawerTileCream   = Color(0xFFF6F0E7) // $tile-cream   (default tile bg)
val DrawerTileSelected = Color(0xFFD19B62) // $selected-tile (active/selected tile bg)
val DrawerIconOnSelected = Color(0xFFF5EFE6) // $selected-icon (glyph on selected tile)

val DrawerIconCommunication = Color(0xFF5F7A72) // $icon-communication
val DrawerIconSocial        = Color(0xFF6E8B86) // $icon-social
val DrawerIconProductivity  = Color(0xFF8A5F43) // $icon-productivity  (== SoftAccent)
val DrawerIconMedia         = Color(0xFFD19B62) // $icon-media         (== Selected tile)
val DrawerIconTravel        = Color(0xFFB06F52) // $icon-travel        (== SoftIndexActive)
val DrawerIconFinance       = Color(0xFF4D7C8A) // $icon-finance
val DrawerIconNeutral       = Color(0xFF625B52) // $icon-neutral       (== SoftTextBody)
```

Extend `SoftColors` with: `drawerTileCream`, `drawerTileSelected`,
`drawerIconOnSelected`, `drawerIconCommunication`, `drawerIconSocial`,
`drawerIconProductivity`, `drawerIconMedia`, `drawerIconTravel`,
`drawerIconFinance`, `drawerIconNeutral`. Provide light + dark values (dark =
warm-derived, analogous to the existing dark adaptations).

> **Note:** these supersede the placeholder `DrawerTintClay/Amber/Pine/Sage` tokens
> from the Warm Right Rail spec. Keep those only if still referenced after P3.5;
> otherwise remove them in the P3.5 cleanup step (mirrors the Warm Right Rail §7
> Phase-7 pattern).

### 3.2 Category → color mapping (the core rule)

Our model exposes `AppInfo.category` as an Android `ApplicationInfo.category` Int
(mirrored in `core:model/AppCategory`). `TpzL1` keys colors by **app domain**, which
does not map 1:1 to Android categories, so P3.5 defines this **pure, testable**
mapping:

| `AppInfo.category` | `AppCategory` const | Pen color token | Hex |
|---|---|---|---|
| SOCIAL (`4`) | `SOCIAL` | `icon-social` | `#6E8B86` |
| PRODUCTIVITY (`7`) | `PRODUCTIVITY` | `icon-productivity` | `#8A5F43` |
| GAME (`0`) | `GAME` | `icon-media` | `#D19B62` |
| AUDIO (`1`) | `AUDIO` | `icon-media` | `#D19B62` |
| VIDEO (`2`) | `VIDEO` | `icon-media` | `#D19B62` |
| IMAGE (`3`) | `IMAGE` | `icon-social` | `#6E8B86` |
| NEWS (`5`) | `NEWS` | `icon-finance` | `#4D7C8A` |
| MAPS (`6`) | `MAPS` | `icon-travel` | `#B06F52` |
| `null` / `UNDEFINED` / unknown | — | `icon-neutral` | `#625B52` |

Rationale for the finer-than-tab mapping: the drawer **tab** grouping collapses
categories (e.g. IMAGE→Tools, SOCIAL→Communication), but the **icon color** should
stay visually expressive, matching how `TpzL1` colors a photo app (`icon-social`)
differently from a docs app (`icon-productivity`) even though both live under
"Tools"-ish surfaces. This keeps the color rule a pure function of the raw category,
independent of the tab filter.

**Communication-home color note:** `TpzL1` colors messaging/phone/contacts/calendar
with `icon-communication #5F7A72`, but Android's `SOCIAL`/`MESSAGE` categories don't
distinguish "communication" from "social" cleanly. P3.5 resolves this as follows:
- `SOCIAL` → `icon-social` (friends/social feeds),
- Communication-styled apps (phone/dialer/messaging/contacts) are **heuristic**
  colored via the existing `IconMasker.symbolFor(app)` domain hint (which already
  detects `message`/`phone`/`contact`/`calendar`): when the resolved glyph is one of
  {`MessageCircle`, `Phone`, `PhoneCall`, `Mail`, `CalendarDays`} **and** the category
  is `null`/`UNDEFINED`, use `icon-communication` instead of `icon-neutral`.

Overall precedence for the glyph color (non-pack apps), highest first:
1. **Selected tile** → `drawerIconOnSelected` (`#F5EFE6`).
2. **Pack app** → real drawable colors (no tint at all).
3. **Category-mapped color** (table above).
4. **Communication heuristic** (glyph ∈ communication set, category unknown) →
   `icon-communication`.
5. **Neutral fallback** → `icon-neutral`.

This rule lives in a **pure Kotlin** mapper (no Android), so it is unit-testable:

```kotlin
// feature/iconpack/domain/DrawerIconColor.kt  (pure)
object DrawerIconColor {
    /** Android ApplicationInfo.category Int -> pen color token key. */
    fun tokenFor(category: Int?, symbolName: String): DrawerIconToken

    enum class DrawerIconToken { Communication, Social, Productivity, Media, Travel, Finance, Neutral }
}
```

The Compose side maps `DrawerIconToken` → a `SoftColors` field (light/dark aware).
Keeping the enum in the domain layer means tests never depend on `android.graphics`.

---

## 4. Rendering changes (files touched)

The change is concentrated in the **presentation** of icons; resolution logic is
untouched. New file first, then the edits:

### 4.1 New: `feature/iconpack/.../ui/DrawerAppIcon.kt`

A drawer-specific icon composable that renders the P3.5 system:

```
DrawerAppIcon(
    resolved: ResolvedIcon,
    size: Dp,
    activePack: IconPack?,
    drawableLoader: IconPackDrawableLoader,
    bitmapProvider: IconBitmapProvider,
    category: Int?,
    selected: Boolean = false,
)
```

Behavior:
- Reuse `resolved` (already computed by `AppDrawerViewModel.resolveIcon`). It carries
  `source`, `drawableName` (for pack apps) and `symbolName` (category glyph).
- **Pack app** (`source is FromPack`/`Override`, drawable decodes): render the
  decoded drawable **untinted**, inset by the existing `ICON_SYMBOL_RATIO` (0.49),
  over `drawerTileCream` (or `drawerTileSelected` when `selected`).
- **Non-pack app**: render `LineIcon.fromName(symbolName)` tinted with the
  `DrawerIconColor` mapping, same inset.
- **Selected tile**: background `drawerTileSelected`; glyph `drawerIconOnSelected`.
- **No auto-mask**: never call `bitmapProvider.maskedIcon` here. `bitmapProvider` is
  kept only for the *system-icon* last resort (a genuinely undecodable pack entry
  still needs *something* — fall back to the category glyph, not charcoal-mono).

### 4.2 Edit: `core/designsystem/.../atom/AppIconTile.kt`

`AppIconTile` is currently **monochrome by construction** (`painter` is tinted to
`onTile` cream; background is `tile` charcoal). Rather than overload it, P3.5 adds a
**new atom** (or a clearly-separated parameter set) so the monochrome tile stays
intact for any surface that still wants it:

- Add `AppIconTile` variants/params: `background: Color`, `painterTint: Color?`
  (null = render the painter's own colors), `symbolTint: Color`. Default values keep
  the current charcoal/cream behavior, so existing callers don't change.
- **Keep `AppIconTile`'s existing contract** for the home/rail *monochrome* uses.
  The drawer uses the new `DrawerAppIcon` (4.1), which composes the widened atom.

> Prefer a new `DrawerIconTile` atom over mutating `AppIconTile` semantics, to keep
> isolation (§6). Decide the exact shape during implementation; the spec's
> requirement is: **the drawer's tile is parameterized by background + tint, and the
> old charcoal/cream path is still available unchanged.**

### 4.3 Edit: `feature/appdrawer/.../AppDrawerScreen.kt`

- `AppCell` (and the folder mini-grid `FolderMiniGrid`, and the folder-popup grid)
  switch from `AppIcon(...)` to `DrawerAppIcon(...)`, passing `item.app.category`
  and a `selected` flag.
- `selected` sources (P3.5-3):
  - a **folder cell's** member preview — not selected by default;
  - the **open folder's** member tiles in the popup — selected (folder is active);
  - **newly-installed app highlight** (optional, P4-adjacent): the plumbing accepts a
    `highlightedKey: String?`; wiring a real "just installed" signal is **deferred**
    (no install-broadcast receiver in P3.5) — the *capability* ships, the *trigger*
    is future.
- Label: keep `item.app.label`, 11pt, `statusText #3A3A3A`, centered — verify it
  matches `TpzL1`'s `J3Lb4` (11 normal, fixed-width 76, centered); adjust the
  text style token if the current `labelMedium` size differs.
- **Do not** pass an `AppCell`-level auto-mask anymore.

### 4.4 Edit: `AppDrawerViewModel.kt` (minimal)

- `DrawerEntry` gains nothing new (it already carries `app` and `resolved`); the
  `category` is read from `entry.app.category`, so **no VM change is strictly
  required**. If a `selected`/`highlightedKey` state is added for folders/newly-
  installed, add a small `StateFlow<String?>` and expose it in `DrawerUiState`.
- **Do not** change `resolveIcon` (pipeline stays: override → pack → auto-mask →
  system). For the drawer, the `AutoMask`/`System` branches are simply rendered as a
  colored category glyph instead of a charcoal mask — that's a *view* decision, not a
  resolver change. `IconResolver` and its tests are untouched.

### 4.5 Not touched

`IconResolver.kt`, `AutoMask.kt`, `IconMasker.kt`, `IconBitmapProvider.kt` (kept — used
elsewhere / system last resort), `IconCompositor.kt`, `IconPackImporter.kt`,
`IconPackRepositoryImpl.kt`, `AppFilterParser.kt`, `InstalledIconPackScanner.kt`,
`IconPackDrawableLoader.kt`, `MaskPalette.kt`, `DrawerCategory.kt`/`DrawerCategoryMapper`.
Their behavior and tests are unchanged.

---

## 5. Testing plan

### 5.1 New unit tests (pure)

| Test | Asserts |
|---|---|
| `DrawerIconColorTest` | table §3.2 exact: SOCIAL→Social, PRODUCTIVITY→Productivity, GAME/AUDIO/VIDEO→Media, IMAGE→Social, NEWS→Finance, MAPS→Travel, null/unknown→Neutral; and the communication heuristic (Phone/Mail/CalendarDays + null category → Communication). |
| `DrawerIconTokenTest` (if split) | every `DrawerIconToken` has a light + dark `SoftColors` color (no gaps). |

### 5.2 Existing icon-pipeline tests — must stay green (regression guard)

P3.5 must **not** perturb the pipeline. These must remain passing **unchanged**:
`AutoMaskTest`, `IconResolverTest`, `IconMaskerTest`,
`IconCompositorGlyphUniquenessTest`, `IconPackImporterTest`,
`IconPackPersistenceTest`, `AppFilterParserTest`. If any needs a change, that is a
signal the boundary leaked — stop and re-scope.

### 5.3 Compose UI (instrumented)

| Test | Asserts |
|---|---|
| `DrawerAppIconTest` (new) | a pack `ResolvedIcon` renders untinted (colors preserved) over cream; a non-pack icon renders a glyph tinted by category; a `selected` icon renders the amber background + `#F5EFE6` glyph. |
| `AppDrawerScreenColorTest` (new or extend `AppDrawerScreenTest`) | with a fake app of known category, the drawer cell shows a category-colored glyph (assert via a test tag/content-desc carrying the token, since tint isn't directly assertable). |

### 5.4 Manual / on-device (Phase verify)

- Build debug APK, install on the emulator, screenshot the drawer.
- Compare against `TpzL1`: tiles cream (or amber when selected), glyphs colored,
  labels 11pt centered.
- Screenshot save to `docs/screenshots/p35-drawer-*.png`.
- Contrast spot-check: each glyph color on `#F6F0E7` and on `#D19B62`.
- Confirm the Session 8 Back behavior still works (drawer closes on BACK).

### 5.5 Token coverage test

A test asserting `LightSoftColors`/`DarkSoftColors` populate every new
`drawer*` field (mirrors any existing token-coverage test). Guarantees no screen hits
a default/black color.

---

## 6. Isolation & boundaries

- **Pipeline vs presentation:** resolution (which drawable / which glyph) already
  exists in `AppDrawerViewModel.resolveIcon` + `IconResolver`. P3.5 changes **only how
  the result is painted**. This is the key boundary that keeps the change small.
- **One owner of the color rule:** `DrawerIconColor` (pure) is the single source of
  the category→color mapping; both UI and tests consume it. No hex in the screen.
- **Monochrome stays available:** `AppIconTile`'s charcoal/cream path and
  `AppIcon` are preserved for any non-drawer surface; the drawer gets its own
  `DrawerAppIcon`. Deleting genuinely-dead tokens/atoms is a final cleanup step.

---

## 7. Phase plan (P3.5)

Each step is a focused, PR-sized unit with its own exit criteria; do not start the
next until the previous passes.

### Step 0 — Tokens
- Add the §3.1 color tokens to `Color.kt`; extend `SoftColors` (light + dark).
- Remove/replace the placeholder `DrawerTint*` if unreferenced after the port.
- **Tests:** token-coverage (§5.5). **Exit:** compiles; coverage test green.

### Step 1 — Color rule (pure domain)
- Add `DrawerIconColor` (§3.2) in `feature/iconpack/.../domain`.
- **Tests:** `DrawerIconColorTest`. **Exit:** mapper green; no UI changed.

### Step 2 — Tile atom
- Add the parameterized drawer tile (new `DrawerIconTile` or widened `AppIconTile`)
  supporting `background`, `painterTint = null`, `symbolTint`; keep the old path.
- **Tests:** atom renders in a preview/test. **Exit:** compiles; atoms green.

### Step 3 — Drawer render
- Add `DrawerAppIcon`; switch `AppCell`, `FolderMiniGrid`, folder popup to it; wire
  `selected`; verify the label style.
- **Tests:** `DrawerAppIconTest`, drawer color test. **Exit:** drawer renders the
  color+label grid.

### Step 4 — Cleanup + docs
- Remove dead `DrawerTint*`/unused charcoal paths **if** confirmed unused.
- Update docs `02` (tokens), `03` (drawer C1–C4), `05` (session log), `09`
  (decisions: **D-020** drawer color system, **D-021** pack-vs-glyph hybrid). The
  log's current highest is D-019.
- **Exit:** docs consistent; build clean; all tests green.

### Step 5 — Verify + STOP
- On-device screenshots vs `TpzL1`; Back regression re-check.
- **STOP for user confirmation** before any P4 work.

---

## 8. Out of scope / deferred (P3.5)

- **P4 Edit Icon editor** — builds on this frozen model; not built here.
- **Drag-and-drop** (app→folder, row reorder) — P4.
- **"Just installed" highlight trigger** — the `selected` capability ships; the
  install-broadcast wiring is deferred.
- **Removing auto-mask entirely** — out of scope; auto-mask is kept for other
  surfaces.
- **Dark-mode drawer polish beyond token parity** — derived warm dark values only.

---

## 9. Open risks

| Risk | Mitigation |
|---|---|
| Category data is sparse (`ApplicationInfo.category` often `UNDEFINED`) | Neutral fallback + glyph heuristic (§3.2); test the unknown path explicitly. |
| A pack's drawable has its own background → clashes with a cream tile | Pack drawables are rendered untinted at the 0.49 inset; if a pack draws a full-bleed square, it reads as "real app icon" (acceptable, matches `TpzL1` intent). Revisit only if screenshots show clash. |
| Dark palette has no `.pen` reference | Derive warm-dark values per the existing convention (`Color.kt` dark block); token-coverage test enforces presence. |
| Label size drift (`labelMedium` vs 11pt) | Assert/verify against `J3Lb4` in Step 3; adjust the type token if needed. |
| Scope creep into the resolver | Hard boundary: no edits to `IconResolver`/`AutoMask`/`IconMasker`; if a test there needs changing, stop and re-scope. |
