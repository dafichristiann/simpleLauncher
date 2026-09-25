# Customize Sidebar Apps — Technical Design

## Scope and decision

This design adds a user-configurable right rail to SOFT / HOME without replacing
the existing P7 rail architecture, P8 motion, app drawer, system bars, or the
existing drag engine. A right-rail item may now be either an existing system
shortcut or a real installed application. `Settings` remains a permanent,
locked system item and is the entry point for customization.

The existing rail has eight visible positions and its dimensions, gaps, and
non-scrolling layout are design constraints. Therefore the capacity is:

| Kind | Limit | Reason |
| --- | ---: | --- |
| Total rail items | 8 | Preserves the current right-rail layout and spacing. |
| Locked `Settings` item | 1 | Always present, never removable or duplicated. |
| User-configurable items | 7 | `8 total - 1 locked Settings`. |

The configuration limit will be expressed by named constants, not scattered
literals. This deliberately chooses seven configurable slots over adding a
ninth item, scrolling, or resizing the rail. If a later visual redesign makes
nine visible positions intentional, only the total-capacity constant and its
tests need change.

## 1. Data model

`core:model` will own a small, UI-independent representation:

```kotlin
sealed interface RailItemId {
    val storageId: String

    data class System(val shortcut: RailShortcutId) : RailItemId {
        override val storageId = "system:${shortcut.name}"
    }

    data class App(val componentKey: String) : RailItemId {
        override val storageId = "app:$componentKey"
    }
}
```

`RailShortcutId` will be a core-model enum containing the existing stable
shortcut identities: `Sparkles`, `CircleDot`, `MessageCircle`, `Send`,
`Camera`, `Wind`, `PanelLeft`, and `Phone`. It keeps the model independent of
the UI module while `feature:home` maps it to the existing `RailShortcut`
rendering and resolver behavior. `PanelLeft` is the canonical ID for Settings.

The sealed interface is preferable to persisting a single untyped string in
every consumer: parsers and logic get exhaustive branches, UI code cannot
mistake an app key for a system shortcut, and persistence is still the compact,
stable string required by DataStore. Parsed app IDs require a non-blank component
key. Labels, icons, `Intent`s, and Compose state are intentionally not persisted.

`RailConfigLogic` replaces the *logic surface* of `RailOrderLogic` while keeping
a deprecated compatibility adapter during the migration. It owns:

- `DEFAULT_ITEMS`: the old visual default expressed as `system:` IDs;
- `LOCKED_SETTINGS`: `system:PanelLeft`;
- `MAX_TOTAL_ITEMS = 8` and `MAX_CONFIGURABLE_ITEMS = 7`;
- parsing, sanitizing, insertion, removal, move, reset, and available-item
  derivation rules.

`LauncherPrefs` gains `railItems: List<RailItemId>` as the canonical field.
The prior `railOrder: List<String>` will be removed from active consumers after
migration; any short-lived compatibility accessor delegates from `railItems` so
there is never a second mutable source of truth.

## 2. Rail order logic and migration

The current `RailOrderLogic.DEFAULT` becomes the default system-item sequence:

`system:Sparkles`, `system:CircleDot`, `system:MessageCircle`, `system:Send`,
`system:Camera`, `system:Wind`, `system:PanelLeft`, `system:Phone`.

`sanitize(raw)` has these deterministic rules:

1. Parse only valid `system:<known shortcut>` and `app:<non-blank component key>`
   IDs.
2. Preserve first occurrence/order; discard duplicates and invalid entries.
3. Keep no more than seven non-Settings entries.
4. Remove every supplied `system:PanelLeft`, then insert exactly one locked
   Settings item at its prior first position; append it if absent.
5. If the resulting list has no user-configurable entries because input is
   absent or wholly corrupt, use `DEFAULT_ITEMS`.

This keeps a valid partial custom rail partial rather than unexpectedly filling
it with defaults after a user intentionally removed items. Missing/invalid
Settings is the exception: it is always repaired.

Migration is one-way and lossless in behavior:

1. If new `rail_items_v2` exists, parse and sanitize it.
2. Otherwise read the legacy `rail_order` CSV. Each known old enum name becomes
   `system:<same name>`, preserving order exactly.
3. Sanitize and write the resulting v2 string once in the existing DataStore
   `onStart` migration path; retain the legacy key for one app version so an
   interrupted upgrade remains recoverable.
4. Later releases may remove the unused legacy key only after the compatibility
   window; it is not needed for normal reads once v2 is present.

There is one configuration list. Home reordering and settings edits both call
the same `RailConfigLogic` operation and `PrefsRepository.setRailItems`; neither
maintains a local order copy.

## 3. Persistence and backup

`PrefsRepository` receives a `RAIL_ITEMS_V2` `stringPreferencesKey` and:

```kotlin
suspend fun setRailItems(items: List<RailItemId>)
```

The encoded format is a comma-delimited list of storage IDs, for example:

```text
system:CircleDot,app:com.android.chrome/com.google.android.apps.chrome.Main,system:PanelLeft,app:com.whatsapp/.Main
```

Component keys are already the app repository's stable identity and contain no
comma under the project contract. The codec rejects malformed tokens rather than
throwing. Missing v2 data triggers legacy migration, and missing both uses the
default. Corrupt values are sanitized; wholly invalid values recover to default.
`system:PanelLeft` is always reinserted by sanitize.

`applyAll` must write `RAIL_ITEMS_V2` from `LauncherPrefs.railItems`; the backup
document/codec needs a versioned optional `railItems` field. On importing an
older backup, its legacy rail order is converted by the same migration helper.
Thus backup and live preference reads share one codec and recovery policy.

## 4. App resolver and rendered rail items

`AppRepository.getInstalledApps()` is the only installed-app source. Its
`AppInfo.componentKey` is used to create `RailItemId.App`; its current `label`
and icon fields are used for rendering. A label is display-only and is never an
identity or persistence value.

`HomeViewModel` loads/combines the installed-app snapshot with preferences once,
then exposes resolved rail display models. `SettingsViewModel` does the same
from its existing one-shot `appsFlow`. Neither queries PackageManager during a
Compose recomposition. `SettingsViewModel` refreshes its app snapshot on entry
and can refresh after lifecycle resume; this is a single bounded resolver query,
not a polling loop.

If an app is no longer installed, its persisted `app:` item remains valid data
but is omitted from the rendered rail and displayed in Settings as an
unavailable/missing entry with a remove action. It never launches or crashes.
Removing it deletes only the configuration entry. If reinstalled with the same
component key it becomes available again in its saved position. The App Drawer
continues to independently use its existing app list and is not filtered or
mutated by sidebar membership.

## 5. System shortcuts

The existing `RailShortcut` behavior remains intact. An adapter maps these
identities to `system:` IDs:

| Existing shortcut | Persisted ID |
| --- | --- |
| More | `system:Sparkles` |
| Web | `system:CircleDot` |
| Messages | `system:MessageCircle` |
| Mail | `system:Send` |
| Camera | `system:Camera` |
| Weather | `system:Wind` |
| Settings | `system:PanelLeft` (locked) |
| Phone | `system:Phone` |

The existing `RailShortcutResolver` still builds and resolves system intents;
only its input is adapted from `RailItemId.System`. Settings keeps its existing
in-app settings action rather than being resolved as a removable external app.

## 6. Settings UI

`SettingsPanel` gains a `Customize Sidebar` row. Selecting it opens a dedicated
Compose route/screen within the existing settings activity, rather than adding a
new activity or a second settings state holder. The screen uses the existing
warm background, cream cards, rounded corners, typography, spacing, and motion
tokens.

It contains:

1. **Current Rail** — ordered items, including a visible `Settings` row marked
   “Always available” with no remove affordance. Real apps use resolver labels
   and icons; system shortcuts use their existing label/icon mapping.
2. **Available Apps** — installed apps not currently configured; add buttons are
   disabled with concise “Sidebar is full” feedback when seven configurable
   items are already present. A local search field appears only when the
   installed-app count exceeds a small documented threshold (for example 20).
3. **Available System Shortcuts** — legacy system shortcuts not currently
   configured, excluding Settings, so the old rail concepts remain usable.
4. **Reset to default** — an inline, low-emphasis action that writes
   `DEFAULT_ITEMS` immediately; no large modal is required.

Add/remove mutate the canonical list immediately and animate the affected rows
with existing transform/opacity-oriented Compose motion in the 180–250 ms range.
There is no explicit Save button: every successful operation is persisted, so
Home and Settings stay real-time synchronized.

## 7. Reorder

The existing `rememberDragController`, `dragSource`, `dropTarget`, drop
indicator, drag preview, long-press threshold, haptics, and `MotionTokens`
reorder motion remain the only drag implementation. The current rail supplies
IDs such as `rail:system:Camera` or `rail:app:<componentKey>` and commits drops
by calling `RailConfigLogic.move` followed by `setRailItems`.

The settings Current Rail list reuses those same design-system drag primitives
with a settings-specific presentation layer only; it does not create a second
gesture controller or reorder algorithm. The locked Settings item may be moved
to a different position to preserve the existing flexible rail order, but cannot
be removed. If the product later requires a fixed Settings location, one
`isMovable` rule can lock it without changing persistence shape.

Because both surfaces collect `PrefsRepository.prefs`, a completed reorder is
written once and then reflected on both surfaces. Optimistic local visual state
is limited to the drag preview; canonical order remains repository-driven.

## 8. UX and safety rules

- Remove means “remove from this launcher rail”; it never uninstalls, hides, or
  changes an application in the App Drawer.
- Add only changes the rail configuration and uses no external launch action.
- Duplicate IDs are rejected by `RailConfigLogic`; Settings cannot appear twice.
- Invalid, unknown, duplicate, corrupt, or uninstalled entries cannot crash the
  rail. They are repaired, skipped from launch/render, or shown as removable
  unavailable configuration as described above.
- Settings is permanent, distinctively labelled as locked, and remains the
  guaranteed route to Customize Sidebar.
- The existing status-bar, home screen, app drawer, and P8 animation behaviors
  are out of scope and untouched.

## 9. State architecture

```text
AppRepository.getInstalledApps() ─┐
                                 ├─> HomeViewModel / SettingsViewModel
PrefsRepository.prefs.railItems ─┘        │
                                           ├─> resolved rail display items
                                           ├─> Home right rail
                                           └─> Customize Sidebar screen

Home drag / Settings add-remove-reorder
        └─> RailConfigLogic ─> PrefsRepository.setRailItems()
                                  └─> DataStore ─> both collectors update
```

DataStore's `LauncherPrefs.railItems` is the sole source of truth. ViewModels
derive display lists; they do not persist their own sidebar lists. App metadata
is a read-only resolver input and does not redefine the user's order.

## 10. Performance

- Reuse existing `AppRepository` and its `AppInfo` model; add no dependency.
- Query installed applications off the main thread once per settings entry or
  controlled resume, not per item or recomposition.
- Use `componentKey`/`storageId` as lazy-list and Compose item keys.
- Derive resolved rail entries with `combine`, `map`, and stable immutable lists;
  memoize resolver mapping by inputs.
- Render the small current rail normally; use `LazyColumn` for potentially large
  available-app lists.
- Keep icon decoding on the existing app/icon path and avoid eager new bitmap
  work. No continuous animation or polling is added.

## 11. Accessibility

- Every rendered rail item has a label-based content description, e.g. “Open
  Chrome” or “Open Phone shortcut.”
- Add/remove controls expose explicit verbs: “Add Chrome to sidebar” and “Remove
  Chrome from sidebar.”
- The locked Settings row communicates “Settings, always available” in text and
  semantics; its lock is not color-only.
- Reorder controls expose accessible move up/down actions as a fallback to
  long-press drag. Drag semantics include the item label and current position.
- Disabled add controls explain that the sidebar has reached its seven-item
  configurable limit.

## 12. Test plan

### Pure unit tests (`core:model`)

- Legacy order converts to the same ordered `system:` IDs.
- Default config has eight total entries and exactly one locked Settings entry.
- Invalid/corrupt, blank, duplicate, unknown, and missing-Settings data sanitize
  safely.
- Add respects the seven configurable-item limit and rejects duplicates.
- Remove refuses Settings but accepts system/app items.
- Move preserves all IDs and handles bounds.
- Reset returns the original system-shortcut default.

### Repository and backup tests (`core:data`)

- DataStore v2 read/write round trip.
- First read migrates legacy `rail_order` and preserves its sequence.
- No v2/legacy value uses default; corrupt v2 falls back safely.
- `applyAll` and backup export/import retain v2 items; legacy backup converts.

### ViewModel/UI tests

- Active list matches the Home rail from identical prefs.
- Add/remove/reorder immediately update derived Home and Settings state.
- Settings is visible, locked, unique, and cannot be removed.
- Uninstalled app is non-launchable/removable and never crashes rendering.
- App Drawer input/output remains unchanged when rail membership changes.
- Max-limit feedback and reset behavior are correct.
- Semantics expose add/remove/locked/move actions.

### Instrumented/device regression

- Upgrade an installation carrying old `rail_order` data.
- Reorder on Home then reopen Customize Sidebar; reverse the operation there.
- Restart the launcher and verify preserved order/membership.
- Verify system shortcuts still resolve, app entries launch their components, and
  Settings remains reachable.
- Verify existing rail long-press, drop animation, drawer, clock/weather P8
  motion, home screen, and system bar continue to work on the Tecno target.

## 13. File impact

### Modify

- `core/model/.../RailOrderLogic.kt` — evolve into compatible item/config logic
  or add a neighboring `RailConfigLogic.kt` while retaining an adapter.
- `core/model/.../LauncherPrefs.kt` — add canonical `railItems` preference.
- `core/data/.../PrefsRepository.kt` — v2 key, codec, migration, repository API,
  and atomic restore support.
- `core/model/.../BackupDocument.kt` and `core/data/.../BackupCodec.kt` — versioned
  rail configuration backup compatibility.
- `feature/home/.../RailShortcut.kt` — adapt old shortcut enum/resolver to core
  shortcut IDs without changing behavior.
- `feature/home/.../HomeViewModel.kt` and `HomeScreen.kt` — derive/render unified
  items and pass their stable IDs into the existing drag engine.
- `app/.../settings/SettingsViewModel.kt` and `SettingsPanel.kt` — expose and
  render the Customize Sidebar flow using the shared repository state.
- Existing targeted unit/UI/instrumented test files and resources for labels.

### Create

- `core/model/.../RailItemId.kt` (and, if clearer, `RailConfigLogic.kt`) — pure
  ID/parser/sanitization/mutation rules.
- `app/.../settings/CustomizeSidebarScreen.kt` — focused Compose screen and only
  its UI presentation helpers/tests.
- Focused tests for the model, migration, repository, ViewModel, and UI flow.

### Do not touch

- App Drawer screen/view-model behavior and app-list ownership.
- Existing status-bar/system-bar implementation.
- P8 motion implementations unrelated to the right rail.
- Existing home-row drag behavior, icon-pack behavior, and unrelated settings.

## 14. Safe implementation order

1. Add pure `RailItemId`/configuration logic and unit tests.
2. Add `LauncherPrefs` v2 field, DataStore codec/API, legacy migration, backup
   compatibility, and repository tests.
3. Adapt system-shortcut mapping and resolve unified display models in the Home
   ViewModel; retain current default behavior.
4. Change the Home rail renderer to consume unified IDs while reusing its exact
   drag controller and motion primitives; add regression tests.
5. Add Settings ViewModel derivation and `Customize Sidebar` navigation/screen.
6. Add add/remove/reset/reorder UI, semantics, capacity feedback, and UI tests.
7. Run full unit, lint, build, and targeted instrumented/device regression tests
   before installation on the Tecno device.

No implementation work is authorized by this document alone. It is a review
artifact; implementation begins only after the user approves this written design
and the corresponding implementation plan.
