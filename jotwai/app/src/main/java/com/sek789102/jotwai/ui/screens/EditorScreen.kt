package com.sek789102.jotwai.ui.screens

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.ChecklistItem
import com.sek789102.jotwai.data.model.NoteEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.RepeatRule
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.ui.components.FormatToolbar
import com.sek789102.jotwai.domain.TextEditing
import com.sek789102.jotwai.ui.components.ColorPalette
import com.sek789102.jotwai.ui.components.ConfirmDialog
import com.sek789102.jotwai.ui.components.DrawingDialog
import com.sek789102.jotwai.ui.components.FolderPickerDialog
import com.sek789102.jotwai.ui.components.LabelPickerDialog
import com.sek789102.jotwai.ui.components.MetaChip
import com.sek789102.jotwai.ui.components.NoteImage
import com.sek789102.jotwai.ui.components.ReminderDialog
import com.sek789102.jotwai.ui.components.ZoomableImageDialog
import com.sek789102.jotwai.ui.theme.NoteColors
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import com.sek789102.jotwai.ui.viewmodel.EditorViewModel
import com.sek789102.jotwai.util.Dates
import com.sek789102.jotwai.util.DeviceAuth
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    appVm: AppViewModel,
    prefs: UserPreferences,
    darkTheme: Boolean,
    focusBodyOnOpen: Boolean,
    snackbar: androidx.compose.material3.SnackbarHostState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNote: (Long) -> Unit
) {
    val context = LocalContext.current
    val vm: EditorViewModel = viewModel(
        key = "editor_$noteId",
        factory = EditorViewModel.Factory(context.applicationContext as Application, noteId)
    )
    val live by vm.note.collectAsStateWithLifecycle()
    val folders by appVm.folders.collectAsStateWithLifecycle()
    val labels by appVm.labels.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val note: NoteEntity? = live?.note
    val trashed = note?.isTrashed == true
    val readOnly = trashed

    var menuOpen by remember { mutableStateOf(false) }
    var addSheet by remember { mutableStateOf(false) }
    var colorSheet by remember { mutableStateOf(false) }
    var showFormat by rememberSaveable { mutableStateOf(false) }
    var reminderDialog by remember { mutableStateOf(false) }
    var labelDialog by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(false) }
    var zoomImage by remember { mutableStateOf<String?>(null) }
    var removeImage by remember { mutableStateOf<String?>(null) }
    var confirmDeleteForever by remember { mutableStateOf(false) }
    var doneExpanded by rememberSaveable { mutableStateOf(true) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }

    fun leave() {
        vm.flush(discardIfBlank = true)
        onBack()
    }
    BackHandler { leave() }

    if (vm.loaded && vm.missing) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) appVm.notify(context.getString(R.string.msg_notif_denied))
    }
    fun ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        scope.launch {
            uris.forEach { uri ->
                val saved = withContext(Dispatchers.IO) { NoteImageStorage.saveCompressedImageFromUri(context, uri) }
                if (saved != null) vm.addImage(saved)
                else Toast.makeText(context, R.string.msg_image_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCamera
        pendingCamera = null
        if (path == null) return@rememberLauncherForActivityResult
        if (!ok) {
            File(path).delete()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) { NoteImageStorage.saveCompressedImageFromFile(context, File(path)) }
            if (saved != null) vm.addImage(saved) else Toast.makeText(context, R.string.msg_image_failed, Toast.LENGTH_SHORT).show()
        }
    }
    fun launchCamera() {
        runCatching {
            val (file, uri) = NoteImageStorage.createCameraTempUri(context)
            pendingCamera = file.absolutePath
            takePhoto.launch(uri)
        }.onFailure {
            pendingCamera = null
            Toast.makeText(context, R.string.msg_no_camera, Toast.LENGTH_SHORT).show()
        }
    }

    fun share() {
        val n = note ?: return
        val text = buildString {
            if (vm.title.text.isNotBlank()) append(vm.title.text.trim()).append("\n\n")
            if (vm.type == NoteType.CHECKLIST) {
                vm.checklist.filter { it.text.isNotBlank() }.forEach { append(if (it.checked) "☑ " else "☐ ").append(it.text).append('\n') }
            } else {
                append(TextEditing.toPlainText(vm.body.text))
            }
        }.trim()
        val uris = vm.images.mapNotNull { name ->
            val f = NoteImageStorage.resolveImageFile(context, name)
            if (f.isFile) runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f) }.getOrNull() else null
        }
        val intent = when {
            uris.isEmpty() -> Intent(Intent.ACTION_SEND).setType("text/plain")
            uris.size == 1 -> Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uris[0])
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }.putExtra(Intent.EXTRA_TEXT, text)
            .putExtra(Intent.EXTRA_SUBJECT, n.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.msg_no_share_app, Toast.LENGTH_SHORT).show()
        }
    }

    val bg = if ((note?.color ?: 0) == 0) MaterialTheme.colorScheme.surface else NoteColors.of(note!!.color, darkTheme)

    Scaffold(
        containerColor = bg,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                navigationIcon = {
                    IconButton(onClick = { leave() }, modifier = Modifier.testTag("editor_back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                title = {},
                actions = {
                    if (note != null && !trashed) {
                        IconButton(onClick = { vm.setPinned(!note.isPinned) }, modifier = Modifier.testTag("editor_pin")) {
                            Icon(
                                if (note.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                                stringResource(if (note.isPinned) R.string.action_unpin else R.string.action_pin)
                            )
                        }
                        IconButton(onClick = { reminderDialog = true }, modifier = Modifier.testTag("editor_reminder")) {
                            Icon(Icons.Outlined.AddAlert, stringResource(R.string.reminder_title))
                        }
                        IconButton(onClick = {
                            appVm.setArchived(setOf(noteId), !note.isArchived)
                            leave()
                        }) {
                            Icon(
                                if (note.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                stringResource(if (note.isArchived) R.string.action_unarchive else R.string.action_archive)
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("editor_more")) {
                                Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                MenuItem({ menuOpen = false }, R.string.action_labels, Icons.AutoMirrored.Outlined.Label, "menu_labels") { labelDialog = true }
                                MenuItem({ menuOpen = false }, R.string.action_move_to_folder, Icons.Outlined.Folder, "menu_folder") { folderDialog = true }
                                MenuItem({ menuOpen = false },
                                    if (vm.type == NoteType.TEXT) R.string.action_to_checklist else R.string.action_to_text,
                                    if (vm.type == NoteType.TEXT) Icons.Outlined.CheckBox else Icons.Outlined.Notes,
                                    "menu_convert"
                                ) { vm.convertType() }
                                MenuItem({ menuOpen = false },
                                    if (note.isLocked) R.string.action_unlock else R.string.action_lock,
                                    if (note.isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                    "menu_lock"
                                ) {
                                    if (!note.isLocked && !DeviceAuth.isAvailable(context)) {
                                        appVm.notify(context.getString(R.string.msg_need_screen_lock))
                                    } else {
                                        appVm.setLocked(setOf(noteId), !note.isLocked)
                                        if (!note.isLocked) appVm.markUnlocked(noteId)
                                    }
                                }
                                MenuItem({ menuOpen = false }, R.string.action_history, Icons.Outlined.History, "menu_history") {
                                    vm.flush(discardIfBlank = false)
                                    onOpenHistory()
                                }
                                MenuItem({ menuOpen = false }, R.string.action_duplicate, Icons.Outlined.ContentCopy, "menu_duplicate") {
                                    vm.flush(discardIfBlank = false)
                                    appVm.duplicate(noteId) { onOpenNote(it) }
                                }
                                MenuItem({ menuOpen = false }, R.string.action_share, Icons.Outlined.Share, "menu_share") { share() }
                                MenuItem({ menuOpen = false }, R.string.action_delete, Icons.Outlined.Delete, "menu_delete") {
                                    vm.flush(discardIfBlank = false)
                                    appVm.moveToTrash(setOf(noteId))
                                    onBack()
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!readOnly) {
                Column(Modifier.background(bg).navigationBarsPadding().imePadding()) {
                    if (showFormat && vm.type == NoteType.TEXT) {
                        FormatToolbar(vm.body) { transform -> vm.applyBodyFormat(transform) }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { addSheet = true }, modifier = Modifier.testTag("editor_add")) {
                            Icon(Icons.Outlined.AddBox, stringResource(R.string.action_add))
                        }
                        IconButton(onClick = { colorSheet = true }, modifier = Modifier.testTag("editor_color")) {
                            Icon(Icons.Outlined.Palette, stringResource(R.string.action_color))
                        }
                        if (vm.type == NoteType.TEXT) {
                            IconButton(onClick = { showFormat = !showFormat }, modifier = Modifier.testTag("editor_format")) {
                                Icon(Icons.Outlined.TextFields, stringResource(R.string.action_format), tint = if (showFormat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            note?.let { Dates.edited(context, it.updatedAt) }.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { vm.undo() }, enabled = vm.canUndo, modifier = Modifier.testTag("editor_undo")) {
                            Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.action_undo))
                        }
                        IconButton(onClick = { vm.redo() }, enabled = vm.canRedo, modifier = Modifier.testTag("editor_redo")) {
                            Icon(Icons.AutoMirrored.Outlined.Redo, stringResource(R.string.action_redo))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (!vm.loaded || note == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (trashed) {
                TrashBanner(
                    onRestore = { appVm.restore(setOf(noteId)) },
                    onDelete = { confirmDeleteForever = true }
                )
            }
            ImagesBlock(
                images = vm.images,
                readOnly = readOnly,
                onOpen = { zoomImage = it },
                onRemove = { removeImage = it }
            )
            val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = vm.title,
                onValueChange = vm::onTitleChange,
                readOnly = readOnly,
                textStyle = titleStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("editor_title"),
                decorationBox = { inner ->
                    Box {
                        if (vm.title.text.isEmpty()) Text(stringResource(R.string.hint_title), style = titleStyle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        inner()
                    }
                }
            )
            if (vm.type == NoteType.TEXT) {
                BodyField(
                    value = vm.body,
                    onChange = vm::onBodyChange,
                    readOnly = readOnly,
                    autoFocus = focusBodyOnOpen && !readOnly
                )
            } else {
                ChecklistEditor(
                    vm = vm,
                    prefs = prefs,
                    readOnly = readOnly,
                    doneExpanded = doneExpanded,
                    onToggleDoneExpanded = { doneExpanded = !doneExpanded }
                )
            }
            // Reminder / folder / labels
            val folder = folders.firstOrNull { it.id == note.folderId }
            FlowRow(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                note.reminderAt?.let { at ->
                    MetaChip(
                        if (note.reminderRepeat == RepeatRule.NONE) Icons.Outlined.Alarm else Icons.Outlined.Repeat,
                        Dates.dayAndTime(context, at),
                        strike = at < System.currentTimeMillis(),
                        onClick = if (readOnly) null else ({ reminderDialog = true })
                    )
                }
                folder?.let { MetaChip(Icons.Outlined.Folder, it.name, onClick = if (readOnly) null else ({ folderDialog = true })) }
                live?.labels.orEmpty().forEach { l ->
                    MetaChip(Icons.AutoMirrored.Outlined.Label, l.name, onClick = if (readOnly) null else ({ labelDialog = true }))
                }
                if (note.isLocked) MetaChip(Icons.Outlined.Lock, stringResource(R.string.locked_chip))
            }
            Text(
                stringResource(R.string.created_at, Dates.full(note.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    // ---- sheets & dialogs ----
    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(bottom = 24.dp)) {
                SheetRow(Icons.Outlined.PhotoCamera, R.string.add_photo_camera, "add_camera") { addSheet = false; launchCamera() }
                SheetRow(Icons.Outlined.Image, R.string.add_photo_gallery, "add_gallery") {
                    addSheet = false
                    pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SheetRow(Icons.Outlined.Brush, R.string.add_drawing, "add_drawing") { addSheet = false; drawing = true }
                SheetRow(
                    if (vm.type == NoteType.TEXT) Icons.Outlined.CheckBox else Icons.Outlined.Notes,
                    if (vm.type == NoteType.TEXT) R.string.action_to_checklist else R.string.action_to_text,
                    "add_convert"
                ) { addSheet = false; vm.convertType() }
            }
        }
    }
    if (colorSheet && note != null) {
        ModalBottomSheet(onDismissRequest = { colorSheet = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
                Text(stringResource(R.string.action_color), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                ColorPalette(selected = note.color, darkTheme = darkTheme, onSelect = { vm.setColor(it) })
            }
        }
    }
    if (reminderDialog && note != null) {
        ReminderDialog(
            currentAt = note.reminderAt,
            currentRepeat = note.reminderRepeat,
            onDismiss = { reminderDialog = false },
            onSet = { at, r ->
                vm.setReminder(at, r)
                ensureNotificationPermission()
            },
            onRemove = { vm.setReminder(null, RepeatRule.NONE) }
        )
    }
    if (labelDialog) {
        LabelPickerDialog(
            labels = labels,
            attached = live?.labels.orEmpty().map { it.id }.toSet(),
            onDismiss = { labelDialog = false },
            onToggle = { id, on -> vm.setLabel(id, on) },
            onCreate = { vm.createAndAttachLabel(it) }
        )
    }
    if (folderDialog && note != null) {
        FolderPickerDialog(
            folders = folders,
            current = note.folderId,
            onDismiss = { folderDialog = false },
            onPick = { vm.setFolder(it) },
            onCreate = { name -> appVm.createFolder(name) { vm.setFolder(it) } }
        )
    }
    if (drawing) DrawingDialog(onDismiss = { drawing = false }, onSaved = { vm.addImage(it) })
    zoomImage?.let { ZoomableImageDialog(imageFileName = it, onDismiss = { zoomImage = null }) }
    removeImage?.let { name ->
        ConfirmDialog(
            title = stringResource(R.string.image_remove_title),
            message = stringResource(R.string.image_remove_message),
            confirmLabel = stringResource(R.string.action_remove),
            onDismiss = { removeImage = null },
            onConfirm = { vm.removeImage(name) }
        )
    }
    if (confirmDeleteForever) {
        ConfirmDialog(
            title = stringResource(R.string.delete_forever_title),
            message = stringResource(R.string.delete_forever_message_one),
            confirmLabel = stringResource(R.string.action_delete_forever),
            onDismiss = { confirmDeleteForever = false },
            onConfirm = {
                appVm.deleteForever(setOf(noteId))
                onBack()
            }
        )
    }
}

@Composable
private fun MenuItem(close: () -> Unit, text: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(text)) },
        leadingIcon = { Icon(icon, null) },
        onClick = { close(); onClick() },
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: Int, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(20.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TrashBanner(onRestore: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.trash_note_banner), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteForever, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete_forever))
                }
                TextButton(onClick = onRestore, modifier = Modifier.testTag("editor_restore")) {
                    Icon(Icons.Outlined.RestoreFromTrash, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_restore))
                }
            }
        }
    }
}

@Composable
private fun ImagesBlock(images: List<String>, readOnly: Boolean, onOpen: (String) -> Unit, onRemove: (String) -> Unit) {
    if (images.isEmpty()) return
    val rows = if (images.size == 1) listOf(images) else images.chunked(if (images.size == 2 || images.size == 4) 2 else 3)
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { name ->
                    Box(
                        Modifier
                            .weight(1f)
                            .then(if (images.size == 1) Modifier.heightIn(max = 360.dp) else Modifier.aspectRatio(1f))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpen(name) }
                    ) {
                        NoteImage(
                            fileName = name,
                            maxPx = if (images.size == 1) 1400 else 600,
                            modifier = if (images.size == 1) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
                            contentScale = if (images.size == 1) ContentScale.FillWidth else ContentScale.Crop,
                            contentDescription = stringResource(R.string.image_desc)
                        )
                        if (!readOnly) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { onRemove(name) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Outlined.Close, stringResource(R.string.action_remove), tint = Color.White, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
                repeat((if (images.size == 1) 1 else row.size.coerceAtLeast(rows.first().size)) - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BodyField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    readOnly: Boolean,
    autoFocus: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    // Unspecified line height lets lines with large text grow instead of overlapping.
    val style = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
    val transformation = remember(scheme) {
        RichText.Transformation(
            RichText.Palette(
                marker = scheme.primary,
                highlight = Color(0x80FFD54F),
                done = scheme.onSurfaceVariant,
                codeBg = scheme.onSurface.copy(alpha = 0.08f),
                quote = scheme.onSurfaceVariant,
                rule = scheme.outlineVariant
            )
        )
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus && value.text.isEmpty()) runCatching { focus.requestFocus() } }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        readOnly = readOnly,
        textStyle = style,
        visualTransformation = transformation,
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .focusRequester(focus)
            .testTag("editor_body"),
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) Text(stringResource(R.string.hint_body), style = style, color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        }
    )
}

@Composable
private fun ChecklistEditor(
    vm: EditorViewModel,
    prefs: UserPreferences,
    readOnly: Boolean,
    doneExpanded: Boolean,
    onToggleDoneExpanded: () -> Unit
) {
    val split = prefs.checklistMoveCheckedDown
    val open = if (split) vm.checklist.filter { !it.checked } else vm.checklist
    val done = if (split) vm.checklist.filter { it.checked } else emptyList()
    val heights = remember { mutableStateMapOf<String, Int>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(Modifier.padding(horizontal = 8.dp)) {
        open.forEach { item ->
            key(item.id) {
                val isDragging = draggingId == item.id
                ChecklistRow(
                    item = item,
                    vm = vm,
                    readOnly = readOnly,
                    modifier = Modifier
                        .onSizeChanged { heights[item.id] = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        },
                    dragHandle = if (readOnly) Modifier else Modifier.pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingId = item.id; dragOffset = 0f },
                            onDragEnd = { draggingId = null; dragOffset = 0f },
                            onDragCancel = { draggingId = null; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val list = if (split) vm.checklist.filter { !it.checked } else vm.checklist
                                val i = list.indexOfFirst { it.id == item.id }
                                val h = (heights[item.id] ?: 120).toFloat()
                                if (dragOffset > h * 0.6f && i < list.lastIndex) {
                                    vm.swapChecklistItems(item.id, list[i + 1].id, recordStep = false)
                                    dragOffset -= (heights[list[i + 1].id] ?: h.toInt()).toFloat()
                                } else if (dragOffset < -h * 0.6f && i > 0) {
                                    vm.swapChecklistItems(item.id, list[i - 1].id, recordStep = false)
                                    dragOffset += (heights[list[i - 1].id] ?: h.toInt()).toFloat()
                                }
                            }
                        )
                    }
                )
            }
        }
        if (!readOnly) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { vm.addChecklistItem(atBottom = prefs.checklistAddToBottom) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("checklist_add"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.AddBox, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.checklist_add_item), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (done.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            var doneMenu by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggleDoneExpanded).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (doneExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.checklist_done_count, done.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!readOnly) {
                    Box {
                        IconButton(onClick = { doneMenu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                        DropdownMenu(expanded = doneMenu, onDismissRequest = { doneMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.checklist_uncheck_all)) }, onClick = { doneMenu = false; vm.uncheckAll() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.checklist_delete_done)) }, onClick = { doneMenu = false; vm.deleteCheckedItems() })
                        }
                    }
                }
            }
            if (doneExpanded) {
                done.forEach { item -> key(item.id) { ChecklistRow(item = item, vm = vm, readOnly = readOnly, modifier = Modifier, dragHandle = null) } }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    vm: EditorViewModel,
    readOnly: Boolean,
    modifier: Modifier,
    dragHandle: Modifier?
) {
    val scheme = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(item.text)) }
    if (field.text != item.text) field = TextFieldValue(item.text, androidx.compose.ui.text.TextRange(item.text.length))
    LaunchedEffect(vm.focusItemId) {
        if (vm.focusItemId == item.id) {
            runCatching { focus.requestFocus() }
            vm.focusItemId = null
        }
    }
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = if (item.checked) scheme.onSurfaceVariant else scheme.onSurface,
        textDecoration = if (item.checked) TextDecoration.LineThrough else null
    )
    Row(modifier.fillMaxWidth().background(Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
        if (dragHandle != null) {
            Icon(
                Icons.Outlined.DragIndicator,
                stringResource(R.string.checklist_drag),
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = dragHandle.padding(4.dp).size(20.dp)
            )
        } else {
            Spacer(Modifier.width(28.dp))
        }
        Checkbox(
            checked = item.checked,
            onCheckedChange = if (readOnly) null else ({ vm.toggleChecklistItem(item.id) }),
            modifier = Modifier.testTag("check_${item.id}")
        )
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                vm.setChecklistItemText(item.id, it.text)
            },
            readOnly = readOnly,
            textStyle = style,
            cursorBrush = SolidColor(scheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace && field.text.isEmpty()) {
                        vm.removeChecklistItem(item.id)
                        true
                    } else false
                }
                .testTag("item_${item.id}"),
            decorationBox = { inner ->
                Box {
                    if (field.text.isEmpty()) Text(stringResource(R.string.checklist_item_hint), style = style, color = scheme.onSurfaceVariant.copy(alpha = 0.5f))
                    inner()
                }
            }
        )
        if (!readOnly && focused) {
            IconButton(onClick = { vm.removeChecklistItem(item.id) }) {
                Icon(Icons.Outlined.Close, stringResource(R.string.action_remove), tint = scheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}
