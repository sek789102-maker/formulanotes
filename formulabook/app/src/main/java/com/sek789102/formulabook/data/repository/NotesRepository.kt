package com.sek789102.formulabook.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.NoteDao
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteEntity
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.StudySessionEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.TagEntity
import com.sek789102.formulabook.data.model.TagWithCount
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import com.sek789102.formulabook.domain.study.Sm2Rating
import com.sek789102.formulabook.domain.study.Sm2Scheduler
import com.sek789102.formulabook.R
import com.sek789102.formulabook.util.NoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.concurrent.TimeUnit

data class BackupImportSummary(
    val importedNotes: Int,
    val skippedNotes: Int,
    val importedExams: Int,
    val skippedExams: Int,
    val importedSessions: Int,
    val skippedSessions: Int,
    val importedSubjects: Int = 0
) {
    val totalImported: Int get() = importedNotes + importedExams + importedSessions + importedSubjects
    val totalSkipped: Int get() = skippedNotes + skippedExams + skippedSessions
}

class InvalidBackupFormatException : IllegalArgumentException("Invalid backup JSON format")
class BackupTooLargeException : IllegalArgumentException("Backup file exceeds maximum allowed size")

class NotesRepository(
    private val appContext: Context,
    private val database: AppDatabase
) {

    private val noteDao: NoteDao = database.noteDao()

    val allNotesWithTags: Flow<List<NoteWithTags>> = noteDao.observeAllNotesWithTags()
    val trashedNotesWithTags: Flow<List<NoteWithTags>> = noteDao.observeTrashedNotesWithTags()
    val allTagsWithCount: Flow<List<TagWithCount>> = noteDao.observeTagsWithCount()
    val allTags: Flow<List<TagEntity>> = noteDao.observeAllTags()
    val allSubjects: Flow<List<SubjectEntity>> = noteDao.observeAllSubjects()
    val allExams: Flow<List<ExamCountdownEntity>> = noteDao.observeAllExams()
    val allStudySessions: Flow<List<StudySessionEntity>> = noteDao.observeAllStudySessions()

    fun observeNoteById(noteId: Long): Flow<NoteWithTags?> = noteDao.observeNoteWithTagsById(noteId)

    suspend fun getNoteSnapshot(noteId: Long): NoteWithTags? = noteDao.getNoteWithTagsById(noteId)

    suspend fun saveNote(
        existingId: Long,
        title: String,
        subjectId: String,
        content: String,
        formulas: List<FormulaBlock>,
        imagePaths: List<String>,
        tagIds: List<Long>,
        isPinned: Boolean,
        needsReview: Boolean,
        paperStyle: Int,
        createdAt: Long? = null,
        updatedAt: Long? = null,
        deletedAt: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        val cleanFormulas = formulas
            .filter { it.title.isNotBlank() || it.expression.isNotBlank() || it.variablesNote.isNotBlank() }
        val note = NoteEntity(
            id = existingId,
            title = title.trim(),
            subject = subjectId,
            content = content,
            formulas = cleanFormulas,
            imagePaths = imagePaths.distinct(),
            isPinned = isPinned,
            needsReview = needsReview,
            paperStyle = paperStyle,
            createdAt = createdAt ?: now,
            updatedAt = updatedAt ?: now,
            deletedAt = deletedAt
        )
        return noteDao.saveNoteWithTags(note, tagIds)
    }

    suspend fun softDeleteNote(noteId: Long) {
        noteDao.softDeleteNoteById(noteId, System.currentTimeMillis())
    }

    suspend fun restoreNoteFromTrash(noteId: Long) {
        noteDao.restoreNoteFromTrash(noteId, System.currentTimeMillis())
    }

    suspend fun permanentlyDeleteNote(noteId: Long) {
        val candidateImages = database.withTransaction {
            val snapshot = noteDao.getNoteWithTagsById(noteId)
            noteDao.permanentlyDeleteNoteById(noteId)
            snapshot?.note?.imagePaths.orEmpty()
        }
        deleteUnreferencedImages(candidateImages)
    }

    suspend fun emptyTrash() {
        val candidateImages = database.withTransaction {
            val trashed = noteDao.getTrashedNotesSnapshot()
            noteDao.emptyTrashNotes()
            trashed.flatMap { it.note.imagePaths }.distinct()
        }
        deleteUnreferencedImages(candidateImages)
    }

    suspend fun purgeExpiredTrash(retentionDays: Long = 30L, nowMillis: Long = System.currentTimeMillis()): Int {
        val cutoff = nowMillis - TimeUnit.DAYS.toMillis(retentionDays)
        val (deletedCount, candidateImages) = database.withTransaction {
            val expired = noteDao.getExpiredTrashedNotes(cutoff)
            if (expired.isEmpty()) {
                0 to emptyList()
            } else {
                noteDao.deleteExpiredTrashedNotes(cutoff)
                expired.size to expired.flatMap { it.imagePaths }.distinct()
            }
        }
        deleteUnreferencedImages(candidateImages)
        return deletedCount
    }

    /**
     * Deletes image files that no note (active or in the trash) references any more —
     * e.g. an image removed in the editor, or added to a note that was then discarded.
     * Files newer than [minAgeMillis] are kept so an image that was just picked and is
     * not yet auto-saved is never touched.
     */
    suspend fun purgeOrphanedImages(
        minAgeMillis: Long = TimeUnit.HOURS.toMillis(24),
        nowMillis: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        val files = NoteImageStorage.imagesDir(appContext).listFiles()?.filter { it.isFile }.orEmpty()
        if (files.isEmpty()) return@withContext 0
        val stillUsed = noteDao.getAllNotesWithTagsSnapshot()
            .flatMap { it.note.imagePaths }
            .toHashSet()
        val orphaned = files.filter { f ->
            f.name !in stillUsed && nowMillis - f.lastModified() >= minAgeMillis
        }
        orphaned.count { runCatching { it.delete() }.getOrDefault(false) }
    }

    private suspend fun deleteUnreferencedImages(candidates: List<String>) {
        if (candidates.isEmpty()) return
        withContext(Dispatchers.IO) {
            val stillUsed = noteDao.getAllNotesWithTagsSnapshot()
                .flatMap { it.note.imagePaths }
                .toHashSet()
            val orphaned = candidates.distinct().filter { it !in stillUsed }
            if (orphaned.isNotEmpty()) {
                NoteImageStorage.deleteImageFiles(appContext, orphaned)
            }
        }
    }

    suspend fun togglePin(noteId: Long, newPinned: Boolean) {
        noteDao.updatePinStatus(noteId, newPinned, System.currentTimeMillis())
    }

    suspend fun toggleNeedsReview(noteId: Long, newNeedsReview: Boolean) {
        noteDao.updateNeedsReviewStatus(noteId, newNeedsReview, System.currentTimeMillis())
    }

    suspend fun toggleChecklistLine(noteId: Long, lineIndex: Int) {
        database.withTransaction {
            val noteWithTags = noteDao.getNoteWithTagsById(noteId) ?: return@withTransaction
            val updatedContent = SimpleMarkdown.toggleChecklistLine(noteWithTags.note.content, lineIndex)
            if (updatedContent != noteWithTags.note.content) {
                noteDao.updateContent(noteId, updatedContent, System.currentTimeMillis())
            }
        }
    }

    suspend fun rateFormulaSm2(
        noteId: Long,
        formulaId: String,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            val noteWithTags = noteDao.getNoteWithTagsById(noteId) ?: return@withTransaction
            val updated = noteWithTags.note.formulas.map { block ->
                if (block.id == formulaId) Sm2Scheduler.scheduleReview(block, rating, nowMillis) else block
            }
            noteDao.updateFormulas(noteId, updated, nowMillis)
        }
    }

    suspend fun toggleFormulaMastered(noteId: Long, formulaId: String) {
        database.withTransaction {
            val noteWithTags = noteDao.getNoteWithTagsById(noteId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val updated = noteWithTags.note.formulas.map { block ->
                if (block.id == formulaId) {
                    val nextRating = if (block.mastered) Sm2Rating.HARD else Sm2Rating.GOOD
                    Sm2Scheduler.scheduleReview(block, nextRating, now)
                } else {
                    block
                }
            }
            noteDao.updateFormulas(noteId, updated, now)
        }
    }

    // Tags
    suspend fun createOrGetTag(name: String, colorIndex: Int): Long {
        val clean = name.trim().removePrefix("#").trim()
        require(clean.isNotEmpty()) { "Tag name must not be empty" }
        return noteDao.getOrCreateTag(clean, colorIndex)
    }

    suspend fun updateTag(tag: TagEntity) {
        val clean = tag.name.trim().removePrefix("#").trim()
        if (clean.isNotEmpty()) {
            noteDao.updateTag(tag.copy(name = clean))
        }
    }

    suspend fun deleteTag(tagId: Long) {
        noteDao.deleteTagById(tagId)
    }

    // Subjects (Built-in + User Custom Subjects)
    suspend fun createCustomSubject(name: String, colorHex: Long): SubjectEntity {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Subject name must not be empty" }
        return database.withTransaction {
            val existing = noteDao.findSubjectByName(clean)
            if (existing != null) return@withTransaction existing
            val nextSort = noteDao.getMaxSubjectSortOrder() + 1
            val entity = SubjectEntity(
                id = "custom_${UUID.randomUUID().toString().take(8)}",
                name = clean,
                colorHex = colorHex,
                isDefault = false,
                sortOrder = nextSort,
                createdAt = System.currentTimeMillis()
            )
            noteDao.insertSubject(entity)
            entity
        }
    }

    suspend fun updateCustomSubject(subject: SubjectEntity) {
        val clean = subject.name.trim()
        if (clean.isNotEmpty() && !subject.isDefault) {
            noteDao.updateSubject(subject.copy(name = clean))
        }
    }

    suspend fun deleteCustomSubject(subjectId: String) {
        noteDao.deleteCustomSubjectSafely(subjectId)
    }

    // Exam Countdowns
    suspend fun addExam(title: String, subjectId: String, examDateMillis: Long, note: String): Long {
        return noteDao.insertExam(
            ExamCountdownEntity(
                title = title.trim(),
                subject = subjectId,
                examDateMillis = examDateMillis,
                note = note.trim()
            )
        )
    }

    suspend fun deleteExam(examId: Long) {
        noteDao.deleteExamById(examId)
    }

    suspend fun getAllExamsSnapshot(): List<ExamCountdownEntity> = noteDao.getAllExamsSnapshot()

    // Study Focus Sessions
    suspend fun recordStudySession(subjectId: String, durationMinutes: Int): Long {
        return noteDao.insertStudySession(
            StudySessionEntity(
                subject = subjectId,
                durationMinutes = durationMinutes
            )
        )
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) { buildBackupJson(embedImages = true) }

    /**
     * Writes a ZIP backup: `backup.json` (data only) plus every referenced image as its own
     * `images/<name>` entry, streamed file by file. Unlike the legacy single-JSON format this
     * never holds all images in memory, so backups with many photos can still be restored.
     */
    suspend fun exportBackupZip(out: OutputStream) = withContext(Dispatchers.IO) {
        val json = buildBackupJson(embedImages = false)
        val imageNames = noteDao.getAllNotesWithTagsSnapshot()
            .flatMap { it.note.imagePaths }
            .mapNotNull { NoteImageStorage.sanitizeFileName(it) }
            .distinct()
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(ZIP_JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (name in imageNames) {
                val file = NoteImageStorage.resolveImageFile(appContext, name)
                if (!file.isFile) continue
                zip.putNextEntry(ZipEntry(ZIP_IMAGES_PREFIX + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Restores either a ZIP backup ([exportBackupZip]) or a legacy JSON backup
     * ([exportBackupJson]), detected from the first bytes of the file.
     */
    suspend fun importBackupStream(input: InputStream): Result<BackupImportSummary> = withContext(Dispatchers.IO) {
        val buffered = BufferedInputStream(input)
        val isZip = try {
            buffered.mark(4)
            val b0 = buffered.read()
            val b1 = buffered.read()
            buffered.reset()
            b0 == 'P'.code && b1 == 'K'.code
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        if (!isZip) {
            val raw = try {
                readLimitedText(buffered)
            } catch (e: BackupTooLargeException) {
                return@withContext Result.failure(e)
            } catch (e: OutOfMemoryError) {
                return@withContext Result.failure(BackupTooLargeException())
            }
            return@withContext importBackupJson(raw)
        }

        val workDir = File(appContext.cacheDir, "backup_import_${UUID.randomUUID()}")
        val imagesDir = File(workDir, "images").apply { mkdirs() }
        try {
            var json: String? = null
            ZipInputStream(buffered).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    when {
                        entry.name == ZIP_JSON_ENTRY -> json = readLimitedText(zip)
                        entry.name.startsWith(ZIP_IMAGES_PREFIX) -> {
                            // Only a plain file name is accepted (no "../" paths out of the folder).
                            val name = NoteImageStorage.sanitizeFileName(entry.name.removePrefix(ZIP_IMAGES_PREFIX))
                            if (name != null && name == entry.name.removePrefix(ZIP_IMAGES_PREFIX)) {
                                copyLimited(zip, File(imagesDir, name))
                            }
                        }
                    }
                }
            }
            val data = json ?: return@withContext Result.failure(InvalidBackupFormatException())
            importBackupJson(data, imagesDir)
        } catch (e: OutOfMemoryError) {
            Result.failure(BackupTooLargeException())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun readLimitedText(input: InputStream): String {
        val reader = InputStreamReader(input, Charsets.UTF_8)
        val sb = StringBuilder()
        val buf = CharArray(8192)
        while (true) {
            val read = reader.read(buf)
            if (read < 0) break
            if (sb.length + read > MAX_BACKUP_CHARS) throw BackupTooLargeException()
            sb.appendRange(buf, 0, read)
        }
        return sb.toString()
    }

    private fun copyLimited(input: InputStream, target: File) {
        var total = 0L
        val buf = ByteArray(16 * 1024)
        target.outputStream().use { out ->
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                total += read
                if (total > MAX_ZIP_IMAGE_BYTES) {
                    out.close()
                    target.delete()
                    return
                }
                out.write(buf, 0, read)
            }
        }
    }

    private suspend fun buildBackupJson(embedImages: Boolean): String {
        val snapshot = noteDao.getAllNotesWithTagsSnapshot()
        val subjectsSnapshot = noteDao.getAllSubjectsSnapshot()
        val examsSnapshot = noteDao.getAllExamsSnapshot()
        val sessionsSnapshot = noteDao.getAllStudySessionsSnapshot()

        val root = JSONObject()
        root.put("app", "SmutNoteFormula")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val subjectsArray = JSONArray()
        for (s in subjectsSnapshot) {
            subjectsArray.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("colorHex", s.colorHex)
                    put("isDefault", s.isDefault)
                    put("sortOrder", s.sortOrder)
                    put("createdAt", s.createdAt)
                }
            )
        }
        root.put("subjects", subjectsArray)

        val embeddedImages = JSONObject()
        val notesArray = JSONArray()
        for (item in snapshot) {
            val n = item.note
            val noteObj = JSONObject().apply {
                put("title", n.title)
                put("subject", n.subject)
                put("content", n.content)
                put("isPinned", n.isPinned)
                put("needsReview", n.needsReview)
                put("paperStyle", n.paperStyle)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                if (n.deletedAt != null) {
                    put("deletedAt", n.deletedAt)
                }

                val imgArr = JSONArray()
                for (imgName in n.imagePaths) {
                    imgArr.put(imgName)
                    if (embedImages && !embeddedImages.has(imgName)) {
                        NoteImageStorage.encodeImageBase64(appContext, imgName)?.let { b64 ->
                            embeddedImages.put(imgName, b64)
                        }
                    }
                }
                put("imagePaths", imgArr)

                val formulasArr = JSONArray()
                for (f in n.formulas) {
                    formulasArr.put(
                        JSONObject().apply {
                            put("id", f.id)
                            put("title", f.title)
                            put("expression", f.expression)
                            put("variablesNote", f.variablesNote)
                            put("mastered", f.mastered)
                            put("repetitions", f.repetitions)
                            put("intervalDays", f.intervalDays)
                            put("easeFactor", f.easeFactor)
                            put("nextReviewAtMillis", f.nextReviewAtMillis)
                            put("lastReviewedAtMillis", f.lastReviewedAtMillis)
                            put("blockType", f.blockType)
                        }
                    )
                }
                put("formulas", formulasArr)

                val tagsArr = JSONArray()
                for (t in item.tags) {
                    tagsArr.put(
                        JSONObject().apply {
                            put("name", t.name)
                            put("colorIndex", t.colorIndex)
                        }
                    )
                }
                put("tags", tagsArr)
            }
            notesArray.put(noteObj)
        }
        root.put("notes", notesArray)
        root.put("embedded_images", embeddedImages)

        val examsArray = JSONArray()
        for (exam in examsSnapshot) {
            examsArray.put(
                JSONObject().apply {
                    put("title", exam.title)
                    put("subject", exam.subject)
                    put("examDateMillis", exam.examDateMillis)
                    put("note", exam.note)
                    put("createdAt", exam.createdAt)
                }
            )
        }
        root.put("exam_countdowns", examsArray)

        val sessionsArray = JSONArray()
        for (session in sessionsSnapshot) {
            sessionsArray.put(
                JSONObject().apply {
                    put("subject", session.subject)
                    put("durationMinutes", session.durationMinutes)
                    put("completedAt", session.completedAt)
                }
            )
        }
        root.put("study_sessions", sessionsArray)

        return root.toString(2)
    }

    private data class ParsedImportNote(
        val title: String,
        val rawSubject: String,
        val content: String,
        val isPinned: Boolean,
        val needsReview: Boolean,
        val paperStyle: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val deletedAt: Long?,
        val imageNames: List<String>,
        val formulas: List<FormulaBlock>,
        val tags: List<Pair<String, Int>>
    )

    private data class ParsedImportExam(
        val title: String,
        val rawSubject: String,
        val examDateMillis: Long,
        val noteText: String,
        val createdAt: Long
    )

    private data class ParsedImportSession(
        val rawSubject: String,
        val durationMinutes: Int,
        val completedAt: Long
    )

    suspend fun importBackupJson(
        rawJson: String,
        extractedImagesDir: File? = null
    ): Result<BackupImportSummary> = withContext(Dispatchers.IO) {
        if (rawJson.length > MAX_BACKUP_CHARS) {
            return@withContext Result.failure(BackupTooLargeException())
        }
        val newlyWrittenImages = mutableListOf<String>()
        try {
            val root = JSONObject(rawJson)
            val notesArray = root.optJSONArray("notes")
                ?: return@withContext Result.failure(InvalidBackupFormatException())

            val subjectsArray = root.optJSONArray("subjects")
            val embeddedImages = root.optJSONObject("embedded_images")
            val examsArray = root.optJSONArray("exam_countdowns") ?: root.optJSONArray("exams")
            val sessionsArray = root.optJSONArray("study_sessions") ?: root.optJSONArray("studySessions")

            val now = System.currentTimeMillis()
            val untitledFallback = appContext.getString(R.string.editor_untitled_note)

            // Phase A: Strictly parse and validate all backup structures in memory before mutating DB or disk.
            val parsedSubjects = buildList {
                if (subjectsArray != null) {
                    for (i in 0 until subjectsArray.length()) {
                        val sObj = subjectsArray.getJSONObject(i)
                        val sId = sObj.optString("id").trim()
                        val sName = sObj.optString("name").trim()
                        if (sId.isEmpty() || sName.isEmpty()) continue
                        add(
                            SubjectEntity(
                                id = sId,
                                name = sName,
                                colorHex = sObj.optLong("colorHex", 0xFF2563EB),
                                isDefault = sObj.optBoolean("isDefault", false),
                                sortOrder = sObj.optInt("sortOrder", 10 + i),
                                createdAt = sObj.optLong("createdAt", now)
                            )
                        )
                    }
                }
            }

            val parsedNotes = buildList {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val rawTitle = obj.optString("title").trim()
                    val content = obj.optString("content")
                    val imgArr = obj.optJSONArray("imagePaths") ?: JSONArray()
                    val formulasArr = obj.optJSONArray("formulas") ?: JSONArray()

                    if (rawTitle.isEmpty() && content.isBlank() && formulasArr.length() == 0 && imgArr.length() == 0) {
                        continue
                    }
                    val title = rawTitle.ifEmpty { untitledFallback }

                    val createdAt = if (obj.has("createdAt") && !obj.isNull("createdAt")) obj.getLong("createdAt") else now
                    val updatedAt = if (obj.has("updatedAt") && !obj.isNull("updatedAt")) obj.optLong("updatedAt", createdAt) else createdAt
                    val deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null

                    val imageNames = buildList {
                        for (m in 0 until imgArr.length()) {
                            val imgName = NoteImageStorage.sanitizeFileName(imgArr.optString(m))
                            if (imgName != null) add(imgName)
                        }
                    }

                    val formulas = buildList {
                        for (j in 0 until formulasArr.length()) {
                            val fObj = formulasArr.getJSONObject(j)
                            val mastered = fObj.optBoolean("mastered", false)
                            val rawType = fObj.optString("blockType")
                            val resolvedType = if (rawType == FormulaBlock.BLOCK_TYPE_FLASHCARD) {
                                FormulaBlock.BLOCK_TYPE_FLASHCARD
                            } else {
                                FormulaBlock.BLOCK_TYPE_FORMULA
                            }
                            add(
                                FormulaBlock(
                                    id = fObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                    title = fObj.optString("title"),
                                    expression = fObj.optString("expression"),
                                    variablesNote = fObj.optString("variablesNote"),
                                    mastered = mastered,
                                    repetitions = fObj.optInt("repetitions", if (mastered) 1 else 0),
                                    intervalDays = fObj.optInt("intervalDays", if (mastered) 6 else 0),
                                    easeFactor = fObj.optDouble("easeFactor", 2.5),
                                    nextReviewAtMillis = fObj.optLong("nextReviewAtMillis", 0L),
                                    lastReviewedAtMillis = fObj.optLong("lastReviewedAtMillis", 0L),
                                    blockType = resolvedType
                                )
                            )
                        }
                    }

                    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = buildList {
                        for (k in 0 until tagsArr.length()) {
                            val tObj = tagsArr.getJSONObject(k)
                            val tName = tObj.optString("name").trim()
                            if (tName.isNotEmpty()) {
                                add(tName to tObj.optInt("colorIndex", 0))
                            }
                        }
                    }

                    add(
                        ParsedImportNote(
                            title = title,
                            rawSubject = obj.optString("subject").trim(),
                            content = content,
                            isPinned = obj.optBoolean("isPinned", false),
                            needsReview = obj.optBoolean("needsReview", false),
                            paperStyle = obj.optInt("paperStyle", 0).coerceIn(0, 2),
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            deletedAt = deletedAt,
                            imageNames = imageNames,
                            formulas = formulas,
                            tags = tags
                        )
                    )
                }
            }

            val parsedExams = buildList {
                if (examsArray != null) {
                    for (i in 0 until examsArray.length()) {
                        val eObj = examsArray.getJSONObject(i)
                        val title = eObj.optString("title").trim()
                        if (title.isEmpty() || !eObj.has("examDateMillis") || eObj.isNull("examDateMillis")) continue
                        add(
                            ParsedImportExam(
                                title = title,
                                rawSubject = eObj.optString("subject").trim(),
                                examDateMillis = eObj.getLong("examDateMillis"),
                                noteText = eObj.optString("note").trim(),
                                createdAt = eObj.optLong("createdAt", now)
                            )
                        )
                    }
                }
            }

            val parsedSessions = buildList {
                if (sessionsArray != null) {
                    for (i in 0 until sessionsArray.length()) {
                        val sObj = sessionsArray.getJSONObject(i)
                        if (!sObj.has("durationMinutes") || !sObj.has("completedAt")) continue
                        val durationMinutes = sObj.getInt("durationMinutes")
                        val completedAt = sObj.getLong("completedAt")
                        if (durationMinutes <= 0) continue
                        add(
                            ParsedImportSession(
                                rawSubject = sObj.optString("subject").trim(),
                                durationMinutes = durationMinutes,
                                completedAt = completedAt
                            )
                        )
                    }
                }
            }

            // Phase B: Persist atomically inside a Room transaction.
            val summary = database.withTransaction {
                var importedSubjects = 0
                for (subject in parsedSubjects) {
                    if (noteDao.getSubjectById(subject.id) == null) {
                        noteDao.insertSubject(subject)
                        importedSubjects++
                    }
                }

                val validSubjectIds = noteDao.getAllSubjectsSnapshot().map { it.id }.toMutableSet()

                var importedNotes = 0
                var skippedNotes = 0
                var importedExams = 0
                var skippedExams = 0
                var importedSessions = 0
                var skippedSessions = 0

                for (n in parsedNotes) {
                    if (noteDao.countNotesByTitleAndCreatedAt(n.title, n.createdAt) > 0) {
                        skippedNotes++
                        continue
                    }

                    val imagePaths = buildList {
                        for (imgName in n.imageNames) {
                            val b64 = embeddedImages?.optString(imgName).orEmpty()
                            val targetFile = NoteImageStorage.resolveImageFile(appContext, imgName)
                            if (b64.isNotEmpty()) {
                                val existedBefore = targetFile.exists()
                                if (NoteImageStorage.restoreImageFromBase64(appContext, imgName, b64)) {
                                    if (!existedBefore) newlyWrittenImages.add(imgName)
                                    add(imgName)
                                }
                            } else if (extractedImagesDir != null &&
                                NoteImageStorage.sanitizeFileName(imgName) == imgName &&
                                File(extractedImagesDir, imgName).isFile
                            ) {
                                val existedBefore = targetFile.exists()
                                if (runCatching {
                                        File(extractedImagesDir, imgName).copyTo(targetFile, overwrite = true)
                                    }.isSuccess
                                ) {
                                    if (!existedBefore) newlyWrittenImages.add(imgName)
                                    add(imgName)
                                }
                            } else if (targetFile.exists()) {
                                add(imgName)
                            }
                        }
                    }

                    val tagIds = buildList {
                        for ((tName, colorIdx) in n.tags) {
                            val tid = noteDao.getOrCreateTag(tName, colorIdx)
                            if (tid > 0L) add(tid)
                        }
                    }

                    saveNote(
                        existingId = 0L,
                        title = n.title,
                        subjectId = resolveImportSubjectId(n.rawSubject, validSubjectIds),
                        content = n.content,
                        formulas = n.formulas,
                        imagePaths = imagePaths,
                        tagIds = tagIds,
                        isPinned = n.isPinned,
                        needsReview = n.needsReview,
                        paperStyle = n.paperStyle,
                        createdAt = n.createdAt,
                        updatedAt = n.updatedAt,
                        deletedAt = n.deletedAt
                    )
                    importedNotes++
                }

                for (exam in parsedExams) {
                    if (noteDao.countExamsByTitleAndDate(exam.title, exam.examDateMillis) > 0) {
                        skippedExams++
                        continue
                    }
                    noteDao.insertExam(
                        ExamCountdownEntity(
                            title = exam.title,
                            subject = resolveImportSubjectId(exam.rawSubject, validSubjectIds),
                            examDateMillis = exam.examDateMillis,
                            note = exam.noteText,
                            createdAt = exam.createdAt
                        )
                    )
                    importedExams++
                }

                for (session in parsedSessions) {
                    val subjectId = resolveImportSubjectId(session.rawSubject, validSubjectIds)
                    if (noteDao.countStudySessions(subjectId, session.durationMinutes, session.completedAt) > 0) {
                        skippedSessions++
                        continue
                    }
                    noteDao.insertStudySession(
                        StudySessionEntity(
                            subject = subjectId,
                            durationMinutes = session.durationMinutes,
                            completedAt = session.completedAt
                        )
                    )
                    importedSessions++
                }

                BackupImportSummary(
                    importedNotes = importedNotes,
                    skippedNotes = skippedNotes,
                    importedExams = importedExams,
                    skippedExams = skippedExams,
                    importedSessions = importedSessions,
                    skippedSessions = skippedSessions,
                    importedSubjects = importedSubjects
                )
            }
            Result.success(summary)
        } catch (e: OutOfMemoryError) {
            NoteImageStorage.deleteImageFiles(appContext, newlyWrittenImages)
            Result.failure(BackupTooLargeException())
        } catch (e: Exception) {
            NoteImageStorage.deleteImageFiles(appContext, newlyWrittenImages)
            Result.failure(e)
        }
    }

    private fun resolveImportSubjectId(rawSubject: String, validIds: Set<String>): String {
        if (rawSubject in validIds) return rawSubject
        return SubjectCategory.fromName(rawSubject).name
    }

    companion object {
        const val BACKUP_VERSION = 3
        const val MAX_BACKUP_CHARS = 25_000_000
        private const val MAX_ZIP_IMAGE_BYTES = 15L * 1024L * 1024L
        private const val ZIP_JSON_ENTRY = "backup.json"
        private const val ZIP_IMAGES_PREFIX = "images/"
    }
}
