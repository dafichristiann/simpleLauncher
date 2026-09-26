package com.softhome.core.data.packages

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single change to the device's set of launchable apps.
 */
sealed interface PackageChange {
    /** One or more apps were installed / became launchable. */
    data object Added : PackageChange

    /** One or more apps were removed / are no longer launchable. */
    data object Removed : PackageChange
}

/**
 * Process-wide watcher for install / uninstall of **launchable** apps, backed by
 * `LauncherApps.registerCallback` (API 21+).
 *
 * Why `LauncherApps` and not a `PACKAGE_ADDED` broadcast:
 *  - the callback filters to apps this launcher can actually display (activities with
 *    `MAIN` + `LAUNCHER`), so we do not refresh on unrelated package churn;
 *  - it delivers the affected `packageName`s directly, so a removal can be pruned
 *    precisely without re-enumerating the whole device;
 *  - it is the platform-blessed API for launchers and needs no extra permission.
 *
 * The callback is registered exactly once for the process lifetime; consumers observe
 * [changes]. The monitor never writes state itself — it is a **signal** source, so the
 * data/UI layers stay the single writer of their own state (docs/01 layer rule).
 */
@Singleton
class PackageEventMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _changes = MutableSharedFlow<PackageChange>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits once per install / uninstall batch of launchable apps. */
    val changes: Flow<PackageChange> = _changes.asSharedFlow()

    private val registered = java.util.concurrent.atomic.AtomicBoolean(false)
    private var callback: LauncherApps.Callback? = null

    /** Register the platform callback. Safe to call repeatedly; only the first registers. */
    fun start() {
        if (!registered.compareAndSet(false, true)) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle?) {
                android.util.Log.d(TAG, "[MONITOR] removed $packageName")
                _changes.tryEmit(PackageChange.Removed)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle?) {
                android.util.Log.d(TAG, "[MONITOR] added $packageName")
                _changes.tryEmit(PackageChange.Added)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle?) {
                // A package update can change its label/icon/launchability — refresh.
                android.util.Log.d(TAG, "[MONITOR] changed $packageName")
                _changes.tryEmit(PackageChange.Added)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle?,
                replacing: Boolean,
            ) {
                android.util.Log.d(TAG, "[MONITOR] available ${packageNames.joinToString()}")
                _changes.tryEmit(PackageChange.Added)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle?,
                replacing: Boolean,
            ) {
                android.util.Log.d(TAG, "[MONITOR] unavailable ${packageNames.joinToString()}")
                _changes.tryEmit(PackageChange.Removed)
            }
        }
        callback = cb
        // Callbacks are delivered on the given Handler's thread; use the main looper so the
        // emit happens promptly, then consumers move work off-thread as needed.
        runCatching { launcherApps.registerCallback(cb, Handler(Looper.getMainLooper())) }
            .onSuccess { android.util.Log.d(TAG, "[MONITOR] registered LauncherApps callback") }
            .onFailure { registered.set(false) }
    }

    /** Unregister (used by tests / future teardown). */
    fun stop() {
        val cb = callback ?: return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        runCatching { launcherApps?.unregisterCallback(cb) }
        callback = null
        registered.set(false)
    }

    private companion object {
        const val TAG = "SOFTHOME_PIPELINE"
    }
}
