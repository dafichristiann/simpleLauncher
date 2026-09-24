package com.softhome.core.model

/**
 * Pure eligibility rules for the long-press context menu (P3 / F3).
 *
 * Decision P3-4: "Uninstall" is shown but **greyed** for system / unremovable apps;
 * "App Info" is always available. No Android types here so this stays unit-testable.
 */
object AppActionLogic {

    /** Menu rows, in display order (spec section 4.8). */
    enum class Action {
        Open,
        AppInfo,
        EditIcon,
        Remove,
        Uninstall,
        Shortcuts,
    }

    /**
     * Whether the row style should render an action as **enabled**.
     *
     * @param action the menu action
     * @param isSystemApp app carries `FLAG_SYSTEM` (never uninstallable by a normal app)
     * @param isRemovable the app is installed for the current user and removable
     *        (`ApplicationInfo.FLAG_INSTALLED` + `<application android:isRemovable>` etc.)
     * @param inDrawer whether the app is currently shown in the drawer (so Remove makes sense)
     * @param hasShortcuts whether the app advertises launcher shortcuts
     */
    fun isEnabled(
        action: Action,
        isSystemApp: Boolean,
        isRemovable: Boolean,
        inDrawer: Boolean = true,
        hasShortcuts: Boolean = true,
    ): Boolean = when (action) {
        // Open / App Info are always available (P3-4).
        Action.Open, Action.AppInfo -> true
        // Edit Icon / Remove only make sense for an app present in the drawer.
        Action.EditIcon, Action.Remove -> inDrawer
        // Uninstall is greyed for system or non-removable apps (P3-4).
        Action.Uninstall -> !isSystemApp && isRemovable
        // Shortcuts only when the app actually has some.
        Action.Shortcuts -> hasShortcuts
    }
}
