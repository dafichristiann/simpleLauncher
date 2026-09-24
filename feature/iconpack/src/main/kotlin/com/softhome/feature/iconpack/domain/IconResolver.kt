package com.softhome.feature.iconpack.domain

import com.softhome.core.model.AppInfo
import com.softhome.core.model.IconOverride
import com.softhome.core.model.IconPack
import com.softhome.core.model.IconSource

/**
 * Decides, per app, which icon source to use. Pure logic (no Android) so it is
 * unit-testable. Pipeline order per docs/08-ICONPACK-FORMAT.md ?2:
 *
 *   0. explicit user override (P4b)   -- a chosen pack drawable OR a chosen glyph+color
 *   1. icon-pack match
 *   2. auto-mask
 *   3. system
 */
class IconResolver {

    fun resolve(
        app: AppInfo,
        activePack: IconPack?,
        overrides: Map<String, IconOverride>,   // componentKey -> user's explicit choice
        maskUnsupported: Boolean,
    ): IconSource {
        // 0. explicit user override (P4b). Highest precedence: a saved choice always wins.
        when (val override = overrides[app.componentKey]) {
            is IconOverride.Glyph ->
                return IconSource.Glyph(override.symbolName, override.colorToken)

            is IconOverride.Pack -> {
                // Only meaningful while the *same* pack is active AND the chosen drawable
                // is really present in it; otherwise fall through to the normal pipeline.
                if (activePack != null && override.drawableName in activePack.entries.values) {
                    return IconSource.Override(activePack.id, override.drawableName)
                }
            }

            null -> Unit
        }
        // 1. icon pack match (exact componentKey, then package fallback)
        if (activePack != null && activePack.hasAppFilter) {
            val drawable = activePack.entries[app.componentKey]
                ?: activePack.entries.entries.firstOrNull { it.key.startsWith("${app.packageName}/") }?.value
            if (drawable != null) {
                return IconSource.FromPack(activePack.id, drawable)
            }
        }
        // 2. auto-mask (design says every unsupported app still looks consistent)
        if (maskUnsupported) return IconSource.AutoMask
        // 3. system fallback
        return IconSource.System
    }
}
