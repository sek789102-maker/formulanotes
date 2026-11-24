package com.sek789102.jotwai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sek789102.jotwai.JotwaiApp
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.data.model.SortOrder
import com.sek789102.jotwai.data.model.TextSize
import com.sek789102.jotwai.data.model.ThemeMode
import com.sek789102.jotwai.data.model.ViewMode
import com.sek789102.jotwai.data.repository.BackupTooLargeException
import com.sek789102.jotwai.data.repository.InvalidBackupException
import com.sek789102.jotwai.domain.NoteQuery
import com.sek789102.jotwai.domain.SearchFilter
import com.sek789102.jotwai.domain.Section
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/** A short message for the snackbar, with an optional undo. */
data class UiMessage(val text: String, val undoLabel: String? = null, val onUndo: (() -> Unit)? = null)

data class HomeState(
    val notes: List<NoteWithLabels> = emptyList(),
    val pinnedCount: Int = 0,
    val loaded: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as JotwaiApp
    private val repo = app.repository
    private val prefsRepo = app.preferences

    val prefs: StateFlow<UserPreferences?> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allNotes: StateFlow<List<NoteWithLabels>?> = repo.notes
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val folders: StateFlow<List<FolderEntity>> = repo.folders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val labels: StateFlow<List<LabelEntity>> = repo.labels.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _section = MutableStateFlow<Section>(Section.Notes)
    val section: StateFlow<Section> = _section.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter())
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    /** Locked notes opened since the app was last in the background. */
    private val _unlocked = MutableStateFlow<Set<Long>>(emptySet())
    val unlocked: StateFlow<Set<Long>> = _unlocked.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val home: StateFlow<HomeState> = combine(allNotes, section, filter, prefs) { all, sec, f, p ->
        if (all == null) return@combine HomeState()
        val visible = all.filter { NoteQuery.inSection(it, sec) && NoteQuery.matches(it, f) }
        val sorted = NoteQuery.sort(visible, p?.sortOrder ?: SortOrder.UPDATED, sec)
        HomeState(sorted, sorted.count { it.note.isPinned }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    private fun str(id: Int, vararg args: Any) = getApplication<Application>().getString(id, *args)
    private fun say(text: String, undo: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, if (undo != null) str(R.string.action_undo) else null, undo))
    }

    // ---- navigation/filter ----
    fun openSection(s: Section) {
        _section.value = s
        _selection.value = emptySet()
    }

    fun setFilter(f: SearchFilter) { _filter.value = f }
    fun clearFilter() { _filter.value = SearchFilter() }

    // ---- selection ----
    fun toggleSelect(id: Long) = _selection.update { if (id in it) it - id else it + id }
    fun selectAll() { _selection.value = home.value.notes.map { it.note.id }.toSet() }
    fun clearSelection() { _selection.value = emptySet() }
    private fun takeSelection(): Set<Long> = _selection.value.also { _selection.value = emptySet() }

    fun markUnlocked(id: Long) = _unlocked.update { it + id }
    fun lockAll() { _unlocked.value = emptySet() }

    // ---- note actions (work on given ids, or on the selection) ----
    fun createNote(type: NoteType, content: String = "", title: String = "", images: List<String> = emptyList(), onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sec = section.value
            val note = NoteEntity(
                title = title,
                content = if (type == NoteType.TEXT) content else "",
                type = type,
                checklist = if (type == NoteType.CHECKLIST) listOf(ChecklistItem()) else emptyList(),
                imagePaths = images,
                folderId = (sec as? Section.Folder)?.id,
                isArchived = false
            )
            val labelIds = listOfNotNull((sec as? Section.Label)?.id)
            val id = repo.createNote(note, labelIds)
            onCreated(id)
        }
    }

    fun setPinned(ids: Set<Long> = takeSelection(), pinned: Boolean) = viewModelScope.launch { repo.setPinned(ids, pinned) }

    fun setColor(ids: Set<Long> = takeSelection(), color: Int) = viewModelScope.launch { repo.setColor(ids, color) }

    fun setArchived(ids: Set<Long> = takeSelection(), archived: Boolean) = viewModelScope.launch {
        repo.setArchived(ids, archived)
        say(
            str(if (archived) R.string.msg_archived else R.string.msg_unarchived, ids.size),
        ) { viewModelScope.launch { repo.setArchived(ids, !archived) } }
    }

    fun moveToTrash(ids: Set<Long> = takeSelection()) = viewModelScope.launch {
        repo.moveToTrash(ids)
        say(str(R.string.msg_trashed, ids.size)) { viewModelScope.launch { repo.restoreFromTrash(ids) } }
    }

    fun restore(ids: Set<Long> = takeSelection()) = viewModelScope.launch {
        repo.restoreFromTrash(ids)
        say(str(R.string.msg_restored, ids.size))
    }

    fun deleteForever(ids: Set<Long> = takeSelection()) = viewModelScope.launch {
        repo.deleteForever(ids)
        say(str(R.string.msg_deleted_forever, ids.size))
    }

    fun emptyTrash() = viewModelScope.launch {
        repo.emptyTrash()
        say(str(R.string.msg_trash_emptied))
    }

    fun moveToFolder(ids: Set<Long> = takeSelection(), folderId: Long?) = viewModelScope.launch {
        repo.moveToFolder(ids, folderId)
        say(str(R.string.msg_moved, ids.size))
    }

    fun addLabel(ids: Set<Long> = takeSelection(), labelId: Long) = viewModelScope.launch { repo.addLabelToNotes(ids, labelId) }

    fun setLocked(ids: Set<Long>, locked: Boolean) = viewModelScope.launch { repo.setLocked(ids, locked) }

    fun duplicate(id: Long, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        repo.duplicate(id)?.let {
            say(str(R.string.msg_duplicated))
            onCreated(it)
        }
    }

    // ---- folders & labels ----
    fun createFolder(name: String, color: Int = 0, onCreated: (Long) -> Unit = {}) = viewModelScope.launch { repo.createFolder(name, color)?.let(onCreated) }
    fun renameFolder(folder: FolderEntity, name: String) = viewModelScope.launch { repo.renameFolder(folder, name) }
    fun setFolderColor(folder: FolderEntity, color: Int) = viewModelScope.launch { repo.setFolderColor(folder, color) }
    fun clearPastReminders() = viewModelScope.launch {
        val n = repo.clearPastReminders()
        say(if (n > 0) "ล้างการแจ้งเตือนที่ผ่านไปแล้ว $n รายการ" else "ไม่มีการแจ้งเตือนที่ผ่านไปแล้ว")
    }
    fun deleteFolder(id: Long) = viewModelScope.launch {
        if ((section.value as? Section.Folder)?.id == id) openSection(Section.Notes)
        repo.deleteFolder(id)
    }

    fun createLabel(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch { repo.createLabel(name)?.let(onCreated) }
    fun renameLabel(id: Long, name: String) = viewModelScope.launch {
        if (!repo.renameLabel(id, name)) say(str(R.string.msg_label_exists))
    }
    fun deleteLabel(id: Long) = viewModelScope.launch {
        if ((section.value as? Section.Label)?.id == id) openSection(Section.Notes)
        repo.deleteLabel(id)
    }

    // ---- settings ----
    fun setThemeMode(v: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(v) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { prefsRepo.setDynamicColor(v) }
    fun setViewMode(v: ViewMode) = viewModelScope.launch { prefsRepo.setViewMode(v) }
    fun setSortOrder(v: SortOrder) = viewModelScope.launch { prefsRepo.setSortOrder(v) }
    fun setTextSize(v: TextSize) = viewModelScope.launch { prefsRepo.setTextSize(v) }
    fun setChecklistAddToBottom(v: Boolean) = viewModelScope.launch { prefsRepo.setChecklistAddToBottom(v) }
    fun setRemindersEnabled(v: Boolean) = viewModelScope.launch { prefsRepo.setRemindersEnabled(v) }
    fun setChecklistMoveCheckedDown(v: Boolean) = viewModelScope.launch { prefsRepo.setChecklistMoveCheckedDown(v) }

    // ---- backup ----
    suspend fun exportBackup(out: OutputStream): Boolean = runCatching {
        repo.exportBackup(out)
        prefsRepo.setLastBackupAt(System.currentTimeMillis())
    }.onFailure { say(str(R.string.msg_backup_failed)) }
        .onSuccess { say(str(R.string.msg_backup_done)) }
        .isSuccess

    suspend fun importBackup(input: InputStream) {
        repo.importBackup(input)
            .onSuccess { say(str(R.string.msg_restore_done, it.imported, it.skipped)) }
            .onFailure { e ->
                say(
                    when (e) {
                        is BackupTooLargeException -> str(R.string.msg_restore_too_large)
                        is InvalidBackupException -> str(R.string.msg_restore_invalid)
                        else -> str(R.string.msg_restore_failed)
                    }
                )
            }
    }

    suspend fun exportText(out: OutputStream): Boolean = runCatching { repo.exportText(out) }
        .onSuccess { say("ส่งออกโน้ต $it รายการแล้ว") }
        .onFailure { say("ส่งออกไม่สำเร็จ") }
        .isSuccess

    suspend fun importTextNotes(files: List<Pair<String, String>>) {
        var n = 0
        files.forEach { (name, text) -> if (repo.importTextNote(name, text) != null) n++ }
        say(if (n > 0) "นำเข้าโน้ต $n รายการแล้ว" else "ไม่พบข้อความในไฟล์ที่เลือก")
    }

    fun notify(text: String) = say(text)

    suspend fun noteExists(id: Long): Boolean = repo.getNote(id) != null

    suspend fun awaitLoaded() { allNotes.first { it != null } }
}
