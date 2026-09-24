# 03 - Feature Map

Master table of every requested feature -> priority -> backing design node -> status.

**Status legend:** [ ] todo - [~] in progress - [x] done - **stub** = skeleton only

---

## Priority 1 - MVP (Home + Icon Pack + App Drawer)  ·  DONE (incl. P1.5)

> **P1.5** (icon-pack real decode) completed after the P1 checkpoint. B1-B3 and B5
> are now fully real; P1's placeholder glyph rendering was retired.

### A. Home Screen Core

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| A1 | Home screen replacement (default launcher) | `launcher/HomeActivity` (manifest `HOME`/`DEFAULT`) | [x] | verified `set-home-activity` on emulator |
| A2 | Customizable grid (columns, spacing, icon size) | *(legacy)* | [~] | `GridConfig` model kept; home is now a row list; UI editor in P3 |
| A3 | Minimal search bar + voice search | `Q1cGYj` `N7ICqS` | [~] | **REDESIGNED**: row with "find something" text + search icon; tapping enters SEARCH state; voice action still a stub |
| A4 | Multiple pages + custom page indicator (pill lines) | *(assumption)* | [x] | removed in favour of the single Warm Right Rail page |
| A5 | Gestures (swipe up -> drawer, swipe L/R -> page, double-tap -> lock) | *(behavior)* | [~] | swipe-up -> drawer done; others P3 |
| A6 | Clock on home | `IDSBb` / `cFKcs` | [x] | **REDESIGNED**: 58 normal, ls −2 |
| A7 | Date on home | `ApuhU` `KkRQg` `ftENm` | [x] | **REDESIGNED**: number 60 normal + spaced day/month |
| A8 | Weather row on home | `lLPZS` | [x] | **REDESIGNED**: "Current 8°C", 28 normal (static values) |
| A9 | **Right icon rail (8 shortcuts)** | `hrsLU` / `B6633e` | [x] | **NEW**: 72dp, `#D8C8B6`, line icons → default device apps |
| A10 | **Music player row** | `QTwqr`… `pvQO0` `d41sbp` | [x] | **NEW**: static UI (title/artist/album/controls/progress); MediaSession deferred |
| A11 | **3 interaction states (Idle/Search/Music)** | `Az7qs` `m9OlxQ` `T8AA1` | [x] | **NEW**: tap-row trigger, tokenized motion (`MotionTokens`) |

### B. Custom Icon Pack System

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| B1 | Import/apply icon pack (`appfilter.xml` + drawable) | `lk7jo`, card `X7SHLQ` | [x] | **P1.5 DONE**: real drawable decode (`IconPackDrawableLoader`); zip import flow + installed-pack query (`IconPackImporter`, `InstalledIconPackScanner`, `IconPackImportSheet`); progress + graceful failure |
| B2 | Preview icon before apply | `lk7jo` grid | [x] | **P1.5 DONE**: the live drawer/home grid *is* the preview - decoded drawables render in place |
| B3 | Auto-mask unknown apps (mask real icon) | `lk7jo` tile spec | [x] | **P1.5 DONE**: real icon composited into the squircle as a cream monochrome mark (`AutoMask`, `IconCompositor`, `IconBitmapProvider`) |
| B4 | Minimalist notification badge (cream dot, no count) | `lQywT` / `ha6OA` | [~] | `CreamBadgeDot` built; listener P3 |
| B5 | Icon glyph set (lucide line-art) | `lk7jo` symbols | [x] | 26 vector drawables; glyphs are now the *fallback* only, not the pack rendering |

### C. App Drawer

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| C1 | Separate drawer, swipe-up trigger, 4-column grid | `TpzL1` | [x] | **REDESIGNED**: tile 68 r21 (was 104 r30), bg `#DCCDBA` |
| C2 | Alphabetical index on right | `czxh4` | [x] | active `#B06F52`, idle `#81796D` |
| C3 | Search/filter apps inside drawer | `V7udUl` | [x] | **REDESIGNED**: h56 r28 `#E8DFD0` stroke `#C8B8A6` |
| C4 | Drawer header (label + "All apps") | `lasU6` | [x] | no eyebrow; "All apps" + "Icon pack" action |
| C5 | **Category nav (All/Communication/Entertainment/Tools)** | `B6gGM` | [x] | **NEW**: mapped from `ApplicationInfo.category` |
| C6 | **App labels under tiles** | `J3Lb4`… | [x] | **NEW**: 11pt `#3A3A3A` |

---

## Priority 2 - Widgets (E) + Folders (D)  ·  DONE (2026-09-25)

> **P2 is implemented under the Warm Right Rail design system** (not the retired P1
> grid-squircle/card language). The `.pen` has **no** widget/folder mock, so these are
> documented assumptions — see [`04` section I](04-ASSUMPTIONS.md) and the
> [P2 spec](superpowers/specs/2026-09-25-p2-widgets-and-folders-design.md). The old
> node IDs previously listed here (`u0a8RP`, `o0Fi0`, `nFk4u`) no longer exist.

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| E1 | Clock & date on home | `IDSBb`/`ApuhU` | [x] | (row, from the redesign) |
| E2 | Weather row on home | `lLPZS` | [x] | (static values, from the redesign) |
| E3 | **Calendar row** (big date + upcoming events) | *(assumption)* | [x] | `CalendarRowContent`; static/empty events — live `CalendarContract` **deferred** |
| E4 | **Battery/storage row** (thin progress bar) | *(assumption)* | [x] | `BatteryStorageRowContent`; **REAL** `BatteryManager` + `StatFs` values |
| E5 | **Quick notes row** (tap-to-expand editor) | *(assumption)* | [x] | `NotesRowContent`; **persisted in DataStore** (survives restart) |
| D1 | **Folder in the app drawer** (68 r21 + 2×2 preview) | *(assumption)* | [x] | `FolderTile`; folders live in the **drawer** (not the row home) |
| D2 | **Folder popup** (cream r24, editable title, add/remove apps) | *(assumption)* | [x] | `FolderPopupBody`; "New folder" + assign-from-popup |
| D3 | Live system widgets (AppWidgetHost) | - | **stub** | P2+ |

### P2 deferred (recorded, not removed from scope)

| Item | Status |
|---|---|
| Drag-and-drop app → folder | ⛔ deferred to **P3** (gesture subsystem) |
| Live calendar events (`CalendarContract` + `READ_CALENDAR`) | ⛔ deferred |
| Battery change callback stream (vs re-read on resume) | ⛔ deferred to **P3** |
| Real music playback / MediaSession | ⛔ deferred (still) |
| KkPN3 dark editorial mode | ⛔ deferred (still) |

---

## Priority 3 - System UI (F) + Settings (G)  ·  NOT STARTED

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| F1 | Status bar icon color follows theme (charcoal, transparent) | `ogMkZ` | [~] | light/dark appearance wired in theme |
| F2 | Nav bar: hide on gesture nav / minimal on button nav | - | [ ] | |
| F3 | Long-press menu near icon (App Info, Uninstall, Edit Icon, Shortcuts) | `Fzobx` / `pu2gg` | [ ] | anchored, not centered |
| G1 | Settings sections: Appearance / Widgets / Wallpaper / Gestures | `nFk4u` | **stub** | shell built (`SettingsStubActivity`) |
| G2 | Custom pill toggle (charcoal, not system default) | `UPa9N` | **stub** | full in P3 |
| G3 | Dark/light mode (warm dark palette) | *(assumption)* | [~] | tokens + theme ready |
| G4 | Backup & restore settings | - | [ ] | |

---

## Priority 4 - Onboarding (H) + Polish  ·  NOT STARTED

| ID | Feature | Design node | Status | Notes |
|---|---|---|---|---|
| H1 | Empty state / onboarding before icon pack applied | hero copy `OHvvM`, CTA `z1fmFx` | [ ] | full in P4 |
| H2 | Line-art illustration + heading + pill CTA | hero section | [ ] | copy: "A softer way to start the day." |
| H3 | Extra polish / animations | - | [~] | drawer slide/fade + reduced-motion aware |

---

## Cross-cutting requirements

| Area | Requirement | Status |
|---|---|---|
| Performance | startup <1s, 60fps, no jank | [~] | app list load off main thread; to measure |
| Battery | minimal background work, no leaks | [~] | no background services in P1 |
| Compatibility | API 26 -> latest | [x] | compileSdk 35 / minSdk 26 |
| Responsive | phone + tablet | [~] | drawer tiles adaptive; home grid column-driven |
| Testing | unit + Compose UI | [~] | 54 unit tests pass; 3 Compose UI smoke tests written |
| Error handling | graceful fallback on corrupt icon pack | [x] | `IconPackParseResult.Invalid` -> mask/system; **P1.5**: corrupt zip / missing drawable folder / unreadable icons all fail as `ImportProgress.Failed` without crashing |
| Accessibility | TalkBack + WCAG contrast | [~] | content descriptions added; contrast verified in docs/06 |
