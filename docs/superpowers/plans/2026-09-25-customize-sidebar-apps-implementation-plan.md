# Customize Sidebar Apps — Implementation Plan

> Prerequisite: follow the approved [technical design](../specs/2026-09-25-customize-sidebar-apps-design.md).
> This plan deliberately excludes App Drawer, status bar, unrelated P8 motion,
> and changes to the existing drag gesture engine.

## Success criteria

- A rail holds at most eight entries: seven configurable entries and one locked
  `Settings` shortcut.
- Entries can be real apps (`app:<componentKey>`) or existing system shortcuts
  (`system:<shortcut>`).
- Settings and Home consume the same DataStore-backed order; all edits persist
  immediately and remain after restart.
- Legacy `rail_order` users retain their existing order as system entries.
- Existing rail reorder motion/gesture and existing system shortcuts keep working.

## Phase 1 — Pure model and migration rules

### 1. Add stable rail identities

Create `core/model/src/main/kotlin/com/softhome/core/model/RailItemId.kt`.

- Define `RailShortcutId` for each current `RailShortcut` enum identity.
- Define sealed `RailItemId` with `System` and `App` variants plus `storageId`.
- Add `RailItemIdCodec.parse(storageId)`; reject blank app component keys,
  unknown shortcut names, and malformed prefixes.
- Keep this module Android/Compose-free.

### 2. Evolve rail logic without two state lists

Replace the implementation internals of
`core/model/src/main/kotlin/com/softhome/core/model/RailOrderLogic.kt`, or add a
neighboring `RailConfigLogic.kt` and leave a small adapter for old callers.

- Define `DEFAULT_ITEMS`, `LOCKED_SETTINGS`, `MAX_TOTAL_ITEMS = 8`, and
  `MAX_CONFIGURABLE_ITEMS = 7` in one place.
- Implement `sanitize`, `add`, `remove`, `move`, `reset`, and
  `fromLegacyShortcutNames` as pure functions.
- `sanitize` deduplicates by storage ID, caps configurable items, repairs one
  Settings entry, and falls back only for missing/wholly invalid input.
- `remove` must be a no-op for `LOCKED_SETTINGS`.
- Preserve a compatibility `DEFAULT`, `sanitize(List<String>)`, and `move` only
  until production callers have migrated, with tests demonstrating equivalent
  legacy behavior.

### 3. Test the pure contract first

Create/update `core/model/src/test/.../RailItemIdTest.kt` and
`RailOrderLogicTest.kt`.

- Parse/format system and app IDs.
- Verify default = eight total / one Settings / seven configurable.
- Verify legacy conversion preserves order.
- Verify Settings restoration, duplicate removal, malformed input recovery,
  capacity, remove-lock, reorder bounds, and reset.

**Checkpoint:** run `./gradlew :core:model:test`.

## Phase 2 — DataStore and backup compatibility

### 4. Make the new configuration canonical

Modify `core/model/.../LauncherPrefs.kt`.

- Add `railItems: List<RailItemId>` as the canonical preference.
- Temporarily retain `railOrder` only as a derived/deprecated compatibility view
  if required by phased call-site migration; it must never be independently
  persisted or mutable.

### 5. Add v2 preference storage and one-time migration

Modify `core/data/.../PrefsRepository.kt`.

- Add `RAIL_ITEMS_V2` (`rail_items_v2`) and `setRailItems(items)`.
- Add a pure `RailItemsCodec` used by every read/write path.
- In the existing `onStart` migration flow, prefer a valid v2 value; otherwise
  convert old `RAIL_ORDER` shortcut names with `fromLegacyShortcutNames` and
  write v2 atomically once.
- Read corrupt/invalid values through `sanitize`; restore Settings automatically.
- Extend `applyAll` to write the canonical v2 value.
- Do not remove the old key in this change; it is fallback protection for
  interrupted upgrades.

### 6. Carry v2 through backups

Modify `core/model/.../BackupDocument.kt` and
`core/data/.../BackupCodec.kt`.

- Add a backward-compatible optional rail-items document property.
- Decode older backups using their old rail-order field through the shared
  conversion helper.
- Encode current backups with v2 storage IDs only.

### 7. Test persistence and restore

Modify/add `core/data/src/test/.../PrefsRepositoryTest.kt` and
`BackupCodecTest.kt`.

- Assert legacy DataStore migration, v2 round-trip, corrupt fallback, Settings
  repair, and atomic `applyAll` retention.
- Assert old and new backup decode compatibility.

**Checkpoint:** run `./gradlew :core:data:test`.

## Phase 3 — Home rail integration

### 8. Adapt current system shortcuts

Modify `feature/home/.../RailShortcut.kt`.

- Map `RailShortcutId` to the existing shortcut enum/resolver and preserve all
  current intents/labels/icons.
- Keep `PanelLeft` routed to the launcher Settings panel; it is not an external
  app resolver entry.

### 9. Derive display data in the view model

Modify `feature/home/.../HomeViewModel.kt`.

- Combine `prefs.railItems` with the existing installed-app repository result.
- Expose immutable resolved items: stable ID, label, icon source, launch target,
  availability, and lock/movability metadata.
- Refresh installed-app metadata through the existing repository boundary, never
  from a composable.
- Preserve an uninstalled app ID in preferences but omit it from Home rendering;
  it stays visible as removable in settings.
- Make all Home reorder commits call `RailConfigLogic.move` and
  `PrefsRepository.setRailItems`.

### 10. Swap the rail renderer's input only

Modify `feature/home/.../HomeScreen.kt`.

- Render resolved unified rail items, using `storageId` in stable Compose keys
  and existing `rail:` drag IDs.
- Reuse the exact current `rememberDragController`, `dragSource`, `dropTarget`,
  preview, haptic, drop indicator, and `MotionTokens.dragReorder()` code path.
- Add app-launch behavior using the resolved app intent; system behavior remains
  delegated to the existing resolver adapter.
- Preserve width, gaps, colors, status-bar composition, and P8 behavior.

### 11. Test Home regressions

Update feature-home unit/UI tests and targeted app instrumented tests.

- Existing legacy default still renders the eight system entries.
- App entry launches only its resolved component.
- Home reorder produces expected shared configuration.
- Missing app has no unsafe launch path.
- Existing rail long press/drop behavior remains covered.

**Checkpoint:** run `./gradlew :feature:home:test :app:assembleDebug`.

## Phase 4 — Customize Sidebar settings experience

### 12. Derive Settings screen state from shared inputs

Modify `app/.../settings/SettingsViewModel.kt`.

- Extend the existing installed-app flow instead of adding a repository.
- Combine installed apps + `prefs.railItems` into current rail, available apps,
  and available system-shortcut models.
- Add `addRailItem`, `removeRailItem`, `moveRailItem`, and `resetRailItems`;
  every one invokes the Phase 1 logic then `setRailItems`.
- Provide a transient “Sidebar is full” event when seven configurable entries
  already exist.
- Expose accessibility-ready labels/actions in screen models, not ad-hoc text
  from multiple composables.

### 13. Add the screen and navigation entry

Create `app/.../settings/CustomizeSidebarScreen.kt` and modify
`app/.../settings/SettingsPanel.kt`.

- Add a `Customize Sidebar` row within the existing settings panel.
- Use the existing settings navigation pattern/activity rather than a new
  activity or persistent screen state.
- Render Current Rail, Available Apps, Available System Shortcuts, and Reset.
- Use `LazyColumn` for installed apps; show optional local search only above the
  selected app-count threshold.
- Clearly label Settings “Always available”; provide no remove action and no
  duplicate add option.
- Use current warm design-system colors, spacing, press handling, rounded cards,
  and 180–250ms add/remove transitions.

### 14. Reuse drag primitives in settings

- Wire Current Rail rows to the same design-system `dragSource`/`dropTarget`
  modifiers and controller behavior already used by Home.
- Reuse the same pure `move` operation and drop semantics; do not implement a
  second gesture recognizer or a separate settings order.
- Expose move-up/move-down semantics for accessibility.

### 15. Test Settings interaction

Create/update settings unit/Compose tests.

- Active/available partition is correct.
- Add, remove, reset, maximum feedback, duplicate prevention, and locked
  Settings behavior are correct.
- Semantics include add/remove/locked/move descriptions.
- The same prefs emission produces matching Settings and Home order.

**Checkpoint:** run focused app tests, then `./gradlew :app:assembleDebug`.

## Phase 5 — Quality gates and device verification

### 16. Static and test verification

Run, in order:

```powershell
./gradlew :core:model:test :core:data:test
./gradlew :feature:home:test :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Run targeted connected tests when the Tecno device is authorized:

```powershell
./gradlew :app:connectedDebugAndroidTest
```

If the full connected suite is impractical, run the new targeted test classes
and report the limitation explicitly rather than claiming the suite passed.

### 17. Manual Tecno acceptance pass

1. Install the debug build with ADB as an update.
2. Confirm old rail order is retained on an app instance with legacy data.
3. Open Settings → Customize Sidebar; add/remove/reorder apps and shortcuts.
4. Confirm Settings stays present and unique; confirm capacity feedback at seven
   configurable items.
5. Return Home after each edit; verify immediate order/membership changes.
6. Restart launcher; verify persistence.
7. Uninstall or temporarily simulate a configured app absence; verify safe
   missing-item treatment and removal.
8. Verify App Drawer, Home rows, drawer gesture, clock/weather P8 motion, and
   system bars are unaffected.

## Non-goals / guardrails

- Do not alter App Drawer membership, search, or app repository ownership.
- Do not alter status bar, system bar, launcher role, or unrelated P8 motion.
- Do not change rail geometry to fit a ninth item.
- Do not add a heavy dependency, duplicated DataStore key, duplicate state list,
  or a second drag handler.
- Do not delete legacy system shortcuts; they remain selectable entries.

## Completion report contents

Report the changed/new files, final data format/migration behavior, shared-state
flow, reused drag components, motion/accessibility choices, exact test/lint/build
results, device verification result, and any limitation (especially connected
test availability).
