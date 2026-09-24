package com.softhome.feature.iconpack.domain

import com.softhome.core.model.AppInfo
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource

/**
 * Decides, per app, which icon source to use. Pure logic (no Android) so it is
 * unit-testable. Pipeline order per docs/08-ICONPACK-FORMAT.md ?2:
 *   1. user override  2. icon pack match  3. auto-mask  4. system
 */
class IconResolver {

    fun resolve(
        app: AppInfo,
        activePack: IconPack?,
        overrides: Map<String, String>,   // componentKey -> packId (custom choice)
        maskUnsupported: Boolean,
    ): IconSource {
        // 1. user override
        val overridePackId = overrides[app.componentKey]
        if (overridePackId != null && activePack != null && overridePackId == activePack.id) {
            val drawable = activePack.entries[app.componentKey]
                ?: activePack.entries.entries.firstOrNull { it.key.startsWith("${app.packageName}/") }?.value
            if (drawable != null) {
                return IconSource.Override(activePack.id, drawable)
            }
        }
        // 2. icon pack match (exact componentKey, then package fallback)
        if (activePack != null && activePack.hasAppFilter) {
            val drawable = activePack.entries[app.componentKey]
                ?: activePack.entries.entries.firstOrNull { it.key.startsWith("${app.packageName}/") }?.value
            if (drawable != null) {
                return IconSource.FromPack(activePack.id, drawable)
            }
        }
        // 3. auto-mask (design says every unsupported app still looks consistent)
        if (maskUnsupported) return IconSource.AutoMask
        // 4. system fallback
        return IconSource.System
    }
}
