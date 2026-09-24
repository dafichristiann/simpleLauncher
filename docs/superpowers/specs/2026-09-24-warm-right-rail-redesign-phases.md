# Warm Right Rail â€” Phase Tracker

> Companion checklist to
> [`2026-09-24-warm-right-rail-redesign-design.md`](./2026-09-24-warm-right-rail-redesign-design.md).
> Tick items as they land. **Do not start a phase until the previous phase's
> exit criteria pass.**

**Legend:** `[ ]` todo Â· `[~]` in progress Â· `[x]` done Â· `[!]` blocked

> **Status: Phases 0-8 DONE (2026-09-24, Session 5).** 87 unit tests green (0 fail),
> debug APK builds, verified on emulator `soft_home_pixel`. Screenshots in
> `docs/screenshots/warmrp-*.png`. **Stopped for review before P2.**

**Design source of truth:** `.pen` frames `znb90` (home), `L7ZAp` (system board),
`TpzL1` (drawer). `KkPN3` = dark reference only.

---

## Phase 0 â€” Foundations: tokens + assets
- [x] Add color tokens: `SoftRailBg #D8C8B6`, `SoftDivider #D0C2B1`,
      `SoftTileWarm #F5EFE6`, `SoftCategoryWash #E4D7C7`, `SoftDrawerBg #DCCDBA`,
      `SoftDrawerStroke #C8B8A6`, `SoftProgressTrack #B9AA98`,
      `SoftIndexActive #B06F52`, `SoftRailBgDark #2E3134` (dark ref).
- [x] Add drawer tint fallbacks: `DrawerTintClay/Amber/Pine/Sage`.
- [x] Extend `SoftColors` (light + dark) with rail/divider/tileWarm/categoryWash/
      drawerBg/drawerStroke/progressTrack/indexActive.
- [x] Add dimens: `railWidth 72`, `railIcon 20`, `railPadTop 58`,
      `railPadBottom 26`, `homeRowPaddingX 42`, `dividerHeight 1`,
      `searchRowIcon 24`, `albumArt 64`, `musicControl 18`,
      `musicControlSmall 16`, `progressHeight 5`, `iconLibTile 68`,
      `drawerTileNew 68`, `drawerTileSymbol 24`.
- [x] Add spacing: `railGap 22`, `rowBand 164`.
- [x] Add typography: `clockLarge` (58 normal, ls âˆ’2), `dateNumber`
      (60 normal, ls âˆ’2), `rowDisplay` (28 normal, ls âˆ’0.5),
      `rowMeta` (13 normal, ls 2), `tweakLabel` (9 bold, ls 1.5),
      `tweakLabelLean` (9 normal, ls 1.2).
- [x] Create `MotionTokens.kt` (220 / 160 / 280 ms; WarmEase 0.2,0.8,0.2,1).
- [x] Add icon drawables: sparkles, circle-dot, send, wind, panel-left,
      skip-back, skip-forward, disc-3, tent, images, book-open, wallet-cards,
      shield, bot, contact, download, box, folder-symlink, folder-open,
      graduation-cap, heart-handshake, shopping-bag, messages-square, users,
      utensils, wallet.
- [x] Test: `MotionTokensTest` (durations + easing control points).
- **Exit:** module compiles; `MotionTokensTest` green; no screen changed.

## Phase 1 â€” Model + domain
- [x] Add `AppInfo.category` (`Int?`, mirrors `ApplicationInfo.category`);
      map in `AppRepository`.
- [x] Add drawer category enum + mapping logic (Social/Messageâ†’Communication,
      Audio/Video/Gameâ†’Entertainment, Productivity/Maps/News/Image/â€¦â†’Tools,
      nullâ†’All-only).
- [x] Add rail shortcut target resolution (dialer/camera/browser/messaging/email/
      settings via `Intent`; no-op on failure).
- [x] Tests: `DrawerCategoryTest`, `RailShortcutTest` (pure parts).
- **Exit:** logic tested; no UI changed.

## Phase 2 â€” Design-system components
- [x] New `core/designsystem/atom/HomeRows.kt`: `HomeRow`, `HomeDivider`
      (318Ã—1 `SoftDivider`), `RailIcon` (20dp, tint, click), `MusicPlayerRow`
      (static UI per design; track `#B9AA98`, fill `#8A5F43`, album 64 circle).
- [x] Keep reused atoms; list dead atoms for Phase 7 removal.
- [x] Test/preview renders each atom.
- **Exit:** atoms compile + render.

## Phase 3 â€” Home screen refactor (list + rail)
- [x] Rewrite `HomeScreen.kt`: `Box` â†’ `Row { rows + HomeRightRail }`.
- [x] Rows full-width, divider-separated (no cream cards): time â†’ date â†’
      weather â†’ search â†’ music.
- [x] Implement `HomeRightRail` (`RailIcon` Ã— 8, `SoftRailBg`, pad 58/26, gap 22).
- [x] Wire `MusicPlayerRow` (static).
- [x] Remove grid usage from home (grid stays in drawer).
- [x] Tests: update `HomeWidgetsTest`; add `HomeScreenTest`.
- **Exit:** home matches `znb90` at IDLE; UI tests green.

## Phase 4 â€” Interaction states + motion
- [x] Add `HomeState { Idle, Search, Music }` to `HomeViewModel` + intents.
- [x] Tap search row â†’ SEARCH; tap music row â†’ MUSIC; tap outside/Back â†’ IDLE.
- [x] Animate rail expand (`RAIL_SLIDE` 220ms), search focus (`SEARCH_FADE`
      160ms), music grow in-place (`MUSIC_RISE` 280ms); rows above stay put.
- [x] Respect reduced motion.
- [x] Tests: `HomeStateTest` + UI transition assertions.
- **Exit:** all 3 states reachable; animated only via tokens.

## Phase 5 â€” App drawer redesign
- [x] Root bg `SoftDrawerBg #DCCDBA`.
- [x] Tile 68 r21 + 11pt label under tile.
- [x] Category nav: All / Communication / Entertainment / Tools + active
      underline `#8A5F43`; wire filtering (Phase 1 logic).
- [x] Search pill h56 r28 `#E8DFD0`, stroke `#C8B8A6`.
- [x] Alphabet rail active `#B06F52`, idle `#81796D`.
- [x] Icons unchanged (icon pack pipeline; tints are optional fallback only).
- [x] Tests: `AppDrawerScreenTest`; update `AlphabetIndexTest` if needed.
- **Exit:** drawer matches `TpzL1`; tests green.

## Phase 6 â€” Verification on device
- [x] Build debug APK; install on emulator (Android 15 / API 35).
- [x] Set as default home; screenshot Home (IDLE/SEARCH/MUSIC) + Drawer.
- [x] Compare vs `znb90` / `L7ZAp` / `TpzL1`.
- [x] TalkBack pass; contrast check; reduced-motion check.
- [x] Save screenshots to `docs/screenshots/`.
- **Exit:** evidence captured; no crashes; no regressions.

## Phase 7 â€” Cleanup + docs
- [x] Remove dead legacy atoms/sizes (old mic pill if replaced; old
      `drawerIconTile`/`drawerIconSymbol` if unused).
- [x] Update docs `00` (rewrite traceability), `02` (tokens + motion),
      `03` (A9/A10/A11, A3, C1â€“C4), `04` (section H), `05` (Session 5),
      `09` (D-017â€¦D-021).
- **Exit:** docs consistent; build clean; all tests green.

## Phase 8 â€” Final review / handoff
- [x] Summarize changes kept vs changed, evidence, open items.
- [x] **STOP for user confirmation** before any further step.

---

## Decisions locked (from design dialogue)

| # | Decision |
|---|---|
| Q1 | Music player = static UI first; MediaSession deferred |
| Q2 | Home rows = 1px divider, not cream cards |
| Q3 | `KkPN3` (dark) = reference only, out of scope |
| Q4 | Drawer = full redesign per `TpzL1` |
| Q5 | Drawer categories mapped from `ApplicationInfo.category` |
| Q6 | Rail icons = shortcuts to default device apps |
| Q7 | Drawer trigger = swipe-up (unchanged) |
| Q8 | Drawer icons = icon pack pipeline (tints optional) |
| Q9 | States triggered by tapping rows |
| Q10 | MUSIC state = grow in-place; other rows stay |
| Q11 | Architecture = in-place refactor; iconpack logic untouched |
| Q12 | Tokens follow new `.pen` exactly (clock 58 normal, etc.) |

