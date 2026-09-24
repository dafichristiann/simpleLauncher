package com.softhome.core.model

/**
 * A snapshot of device status shown on the home battery/storage row (P2 / E4).
 *
 * Decision P2-3: values are **real**, read from `BatteryManager` and `StatFs`
 * (pure Android APIs, no extra permission) -- not static design values.
 *
 * Every field is nullable so the UI degrades gracefully when a value cannot be
 * read (e.g. an OEM returns no battery capacity): the row simply omits that part
 * instead of crashing.
 */
data class DeviceStatusSnapshot(
    /** Battery level 0..100, or null when unavailable. */
    val batteryPercent: Int? = null,
    /** True when the device is currently charging. */
    val batteryCharging: Boolean? = null,
    /** Storage used, 0f..1f of the app-visible data partition, or null when unavailable. */
    val storageUsedFraction: Float? = null,
    /** Free storage in bytes, or null when unavailable. */
    val storageFreeBytes: Long? = null,
) {
    /** Storage used percentage 0..100 as an int, or null. */
    val storageUsedPercent: Int?
        get() = storageUsedFraction?.let { (it.coerceIn(0f, 1f) * 100f).toInt() }

    companion object {
        val EMPTY = DeviceStatusSnapshot()
    }
}
