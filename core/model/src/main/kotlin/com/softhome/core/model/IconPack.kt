package com.softhome.core.model

/**
 * Where an icon pack's drawables actually live. Resolution differs per source:
 *  - [Zip]           a `.zip` on disk / copied into app storage (P1.5 import flow)
 *  - [InstalledPack] an icon-pack APK already installed on the device (schema query)
 *  - [Assets]        bundled inside SOFT / HOME's own assets (seed / demo pack)
 */
sealed interface IconPackSource {
    /** A zip archive on the filesystem. `zipPath` is absolute. */
    data class Zip(val zipPath: String) : IconPackSource

    /**
     * An installed icon-pack APK. Drawables are resolved through the pack's own
     * package resources via `PackageManager.getResourcesForApplication`.
     */
    data class InstalledPack(val packageName: String) : IconPackSource

    /** Drawables bundled in our own `assets/iconpacks/<id>/`. */
    data class Assets(val assetDir: String) : IconPackSource
}

/** A parsed icon pack (zip / apk / assets). */
data class IconPack(
    val id: String,
    val name: String,
    val sourcePath: String,
    /** component key -> drawable resource name inside the pack. */
    val entries: Map<String, String>,
    val hasAppFilter: Boolean,
    val iconCount: Int = entries.size,
    /** Where the drawables live. Defaults to a zip at [sourcePath] for back-compat. */
    val source: IconPackSource = IconPackSource.Zip(sourcePath),
    /** Human label for the source kind (used by the import sheet). */
    val sourceKind: SourceKind = SourceKind.Zip,
    /** True when the pack declared a `drawable.xml`/`appfilter` we could read. */
    val drawableCount: Int = entries.values.toSet().size,
) {
    enum class SourceKind { Zip, Installed, Assets }

    val displaySource: String
        get() = when (source) {
            is IconPackSource.Zip -> "Imported .zip"
            is IconPackSource.InstalledPack -> "Installed app"
            is IconPackSource.Assets -> "Bundled"
        }

    companion object {
        fun invalid(id: String, name: String, path: String) = IconPack(
            id = id, name = name, sourcePath = path,
            entries = emptyMap(), hasAppFilter = false, iconCount = 0,
            source = IconPackSource.Zip(path), sourceKind = SourceKind.Zip,
        )
    }
}

/** Result of parsing a pack -- carries a graceful-failure reason (docs/08 ?5). */
sealed interface IconPackParseResult {
    data class Success(val pack: IconPack) : IconPackParseResult
    data class NoAppFilter(val pack: IconPack) : IconPackParseResult
    data class Invalid(val reason: String) : IconPackParseResult
}

/**
 * A discovered-but-not-yet-parsed icon pack on the device, surfaced to the import
 * sheet so the user can pick one. [label] comes from the pack app's own label.
 */
data class DiscoveredIconPack(
    val id: String,
    val label: String,
    val packageName: String,
    /** Resource name in the pack app that holds `appfilter.xml`, if any. */
    val appFilterResName: String? = null,
    /** Where its appfilter asset lives: "assets" or "res/xml" (informational). */
    val appFilterLocation: String? = null,
)
