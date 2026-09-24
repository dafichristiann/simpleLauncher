# P4 — Scope & Phase-Split (P4a…P4e) — Design Spec

> **Date:** 2026-09-25
> **Type:** Planning + scoping spec. P4 is **not** one phase: it is split into
> sub-phases (P4a…P4e), each its own audit → spec → confirm → implement → verify → docs
> cycle.
> **Design source:** [`design/homeApp.pen`](../../../design/homeApp.pen) — Pen v2.18 (read-only)
> **Status:** Spec written — **awaiting user review before any implementation starts.**

This document is the single source of truth for the **P4 scope decision**. It contains:
(0) the pre-spec `.pen` re-audit, (1) the item-by-item disposition (do-now vs. keep-deferred
vs. drop), (2) the proposed sub-phase split with priorities, (3) the carry-in deferrals,
and (4) an explicit "what is NOT being built and why" list.

**No code is written under this spec. It stops for user review, same as P1–P3.5.**

---

## 0. `.pen` re-audit (2026-09-25) — for the P4 scope

Re-audited just before writing this spec, because the user is editing `homeApp.pen`
manually and the file may have moved since P3.5.

**File:** `design/homeApp.pen`, **176,128 bytes**, last written 2026-09-25 11:58
(the P3.5 build read it at the same size). `git status` shows the file **modified vs.
the committed baseline** (baseline predates P3.5) — expected, since the user edits it
by hand. **No P4-relevant nodes were added since P3.5.**

**Top-level frames (all 6, unchanged from P3.5):**

| Node ID | Name | W×H | Role for P4 |
|---|---|---|---|
| `znb90` | Home Screen Mockup — Warm Right Rail | 390×720 | Primary **light** home |
| `L7ZAp` | Warm Home Screen — Full System Board | 1440×1180 | Spec sheet (4 panels) |
| `TpzL1` | Warm App Drawer — Unique Icon Grid | 430×860 | App drawer (drag/assign target) |
| `KkPN3` | Home Screen Mockup — Dark Editorial | 390×720 | **Dark home reference — now relevant** |
| `QBhow` | Icon Library Panel | 410×900 | Monochrome glyph library (rail/atoms) |
| `ciHU3` | Warm Right Rail — Icon Language Library | 520×844 | 144 monochrome glyphs, 8 groups |

**Keyword sweep across every node name — all P4 concepts verified ABSENT:**

| Keyword | Hits | Meaning in file |
|---|---|---|
| `drag` / `drop` / `reorder` | 0 | **Nothing.** No reorder or drag affordance is designed. |
| `folder` | 0 | **Nothing.** No folder mock (folders are a code-only feature). |
| `edit` | 1 | Only the frame name **"Home Screen Mockup — Dark Editorial"** (substring "Edi**t**orial"). Not an "Edit Icon" spec. |
| `backup` / `restore` / `export` / `import` | 0 | **Nothing.** |
| `wallpaper` | 0 | **Nothing.** |
| `media` / `session` / `playback` | 0 | Only two **category labels** ("Media & Entertainment", "Camera & Media"). No MediaSession spec. |
| `widget` | 0 | **Nothing.** |
| `calendar` | 8 | Only drawer **app tiles** (`App Tile Calendar`) + lucide glyph names, as in P2/P3. Not a live-calendar spec. |
| `theme` | 2 | Only the **`icon-themes` glyph** (a drawable), not a theming spec. |

**So: P4 has exactly ONE reference frame — `KkPN3` (dark editorial).** Everything
else is authored from the existing Warm Right Rail vocabulary as documented assumptions.

### `KkPN3` dark palette (newly extracted — this is the authoritative dark source)

Unlike P1–P3, the dark home now has a **named** palette in the file, not just a derived
guess. Extracted node-by-node:

| Element | Node | Value |
|---|---|---|
| Screen bg | `KkPN3` | `#18191A` |
| Dark rail bg | `q0n2Wd` | `#2E3134` |
| Divider | `QzH19` | `#343638` |
| Rail icon | `tYsFj` | `#F2EEE7` |
| Time / Date number / Weather / Search icon / controls / signature | — | `#F2EEE7` / `#E2DDD5` / `#F2EEE7` |
| Date day | `pUJJF` | `#D2CBC1` |
| Date month / music track | `nis9l` / `l4APa` | `#918F8B` / `#817F7B` |
| Search placeholder | `M5WWZf` | `#A8A29A` |
| Music artist | `I9K2BT` | `#C8C0B6` |
| Progress track / fill | `Tl7UC` / `wpEPF` | `#676866` / `#F2EEE7` |
| Album art circle / album mark | `rkXwU` / `PDi5L` | `#E3DED6` / `#454648` |

*(Screenshot `KkPN3` confirm: dark rail on the left, warm-neutral off-white type on a
near-black `#18191A` canvas. It is **not** the derived `#1F1D1A` warm-dark from P1
assumption #5 — this is a genuinely new, cooler/neutral palette.)*

**This is the one real design change P4 inherits: dark mode can now be rendered
faithfully instead of from a derived palette (see P4c).**

**Verdict for the user's question "any new frame/node relevant to P4?"** — One: `KkPN3`
already existed but was only ever a "reference"; it is now the concrete target for the
dark-mode sub-phase. **No brand-new frames** were added since P3.5.

---

## 1. Item-by-item disposition

Legend: **✅ DO-NOW** · **🟡 KEEP-DEFERRED** · **⚪ DROP-FROM-ROADMAP**

| # | Item | Origin | Verdict | Rationale |
|---|---|---|---|---|
| 1 | **Drag-and-drop: reorder home rows** | Defer P3 (P3-2) | **✅ DO-NOW (P4a)** | Explicitly promised as the P4 half of the P3-2 decision. Up/down buttons exist now; drag is the missing piece. Self-contained gesture on the home list. |
| 2 | **Drag-and-drop: assign app → folder** | Defer P2 (#52) | **✅ DO-NOW (P4a)** | Same gesture subsystem as #1; P3-2 explicitly couples them ("together … as one gesture subsystem"). Folder infra already exists (P2). |
| 3 | **Edit Icon — rich editor** | Defer P3 (Q4) | **✅ DO-NOW (P4b)** | The whole reason P3.5 was run first: "so P4's Edit Icon editor builds on the final model." Hybrid renderer (pack drawable or colored category glyph) is done; the editor is the deferred payoff. |
| 4 | **Dark mode (KkPN3 "dark editorial")** | Defer P2/P3 (#46) | **✅ DO-NOW (P4c)** | Now has a concrete, named palette (§0). `ThemeMode` plumbing already exists from P3 — P4c is mostly **replacing the derived dark palette with the KkPN3 values** + auditing every surface. Highest visual payoff per unit of work. |
| 5 | **Backup & restore settings (G4)** | Original H, untouched | **✅ DO-NOW (P4d)** | Cheap, self-contained (export/import a DataStore snapshot). Genuinely useful for testing the growing pref surface. No design dependency. |
| 6 | **MediaSession real music integration** | Defer P2 (#39) | **🟡 KEEP-DEFERRED** | Still **no playback spec** in the file (sweep: 0 hits). Real work = `MediaSessionManager` + notifications permission + the notification-listener pump. High cost, no design source, and the music row is cosmetic today. **Recommend defer past P4.** |
| 7 | **Live CalendarContract** | Defer P3 | **🟡 KEEP-DEFERRED** | Same class as weather (#7): needs `READ_CALENDAR`, permission-denied/no-calendar-app/multi-calendar edge cases. No design node. Low value for MVP. **Recommend defer past P4.** |
| 8 | **Live wallpaper engine** | Defer P3 (P3-6, #12) | **⚪ DROP (or park indefinitely)** | P3-6 already shipped "flat default + system picker", which is what a launcher of this style actually needs. A full live-wallpaper **engine** is a large, separate product with no design source. **Recommend dropping from the roadmap** (keep the system-picker path). |
| 9 | **Real gesture actions** (swipe-down / double-tap) | Defer P3 | **🟡 KEEP-DEFERRED** | Needs notification access / Device Admin (@15) — permission-gated, out of P4. Rows already show "Coming soon". Not in the user's P4 list; leave parked. |
| 10 | **Battery change callback stream** | Defer P3 | **⚪ DROP (low value)** | P2 re-reads on `ON_RESUME`, which is sufficient. A live stream adds a receiver for a value that barely changes. Not worth a phase. |

**Net:** P4 does **4** do-now workstreams (#1–2, #3, #4, #5) → split into
**P4a / P4b / P4c / P4d**. #6, #7, #9 stay deferred with reasons; #8, #10 are recommended
for **drop/park**.

---

## 2. Proposed sub-phase split

Each sub-phase below is its **own** full cycle (audit → spec → confirm → implement →
verify → docs), like P1…P3.5. They are ordered so earlier phases de-risk / enable later
ones, but they are **independent enough to be reordered or skipped by the user**.

| Phase | Name | Scope | Why this priority |
|---|---|---|---|
| **P4a** | **Drag & Drop** | Reorder home rows by long-press-drag; drag a drawer app onto a folder tile to assign. | The one item promised twice (P2 + P3-2) and explicitly decomposed into "one gesture subsystem". Highest expectation. Pure interaction layer over existing models/repos — no new design language. |
| **P4b** | **Edit Icon — rich editor** | From the P3 "Edit Icon" menu action: a real editor over the P3.5 hybrid renderer (choose pack drawable **or** category glyph; recolor; reset). | P3.5 was deliberately sequenced before P4 *for this*. Now unblocked. Depends on nothing from P4a. |
| **P4c** | **Dark Mode (KkPN3)** | Replace the derived warm-dark palette with the **KkPN3 named palette**; audit every surface (home, drawer, settings, context menu, folder popup, widgets, import sheet) under dark. | Highest visual payoff; `ThemeMode` plumbing already exists, so this is mostly palette + a QA sweep. Independent of P4a/P4b. |
| **P4d** | **Backup & Restore** | Export/import launcher prefs (rows, spacing, hidden apps, folders, notes, active pack ref, theme) as one file via SAF. | Small, self-contained, no design dependency. Last because it benefits from the pref surface being final after P4a–P4c settle. |

**Explicitly OUT of P4** (kept deferred / dropped — see §1): MediaSession (#6),
live calendar (#7), live-wallpaper engine (#8), real gesture actions (#9), battery
stream (#10).

**Why not do it all as one "P4"?** P1–P3 each bundled 2 subsystems and were already
large. P4's do-now list is 4 unrelated subsystems (gesture layer, editor UI, theming
sweep, persistence) with **no shared code**. Forcing them into one phase repeats the
"too big to review" problem the user flagged. Splitting lets each be reviewed, verified,
and (if desired) **stopped after any sub-phase**.

---

## 3. Carry-in deferrals (for the record)

These stay deferred after P4 and are **not** silently lost:

| Item | Status after P4 | Reason |
|---|---|---|
| MediaSession (#39) | 🟡 Deferred | No playback design source; high cost (MediaSessionManager + notification access). |
| Live calendar (`CalendarContract`) | 🟡 Deferred | Permission + edge-case heavy; no design node. |
| Real gesture actions (swipe-down / double-tap) | 🟡 Deferred | Needs notification access / Device Admin. |
| Live-wallpaper **engine** | ⚪ Recommended DROP | P3-6 "flat + system picker" already ships the useful part. |
| Battery change callback stream | ⚪ Recommended DROP | `ON_RESUME` re-read is sufficient. |

---

## 4. What P4 explicitly does NOT do

- **No `.pen` edits.** The file stays read-only; `KkPN3` is read as the dark source of truth.
- **No new visual language.** Drag/drop, editor, backup all reuse Warm Right Rail tokens.
- **No onboarding (H) unless the user asks.** The old P4 note said "H + polish"; there is
  **no onboarding mock** in the file (sweep: `onboarding`/`welcome`/`setup` = 0 hits), so
  H is **not** auto-included. If H is wanted, I'll audit + spec it separately.
- **No MediaSession, live calendar, live wallpaper, or gesture actions** (see §1).

---

## 5. Decisions needed from the user before any sub-phase starts

1. **Approve the split** (P4a…P4d) — or reorder / merge / drop any.
2. **Confirm the drops**: live-wallpaper engine + battery stream — drop from roadmap?
3. **Confirm the deferrals**: MediaSession + live calendar stay parked (past P4)?
4. **Onboarding (H)**: in or out of P4? (Currently proposed **out** — no design source.)
5. **Start order**: proceed with **P4a (Drag & Drop)** first on approval? (Recommended.)

**STOP — no implementation until these are answered.**
