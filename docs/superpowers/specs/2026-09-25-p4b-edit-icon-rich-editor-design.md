# P4b — Edit Icon (rich editor) — Design Spec

> **Date:** 2026-09-25
> **Type:** Feature UI + persistence. Builds on the P3.5 hybrid drawer renderer.
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Parent spec:** [P4 scope & phase split](2026-09-25-p4-scope-and-phase-split-design.md)
> **Predecessor:** [P4a drag & drop](2026-09-25-p4a-drag-and-drop-design.md) (done) ·
> [P3.5 drawer icon redesign](2026-09-24-p35-drawer-icon-redesign-design.md) (the frozen render model)
> **Status:** Spec written — **awaiting user review before implementation.**

This document is the single source of truth for **P4b**. It contains: (0) the `.pen`
re-audit, (1) the **actual current state of the icon system** (what P3 "pick-from-pack"
really shipped — there is a gap), (2) the **scope decision** the user asked for
(pack app vs non-pack app; what "rich" means), (3) the locked decisions, (4) the
technical design (model + persistence + UI), (5) files to touch, (6) the test plan,
(7) the phase plan, and (8) the questions that need the user's answer before code.

**No code is written under this spec. It stops for user review, same as P1–P4a.**

---

## 0. `.pen` re-audit (2026-09-25, pre-spec)

Re-audited immediately before writing this spec, because the user edits `homeApp.pen`
by hand.

**File:** `design/homeApp.pen`, **176,128 bytes**, last written **2026-09-24 11:58:44**,
SHA256 `08DB2A81909EF7F71B351EB23D7F1AB993DC50053D82BD7AA8609A5D30CA6201`.

**Result: the file is byte-identical to the P3.5 and P4a audits.** The user has **not**
edited it since P3.5. Top-level frames are unchanged (6): `znb90` (light home),
`L7ZAp` (spec board), `TpzL1` (drawer), `KkPN3` (dark home), `QBhow` + `ciHU3`
(glyph libraries).

**Keyword sweep for an icon-editor design — all ABSENT:**

| Keyword | Hits | Meaning |
|---|---|---|
| `picker` / `swatch` / `choose` / `recolor` / `crop` / `resize` / `icon editor` / `custom icon` | 0 | **Nothing.** No editor mock exists. |
| `edit` | 3 | Only the frame name "Home Screen Mockup — Dark **Edi**torial" + the `pen-line`/`edit` glyph names. |
| `upload` | 2 | Only glyph/drawable names; no upload flow. |
| `palette` | 2 | Only a color-variable naming; no palette UI. |

**Consequence:** like P3/P4a, P4b has **no design source**. The editor is authored from
the existing Warm Right Rail vocabulary (the row-card `AppContextMenu` / `FolderPopupBody`
pattern, the `SoftToggle`/`SettingsRow` controls, the `TpzL1` drawer palette) as
documented assumptions (§7). **The `.pen` stays read-only** (byte-identical after build).

**Relevant existing nodes reused as *vocabulary*, not as a spec:**
- The drawer palette → `TpzL1`'s 7 `$icon-*` color variables (already ported in P3.5).
- The card vocabulary → the P2 `FolderPopupBody` (cream r24) and the P3 `AppContextMenu`.
- The glyph set → `QBhow` / `ciHU3` libraries, already bundled as `LineIcon` vectors.

---

## 1. Current state of the icon system — and the gap P4b must close

The P3 log says *"Q4: Edit Icon depth = pick from the active pack's drawables"* and the
feature map marks P4b as *"pack drawable or category glyph; recolor; reset"*. **But the
"pick-from-pack" capability does not actually exist yet.** This is the key finding that
sizes P4b, so it is stated up front. Audited 2026-09-25:

### 1.1 What exists

| Piece | Reality |
|---|---|
| `LauncherPrefs.iconOverrides` | `Map<String, String>` = **componentKey → *packId*** — *not* a drawable name. |
| `IconResolver.resolve` | If `overrides[key] == activePack.id`, it returns `IconSource.Override(packId, drawable)` where `drawable` is **re-derived from the pack's own `appfilter` entry** — i.e. the *same* drawable `FromPack` would have used. |
| The "Edit Icon" menu row | In `AppDrawerScreen.AppMenu`, `onEditIcon = viewModel::closeMenu` — a **no-op stub**. Tapping it just closes the menu. |
| `PrefsRepository` | Has **no** setter for an icon override at all. `setActiveIconPack` exists; per-app override writes do not. |

**Net:** today `iconOverrides` cannot change *which* drawable an app shows. Even if it
were written, the resolver would pick the pack's default entry for that app — identical
to no override. The only meaningful per-app override that the *current* model can express
is "this app is (or is not) exempt from the active pack". **There is no UI and no data
path for actually choosing an icon.** P4b is therefore **not** "wire a picker to existing
state" — it is "build the per-app override data path *and* the editor UI".

### 1.2 What the pack can offer an editor

| Capability | Exists? | Note |
|---|---|---|
| The active pack's `entries: Map<componentKey, drawableName>` | ✅ | Lets the editor show **this app's own pack drawable** (its default). |
| A pack's **full list of distinct drawables** (`drawableCount` distinct values) | ⚠️ Partly | `IconPack.entries.values.toSet()` gives the distinct drawables the `appfilter` *maps*. A pack may also ship drawables the appfilter never references. **Enumerating raw zip/APK drawables is not implemented** and would be new infrastructure (list zip entries / resource names). |
| Loading *any* drawable name from the pack | ✅ | `IconPackDrawableLoader.load(pack, name)` is name-agnostic. |
| The category glyph set | ✅ | `LineIcon` enum (~60 glyphs), `IconMasker.symbolFor`. |
| The 7 category colors | ✅ | `DrawerIconColor.Token` + `SoftColors.drawerIcon*` (from P3.5). |

**So a "choose any drawable from the pack" picker is possible only if we either
(a) enumerate the pack's real drawables (new infra), or (b) restrict the picker to the
distinct drawables the `appfilter` already maps** (a set the pack author curated for apps
— not for arbitrary use). This is a scope decision — see §2.

---

## 2. Scope decision — the question the user asked

The user asked three things explicitly. Each is answered below with a **recommendation**
and the alternative, so the review can pick.

> **Q-A. For an app that IS in the active pack: can it pick an alternative drawable from
> the same pack?**
>
> **Recommendation: YES — from the pack's *mapped* drawable set** (the distinct values of
> `IconPack.entries`), rendered as a picker grid. This needs the per-app override to
> store a **drawable name**, not a packId (a model change — §4). It does **not** need raw
> zip/APK drawable enumeration.
> *Alternative if you want the full pack arsenal:* add `IconPackDrawableLoader.listDrawables(pack)`
> (zip entries / `Resources` names). More infra; still bounded. **Recommend deferring**
> this to keep P4b tight — the mapped set is usually the pack's intended icon repertoire.

> **Q-B. For an app NOT in the pack (renders a category-colored glyph): what can be edited?**
>
> **Recommendation: pick a *different glyph* and/or a *different color*.** The editor
> presents (i) a **glyph picker** (the `LineIcon` set) defaulting to the app's
> `IconMasker.symbolFor` glyph, and (ii) a **color picker** over the 7 P3.5 category
> colors (`DrawerIconColor.Token`), defaulting to the app's mapped token. This directly
> extends the P3.5 hybrid: a non-pack app that *was* "category glyph + category color"
> becomes "chosen glyph + chosen color". **A user-chosen glyph/color for a non-pack app
> is expressible today** (the renderer already supports `symbol` + `symbolTint`); only
> the override **storage** and the **picker UI** are new.
> *Also offered, for both pack and non-pack apps:* a **"Reset to automatic"** action that
> clears the override and returns to the derived (pack / category) rendering.

> **Q-C. Does "rich editor" in P4b include crop / resize / upload a custom image, or is it
> limited to "pick from options that already exist"?**
>
> **Recommendation: LIMITED to "pick from options that already exist" (pack drawables,
> glyphs, colors, reset). NO crop / resize / upload in P4b.** Rationale:
> 1. **No design source** (§0) — a crop/upload flow is a large, unbounded UI with no mock.
> 2. **A custom-image path is a real subsystem:** SAF/photo-picker permission, decode +
>    downscale, safe-area/adaptive-icon handling (launcher icons must survive masks), the
>    0.49 inset + squircle compositing, storage quota, and a new persistence format. It
>    is genuinely its own phase.
> 3. The P3 promise ("pick from the active pack's drawables") and the P4 scope text
>    ("choose pack drawable **or** category glyph; recolor; reset") are **already**
>    satisfied by the recommendation above — nothing is under-delivered.
>
> **So: crop/resize/upload is proposed as an explicit OUT-of-P4b item**, to be scheduled
> separately (call it a future "P4b-2 / custom icon" phase) only if you want it.

### 2.1 The resulting scope (recommended)

**IN (P4b):**
1. A real **per-app icon override model** that can express: pack-drawable choice,
   glyph choice, and color choice (and "no override").
2. An **Icon Editor card** (cream r24, over a dim scrim — the `FolderPopupBody` /
   `AppContextMenu` vocabulary) opened from the existing **"Edit Icon"** menu row.
3. **Live preview** inside the editor: the tile re-renders exactly as the drawer will.
4. Editors that adapt to the app:
   - **In the pack:** show the pack drawable picker (mapped set) + Reset.
   - **Not in the pack:** show the glyph picker + the color picker + Reset.
5. **Persistence** in `LauncherPrefs` (new per-app override store) via a pure codec.
6. The override applies in **both the drawer and the home rail** where the app is shown
   (same resolver path; see §4.5). *(Question for review — see §8 Q3.)*

**OUT (P4b):**
- Crop / resize / upload a custom image (§2 Q-C).
- Raw pack-drawable enumeration beyond the `appfilter` mapped set (§2 Q-A alt).
- Editing rail icons' *shortcut targets* (that is the "Edit Shortcut" row, unrelated).
- Dark-mode palette for the editor beyond existing token parity (P4c owns the sweep).

---

## 3. Locked decisions (carried in)

These are already user-confirmed and not re-litigated:

| # | Decision | Source |
|---|---|---|
| **P4b-1** | The editor builds on the **P3.5 hybrid renderer** (pack drawable *or* category-colored glyph). | P3.5-4 / P4 scope §2 |
| **P4b-2** | The entry point is the existing **"Edit Icon"** row in the long-press `AppContextMenu`. | P3-3 / D-030 |
| **P4b-3** | Icon changes are **user data** → persisted (DataStore), survive process death — same bar as P2/P3 prefs. | house rule (P2-2 / P3) |
| **P4b-4** | The `.pen` stays **read-only**; the editor is authored from Warm tokens as a documented assumption (§7). | D-002 / D-005 |
| **P4b-5** | No new visual language: cream r24 card + row-style controls + the `TpzL1` color set. | D-017 / D-030 |

**New decisions proposed for P4b (need confirmation — §8):**

| # | Decision | Proposal | Why |
|---|---|---|---|
| **P4b-6** | **Override data shape** | Replace the ambiguous `iconOverrides: Map<String,String>` (key→packId) with a typed `Map<componentKey, IconOverride>` where `IconOverride` = `Pack(drawableName)` \| `Glyph(symbolName, colorToken)` \| (absent = auto). | The current shape **cannot** express a chosen drawable/glyph/color (§1.1). A typed sealed value is the minimal honest model, and it is pure/testable. |
| **P4b-7** | **Pack app editor** | Drawable picker over the pack's **mapped distinct drawables**; Reset. | Fulfils the P3 Q4 promise without new zip-enumeration infra. |
| **P4b-8** | **Non-pack app editor** | Glyph picker (LineIcon set) + color picker (7 P3.5 tokens); Reset. | Directly extends the P3.5 hybrid; no new render capability needed. |
| **P4b-9** | **Reset** | Clearing the override returns the app to its derived rendering (pack default / category glyph+color). Always offered. | Non-destructive, matches P4a's "snap back = no change" spirit. |
| **P4b-10** | **Applies to** | The override is applied wherever the app is rendered **through the shared resolver** — drawer grid, folder previews/popups, and the **home rail** if it shows that app. | One override, one source of truth (the `IconResolver`). |
| **P4b-11** | **Migration** | Existing `iconOverrides` (key→packId) migrate to `IconOverride.Pack(defaultDrawableFor(key))` on read; an unresolvable value is dropped (falls back to auto). | Never lose data, never crash on an old value (D-028 spirit). |

---

## 4. Technical design

### 4.1 The override model (pure, `core:model`)

```kotlin
// core:model/IconOverride.kt
/** A user's explicit icon choice for one app. Absence = automatic (P3.5 hybrid). */
sealed interface IconOverride {
    /** Use a specific drawable from the active pack (P4b-7). */
    data class Pack(val drawableName: String) : IconOverride

    /** Use a specific lucide glyph in a specific category color (P4b-8). */
    data class Glyph(val symbolName: String, val colorToken: DrawerIconTokenName) : IconOverride

    /** P4b-9: an explicit "use the automatic choice" marker (clears any stored value). */
    // (Reset is modeled as *removing* the key, not as a third variant — see the codec.)
}

/**
 * The color token, mirroring DrawerIconColor.Token but declared in core:model so the
 * persisted value never depends on the iconpack feature module. One place owns the names.
 */
enum class DrawerIconTokenName { Communication, Social, Productivity, Media, Travel, Finance, Neutral }
```

`LauncherPrefs` gains:

```kotlin
val iconOverrides: Map<String, IconOverride> = emptyMap(),   // was Map<String, String>
```

> **Migration (P4b-11).** The DataStore key is currently unused in practice (no setter
> exists — §1.1), so the *storage* may hold an old `key→packId` string map only if a
> prior session wrote one. The codec must therefore: decode new format → if absent, try
> legacy `key→packId` → map each to `IconOverride.Pack(<the pack's default drawable for
> that key>)` **only if the drawable resolves**; otherwise drop the entry. There is no
> crash path.

### 4.2 The pure codec (`core:data`, JVM-tested)

A `IconOverridesCodec` (mirroring `HomeRowsCodec`) encodes `Map<String, IconOverride>`
to a single string key (JSON-in-DataStore, as `FoldersCodec` already does) and decodes
**totally** (unknown variants / malformed segments are skipped). Unit-tested for:
round-trip, legacy migration, malformed input, empty map, unknown token names.

### 4.3 The pure edit logic (`core:model`, JVM-tested)

The resolver is the single decision point. Extend `IconResolver.resolve` to consult the
typed override **first**:

```
0. explicit override
   - IconOverride.Glyph  -> IconSource.Glyph(symbolName, colorToken)   /* new source */
   - IconOverride.Pack   -> IconSource.Override(packId, drawableName)  /* now the *chosen* name */
1. icon-pack match (FromPack)
2. auto-mask / system
```

`IconSource` gains one variant:

```kotlin
/** P4b: a user-chosen glyph in a user-chosen color (non-pack apps). */
data class Glyph(val symbolName: String, val colorToken: DrawerIconTokenName) : IconSource
```

`ResolvedIcon` already carries `symbolName`; add an optional `overrideColorToken` so the
drawer renderer can tint a chosen glyph. This keeps resolution **pure** and keeps the
renderer's job unchanged (it already draws either a painter or a glyph).

### 4.4 The renderer change (`DrawerAppIcon`, minimal)

`DrawerAppIcon` already selects `packPainter` vs `symbol` from `ResolvedIcon`. P4b only
adds: when `resolved.source is IconSource.Glyph`, the symbol is the override's
`symbolName` and the tint is `overrideColorToken`'s color instead of the category-derived
one. **No layout change; the tile atom is untouched.** A pack override already flows
through the existing `drawableName` decode path.

### 4.5 Where the override is applied

`IconResolver` is already used by **both** `AppDrawerViewModel.resolveIcon` and
`HomeViewModel.resolveIcon`. Wiring the typed override into the resolver means the drawer
**and** the home rail honor it automatically — one change, both surfaces (P4b-10). *This
is a review question — see §8 Q3.*

### 4.6 The editor UI (new: `feature:iconpack/ui/IconEditorSheet.kt`)

A cream **r24 card over a dim scrim** (the `FolderPopupBody` / `AppContextMenu`
vocabulary — not a platform dialog, not the M3 `ModalBottomSheet` unless you prefer that;
see §8 Q2). Layout, top-to-bottom:

```
┌────────────────────────────────────────────┐  cream r24 card, ~min(360, w-48)
│  Edit icon · <App label>            [ × ]   │  title row (titleLarge + X)
│                                             │
│      ┌──────────┐   live preview           │  DrawerAppIcon at 68 r21, the
│      │  <icon>  │   (same render path)     │  EXACT drawer composable, so the
│      └──────────┘                           │  preview cannot drift from the grid
│                                             │
│  [ Pack ]   [ Glyph ]   ← segmented tab,    │  the tab shown depends on whether
│                 only the relevant is active │  the app is in the pack (P4b-7/8)
│  ── drawable grid ── or ── glyph grid ──    │  4-6 col grid of 48 r14 tiles
│  ── color swatches (glyph mode) ──          │  the 7 TpzL1 tokens as r14 swatches
│                                             │
│  Reset to automatic            [ Save ]     │  Reset = clear; Save = persist & close
└────────────────────────────────────────────┘
```

- **Pack mode (app in pack):** a grid of the pack's mapped distinct drawables (each drawn
  at the 0.49 inset on a cream tile); tapping one updates the preview; Save writes
  `IconOverride.Pack(name)`.
- **Glyph mode (app not in pack):** a grid of `LineIcon` glyphs (the bundled set) + a row
  of 7 color swatches; tapping updates the preview; Save writes
  `IconOverride.Glyph(symbol, token)`.
- **Reset:** clears the override (persist the key removed) and closes — the tile returns
  to derived rendering.
- **Save is only enabled when something changed** (no-op otherwise); **BACK / scrim tap /
  ×** = cancel (no write). (Matches P4a-10 "predictable, non-destructive".)

The editor is a **pure function of injected data** (`pack`, current override, the app's
resolved defaults) so its state machine is unit-testable without a device (§6).

### 4.7 Persistence path

`PrefsRepository` gains:

```kotlin
suspend fun setIconOverride(componentKey: String, override: IconOverride?)
suspend fun clearIconOverride(componentKey: String)   // == setIconOverride(key, null)
```

Writing `null` removes the key. The drawer/home re-resolve reactively (the VM already
`combine`s `prefsRepository.prefs` + `activePack`), so the change is live with no extra
plumbing (same reactive shape as P3.5/P4a).

---

## 5. Files to touch (module map)

| Module | Change |
|---|---|
| `core:model` | New `IconOverride.kt` (`IconOverride`, `DrawerIconTokenName`); change `LauncherPrefs.iconOverrides` type; add `IconSource.Glyph`; extend `ResolvedIcon` (`overrideColorToken`); update `IconResolver.resolve` to consult the typed override first (pure). |
| `core:data` | New `IconOverridesCodec` (pure, JSON, total decode + legacy migration); `PrefsRepository.setIconOverride` + clear; wire the codec into `prefs` read + writes. |
| `core:designsystem` | New `atom/IconEditorScaffold.kt`: the cream r24 editor card shell + a `ChoiceTile` (48 r14 selectable tile used by both the drawable and glyph grids) + a `ColorSwatchRow`. New `Dimens` (`editorCardMaxWidth`, `choiceTile`, `choiceTileRadius`, `swatch`, `swatchRadius`). No new colors (P3.5 drawer palette + facade tokens). |
| `feature:iconpack` | New `ui/IconEditorSheet.kt` (+ a small `IconEditorState` pure reducer). |
| `feature:appdrawer` | `AppMenu`'s `onEditIcon` opens the editor; `AppDrawerViewModel` gains `editingKey` state + `applyIconOverride` / `resetIconOverride`; `AppDrawerScreen` renders the sheet. |
| `feature:home` | **No change expected** (honors the override via the shared resolver). Verify the rail re-renders on override change; only touch if the rail does not observe `prefs` (audit in Phase 0). |
| `app` | **No change** (entry point is the drawer's menu). |
| `docs` | `00` (P4b note), `03` (feature map: P4b rows), `04` (new section L), `05` (session log), `06` (test plan), `09` (D-037 override model, D-038 scope). |

No new Gradle module.

---

## 6. Test plan (real evidence, per prior phases)

**Unit (JVM, no device):**
- `IconOverridesCodecTest` — round-trip each variant; legacy `key→packId` migration;
  malformed / unknown-token segments skipped; empty map; **total** decode (never throws).
- `IconResolverOverrideTest` — an `IconOverride.Glyph` wins over pack/auto; a
  `Pack(name)` yields `IconSource.Override` with *that* drawable; **no** override → the
  P3.5 path is unchanged (regression); unknown drawable name → falls through safely.
- `IconEditorStateTest` (pure reducer) — select drawable/glyph/color updates the draft;
  Reset clears; Save emits the right `IconOverride`; cancel emits nothing; "changed" gating.
- **Regression guard:** the **7 icon-pipeline tests stay green** (AutoMask, IconResolver,
  IconMasker, GlyphUniqueness, Importer, Persistence, AppFilterParser). If any needs an
  edit, the boundary leaked — stop and re-scope (same rule as P3.5 §5.2).

**Instrumented (Compose, AVD):**
- `IconEditorOpenTest` — long-press a drawer tile → "Edit Icon" → the editor card shows
  the app label + a live preview.
- `IconEditorPackTest` — pick a different drawable → Save → the drawer tile changes to it.
- `IconEditorGlyphTest` — for a non-pack app, pick a glyph + color → Save → the tile
  shows the chosen glyph in the chosen color.
- `IconEditorResetTest` — after an override, Reset → the tile returns to the derived icon.
- `IconEditorPersistTest` — an override survives a VM re-creation (rehydrate from prefs).

**On-device verification (emulator, API 35 `soft_home_pixel`)** — real screenshots:
- [ ] Editor open (pack app): preview + drawable grid.
- [ ] Editor open (non-pack app): preview + glyph grid + color swatches.
- [ ] After a pack-drawable change → the drawer tile shows the chosen drawable.
- [ ] After a glyph+color change → the tile shows the chosen glyph/color.
- [ ] **The override survives `am force-stop`** (dumped DataStore value + restored tile).
- [ ] Reset → derived icon returns.
- [ ] The long-press menu is otherwise intact (Open/App Info/Remove/Uninstall greyed).

---

## 7. Documented assumptions (to record in `04` section L)

| # | Gap | Assumption |
|---|---|---|
| L1 | `.pen` has no editor mock | Cream r24 editor card over a dim scrim (the `FolderPopupBody`/`AppContextMenu` vocabulary), not a platform dialog. |
| L2 | No picker mock | Drawable/glyph grids of 48 r14 selectable tiles; 7 color swatches from the P3.5 `TpzL1` palette. |
| L3 | "Pick from pack" ambiguity | The picker offers the pack's **mapped distinct drawables** (`entries.values`), not raw zip/APK drawables (deferred). |
| L4 | Non-pack edit depth | A non-pack app may choose a **glyph + color**; that is the "rich" depth in P4b (no crop/upload). |
| L5 | Reset semantics | Reset = remove the override → derived (pack/category) rendering; no confirmation needed (reversible). |
| L6 | Override scope | One override per app, applied wherever the shared `IconResolver` renders it (drawer + folder previews + home rail). |
| L7 | Preview fidelity | The editor preview uses the **same** `DrawerAppIcon` composable as the grid, so it cannot drift. |

---

## 8. Decisions needed from the user before implementation

1. **Scope of "rich" (§2):** confirm **IN** = pack-drawable pick + glyph pick + color pick
   + reset; **OUT** = crop / resize / upload a custom image. (Recommended: yes.)
2. **Editor surface (§4.6):** a **cream r24 card over a dim scrim** (Warm vocabulary,
   like the folder popup) — **or** an M3 `ModalBottomSheet` (like `IconPackImportSheet`,
   which already exists)? (Recommended: the cream card, for vocabulary consistency.)
3. **Override scope (§4.5 / P4b-10):** apply the override to **the drawer + home rail +
   folder previews** (everything through the resolver), or **drawer only**? (Recommended:
   everywhere — one source of truth.)
4. **Pack picker breadth (§2 Q-A):** the **mapped** drawable set only (recommended), or
   also enumerate the pack's **raw** drawables (adds `listDrawables` infra)?
5. **Model change approval (§4.1 / P4b-6):** OK to change `iconOverrides` from
   `Map<String,String>` (key→packId) to the typed `Map<String, IconOverride>` with the
   legacy migration described in P4b-11?

**STOP — no implementation until these are answered.**

---

## 9. Phase plan (P4b)

| Phase | Work | Exit |
|---|---|---|
| **0** | Audit: confirm the current gap (§1), the resolver call sites (drawer + home), and that no `iconOverrides` setter/writer exists. Re-confirm the `.pen` is byte-identical. | Findings recorded; scope locked. |
| **1** | Pure model: `IconOverride`, `DrawerIconTokenName`, `IconSource.Glyph`, `ResolvedIcon.overrideColorToken`; extend `IconResolver`. | `IconResolverOverrideTest` green; pipeline tests untouched. |
| **2** | Persistence: `IconOverridesCodec` + `PrefsRepository.setIconOverride/clear` + `LauncherPrefs` type change + migration. | `IconOverridesCodecTest` green; persistence test green. |
| **3** | Editor logic (pure reducer) + tokens (`Dimens`) + `IconEditorScaffold` atoms. | `IconEditorStateTest` green; atoms render in a preview. |
| **4** | Editor UI (`IconEditorSheet`) + drawer wiring (menu → editor; VM methods; renderer honors `IconSource.Glyph`). | Instrumented tests green. |
| **5** | On-device verification (screenshots + force-stop persistence + reset). | Evidence captured. |
| **6** | Docs (`00/03/04/05/06/09`) + cleanup (remove any dead override handling). | Docs consistent; build clean; suites green. |
