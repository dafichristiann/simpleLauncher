package com.softhome.feature.home

import android.content.ComponentName
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.softhome.core.model.HomeRowKind
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P3: the home-row launch resolver + its graceful fallback chain. Pure decision logic,
 * tested with a fake resolver (no device). Decisions Q2/Q3 are locked by these cases.
 */
@RunWith(RobolectricTestRunner::class)
class RowLaunchResolverTest {

    /** A fake resolver: a set of intent "signatures" that the fake accepts. */
    private fun resolverAccepting(vararg accepted: (Intent) -> Boolean): RowLaunchResolver =
        RowLaunchResolver { intent -> if (accepted.any { it(intent) }) ComponentName("x", "y") else null }

    private fun category(cat: String): (Intent) -> Boolean = { it.categories?.contains(cat) == true }
    private fun pkg(name: String): (Intent) -> Boolean = { it.`package` == name }
    private fun action(act: String): (Intent) -> Boolean = { it.action == act }

    @Test
    fun time_opens_the_known_clock_package_via_launcher_activity() {
        // Android has no clock category and SET_ALARM is permission-gated (denied on the
        // Tecno), so Time opens the clock app through its known launcher package.
        val r = resolverAccepting(pkg("com.transsion.deskclock"))
        val intent = r.intentFor(HomeRowKind.Time)
        assertThat(intent).isNotNull()
        assertThat(intent!!.`package`).isEqualTo("com.transsion.deskclock")
        assertThat(intent.action).isEqualTo(Intent.ACTION_MAIN)
        assertThat(intent.categories).contains(Intent.CATEGORY_LAUNCHER)
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun time_returns_null_when_no_clock_package_present() {
        // No clock app at all -> null (caller shows a message); never a dead tap / crash.
        assertThat(resolverAccepting().intentFor(HomeRowKind.Time)).isNull()
    }

    @Test
    fun date_prefers_calendar_category_then_known_package() {
        // Category resolves.
        val viaCategory = resolverAccepting(category(Intent.CATEGORY_APP_CALENDAR))
        assertThat(viaCategory.intentFor(HomeRowKind.Date)).isNotNull()

        // Category absent (real Tecno case) -> known package wins.
        val viaPackage = resolverAccepting(pkg("com.transsion.calendar"))
        val intent = viaPackage.intentFor(HomeRowKind.Date)
        assertThat(intent!!.`package`).isEqualTo("com.transsion.calendar")
    }

    @Test
    fun weather_prefers_category_then_package_then_browser_fallback_Q3() {
        // 1. Category resolves (AOSP-like).
        assertThat(
            resolverAccepting(category(Intent.CATEGORY_APP_WEATHER)).intentFor(HomeRowKind.Weather),
        ).isNotNull()

        // 2. Category absent, known weather package present.
        val viaPackage = resolverAccepting(pkg("com.rlk.weathers")).intentFor(HomeRowKind.Weather)
        assertThat(viaPackage!!.`package`).isEqualTo("com.rlk.weathers")

        // 3. Nothing weather at all -> browser weather URL (Q3), never a dead tap.
        val none = resolverAccepting().intentFor(HomeRowKind.Weather)
        assertThat(none).isNotNull()
        assertThat(none!!.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(none.dataString).contains("weather")
        assertThat(none.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun search_opens_google_view_intent() {
        val r = resolverAccepting(action(Intent.ACTION_VIEW))
        val intent = r.intentFor(HomeRowKind.Search)
        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("https://www.google.com")
    }

    @Test
    fun music_prefers_spotify_deeplink() {
        val r = resolverAccepting(pkg(RowLaunchResolver.SPOTIFY))
        val intent = r.intentFor(HomeRowKind.Music)
        assertThat(intent).isNotNull()
        assertThat(intent!!.`package`).isEqualTo(RowLaunchResolver.SPOTIFY)
    }

    @Test
    fun music_with_no_target_returns_null() {
        // Music has no browser-style fallback -- a dead tap must be a null, not a crash.
        assertThat(resolverAccepting().intentFor(HomeRowKind.Music)).isNull()
    }

    @Test
    fun non_launching_rows_return_null() {
        val r = resolverAccepting()
        assertThat(r.intentFor(HomeRowKind.Calendar)).isNull()
        assertThat(r.intentFor(HomeRowKind.BatteryStorage)).isNull()
        assertThat(r.intentFor(HomeRowKind.Notes)).isNull()
    }

    @Test
    fun every_resolved_intent_carries_new_task_flag() {
        val r = resolverAccepting(
            pkg("com.transsion.deskclock"),
            category(Intent.CATEGORY_APP_CALENDAR),
            category(Intent.CATEGORY_APP_WEATHER),
            action(Intent.ACTION_VIEW),
        )
        listOf(
            HomeRowKind.Time, HomeRowKind.Date, HomeRowKind.Weather,
            HomeRowKind.Search, HomeRowKind.Music,
        ).forEach { kind ->
            val intent = r.intentFor(kind)
            assertThat(intent).isNotNull()
            assertThat(intent!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
                .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
