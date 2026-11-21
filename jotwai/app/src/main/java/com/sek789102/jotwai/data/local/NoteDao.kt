package com.sek789102.jotwai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteLabelCrossRef
import com.sek789102.jotwai.data.model.NoteVersionEntity
import com.sek789102.jotwai.data.model.NoteWithLabels
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // ---- Notes ----
    @Transaction
    @Query("SELECT * FROM notes")
    fun observeAllNotes(): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteWithLabels>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<NoteWithLabels?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteWithLabels?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteEntity(id: Long): NoteEntity?

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun getTrashedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun getTrashedBefore(cutoff: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL AND deletedAt IS NULL")
    suspend fun getNotesWithReminders(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE title = :title AND createdAt = :createdAt")
    suspend fun countByTitleAndCreated(title: String, createdAt: Long): Int

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    // ---- Folders ----
    @Query("SELECT * FROM folders ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getFolders(): List<FolderEntity>

    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    // ---- Labels ----
    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE")
    fun observeLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE")
    suspend fun getLabels(): List<LabelEntity>

    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    suspend fun getLabelByName(name: String): LabelEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabel(label: LabelEntity): Long

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteLabel(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteLabel(ref: NoteLabelCrossRef)

    @Query("DELETE FROM note_labels WHERE noteId = :noteId")
    suspend fun clearNoteLabels(noteId: Long)

    @Query("DELETE FROM note_labels WHERE noteId = :noteId AND labelId = :labelId")
    suspend fun deleteNoteLabel(noteId: Long, labelId: Long)

    // ---- Versions ----
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC, id DESC")
    fun observeVersions(noteId: Long): Flow<List<NoteVersionEntity>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC, id DESC")
    suspend fun getVersions(noteId: Long): List<NoteVersionEntity>

    @Query("SELECT * FROM note_versions WHERE id = :id")
    suspend fun getVersion(id: Long): NoteVersionEntity?

    @Query("SELECT imagePaths FROM note_versions")
    suspend fun getAllVersionImagePaths(): List<String>

    @Insert
    suspend fun insertVersion(version: NoteVersionEntity): Long

    @Query("DELETE FROM note_versions WHERE id = :id")
    suspend fun deleteVersion(id: Long)
}
