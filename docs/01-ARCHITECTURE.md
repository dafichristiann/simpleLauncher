# 01 — Architecture

Native Android launcher built with **MVVM + Clean Architecture**, single Gradle
project with feature modules. P1 ships `home`, `iconpack`, `appdrawer`; `settings`
and `onboarding` exist as shells.

---

## Module map

```
menu/
├─ app/                     # Application class, DI wiring, navigation graph, MainActivity
├─ launcher/                # HomeActivity (HOME intent target) — manifest-only concern
├─ core/
│  ├─ designsystem/         # tokens, theme, reusable atoms (no feature logic)
│  ├─ common/               # Dispatchers, Result wrapper, extensions, constants
│  ├─ model/                # pure Kotlin domain models (no Android deps)
│  └─ data/                 # DataStore + Room sources, repository impls, mappers
└─ feature/
   ├─ home/                 # HomeScreen + pages, clock/weather widgets, dock, pager
   ├─ iconpack/             # appfilter parser, masking, import/preview/apply
   ├─ appdrawer/            # app grid, alphabetical index, search/filter
   ├─ settings/             # settings shell (stub in P1, full in P3)
   └─ onboarding/           # onboarding shell (stub in P1, full in P4)
```

> For P1 we may start with a **single `:app` module + packages** and split into
> Gradle modules incrementally, to keep early builds fast. The package structure
> above is the target; module split is a mechanical follow-up (see
> [09-DECISIONS-LOG](09-DECISIONS-LOG.md)).

---

## Layer model (per feature)

```
┌───────────────────────────────────────────────┐
│ UI  (Compose)                                  │
│  Screen composables + state hoisting           │
├───────────────────────────────────────────────┤
│ Presentation (ViewModel)                       │
│  UI state (StateFlow), user intents            │
├───────────────────────────────────────────────┤
│ Domain (UseCases + models)                     │
│  GetInstalledApps, ResolveIcon, ApplyIconPack…  │
├───────────────────────────────────────────────┤
│ Data (Repositories)                            │
│  interfaces in domain, impls in core:data      │
├───────────────────────────────────────────────┤
│ Sources                                        │
│  PackageManager · DataStore · Room · File/Zip  │
└───────────────────────────────────────────────┘
```

Dependency rule: **outer depends on inner**. `core:model` and domain interfaces
have **no Android imports**. Compose never touches `PackageManager` directly — it
always goes through a ViewModel → UseCase → Repository.

---

## Data flow (example: resolving an app icon)

```
HomeScreen
  └─ HomeViewModel.icons: StateFlow<List<AppIconUi>>
       └─ ResolveIconUseCase(app, iconPack, maskConfig)
            └─ IconRepository
                 ├─ IconPackSource        (appfilter.xml → drawable)
                 ├─ OverrideStore         (DataStore: user per-app overrides)
                 └─ IconMasker            (fallback: draw real icon into squircle)
```

The pipeline short-circuits: **user override → icon pack match → auto-mask
fallback**. See [08-ICONPACK-FORMAT](08-ICONPACK-FORMAT.md).

---

## Hilt modules

| Module | Provides |
|---|---|
| `AppModule` | `Application`, app-scoped `CoroutineScope` |
| `DispatcherModule` | `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` |
| `DataModule` | `DataStore<Preferences>`, Room database + DAOs |
| `RepositoryModule` | binds `IconRepository`, `AppRepository`, `PrefsRepository`, `FolderRepository`, `NotesRepository`, `DeviceStatusRepository` |
| `LauncherModule` | `PackageManager` wrapper, `AppWidgetHost` (P2+), `LauncherApps` |

> P2 added three `core:data` repositories (all DataStore- or system-API-backed):
> `NotesRepository` (DataStore key `quick_notes`), `FolderRepository` (DataStore,
> JSON via `FoldersCodec`), and `DeviceStatusRepository` (`BatteryManager` +
> `StatFs`, pure Android, no permission). Bindings live in `RepositoryModule`.

---

## Key components

### Launcher replacement (real, functional in P1)
- `launcher/HomeActivity` declared in the manifest with
  `<category android:name="android.intent.category.HOME" />` and `DEFAULT`.
- A "Set as default launcher" action fires the system home picker intent
  (`ACTION_MAIN` + `CATEGORY_HOME`) so the user can choose SOFT / HOME.
- `android:launchMode="singleTask"` + `stateNotifierEnabled` handling for HOME.

### Icon pack system (B)
- `IconPackParser` reads `appfilter.xml` + `drawable.xml` from a pack (zip/apk/assets).
- `IconMasker` renders the charcoal squircle + cream symbol for unsupported apps.
- `IconCache` (Coil) memoizes produced bitmaps; invalidated on pack change.

### Home (A) & Drawer (C)
- `home`: `HorizontalPager` of pages, page indicator (pill lines), grid composer,
  clock/weather widgets, mic search pill, swipe-up → drawer.
- `appdrawer`: `LazyVerticalGrid` 4 columns, `AlphabetIndex` rail, in-drawer search.

---

## P1 stubs (skeleton only — not yet functional)

| Area | Stub behavior in P1 |
|---|---|
| Default-launcher switch intent | Implemented, but picker flow is basic |
| Live wallpaper engine | No custom engine; flat color background only |
| KWGT-style system widgets | Static in-app Compose widgets; no AppWidgetHost |
| Settings panel | Read-only shell, real controls in P3 |
| Onboarding | Static empty-state shell, full flow in P4 |
| Backup & restore | Not implemented (P3) |

---

## Concurrency & performance targets

- Startup → first frame **< 1s**; app list load done off the main thread.
- Animations/composables targeted at **60fps**; no jank on scroll/drawer/page swipe.
- Heavy work (icon masking, app list scan) on `Dispatchers.IO`; results cached.
- Minimized background work; no leaked contexts (`LauncherApps`, not raw
  `Context` in long-lived objects).
