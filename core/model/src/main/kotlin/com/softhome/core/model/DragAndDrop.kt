package com.softhome.core.model

/**
 * Drag-and-drop model (P4a) — pure, Android-free so it is fully unit-testable.
 *
 * P4a ships **one gesture engine** with two drop targets:
 *   - **Home row reorder** (drag a [HomeRowKind] to a new position in the row list).
 *   - **App -> folder** in the drawer (drag an app key onto a folder tile / the
 *     "new folder" action, or drag a member out of an open folder).
 *
 * The engine's *state* and *drop resolution* live here (and in [HomeRowLogic] /
 * [FolderLogic]); the Compose gesture modifiers + preview live in `core:designsystem`.
 * Keeping resolution pure means the two adapters get JVM tests without a device.
 */
sealed interface DragKind {
    /** Dragging a home row (reorder). */
    data class HomeRow(val kind: HomeRowKind) : DragKind

    /** Dragging a drawer app onto a folder / the new-folder action. */
    data class App(val componentKey: String) : DragKind

    /** Dragging an app **out of** an open folder popup. */
    data class FolderMember(val folderId: String, val componentKey: String) : DragKind
}

/**
 * The engine's transient state. [originPx]/[deltaPx] are in raw pixels (pointer space),
 * so this type carries no Android dependency. A surface owns one instance; drags never
 * cross surfaces.
 */
data class DragAndDropState(
    val dragging: DragKind? = null,
    val originPx: FloatPair = FloatPair.Zero,
    val deltaPx: FloatPair = FloatPair.Zero,
    /** Id of the currently hovered valid drop target, or null. */
    val hoveredTargetId: String? = null,
) {
    val isDragging: Boolean get() = dragging != null

    fun begin(kind: DragKind, origin: FloatPair): DragAndDropState =
        copy(dragging = kind, originPx = origin, deltaPx = FloatPair.Zero, hoveredTargetId = null)

    fun move(delta: FloatPair): DragAndDropState = copy(deltaPx = delta)

    fun hover(targetId: String?): DragAndDropState = copy(hoveredTargetId = targetId)

    fun end(): DragAndDropState = DragAndDropState()
}

/** Immutable float pair (pixel offset) — avoids an Android `Offset` in the pure layer. */
data class FloatPair(val x: Float, val y: Float) {
    companion object {
        val Zero = FloatPair(0f, 0f)
    }
}

/**
 * Pure drop resolution for the **home row reorder** target. Thin wrapper over
 * [HomeRowLogic.move] so the intent is explicit at the drag site and testable alone.
 */
object HomeRowDropResolver {
    /**
     * Result of dropping [dragged] at [targetIndex] (a position in the resulting list).
     * Unknown kinds / no movement return [prefs] unchanged.
     */
    fun reorder(
        prefs: List<HomeRowPref>,
        dragged: HomeRowKind,
        targetIndex: Int,
    ): List<HomeRowPref> = HomeRowLogic.move(prefs, dragged, targetIndex)
}

/**
 * Pure drop resolution for the **app -> folder** target. Every function delegates to
 * [FolderLogic] so folder membership has exactly one source of truth.
 */
object FolderDropResolver {

    /** App [key] dropped onto [folderId]. A no-op when already inside (Order/[addApp]). */
    fun assign(folders: List<Folder>, folderId: String, key: String): List<Folder> =
        folders.map { if (it.id == folderId) FolderLogic.addApp(it, key) else it }

    /**
     * App [key] dropped onto the "New folder" action: a new folder [newId] is created
     * containing [key] and appended to the list.
     */
    fun createWith(
        folders: List<Folder>,
        key: String,
        newId: String,
        name: String,
    ): List<Folder> = folders + FolderLogic.createFolder(id = newId, name = name, keys = listOf(key))

    /** App [key] dragged out of [folderId]. No-op when the folder is unknown. */
    fun removeFrom(folders: List<Folder>, folderId: String, key: String): List<Folder> =
        folders.map { if (it.id == folderId) FolderLogic.removeApp(it, key) else it }
}
