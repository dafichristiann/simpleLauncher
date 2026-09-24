# P4a — Drag & Drop — Design Spec

> **Date:** 2026-09-25
> **Type:** Interaction subsystem (one gesture layer, two drop targets).
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Parent spec:** [P4 scope & phase split](2026-09-25-p4-scope-and-phase-split-design.md)
> **Status:** Spec written — **awaiting user review before implementation.**

This document is the single source of truth for **P4a**. It contains the `.pen`
re-audit, the locked decisions, the technical design (gesture detection, drag preview,
drop-zone feedback), the **shared-vs-separate** analysis the user asked for, the exact
files to touch, the test plan, and the phase plan.

**No code is written under this spec. It stops for user review, same as P1–P3.5.**

---

## 0. `.pen` re-audit (2026-09-25) — still no drag/drop design nodes

Re-audited immediately before writing this spec (the user edits `homeApp.pen` by hand).

**File:** `design/homeApp.pen`, **176,128 bytes**, last written 2026-09-25 11:58 —
**unchanged** since the P4-scope audit. `git status`: modified vs. the pre-P3.5 baseline
(expected; the user edits it).

**Top-level frames (6, unchanged):** `znb90` (light home), `L7ZAp` (spec board),
`TpzL1` (drawer), `KkPN3` (dark home), `QBhow` + `ciHU3` (glyph libraries).

**Keyword sweep for drag/drop concepts — all ABSENT:**

| Keyword | Hits | Meaning in file |
|---|---|---|
| `drag` / `drop` / `reorder` | 0 | **Nothing.** No reorder or drag affordance is designed. |
| `folder` | 0 | **Nothing.** No folder mock (folders are a code-only feature from P2). |
| `handle` / `grip` / `grab` | 0 | Nothing — no drag-handle glyph exists in the libraries. |

**Consequence:** P4a has **no design source**. The gesture is authored from the existing
Warm Right Rail vocabulary as documented assumptions (§7). The `.pen` stays **read-only**
(byte-identical after the build, like every prior phase).

**Relevant existing nodes (reused as *layout*, not as a drag spec):**
- Home row list → `znb90` rows (`IDSBb` time … `QTwqr` music) + P2 rows (`CalendarRowContent`,
  `BatteryStorageRowContent`, `NotesRowContent`).
- Drawer grid → `TpzL1` 4-col grid (`dBQmH` 68 r21 tiles + `J3Lb4` labels); `FolderTile`
  is the existing 2×2-preview tile atom.
- Motion → the board `Z5V7Y9` tokens (`RAIL SLIDE 220 / SEARCH FADE 160 / MUSIC RISE 280`,
  ease `cubic-bezier(0.2,0.8,0.2,1)`) — the drag/lift animations borrow this family.

---

## 1. Locked decisions (carried from P3-2 + P4 scope)

These are already user-confirmed and are not re-litigated here:

| # | Decision | Source |
|---|---|---|
| **P4a-1** | Drag-and-drop is **one gesture subsystem** delivering **both** targets (row reorder + app→folder). | P3-2, P4 scope §2 |
| **P4a-2** | Drag **complements** the existing up/down buttons — it does **not** replace them. Both stay. | D-031 keeps buttons; P4 adds drag |
| **P4a-3** | Folder membership is driven by the existing `FolderLogic` / `FolderRepository`; drag only produces the same `addApp`/`removeApp`/reorder calls the buttons/popup already make. | P2 folder infra |
| **P4a-4** | Row order is the existing `HomeRowPref` list order; drag produces the same `setHomeRows` call the buttons make. | `HomeRowLogic` |
| **P4a-5** | Locked rows (Time/Date/Weather) stay visible, but **are reorderable** (only visibility is locked, not order). | `HomeRowLogic.LOCKED` = visibility-only |

**New decisions proposed for P4a (need user confirmation):**

| # | Decision | Proposal | Why |
|---|---|---|---|
| **P4a-6** | **Drag trigger** | **Long-press then drag** (haptic on lift). No permanent drag handle on home rows (keeps the design clean); the drawer grid also long-press-lifts. | Matches the existing long-press vocabulary (P3-3). A persistent handle would add visual language the `.pen` doesn't have. |
| **P4a-7** | **Row reorder drop target** | Drop **anywhere vertically** in the row list; the list animates live to show the insertion point (other rows shift). | Standard list-reorder; no empty "slots" are drawn. |
| **P4a-8** | **App→folder drop target** | Dropping an app onto a **folder tile** adds it to that folder. Dropping onto the **"New folder" header action / empty space** creates a new folder containing the app. Dropping anywhere else = snap back (no-op). | Covers the P2-deferred "assign app to folder"; also enables a fast create-by-drag. |
| **P4a-9** | **Dragging an app out of a folder** | Dragging a member **out of an open folder popup** onto the drawer background removes it from the folder. | Symmetry: in-by-drag must have out-by-drag. Small addition. |
| **P4a-10** | **Cancel gesture** | Release outside a valid target, or BACK → snap back with no change. | Predictable, non-destructive. |

---

## 2. Shared vs. separate — the key technical question

The user explicitly asked: *can row reorder and app→folder share the same
component/logic, or must they be handled separately?*

**Answer: split into a SHARED FOUNDATION + two THIN TARGET ADAPTERS.** They share the
gesture *engine*; they differ only in (a) what item is dragged and (b) what a drop means.
Forcing them into one composable would be worse, because the two surfaces (a vertical
Row/Column list vs. a `LazyVerticalGrid` of mixed app/folder cells) have incompatible
hit-testing.

```
┌──────────────────────────────────────────────────────────────────┐
│ SHARED (core:designsystem  ──  pure/재사용)                        │
│  • DragAndDropState  … "what is being dragged" + pointer offset    │
│  • Modifier.dragSource(…)  … long-press-lift + emit drag start/move│
│  • Modifier.dropTarget(…)  … report hover + bounds, call onDrop    │
│  • DragPreview overlay    … the lifted item rendered under finger  │
│  • DragInsertionLine atom … the 3px accent insertion indicator      │
├──────────────────────────────────────────────────────────────────┤
│ ADAPTER A: Home row reorder (feature:home)                         │
│  drag kind=HomeRowKind  → drop = new index in HomeRowPref list     │
├──────────────────────────────────────────────────────────────────┤
│ ADAPTER B: App → folder (feature:appdrawer)                        │
│  drag kind=AppKey       → drop = addApp/removeApp/newFolder         │
└──────────────────────────────────────────────────────────────────┘
```

**Why a shared engine is worth it (not over-engineering):**
1. **Same gesture math** — long-press detection, finger offset, lift animation, snap-back
   are identical; writing them twice risks two subtly different feels.
2. **One visual language** — the drag preview + insertion indicator are defined once, so
   both surfaces lift the same way (consistent with "small movement, clear purpose").
3. **Both are unit-testable at the pure layer** — the adapters' drop resolution is pure
   functions (index math / folder membership), so they get JVM tests without a device.

**Why NOT one composable:** home rows are a `Column` of unequal-height full-width rows;
the drawer is a `LazyVerticalGrid` with `DrawerCell.AppEntry`/`FolderCell`. A single
"reorderable list" component cannot serve a grid with mixed cell types. The **shared
part is state + modifier + preview**, not layout.

---

## 3. Technical design

### 3.1 Gesture detection

- **Lift:** `Modifier.pointerInput` with `detectDragGesturesAfterLongPress` (Compose
  foundation). Long-press threshold = platform default; fire `HapticFeedbackType.LongPress`
  on lift.
- **Why after-long-press, not plain drag:** the drawer grid must not steal horizontal
  scroll/edge drags; the home rows must not fight the **swipe-up-to-drawer** gesture
  (P3/A-#47). A long-press gate keeps tap = open, long-press = context menu vs drag —
  **conflict to resolve:** see §3.5 (long-press currently opens the context menu).
- **Pointer tracking:** accumulate `change.position` into a `DragAndDropState.delta`;
  render the preview at `origin + delta`.
- **Threshold:** require ~`touchSlop` movement after the long-press before treating it as
  a drag; a long-press with no movement keeps the existing **context-menu** behaviour.

### 3.2 Drag preview (the "lift")

- **Source rendering:** the dragged item is drawn as a **floating copy** in an overlay
  `Box` at the top of the surface, at a slight **scale-up (~1.05)** with a soft shadow —
  reusing `Elevation` tokens (no new shadow language). The in-place original is drawn at
  **~35% alpha** so the gap is legible.
- **Home rows:** preview = a compact **row chip** (row label e.g. "Weather" on a cream
  r14 pill), not the full-height row — a full row under the finger would be unwieldy.
- **Drawer apps:** preview = the **actual app icon tile** (`DrawerAppIcon`) at the tile
  size — the icon is the thing being moved, so it must look identical.
- **Motion:** lift uses the board ease `cubic-bezier(0.2,0.8,0.2,1)` at `SEARCH FADE`-class
  duration (~160ms); snap-back uses the same ease at ~180ms.

### 3.3 Drop-zone feedback

- **Row reorder:** a **3px accent `#8A5F43` insertion line** (`DragInsertionLine`) drawn
  between rows as the finger crosses a midpoint; the list **shifts rows live** using the
  same "grow in-place" motion family as `MUSIC RISE`. No empty slot is drawn.
- **App→folder:** the hovered **folder tile scales ~1.08 and gets an accent
  (`#8A5F43`) ring**; the "New folder" header chip highlights when hovered. Non-targets
  do nothing.
- **Valid-target semantics:** a target exposes `onHoverEnter/Exit`; the state marks
  `hoveredTargetId`. Only a valid target can accept a drop.

### 3.4 State model (pure, testable)

```kotlin
// core:designsystem — pure data, no Android
sealed interface DragKind {
    data class HomeRow(val kind: HomeRowKind) : DragKind
    data class App(val componentKey: String) : DragKind
    /** A member dragged OUT of an open folder popup. */
    data class FolderMember(val folderId: String, val componentKey: String) : DragKind
}

data class DragAndDropState(
    val dragging: DragKind? = null,
    val originPx: Offset = Offset.Zero,
    val deltaPx: Offset = Offset.Zero,
    val hoveredTargetId: String? = null,
)

// Pure resolvers (unit-tested, no Compose):
object HomeRowDropResolver {
    /** New HomeRowPref list when [dragged] is dropped before visible index [targetIndex]. */
    fun reorder(prefs: List<HomeRowPref>, dragged: HomeRowKind, targetIndex: Int): List<HomeRowPref>
}

object FolderDropResolver {
    /** Folder list after app [key] is dropped on [folderId]. */
    fun assign(folders: List<Folder>, folderId: String, key: String): List<Folder>
    /** Folder list after app [key] is dropped on the "new folder" action. */
    fun createWith(folders: List<Folder>, key: String, newId: String, name: String): List<Folder>
    /** Folder list after [key] is dragged out of [folderId]. */
    fun removeFrom(folders: List<Folder>, folderId: String, key: String): List<Folder>
}
```

The resolvers are **pure** and delegate to the existing `HomeRowLogic` / `FolderLogic`
(so there is exactly one place that knows row order / folder membership).

### 3.5 The long-press conflict (must be resolved in implementation)

Today: **long-press a drawer tile or rail icon → context menu** (P3-3).
P4a wants: **long-press then drag → move**.

**Resolution (proposed, P4a-6):**
- **Long-press with no movement → context menu** (unchanged).
- **Long-press then move past `touchSlop` → drag** (new).
- On the **home rows**, long-press currently has **no** menu (only rail icons + drawer
  tiles have menus), so rows are conflict-free.
- On the **drawer grid**, the two are split by movement: hold-and-release = menu,
  hold-and-drag = move. This matches Android list conventions.
- Rail icons are **not** drag sources in P4a (their membership is fixed in P3); their
  long-press menu is untouched.

### 3.6 Where the overlay lives

Each surface owns its own `DragAndDropState` (home screen, drawer) so a drag never
crosses surfaces. The preview overlay is a sibling `Box` inside the existing root `Box`
of the surface — no global overlay, no new window.

---

## 4. Files to touch (module map)

| Module | Change |
|---|---|
| `core:model` | Add `DragKind`, `DragAndDropState` (or place in designsystem); add pure `HomeRowDropResolver` (wraps `HomeRowLogic`) and `FolderDropResolver` (wraps `FolderLogic`). Extend `FolderLogic` if a `reorderWithin` is needed. **No Android deps → JVM-tested.** |
| `core:designsystem` | New `atom/DragAndDrop.kt`: `Modifier.dragSource` / `Modifier.dropTarget`, `DragPreview` overlay, `DragInsertionLine` atom. Extend `Dimens` (insertion line 3dp, lift scale 1.05, row-chip radius 14) + reuse `MotionTokens` (add `dragLift()`/`snapBack()`). |
| `feature:home` | Wrap the row list in drag state; each `HomeRowSlot` becomes a `dragSource`; render insertion line; `HomeViewModel.reorderHomeRow(dragged, targetIndex)` → `prefsRepository.setHomeRows(...)`. |
| `feature:appdrawer` | Wrap the grid in drag state; `AppCell` = `dragSource` + (folder tiles) `dropTarget`; `FolderPopup` members = `dragSource` (drag-out); `AppDrawerViewModel.assignToFolder/createFolderWith/moveOut` via `FolderRepository`. |
| `app` | Nothing (settings unchanged; up/down buttons stay). |
| `docs` | `04` (new section K), `05` (session log), `09` (D-034), `00` (P4a note), `03` (feature map), `06` (test plan). |

No new Gradle module (drag is atoms + feature wiring).

---

## 5. Test plan (real evidence, per prior phases)

**Unit (JVM, no device):**
- `HomeRowDropResolverTest` — reorder in every direction, clamping at ends, locked rows
  remain and still move, unknown kind → no-op, sanitize-stable.
- `FolderDropResolverTest` — assign existing app, assign duplicate (no-op), create-with,
  drag-out, empty-folder after removal, folder id collisions.
- `DragAndDropStateTest` — hover enter/exit, valid-target gating, cancel clears state.
- Keep the **7 icon-pipeline tests green** (AutoMask, IconResolver, IconMasker,
  GlyphUniqueness, Importer, Persistence, AppFilterParser) — guarded as in P3.5.

**Instrumented (Compose, AVD):**
- `HomeRowDragTest` — long-press a row, drag it up/down, assert order changed + persisted.
- `DrawerFolderDropTest` — long-press an app, drag onto a folder tile, assert membership.
- `DrawerDragOutTest` — drag a member out of an open folder, assert removal.
- `DragCancelTest` — release over empty space → unchanged.
- `LongPressMenuStillWorksTest` — long-press with no movement still opens the menu.

**On-device verification (emulator, API 35 `soft_home_pixel`)** — screenshots **+ short
screen-recordings** (gestures cannot be proven by a still):
- [ ] Row reorder mid-drag (insertion line visible) + settled result.
- [ ] App → folder drop (hover ring) + folder preview updated.
- [ ] App → "New folder" header (drop-to-create).
- [ ] Drag-out from an open folder.
- [ ] Cancel / snap-back.
- [ ] Row order + folder membership **survive `am force-stop`**.

---

## 6. Phase plan

| Phase | Work | Exit |
|---|---|---|
| **0** | Pure model + resolvers (`core:model`) + tests | Unit tests green |
| **1** | Design-system atoms (`dragSource`/`dropTarget`/`DragPreview`/`DragInsertionLine`) + `MotionTokens` additions | Renders in isolation |
| **2** | **Home row reorder** wired end-to-end + persistence | Instrumented + on-device |
| **3** | **Drawer app→folder** (assign / create-with / drag-out) | Instrumented + on-device |
| **4** | Long-press-conflict handling (menu vs drag) + cancel paths | Tests green |
| **5** | On-device verification (screenshots + recordings) | Evidence captured |
| **6** | Docs (`00/03/04/05/06/09`) | Docs updated |

---

## 7. Documented assumptions (to record in `04` section K)

| # | Gap | Assumption |
|---|---|---|
| K1 | `.pen` has no drag/drop mock | Long-press-lift (haptic), floating preview (~1.05 scale, soft shadow), 3px accent insertion line / accent hover ring. Built from Warm tokens. |
| K2 | No drag-handle glyph | No persistent handle; the whole row/tile is the drag source after long-press. |
| K3 | Long-press already opens a menu | Split by movement: no-move → menu; move-past-slop → drag. |
| K4 | Row preview shape | Compact **row chip** (label pill), not the full-height row. |
| K5 | Drag-out of a folder | Supported (P4a-9) for symmetry, though not explicitly requested. |
| K6 | Drop-to-create folder | Dropping an app on the "New folder" header creates a folder containing it. |

---

## 8. Decisions needed from the user before implementation

1. **Approve P4a-6…P4a-10** (drag trigger, drop targets, drag-out, cancel).
2. **Long-press conflict** (§3.5): OK with "hold-and-release = menu, hold-and-drag = move"?
3. **Drag-out of folders** (P4a-9) — in scope, or keep P4a to app→folder only?
4. **Any surface order preference** — do rows first (Phase 2) then app→folder (Phase 3), as
   planned? (Recommended: yes.)

**STOP — no implementation until these are answered.**
