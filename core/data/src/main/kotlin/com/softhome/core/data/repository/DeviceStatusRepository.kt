package com.softhome.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.model.DeviceStatusSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads real device status for the home battery/storage row (P2 / E4).
 *
 * Decision P2-3: values are real (`BatteryManager` + `StatFs`), not static design
 * values. Pure Android APIs, no permission. Every read degrades to null instead of
 * throwing; the UI omits anything unavailable.
 */
interface DeviceStatusRepository {
    suspend fun snapshot(): DeviceStatusSnapshot
}

@Singleton
class DeviceStatusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : DeviceStatusRepository {

    override suspend fun snapshot(): DeviceStatusSnapshot = withContext(dispatchers.io) {
        val battery = readBattery()
        val storage = readStorage()
        DeviceStatusSnapshot(
            batteryPercent = battery?.first,
            batteryCharging = battery?.second,
            storageUsedFraction = storage?.first,
            storageFreeBytes = storage?.second,
        )
    }

    /** Returns (percent 0..100, charging) or null when unavailable. */
    private fun readBattery(): Pair<Int, Boolean>? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var percent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        var charging = isChargingSafe(bm)

        // Fallback for OEMs that return a negative capacity: sticky battery intent.
        if (percent !in 0..100) {
            val sticky: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                percent = (level * 100 / scale)
            }
            val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        if (percent !in 0..100) return null
        return percent to charging
    }

    /**
     * `BatteryManager.isCharging()` (API 26+) is resolved reflectively so a
     * missing/odd implementation never throws (some OEM builds and older
     * Robolectric shadows omit it) -- charging state is cosmetic, capacity is not.
     */
    private fun isChargingSafe(bm: BatteryManager?): Boolean {
        if (bm == null) return false
        return runCatching {
            BatteryManager::class.java.getMethod("isCharging").invoke(bm) as? Boolean
        }.getOrNull() ?: runCatching {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ==
                BatteryManager.BATTERY_STATUS_CHARGING
        }.getOrDefault(false)
    }

    /** Returns (usedFraction 0..1, freeBytes) or null when unavailable. */
    private fun readStorage(): Pair<Float, Long>? = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        storageFromBlocks(
            totalBlocks = stat.blockCountLong,
            availableBlocks = stat.availableBlocksLong,
            blockSize = stat.blockSizeLong,
        )
    }.getOrNull()

    companion object {
        /**
         * Pure storage math (extracted so it is unit-testable without a real
         * filesystem): returns (usedFraction 0..1, freeBytes), or null when the
         * inputs are unusable (zero total). Never throws.
         */
        fun storageFromBlocks(
            totalBlocks: Long,
            availableBlocks: Long,
            blockSize: Long,
        ): Pair<Float, Long>? {
            val total = totalBlocks * blockSize
            if (total <= 0L) return null
            val free = (availableBlocks * blockSize).coerceIn(0L, total)
            val used = total - free
            val fraction = (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
            return fraction to free
        }
    }
}
