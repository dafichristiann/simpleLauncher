# 00 — Design Source of Truth

> **File:** [`design/homeApp.pen`](../design/homeApp.pen) — a Pen (pen.dev) canvas, v2.18.
> It is the **single** design reference. No PNG/JPG mockups exist in the project.
> This file is treated as **read-only**; the app implements it, we do not edit it.

The `.pen` file is a **design spec sheet**, not a pixel-perfect screen spec. It
contains exact token values, component mockups, and behavioral notes.

> **2026-09-24 update:** the file was restructured into the **"Warm Right Rail"**
> system. The old frames (`bi8Au`, `lk7jo`, `IwpZU`) and their nodes are **gone**.
> This document reflects the new frames. See
> [`docs/superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md`](superpowers/specs/2026-09-24-warm-right-rail-redesign-design.md)
> for the full audit + design + phase plan.

---

## Top-level frames (current)

| Node ID | Name | Size | Role |
|---|---|---|---|
| `znb90` | Home Screen Mockup — Warm Right Rail | 390×720, r44, bg `#E8DFD0` | **Primary home mockup** |
| `L7ZAp` | Warm Home Screen — Full System Board | 1440×1180, r32, bg `#F1EBE1` | **Spec sheet** (4 panels) |
| `TpzL1` | Warm App Drawer — Unique Icon Grid | 430×860, r36, bg `#DCCDBA` | **App drawer** |
| `KkPN3` | Home Screen Mockup — Dark Editorial | 390×720, r44, bg `#18191A` | Dark reference only (not implemented) |

### `L7ZAp` board panels

| Panel | Label node(s) | Content |
|---|---|---|
| Final mockup | `RwVPc` | Warm Right Rail home (390×720) |
| 01 · Icon Language | `QeMXD` `u3sUQ` `X26hv` `QBhow` | "Quiet symbols, consistent weight." |
| 02 · Motion System | `JNFYC` `pBKHX` `X3DMZL` `Z5V7Y9` | "Small movement, clear purpose." + motion tokens |
| 03 · Interaction Storyboard | `YzfzQ` `hew89` `Az7qs` `m9OlxQ` `T8AA1` | IDLE / SEARCH / MUSIC |

---

## Reference nodes used per screen/component

| App component | Design node(s) | Notes |
|---|---|---|
| Home root | `znb90` / `RwVPc` | 390×720 r44 `#E8DFD0`; vertical list of rows, not a grid |
| Right rail | `hrsLU` / `B6633e` | 72 wide, fill `#D8C8B6`, pad 58/26, gap 22 |
| Rail icons (8) | `vsygX` `Ws2pl` `rU7nM` `ibX83` `q2W46j` `RKZW7` `o3McO3` `o1ALlt` | 20×20 lucide `#2B2B2B`: sparkles, circle-dot, message-circle, send, camera, wind, panel-left, phone |
| Time | `IDSBb` / `cFKcs` | "04:35", 58 normal, ls −2, `#2B2B2B` |
| Date number | `ApuhU` / `xFSYv` | "22", 60 normal, ls −2 |
| Date day/month | `KkRQg` `ftENm` | 9 bold / 9 normal |
| Row dividers | `MrrnQ` `RDJW1` `LKAGP` `FH9n4` | 318×1 `#D0C2B1` |
| Weather | `lLPZS` | "Current 8°C", 28 normal, `#3A3A3A` |
| Search row | `Q1cGYj` `N7ICqS` `obDAF` | placeholder 13 ls2 + search icon 24 + 6 dots |
| Music player | `QTwqr` `juzvC` `E7w0Vk` `mdsQP` `lAfW2` `x5SL5` `pvQO0` `d41sbp` | title 28; artist 10 bold `#8A5F43`; album 64 `#F5EFE6`; controls; progress |
| Icon library tile | `QBhow` + `E9T8qF`… | tile 68×68 r20 `#F5EFE6`, glyph 24; panel `#E4D7C7` |
| Motion tokens | `Z5V7Y9` + `Cvy3V` | card `#2B2B2B` r24; RAIL SLIDE 220 / SEARCH FADE 160 / MUSIC RISE 280 / ease cubic-bezier(0.2,0.8,0.2,1) |
| Storyboard | `Az7qs` `m9OlxQ` `T8AA1` | 250×300 `#E4D7C7` r24; IDLE / SEARCH / MUSIC |
| Drawer root | `TpzL1` | 430×860 r36 `#DCCDBA` |
| Category nav | `B6gGM` (+ underline `n0TeI`) | All / Communication / Entertainment / Tools |
| Drawer tile | `dBQmH`… | 68×68 **r21**, glyph 24 |
| Drawer label | `J3Lb4`… | 11 normal `#3A3A3A`, centered |
| Alphabet index | `czxh4` | active `#B06F52`, idle `#81796D` |
| Drawer search | `V7udUl` | 390×56 r28, fill `#E8DFD0`, stroke `#C8B8A6` |
| Gesture bar | `OGsws` | 98×4 r2 `#81796D` |

---

## Extracted design tokens (authoritative)

### Colors

| Token | Hex | Usage in design |
|---|---|---|
| `background` | `#EDE6D8` | app / canvas background |
| `surface` | `#E8DFD0` | phone screen bg, search pill fill |
| `card` | `#F6F0E7` | legacy cards |
| `tileWarm` | `#F5EFE6` | icon tile / album circle / drawer tile |
| `categoryWash` | `#E4D7C7` | icon library + storyboard card bg |
| `railBg` | `#D8C8B6` | right rail background |
| `divider` | `#D0C2B1` | 1px row divider |
| `drawerBg` | `#DCCDBA` | drawer screen bg |
| `drawerStroke` | `#C8B8A6` | drawer search pill border |
| `progressTrack` | `#B9AA98` | music progress track |
| `tileCharcoal` | `#2B2B2B` | rail icons, titles, primary button |
| `textTitle` | `#1A1A1A` | legacy headings |
| `textBody` | `#625B52` | paragraphs / date day |
| `textMuted` | `#81796D` | search placeholder / month / index idle |
| `statusText` | `#3A3A3A` | weather, drawer labels |
| `accent` | `#8A5F43` | artist, active underline, progress fill |
| `indexActive` | `#B06F52` | alphabet active letter |

### Typography (new design uses NORMAL weight for clock/date/rows)

| Role | Family | Size | Weight | ls |
|---|---|---|---|---|
| Clock | DM Sans | 58 | normal | −2 |
| Date number | DM Sans | 60 | normal | −2 |
| Row display (weather / music title) | DM Sans | 28 | normal | −0.5 |
| Search placeholder | DM Sans | 13 | normal | 2 |
| Tweak label (day) | DM Sans | 9 | bold | 1.5 |
| Tweak label lean (month/track) | DM Sans | 9 | normal | 1.2 |

### Motion (board `L7ZAp` → card `Z5V7Y9`)

```
RAIL SLIDE   220ms
SEARCH FADE  160ms
MUSIC RISE   280ms
EASE  cubic-bezier(0.2, 0.8, 0.2, 1)
```

Implemented in `core/designsystem/theme/MotionTokens.kt`.

---

## Behavioral notes (from storyboard `Az7qs` / `m9OlxQ` / `T8AA1`)

- **IDLE** — home at rest; clock + weather visible; rail minimal.
- **SEARCH** — rail expands; search row focused. (tap search row)
- **MUSIC** — player (music row) rises / grows in-place. (tap music row)
- Tap outside / Back returns to IDLE.
- Motion principle: "small movement, clear purpose" — soft fades, short slides,
  tactile scale changes; nothing competes with the information.

---

## P2 (widgets + folders) — no design nodes

Re-audited 2026-09-25: the `.pen` contains **no** clock/calendar/battery/storage/
quick-notes widget mock and **no** folder mock (the only `calendar`/`folder` keyword
hits are drawer app *tiles* and lucide icon names). P2 is therefore authored as
**documented assumptions** built from the existing Warm Right Rail patterns — see
[`04` section I](04-ASSUMPTIONS.md) and the
[P2 spec](superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md). The `.pen`
stays read-only.

- **Widgets** are additional full-width `HomeRow`s on the home list (calendar →
  battery/storage → quick notes), not cream cards.
- **Folders** live in the **app drawer** (the surviving grid surface), as a 68 r21
  tile with a 2×2 mini preview + a cream r24 popup.

---

## P3 (System UI + Settings) — no design nodes

Re-audited 2026-09-25: the `.pen` contains **no** status-bar treatment, nav-bar
handling, long-press menu, settings panel, or toggle mock (the only `status`/`nav`
hits are a `Drawer Status` mock snippet; `gesture` is the Gesture-Bar drawable only).
The five P3 node IDs previously cited in `docs/03` (`ogMkZ`, `Fzobx`, `pu2gg`,
`nFk4u`, `UPa9N`) are from the **retired P1 file** and do **not** exist. P3 is
authored as documented assumptions on the Warm Right Rail vocabulary — see
[`04` section J](04-ASSUMPTIONS.md) and the
[P3 spec](superpowers/specs/2026-09-25-p3-systemui-and-settings-design.md). The `.pen`
stays **read-only** (verified byte-identical after the P3 build).

- **Status/nav bar** follow the app theme (F1/F2); the bar icons themselves are
  system-drawn — we request light/dark icon appearance via `WindowInsetsControllerCompat`.
- **Long-press menus** (rail + drawer tile) are row-style cream r24 cards, **not** the
  platform `PopupMenu`.
- **Settings** is a sectioned panel (Appearance / Widgets / Wallpaper / Gestures) built
  from `SettingsRow`s + the `SoftToggle` pill.
