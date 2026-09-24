# 08 — Icon Pack Format & Masking

How SOFT / HOME finds, parses, resolves, and masks app icons to produce the uniform
monochrome "SOFT MONOCHROME" look from [`design/homeApp.pen`](../design/homeApp.pen)
(frame `lk7jo`).

---

## 1. Icon pack format (standard Android)

An icon pack is a zip (or installed apk / app assets) containing:

| File | Purpose |
|---|---|
| `appfilter.xml` | maps apps → drawable names |
| `drawable.xml` | declares drawable resources (optional, for apk packs) |
| `res/drawable-*/<name>.png` | the actual icon images |

### `appfilter.xml` schema

```xml
<resources>
  <!-- one entry per app -->
  <item component="ComponentInfo{com.example.app/com.example.app.MainActivity}"
        drawable="app_example"/>
  <!-- some packs also use drawable packages -->
  <item component="ComponentInfo{com.foo/.Main}" drawable="foo"/>
</resources>
```

Parser rules:
- `component` = `ComponentInfo{pkg/cls}` — cls may be **fully qualified** or
  **short** (`.Main`); normalize to `ComponentName`.
- `drawable` is a resource name resolved inside the pack.
- Ignore unknown tags; be lenient (packs vary wildly).

### `appfilter.xml` variants to handle

| Variant | Handling |
|---|---|
| Missing `appfilter.xml` | Treat pack as invalid → fall back to system icons + notify |
| Malformed XML | Catch, skip file, log, fall back per-app |
| `ComponentInfo` short class (`.Main`) | Expand with the package prefix |
| Duplicate entries | Last-write-wins (log a warning) |
| Non-`ComponentInfo` `component` values | Skip |

---

## 2. Icon resolution pipeline

Ordered, short-circuiting:

```
For each installed app (ComponentName):
  1. USER OVERRIDE      → if user set a custom icon for this app, use it
  2. ICON PACK MATCH    → if the active pack has an entry, decode + render its drawable
  3. AUTO-MASK FALLBACK → render the app's REAL icon into the SOFT squircle
  4. SYSTEM FALLBACK    → if masking fails, use the raw system icon
  5. GLYPH (last resort) → category lucide symbol if even that fails
```

```
IconRepository
├─ OverrideStore (DataStore)      # per-app icon overrides
├─ IconPackSource (active pack)   # appfilter → drawable
├─ IconMasker / AutoMask          # charcoal squircle + cream mark rules
├─ IconCompositor / IconBitmapProvider  # paint + cache the masked real icon
└─ IconCache (LRU)                # memoize produced bitmaps/drawables
```

### 2.1 Real drawable decode (P1.5, implemented)

`IconPackDrawableLoader` turns a pack entry's `drawable` name into a real
`android.graphics.drawable.Drawable`, per **source kind** (`IconPackSource`):

| Source | How the drawable is resolved |
|---|---|
| `Zip(path)` | Open the archive, find `res/drawable*/<name>.<ext>` (preferring the highest density bucket), `BitmapFactory.decodeStream` |
| `InstalledPack(pkg)` | `PackageManager.getResourcesForApplication(pkg)` → `res.getIdentifier(name, "drawable", pkg)` → `res.getDrawable(id)` |
| `Assets(dir)` | `assets/<dir>/<name>.png|webp|jpg` |

Decoded drawables are cached (LRU 256) and rendered at the **design's 0.49 symbol
ratio** inside the tile (see §3 / the `lk7jo` icon-set spec).

### 2.2 Import flows (P1.5, implemented)

| Flow | Entry point | What happens |
|---|---|---|
| **Zip import** | `IconPackImporter.importZip(stream)` | validate (zip + drawable folder + appfilter) → copy into `filesDir/iconpacks/` → parse → verify drawables → emit `ImportProgress` |
| **Installed pick** | `InstalledIconPackScanner.scan()` | query the standard pack intents, list packs, parse the chosen one's appfilter (assets or `res/xml`) |

`ImportProgress` is a `Flow` (Validating → Indexing* → Done/Failed) so a pack with
hundreds of drawables never blocks the frame.

---

## 3. Auto-mask (fallback for apps not in the pack)

Goal: every app — including newly installed ones — looks consistent even without
a pack entry.

> **P1.5 status: implemented.** `AutoMask` holds the pure rules (radius, icon inset
> 0.62, tint strength, luminance), `IconCompositor` paints the squircle + tinted icon,
> and `IconBitmapProvider` fetches the app's real icon from `PackageManager` and caches
> the result (LRU). If the real icon can't be loaded the tile falls back to the
> category glyph (mode b), so it is never blank.

Two masking modes:

### (a) Tile mask over the real icon  ← implemented (default)
- Draw a **charcoal squircle** (`#2B2B2B`, radius = **30%** of tile size).
- Composite the app's real icon, inset at **0.62** of the tile, **desaturated /
  tinted** to monochrome (luminance → cream `#E8DFD0`), OR
- Fall back to (b) if the real icon is unusable.

### (b) Symbol mask (generic)  ← implemented (last resort)
- Draw the charcoal squircle.
- Place a **lucide line-art symbol** in cream (`#E8DFD0`) chosen by app category
  heuristics (e.g. phone → `phone`, browser → `compass`, camera → `camera`).
- Default symbol when unknown: a generic `app-window` / `square`.

### Squircle radius math (matches design)
```
iconRadius(size) = size * 0.30     # 62 → 19, 104 → 30, 512 → 154
```
Implemented via `RoundedCornerShape(size * 0.30f)` or a true superellipse path if
needed for fidelity.

---

## 4. Notification badge (B4)

From node `ha6OA`:
- Cream dot (`#E8DFD0`), size ≈ **15%** of the tile, positioned top-right inset.
- **No red, no count.**
- Soft glow (`#F3EBDD`, blur 8) for legibility.
- Shown best-effort when the app has active notifications (requires notification
  access permission; degrade gracefully if unavailable).

---

## 5. Error handling (non-functional requirement)

| Failure | Behavior |
|---|---|
| Corrupt / invalid pack | Skip pack, revert to system icons, show a toast/snackbar |
| Incompatible pack (missing appfilter) | Mark pack "no appfilter", allow manual apply, use masks |
| Drawable missing for an entry | Fall through to mask, log once |
| OOM while masking many apps | Bound cache (LRU), mask lazily/on-demand, recycle bitmaps |
| Pack removed/uninstalled | Detect, clear active pack, re-resolve icons |

**Implemented in P1.5** (`IconPackImporter` returns `ImportProgress.Failed` for each;
the previous active pack is kept and the launcher falls back to auto-mask):

| Import failure | Result |
|---|---|
| File is not a readable zip | `Failed("Not a valid .zip archive")` |
| Zip has no drawable folder | `Failed("Missing a drawable folder - not an icon pack")` |
| `appfilter.xml` malformed / missing | Lenient parse; missing → `NoAppFilter` (masks), malformed → `Failed` |
| appfilter references no decodable drawable | `Failed("Pack has an appfilter but no readable icons")` |
| File unreadable / not found | `Failed("Could not read ...")` / `Failed("File not found")` |

None of these paths can crash the UI thread.

---

## 6. Caching & invalidation

- Produced icons cached in memory (LRU) + disk (Coil) keyed by
  `(pkg, componentName, activePackId, density, theme)`.
- Invalidate on: pack change, icon override change, theme (light/dark) change,
  `ACTION_PACKAGE_ADDED/REMOVED` broadcasts.
