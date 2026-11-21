package com.sek789102.formulabook.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteEntity
import com.sek789102.formulabook.data.model.NoteTagCrossRef
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.StudySessionEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.TagEntity
import com.sek789102.formulabook.data.model.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashedNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun observeNoteWithTagsById(noteId: Long): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithTagsById(noteId: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesWithTagsSnapshot(): List<NoteWithTags>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun getTrashedNotesSnapshot(): List<NoteWithTags>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoffMillis")
    suspend fun getExpiredTrashedNotes(cutoffMillis: Long): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE title = :title AND createdAt = :createdAt")
    suspend fun countNotesByTitleAndCreatedAt(title: String, createdAt: Long): Int

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT tags.*, COUNT(notes.id) AS noteCount
        FROM tags
        LEFT JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        LEFT JOIN notes ON notes.id = note_tag_cross_ref.noteId AND notes.deletedAt IS NULL
        GROUP BY tags.id
        ORDER BY noteCount DESC, tags.name COLLATE NOCASE ASC
        """
    )
    fun observeTagsWithCount(): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :noteId")
    suspend fun softDeleteNoteById(noteId: Long, deletedAt: Long)

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun restoreNoteFromTrash(noteId: Long, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun permanentlyDeleteNoteById(noteId: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrashNotes()

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoffMillis")
    suspend fun deleteExpiredTrashedNotes(cutoffMillis: Long)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updatePinStatus(noteId: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET needsReview = :needsReview, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateNeedsReviewStatus(noteId: Long, needsReview: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET content = :content, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateContent(noteId: Long, content: String, updatedAt: Long)

    @Query("UPDATE notes SET formulas = :formulas, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun updateFormulas(noteId: Long, formulas: List<FormulaBlock>, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearCrossRefsForNote(noteId: Long)

    @Transaction
    suspend fun saveNoteWithTags(note: NoteEntity, tagIds: List<Long>): Long {
        val noteId = if (note.id == 0L) {
            insertNote(note)
        } else {
            updateNote(note)
            note.id
        }
        clearCrossRefsForNote(noteId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.distinct().map { NoteTagCrossRef(noteId = noteId, tagId = it) })
        }
        return noteId
    }

    @Transaction
    suspend fun restoreNoteWithTags(note: NoteEntity, tagIds: List<Long>): Long {
        val restoredId = insertNote(note)
        clearCrossRefsForNote(restoredId)
        if (tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.distinct().map { NoteTagCrossRef(noteId = restoredId, tagId = it) })
        }
        return restoredId
    }

    @Transaction
    suspend fun getOrCreateTag(name: String, colorIndex: Int): Long {
        val clean = name.trim().removePrefix("#").trim()
        val existing = findTagByName(clean)
        if (existing != null) return existing.id
        val insertedId = insertTag(TagEntity(name = clean, colorIndex = colorIndex))
        if (insertedId != -1L) return insertedId
        return findTagByName(clean)?.id ?: -1L
    }

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllSubjectsSnapshot(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findSubjectByName(name: String): SubjectEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM subjects")
    suspend fun getMaxSubjectSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE notes SET subject = :fallbackId WHERE subject = :subjectId")
    suspend fun reassignNotesSubject(subjectId: String, fallbackId: String = SubjectCategory.GENERAL.name)

    @Query("UPDATE exam_countdowns SET subject = :fallbackId WHERE subject = :subjectId")
    suspend fun reassignExamsSubject(subjectId: String, fallbackId: String = SubjectCategory.GENERAL.name)

    @Query("UPDATE study_sessions SET subject = :fallbackId WHERE subject = :subjectId")
    suspend fun reassignStudySessionsSubject(subjectId: String, fallbackId: String = SubjectCategory.GENERAL.name)

    @Query("DELETE FROM subjects WHERE id = :subjectId AND isDefault = 0")
    suspend fun deleteCustomSubjectById(subjectId: String)

    @Transaction
    suspend fun deleteCustomSubjectSafely(subjectId: String) {
        reassignNotesSubject(subjectId)
        reassignExamsSubject(subjectId)
        reassignStudySessionsSubject(subjectId)
        deleteCustomSubjectById(subjectId)
    }

    // Exam Countdowns
    @Query("SELECT * FROM exam_countdowns ORDER BY examDateMillis ASC")
    fun observeAllExams(): Flow<List<ExamCountdownEntity>>

    @Query("SELECT * FROM exam_countdowns ORDER BY examDateMillis ASC")
    suspend fun getAllExamsSnapshot(): List<ExamCountdownEntity>

    @Query("SELECT * FROM exam_countdowns WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: Long): ExamCountdownEntity?

    @Query("SELECT COUNT(*) FROM exam_countdowns WHERE title = :title AND examDateMillis = :examDateMillis")
    suspend fun countExamsByTitleAndDate(title: String, examDateMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamCountdownEntity): Long

    @Query("DELETE FROM exam_countdowns WHERE id = :examId")
    suspend fun deleteExamById(examId: Long)

    // Study Focus Sessions
    @Query("SELECT * FROM study_sessions ORDER BY completedAt DESC")
    fun observeAllStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY completedAt DESC")
    suspend fun getAllStudySessionsSnapshot(): List<StudySessionEntity>

    @Query(
        "SELECT COUNT(*) FROM study_sessions WHERE subject = :subject AND durationMinutes = :durationMinutes AND completedAt = :completedAt"
    )
    suspend fun countStudySessions(subject: String, durationMinutes: Int, completedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long
}
