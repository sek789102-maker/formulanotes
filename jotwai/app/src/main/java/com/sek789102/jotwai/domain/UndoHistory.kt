package com.sek789102.jotwai.domain

/**
 * Undo/redo for the editor. Rapid typing is grouped: a new step is recorded only when
 * [groupWindowMillis] passed since the last change, or when [force] is set (e.g. ticking a box).
 */
class UndoHistory<T>(
    initial: T,
    private val limit: Int = 200,
    private val groupWindowMillis: Long = 800L
) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    var current: T = initial
        private set
    private var lastRecordAt = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(state: T, now: Long = System.currentTimeMillis(), force: Boolean = false) {
        if (state == current) return
        if (force || undoStack.isEmpty() || now - lastRecordAt > groupWindowMillis) {
            undoStack.addLast(current)
            if (undoStack.size > limit) undoStack.removeFirst()
        }
        lastRecordAt = now
        current = state
        redoStack.clear()
    }

    fun undo(): T? {
        val prev = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        current = prev
        lastRecordAt = 0L
        return prev
    }

    fun redo(): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        current = next
        lastRecordAt = 0L
        return next
    }

    fun reset(state: T) {
        undoStack.clear()
        redoStack.clear()
        current = state
        lastRecordAt = 0L
    }
}
