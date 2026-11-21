package com.sek789102.jotwai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.SortOrder
import com.sek789102.jotwai.data.model.ViewMode
import com.sek789102.jotwai.data.repository.NotesRepository
import com.sek789102.jotwai.domain.SearchFilter
import com.sek789102.jotwai.domain.Section
import com.sek789102.jotwai.ui.components.ColorDialog
import com.sek789102.jotwai.ui.components.ConfirmDialog
import com.sek789102.jotwai.ui.components.EmptyState
import com.sek789102.jotwai.ui.components.FolderPickerDialog
import com.sek789102.jotwai.ui.components.LabelPickerDialog
import com.sek789102.jotwai.ui.components.NameDialog
import com.sek789102.jotwai.ui.components.NoteCard
import com.sek789102.jotwai.ui.theme.NoteColors
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    prefs: UserPreferences,
    darkTheme: Boolean,
    snackbar: SnackbarHostState,
    onOpenNote: (Long) -> Unit,
    onNewNote: (NoteType) -> Unit,
    onOpenSettings: () -> Unit,
    onManageLabels: () -> Unit
) {
    val home by vm.home.collectAsStateWithLifecycle()
    val section by vm.section.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var colorDialog by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var labelDialog by remember { mutableStateOf(false) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf(false) }
    var deleteFolder by remember { mutableStateOf(false) }


    val selecting = selection.isNotEmpty()
    androidx.activity.compose.BackHandler(enabled = selecting) { vm.clearSelection() }
    androidx.activity.compose.BackHandler(enabled = !selecting && drawer.isOpen) { scope.launch { drawer.close() } }
    androidx.activity.compose.BackHandler(enabled = !selecting && !drawer.isOpen && (filter.isActive || section != Section.Notes)) {
        if (filter.isActive) vm.clearFilter() else vm.openSection(Section.Notes)
        focusManager.clearFocus()
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = !selecting,
        drawerContent = {
            AppDrawer(
                section = section,
                folders = folders.map { it.id to it.name },
                labels = labels.map { it.id to it.name },
                onSelect = { s ->
                    vm.clearFilter()
                    vm.openSection(s)
                    scope.launch { drawer.close() }
                },
                onNewFolder = { newFolder = true },
                onManageLabels = { scope.launch { drawer.close() }; onManageLabels() },
                onSettings = { scope.launch { drawer.close() }; onOpenSettings() }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (selecting) {
                    SelectionBar(
                        count = selection.size,
                        section = section,
                        allPinned = home.notes.filter { it.note.id in selection }.all { it.note.isPinned },
                        onClose = vm::clearSelection,
                        onPin = { pin -> vm.setPinned(pinned = pin) },
                        onColor = { colorDialog = true },
                        onArchive = { vm.setArchived(archived = section != Section.Archive) },
                        onTrash = { vm.moveToTrash() },
                        onRestore = { vm.restore() },
                        onDeleteForever = { confirmDeleteSelected = true },
                        onFolder = { folderDialog = true },
                        onLabel = { labelDialog = true },
                        onSelectAll = vm::selectAll
                    )
                } else {
                    SearchBarTop(
                        filter = filter,
                        viewMode = prefs.viewMode,
                        sortOrder = prefs.sortOrder,
                        onMenu = { scope.launch { drawer.open() } },
                        onQuery = { vm.setFilter(filter.copy(query = it)) },
                        onClear = { vm.clearFilter(); focusManager.clearFocus() },
                        onToggleView = { vm.setViewMode(if (prefs.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID) },
                        onSort = vm::setSortOrder
                    )
                }
            },
            floatingActionButton = {
                if (!selecting && section != Section.Trash && section != Section.Archive) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallFloatingActionButton(
                            onClick = { onNewNote(NoteType.CHECKLIST) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.testTag("fab_checklist")
                        ) { Icon(Icons.Outlined.CheckBox, stringResource(R.string.new_checklist)) }
                        ExtendedFloatingActionButton(
                            onClick = { onNewNote(NoteType.TEXT) },
                            icon = { Icon(Icons.Outlined.Add, null) },
                            text = { Text(stringResource(R.string.new_note)) },
                            modifier = Modifier.testTag("fab_note")
                        )
                    }
                }
            }
        ) { padding ->
            val headerTitle = sectionHeader(
                section,
                folders.firstOrNull { (section as? Section.Folder)?.id == it.id }?.name,
                labels.firstOrNull { (section as? Section.Label)?.id == it.id }?.name
            )
            LazyVerticalStaggeredGrid(
                columns = if (prefs.viewMode == ViewMode.GRID) StaggeredGridCells.Adaptive(160.dp) else StaggeredGridCells.Fixed(1),
                modifier = Modifier.fillMaxSize().padding(padding).testTag("notes_grid"),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                if (filter.isActive) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        FilterChips(filter = filter, darkTheme = darkTheme, onChange = vm::setFilter)
                    }
                }
                headerTitle?.let { title ->
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SectionHeader(
                            title = title,
                            section = section,
                            onRename = { renameFolder = true },
                            onDelete = { deleteFolder = true },
                            onEmptyTrash = { confirmEmptyTrash = true },
                            trashHasItems = home.notes.isNotEmpty(),
                            onManageLabels = onManageLabels
                        )
                    }
                }
                if (home.loaded && home.notes.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        if (filter.isActive) {
                            EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.empty_search_title), stringResource(R.string.empty_search_message))
                        } else {
                            val (icon, t, m) = when (section) {
                                Section.Notes -> Triple(Icons.Outlined.Lightbulb, R.string.empty_notes_title, R.string.empty_notes_message)
                                Section.Reminders -> Triple(Icons.Outlined.Alarm, R.string.empty_reminders_title, R.string.empty_reminders_message)
                                Section.Archive -> Triple(Icons.Outlined.Archive, R.string.empty_archive_title, R.string.empty_archive_message)
                                Section.Trash -> Triple(Icons.Outlined.Delete, R.string.empty_trash_title, R.string.empty_trash_message)
                                is Section.Folder -> Triple(Icons.Outlined.Folder, R.string.empty_folder_title, R.string.empty_folder_message)
                                is Section.Label -> Triple(Icons.AutoMirrored.Outlined.Label, R.string.empty_label_title, R.string.empty_label_message)
                            }
                            EmptyState(icon, stringResource(t), stringResource(m))
                        }
                    }
                }
                val showPinnedGroups = home.pinnedCount > 0 && section !is Section.Reminders && section != Section.Trash && section != Section.Archive
                val pinned = if (showPinnedGroups) home.notes.filter { it.note.isPinned } else emptyList()
                val others = if (showPinnedGroups) home.notes.filterNot { it.note.isPinned } else home.notes
                if (pinned.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { GroupLabel(stringResource(R.string.group_pinned)) }
                    items(pinned, key = { it.note.id }) { n ->
                        NoteCard(
                            item = n,
                            folderName = folders.firstOrNull { it.id == n.note.folderId && section !is Section.Folder }?.name,
                            selected = n.note.id in selection,
                            darkTheme = darkTheme,
                            compact = prefs.viewMode == ViewMode.GRID,
                            onClick = { if (selecting) vm.toggleSelect(n.note.id) else onOpenNote(n.note.id) },
                            onLongClick = { vm.toggleSelect(n.note.id) }
                        )
                    }
                    if (others.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) { GroupLabel(stringResource(R.string.group_others)) }
                    }
                }
                items(others, key = { it.note.id }) { n ->
                    NoteCard(
                        item = n,
                        folderName = folders.firstOrNull { it.id == n.note.folderId && section !is Section.Folder }?.name,
                        selected = n.note.id in selection,
                        darkTheme = darkTheme,
                        compact = prefs.viewMode == ViewMode.GRID,
                        onClick = { if (selecting) vm.toggleSelect(n.note.id) else onOpenNote(n.note.id) },
                        onLongClick = { vm.toggleSelect(n.note.id) }
                    )
                }
            }
        }
    }

    // ---- dialogs ----
    if (colorDialog) {
        ColorDialog(selected = null, darkTheme = darkTheme, onDismiss = { colorDialog = false }, onSelect = { vm.setColor(color = it) })
    }
    if (folderDialog) {
        FolderPickerDialog(
            folders = folders,
            current = -1L,
            onDismiss = { folderDialog = false },
            onPick = { vm.moveToFolder(folderId = it) },
            onCreate = { name ->
                val ids = selection
                vm.createFolder(name) { id -> vm.moveToFolder(ids, id) }
                vm.clearSelection()
            }
        )
    }
    if (labelDialog) {
        LabelPickerDialog(
            labels = labels,
            attached = emptySet(),
            onDismiss = { labelDialog = false; vm.clearSelection() },
            onToggle = { id, on -> if (on) vm.addLabel(selection, id) },
            onCreate = { name -> val ids = selection; vm.createLabel(name) { vm.addLabel(ids, it) } }
        )
    }
    if (confirmEmptyTrash) {
        ConfirmDialog(
            title = stringResource(R.string.empty_trash_confirm_title),
            message = stringResource(R.string.empty_trash_confirm_message),
            confirmLabel = stringResource(R.string.action_empty_trash),
            onDismiss = { confirmEmptyTrash = false },
            onConfirm = { vm.emptyTrash() }
        )
    }
    if (confirmDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.delete_forever_title),
            message = stringResource(R.string.delete_forever_message_many, selection.size),
            confirmLabel = stringResource(R.string.action_delete_forever),
            onDismiss = { confirmDeleteSelected = false },
            onConfirm = { vm.deleteForever() }
        )
    }
    if (newFolder) {
        NameDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { newFolder = false },
            onConfirm = { name ->
                newFolder = false
                vm.createFolder(name) { vm.openSection(Section.Folder(it)); scope.launch { drawer.close() } }
            }
        )
    }
    val currentFolder = folders.firstOrNull { (section as? Section.Folder)?.id == it.id }
    if (renameFolder && currentFolder != null) {
        NameDialog(
            title = stringResource(R.string.folder_rename),
            initial = currentFolder.name,
            onDismiss = { renameFolder = false },
            onConfirm = { vm.renameFolder(currentFolder, it); renameFolder = false }
        )
    }
    if (deleteFolder && currentFolder != null) {
        ConfirmDialog(
            title = stringResource(R.string.folder_delete_title),
            message = stringResource(R.string.folder_delete_message, currentFolder.name),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { deleteFolder = false },
            onConfirm = { vm.deleteFolder(currentFolder.id) }
        )
    }
}

@Composable
private fun sectionHeader(section: Section, folderName: String?, labelName: String?): String? = when (section) {
    Section.Notes -> null
    Section.Reminders -> stringResource(R.string.nav_reminders)
    Section.Archive -> stringResource(R.string.nav_archive)
    Section.Trash -> stringResource(R.string.nav_trash)
    is Section.Folder -> folderName
    is Section.Label -> labelName
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SectionHeader(
    title: String,
    section: Section,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onEmptyTrash: () -> Unit,
    trashHasItems: Boolean,
    onManageLabels: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (section) {
                is Section.Folder -> Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.folder_rename)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.folder_delete_title)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; onDelete() })
                    }
                }
                is Section.Label -> IconButton(onClick = onManageLabels) { Icon(Icons.Outlined.Edit, stringResource(R.string.labels_manage)) }
                Section.Trash -> if (trashHasItems) TextButton(onClick = onEmptyTrash, modifier = Modifier.testTag("empty_trash")) { Text(stringResource(R.string.action_empty_trash)) }
                else -> {}
            }
        }
        if (section == Section.Trash) {
            Text(
                stringResource(R.string.trash_info, NotesRepository.TRASH_RETENTION_DAYS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterChips(filter: SearchFilter, darkTheme: Boolean, onChange: (SearchFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = filter.onlyChecklists, onClick = { onChange(filter.copy(onlyChecklists = !filter.onlyChecklists)) },
                    label = { Text(stringResource(R.string.filter_checklists)) }, leadingIcon = { Icon(Icons.Outlined.CheckBox, null, Modifier.size(18.dp)) })
            }
            item {
                FilterChip(selected = filter.onlyWithImages, onClick = { onChange(filter.copy(onlyWithImages = !filter.onlyWithImages)) },
                    label = { Text(stringResource(R.string.filter_images)) }, leadingIcon = { Icon(Icons.Outlined.Image, null, Modifier.size(18.dp)) })
            }
            item {
                FilterChip(selected = filter.onlyWithReminder, onClick = { onChange(filter.copy(onlyWithReminder = !filter.onlyWithReminder)) },
                    label = { Text(stringResource(R.string.filter_reminders)) }, leadingIcon = { Icon(Icons.Outlined.Alarm, null, Modifier.size(18.dp)) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(NoteColors.count - 1) { i0 ->
                val i = i0 + 1
                val sel = filter.color == i
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(NoteColors.of(i, darkTheme))
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onChange(filter.copy(color = if (sel) null else i)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarTop(
    filter: SearchFilter,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onMenu: () -> Unit,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
    onToggleView: () -> Unit,
    onSort: (SortOrder) -> Unit
) {
    var sortMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu, modifier = Modifier.testTag("open_drawer")) { Icon(Icons.Outlined.Menu, stringResource(R.string.action_menu)) }
            val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = filter.query,
                onValueChange = { onQuery(it.replace("\n", "")) },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).testTag("search_input"),
                decorationBox = { inner ->
                    Box {
                        if (filter.query.isEmpty()) Text(stringResource(R.string.search_hint), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                }
            )
            if (filter.isActive) {
                IconButton(onClick = onClear, modifier = Modifier.testTag("search_clear")) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear)) }
            } else {
                Icon(Icons.Outlined.Search, null, Modifier.padding(horizontal = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleView, modifier = Modifier.testTag("toggle_view")) {
                Icon(
                    if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    stringResource(if (viewMode == ViewMode.GRID) R.string.view_list else R.string.view_grid)
                )
            }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.sort_title)) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf(SortOrder.UPDATED to R.string.sort_updated, SortOrder.CREATED to R.string.sort_created, SortOrder.TITLE to R.string.sort_title_az)
                        .forEach { (o, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                trailingIcon = { if (o == sortOrder) Icon(Icons.Outlined.CheckBox, null) },
                                onClick = { onSort(o); sortMenu = false }
                            )
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
    count: Int,
    section: Section,
    allPinned: Boolean,
    onClose: () -> Unit,
    onPin: (Boolean) -> Unit,
    onColor: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onFolder: () -> Unit,
    onLabel: () -> Unit,
    onSelectAll: () -> Unit
) {
    var more by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        navigationIcon = { IconButton(onClick = onClose, modifier = Modifier.testTag("selection_close")) { Icon(Icons.Outlined.Close, stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.selected_count, count)) },
        actions = {
            if (section == Section.Trash) {
                IconButton(onClick = onRestore, modifier = Modifier.testTag("sel_restore")) { Icon(Icons.Outlined.RestoreFromTrash, stringResource(R.string.action_restore)) }
                IconButton(onClick = onDeleteForever, modifier = Modifier.testTag("sel_delete_forever")) { Icon(Icons.Outlined.DeleteForever, stringResource(R.string.action_delete_forever)) }
                IconButton(onClick = onSelectAll) { Icon(Icons.Outlined.SelectAll, stringResource(R.string.action_select_all)) }
            } else {
                IconButton(onClick = { onPin(!allPinned) }, modifier = Modifier.testTag("sel_pin")) {
                    Icon(if (allPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin, stringResource(if (allPinned) R.string.action_unpin else R.string.action_pin))
                }
                IconButton(onClick = onColor, modifier = Modifier.testTag("sel_color")) { Icon(Icons.Outlined.Palette, stringResource(R.string.action_color)) }
                IconButton(onClick = onArchive, modifier = Modifier.testTag("sel_archive")) {
                    Icon(
                        if (section == Section.Archive) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                        stringResource(if (section == Section.Archive) R.string.action_unarchive else R.string.action_archive)
                    )
                }
                Box {
                    IconButton(onClick = { more = true }, modifier = Modifier.testTag("sel_more")) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_move_to_folder)) }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { more = false; onFolder() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_add_label)) }, leadingIcon = { Icon(Icons.Outlined.NewLabel, null) }, onClick = { more = false; onLabel() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_select_all)) }, leadingIcon = { Icon(Icons.Outlined.SelectAll, null) }, onClick = { more = false; onSelectAll() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { more = false; onTrash() }, modifier = Modifier.testTag("sel_delete"))
                    }
                }
            }
        }
    )
}

@Composable
private fun AppDrawer(
    section: Section,
    folders: List<Pair<Long, String>>,
    labels: List<Pair<Long, String>>,
    onSelect: (Section) -> Unit,
    onNewFolder: () -> Unit,
    onManageLabels: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
            )
            @Composable
            fun entry(s: Section, icon: ImageVector, label: String, tag: String) = NavigationDrawerItem(
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(icon, null) },
                selected = section == s,
                onClick = { onSelect(s) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag(tag)
            )
            entry(Section.Notes, Icons.Outlined.Lightbulb, stringResource(R.string.nav_notes), "nav_notes")
            entry(Section.Reminders, Icons.Outlined.Alarm, stringResource(R.string.nav_reminders), "nav_reminders")
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            DrawerGroupTitle(stringResource(R.string.nav_folders))
            folders.forEach { (id, name) -> entry(Section.Folder(id), Icons.Outlined.Folder, name, "nav_folder_$id") }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.folder_new)) },
                icon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                selected = false,
                onClick = onNewFolder,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("nav_new_folder")
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            DrawerGroupTitle(stringResource(R.string.nav_labels))
            labels.forEach { (id, name) -> entry(Section.Label(id), Icons.AutoMirrored.Outlined.Label, name, "nav_label_$id") }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.labels_manage)) },
                icon = { Icon(Icons.Outlined.Edit, null) },
                selected = false,
                onClick = onManageLabels,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("nav_manage_labels")
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            entry(Section.Archive, Icons.Outlined.Archive, stringResource(R.string.nav_archive), "nav_archive")
            entry(Section.Trash, Icons.Outlined.Delete, stringResource(R.string.nav_trash), "nav_trash")
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_settings)) },
                icon = { Icon(Icons.Outlined.Settings, null) },
                selected = false,
                onClick = onSettings,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("nav_settings")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerGroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}
