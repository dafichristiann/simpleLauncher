package com.softhome.core.data.repository

import android.content.Context
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.softhome.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

private class TestDispatchers : DispatcherProvider {
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
    override val main = Dispatchers.Unconfined
}

@RunWith(RobolectricTestRunner::class)
class DeviceStatusTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun repo() = DeviceStatusRepositoryImpl(context, TestDispatchers())

    @Test
    fun `reads battery percent from BatteryManager`() = runTest {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        shadowOf(bm).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 73)
        val snapshot = repo().snapshot()
        assertThat(snapshot.batteryPercent).isEqualTo(73)
    }

    @Test
    fun `storage math computes used fraction and free bytes`() {
        // 100 blocks total, 40 free, 10-byte blocks -> 60% used, 400 free bytes.
        val (fraction, free) = DeviceStatusRepositoryImpl.storageFromBlocks(
            totalBlocks = 100, availableBlocks = 40, blockSize = 10,
        )!!
        assertThat(fraction).isWithin(0.001f).of(0.6f)
        assertThat(free).isEqualTo(400L)
    }

    @Test
    fun `storage math is null on unusable input and never throws`() {
        assertThat(
            DeviceStatusRepositoryImpl.storageFromBlocks(0, 0, 0),
        ).isNull()
        // over-reported free blocks are clamped to total (0% used, all free)
        val (fraction, free) = DeviceStatusRepositoryImpl.storageFromBlocks(10, 999, 5)!!
        assertThat(fraction).isEqualTo(0f)
        assertThat(free).isEqualTo(50L)
    }

    @Test
    fun `snapshot storage is null-or-in-range`() = runTest {
        // Under Robolectric StatFs reports zero blocks, so storage may be null here;
        // the REAL value is verified on the emulator in Phase 6 (screenshot vs `df`).
        val snapshot = repo().snapshot()
        val frac = snapshot.storageUsedFraction
        if (frac != null) {
            assertThat(frac).isAtLeast(0f)
            assertThat(frac).isAtMost(1f)
        }
        assertThat(snapshot.storageUsedPercent).isEqualTo(
            frac?.let { (it * 100f).toInt() },
        )
    }

    @Test
    fun `empty snapshot exposes nulls safely`() {
        val empty = com.softhome.core.model.DeviceStatusSnapshot.EMPTY
        assertThat(empty.batteryPercent).isNull()
        assertThat(empty.storageUsedPercent).isNull()
    }
}
