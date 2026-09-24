package com.softhome.feature.iconpack.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.softhome.core.model.DiscoveredIconPack
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconPackParseResult
import com.softhome.core.model.IconPackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discovers icon packs already installed on the device and parses their
 * `appfilter.xml` (P1.5). This is the "pick from another installed icon pack app"
 * half of the import flow (docs/08 ?1).
 *
 * The standard, launcher-agnostic way to advertise an icon pack is an activity with
 * an intent filter for one of a handful of well-known actions (ADW, Apex, Nova,
 * Go, Lawnchair, ...). We query for all of them; the pack's appfilter usually lives
 * in `assets/appfilter.xml`, sometimes `res/xml/appfilter.xml`.
 */
@Singleton
class InstalledIconPackScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: AppFilterParser,
) {

    /** Actions that icon-pack apps use to advertise themselves (superset, well-known). */
    private val packActions = listOf(
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "ch.deletescape.lawnchair.ICONPACK",
        "com.google.android.apps.nexuslauncher.ICONPACK",
    )

    /** Installed packs, de-duplicated by package name. Cheap - safe to call on IO. */
    fun scan(): List<DiscoveredIconPack> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, DiscoveredIconPack>()
        for (action in packActions) {
            val intent = Intent(action)
            val resolved: List<ResolveInfo> = try {
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            } catch (_: Throwable) {
                emptyList()
            }
            for (ri in resolved) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (seen.containsKey(pkg)) continue
                val label = ri.loadLabel(pm).toString().ifBlank { pkg }
                val loc = locateAppFilter(pkg)
                seen[pkg] = DiscoveredIconPack(
                    id = pkg,
                    label = label,
                    packageName = pkg,
                    appFilterResName = loc?.first,
                    appFilterLocation = loc?.second,
                )
            }
        }
        return seen.values.toList()
    }

    /** Parse a chosen installed pack's appfilter into an [IconPack]. */
    fun parse(discovered: DiscoveredIconPack): IconPackParseResult {
        val pkg = discovered.packageName
        val packId = pkg.lowercase()
        val name = discovered.label
        val source = IconPackSource.InstalledPack(pkg)

        val xml = readAppFilter(pkg, discovered) ?: return IconPackParseResult.NoAppFilter(
            IconPack.invalid(packId, name, pkg).copy(source = source, sourceKind = IconPack.SourceKind.Installed),
        )
        val result = parser.parseString(xml, packId, name, pkg)
        return relabel(result, source)
    }

    private fun relabel(result: IconPackParseResult, source: IconPackSource): IconPackParseResult =
        when (result) {
            is IconPackParseResult.Success -> IconPackParseResult.Success(
                result.pack.copy(sourceKind = IconPack.SourceKind.Installed, source = source),
            )
            is IconPackParseResult.NoAppFilter -> IconPackParseResult.NoAppFilter(
                result.pack.copy(sourceKind = IconPack.SourceKind.Installed, source = source),
            )
            is IconPackParseResult.Invalid -> result
        }

    /** Read the appfilter XML: first from assets, then from res/xml. */
    private fun readAppFilter(pkg: String, discovered: DiscoveredIconPack): String? {
        val candidates = listOfNotNull(
            discovered.appFilterLocation?.let { "$pkg:$it" },
            "$pkg:assets/appfilter.xml",
            "$pkg:res/xml/appfilter.xml",
        )
        for (candidate in candidates) {
            val (_, path) = candidate.split(':', limit = 2)
            val xml = try {
                context.createPackageContext(pkg, 0).assets.open(path.removePrefix("assets/"))
                    .bufferedReader().use { it.readText() }
            } catch (_: Throwable) {
                null
            }
            if (!xml.isNullOrBlank()) return xml
        }
        // Fall back to a resource-embedded appfilter (some packs ship res/xml/<name>.xml).
        return try {
            val res = context.packageManager.getResourcesForApplication(pkg)
            val id = res.getIdentifier("appfilter", "xml", pkg)
            if (id == 0) null else res.getXml(id).let { _ -> null }
        } catch (_: Throwable) {
            null
        }
    }

    /** Best-effort: does this package expose an `appfilter.xml` asset or res/xml file? */
    private fun locateAppFilter(pkg: String): Pair<String, String>? {
        return try {
            val ctx = context.createPackageContext(pkg, 0)
            if (ctx.assets.list("")?.contains("appfilter.xml") == true) {
                return "appfilter.xml" to "assets/appfilter.xml"
            }
            val res = context.packageManager.getResourcesForApplication(pkg)
            val id = res.getIdentifier("appfilter", "xml", pkg)
            if (id != 0) "appfilter" to "res/xml/appfilter.xml" else null
        } catch (_: Throwable) {
            null
        }
    }
}
