package com.softhome.core.model

/**
 * P4: the per-package drawer-icon table, ported from the design library
 * (design/homeApp.pen).
 *
 * **Two design sources, one table** (P4 decision 1: the tiles win for the apps they show):
 *  - frame `TpzL1` "Warm App Drawer -- Unique Icon Grid" draws **24 real tiles**, each with an
 *    exact lucide glyph + a `$icon-*` color variable. Those 24 are the source of truth for
 *    the apps shown there (e.g. Agoda -> `tent`/Travel, DANA -> `wallet-minimal`/Finance,
 *    Chrome -> `globe`/Browser blue).
 *  - frame `ciHU3` "Warm Right Rail -- Icon Language Library" enumerates the **8 categories**
 *    and the remaining apps (glyph + category). Those fill in everything `TpzL1` does not
 *    draw (e.g. Telegram -> `send`/Communication, GoPay -> `wallet-cards`/Finance).
 *
 * The design draws **one lucide glyph per app**, tinted by a color slot -- NOT a real brand
 * logo. This table is that mapping in code, so two different apps never collapse to the same
 * generic glyph (the "duplicate icons" complaint).
 *
 * `glyph` is a **lucide kebab name** (e.g. "message-circle", "car-front"); the UI resolves
 * it through `LineIcon.fromLucide`. `category` is the 8-way [DrawerCategory]; `colorToken`
 * is the [DrawerIconTokenName] slot (which is **not** always the category color -- the
 * design proves this, e.g. Chrome/Browser uses the blue token, Calculator uses Media).
 *
 * Matching is by **package-name fragment** (case-insensitive `contains`) against the real
 * Tecno launcher package list, because the design labels apps by name ("DANA", "Brave")
 * rather than package id. The first matching entry wins, so more specific fragments must come
 * before any substring that could shadow them (e.g. GoPay's `gojek.gopay` before `gojek`;
 * Threads' `instagram.barcelona` before `instagram`).
 */
data class DrawerIconEntry(
    val fragment: String,
    val glyph: String,
    val category: DrawerCategory,
    val colorToken: DrawerIconTokenName,
)

object DrawerIconMap {

    /**
     * The seeded table. Order matters: the **first** entry whose [DrawerIconEntry.fragment]
     * appears in the package name wins, so put more specific fragments before generic ones
     * and before any substring that could shadow them.
     *
     * Section 1 = the exact `TpzL1` tiles (the apps the drawer mock draws).
     * Sections 2+ = the `ciHU3` library, grouped by category.
     */
    private val entries: List<DrawerIconEntry> = listOf(
        // =====================================================================
        // 0. Specific overrides that MUST precede any generic substring that
        //    would otherwise shadow them (first-match-wins). Kept first so the
        //    real Tecno packages resolve to their intended glyph + color.
        // =====================================================================
        // Shopee Food Driver contains the substring "drive", which used to win
        // (giving it the Google Drive cloud). Its own fragment must come first.
        DrawerIconEntry("shopee.foody.driver", "bike", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        // Phone Master contains the substring "phone", which used to win (giving
        // it the dialer handset). Its own fragment must come first.
        DrawerIconEntry("phonemaster", "wrench", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        // Gboard (Android keyboard) contains the substring "google", which used
        // to win (giving it the Google search glyph). Gboard needs its own entry.
        DrawerIconEntry("inputmethod.latin", "keyboard", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("inputmethod", "keyboard", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),

        // =====================================================================
        // 1. TpzL1 -- the 24 real drawer tiles (glyph + $icon-* color, verbatim)
        // =====================================================================
        DrawerIconEntry("whatsapp", "message-circle", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("instagram.barcelona", "network", DrawerCategory.Communication, DrawerIconTokenName.Communication), // Threads (before instagram)
        DrawerIconEntry("instagram", "image", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("android.gm", "mail", DrawerCategory.Communication, DrawerIconTokenName.Communication), // Gmail
        DrawerIconEntry("chrome", "globe", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        // Google Messages ships as ...apps.messaging; AOSP Messages as ...messaging.
        DrawerIconEntry("apps.messaging", "message-square", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("smartmessage", "message-square", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("messaging", "message-square", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("messages", "message-square", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("dialer", "phone", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("contacts", "contact-round", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("calendar", "calendar-days", DrawerCategory.ProductivityTools, DrawerIconTokenName.Communication),
        DrawerIconEntry("calculator", "calculator", DrawerCategory.ProductivityTools, DrawerIconTokenName.Media),
        DrawerIconEntry("camera", "camera", DrawerCategory.CameraMedia, DrawerIconTokenName.Media),
        DrawerIconEntry("deskclock", "alarm-clock", DrawerCategory.ProductivityTools, DrawerIconTokenName.Neutral),
        DrawerIconEntry("clock", "alarm-clock", DrawerCategory.ProductivityTools, DrawerIconTokenName.Neutral),
        DrawerIconEntry("settings", "settings-2", DrawerCategory.ProductivityTools, DrawerIconTokenName.Neutral),
        DrawerIconEntry("documentsui", "folder-open", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("nbu.files", "folder-open", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("files", "folder-open", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("filemanager", "folder-open", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("photos", "images", DrawerCategory.CameraMedia, DrawerIconTokenName.Media),
        // YouTube Music (`...youtube.music`) must be read before plain YouTube.
        DrawerIconEntry("youtube.music", "music-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),
        DrawerIconEntry("ytmusic", "music-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),
        DrawerIconEntry("youtube", "play", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),
        DrawerIconEntry("spotify", "disc-3", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),
        DrawerIconEntry("maps", "map", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("dana", "wallet-minimal", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("alkitab", "book-open", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Communication),
        DrawerIconEntry("brave", "shield-check", DrawerCategory.BrowserSearch, DrawerIconTokenName.Productivity),
        // Agoda: the TpzL1 tile draws `tent`/Travel (ciHU3 lists `tag` -- tiles win).
        DrawerIconEntry("agoda", "tent", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        // "gallery20" = AI Gallery; placed before the generic "gallery".
        DrawerIconEntry("gallery20", "images", DrawerCategory.CameraMedia, DrawerIconTokenName.Media),
        // Claude (ciHU3 glyph `sparkles`); tile shows `$selected-icon` (a mock leak) -> use its
        // ciHU3 category color (Browser blue). Documented assumption.
        DrawerIconEntry("claude", "sparkles", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("discord", "messages-square", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        // Tecno/user-installed launchers and utilities present on the reference device.
        // Keep these explicit so they use the same design glyph language as the seeded
        // drawer instead of falling through to the generic category heuristic.
        DrawerIconEntry("bitpit.launcher", "package", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity), // Niagara Launcher
        DrawerIconEntry("honestbank", "banknote", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("growtons", "phone-off", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("guitartuna", "music-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),
        DrawerIconEntry("nfcchecker", "shield-check", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),

        // =====================================================================
        // 2. ciHU3 library -- Communication
        // =====================================================================
        DrawerIconEntry("telegram", "send", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("tachyon", "video", DrawerCategory.Communication, DrawerIconTokenName.Communication), // Google Meet
        DrawerIconEntry("truecaller", "phone-off", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("spam", "phone-off", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("outlook", "mail", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("mail", "mail", DrawerCategory.Communication, DrawerIconTokenName.Communication),
        DrawerIconEntry("phone", "phone", DrawerCategory.Communication, DrawerIconTokenName.Communication),

        // =====================================================================
        // 3. ciHU3 library -- Social & Entertainment
        // =====================================================================
        DrawerIconEntry("facebook", "share-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("katana", "share-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social), // FB
        DrawerIconEntry("trill", "play", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media), // TikTok
        DrawerIconEntry("aweme", "play", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media), // TikTok lite
        DrawerIconEntry("pinterest", "pin", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("fortnite", "gamepad-2", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("gamespace", "joystick", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        // "123-4-player" = JindoBlu FourPlayers (a 4-player local game).
        DrawerIconEntry("jindoblu", "users-round", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("fourplayers", "users-round", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("tandem", "languages", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Social),
        DrawerIconEntry("netflix", "tv", DrawerCategory.SocialEntertainment, DrawerIconTokenName.Media),

        // =====================================================================
        // 4. ciHU3 library -- Productivity & Tools
        // =====================================================================
        DrawerIconEntry("notepad", "notebook-pen", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("notebook", "notebook-pen", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("drive", "cloud-upload", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("apps.docs", "cloud-upload", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("recorder", "mic", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("theme", "palette", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("simtoolkit", "credit-card", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("safety", "shield-check", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("roaming", "globe", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity),
        DrawerIconEntry("gotii", "globe", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity), // TECNO Roaming
        DrawerIconEntry("android.stk", "credit-card", DrawerCategory.ProductivityTools, DrawerIconTokenName.Productivity), // SIM Toolkit
        DrawerIconEntry("smart.caller", "phone", DrawerCategory.Communication, DrawerIconTokenName.Communication), // TECNO dialer/contacts

        // =====================================================================
        // 5. ciHU3 library -- Browser & Search
        // =====================================================================
        DrawerIconEntry("firefox", "globe", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("opera", "globe", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("browser", "globe", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("googlequicksearchbox", "search", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        // Specific `google.*` apps must come BEFORE the generic "google" fragment.
        DrawerIconEntry("google.android.videos", "tv", DrawerCategory.CameraMedia, DrawerIconTokenName.Media), // Google TV
        DrawerIconEntry("apps.bard", "sparkles", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser), // Gemini
        DrawerIconEntry("gemini", "sparkles", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("google", "search", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("chatgpt", "bot", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("openai", "bot", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("anthropic", "sparkles", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("transsnet", "store", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser), // Palm Store
        DrawerIconEntry("palmstore", "store", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser),
        DrawerIconEntry("vending", "store", DrawerCategory.BrowserSearch, DrawerIconTokenName.Browser), // Play Store pkg
        DrawerIconEntry("playstore", "play", DrawerCategory.BrowserSearch, DrawerIconTokenName.Media),

        // =====================================================================
        // 6. ciHU3 library -- Camera & Media
        // =====================================================================
        DrawerIconEntry("gallery", "images", DrawerCategory.CameraMedia, DrawerIconTokenName.Media),
        DrawerIconEntry("videoplayer", "play", DrawerCategory.CameraMedia, DrawerIconTokenName.Media),

        // =====================================================================
        // 7. ciHU3 library -- Maps & Travel
        // =====================================================================
        DrawerIconEntry("compass", "compass", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("weather", "cloud-sun", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("tripcom", "briefcase", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("trip.com", "briefcase", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("ctrip", "briefcase", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel), // Trip.com
        DrawerIconEntry("grab", "map-pin", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("gojek.gopay", "wallet-cards", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance), // GoPay (before gojek)
        DrawerIconEntry("gojek", "car-front", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),
        DrawerIconEntry("driver", "bike", DrawerCategory.MapsTravel, DrawerIconTokenName.Travel),

        // =====================================================================
        // 8. ciHU3 library -- Finance & Shopping
        // =====================================================================
        DrawerIconEntry("jago", "credit-card", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("bankbke", "banknote", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance), // Seabank
        DrawerIconEntry("seabank", "banknote", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("shopeepay", "wallet-minimal", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("yup.bank", "wallet", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("shopee", "shopping-cart", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("tokopedia", "shopping-bag", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("circlekindonesia", "shopping-basket", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("circlekshop", "shopping-basket", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("circle_k", "shopping-basket", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("bcadigital", "wallet-cards", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance), // blu
        DrawerIconEntry("welife", "heart-handshake", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance),
        DrawerIconEntry("smartlife.nebula", "heart-handshake", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance), // TECNO Welife
        DrawerIconEntry("indosat", "wallet-cards", DrawerCategory.FinanceShopping, DrawerIconTokenName.Finance), // myIM3

        // =====================================================================
        // 9. ciHU3 library -- Food & Lifestyle
        // =====================================================================
        DrawerIconEntry("kfc", "utensils", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("kopikenangan", "coffee", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("tixid", "ticket", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("tix.android", "ticket", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("carlcare", "heart-handshake", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("edlink", "graduation-cap", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance),
        DrawerIconEntry("aivoiceassistant", "package", DrawerCategory.FoodLifestyle, DrawerIconTokenName.Finance), // Ella AI
    )

    /** The design's per-package glyph + category + color, or null when unknown. */
    fun forPackage(packageName: String): DrawerIconEntry? {
        if (packageName.isBlank()) return null
        val hay = packageName.lowercase()
        return entries.firstOrNull { hay.contains(it.fragment) }
    }

    /** Every entry in [category] (used by tests + the category resolver audit). */
    fun allIn(category: DrawerCategory): List<DrawerIconEntry> =
        entries.filter { it.category == category }

    /** All entries (read-only) -- for tests + tooling. */
    fun all(): List<DrawerIconEntry> = entries
}
