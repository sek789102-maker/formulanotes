package com.sek789102.jotwai.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.sek789102.jotwai.data.local.AppDatabase
import com.sek789102.jotwai.data.local.ChecklistJson
import com.sek789102.jotwai.data.local.Converters
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteLabelCrossRef
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteVersionEntity
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.util.NoteImageStorage
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
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class InvalidBackupException : IllegalArgumentException("Not a valid backup file")
class BackupTooLargeException : IllegalArgumentException("Backup file is too large")

data class ImportSummary(val imported: Int, val skipped: Int)

/** Receives reminder changes so alarms stay in sync with the database. */
interface ReminderSink {
    fun schedule(note: NoteEntity) {}
    fun cancel(noteId: Long) {}
}

class NotesRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val reminders: ReminderSink = object : ReminderSink {},
    /** Called after any change that home-screen widgets show. */
    private val onDataChanged: suspend () -> Unit = {}
) {
    private val dao = db.noteDao()
    private val converters = Converters()

    val notes: Flow<List<NoteWithLabels>> = dao.observeAllNotes()
    val folders: Flow<List<FolderEntity>> = dao.observeFolders()
    val labels: Flow<List<LabelEntity>> = dao.observeLabels()

    fun observeNote(id: Long): Flow<NoteWithLabels?> = dao.observeNote(id)
    fun observeVersions(noteId: Long) = dao.observeVersions(noteId)
    suspend fun getNote(id: Long): NoteWithLabels? = dao.getNote(id)
    suspend fun getAllNotes(): List<NoteWithLabels> = dao.getAllNotes()

    // ------------------------------------------------------------------ notes

    suspend fun createNote(note: NoteEntity, labelIds: Collection<Long> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = db.withTransaction {
            val newId = dao.insertNote(note.copy(id = 0L, createdAt = now, updatedAt = now))
            labelIds.forEach { dao.insertNoteLabel(NoteLabelCrossRef(newId, it)) }
            newId
        }
        dao.getNoteEntity(id)?.let { syncReminder(it) }
        onDataChanged()
        return id
    }

    /** Saves editor content. [touch] = false keeps the "edited" time (e.g. only ticking a box). */
    suspend fun saveNote(note: NoteEntity, touch: Boolean = true) {
        val saved = if (touch) note.copy(updatedAt = System.currentTimeMillis()) else note
        dao.updateNote(saved)
        syncReminder(saved)
        onDataChanged()
    }

    /**
     * Saves only the text/checklist/images of a note, re-reading everything else from the
     * database so a reminder or colour changed elsewhere meanwhile is never overwritten.
     */
    suspend fun saveContent(
        id: Long,
        title: String,
        content: String,
        type: NoteType,
        checklist: List<ChecklistItem>,
        imagePaths: List<String>
    ): NoteEntity? {
        val cur = dao.getNoteEntity(id) ?: return null
        if (cur.title == title && cur.content == content && cur.type == type &&
            cur.checklist == checklist && cur.imagePaths == imagePaths
        ) return cur
        val updated = cur.copy(
            title = title.take(MAX_TITLE),
            content = content,
            type = type,
            checklist = checklist,
            imagePaths = imagePaths,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateNote(updated)
        onDataChanged()
        return updated
    }

    private suspend fun update(ids: Collection<Long>, touch: Boolean = false, change: (NoteEntity) -> NoteEntity) {
        val now = System.currentTimeMillis()
        val changed = db.withTransaction {
            ids.mapNotNull { id ->
                dao.getNoteEntity(id)?.let { old ->
                    change(old).let { if (touch) it.copy(updatedAt = now) else it }.also { dao.updateNote(it) }
                }
            }
        }
        changed.forEach { syncReminder(it) }
        onDataChanged()
    }

    suspend fun setPinned(ids: Collection<Long>, pinned: Boolean) =
        update(ids) { it.copy(isPinned = pinned, isArchived = if (pinned) false else it.isArchived) }

    suspend fun setArchived(ids: Collection<Long>, archived: Boolean) =
        update(ids) { it.copy(isArchived = archived, isPinned = if (archived) false else it.isPinned) }

    suspend fun setColor(ids: Collection<Long>, color: Int) = update(ids) { it.copy(color = color) }

    suspend fun setLocked(ids: Collection<Long>, locked: Boolean) = update(ids) { it.copy(isLocked = locked) }

    suspend fun moveToFolder(ids: Collection<Long>, folderId: Long?) = update(ids) { it.copy(folderId = folderId) }

    suspend fun moveToTrash(ids: Collection<Long>) {
        val now = System.currentTimeMillis()
        update(ids) { it.copy(deletedAt = now, isPinned = false) }
    }

    suspend fun restoreFromTrash(ids: Collection<Long>) = update(ids) { it.copy(deletedAt = null) }

    suspend fun setReminder(id: Long, at: Long?, repeat: RepeatRule = RepeatRule.NONE) =
        update(listOf(id)) { it.copy(reminderAt = at, reminderRepeat = if (at == null) RepeatRule.NONE else repeat) }

    /** After a reminder fires: move a repeating one to its next time, or clear a one-off. */
    suspend fun advanceReminder(id: Long, now: Long = System.currentTimeMillis()) {
        val note = dao.getNoteEntity(id) ?: return
        val at = note.reminderAt ?: return
        val next = ReminderMath.nextOccurrence(at, note.reminderRepeat, now)
        update(listOf(id)) { it.copy(reminderAt = next, reminderRepeat = if (next == null) RepeatRule.NONE else it.reminderRepeat) }
    }

    suspend fun duplicate(id: Long): Long? {
        val src = dao.getNote(id) ?: return null
        val copyImages = src.note.imagePaths.mapNotNull { name ->
            val from = NoteImageStorage.resolveImageFile(context, name)
            if (!from.isFile) return@mapNotNull null
            val newName = "note_${UUID.randomUUID()}.${from.extension.ifBlank { "jpg" }}"
            runCatching { from.copyTo(NoteImageStorage.resolveImageFile(context, newName)) }.getOrNull()?.let { newName }
        }
        return createNote(
            src.note.copy(
                id = 0L,
                checklist = src.note.checklist.map { it.copy(id = UUID.randomUUID().toString()) },
                imagePaths = copyImages,
                reminderAt = null,
                reminderRepeat = RepeatRule.NONE,
                isPinned = false
            ),
            src.labels.map { it.id }
        )
    }

    suspend fun deleteForever(ids: Collection<Long>) {
        val images = db.withTransaction {
            ids.flatMap { id ->
                val n = dao.getNoteEntity(id)
                val versionImages = dao.getVersions(id).flatMap { it.imagePaths }
                dao.deleteNote(id)
                n?.imagePaths.orEmpty() + versionImages
            }
        }
        ids.forEach { reminders.cancel(it) }
        deleteUnreferencedImages(images)
        onDataChanged()
    }

    suspend fun emptyTrash() = deleteForever(dao.getTrashedNotes().map { it.id })

    suspend fun purgeTrash(retentionDays: Long = TRASH_RETENTION_DAYS, now: Long = System.currentTimeMillis()): Int {
        val expired = dao.getTrashedBefore(now - TimeUnit.DAYS.toMillis(retentionDays))
        if (expired.isNotEmpty()) deleteForever(expired.map { it.id })
        return expired.size
    }

    /** Deletes the note if it holds nothing (e.g. opened, then closed without typing). */
    suspend fun deleteIfBlank(id: Long): Boolean {
        val n = dao.getNoteEntity(id) ?: return false
        if (!n.isBlank) return false
        deleteForever(listOf(id))
        return true
    }

    // ------------------------------------------------------------- labels/folders

    suspend fun createLabel(name: String): Long? {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return null
        dao.getLabelByName(clean)?.let { return it.id }
        val id = dao.insertLabel(LabelEntity(name = clean))
        return if (id > 0) id else dao.getLabelByName(clean)?.id
    }

    suspend fun renameLabel(id: Long, name: String): Boolean {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return false
        val existing = dao.getLabelByName(clean)
        if (existing != null && existing.id != id) return false
        dao.updateLabel(LabelEntity(id = id, name = clean))
        onDataChanged()
        return true
    }

    suspend fun deleteLabel(id: Long) {
        dao.deleteLabel(id)
        onDataChanged()
    }

    suspend fun setNoteLabel(noteId: Long, labelId: Long, attached: Boolean) {
        if (attached) dao.insertNoteLabel(NoteLabelCrossRef(noteId, labelId)) else dao.deleteNoteLabel(noteId, labelId)
        onDataChanged()
    }

    suspend fun addLabelToNotes(ids: Collection<Long>, labelId: Long) {
        db.withTransaction { ids.forEach { dao.insertNoteLabel(NoteLabelCrossRef(it, labelId)) } }
        onDataChanged()
    }

    suspend fun createFolder(name: String): Long? {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return null
        val order = (dao.getFolders().maxOfOrNull { it.sortOrder } ?: 0) + 1
        return dao.insertFolder(FolderEntity(name = clean, sortOrder = order))
    }

    suspend fun renameFolder(folder: FolderEntity, name: String): Boolean {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return false
        dao.updateFolder(folder.copy(name = clean))
        return true
    }

    /** Deletes the folder only; its notes stay and move to "no folder". */
    suspend fun deleteFolder(id: Long) {
        db.withTransaction {
            dao.clearFolder(id)
            dao.deleteFolder(id)
        }
        onDataChanged()
    }

    // ---------------------------------------------------------------- versions

    /**
     * Stores [note]'s current state as a version, unless it equals the newest stored version.
     * Keeps the newest [MAX_VERSIONS] per note.
     */
    suspend fun snapshotVersion(note: NoteEntity) {
        if (note.isBlank) return
        val existing = dao.getVersions(note.id)
        val latest = existing.firstOrNull()
        if (latest != null && latest.title == note.title && latest.content == note.content &&
            latest.checklist == note.checklist && latest.imagePaths == note.imagePaths && latest.type == note.type
        ) return
        dao.insertVersion(
            NoteVersionEntity(
                noteId = note.id,
                title = note.title,
                content = note.content,
                type = note.type,
                checklist = note.checklist,
                imagePaths = note.imagePaths,
                savedAt = note.updatedAt
            )
        )
        val overflow = (existing.size + 1) - MAX_VERSIONS
        if (overflow > 0) {
            val removed = existing.takeLast(overflow)
            removed.forEach { dao.deleteVersion(it.id) }
            deleteUnreferencedImages(removed.flatMap { it.imagePaths })
        }
    }

    /** Puts an old version back. The current state is saved as a version first, so this can be undone. */
    suspend fun restoreVersion(versionId: Long): Boolean {
        val v = dao.getVersion(versionId) ?: return false
        val current = dao.getNoteEntity(v.noteId) ?: return false
        snapshotVersion(current)
        saveNote(
            current.copy(
                title = v.title,
                content = v.content,
                type = v.type,
                checklist = v.checklist,
                imagePaths = v.imagePaths.filter { NoteImageStorage.resolveImageFile(context, it).isFile }
            )
        )
        return true
    }

    // ------------------------------------------------------------------ images

    private suspend fun referencedImages(): Set<String> {
        val inNotes = dao.getAllNotes().flatMap { it.note.imagePaths }
        val inVersions = dao.getAllVersionImagePaths().flatMap { converters.jsonToStrings(it) }
        return (inNotes + inVersions).toHashSet()
    }

    private suspend fun deleteUnreferencedImages(candidates: List<String>) {
        if (candidates.isEmpty()) return
        withContext(Dispatchers.IO) {
            val used = referencedImages()
            NoteImageStorage.deleteImageFiles(context, candidates.distinct().filter { it !in used })
        }
    }

    /**
     * Deletes image files no note or saved version references (e.g. a photo removed in the
     * editor). Files younger than [minAgeMillis] are kept so a just-added photo is never lost.
     */
    suspend fun purgeOrphanedImages(
        minAgeMillis: Long = TimeUnit.HOURS.toMillis(24),
        now: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        val files = NoteImageStorage.imagesDir(context).listFiles()?.filter { it.isFile }.orEmpty()
        if (files.isEmpty()) return@withContext 0
        val used = referencedImages()
        files.filter { it.name !in used && now - it.lastModified() >= minAgeMillis }
            .count { runCatching { it.delete() }.getOrDefault(false) }
    }

    // --------------------------------------------------------------- reminders

    private fun syncReminder(note: NoteEntity) {
        if (note.reminderAt != null && !note.isTrashed) reminders.schedule(note) else reminders.cancel(note.id)
    }

    suspend fun rescheduleAllReminders() {
        dao.getNotesWithReminders().forEach { reminders.schedule(it) }
    }

    // ------------------------------------------------------------------ backup

    /**
     * Writes a ZIP backup: `backup.json` (all notes, folders, labels) and every image as its own
     * `images/<name>` entry, streamed one file at a time so large backups stay restorable.
     */
    suspend fun exportBackup(out: OutputStream) = withContext(Dispatchers.IO) {
        val all = dao.getAllNotes()
        val folderNames = dao.getFolders().associate { it.id to it.name }
        val root = JSONObject()
            .put("app", BACKUP_APP)
            .put("version", BACKUP_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("folders", JSONArray().apply { dao.getFolders().forEach { put(it.name) } })
            .put("labels", JSONArray().apply { dao.getLabels().forEach { put(it.name) } })
        val notesArr = JSONArray()
        for (item in all) {
            val n = item.note
            notesArr.put(
                JSONObject()
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("type", n.type.name)
                    .put("checklist", ChecklistJson.toArray(n.checklist))
                    .put("color", n.color)
                    .put("folder", n.folderId?.let { folderNames[it] } ?: JSONObject.NULL)
                    .put("labels", JSONArray(item.labels.map { it.name }))
                    .put("imagePaths", JSONArray(n.imagePaths))
                    .put("isPinned", n.isPinned)
                    .put("isArchived", n.isArchived)
                    .put("isLocked", n.isLocked)
                    .put("reminderAt", n.reminderAt ?: JSONObject.NULL)
                    .put("reminderRepeat", n.reminderRepeat.name)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
                    .put("deletedAt", n.deletedAt ?: JSONObject.NULL)
            )
        }
        root.put("notes", notesArr)

        val images = all.flatMap { it.note.imagePaths }.mapNotNull { NoteImageStorage.sanitizeFileName(it) }.distinct()
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(ZIP_JSON))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (name in images) {
                val f = NoteImageStorage.resolveImageFile(context, name)
                if (!f.isFile) continue
                zip.putNextEntry(ZipEntry(ZIP_IMAGES + name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Restores a backup made by this app (.zip) or by the earlier "สมุดโน้ตสูตร" app
     * (.zip or .json). Notes already present (same title and creation time) are skipped,
     * so restoring the same file twice never duplicates notes.
     */
    suspend fun importBackup(input: InputStream): Result<ImportSummary> = withContext(Dispatchers.IO) {
        val buffered = BufferedInputStream(input)
        val workDir = File(context.cacheDir, "import_${UUID.randomUUID()}")
        val imagesDir = File(workDir, "images")
        try {
            buffered.mark(4)
            val isZip = buffered.read() == 'P'.code && buffered.read() == 'K'.code
            buffered.reset()
            val json: String = if (isZip) {
                imagesDir.mkdirs()
                var found: String? = null
                ZipInputStream(buffered).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        when {
                            entry.name == ZIP_JSON -> found = readLimitedText(zip)
                            entry.name.startsWith(ZIP_IMAGES) -> {
                                val raw = entry.name.removePrefix(ZIP_IMAGES)
                                // Plain file names only: blocks "../" paths escaping the folder.
                                if (NoteImageStorage.sanitizeFileName(raw) == raw) copyLimited(zip, File(imagesDir, raw))
                            }
                        }
                    }
                }
                found ?: return@withContext Result.failure(InvalidBackupException())
            } else {
                readLimitedText(buffered)
            }
            Result.success(importJson(json, imagesDir.takeIf { isZip }))
        } catch (e: OutOfMemoryError) {
            Result.failure(BackupTooLargeException())
        } catch (e: org.json.JSONException) {
            Result.failure(InvalidBackupException())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private class ParsedNote(
        val note: NoteEntity,
        val folderName: String?,
        val labelNames: List<String>,
        val images: List<String>
    )

    private suspend fun importJson(raw: String, imagesDir: File?): ImportSummary {
        val root = JSONObject(raw)
        val notesArr = root.optJSONArray("notes") ?: throw InvalidBackupException()
        val embedded = root.optJSONObject("embedded_images") // older app: base64 images
        // Older app: notes point at subject ids; map them to subject names → folders.
        val subjectNames = buildMap {
            root.optJSONArray("subjects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val name = o.optString("name").trim()
                    if (id.isNotBlank() && name.isNotBlank()) put(id, name)
                }
            }
        }
        val now = System.currentTimeMillis()

        // Parse everything first so a broken file changes nothing.
        val parsed = buildList {
            for (i in 0 until notesArr.length()) {
                val o = notesArr.optJSONObject(i) ?: continue
                add(parseNote(o, subjectNames, now) ?: continue)
            }
        }

        val written = mutableListOf<String>()
        var imported = 0
        var skipped = 0
        try {
            db.withTransaction {
                for (p in parsed) {
                    if (dao.countByTitleAndCreated(p.note.title, p.note.createdAt) > 0) {
                        skipped++
                        continue
                    }
                    val images = p.images.filter { name ->
                        val target = NoteImageStorage.resolveImageFile(context, name)
                        val existed = target.exists()
                        val ok = when {
                            imagesDir != null && File(imagesDir, name).isFile ->
                                runCatching { File(imagesDir, name).copyTo(target, overwrite = true) }.isSuccess
                            embedded != null && embedded.optString(name).isNotEmpty() ->
                                NoteImageStorage.restoreImageFromBase64(context, name, embedded.optString(name))
                            else -> existed
                        }
                        if (ok && !existed) written += name
                        ok
                    }
                    val folderId = p.folderName?.let { name ->
                        dao.getFolders().firstOrNull { it.name == name }?.id
                            ?: dao.insertFolder(FolderEntity(name = name, sortOrder = dao.getFolders().size + 1))
                    }
                    val id = dao.insertNote(p.note.copy(folderId = folderId, imagePaths = images))
                    for (labelName in p.labelNames) {
                        val labelId = dao.getLabelByName(labelName)?.id ?: dao.insertLabel(LabelEntity(name = labelName))
                        if (labelId > 0) dao.insertNoteLabel(NoteLabelCrossRef(id, labelId))
                    }
                    imported++
                }
            }
        } catch (e: Exception) {
            NoteImageStorage.deleteImageFiles(context, written)
            throw e
        }
        rescheduleAllReminders()
        onDataChanged()
        return ImportSummary(imported, skipped)
    }

    private fun parseNote(o: JSONObject, subjectNames: Map<String, String>, now: Long): ParsedNote? {
        val title = o.optString("title").trim().take(MAX_TITLE)
        var content = o.optString("content")
        // Older app: formula blocks become plain lines at the end of the note.
        o.optJSONArray("formulas")?.let { arr ->
            val lines = buildList {
                for (j in 0 until arr.length()) {
                    val f = arr.optJSONObject(j) ?: continue
                    val t = f.optString("title").trim()
                    val e = f.optString("expression").trim()
                    val v = f.optString("variablesNote").trim()
                    if (t.isEmpty() && e.isEmpty()) continue
                    add(listOf(t, e).filter { it.isNotEmpty() }.joinToString(": ") + if (v.isNotEmpty()) " ($v)" else "")
                }
            }
            if (lines.isNotEmpty()) content = (content.trimEnd() + "\n\n" + lines.joinToString("\n") { "- $it" }).trimStart()
        }
        val checklist = ChecklistJson.fromArray(o.optJSONArray("checklist"))
        val images = buildList {
            o.optJSONArray("imagePaths")?.let { arr ->
                for (j in 0 until arr.length()) NoteImageStorage.sanitizeFileName(arr.optString(j))?.let { add(it) }
            }
        }
        if (title.isEmpty() && content.isBlank() && checklist.isEmpty() && images.isEmpty()) return null

        val labelNames = buildList {
            o.optJSONArray("labels")?.let { arr -> for (j in 0 until arr.length()) add(arr.optString(j)) }
            o.optJSONArray("tags")?.let { arr -> for (j in 0 until arr.length()) arr.optJSONObject(j)?.optString("name")?.let { add(it) } }
        }.map { it.trim().take(MAX_NAME) }.filter { it.isNotEmpty() }.distinct()

        val folderName = when {
            o.has("folder") && !o.isNull("folder") -> o.optString("folder").trim()
            o.has("subject") -> o.optString("subject").let { subjectNames[it] ?: it }.trim()
            else -> ""
        }.take(MAX_NAME).ifEmpty { null }

        fun optLongOrNull(key: String) = if (o.has(key) && !o.isNull(key)) o.optLong(key) else null
        val createdAt = optLongOrNull("createdAt") ?: now
        val type = NoteType.entries.firstOrNull { it.name == o.optString("type") } ?: NoteType.TEXT
        return ParsedNote(
            note = NoteEntity(
                title = title,
                content = content,
                type = if (type == NoteType.CHECKLIST && checklist.isEmpty()) NoteType.TEXT else type,
                checklist = checklist,
                color = o.optInt("color", 0).coerceIn(0, MAX_COLOR_INDEX),
                isPinned = o.optBoolean("isPinned"),
                isArchived = o.optBoolean("isArchived"),
                isLocked = o.optBoolean("isLocked"),
                reminderAt = optLongOrNull("reminderAt"),
                reminderRepeat = RepeatRule.entries.firstOrNull { it.name == o.optString("reminderRepeat") } ?: RepeatRule.NONE,
                createdAt = createdAt,
                updatedAt = optLongOrNull("updatedAt") ?: createdAt,
                deletedAt = optLongOrNull("deletedAt")
            ),
            folderName = folderName,
            labelNames = labelNames,
            images = images
        )
    }

    private fun readLimitedText(input: InputStream): String {
        val reader = InputStreamReader(input, Charsets.UTF_8)
        val sb = StringBuilder()
        val buf = CharArray(8192)
        while (true) {
            val read = reader.read(buf)
            if (read < 0) break
            if (sb.length + read > MAX_JSON_CHARS) throw BackupTooLargeException()
            sb.appendRange(buf, 0, read)
        }
        return sb.toString()
    }

    private fun copyLimited(input: InputStream, target: File) {
        var total = 0L
        val buf = ByteArray(16 * 1024)
        var tooBig = false
        target.outputStream().use { out ->
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                total += read
                if (total > MAX_IMAGE_BYTES) {
                    tooBig = true
                    break
                }
                out.write(buf, 0, read)
            }
        }
        if (tooBig) target.delete()
    }

    companion object {
        const val TRASH_RETENTION_DAYS = 30L
        const val MAX_VERSIONS = 30
        const val MAX_NAME = 60
        const val MAX_TITLE = 500
        const val MAX_COLOR_INDEX = 11
        const val BACKUP_APP = "Jotwai"
        const val BACKUP_VERSION = 1
        const val MAX_JSON_CHARS = 40_000_000
        private const val MAX_IMAGE_BYTES = 20L * 1024L * 1024L
        private const val ZIP_JSON = "backup.json"
        private const val ZIP_IMAGES = "images/"
    }
}

object ReminderMath {
    /** Next time a reminder should fire after [now], or null when it does not repeat. */
    fun nextOccurrence(at: Long, repeat: RepeatRule, now: Long): Long? {
        if (repeat == RepeatRule.NONE) return null
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = at }
        val field = when (repeat) {
            RepeatRule.DAILY -> java.util.Calendar.DAY_OF_YEAR
            RepeatRule.WEEKLY -> java.util.Calendar.WEEK_OF_YEAR
            RepeatRule.MONTHLY -> java.util.Calendar.MONTH
            RepeatRule.YEARLY -> java.util.Calendar.YEAR
            RepeatRule.NONE -> return null
        }
        var guard = 0
        while (cal.timeInMillis <= now && guard < 100_000) {
            cal.add(field, 1)
            guard++
        }
        return cal.timeInMillis
    }
}
