package com.sek789102.formulabook

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sek789102.formulabook.alarm.ExamNotificationScheduler
import com.sek789102.formulabook.data.local.AppDatabase
import com.sek789102.formulabook.data.local.UserPreferencesRepository
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.data.repository.NotesRepository
import com.sek789102.formulabook.ui.screens.CalculatorScreen
import com.sek789102.formulabook.ui.screens.FormulaStudyScreen
import com.sek789102.formulabook.ui.screens.NoteDetailScreen
import com.sek789102.formulabook.ui.screens.NoteEditorScreen
import com.sek789102.formulabook.ui.screens.NotesHomeScreen
import com.sek789102.formulabook.ui.screens.SettingsBottomSheet
import com.sek789102.formulabook.ui.screens.StudentPlannerScreen
import com.sek789102.formulabook.ui.screens.SubjectVaultScreen
import com.sek789102.formulabook.ui.screens.TrashScreen
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.SmutNoteTheme
import com.sek789102.formulabook.ui.theme.resolveDarkTheme
import com.sek789102.formulabook.ui.viewmodel.NotesViewModel
import com.sek789102.formulabook.worker.TrashCleanupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    NOTES(R.string.nav_notes, Icons.Rounded.Description, Icons.Outlined.Description, "nav_notes"),
    VAULT(R.string.nav_vault, Icons.Rounded.AutoStories, Icons.Outlined.AutoStories, "nav_vault"),
    STUDY(R.string.nav_study, Icons.Rounded.School, Icons.Outlined.School, "nav_study"),
    PLANNER(R.string.nav_planner, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_planner"),
    CALCULATOR(R.string.nav_calculator, Icons.Rounded.Calculate, Icons.Outlined.Calculate, "nav_calculator")
}

private sealed interface ScreenRoute {
    data object HomeTabs : ScreenRoute
    data class Detail(val noteId: Long) : ScreenRoute
    data class Editor(val noteId: Long?, val initialSubjectId: String? = null) : ScreenRoute
    data object Trash : ScreenRoute
}

private val ScreenRouteSaver: Saver<ScreenRoute, Any> = listSaver(
    save = { route ->
        when (route) {
            ScreenRoute.HomeTabs -> listOf("home", -1L, "")
            ScreenRoute.Trash -> listOf("trash", -1L, "")
            is ScreenRoute.Detail -> listOf("detail", route.noteId, "")
            is ScreenRoute.Editor -> listOf("editor", route.noteId ?: -1L, route.initialSubjectId.orEmpty())
        }
    },
    restore = { list ->
        val kind = list.getOrNull(0) as? String
        val id = (list.getOrNull(1) as? Number)?.toLong() ?: -1L
        val subj = (list.getOrNull(2) as? String)?.takeIf { it.isNotBlank() }
        when (kind) {
            "detail" -> ScreenRoute.Detail(id)
            "editor" -> ScreenRoute.Editor(if (id >= 0L) id else null, subj)
            "trash" -> ScreenRoute.Trash
            else -> ScreenRoute.HomeTabs
        }
    }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ExamNotificationScheduler.createNotificationChannel(applicationContext)
        TrashCleanupWorker.schedulePeriodic(applicationContext)

        val db = AppDatabase.getInstance(applicationContext)
        val repository = NotesRepository(applicationContext, db)
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val factory = NotesViewModel.Factory(applicationContext, repository, prefsRepository)

        setContent {
            val viewModel: NotesViewModel = viewModel(factory = factory)
            val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
            val isDarkTheme = resolveDarkTheme(prefs.themeMode)

            SideEffect {
                val style = if (isDarkTheme) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            SmutNoteTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor
            ) {
                StudentFormulaNotesApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun StudentFormulaNotesApp(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val studyFormulas by viewModel.studyFormulas.collectAsStateWithLifecycle()
    val showBackupReminder by viewModel.shouldShowBackupReminder.collectAsStateWithLifecycle()

    val allExams by viewModel.allExams.collectAsStateWithLifecycle()
    val pendingNoteTasks by viewModel.pendingNoteTasks.collectAsStateWithLifecycle()

    val activeMathTool by viewModel.activeMathTool.collectAsStateWithLifecycle()
    val graphExpression by viewModel.graphExpression.collectAsStateWithLifecycle()
    val calcExpression by viewModel.calcExpression.collectAsStateWithLifecycle()
    val calcDegrees by viewModel.calcUseDegrees.collectAsStateWithLifecycle()
    val calcHistory by viewModel.calcHistory.collectAsStateWithLifecycle()
    val calcError by viewModel.calcError.collectAsStateWithLifecycle()
    val lastAnswer by viewModel.lastAnswer.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.NOTES) }
    var currentRoute by rememberSaveable(stateSaver = ScreenRouteSaver) {
        mutableStateOf<ScreenRoute>(ScreenRoute.HomeTabs)
    }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    val quickBackupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val out = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Unable to open output stream")
                        out.use { stream -> viewModel.exportBackupZip(stream) }
                    }
                    viewModel.markBackupExported()
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_export_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_export_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.undoSnackbarTrigger.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_moved_to_trash),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteNote()
            }
        }
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "root_route"
    ) { route ->
        when (route) {
            is ScreenRoute.Detail -> {
                val note by viewModel.observeNoteById(route.noteId)
                    .collectAsStateWithLifecycle(initialValue = allNotes.firstOrNull { it.note.id == route.noteId })
                val subject = note?.let { allSubjects.resolveSubject(it.note.subject) }
                NoteDetailScreen(
                    noteWithTags = note,
                    subject = subject,
                    onBack = { currentRoute = ScreenRoute.HomeTabs },
                    onEdit = { id -> currentRoute = ScreenRoute.Editor(id) },
                    onDelete = { id ->
                        viewModel.deleteNote(id) {
                            currentRoute = ScreenRoute.HomeTabs
                        }
                    },
                    onTogglePin = viewModel::togglePin,
                    onToggleNeedsReview = viewModel::toggleNeedsReview,
                    onToggleFormulaMastered = viewModel::toggleFormulaMastered,
                    onToggleChecklistLine = viewModel::toggleChecklistLine,
                    onPlotFormula = { expr ->
                        viewModel.openFormulaInGraph(expr)
                        currentTab = MainTab.CALCULATOR
                        currentRoute = ScreenRoute.HomeTabs
                    }
                )
            }

            is ScreenRoute.Editor -> {
                val targetId = route.noteId ?: 0L
                val existing = route.noteId?.let { id -> allNotes.firstOrNull { it.note.id == id } }
                val preservedTagIds = existing?.tags?.map { it.id }.orEmpty()
                NoteEditorScreen(
                    targetNoteId = targetId,
                    existingNote = existing,
                    defaultPaperStyle = prefs.defaultPaperStyle,
                    initialSubjectId = route.initialSubjectId,
                    subjects = allSubjects,
                    onBack = {
                        currentRoute = if (route.noteId != null) {
                            ScreenRoute.Detail(route.noteId)
                        } else {
                            ScreenRoute.HomeTabs
                        }
                    },
                    onCreateSubject = { name, colorHex, onCreated ->
                        viewModel.createCustomSubject(name, colorHex, onCreated)
                    },
                    onAutoSave = { existingId, title, subjectId, content, formulas, imagePaths, isPinned, needsReview, paperStyle, createdAt ->
                        viewModel.saveNoteSuspend(
                            existingId = existingId,
                            title = title,
                            subjectId = subjectId,
                            content = content,
                            formulas = formulas,
                            imagePaths = imagePaths,
                            tagIds = preservedTagIds,
                            isPinned = isPinned,
                            needsReview = needsReview,
                            paperStyle = paperStyle,
                            createdAt = createdAt
                        )
                    },
                    onSave = { existingId, title, subjectId, content, formulas, imagePaths, isPinned, needsReview, paperStyle, createdAt ->
                        viewModel.saveNote(
                            existingId = existingId,
                            title = title,
                            subjectId = subjectId,
                            content = content,
                            formulas = formulas,
                            imagePaths = imagePaths,
                            tagIds = preservedTagIds,
                            isPinned = isPinned,
                            needsReview = needsReview,
                            paperStyle = paperStyle,
                            createdAt = createdAt
                        ) { savedId ->
                            currentRoute = ScreenRoute.Detail(savedId)
                        }
                    }
                )
            }

            ScreenRoute.Trash -> {
                TrashScreen(
                    trashedNotes = trashedNotes,
                    subjects = allSubjects,
                    onBack = { currentRoute = ScreenRoute.HomeTabs },
                    onRestoreNote = viewModel::restoreFromTrash,
                    onPermanentDeleteNote = viewModel::permanentlyDeleteNote,
                    onEmptyTrash = viewModel::emptyTrash
                )
            }

            ScreenRoute.HomeTabs -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 600.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (!isWide) {
                                Column {
                                    HorizontalDivider(
                                        thickness = Dimens.BorderThin,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = Dimens.ElevationNone
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            val selected = currentTab == tab
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { currentTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = stringResource(tab.labelRes)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = stringResource(tab.labelRes),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                modifier = Modifier.testTag(tab.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (currentTab == MainTab.NOTES) {
                                ExtendedFloatingActionButton(
                                    onClick = { currentRoute = ScreenRoute.Editor(null) },
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.action_new_note)) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_new_note")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWide) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        FloatingActionButton(
                                            onClick = { currentRoute = ScreenRoute.Editor(null) },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_new_note))
                                        }
                                    }
                                ) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = currentTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = stringResource(tab.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(tab.labelRes)) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (currentTab) {
                                    MainTab.NOTES -> NotesHomeScreen(
                                        allNotesCount = allNotes.size,
                                        totalFormulasCount = studyFormulas.size,
                                        pendingTasksCount = pendingNoteTasks.size,
                                        showBackupReminder = showBackupReminder,
                                        subjects = allSubjects,
                                        filteredNotes = filteredNotes,
                                        filterState = filterState,
                                        sortOrder = prefs.sortOrder,
                                        isWideLayout = isWide,
                                        onSearchChange = viewModel::updateSearchQuery,
                                        onSelectSubject = viewModel::selectSubjectFilter,
                                        onCreateSubject = { name, hex -> viewModel.createCustomSubject(name, hex) },
                                        onChangeSortOrder = viewModel::setSortOrder,
                                        onTogglePinnedFilter = viewModel::toggleOnlyPinned,
                                        onToggleFormulaFilter = viewModel::toggleOnlyWithFormulas,
                                        onToggleReviewFilter = viewModel::toggleOnlyNeedsReview,
                                        onClearFilters = viewModel::clearAllFilters,
                                        onOpenNote = { id -> currentRoute = ScreenRoute.Detail(id) },
                                        onTogglePinNote = viewModel::togglePin,
                                        onOpenSettings = { showSettingsSheet = true },
                                        onBackupNowFromReminder = {
                                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            quickBackupExportLauncher.launch("formulabook_backup_$stamp.zip")
                                        },
                                        onDismissBackupReminder = viewModel::dismissBackupReminderForLater
                                    )

                                    MainTab.VAULT -> SubjectVaultScreen(
                                        allNotes = allNotes,
                                        subjects = allSubjects,
                                        onSaveReferenceAsNote = { refItem ->
                                            viewModel.saveReferenceItemAsNote(refItem, prefs.defaultPaperStyle) { savedId ->
                                                currentRoute = ScreenRoute.Detail(savedId)
                                            }
                                        },
                                        onBrowseSubjectNotes = { subjectId ->
                                            viewModel.filterNotesBySubject(subjectId)
                                            currentTab = MainTab.NOTES
                                        },
                                        onCreateNoteInSubject = { subjectId ->
                                            currentRoute = ScreenRoute.Editor(noteId = null, initialSubjectId = subjectId)
                                        }
                                    )

                                    MainTab.STUDY -> FormulaStudyScreen(
                                        studyFormulas = studyFormulas,
                                        subjects = allSubjects,
                                        onRateFormulaSm2 = viewModel::rateFormulaSm2,
                                        onCreateNote = { currentRoute = ScreenRoute.Editor(null) },
                                        onOpenNote = { id -> currentRoute = ScreenRoute.Detail(id) }
                                    )

                                    MainTab.PLANNER -> StudentPlannerScreen(
                                        timetableEntries = prefs.timetableEntries,
                                        pendingTasks = pendingNoteTasks,
                                        exams = allExams,
                                        subjects = allSubjects,
                                        onAddTimetableEntry = viewModel::addTimetableEntry,
                                        onRemoveTimetableEntry = viewModel::removeTimetableEntry,
                                        onBrowseSubjectNotes = { subjectId ->
                                            viewModel.filterNotesBySubject(subjectId)
                                            currentTab = MainTab.NOTES
                                        },
                                        onToggleTaskDone = viewModel::toggleChecklistLine,
                                        onOpenNote = { id -> currentRoute = ScreenRoute.Detail(id) },
                                        onAddExam = viewModel::addExamCountdown,
                                        onDeleteExam = viewModel::deleteExamCountdown
                                    )

                                    MainTab.CALCULATOR -> CalculatorScreen(
                                        activeTool = activeMathTool,
                                        graphExpression = graphExpression,
                                        expression = calcExpression,
                                        useDegrees = calcDegrees,
                                        errorText = calcError,
                                        lastAnswer = lastAnswer,
                                        history = calcHistory,
                                        gpaCourses = prefs.gpaCourses,
                                        onSelectTool = viewModel::setActiveMathTool,
                                        onGraphExpressionChange = viewModel::setGraphExpression,
                                        onAppend = viewModel::appendCalcToken,
                                        onBackspace = viewModel::backspaceCalc,
                                        onClear = viewModel::clearCalc,
                                        onEvaluate = viewModel::evaluateCalculator,
                                        onToggleDegrees = viewModel::toggleCalcDegrees,
                                        onSelectHistory = viewModel::setCalcExpression,
                                        onClearHistory = viewModel::clearCalcHistory,
                                        onAddGpaCourse = viewModel::addGpaCourse,
                                        onRemoveGpaCourse = viewModel::removeGpaCourse,
                                        onClearGpaCourses = viewModel::clearGpaCourses
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            preferences = prefs,
            notesCount = allNotes.size,
            formulasCount = studyFormulas.size,
            trashedCount = trashedNotes.size,
            subjects = allSubjects,
            onSelectThemeMode = viewModel::setThemeMode,
            onToggleDynamicColor = viewModel::setDynamicColor,
            onToggleExamNotifications = viewModel::setExamNotificationsEnabled,
            onChangeDefaultPaper = viewModel::setDefaultPaperStyle,
            onCreateSubject = { name, hex -> viewModel.createCustomSubject(name, hex) },
            onUpdateSubject = viewModel::updateCustomSubject,
            onDeleteSubject = viewModel::deleteCustomSubject,
            onOpenTrash = { currentRoute = ScreenRoute.Trash },
            onExportBackup = { out -> viewModel.exportBackupZip(out) },
            onBackupExported = viewModel::markBackupExported,
            onImportBackup = { input -> viewModel.importBackupStream(input) },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
