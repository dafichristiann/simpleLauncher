# SOFT / HOME

A warm, minimal, monochrome Android **home screen replacement (launcher)** — built
native with **Kotlin + Jetpack Compose**, designed around quiet hierarchy, tactile
depth, and icons that feel considered, never loud.

> Design direction: *"A softer way to start the day."*

---

## Status

| Priority | Scope | Status |
|---|---|---|
| **P1** | Home core (A) + Custom icon pack (B) + App drawer (C) → installable MVP, can be set as default launcher | ✅ Done (incl. P1.5 real icon decode) |
| **P2** | Custom widgets (E) + Folder system (D) | ✅ Done (2026-09-25) |
| P3 | System UI integration (F) + Settings panel (G) | ⬜ Not started |
| P4 | Onboarding (H) + polish / animation | ⬜ Not started |

Legend: ⬜ todo · 🟡 in progress · ✅ done · **stub** = skeleton only (logic not yet functional)

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Kotlin (native Android) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (KSP) |
| Local storage | DataStore (prefs) + Room (folders / notes) |
| Images | Coil |
| Widgets | Android AppWidgetProvider + Glance *(P2)* |
| Min SDK | API 26 (Android 8.0) |

**Why native Kotlin instead of Flutter/React Native:** a launcher (replace-home
intent, system icon replacement, widgets) needs deep, stable access to Android APIs
(PackageManager, AppWidgetHost, ResolveInfo) that is more reliable and performant
in native code than via cross-platform bridges.

---

## Design Source of Truth

The **only** design reference is [`design/homeApp.pen`](design/homeApp.pen) — a Pen
canvas containing a concept guide, a monochrome icon set, and launcher component
mockups. All colors, radii, shadows, spacing, and typography are extracted from it.
See [`docs/00-DESIGN-SOURCE.md`](docs/00-DESIGN-SOURCE.md).

> The `.pen` file is **read-only reference**. It is not edited by this project.

---

## Documentation Index

| Doc | What it covers |
|---|---|
| [`docs/00-DESIGN-SOURCE.md`](docs/00-DESIGN-SOURCE.md) | `.pen` analysis + every extracted token + node IDs |
| [`docs/01-ARCHITECTURE.md`](docs/01-ARCHITECTURE.md) | Module map, Clean/MVVM layering, DI graph, data flow |
| [`docs/02-DESIGN-SYSTEM.md`](docs/02-DESIGN-SYSTEM.md) | Design value → token → Kotlin mapping (light + dark) |
| [`docs/03-FEATURE-MAP.md`](docs/03-FEATURE-MAP.md) | Features A–H ↔ priority ↔ design node ↔ status |
| [`docs/04-ASSUMPTIONS.md`](docs/04-ASSUMPTIONS.md) | Every design gap + assumption taken (+ risk) |
| [`docs/05-PROGRESS.md`](docs/05-PROGRESS.md) | Living changelog per priority |
| [`docs/06-TESTING.md`](docs/06-TESTING.md) | Test strategy, coverage, how to run |
| [`docs/07-BUILD-AND-RUN.md`](docs/07-BUILD-AND-RUN.md) | Toolchain setup, build, emulator, set-as-default |
| [`docs/08-ICONPACK-FORMAT.md`](docs/08-ICONPACK-FORMAT.md) | appfilter.xml spec + masking pipeline + fallback |
| [`docs/09-DECISIONS-LOG.md`](docs/09-DECISIONS-LOG.md) | Short ADR-style decision entries |

---

## Quick Start

> ⚠️ Toolchain is **not yet installed** on this machine. Full steps in
> [`docs/07-BUILD-AND-RUN.md`](docs/07-BUILD-AND-RUN.md).

```powershell
# 1. Install JDK 17 + Android Studio (SDK goes to D:\ to save C: space)
winget install --id EclipseAdoptium.Temurin.17.JDK
winget install --id Google.AndroidStudio

# 2. Build a debug APK
./gradlew :app:assembleDebug

# 3. Install on a connected device / emulator
./gradlew :app:installDebug
```

Then on the device: **Settings → Apps → Default apps → Home app → SOFT / HOME**.

---

## Repository Layout (target)

```
menu/
├─ design/homeApp.pen        # design source of truth (read-only)
├─ docs/                     # this documentation set
├─ app/                      # Application, MainActivity/HomeActivity, DI, nav
├─ core/
│  ├─ designsystem/          # tokens, theme, reusable atoms
│  ├─ common/                # dispatchers, Result, extensions
│  ├─ model/                 # AppInfo, IconPack, Folder, GridConfig
│  └─ data/                  # DataStore + Room sources
├─ feature/
│  ├─ home/                  # home pages, clock+weather, dock
│  ├─ iconpack/              # parse / mask / apply icon packs
│  ├─ appdrawer/             # grid, alphabetical index, search
│  ├─ settings/              # settings shell (P1 stub)
│  └─ onboarding/            # onboarding shell (P1 stub)
└─ launcher/                 # HomeActivity as HOME intent target
```
