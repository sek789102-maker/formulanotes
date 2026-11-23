package com.sek789102.jotwai.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sek789102.jotwai.JotwaiApp
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.domain.TextEditing
import com.sek789102.jotwai.domain.UndoHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What undo/redo restores. */
data class EditorSnapshot(
    val title: String,
    val body: String,
    val bodySelection: TextRange,
    val type: NoteType,
    val checklist: List<ChecklistItem>,
    val images: List<String>
)

class EditorViewModel(application: Application, val noteId: Long) : AndroidViewModel(application) {
    private val app = application as JotwaiApp
    private val repo = app.repository

    /** Live note (colour, pin, reminder, labels…) — content fields below are the editable copy. */
    val note: StateFlow<NoteWithLabels?> = repo.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var loaded by mutableStateOf(false)
        private set
    var missing by mutableStateOf(false)
        private set
    var title by mutableStateOf(TextFieldValue(""))
        private set
    var body by mutableStateOf(TextFieldValue(""))
        private set
    var type by mutableStateOf(NoteType.TEXT)
        private set
    var checklist by mutableStateOf<List<ChecklistItem>>(emptyList())
        private set
    var images by mutableStateOf<List<String>>(emptyList())
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    /** Item that should take focus (new checklist row). */
    var focusItemId by mutableStateOf<String?>(null)

    private lateinit var history: UndoHistory<EditorSnapshot>
    private var original: NoteEntity? = null
    private var versionSaved = false
    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            val n = repo.getNote(noteId)?.note
            if (n == null) {
                missing = true
                loaded = true
                return@launch
            }
            original = n
            title = TextFieldValue(n.title)
            val content = com.sek789102.jotwai.domain.RichText.repairTags(n.content)
            body = TextFieldValue(content, TextRange(content.length))
            type = n.type
            checklist = n.checklist
            images = n.imagePaths
            history = UndoHistory(snapshot())
            // A brand-new checklist starts with the cursor in its first item.
            if (n.type == NoteType.CHECKLIST && n.isBlank) focusItemId = n.checklist.firstOrNull()?.id
            loaded = true
        }
    }

    private fun snapshot() = EditorSnapshot(title.text, body.text, body.selection, type, checklist, images)

    private fun changed(force: Boolean = false) {
        if (!loaded || missing) return
        history.record(snapshot(), force = force)
        canUndo = history.canUndo
        canRedo = history.canRedo
        scheduleSave()
    }

    // ---- edits ----
    fun onTitleChange(v: TextFieldValue) {
        val textChanged = v.text != title.text
        title = v.copy(text = v.text.replace("\n", " "))
        if (textChanged) changed()
    }

    fun onBodyChange(v: TextFieldValue) {
        val afterDelete = com.sek789102.jotwai.domain.RichText.fixDeletion(body, v)
        val safe = if (afterDelete === v) com.sek789102.jotwai.domain.RichText.fixEdit(body, v) else afterDelete
        val next = TextEditing.continueList(body, safe)
        val textChanged = next.text != body.text
        body = next
        if (textChanged) changed()
    }

    fun applyBodyFormat(transform: (TextFieldValue) -> TextFieldValue) {
        body = transform(body)
        changed(force = true)
    }

    fun setChecklistItemText(id: String, text: String) {
        // Pasting several lines into one item splits them into separate items.
        val lines = text.split('\n')
        checklist = checklist.flatMap { item ->
            if (item.id != id) listOf(item)
            else listOf(item.copy(text = lines.first())) + lines.drop(1).map { ChecklistItem(text = it) }
        }
        if (lines.size > 1) focusItemId = checklist.last { it.text == lines.last() }.id
        changed()
    }

    fun toggleChecklistItem(id: String) {
        checklist = checklist.map { if (it.id == id) it.copy(checked = !it.checked) else it }
        changed(force = true)
    }

    fun addChecklistItem(afterId: String? = null, atBottom: Boolean = true) {
        val item = ChecklistItem()
        checklist = when {
            afterId != null -> {
                val i = checklist.indexOfFirst { it.id == afterId }
                if (i < 0) checklist + item else checklist.toMutableList().apply { add(i + 1, item) }
            }
            atBottom -> {
                // New items go above the ticked ones.
                val firstChecked = checklist.indexOfFirst { it.checked }
                if (firstChecked < 0) checklist + item else checklist.toMutableList().apply { add(firstChecked, item) }
            }
            else -> listOf(item) + checklist
        }
        focusItemId = item.id
        changed(force = true)
    }

    fun removeChecklistItem(id: String) {
        val i = checklist.indexOfFirst { it.id == id }
        checklist = checklist.filterNot { it.id == id }
        focusItemId = checklist.getOrNull((i - 1).coerceAtLeast(0))?.id
        changed(force = true)
    }

    /** Swaps two items (used while dragging an item past its neighbour). */
    fun swapChecklistItems(a: String, b: String, recordStep: Boolean) {
        val list = checklist.toMutableList()
        val i = list.indexOfFirst { it.id == a }
        val j = list.indexOfFirst { it.id == b }
        if (i < 0 || j < 0) return
        java.util.Collections.swap(list, i, j)
        checklist = list
        changed(force = recordStep)
    }

    fun uncheckAll() {
        checklist = checklist.map { it.copy(checked = false) }
        changed(force = true)
    }

    fun deleteCheckedItems() {
        checklist = checklist.filterNot { it.checked }
        changed(force = true)
    }

    /** Text ⇄ checklist. Each non-empty line becomes an item and back. */
    fun convertType() {
        if (type == NoteType.TEXT) {
            val lines = body.text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            checklist = lines.map { line ->
                val m = Regex("""^\[( |x|X)\]\s*""").find(line)
                val clean = line.replace(Regex("""^([-*•]|\d{1,4}[.)])\s+"""), "")
                if (m != null) ChecklistItem(text = line.substring(m.range.last + 1), checked = m.groupValues[1].isNotBlank())
                else ChecklistItem(text = clean)
            }.ifEmpty { listOf(ChecklistItem()) }
            body = TextFieldValue("")
            type = NoteType.CHECKLIST
        } else {
            val text = checklist.filter { it.text.isNotBlank() }
                .joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
            body = TextFieldValue(text, TextRange(text.length))
            checklist = emptyList()
            type = NoteType.TEXT
        }
        changed(force = true)
    }

    fun addImage(name: String) {
        images = images + name
        changed(force = true)
    }

    fun removeImage(name: String) {
        // The file itself stays until the daily clean-up, so undo can bring the photo back.
        images = images - name
        changed(force = true)
    }

    fun undo() = history.undo()?.let { restoreSnapshot(it) }
    fun redo() = history.redo()?.let { restoreSnapshot(it) }

    private fun restoreSnapshot(s: EditorSnapshot) {
        title = TextFieldValue(s.title, TextRange(s.title.length))
        body = TextFieldValue(s.body, s.bodySelection)
        type = s.type
        checklist = s.checklist
        images = s.images
        canUndo = history.canUndo
        canRedo = history.canRedo
        scheduleSave()
    }

    // ---- metadata (saved straight away) ----
    fun setColor(c: Int) = viewModelScope.launch { repo.setColor(listOf(noteId), c) }
    fun setPinned(v: Boolean) = viewModelScope.launch { repo.setPinned(listOf(noteId), v) }
    fun setReminder(at: Long?, repeat: RepeatRule) = viewModelScope.launch { repo.setReminder(noteId, at, repeat) }
    fun setFolder(id: Long?) = viewModelScope.launch { repo.moveToFolder(listOf(noteId), id) }
    fun setLabel(labelId: Long, attached: Boolean) = viewModelScope.launch { repo.setNoteLabel(noteId, labelId, attached) }
    fun createAndAttachLabel(name: String) = viewModelScope.launch {
        repo.createLabel(name)?.let { repo.setNoteLabel(noteId, it, true) }
    }

    // ---- saving ----
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DELAY_MS)
            saveNow()
        }
    }

    private suspend fun saveNow() = saveMutex.withLock {
        if (!loaded || missing) return@withLock
        val s = snapshot()
        // Keep the state from before this editing session, once, so it can be restored later.
        if (!versionSaved) {
            original?.let { orig ->
                if (orig.title != s.title || orig.content != s.body || orig.checklist != s.checklist ||
                    orig.imagePaths != s.images || orig.type != s.type
                ) {
                    repo.snapshotVersion(orig)
                    versionSaved = true
                }
            }
        }
        repo.saveContent(noteId, s.title.trim(), s.body, s.type, s.checklist, s.images)
    }

    /** Called when leaving the editor: saves, and drops the note if it was left empty. */
    fun flush(discardIfBlank: Boolean = true) {
        saveJob?.cancel()
        app.appScope.launch {
            saveNow()
            if (discardIfBlank) repo.deleteIfBlank(noteId)
        }
    }

    override fun onCleared() {
        // Last-chance save (e.g. the system closed the screen); does not delete anything.
        if (loaded && !missing) {
            val s = snapshot()
            app.appScope.launch {
                saveMutex.withLock { repo.saveContent(noteId, s.title.trim(), s.body, s.type, s.checklist, s.images) }
            }
        }
        super.onCleared()
    }

    class Factory(private val application: Application, private val noteId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(application, noteId) as T
    }

    companion object {
        const val SAVE_DELAY_MS = 600L
    }
}
