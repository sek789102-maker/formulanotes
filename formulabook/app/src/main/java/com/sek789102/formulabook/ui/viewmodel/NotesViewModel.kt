package com.sek789102.formulabook.ui.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sek789102.formulabook.R
import com.sek789102.formulabook.alarm.ExamNotificationScheduler
import com.sek789102.formulabook.data.local.UserPreferences
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.data.model.ExamCountdownEntity
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteSortOrder
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.StudySessionEntity
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.ThemeMode
import com.sek789102.formulabook.data.model.resolveDisplayName
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.data.repository.BackupImportSummary
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.domain.math.EvalResult
import com.sek789102.formulabook.domain.math.MathEvaluator
import com.sek789102.formulabook.domain.study.GpaCourseEntry
import com.sek789102.formulabook.domain.study.PendingNoteTask
import com.sek789102.formulabook.domain.study.PlannerHelper
import com.sek789102.formulabook.domain.study.QuickReferenceCatalog
import com.sek789102.formulabook.domain.study.QuickReferenceItem
import com.sek789102.formulabook.domain.study.Sm2Rating
import com.sek789102.formulabook.domain.study.StudyStatsCalculator
import com.sek789102.formulabook.domain.study.TimetableEntry
import com.sek789102.formulabook.widget.ExamCountdownWidgetUpdater
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StudyFormulaItem(
    val noteId: Long,
    val noteTitle: String,
    val subjectId: String,
    val formula: FormulaBlock
)

data class CalcHistoryEntry(
    val id: Long,
    val expression: String,
    val result: String
)

enum class MathLabTool(@param:StringRes val labelRes: Int) {
    CALCULATOR(R.string.math_tool_calculator),
    GRAPH(R.string.math_tool_graph),
    CONVERTER(R.string.math_tool_converter),
    GPA(R.string.math_tool_gpa)
}

data class FilterState(
    val searchQuery: String = "",
    val selectedSubjectId: String? = null,
    val onlyPinned: Boolean = false,
    val onlyWithFormulas: Boolean = false,
    val onlyNeedsReview: Boolean = false
) {
    val isAnyFilterActive: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedSubjectId != null ||
            onlyPinned ||
            onlyWithFormulas ||
            onlyNeedsReview
}

class NotesViewModel(
    private val appContext: Context,
    private val repository: NotesRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val allNotes: StateFlow<List<NoteWithTags>> = repository.allNotesWithTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<NoteWithTags>> = repository.trashedNotesWithTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SubjectCategory.entries.map { it.toEntity() }
        )

    val allExams: StateFlow<List<ExamCountdownEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingNoteTasks: StateFlow<List<PendingNoteTask>> = allNotes
        .combine(MutableStateFlow(Unit)) { notes, _ ->
            PlannerHelper.extractPendingTasks(notes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val thaiCollator: Collator = Collator.getInstance(Locale.forLanguageTag("th-TH")).apply {
        strength = Collator.PRIMARY
    }

    val filteredNotes: StateFlow<List<NoteWithTags>> = combine(
        allNotes,
        _filterState,
        userPreferences,
        allSubjects
    ) { notes, filter, prefs, subjects ->
        val filtered = notes.filter { item ->
            val n = item.note
            val matchesSubject = filter.selectedSubjectId == null || n.subject == filter.selectedSubjectId
            val matchesPinned = !filter.onlyPinned || n.isPinned
            val matchesFormulas = !filter.onlyWithFormulas || n.formulas.isNotEmpty()
            val matchesReview = !filter.onlyNeedsReview || n.needsReview
            val q = filter.searchQuery.trim()
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                val subjectName = subjects.resolveSubject(n.subject).resolveDisplayName(appContext)
                n.title.contains(q, ignoreCase = true) ||
                    n.content.contains(q, ignoreCase = true) ||
                    subjectName.contains(q, ignoreCase = true) ||
                    n.formulas.any {
                        it.title.contains(q, ignoreCase = true) ||
                            it.expression.contains(q, ignoreCase = true) ||
                            it.variablesNote.contains(q, ignoreCase = true)
                    }
            }
            matchesSubject && matchesPinned && matchesFormulas && matchesReview && matchesQuery
        }

        val comparator = Comparator<NoteWithTags> { a, b ->
            val pinCmp = b.note.isPinned.compareTo(a.note.isPinned)
            if (pinCmp != 0) return@Comparator pinCmp
            when (prefs.sortOrder) {
                NoteSortOrder.UPDATED_DESC -> b.note.updatedAt.compareTo(a.note.updatedAt)
                NoteSortOrder.CREATED_DESC -> b.note.createdAt.compareTo(a.note.createdAt)
                NoteSortOrder.TITLE_ASC -> {
                    val c = thaiCollator.compare(a.note.title, b.note.title)
                    if (c != 0) c else b.note.updatedAt.compareTo(a.note.updatedAt)
                }
                NoteSortOrder.SUBJECT_ASC -> {
                    val sA = subjects.resolveSubject(a.note.subject).resolveDisplayName(appContext)
                    val sB = subjects.resolveSubject(b.note.subject).resolveDisplayName(appContext)
                    val c = thaiCollator.compare(sA, sB)
                    if (c != 0) c else b.note.updatedAt.compareTo(a.note.updatedAt)
                }
            }
        }
        filtered.sortedWith(comparator)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyFormulas: StateFlow<List<StudyFormulaItem>> = allNotes
        .combine(MutableStateFlow(Unit)) { notes, _ ->
            notes.flatMap { noteWithTags ->
                noteWithTags.note.formulas
                    .filter { it.expression.isNotBlank() }
                    .map { formula ->
                        StudyFormulaItem(
                            noteId = noteWithTags.note.id,
                            noteTitle = noteWithTags.note.title,
                            subjectId = noteWithTags.note.subject,
                            formula = formula
                        )
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shouldShowBackupReminder: StateFlow<Boolean> = combine(allNotes, userPreferences) { notes, prefs ->
        val oldestCreated = notes.minOfOrNull { it.note.createdAt }
        StudyStatsCalculator.shouldShowBackupReminder(
            notesCount = notes.size,
            oldestNoteCreatedAtMillis = oldestCreated,
            lastBackupExportedAtMillis = prefs.lastBackupExportedAtMillis,
            dismissedUntilMillis = prefs.backupReminderDismissedUntilMillis
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val saveMutex = Mutex()

    // Undo Delete (moves to Trash; Undo restores immediately)
    private var pendingUndoNoteId: Long? = null
    private val _undoSnackbarTrigger = Channel<Unit>(Channel.BUFFERED)
    val undoSnackbarTrigger: Flow<Unit> = _undoSnackbarTrigger.receiveAsFlow()

    // Calculator & Math Lab State (persisted via SavedStateHandle)
    private val _activeMathTool = MutableStateFlow(
        MathLabTool.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_MATH_TOOL) }
            ?: MathLabTool.CALCULATOR
    )
    val activeMathTool: StateFlow<MathLabTool> = _activeMathTool.asStateFlow()

    private val _graphExpression = MutableStateFlow(savedStateHandle.get<String>(KEY_GRAPH_EXPR).orEmpty())
    val graphExpression: StateFlow<String> = _graphExpression.asStateFlow()

    private val _calcExpression = MutableStateFlow(savedStateHandle.get<String>(KEY_CALC_EXPR).orEmpty())
    val calcExpression: StateFlow<String> = _calcExpression.asStateFlow()

    private val _calcUseDegrees = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_CALC_DEGREES) ?: true)
    val calcUseDegrees: StateFlow<Boolean> = _calcUseDegrees.asStateFlow()

    private val _calcHistory = MutableStateFlow<List<CalcHistoryEntry>>(emptyList())
    val calcHistory: StateFlow<List<CalcHistoryEntry>> = _calcHistory.asStateFlow()

    private val _calcError = MutableStateFlow<String?>(null)
    val calcError: StateFlow<String?> = _calcError.asStateFlow()

    private val _lastAnswer = MutableStateFlow(savedStateHandle.get<String>(KEY_LAST_ANSWER))
    val lastAnswer: StateFlow<String?> = _lastAnswer.asStateFlow()

    fun observeNoteById(noteId: Long): Flow<NoteWithTags?> = repository.observeNoteById(noteId)

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun selectSubjectFilter(subjectId: String?) {
        _filterState.update {
            it.copy(selectedSubjectId = if (it.selectedSubjectId == subjectId) null else subjectId)
        }
    }

    fun toggleOnlyPinned() {
        _filterState.update { it.copy(onlyPinned = !it.onlyPinned) }
    }

    fun toggleOnlyWithFormulas() {
        _filterState.update { it.copy(onlyWithFormulas = !it.onlyWithFormulas) }
    }

    fun toggleOnlyNeedsReview() {
        _filterState.update { it.copy(onlyNeedsReview = !it.onlyNeedsReview) }
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
    }

    fun setSortOrder(order: NoteSortOrder) {
        viewModelScope.launch {
            prefsRepository.setSortOrder(order)
        }
    }

    suspend fun saveNoteSuspend(
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
        createdAt: Long? = null
    ): Long {
        return saveMutex.withLock {
            repository.saveNote(
                existingId = existingId,
                title = title,
                subjectId = subjectId,
                content = content,
                formulas = formulas,
                imagePaths = imagePaths,
                tagIds = tagIds,
                isPinned = isPinned,
                needsReview = needsReview,
                paperStyle = paperStyle,
                createdAt = createdAt
            )
        }
    }

    fun saveNote(
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
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedId = saveNoteSuspend(
                existingId = existingId,
                title = title,
                subjectId = subjectId,
                content = content,
                formulas = formulas,
                imagePaths = imagePaths,
                tagIds = tagIds,
                isPinned = isPinned,
                needsReview = needsReview,
                paperStyle = paperStyle,
                createdAt = createdAt
            )
            onSaved(savedId)
        }
    }

    fun deleteNote(noteId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            pendingUndoNoteId = noteId
            repository.softDeleteNote(noteId)
            onDeleted()
            _undoSnackbarTrigger.trySend(Unit)
        }
    }

    fun undoDeleteNote() {
        val noteId = pendingUndoNoteId ?: return
        pendingUndoNoteId = null
        viewModelScope.launch {
            repository.restoreNoteFromTrash(noteId)
        }
    }

    fun restoreFromTrash(noteId: Long) {
        viewModelScope.launch {
            repository.restoreNoteFromTrash(noteId)
        }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteNote(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun togglePin(noteId: Long, currentlyPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(noteId, !currentlyPinned)
        }
    }

    fun toggleNeedsReview(noteId: Long, currentlyNeedsReview: Boolean) {
        viewModelScope.launch {
            repository.toggleNeedsReview(noteId, !currentlyNeedsReview)
        }
    }

    fun toggleChecklistLine(noteId: Long, lineIndex: Int) {
        viewModelScope.launch {
            repository.toggleChecklistLine(noteId, lineIndex)
        }
    }

    fun rateFormulaSm2(noteId: Long, formulaId: String, rating: Sm2Rating) {
        viewModelScope.launch {
            repository.rateFormulaSm2(noteId, formulaId, rating)
        }
    }

    fun toggleFormulaMastered(noteId: Long, formulaId: String) {
        viewModelScope.launch {
            repository.toggleFormulaMastered(noteId, formulaId)
        }
    }

    fun saveReferenceItemAsNote(
        item: QuickReferenceItem,
        paperStyle: Int,
        onSaved: (Long) -> Unit = {}
    ) {
        val (content, block) = QuickReferenceCatalog.toNoteDraftContent(item)
        saveNote(
            existingId = 0L,
            title = item.title,
            subjectId = item.subjectId,
            content = content,
            formulas = listOf(block),
            imagePaths = emptyList(),
            tagIds = emptyList(),
            isPinned = false,
            needsReview = true,
            paperStyle = paperStyle,
            onSaved = onSaved
        )
    }

    fun createCustomSubject(name: String, colorHex: Long, onCreated: (SubjectEntity) -> Unit = {}) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val created = repository.createCustomSubject(clean, colorHex)
            onCreated(created)
        }
    }

    fun updateCustomSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateCustomSubject(subject)
            ExamCountdownWidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteCustomSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteCustomSubject(subjectId)
            _filterState.update { state ->
                if (state.selectedSubjectId == subjectId) state.copy(selectedSubjectId = null) else state
            }
            ExamCountdownWidgetUpdater.updateAll(appContext)
        }
    }

    fun addExamCountdown(title: String, subjectId: String, examDateMillis: Long, note: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addExam(title, subjectId, examDateMillis, note)
            val exams = repository.getAllExamsSnapshot()
            val notifEnabled = prefsRepository.preferencesFlow.first().examNotificationsEnabled
            ExamNotificationScheduler.rescheduleAll(
                context = appContext,
                exams = exams,
                enabled = notifEnabled
            )
            ExamCountdownWidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteExamCountdown(examId: Long) {
        viewModelScope.launch {
            ExamNotificationScheduler.cancelForExam(appContext, examId)
            repository.deleteExam(examId)
            ExamCountdownWidgetUpdater.updateAll(appContext)
        }
    }

    fun setExamNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setExamNotificationsEnabled(enabled)
            val exams = repository.getAllExamsSnapshot()
            ExamNotificationScheduler.rescheduleAll(
                context = appContext,
                exams = exams,
                enabled = enabled
            )
        }
    }

    fun markBackupExported() {
        viewModelScope.launch {
            prefsRepository.setLastBackupExportedAt(System.currentTimeMillis())
        }
    }

    fun dismissBackupReminderForLater() {
        viewModelScope.launch {
            val threeDaysLater = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)
            prefsRepository.dismissBackupReminderUntil(threeDaysLater)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDynamicColor(enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDarkMode(enabled) }
    }

    fun setDefaultPaperStyle(style: Int) {
        viewModelScope.launch { prefsRepository.setDefaultPaperStyle(style) }
    }

    fun addTimetableEntry(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        subjectId: String,
        room: String
    ) {
        val cleanStart = PlannerHelper.normalizeTimeString(startTime).ifBlank { "08:30" }
        val cleanEnd = PlannerHelper.normalizeTimeString(endTime).ifBlank { "09:30" }
        viewModelScope.launch {
            prefsRepository.updateTimetableEntries { current ->
                current + TimetableEntry(
                    dayOfWeek = dayOfWeek.coerceIn(1, 7),
                    startTime = cleanStart,
                    endTime = cleanEnd,
                    subjectId = subjectId,
                    room = room.trim()
                )
            }
        }
    }

    fun removeTimetableEntry(id: String) {
        viewModelScope.launch {
            prefsRepository.updateTimetableEntries { current ->
                current.filterNot { it.id == id }
            }
        }
    }

    fun filterNotesBySubject(subjectId: String) {
        _filterState.value = FilterState(selectedSubjectId = subjectId)
    }

    fun addGpaCourse(name: String, credits: Double, gradePoint: Double) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || credits <= 0.0) return
        viewModelScope.launch {
            prefsRepository.updateGpaCourses { current ->
                current + GpaCourseEntry(
                    name = cleanName,
                    credits = credits,
                    gradePoint = gradePoint.coerceIn(0.0, 4.0)
                )
            }
        }
    }

    fun removeGpaCourse(id: String) {
        viewModelScope.launch {
            prefsRepository.updateGpaCourses { current ->
                current.filterNot { it.id == id }
            }
        }
    }

    fun clearGpaCourses() {
        viewModelScope.launch {
            prefsRepository.setGpaCourses(emptyList())
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun exportBackupZip(out: java.io.OutputStream) = repository.exportBackupZip(out)

    suspend fun importBackupStream(input: java.io.InputStream): Result<BackupImportSummary> =
        afterImport(repository.importBackupStream(input))

    suspend fun importBackupJson(json: String): Result<BackupImportSummary> =
        afterImport(repository.importBackupJson(json))

    private suspend fun afterImport(result: Result<BackupImportSummary>): Result<BackupImportSummary> {
        if (result.isSuccess) {
            val exams = repository.getAllExamsSnapshot()
            val notifEnabled = prefsRepository.preferencesFlow.first().examNotificationsEnabled
            ExamNotificationScheduler.rescheduleAll(
                context = appContext,
                exams = exams,
                enabled = notifEnabled
            )
            ExamCountdownWidgetUpdater.updateAll(appContext)
        }
        return result
    }

    // Math Lab & Calculator methods
    fun setActiveMathTool(tool: MathLabTool) {
        _activeMathTool.value = tool
        savedStateHandle[KEY_MATH_TOOL] = tool.name
    }

    fun setGraphExpression(expr: String) {
        _graphExpression.value = expr
        savedStateHandle[KEY_GRAPH_EXPR] = expr
    }

    fun openFormulaInGraph(rawFormula: String) {
        val parsed = MathEvaluator.inspectFormula(rawFormula)
        setGraphExpression(parsed.evaluableExpression)
        setActiveMathTool(MathLabTool.GRAPH)
    }

    fun setCalcExpression(expr: String) {
        _calcExpression.value = expr
        savedStateHandle[KEY_CALC_EXPR] = expr
        _calcError.value = null
    }

    fun appendCalcToken(token: String) {
        val next = _calcExpression.value + token
        _calcExpression.value = next
        savedStateHandle[KEY_CALC_EXPR] = next
        _calcError.value = null
    }

    fun backspaceCalc() {
        val cur = _calcExpression.value
        val next = if (cur.isNotEmpty()) cur.dropLast(1) else ""
        _calcExpression.value = next
        savedStateHandle[KEY_CALC_EXPR] = next
        _calcError.value = null
    }

    fun clearCalc() {
        _calcExpression.value = ""
        savedStateHandle[KEY_CALC_EXPR] = ""
        _calcError.value = null
    }

    fun toggleCalcDegrees() {
        val next = !_calcUseDegrees.value
        _calcUseDegrees.value = next
        savedStateHandle[KEY_CALC_DEGREES] = next
    }

    fun evaluateCalculator() {
        val expr = _calcExpression.value.trim()
        if (expr.isEmpty()) return
        when (val res = MathEvaluator.evaluate(expr, useDegrees = _calcUseDegrees.value)) {
            is EvalResult.Success -> {
                _lastAnswer.value = res.formatted
                savedStateHandle[KEY_LAST_ANSWER] = res.formatted
                _calcError.value = null
                _calcHistory.update { old ->
                    (listOf(CalcHistoryEntry(System.nanoTime(), expr, res.formatted)) + old).take(30)
                }
                _calcExpression.value = res.formatted
                savedStateHandle[KEY_CALC_EXPR] = res.formatted
            }
            is EvalResult.Error -> {
                _calcError.value = res.localizedMessage(appContext)
            }
        }
    }

    fun clearCalcHistory() {
        _calcHistory.value = emptyList()
    }

    class Factory(
        private val appContext: Context,
        private val repository: NotesRepository,
        private val prefsRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return NotesViewModel(appContext, repository, prefsRepository, savedStateHandle) as T
        }
    }

    companion object {
        private const val KEY_MATH_TOOL = "math_tool"
        private const val KEY_GRAPH_EXPR = "graph_expr"
        private const val KEY_CALC_EXPR = "calc_expr"
        private const val KEY_CALC_DEGREES = "calc_degrees"
        private const val KEY_LAST_ANSWER = "last_answer"
    }
}
