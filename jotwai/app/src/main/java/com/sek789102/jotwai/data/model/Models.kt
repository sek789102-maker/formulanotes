package com.sek789102.jotwai.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

enum class NoteType { TEXT, CHECKLIST }

/** How a reminder repeats after it fires. */
enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val checked: Boolean = false
)

@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("updatedAt"), Index("deletedAt")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.TEXT,
    val checklist: List<ChecklistItem> = emptyList(),
    /** Index into [NoteColors]; 0 = default surface. */
    val color: Int = 0,
    val folderId: Long? = null,
    val imagePaths: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    /** Hidden behind the device screen lock. Not encrypted. */
    val isLocked: Boolean = false,
    val reminderAt: Long? = null,
    val reminderRepeat: RepeatRule = RepeatRule.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    val isTrashed: Boolean get() = deletedAt != null

    /** True when the note holds nothing worth keeping. */
    val isBlank: Boolean
        get() = title.isBlank() && content.isBlank() && imagePaths.isEmpty() &&
            checklist.none { it.text.isNotBlank() }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "labels", indices = [Index(value = ["name"], unique = true)])
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String
)

@Entity(
    tableName = "note_labels",
    primaryKeys = ["noteId", "labelId"],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LabelEntity::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("labelId")]
)
data class NoteLabelCrossRef(val noteId: Long, val labelId: Long)

/** An earlier state of a note, kept so edits can be undone after the editor is closed. */
@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("noteId")]
)
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val title: String,
    val content: String,
    val type: NoteType,
    val checklist: List<ChecklistItem>,
    val imagePaths: List<String>,
    val savedAt: Long = System.currentTimeMillis()
)

data class NoteWithLabels(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(NoteLabelCrossRef::class, parentColumn = "noteId", entityColumn = "labelId")
    )
    val labels: List<LabelEntity>
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortOrder { UPDATED, CREATED, TITLE }

enum class ViewMode { GRID, LIST }

enum class TextSize(val scale: Float) { SMALL(0.9f), NORMAL(1f), LARGE(1.15f), XLARGE(1.3f) }
