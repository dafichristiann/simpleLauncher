# SOFT / HOME Motion Polish Design

**Status:** Implemented — verification follow-up (2026-09-25)  
**Scope:** Warm Right Rail motion system polish across widgets, rail, drawer,
overlays, settings, and theme transitions.

## Implementation status

The motion implementation is complete for the existing product surface:

- shared tokenized Warm press feedback is applied to home widgets, music controls,
  rail actions, drawer category tabs, settings actions, and toggles;
- notes/music expansion, widget progress, drag lift/reorder, drawer enter/exit,
  folder/context-menu/icon-editor overlays, and theme changes use `MotionTokens`;
- drawer pager and vertical scroll guards preserve scroll-vs-tap semantics;
- stable lazy-item keys and the existing icon cache remain in place.

Verification is complete on the Tecno device for build/install, drawer opening,
vertical scrolling, and the no-crash/no-accidental-launch path. A fresh `gfxinfo`
sample measured 174 rendered frames, 1 janky frame (0.57%), and 0 missed vsyncs.

The following are intentionally follow-up items rather than missing behavior:

- formal before/after screen recordings for every listed gesture still need to be
  captured as video;
- badge fade/scale remains pending until the app has a real badge data source;
- Settings section transition is not added because Settings is currently one scroll
  surface, with no section-navigation state to animate;
- broad first-decode profiling remains optional because the current Tecno sample is
  within the target frame budget.

## Intent and success criteria

The launcher should feel premium and smooth while preserving the existing small-
movement design language and all current gesture semantics. A tap must still launch,
a stationary long-press must still open its menu, and a long-press drag must still
reorder or assign a folder. New motion must stay within the existing 150–300 ms
family and use `MotionTokens` rather than component-local durations.

Success means:

- every interactive tile/row/icon has consistent Warm press feedback;
- widget expansion, row reorder, drawer/overlay transitions, and theme changes are
  visibly smooth but do not delay interaction;
- lazy content keeps stable keys and icon decoding does not block the render path;
- existing unit/instrumented behavior remains green;
- Tecno verification shows no meaningful frame drops during representative gestures.

## Motion foundation

Extend `MotionTokens` with named specs for press feedback, overlay enter/exit,
theme color interpolation, and drag settle/reorder. Keep the existing WarmEase and
durations; any new duration must be a named token in the 150–300 ms range.

Add a reusable design-system press modifier/indication that combines a subtle scale
down and opacity change while pressed with a Warm-colored ripple/indication. It must
share one `MutableInteractionSource` with clickable/combinedClickable so press state
does not compete with drag-source gesture detection.

## Component behavior

### Widgets and home rows

- Apply the shared press feedback to clickable home rows and music controls.
- Keep notes/music size animations on tokenized specs.
- Animate drag preview lift/elevation and row insertion/reorder movement using the
  existing drag tokens; preserve the current touch-slop and long-press guard.
- Animate music and battery/storage progress changes rather than snapping.
- Keep the home list's current simple layout because its row count is bounded; do not
  introduce a lazy container unless profiling demonstrates a real need.

### Right rail

- Apply shared press feedback to every rail icon and shortcut action.
- Keep the current rail slide behavior but route it through tokens consistently.
- Animate any selected/badge state with fade + scale when that state exists; do not
  invent persistent badges without a data source.

### Drawer and overlays

- Route drawer enter/exit through MotionTokens and combine vertical movement with a
  restrained fade/parallax.
- Animate folder popup, context menu, and icon editor card with scale/fade or
  slide/fade plus synchronized scrim opacity.
- Preserve existing pager, vertical-scroll, and scroll-vs-tap behavior.
- Retain stable keys for all lazy grids/lists and add any missing keys discovered in
  the implementation pass.

### Settings and theme

- Apply shared press feedback to SettingsRow, action pills, and SoftToggle while
  preserving the existing knob animation.
- Animate theme color changes through a tokenized transition; system-bar appearance
  changes remain side effects and must not delay composition.
- Avoid adding speculative section navigation animation where Settings is currently a
  single scroll surface; animate only stateful section content that actually changes.

## Performance and loading

Profile before optimizing broadly. Inspect recomposition and frame timing around
drawer opening, icon editor opening, and drag+scroll. The icon-pack loader's first
decode should be moved off the render-critical path only if profiling confirms a
jank risk; its existing LRU cache remains the source of truth.

## Verification

- Unit tests for MotionTokens and pure animation/state helpers.
- Compose tests for press semantics and functional callbacks (tap, long-press,
  drag/reorder, popup dismiss, theme toggle).
- Screen recordings before/after for notes expansion, row reorder, drawer open/close,
  folder popup, rail press feedback, and icon editor.
- On Tecno, collect frame timing with `dumpsys gfxinfo` around representative gestures;
  document any dropped-frame threshold and result.

## Rollout order

1. Motion foundation and press indication.
2. Widget rows and drag polish.
3. Rail feedback.
4. Drawer/overlay transitions.
5. Settings/theme and loading/performance follow-up.
6. Full regression, recordings, docs, and final review.

No persistence schema changes are planned.
