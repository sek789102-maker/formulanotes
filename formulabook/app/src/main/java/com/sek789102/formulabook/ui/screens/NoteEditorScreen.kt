package com.sek789102.formulabook.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.mascotDrawableRes
import com.sek789102.formulabook.data.model.resolveSubject
import com.sek789102.formulabook.domain.math.SymbolPaletteData
import com.sek789102.formulabook.ui.components.CustomSubjectDialog
import com.sek789102.formulabook.ui.components.FormulaEvaluatorDialog
import com.sek789102.formulabook.ui.components.MathSymbolToolbar
import com.sek789102.formulabook.ui.components.NoteImageThumbnail
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.components.ZoomableImageDialog
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily
import com.sek789102.formulabook.util.NoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private enum class EditorFocusTarget {
    BODY,
    FORMULA_EXPR,
    OTHER
}

private enum class EditorToolbarTab {
    MARKDOWN,
    MATH
}

private enum class AutoSaveStatus {
    IDLE,
    SAVING,
    SAVED
}

private class EditableFormulaState(
    val id: String,
    title: String,
    expression: String,
    variablesNote: String,
    val mastered: Boolean,
    val blockType: String = FormulaBlock.BLOCK_TYPE_FORMULA,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val nextReviewAtMillis: Long = 0L,
    val lastReviewedAtMillis: Long = 0L,
    selStart: Int = expression.length,
    selEnd: Int = expression.length
) {
    var title by mutableStateOf(title)
    var expression by mutableStateOf(
        TextFieldValue(
            text = expression,
            selection = TextRange(selStart.coerceIn(0, expression.length), selEnd.coerceIn(0, expression.length))
        )
    )
    var variablesNote by mutableStateOf(variablesNote)

    fun toFormulaBlock(): FormulaBlock = FormulaBlock(
        id = id,
        title = title.trim(),
        expression = expression.text.trim(),
        variablesNote = variablesNote.trim(),
        mastered = mastered,
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        nextReviewAtMillis = nextReviewAtMillis,
        lastReviewedAtMillis = lastReviewedAtMillis,
        blockType = blockType
    )
}


private val StringStateListSaver: Saver<SnapshotStateList<String>, Any> = listSaver(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

private val FormulaDraftListSaver: Saver<SnapshotStateList<EditableFormulaState>, String> = Saver(
    save = { list ->
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("expr", item.expression.text)
                    put("selStart", item.expression.selection.start)
                    put("selEnd", item.expression.selection.end)
                    put("vars", item.variablesNote)
                    put("mastered", item.mastered)
                    put("blockType", item.blockType)
                    put("repetitions", item.repetitions)
                    put("intervalDays", item.intervalDays)
                    put("easeFactor", item.easeFactor)
                    put("nextReviewAtMillis", item.nextReviewAtMillis)
                    put("lastReviewedAtMillis", item.lastReviewedAtMillis)
                }
            )
        }
        arr.toString()
    },
    restore = { raw ->
        val restored = mutableStateListOf<EditableFormulaState>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val expr = obj.optString("expr")
                restored.add(
                    EditableFormulaState(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = obj.optString("title"),
                        expression = expr,
                        variablesNote = obj.optString("vars"),
                        mastered = obj.optBoolean("mastered", false),
                        blockType = obj.optString("blockType", FormulaBlock.BLOCK_TYPE_FORMULA),
                        repetitions = obj.optInt("repetitions", 0),
                        intervalDays = obj.optInt("intervalDays", 0),
                        easeFactor = obj.optDouble("easeFactor", 2.5),
                        nextReviewAtMillis = obj.optLong("nextReviewAtMillis", 0L),
                        lastReviewedAtMillis = obj.optLong("lastReviewedAtMillis", 0L),
                        selStart = obj.optInt("selStart", expr.length),
                        selEnd = obj.optInt("selEnd", expr.length)
                    )
                )
            }
        } catch (_: Exception) {
        }
        restored
    }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    targetNoteId: Long,
    existingNote: NoteWithTags?,
    defaultPaperStyle: Int,
    initialSubjectId: String? = null,
    subjects: List<SubjectEntity>,
    onBack: () -> Unit,
    onCreateSubject: (String, Long, (SubjectEntity) -> Unit) -> Unit,
    onAutoSave: suspend (
        existingId: Long,
        title: String,
        subjectId: String,
        content: String,
        formulas: List<FormulaBlock>,
        imagePaths: List<String>,
        isPinned: Boolean,
        needsReview: Boolean,
        paperStyle: Int,
        createdAt: Long?
    ) -> Long,
    onSave: (
        existingId: Long,
        title: String,
        subjectId: String,
        content: String,
        formulas: List<FormulaBlock>,
        imagePaths: List<String>,
        isPinned: Boolean,
        needsReview: Boolean,
        paperStyle: Int,
        createdAt: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeNoteId by rememberSaveable(targetNoteId) { mutableLongStateOf(targetNoteId) }
    var draftInitialized by rememberSaveable(targetNoteId) { mutableStateOf(targetNoteId == 0L) }
    var createdAtHolder by rememberSaveable(targetNoteId) { mutableLongStateOf(0L) }

    var title by rememberSaveable(targetNoteId) { mutableStateOf("") }
    var subjectId by rememberSaveable(targetNoteId, initialSubjectId) {
        mutableStateOf(initialSubjectId ?: SubjectCategory.MATH.name)
    }
    var bodyField by rememberSaveable(targetNoteId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var isPinned by rememberSaveable(targetNoteId) { mutableStateOf(false) }
    var needsReview by rememberSaveable(targetNoteId) { mutableStateOf(false) }
    var paperStyle by rememberSaveable(targetNoteId) { mutableIntStateOf(defaultPaperStyle) }
    val formulaStates = rememberSaveable(targetNoteId, saver = FormulaDraftListSaver) {
        mutableStateListOf()
    }
    val imagePaths = rememberSaveable(targetNoteId, saver = StringStateListSaver) {
        mutableStateListOf()
    }

    var lastSavedDigest by rememberSaveable(targetNoteId) { mutableStateOf("") }
    var autoSaveStatus by remember { mutableStateOf(AutoSaveStatus.IDLE) }
    val editorSaveMutex = remember { Mutex() }
    var isSaving by remember { mutableStateOf(false) }

    var showEditorToolbar by rememberSaveable { mutableStateOf(true) }
    var userToggledToolbar by rememberSaveable(targetNoteId) { mutableStateOf(false) }
    var toolbarTabName by rememberSaveable(targetNoteId, initialSubjectId) {
        val defaultTab = if (initialSubjectId != null && !SubjectCategory.isStemSubject(initialSubjectId)) {
            EditorToolbarTab.MARKDOWN.name
        } else {
            EditorToolbarTab.MATH.name
        }
        mutableStateOf(defaultTab)
    }
    val toolbarTab = EditorToolbarTab.entries.firstOrNull { it.name == toolbarTabName } ?: EditorToolbarTab.MATH

    fun buildCurrentDigest(): String {
        val fDigest = formulaStates.joinToString("|") {
            "${it.id}:${it.blockType}:${it.title}:${it.expression.text}:${it.variablesNote}"
        }
        return listOf(
            title,
            subjectId,
            bodyField.text,
            isPinned.toString(),
            needsReview.toString(),
            paperStyle.toString(),
            imagePaths.joinToString(","),
            fDigest
        ).joinToString("§")
    }

    LaunchedEffect(targetNoteId, existingNote) {
        if (!draftInitialized && existingNote != null && existingNote.note.id == targetNoteId) {
            val n = existingNote.note
            createdAtHolder = n.createdAt
            title = n.title
            subjectId = n.subject
            if (!userToggledToolbar) {
                toolbarTabName = if (SubjectCategory.isStemSubject(n.subject)) {
                    EditorToolbarTab.MATH.name
                } else {
                    EditorToolbarTab.MARKDOWN.name
                }
            }
            bodyField = TextFieldValue(n.content, TextRange(n.content.length))
            isPinned = n.isPinned
            needsReview = n.needsReview
            paperStyle = n.paperStyle
            formulaStates.clear()
            n.formulas.forEach { f ->
                formulaStates.add(
                    EditableFormulaState(
                        id = f.id,
                        title = f.title,
                        expression = f.expression,
                        variablesNote = f.variablesNote,
                        mastered = f.mastered,
                        blockType = f.blockType,
                        repetitions = f.repetitions,
                        intervalDays = f.intervalDays,
                        easeFactor = f.easeFactor,
                        nextReviewAtMillis = f.nextReviewAtMillis,
                        lastReviewedAtMillis = f.lastReviewedAtMillis
                    )
                )
            }
            imagePaths.clear()
            n.imagePaths.forEach { imagePaths.add(it) }
            lastSavedDigest = buildCurrentDigest()
            draftInitialized = true
        } else if (draftInitialized && lastSavedDigest.isEmpty() && targetNoteId == 0L) {
            lastSavedDigest = buildCurrentDigest()
        }
    }

    val currentDigest = buildCurrentDigest()
    val hasPersistableContent = title.isNotBlank() ||
        bodyField.text.isNotBlank() ||
        imagePaths.isNotEmpty() ||
        formulaStates.any { it.expression.text.isNotBlank() || it.title.isNotBlank() }

    // Auto-save with 1.5s debounce (Feature 3)
    LaunchedEffect(currentDigest, draftInitialized, hasPersistableContent) {
        if (!draftInitialized || lastSavedDigest.isEmpty()) return@LaunchedEffect
        if (!hasPersistableContent) {
            autoSaveStatus = AutoSaveStatus.IDLE
            return@LaunchedEffect
        }
        if (currentDigest == lastSavedDigest) return@LaunchedEffect

        autoSaveStatus = AutoSaveStatus.SAVING
        delay(1500L)

        editorSaveMutex.withLock {
            if (isSaving) return@withLock
            val resolvedTitle = title.trim().ifBlank {
                formulaStates.firstOrNull { it.title.isNotBlank() }?.title
                    ?: bodyField.text.trim().lineSequence().firstOrNull()?.take(36)
                    ?: context.getString(R.string.editor_untitled_note)
            }
            val createdAtParam = if (createdAtHolder > 0L) createdAtHolder else System.currentTimeMillis()
            val savedId = onAutoSave(
                activeNoteId,
                resolvedTitle,
                subjectId,
                bodyField.text,
                formulaStates.map { it.toFormulaBlock() },
                imagePaths.toList(),
                isPinned,
                needsReview,
                paperStyle,
                createdAtParam
            )
            if (savedId > 0L) {
                activeNoteId = savedId
                if (createdAtHolder == 0L) createdAtHolder = createdAtParam
                lastSavedDigest = buildCurrentDigest()
                autoSaveStatus = AutoSaveStatus.SAVED
            }
        }
    }

    var focusTargetName by rememberSaveable { mutableStateOf(EditorFocusTarget.BODY.name) }
    var activeFormulaId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusTarget = EditorFocusTarget.entries.firstOrNull { it.name == focusTargetName } ?: EditorFocusTarget.BODY

    var showCreateSubjectDialog by rememberSaveable { mutableStateOf(false) }
    var showTemplatesDialog by rememberSaveable { mutableStateOf(false) }
    var showNoteTemplatesDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var previewEvalFormula by remember { mutableStateOf<FormulaBlock?>(null) }
    var showValidationError by remember { mutableStateOf(false) }
    var zoomedImageFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Photo Picker launcher (zero storage permissions needed)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    NoteImageStorage.saveCompressedImageFromUri(context, uri)
                }
                if (saved != null) {
                    imagePaths.add(saved)
                } else {
                    Toast.makeText(context, context.getString(R.string.editor_image_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera capture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (success && path != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    NoteImageStorage.saveCompressedImageFromFile(context, File(path))
                }
                if (saved != null) {
                    imagePaths.add(saved)
                } else {
                    Toast.makeText(context, context.getString(R.string.editor_image_error), Toast.LENGTH_SHORT).show()
                }
            }
        } else if (path != null) {
            runCatching { File(path).delete() }
        }
    }

    fun launchCameraCapture() {
        var tempFile: File? = null
        runCatching {
            val (createdFile, tempUri) = NoteImageStorage.createCameraTempUri(context)
            tempFile = createdFile
            pendingCameraPath = createdFile.absolutePath
            takePictureLauncher.launch(tempUri)
        }.onFailure {
            pendingCameraPath = null
            runCatching { tempFile?.delete() }
            Toast.makeText(
                context,
                context.getString(R.string.toast_no_app_to_handle),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val hasUnsavedChanges = draftInitialized &&
        lastSavedDigest.isNotEmpty() &&
        currentDigest != lastSavedDigest

    fun attemptBack() {
        if (isSaving) return
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    fun attemptSave() {
        if (isSaving) return
        if (!hasPersistableContent) {
            showValidationError = true
            return
        }
        isSaving = true
        scope.launch {
            try {
                editorSaveMutex.withLock {
                    val resolvedTitle = title.trim().ifBlank {
                        formulaStates.firstOrNull { it.title.isNotBlank() }?.title
                            ?: bodyField.text.trim().lineSequence().firstOrNull()?.take(36)
                            ?: context.getString(R.string.editor_untitled_note)
                    }
                    val createdAtParam = when {
                        createdAtHolder > 0L -> createdAtHolder
                        existingNote != null -> existingNote.note.createdAt
                        else -> null
                    }
                    onSave(
                        activeNoteId,
                        resolvedTitle,
                        subjectId,
                        bodyField.text,
                        formulaStates.map { it.toFormulaBlock() },
                        imagePaths.toList(),
                        isPinned,
                        needsReview,
                        paperStyle,
                        createdAtParam
                    )
                }
            } catch (_: Exception) {
                isSaving = false
            }
        }
    }

    BackHandler { attemptBack() }

    fun insertAtActiveCursor(snippet: String) {
        if (focusTarget == EditorFocusTarget.FORMULA_EXPR && activeFormulaId != null) {
            val target = formulaStates.firstOrNull { it.id == activeFormulaId }
            if (target != null) {
                target.expression = insertTextAtSelection(target.expression, snippet)
                return
            }
        }
        bodyField = insertTextAtSelection(bodyField, snippet)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(if (activeNoteId == 0L) R.string.editor_title_new else R.string.editor_title_edit),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (autoSaveStatus != AutoSaveStatus.IDLE) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = if (autoSaveStatus == AutoSaveStatus.SAVED) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (autoSaveStatus == AutoSaveStatus.SAVING) {
                                            R.string.editor_autosave_saving
                                        } else {
                                            R.string.editor_autosave_saved
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoSaveStatus == AutoSaveStatus.SAVED) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::attemptBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditorToolbar = !showEditorToolbar }) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = stringResource(R.string.editor_toggle_math_toolbar),
                            tint = if (showEditorToolbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(R.string.pin_action_pin),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { needsReview = !needsReview }) {
                        Icon(
                            imageVector = if (needsReview) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.badge_needs_review),
                            tint = if (needsReview) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = ::attemptSave,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier
                            .padding(end = Dimens.Space8)
                            .testTag("save_note_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(
                            text = stringResource(R.string.action_save),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = showEditorToolbar) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = Dimens.ElevationNone
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.Space12, vertical = Dimens.Space4),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = toolbarTab == EditorToolbarTab.MARKDOWN,
                                onClick = {
                                    userToggledToolbar = true
                                    toolbarTabName = EditorToolbarTab.MARKDOWN.name
                                },
                                label = { Text(stringResource(R.string.editor_toolbar_markdown)) }
                            )
                            FilterChip(
                                selected = toolbarTab == EditorToolbarTab.MATH,
                                onClick = {
                                    userToggledToolbar = true
                                    toolbarTabName = EditorToolbarTab.MATH.name
                                },
                                label = { Text(stringResource(R.string.editor_toolbar_math)) }
                            )
                        }
                        if (toolbarTab == EditorToolbarTab.MARKDOWN) {
                            MarkdownShortcutBar(
                                onBold = { bodyField = wrapSelection(bodyField, "**") },
                                onHighlight = { bodyField = wrapSelection(bodyField, "==") },
                                onHeading = { bodyField = insertLinePrefix(bodyField, "# ") },
                                onBullet = { bodyField = insertLinePrefix(bodyField, "- ") },
                                onChecklist = { bodyField = insertLinePrefix(bodyField, "[ ] ") }
                            )
                        } else {
                            MathSymbolToolbar(
                                onInsertSymbol = { sym -> insertAtActiveCursor(sym) },
                                onOpenTemplates = { showTemplatesDialog = true }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
            ) {
                if (showValidationError) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(R.string.editor_validation_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showValidationError = false
                    },
                    label = { Text(stringResource(R.string.editor_title_label)) },
                    placeholder = { Text(stringResource(R.string.editor_title_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) focusTargetName = EditorFocusTarget.OTHER.name }
                        .testTag("note_title_input")
                )

                // Subject Selector + Subject Cartoon Companion & Smart Note Templates
                val currentSubjectEntity = subjects.resolveSubject(subjectId)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudentMascotAvatar(
                                sizeDp = 34,
                                drawableRes = currentSubjectEntity.mascotDrawableRes(),
                                contentDescription = currentSubjectEntity.displayName()
                            )
                            Text(
                                text = stringResource(R.string.editor_subject_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(
                            onClick = { showNoteTemplatesDialog = true },
                            label = { Text(stringResource(R.string.editor_note_templates_btn)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("open_note_templates_button")
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subjects.forEach { cat ->
                            FilterChip(
                                selected = subjectId == cat.id,
                                onClick = {
                                    subjectId = cat.id
                                    if (!userToggledToolbar) {
                                        toolbarTabName = if (SubjectCategory.isStemSubject(cat.id)) {
                                            EditorToolbarTab.MATH.name
                                        } else {
                                            EditorToolbarTab.MARKDOWN.name
                                        }
                                    }
                                },
                                label = { Text(cat.displayName()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cat.accentStripeColor(),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        AssistChip(
                            onClick = { showCreateSubjectDialog = true },
                            label = { Text(stringResource(R.string.subject_add_button)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Paper Style Picker
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.editor_paper_style_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    listOf(
                        0 to R.string.paper_style_lined,
                        1 to R.string.paper_style_grid,
                        2 to R.string.paper_style_plain
                    ).forEach { (styleId, labelRes) ->
                        FilterChip(
                            selected = paperStyle == styleId,
                            onClick = { paperStyle = styleId },
                            label = { Text(stringResource(labelRes)) },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                // Note Body Content
                OutlinedTextField(
                    value = bodyField,
                    onValueChange = {
                        bodyField = it
                        showValidationError = false
                    },
                    label = { Text(stringResource(R.string.editor_body_label)) },
                    placeholder = { Text(stringResource(R.string.editor_body_placeholder)) },
                    minLines = 5,
                    maxLines = 14,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusTargetName = EditorFocusTarget.BODY.name
                            }
                        }
                        .testTag("note_body_input")
                )

                // Image Attachments Section (Feature 1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.editor_images_header, imagePaths.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pick_gallery_image_button")
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.editor_pick_gallery))
                            }
                            OutlinedButton(
                                onClick = { launchCameraCapture() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("take_photo_button")
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.editor_take_photo))
                            }
                        }
                    }

                    if (imagePaths.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            imagePaths.forEach { fileName ->
                                NoteImageThumbnail(
                                    imageFileName = fileName,
                                    size = 96.dp,
                                    onClick = { zoomedImageFileName = fileName },
                                    onRemove = {
                                        imagePaths.remove(fileName)
                                        showValidationError = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Formula & Flashcard Blocks Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Functions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.editor_formulas_header, formulaStates.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newId = UUID.randomUUID().toString()
                                formulaStates.add(
                                    EditableFormulaState(
                                        id = newId,
                                        title = "",
                                        expression = "",
                                        variablesNote = "",
                                        mastered = false,
                                        blockType = FormulaBlock.BLOCK_TYPE_FLASHCARD
                                    )
                                )
                                activeFormulaId = newId
                                focusTargetName = EditorFocusTarget.OTHER.name
                                toolbarTabName = EditorToolbarTab.MARKDOWN.name
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_flashcard_block_button")
                        ) {
                            Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.editor_add_flashcard), maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = {
                                val newId = UUID.randomUUID().toString()
                                formulaStates.add(
                                    EditableFormulaState(
                                        id = newId,
                                        title = "",
                                        expression = "",
                                        variablesNote = "",
                                        mastered = false,
                                        blockType = FormulaBlock.BLOCK_TYPE_FORMULA
                                    )
                                )
                                activeFormulaId = newId
                                focusTargetName = EditorFocusTarget.FORMULA_EXPR.name
                                toolbarTabName = EditorToolbarTab.MATH.name
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_formula_block_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.editor_add_formula), maxLines = 1, softWrap = false)
                        }
                    }
                }

                formulaStates.forEachIndexed { index, fState ->
                    val isFlashcard = fState.blockType == FormulaBlock.BLOCK_TYPE_FLASHCARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.Radius16),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            if (activeFormulaId == fState.id && focusTarget == EditorFocusTarget.FORMULA_EXPR) {
                                Dimens.BorderSelected
                            } else {
                                Dimens.BorderThin
                            },
                            if (activeFormulaId == fState.id && focusTarget == EditorFocusTarget.FORMULA_EXPR) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.Space16),
                            verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFlashcard) Icons.Rounded.Style else Icons.Rounded.Functions,
                                        contentDescription = null,
                                        tint = if (isFlashcard) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_item_title else R.string.editor_formula_item_title,
                                            index + 1
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFlashcard) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row {
                                    if (!isFlashcard && fState.expression.text.isNotBlank()) {
                                        IconButton(onClick = { previewEvalFormula = fState.toFormulaBlock() }) {
                                            Icon(
                                                Icons.Rounded.Calculate,
                                                contentDescription = stringResource(R.string.editor_test_eval_formula),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { formulaStates.remove(fState) }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.editor_remove_formula_box),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = fState.title,
                                onValueChange = {
                                    fState.title = it
                                    showValidationError = false
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_term_label else R.string.editor_formula_name_label
                                        )
                                    )
                                },
                                placeholder = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_term_placeholder else R.string.editor_formula_name_placeholder
                                        )
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) focusTargetName = EditorFocusTarget.OTHER.name }
                            )

                            OutlinedTextField(
                                value = fState.expression,
                                onValueChange = {
                                    fState.expression = it
                                    showValidationError = false
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_answer_label else R.string.editor_formula_expr_label
                                        )
                                    )
                                },
                                placeholder = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_answer_placeholder else R.string.editor_formula_expr_placeholder
                                        )
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = if (isFlashcard) FontFamily.Default else JetBrainsMonoFontFamily,
                                    fontSize = if (isFlashcard) 15.sp else 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            focusTargetName = EditorFocusTarget.FORMULA_EXPR.name
                                            activeFormulaId = fState.id
                                            if (!isFlashcard) {
                                                toolbarTabName = EditorToolbarTab.MATH.name
                                            }
                                        }
                                    }
                                    .testTag("formula_expr_input_$index")
                            )

                            OutlinedTextField(
                                value = fState.variablesNote,
                                onValueChange = { fState.variablesNote = it },
                                label = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_hint_label else R.string.editor_formula_vars_label
                                        )
                                    )
                                },
                                placeholder = {
                                    Text(
                                        stringResource(
                                            if (isFlashcard) R.string.editor_flashcard_hint_placeholder else R.string.editor_formula_vars_placeholder
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) focusTargetName = EditorFocusTarget.OTHER.name }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.editor_unsaved_title)) },
            text = { Text(stringResource(R.string.editor_unsaved_message)) },
            confirmButton = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { showDiscardDialog = false },
                        modifier = Modifier.testTag("unsaved_cancel_button")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onBack()
                        },
                        modifier = Modifier.testTag("unsaved_discard_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_discard),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            attemptSave()
                        },
                        modifier = Modifier.testTag("unsaved_save_button")
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        )
    }

    if (showCreateSubjectDialog) {
        CustomSubjectDialog(
            onDismiss = { showCreateSubjectDialog = false },
            onConfirm = { name, colorHex ->
                onCreateSubject(name, colorHex) { created ->
                    subjectId = created.id
                }
                showCreateSubjectDialog = false
            }
        )
    }

    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = { Text(stringResource(R.string.editor_templates_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SymbolPaletteData.structureHelpers.forEach { helper ->
                        Surface(
                            onClick = {
                                insertAtActiveCursor(helper.snippet)
                                showTemplatesDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(helper.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = helper.snippet,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatesDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    if (showNoteTemplatesDialog) {
        val noteTemplates = listOf(
            Triple(R.string.template_cornell_title, R.string.template_cornell_desc, R.string.template_cornell_body),
            Triple(R.string.template_vocab_title, R.string.template_vocab_desc, R.string.template_vocab_body),
            Triple(R.string.template_history_title, R.string.template_history_desc, R.string.template_history_body),
            Triple(R.string.template_stem_title, R.string.template_stem_desc, R.string.template_stem_body)
        )
        AlertDialog(
            onDismissRequest = { showNoteTemplatesDialog = false },
            title = { Text(stringResource(R.string.editor_note_templates_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    noteTemplates.forEach { (titleRes, descRes, bodyRes) ->
                        Surface(
                            onClick = {
                                val tpl = context.getString(bodyRes)
                                val updated = if (bodyField.text.isBlank()) {
                                    tpl
                                } else {
                                    "${bodyField.text.trimEnd()}\n\n$tpl"
                                }
                                bodyField = TextFieldValue(updated, TextRange(updated.length))
                                showValidationError = false
                                showNoteTemplatesDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(descRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNoteTemplatesDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    previewEvalFormula?.let { formula ->
        FormulaEvaluatorDialog(
            formula = formula,
            onDismiss = { previewEvalFormula = null }
        )
    }

    zoomedImageFileName?.let { fileName ->
        ZoomableImageDialog(
            imageFileName = fileName,
            onDismiss = { zoomedImageFileName = null }
        )
    }
}

@Composable
private fun MarkdownShortcutBar(
    onBold: () -> Unit,
    onHighlight: () -> Unit,
    onHeading: () -> Unit,
    onBullet: () -> Unit,
    onChecklist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MarkdownActionButton(Icons.Rounded.FormatBold, stringResource(R.string.md_bar_bold), onBold)
        MarkdownActionButton(Icons.Rounded.Highlight, stringResource(R.string.md_bar_highlight), onHighlight)
        MarkdownActionButton(Icons.Rounded.Title, stringResource(R.string.md_bar_heading), onHeading)
        MarkdownActionButton(Icons.AutoMirrored.Rounded.FormatListBulleted, stringResource(R.string.md_bar_bullet), onBullet)
        MarkdownActionButton(Icons.Rounded.CheckBox, stringResource(R.string.md_bar_checklist), onChecklist)
    }
}

@Composable
private fun MarkdownActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun insertTextAtSelection(field: TextFieldValue, insertion: String): TextFieldValue {
    val start = field.selection.min.coerceIn(0, field.text.length)
    val end = field.selection.max.coerceIn(0, field.text.length)
    val updatedText = buildString {
        append(field.text.substring(0, start))
        append(insertion)
        append(field.text.substring(end))
    }
    val cursor = start + insertion.length
    return TextFieldValue(text = updatedText, selection = TextRange(cursor))
}

private fun wrapSelection(field: TextFieldValue, marker: String): TextFieldValue {
    val start = field.selection.min.coerceIn(0, field.text.length)
    val end = field.selection.max.coerceIn(0, field.text.length)
    return if (start < end) {
        val selected = field.text.substring(start, end)
        val updated = field.text.substring(0, start) + marker + selected + marker + field.text.substring(end)
        val newCursor = start + marker.length + selected.length + marker.length
        TextFieldValue(updated, TextRange(newCursor))
    } else {
        val updated = field.text.substring(0, start) + marker + marker + field.text.substring(end)
        TextFieldValue(updated, TextRange(start + marker.length))
    }
}

private fun insertLinePrefix(field: TextFieldValue, prefix: String): TextFieldValue {
    val pos = field.selection.min.coerceIn(0, field.text.length)
    val lineStart = field.text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
        .let { if (it < 0 || pos == 0) 0 else it + 1 }
    val existingLine = field.text.substring(lineStart)
    return if (existingLine.startsWith(prefix)) {
        TextFieldValue(field.text, TextRange((lineStart + prefix.length).coerceAtMost(field.text.length)))
    } else {
        val updated = field.text.substring(0, lineStart) + prefix + field.text.substring(lineStart)
        TextFieldValue(updated, TextRange(pos + prefix.length))
    }
}
