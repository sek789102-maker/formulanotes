package com.sek789102.jotwai.ui.screens

import com.sek789102.jotwai.util.t
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.FolderEntity
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteWithLabels
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.domain.Section
import com.sek789102.jotwai.ui.components.ConfirmDialog
import com.sek789102.jotwai.ui.components.NameDialog
import com.sek789102.jotwai.ui.components.NoteImage
import com.sek789102.jotwai.ui.components.ReminderReliabilityCard
import com.sek789102.jotwai.ui.design.ArtImage
import com.sek789102.jotwai.ui.design.GlassCard
import com.sek789102.jotwai.ui.design.Gold
import com.sek789102.jotwai.ui.design.GoldBackdrop
import com.sek789102.jotwai.ui.design.GoldBottomBar
import com.sek789102.jotwai.ui.design.GoldChip
import com.sek789102.jotwai.ui.design.GoldDeep
import com.sek789102.jotwai.ui.design.GoldFab
import com.sek789102.jotwai.ui.design.GoldSearchBox
import com.sek789102.jotwai.ui.design.SectionTitle
import com.sek789102.jotwai.ui.design.TabHeader
import com.sek789102.jotwai.ui.design.TabItem
import com.sek789102.jotwai.ui.design.isDarkUi
import com.sek789102.jotwai.ui.theme.NoteColors
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import com.sek789102.jotwai.util.Dates
import java.util.Calendar

/** The four main tabs: home, reminders, folders, settings. */
@Composable
fun MainTabs(
    vm: AppViewModel,
    prefs: UserPreferences,
    darkTheme: Boolean,
    snackbar: SnackbarHostState,
    onOpenNote: (Long) -> Unit,
    onNewNote: (NoteType, Section) -> Unit,
    onOpenList: (Section, Boolean) -> Unit,
    onManageLabels: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    androidx.activity.compose.BackHandler(enabled = tab != 0) { tab = 0 }
    val tabs = listOf(
        TabItem(Icons.Outlined.Home, Icons.Filled.Home, t("หน้าหลัก", "Home"), "tab_home"),
        TabItem(Icons.Outlined.Notifications, Icons.Filled.Notifications, t("การแจ้งเตือน", "Reminders"), "tab_reminders"),
        TabItem(Icons.Outlined.Folder, Icons.Filled.Folder, t("โฟลเดอร์", "Folders"), "tab_folders"),
        TabItem(Icons.Outlined.Settings, Icons.Filled.Settings, t("ตั้งค่า", "Settings"), "tab_settings")
    )
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { GoldBottomBar(tabs, tab) { tab = it } }
    ) { pad ->
        GoldBackdrop {
            Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
                when (tab) {
                    0 -> DashboardTab(vm, darkTheme, onOpenNote, onNewNote, onOpenList, onSettings = { tab = 3 }, onFolders = { tab = 2 })
                    1 -> RemindersTab(vm, onOpenNote)
                    2 -> FoldersTab(vm, onNewNote, onOpenList, onManageLabels)
                    else -> SettingsTab(vm, prefs, onManageLabels)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ home

@Composable
private fun DashboardTab(
    vm: AppViewModel,
    darkTheme: Boolean,
    onOpenNote: (Long) -> Unit,
    onNewNote: (NoteType, Section) -> Unit,
    onOpenList: (Section, Boolean) -> Unit,
    onSettings: () -> Unit,
    onFolders: () -> Unit
) {
    val all by vm.allNotes.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val notes = all.orEmpty().filter { !it.note.isTrashed && !it.note.isArchived }
    val favorites = all.orEmpty().count { !it.note.isTrashed && it.note.isPinned }
    val recent = notes.sortedByDescending { it.note.updatedAt }
    val latest = recent.firstOrNull()?.note?.updatedAt

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().testTag("dashboard"),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                TabHeader(
                    title = t("จดไว้", "Jotwai"),
                    subtitle = t("บันทึกทุกไอเดียของคุณ ✨", "Save every idea you have ✨"),
                    leading = { ArtImage(R.drawable.art_app_tile, 76.dp) },
                    actions = {
                        IconButton(onClick = onSettings, modifier = Modifier.size(56.dp).testTag("home_settings")) {
                            Icon(Icons.Filled.Settings, t("ตั้งค่า", "Settings"), Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                )
            }
            item {
                GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clickable { onOpenList(Section.Notes, true) }.testTag("home_search")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(t("ค้นหาโน้ต...", "Search notes..."), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatCard(Icons.AutoMirrored.Outlined.StickyNote2, t("โน้ตทั้งหมด", "All notes"), t("${notes.size} รายการ", "${notes.size} notes"), Modifier.weight(1f).testTag("stat_all")) { onOpenList(Section.Notes, false) }
                        StatCard(Icons.Filled.Star, t("รายการโปรด", "Favourites"), t("$favorites รายการ", "$favorites notes"), Modifier.weight(1f).testTag("stat_fav")) { onOpenList(Section.Favorites, false) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatCard(Icons.Filled.Folder, t("หมวดหมู่", "Folders"), t("${folders.size} โฟลเดอร์", "${folders.size} folders"), Modifier.weight(1f).testTag("stat_folders"), onClick = onFolders)
                        StatCard(Icons.Outlined.AccessTime, t("ล่าสุด", "Latest"), latest?.let { relativeDay(it) } ?: "-", Modifier.weight(1f).testTag("stat_recent")) {
                            recent.firstOrNull()?.let { onOpenNote(it.note.id) } ?: onOpenList(Section.Notes, false)
                        }
                    }
                }
            }
            item {
                SectionTitle(t("โน้ตล่าสุด", "Recent notes")) {
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onOpenList(Section.Notes, false) }.padding(6.dp).testTag("see_all"), verticalAlignment = Alignment.CenterVertically) {
                        Text(t("ดูทั้งหมด", "See all"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (all != null && recent.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            ArtImage(R.drawable.art_note_thumb, 96.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(t("ยังไม่มีโน้ต", "No notes yet"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(t("กดปุ่ม + เพื่อเริ่มจดโน้ตแรก", "Tap + to write your first note"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(recent.take(8), key = { it.note.id }) { n ->
                RecentNoteRow(n, darkTheme, vm, Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { onOpenNote(n.note.id) }
            }
        }
        NewNoteFab(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp)) { onNewNote(it, Section.Notes) }
    }
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.height(108.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentNoteRow(n: NoteWithLabels, darkTheme: Boolean, vm: AppViewModel, modifier: Modifier, onClick: () -> Unit) {
    val note = n.note
    var menu by remember { mutableStateOf(false) }
    GlassCard(modifier.fillMaxWidth().testTag("recent_${note.id}"), accentLeft = false, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 112.dp, height = 84.dp).clip(RoundedCornerShape(16.dp))) {
                val img = note.imagePaths.firstOrNull()
                if (img != null && !note.isLocked) NoteImage(img, 360, Modifier.fillMaxSize())
                else ArtImage(R.drawable.art_note_thumb, 112.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { preview(n).lineSequence().firstOrNull().orEmpty().ifBlank { t("โน้ตไม่มีชื่อ", "Untitled note") } },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (note.isLocked) t("🔒 โน้ตที่ล็อกไว้", "🔒 Locked note") else preview(n).replace('\n', ' ').ifBlank { " " },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(dateLine(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    note.isPinned -> Icon(Icons.Filled.Star, t("รายการโปรด", "Favourites"), tint = Gold, modifier = Modifier.size(24.dp))
                    note.color != 0 -> Box(Modifier.padding(4.dp).size(18.dp).clip(CircleShape).background(NoteColors.of(note.color, false)).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("recent_menu_${note.id}")) { Icon(Icons.Outlined.MoreVert, t("เพิ่มเติม", "More")) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) t("เอาออกจากรายการโปรด", "Remove from favourites") else t("เพิ่มในรายการโปรด", "Add to favourites")) },
                            leadingIcon = { Icon(if (note.isPinned) Icons.Outlined.StarBorder else Icons.Outlined.Star, null) },
                            onClick = { menu = false; vm.setPinned(setOf(note.id), !note.isPinned) }
                        )
                        DropdownMenuItem(text = { Text(t("ทำสำเนา", "Make a copy")) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; vm.duplicate(note.id) })
                        DropdownMenuItem(text = { Text(t("ย้ายไปถังขยะ", "Move to trash")) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; vm.moveToTrash(setOf(note.id)) })
                    }
                }
            }
        }
    }
}

/** Plain text of a note for one-line previews. */
private fun preview(n: NoteWithLabels): String {
    val note = n.note
    return if (note.type == NoteType.CHECKLIST) note.checklist.filter { it.text.isNotBlank() }.joinToString(" · ") { it.text }
    else RichText.stripTags(note.content.take(600)).replace(Regex("""[*_~`#>]+"""), "").trim()
}

/** "Today", "Yesterday", "3 days ago", or a date. */
private fun relativeDay(millis: Long): String = Dates.relativeDay(millis)

/** "เมื่อวานนี้ • 21:42" / "22 พ.ย. 2568 • 19:20". */
private fun dateLine(millis: Long): String = Dates.dayLine(millis)

@Composable
private fun NewNoteFab(modifier: Modifier, onNew: (NoteType) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        GoldFab(onClick = { menu = true }, modifier = Modifier.testTag("fab_new"), description = t("สร้างโน้ต", "New note"))
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(t("โน้ตใหม่", "New note")) }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { menu = false; onNew(NoteType.TEXT) }, modifier = Modifier.testTag("fab_note"))
            DropdownMenuItem(text = { Text(t("เช็กลิสต์ใหม่", "New checklist")) }, leadingIcon = { Icon(Icons.Outlined.CheckBox, null) }, onClick = { menu = false; onNew(NoteType.CHECKLIST) }, modifier = Modifier.testTag("fab_checklist"))
        }
    }
}

// ------------------------------------------------------------------ reminders

private enum class RemFilter(private val th: String, private val en: String) {
    ALL("ทั้งหมด", "All"), NOTE("โน้ต", "Notes"), FOLDER("โฟลเดอร์", "Folders"), SYSTEM("ระบบ", "System");
    val label: String get() = t(th, en)
}

@Composable
private fun RemindersTab(vm: AppViewModel, onOpenNote: (Long) -> Unit) {
    val all by vm.allNotes.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(RemFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val focus = remember { FocusRequester() }

    val withReminder = all.orEmpty().filter { !it.note.isTrashed && it.note.reminderAt != null }
        .filter { query.isBlank() || it.note.title.contains(query, true) || (!it.note.isLocked && preview(it).contains(query, true)) }
    val shown = when (filter) {
        RemFilter.ALL -> withReminder
        RemFilter.NOTE -> withReminder.filter { it.note.folderId == null }
        RemFilter.FOLDER -> withReminder.filter { it.note.folderId != null }
        RemFilter.SYSTEM -> emptyList()
    }
    val now = remember(tick, all) { System.currentTimeMillis() }
    val upcoming = shown.filter { (it.note.reminderAt ?: 0L) >= now }.sortedBy { it.note.reminderAt }
    val past = shown.filter { (it.note.reminderAt ?: 0L) < now }.sortedByDescending { it.note.reminderAt }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabHeader(
            title = t("การแจ้งเตือน", "Reminders"),
            leading = { com.sek789102.jotwai.ui.design.GoldTile(Icons.Filled.Notifications, 64.dp, glass = true) },
            actions = {
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }, modifier = Modifier.size(52.dp).testTag("rem_search")) {
                    Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, t("ค้นหา", "Search"), Modifier.size(34.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(52.dp).testTag("rem_more")) { Icon(Icons.Outlined.MoreVert, t("เพิ่มเติม", "More"), Modifier.size(34.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(t("ล้างการแจ้งเตือนที่ผ่านไปแล้ว", "Clear past reminders")) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; vm.clearPastReminders() })
                        DropdownMenuItem(text = { Text(t("ตั้งค่าการแจ้งเตือน", "Reminder settings")) }, leadingIcon = { Icon(Icons.Outlined.Settings, null) }, onClick = { menu = false; filter = RemFilter.SYSTEM })
                    }
                }
            }
        )
        if (searching) {
            GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                SearchField(query, t("ค้นหาการแจ้งเตือน...", "Search reminders..."), focus) { query = it }
            }
            androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(RemFilter.entries.toList()) { f ->
                val icon = when (f) {
                    RemFilter.ALL -> Icons.Filled.Notifications
                    RemFilter.NOTE -> Icons.Outlined.Description
                    RemFilter.FOLDER -> Icons.Outlined.Folder
                    RemFilter.SYSTEM -> Icons.Outlined.Settings
                }
                GoldChip(f.label, icon, filter == f, { filter = f }, Modifier.testTag("rem_filter_${f.name}"))
            }
        }
        if (filter == RemFilter.SYSTEM) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(t("ความพร้อมของการแจ้งเตือน", "Reminder readiness"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        ReminderReliabilityCard()
                    }
                }
            }
        } else if (upcoming.isEmpty() && past.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ArtImage(R.drawable.art_bell, 240.dp)
                Spacer(Modifier.height(18.dp))
                Text(if (query.isBlank()) t("ยังไม่มีการแจ้งเตือน", "No reminders yet") else t("ไม่พบการแจ้งเตือน", "No reminders found"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    t("คุณจะเห็นการแจ้งเตือนที่นี่\nเมื่อตั้งเตือนในโน้ต (เปิดโน้ต แล้วกดรูปกระดิ่ง)", "Reminders you set in notes show here\n(open a note and tap the bell)"),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(30.dp)).background(com.sek789102.jotwai.ui.design.GoldBrush)
                        .clickable { tick++; query = ""; android.widget.Toast.makeText(context, t("อัปเดตแล้ว", "Updated"), android.widget.Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 40.dp, vertical = 16.dp).testTag("rem_retry"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = Color(0xFF1A1206))
                    Spacer(Modifier.width(10.dp))
                    Text(t("ลองใหม่", "Refresh"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1206))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (upcoming.isNotEmpty()) item { Text(t("กำลังจะมาถึง", "Upcoming"), style = MaterialTheme.typography.titleMedium, color = com.sek789102.jotwai.ui.design.goldText(), modifier = Modifier.padding(top = 4.dp)) }
                items(upcoming, key = { "u${it.note.id}" }) { ReminderRow(it, folders, false) { onOpenNote(it.note.id) } }
                if (past.isNotEmpty()) item { Text(t("ผ่านไปแล้ว", "Past"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
                items(past, key = { "p${it.note.id}" }) { ReminderRow(it, folders, true) { onOpenNote(it.note.id) } }
            }
        }
    }
}

@Composable
private fun ReminderRow(n: NoteWithLabels, folders: List<FolderEntity>, past: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val note = n.note
    GlassCard(Modifier.fillMaxWidth().testTag("rem_${note.id}"), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            com.sek789102.jotwai.ui.design.GoldTile(Icons.Outlined.Alarm, 48.dp, glass = past)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(note.title.ifBlank { if (note.isLocked) t("โน้ตที่ล็อกไว้", "Locked note") else preview(n).take(60).ifBlank { t("โน้ตไม่มีชื่อ", "Untitled note") } }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val repeat = if (note.reminderRepeat != com.sek789102.jotwai.data.model.RepeatRule.NONE) " · " + Dates.repeatLabel(context, note.reminderRepeat) else ""
                Text(Dates.dayAndTime(context, note.reminderAt ?: 0L) + repeat, style = MaterialTheme.typography.bodyMedium, color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else com.sek789102.jotwai.ui.design.goldText())
                folders.firstOrNull { it.id == note.folderId }?.let {
                    Text("📁 ${it.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable
private fun SearchField(value: String, hint: String, focus: FocusRequester, onChange: (String) -> Unit) {
    val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Gold),
            modifier = Modifier.weight(1f).focusRequester(focus).testTag("tab_search_input"),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(hint, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, t("ล้าง", "Clear")) }
    }
}

// ------------------------------------------------------------------ folders

private class Cell(val key: String, val content: @Composable (Modifier) -> Unit)

private enum class FolderSort(private val th: String, private val en: String) {
    ORDER("ตามลำดับที่สร้าง", "Order created"), NAME("ชื่อ ก-ฮ", "Name A–Z"), COUNT("จำนวนโน้ต", "Number of notes");
    val label: String get() = t(th, en)
}

val folderArt = listOf(R.drawable.art_folder_yellow, R.drawable.art_folder_blue, R.drawable.art_folder_purple, R.drawable.art_folder_green)
private val folderColorNames: List<String> get() = listOf(t("เหลือง", "Yellow"), t("ฟ้า", "Blue"), t("ม่วง", "Purple"), t("เขียว", "Green"))

@Composable
private fun FoldersTab(
    vm: AppViewModel,
    onNewNote: (NoteType, Section) -> Unit,
    onOpenList: (Section, Boolean) -> Unit,
    onManageLabels: () -> Unit
) {
    val all by vm.allNotes.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(FolderSort.ORDER) }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FolderEntity?>(null) }
    var colorTarget by remember { mutableStateOf<FolderEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<FolderEntity?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    val notes = all.orEmpty()
    val live = notes.filter { !it.note.isTrashed }
    fun countIn(id: Long) = live.count { it.note.folderId == id }
    val favCount = live.count { it.note.isPinned }
    val trashCount = notes.count { it.note.isTrashed }
    val list = folders.filter { query.isBlank() || it.name.contains(query, true) }.let { l ->
        when (sort) {
            FolderSort.ORDER -> l
            FolderSort.NAME -> l.sortedBy { it.name.lowercase() }
            FolderSort.COUNT -> l.sortedByDescending { countIn(it.id) }
        }
    }
    val showFav = query.isBlank() || t("รายการโปรด", "Favourites").contains(query, true)
    val showTrash = query.isBlank() || t("ถังขยะ", "Trash").contains(query, true)

    val cells = buildList {
        if (showFav) add(Cell("fav") { m ->
            FolderCard(R.drawable.art_folder_fav, t("รายการโปรด", "Favourites"), t("$favCount โน้ต", "$favCount notes"), m.testTag("folder_fav"), onClick = { onOpenList(Section.Favorites, false) }) { close ->
                DropdownMenuItem(text = { Text(t("เปิดดู", "Open")) }, onClick = { close(); onOpenList(Section.Favorites, false) })
            }
        })
        list.forEach { f ->
            add(Cell("f${f.id}") { m ->
                FolderCard(folderArt[f.color.coerceIn(0, 3)], f.name, t("${countIn(f.id)} โน้ต", "${countIn(f.id)} notes"), m.testTag("folder_${f.id}"), onClick = { onOpenList(Section.Folder(f.id), false) }) { close ->
                    DropdownMenuItem(text = { Text(t("เปลี่ยนชื่อ", "Rename")) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { close(); renameTarget = f })
                    DropdownMenuItem(text = { Text(t("เปลี่ยนสี", "Change colour")) }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = { close(); colorTarget = f })
                    DropdownMenuItem(text = { Text(t("ลบโฟลเดอร์", "Delete folder")) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { close(); deleteTarget = f })
                }
            })
        }
        if (showTrash) add(Cell("trash") { m ->
            FolderCard(R.drawable.art_folder_trash, t("ถังขยะ", "Trash"), t("$trashCount โน้ต", "$trashCount notes"), m.testTag("folder_trash"), onClick = { onOpenList(Section.Trash, false) }) { close ->
                DropdownMenuItem(text = { Text(t("เปิดดู", "Open")) }, onClick = { close(); onOpenList(Section.Trash, false) })
                if (trashCount > 0) DropdownMenuItem(text = { Text(t("ล้างถังขยะ", "Empty trash")) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { close(); confirmEmptyTrash = true })
            }
        })
        add(Cell("new") { m -> NewFolderCard(m.testTag("folder_new")) { createDialog = true } })
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                TabHeader(
                    title = t("โฟลเดอร์", "Folders"),
                    leading = { ArtImage(R.drawable.art_folder_yellow, 76.dp) },
                    actions = {
                        IconButton(onClick = { runCatching { focus.requestFocus() } }, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.Search, t("ค้นหา", "Search"), Modifier.size(34.dp)) }
                        Box {
                            IconButton(onClick = { moreMenu = true }, modifier = Modifier.size(52.dp).testTag("folders_more")) { Icon(Icons.Outlined.MoreVert, t("เพิ่มเติม", "More"), Modifier.size(34.dp)) }
                            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                                DropdownMenuItem(text = { Text(t("สร้างโฟลเดอร์ใหม่", "Create new folder")) }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { moreMenu = false; createDialog = true })
                                DropdownMenuItem(text = { Text(t("คลังเก็บ", "Archive")) }, leadingIcon = { Icon(Icons.Outlined.Archive, null) }, onClick = { moreMenu = false; onOpenList(Section.Archive, false) })
                                DropdownMenuItem(text = { Text(t("จัดการป้ายกำกับ", "Manage labels")) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) }, onClick = { moreMenu = false; onManageLabels() })
                            }
                        }
                    }
                )
            }
            item {
                GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { SearchField(query, t("ค้นหาโฟลเดอร์...", "Search folders..."), focus) { query = it } }
            }
            item {
                SectionTitle(t("โฟลเดอร์ของฉัน", "My folders")) {
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { sortMenu = true }.padding(horizontal = 14.dp, vertical = 10.dp).testTag("folders_sort"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t("เรียงลำดับ", "Sort"), style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            FolderSort.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                                    trailingIcon = { if (s == sort) Icon(Icons.Outlined.CheckBox, null, tint = Gold) },
                                    onClick = { sort = s; sortMenu = false }
                                )
                            }
                        }
                    }
                }
            }
            cells.chunked(2).forEach { row ->
                item(key = row.joinToString { it.key }) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { it.content(Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item { SectionTitle(t("โน้ตทั้งหมด", "All notes"), Icons.Outlined.BarChart, Modifier.padding(top = 10.dp)) }
            item {
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("folders_all_notes"), onClick = { onOpenList(Section.Notes, false) }) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = Gold, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t("ดูโน้ตทั้งหมด", "See all notes"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(t("มีทั้งหมด ${live.count { !it.note.isArchived }} โน้ต", "${live.count { !it.note.isArchived }} notes in total"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null)
                    }
                }
            }
        }
        NewNoteFab(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp)) { onNewNote(it, Section.Notes) }
    }

    if (createDialog) {
        FolderDialog(title = t("สร้างโฟลเดอร์ใหม่", "Create new folder"), initialName = "", initialColor = 0, onDismiss = { createDialog = false }) { name, color ->
            createDialog = false
            vm.createFolder(name, color)
        }
    }
    renameTarget?.let { f ->
        NameDialog(title = t("เปลี่ยนชื่อโฟลเดอร์", "Rename folder"), initial = f.name, onDismiss = { renameTarget = null }, onConfirm = { vm.renameFolder(f, it); renameTarget = null })
    }
    colorTarget?.let { f ->
        FolderDialog(title = t("สีของโฟลเดอร์", "Folder colour"), initialName = f.name, initialColor = f.color, nameEditable = false, onDismiss = { colorTarget = null }) { _, c ->
            vm.setFolderColor(f, c); colorTarget = null
        }
    }
    deleteTarget?.let { f ->
        ConfirmDialog(
            title = t("ลบโฟลเดอร์", "Delete folder"),
            message = t("ลบโฟลเดอร์ \"${f.name}\" ? โน้ตข้างในจะไม่ถูกลบ แต่จะย้ายออกจากโฟลเดอร์", "Delete folder \"${f.name}\"? Its notes are kept and moved out of the folder."),
            confirmLabel = t("ลบ", "Delete"),
            onDismiss = { deleteTarget = null },
            onConfirm = { vm.deleteFolder(f.id) }
        )
    }
    if (confirmEmptyTrash) {
        ConfirmDialog(title = t("ล้างถังขยะ", "Empty trash"), message = t("ลบโน้ตในถังขยะทั้งหมดถาวร กู้คืนไม่ได้", "Delete every note in the trash for good. This cannot be undone."), confirmLabel = t("ล้างถังขยะ", "Empty trash"), onDismiss = { confirmEmptyTrash = false }, onConfirm = { vm.emptyTrash() })
    }
}

@Composable
private fun FolderCard(art: Int, name: String, count: String, modifier: Modifier, onClick: () -> Unit, menuItems: @Composable (close: () -> Unit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    GlassCard(modifier.height(118.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(Gold))
            Row(Modifier.weight(1f).fillMaxSize().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtImage(art, 54.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(count, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.width(30.dp)) { Icon(Icons.Outlined.MoreVert, t("เพิ่มเติม", "More")) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) { menuItems { menu = false } }
                }
            }
        }
    }
}

@Composable
private fun NewFolderCard(modifier: Modifier, onClick: () -> Unit) {
    val dark = isDarkUi()
    Box(
        modifier.height(118.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Gold.copy(alpha = 0.8f),
                cornerRadius = CornerRadius(24.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", fontSize = 34.sp, color = Gold, fontWeight = FontWeight.Bold)
            Text(t("สร้างโฟลเดอร์ใหม่", "Create new folder"), style = MaterialTheme.typography.titleSmall, color = if (dark) Gold else GoldDeep, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FolderDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    nameEditable: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (nameEditable) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.replace("\n", "").take(60) },
                        singleLine = true,
                        label = { Text(t("ชื่อโฟลเดอร์", "Folder name")) },
                        modifier = Modifier.fillMaxWidth().testTag("name_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    folderArt.forEachIndexed { i, res ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .border(if (color == i) BorderStroke(2.dp, Gold) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp))
                                .clickable { color = i }.padding(6.dp).testTag("folder_color_$i"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ArtImage(res, 44.dp)
                            Text(folderColorNames[i], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), color) }, enabled = !nameEditable || name.isNotBlank()) { Text(t("บันทึก", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("ยกเลิก", "Cancel")) } }
    )
}
