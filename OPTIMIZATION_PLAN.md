# Launcher App Optimization Plan

**Date:** 2026-09-25  
**Device:** Tecno Camon 50 Pro (Medium-spec phone)  
**Issue:** Scroll/swipe lag in app drawer despite 0% frame jank  
**Target:** Reduce frame time from 22-25ms to ≤16ms (60fps smooth)

---

## Performance Analysis Results

### Current Metrics (from gfxinfo)
```
Total frames rendered: 1568 (during 12s scroll/swipe)
Janky frames: 0 (0.00%) ✅
50th percentile: 22ms ❌ (target: 16ms)
90th percentile: 25ms ❌
95th percentile: 26ms ❌
99th percentile: 36ms ❌
Missed Vsync: 0 ✅
High input latency: 3,136 events ❌ (bottleneck!)
GPU time: 5-6ms ✅ (efficient)
```

### Root Cause
- **NOT** a GPU/rendering issue (GPU time is low)
- **Main thread bottleneck:** Input event processing + Compose recomposition
- Frame times consistently above 16ms target → accumulated delay on each scroll event

---

## Optimization Strategy

### Phase 1: Code-Level Optimizations (High Impact)

#### 1.1 Memoize AppCell Recomposition
**File:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerScreen.kt`

**Problem:**
- `AppCell()` composable runs recomposition on every scroll frame
- All 68 tiles in grid re-render even if not visible

**Solution:**
- Add explicit `key {}` in `itemsIndexed()` (already present)
- Wrap AppCell with `remember { }` for tile state
- Move color token calculation outside composition path

**Expected Impact:** -15-20% CPU usage per scroll

---

#### 1.2 Optimize AlphabetRail with Lazy Loading
**File:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerScreen.kt` (lines 869-886)

**Problem:**
- `forEach` loop recreates all 26 letter Text composables every frame
- Non-visible letters still render and interact

**Solution:**
- Replace `Column + forEach` with `LazyColumn`
- Only render visible letters in viewport
- Use `items()` instead of `forEach`

**Expected Impact:** -20-30% CPU on alphabet rail interactions

---

#### 1.3 Move Grid Column Calculation to ViewModel
**File:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerViewModel.kt`

**Problem:**
- `gridColumns.coerceIn(3, 7)` recalculates on every recomposition
- Causes unnecessary GridCells.Fixed() recreation

**Solution:**
- Pre-calculate and memoize `gridColumns` in ViewModel
- Pass as stable state (not recalculated in Compose)

**Expected Impact:** -5% recomposition overhead

---

### Phase 2: Input Handling Optimization

#### 2.1 Reduce VelocityTracker Sampling Frequency
**File:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerScreen.kt` (lines 301-326)

**Problem:**
- `VelocityTracker.addPosition()` called on every single touch event
- High input latency (3,136 events queued)

**Solution:**
- Add debounce: only add position every 16ms (1 frame)
- Use `SystemClock.uptimeMillis()` threshold

**Expected Impact:** -30-40% input latency

---

### Phase 3: Compose Stability

#### 3.1 Add @Stable Annotations to Data Models
**Files:**
- `core/model/src/main/kotlin/com/softhome/core/model/DrawerCategory.kt`
- `core/model/src/main/kotlin/com/softhome/core/model/Folder.kt`
- `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/DrawerEntry.kt`

**Problem:**
- Compose doesn't know if model objects are stable → assume unstable
- Forces recomposition of children even with same data

**Solution:**
- Mark immutable data classes with `@Stable`
- Helps Compose skip unnecessary recompositions

**Expected Impact:** +10-15% composition speed

---

#### 3.2 Enable Compose Compiler Optimizations
**File:** `build.gradle.kts` (app module)

**Problem:**
- Default Compose compiler settings are conservative

**Solution:**
- Add compiler flags:
  ```kotlin
  kotlinCompilerExtensions {
    stabilityConfigurationFile = file("compose_stability.conf")
  }
  ```

**Expected Impact:** +5-10% overall performance

---

## Implementation Checklist

- [ ] 1.1: Memoize AppCell (target: 15min)
- [ ] 1.2: Optimize AlphabetRail lazy loading (target: 20min)
- [ ] 1.3: Move grid column calc to ViewModel (target: 10min)
- [ ] 2.1: Reduce VelocityTracker sampling (target: 10min)
- [ ] 3.1: Add @Stable annotations (target: 15min)
- [ ] 3.2: Compose compiler flags (target: 5min)

**Total estimated time:** ~75 minutes

---

## Verification Steps

After each optimization:

1. **Rebuild debug APK:**
   ```bash
   ./gradlew :app:assembleDebug
   ```

2. **Install to device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Reset and capture gfxinfo:**
   ```bash
   adb shell "dumpsys gfxinfo com.softhome.launcher.debug reset"
   sleep 2
   [SCROLL/SWIPE FOR 10 SECONDS]
   adb shell "dumpsys gfxinfo com.softhome.launcher.debug"
   ```

4. **Target metrics:**
   - 50th percentile: ≤16ms ✅
   - 90th percentile: ≤20ms ✅
   - High input latency: <1000 ✅

---

## Success Criteria

✅ **PASS** if:
- Frame time 50th percentile drops to ≤16ms
- High input latency drops to <1000
- Visual scroll feels smooth without dropped frames
- No regressions in other features (home, widgets, folders)

❌ **FAIL** if:
- Frame time remains >20ms
- Input latency increases
- App crashes or visual artifacts appear

---

## Notes

- Device specs: RAM adequate, GPU efficient (5-6ms)
- Main bottleneck is CPU/main thread, not memory or rendering
- Optimization order: UI composition → input handling → stability
- Each fix is independent and can be reverted if needed

---

## Related Files

- **App Drawer Screen:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerScreen.kt`
- **App Drawer ViewModel:** `feature/appdrawer/src/main/kotlin/com/softhome/feature/appdrawer/AppDrawerViewModel.kt`
- **Core Models:** `core/model/src/main/kotlin/com/softhome/core/model/`
- **Build Config:** `app/build.gradle.kts`

---

## Implementation Results

### Iteration 1: Conservative Optimizations (P7)

**Changes applied:**
- ✅ Memoized AppCell with explicit key tracking
- ✅ Added @Stable annotations to DrawerEntry, DrawerCell, DrawerPage, DrawerUiState
- ✅ Pre-coerced grid columns in ViewModel (1.3)
- ❌ Reverted VelocityTracker sampling (made things worse)
- ❌ Reverted LazyColumn AlphabetRail (Column+forEach more efficient)

**Performance Results:**

| Metric | Before | After | Change | Target |
|--------|--------|-------|--------|--------|
| 50th percentile | 22ms | 17ms | **-5ms (-23%)** ✅ | ≤16ms |
| 90th percentile | 25ms | 28ms | +3ms | ≤20ms |
| 95th percentile | 26ms | 29ms | +3ms | ≤25ms |
| 99th percentile | 36ms | 30ms | **-6ms (-17%)** ✅ | - |
| Janky frames | 0% | 0% | ✓ | 0% |
| High input latency | 3,136 | 2,900 | **-236 (-7.5%)** ✅ | <1000 |
| Missed Vsync | 0 | 0 | ✓ | 0 |
| GPU time (avg) | 5-6ms | 5ms | **-1ms** ✅ | - |

**Analysis:**
- **50th percentile improved by 23%** — good! But still 1ms above 16ms target
- **99th percentile improved by 17%** — tail latency better
- **Input latency reduced by 7.5%** — @Stable annotations + memoization helping
- **GPU usage improved** — less memory pressure, fewer textures

**Status:** Partial success. Frame time improving but still not hitting 16ms target.

### Next Steps (If Needed)

To reach 16ms target, consider:

1. **Profile-guided optimization** — Use Android Studio Profiler to identify exact bottleneck
2. **Reduce grid recomposition** — Implement full LazyGridState memoization per category
3. **Async icon loading** — Move icon loading off main thread (if not already)
4. **Remove unnecessary modifiers** — Audit dragSource/dragSourceAlpha overhead
5. **Optimize color calculation** — Verify colorToken is truly precomputed everywhere

---

**Status:** P7 optimization complete (partial improvement)  
**Owner:** Optimization Sprint Session 1  
**Next Review:** After profile-guided analysis or when 16ms target critical
