package com.sek789102.jotwai.domain

import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.SortOrder

/** Which list the home screen shows. */
sealed interface Section {
    data object Notes : Section
    data object Favorites : Section
    data object Reminders : Section
    data object Archive : Section
    data object Trash : Section
    data class Folder(val id: Long) : Section
    data class Label(val id: Long) : Section

    fun encode(): String = when (this) {
        Notes -> "notes"
        Favorites -> "fav"
        Reminders -> "reminders"
        Archive -> "archive"
        Trash -> "trash"
        is Folder -> "folder-$id"
        is Label -> "label-$id"
    }

    companion object {
        fun decode(s: String): Section = when {
            s == "fav" -> Favorites
            s == "reminders" -> Reminders
            s == "archive" -> Archive
            s == "trash" -> Trash
            s.startsWith("folder-") -> Folder(s.removePrefix("folder-").toLongOrNull() ?: 0L)
            s.startsWith("label-") -> Label(s.removePrefix("label-").toLongOrNull() ?: 0L)
            else -> Notes
        }
    }
}

data class SearchFilter(
    val query: String = "",
    val color: Int? = null,
    val onlyChecklists: Boolean = false,
    val onlyWithImages: Boolean = false,
    val onlyWithReminder: Boolean = false
) {
    val isActive: Boolean get() = query.isNotBlank() || color != null || onlyChecklists || onlyWithImages || onlyWithReminder
}

object NoteQuery {

    fun inSection(n: NoteWithLabels, section: Section): Boolean {
        val note = n.note
        return when (section) {
            Section.Notes -> !note.isTrashed && !note.isArchived
            Section.Favorites -> !note.isTrashed && note.isPinned
            Section.Reminders -> !note.isTrashed && note.reminderAt != null
            Section.Archive -> !note.isTrashed && note.isArchived
            Section.Trash -> note.isTrashed
            is Section.Folder -> !note.isTrashed && note.folderId == section.id
            is Section.Label -> !note.isTrashed && n.labels.any { it.id == section.id }
        }
    }

    /**
     * Searches title, body, checklist items and label names (not just titles), case-insensitive.
     * Words are matched separately, so "ซื้อ นม" finds a note containing both.
     * Locked notes match on their title only.
     */
    fun matches(n: NoteWithLabels, filter: SearchFilter): Boolean {
        val note = n.note
        if (filter.color != null && note.color != filter.color) return false
        if (filter.onlyChecklists && note.type != NoteType.CHECKLIST) return false
        if (filter.onlyWithImages && note.imagePaths.isEmpty()) return false
        if (filter.onlyWithReminder && note.reminderAt == null) return false
        val q = filter.query.trim()
        if (q.isEmpty()) return true
        val haystack = buildString {
            append(note.title).append('\n')
            if (!note.isLocked) {
                append(note.content).append('\n')
                note.checklist.forEach { append(it.text).append('\n') }
            }
            n.labels.forEach { append(it.name).append('\n') }
        }.lowercase()
        return q.lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }.all { haystack.contains(it) }
    }

    fun sort(list: List<NoteWithLabels>, order: SortOrder, section: Section): List<NoteWithLabels> {
        val base = when (order) {
            SortOrder.UPDATED -> list.sortedByDescending { it.note.updatedAt }
            SortOrder.CREATED -> list.sortedByDescending { it.note.createdAt }
            SortOrder.TITLE -> list.sortedBy { it.note.title.ifBlank { it.note.content }.lowercase() }
        }
        return when (section) {
            Section.Reminders -> list.sortedBy { it.note.reminderAt ?: Long.MAX_VALUE }
            Section.Trash -> list.sortedByDescending { it.note.deletedAt ?: 0L }
            else -> base.sortedByDescending { it.note.isPinned } // stable: keeps order inside groups
        }
    }
}
