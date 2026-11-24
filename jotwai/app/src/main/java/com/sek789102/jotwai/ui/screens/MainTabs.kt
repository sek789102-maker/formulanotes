package com.sek789102.jotwai.ui.screens

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
        TabItem(Icons.Outlined.Home, Icons.Filled.Home, "หน้าหลัก", "tab_home"),
        TabItem(Icons.Outlined.Notifications, Icons.Filled.Notifications, "การแจ้งเตือน", "tab_reminders"),
        TabItem(Icons.Outlined.Folder, Icons.Filled.Folder, "โฟลเดอร์", "tab_folders"),
        TabItem(Icons.Outlined.Settings, Icons.Filled.Settings, "ตั้งค่า", "tab_settings")
    )
    Scaffold(
        containerColor = Color.Transparent,
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
                    title = "จดไว้",
                    subtitle = "บันทึกทุกไอเดียของคุณ ✨",
                    leading = { ArtImage(R.drawable.art_app_tile, 76.dp) },
                    actions = {
                        IconButton(onClick = onSettings, modifier = Modifier.size(56.dp).testTag("home_settings")) {
                            Icon(Icons.Filled.Settings, "ตั้งค่า", Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                )
            }
            item {
                GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clickable { onOpenList(Section.Notes, true) }.testTag("home_search")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("ค้นหาโน้ต...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatCard(Icons.AutoMirrored.Outlined.StickyNote2, "โน้ตทั้งหมด", "${notes.size} รายการ", Modifier.weight(1f).testTag("stat_all")) { onOpenList(Section.Notes, false) }
                        StatCard(Icons.Filled.Star, "รายการโปรด", "$favorites รายการ", Modifier.weight(1f).testTag("stat_fav")) { onOpenList(Section.Favorites, false) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatCard(Icons.Filled.Folder, "หมวดหมู่", "${folders.size} โฟลเดอร์", Modifier.weight(1f).testTag("stat_folders"), onClick = onFolders)
                        StatCard(Icons.Outlined.AccessTime, "ล่าสุด", latest?.let { relativeDay(it) } ?: "-", Modifier.weight(1f).testTag("stat_recent")) {
                            recent.firstOrNull()?.let { onOpenNote(it.note.id) } ?: onOpenList(Section.Notes, false)
                        }
                    }
                }
            }
            item {
                SectionTitle("โน้ตล่าสุด") {
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onOpenList(Section.Notes, false) }.padding(6.dp).testTag("see_all"), verticalAlignment = Alignment.CenterVertically) {
                        Text("ดูทั้งหมด", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                            Text("ยังไม่มีโน้ต", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("กดปุ่ม + เพื่อเริ่มจดโน้ตแรก", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
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
                    note.title.ifBlank { preview(n).lineSequence().firstOrNull().orEmpty().ifBlank { "โน้ตไม่มีชื่อ" } },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (note.isLocked) "🔒 โน้ตที่ล็อกไว้" else preview(n).replace('\n', ' ').ifBlank { " " },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(dateLine(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    note.isPinned -> Icon(Icons.Filled.Star, "รายการโปรด", tint = Gold, modifier = Modifier.size(24.dp))
                    note.color != 0 -> Box(Modifier.padding(4.dp).size(18.dp).clip(CircleShape).background(NoteColors.of(note.color, false)).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("recent_menu_${note.id}")) { Icon(Icons.Outlined.MoreVert, "เพิ่มเติม") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) "เอาออกจากรายการโปรด" else "เพิ่มในรายการโปรด") },
                            leadingIcon = { Icon(if (note.isPinned) Icons.Outlined.StarBorder else Icons.Outlined.Star, null) },
                            onClick = { menu = false; vm.setPinned(setOf(note.id), !note.isPinned) }
                        )
                        DropdownMenuItem(text = { Text("ทำสำเนา") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; vm.duplicate(note.id) })
                        DropdownMenuItem(text = { Text("ย้ายไปถังขยะ") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; vm.moveToTrash(setOf(note.id)) })
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

private val thaiMonths = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")

private fun daysAgo(millis: Long, now: Long = System.currentTimeMillis()): Int {
    fun noon(t: Long) = Calendar.getInstance().apply { timeInMillis = t; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }.timeInMillis
    return Math.round((noon(now) - noon(millis)) / 86_400_000.0).toInt()
}

private fun thaiDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(Calendar.DAY_OF_MONTH)} ${thaiMonths[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR) + 543}"
}

/** "วันนี้", "เมื่อวานนี้", "3 วันที่แล้ว", or a date. */
private fun relativeDay(millis: Long): String = when (val d = daysAgo(millis)) {
    0 -> "วันนี้"
    1 -> "เมื่อวานนี้"
    in 2..6 -> "$d วันที่แล้ว"
    else -> thaiDate(millis)
}

/** "เมื่อวานนี้ • 21:42" / "22 พ.ย. 2568 • 19:20". */
private fun dateLine(millis: Long): String = "${relativeDay(millis).let { if (it.endsWith("วันที่แล้ว")) thaiDate(millis) else it }} • ${Dates.time(millis)}"

@Composable
private fun NewNoteFab(modifier: Modifier, onNew: (NoteType) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        GoldFab(onClick = { menu = true }, modifier = Modifier.testTag("fab_new"), description = "สร้างโน้ต")
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("โน้ตใหม่") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { menu = false; onNew(NoteType.TEXT) }, modifier = Modifier.testTag("fab_note"))
            DropdownMenuItem(text = { Text("เช็กลิสต์ใหม่") }, leadingIcon = { Icon(Icons.Outlined.CheckBox, null) }, onClick = { menu = false; onNew(NoteType.CHECKLIST) }, modifier = Modifier.testTag("fab_checklist"))
        }
    }
}

// ------------------------------------------------------------------ reminders

private enum class RemFilter(val label: String) { ALL("ทั้งหมด"), NOTE("โน้ต"), FOLDER("โฟลเดอร์"), SYSTEM("ระบบ") }

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
            title = "การแจ้งเตือน",
            leading = { com.sek789102.jotwai.ui.design.GoldTile(Icons.Filled.Notifications, 64.dp, glass = true) },
            actions = {
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }, modifier = Modifier.size(52.dp).testTag("rem_search")) {
                    Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, "ค้นหา", Modifier.size(34.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(52.dp).testTag("rem_more")) { Icon(Icons.Outlined.MoreVert, "เพิ่มเติม", Modifier.size(34.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("ล้างการแจ้งเตือนที่ผ่านไปแล้ว") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; vm.clearPastReminders() })
                        DropdownMenuItem(text = { Text("ตั้งค่าการแจ้งเตือน") }, leadingIcon = { Icon(Icons.Outlined.Settings, null) }, onClick = { menu = false; filter = RemFilter.SYSTEM })
                    }
                }
            }
        )
        if (searching) {
            GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                SearchField(query, "ค้นหาการแจ้งเตือน...", focus) { query = it }
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
                        Text("ความพร้อมของการแจ้งเตือน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        ReminderReliabilityCard()
                    }
                }
            }
        } else if (upcoming.isEmpty() && past.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ArtImage(R.drawable.art_bell, 240.dp)
                Spacer(Modifier.height(18.dp))
                Text(if (query.isBlank()) "ยังไม่มีการแจ้งเตือน" else "ไม่พบการแจ้งเตือน", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "คุณจะเห็นการแจ้งเตือนที่นี่\nเมื่อตั้งเตือนในโน้ต (เปิดโน้ต แล้วกดรูปกระดิ่ง)",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(30.dp)).background(com.sek789102.jotwai.ui.design.GoldBrush)
                        .clickable { tick++; query = ""; android.widget.Toast.makeText(context, "อัปเดตแล้ว", android.widget.Toast.LENGTH_SHORT).show() }
                        .padding(horizontal = 40.dp, vertical = 16.dp).testTag("rem_retry"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = Color(0xFF1A1206))
                    Spacer(Modifier.width(10.dp))
                    Text("ลองใหม่", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1206))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (upcoming.isNotEmpty()) item { Text("กำลังจะมาถึง", style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.padding(top = 4.dp)) }
                items(upcoming, key = { "u${it.note.id}" }) { ReminderRow(it, folders, false) { onOpenNote(it.note.id) } }
                if (past.isNotEmpty()) item { Text("ผ่านไปแล้ว", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
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
                Text(note.title.ifBlank { if (note.isLocked) "โน้ตที่ล็อกไว้" else preview(n).take(60).ifBlank { "โน้ตไม่มีชื่อ" } }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val repeat = if (note.reminderRepeat != com.sek789102.jotwai.data.model.RepeatRule.NONE) " · " + Dates.repeatLabel(context, note.reminderRepeat) else ""
                Text(Dates.dayAndTime(context, note.reminderAt ?: 0L) + repeat, style = MaterialTheme.typography.bodyMedium, color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else Gold)
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
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "ล้าง") }
    }
}

// ------------------------------------------------------------------ folders

private class Cell(val key: String, val content: @Composable (Modifier) -> Unit)

private enum class FolderSort(val label: String) { ORDER("ตามลำดับที่สร้าง"), NAME("ชื่อ ก-ฮ"), COUNT("จำนวนโน้ต") }

val folderArt = listOf(R.drawable.art_folder_yellow, R.drawable.art_folder_blue, R.drawable.art_folder_purple, R.drawable.art_folder_green)
private val folderColorNames = listOf("เหลือง", "ฟ้า", "ม่วง", "เขียว")

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
    val showFav = query.isBlank() || "รายการโปรด".contains(query, true)
    val showTrash = query.isBlank() || "ถังขยะ".contains(query, true)

    val cells = buildList {
        if (showFav) add(Cell("fav") { m ->
            FolderCard(R.drawable.art_folder_fav, "รายการโปรด", "$favCount โน้ต", m.testTag("folder_fav"), onClick = { onOpenList(Section.Favorites, false) }) { close ->
                DropdownMenuItem(text = { Text("เปิดดู") }, onClick = { close(); onOpenList(Section.Favorites, false) })
            }
        })
        list.forEach { f ->
            add(Cell("f${f.id}") { m ->
                FolderCard(folderArt[f.color.coerceIn(0, 3)], f.name, "${countIn(f.id)} โน้ต", m.testTag("folder_${f.id}"), onClick = { onOpenList(Section.Folder(f.id), false) }) { close ->
                    DropdownMenuItem(text = { Text("เปลี่ยนชื่อ") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { close(); renameTarget = f })
                    DropdownMenuItem(text = { Text("เปลี่ยนสี") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = { close(); colorTarget = f })
                    DropdownMenuItem(text = { Text("ลบโฟลเดอร์") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { close(); deleteTarget = f })
                }
            })
        }
        if (showTrash) add(Cell("trash") { m ->
            FolderCard(R.drawable.art_folder_trash, "ถังขยะ", "$trashCount โน้ต", m.testTag("folder_trash"), onClick = { onOpenList(Section.Trash, false) }) { close ->
                DropdownMenuItem(text = { Text("เปิดดู") }, onClick = { close(); onOpenList(Section.Trash, false) })
                if (trashCount > 0) DropdownMenuItem(text = { Text("ล้างถังขยะ") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { close(); confirmEmptyTrash = true })
            }
        })
        add(Cell("new") { m -> NewFolderCard(m.testTag("folder_new")) { createDialog = true } })
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                TabHeader(
                    title = "โฟลเดอร์",
                    leading = { ArtImage(R.drawable.art_folder_yellow, 76.dp) },
                    actions = {
                        IconButton(onClick = { runCatching { focus.requestFocus() } }, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.Search, "ค้นหา", Modifier.size(34.dp)) }
                        Box {
                            IconButton(onClick = { moreMenu = true }, modifier = Modifier.size(52.dp).testTag("folders_more")) { Icon(Icons.Outlined.MoreVert, "เพิ่มเติม", Modifier.size(34.dp)) }
                            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                                DropdownMenuItem(text = { Text("สร้างโฟลเดอร์ใหม่") }, leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) }, onClick = { moreMenu = false; createDialog = true })
                                DropdownMenuItem(text = { Text("คลังเก็บ") }, leadingIcon = { Icon(Icons.Outlined.Archive, null) }, onClick = { moreMenu = false; onOpenList(Section.Archive, false) })
                                DropdownMenuItem(text = { Text("จัดการป้ายกำกับ") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) }, onClick = { moreMenu = false; onManageLabels() })
                            }
                        }
                    }
                )
            }
            item {
                GoldSearchBox(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { SearchField(query, "ค้นหาโฟลเดอร์...", focus) { query = it } }
            }
            item {
                SectionTitle("โฟลเดอร์ของฉัน") {
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { sortMenu = true }.padding(horizontal = 14.dp, vertical = 10.dp).testTag("folders_sort"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("เรียงลำดับ", style = MaterialTheme.typography.labelLarge)
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
            item { SectionTitle("โน้ตทั้งหมด", Icons.Outlined.BarChart, Modifier.padding(top = 10.dp)) }
            item {
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("folders_all_notes"), onClick = { onOpenList(Section.Notes, false) }) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = Gold, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ดูโน้ตทั้งหมด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("มีทั้งหมด ${live.count { !it.note.isArchived }} โน้ต", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null)
                    }
                }
            }
        }
        NewNoteFab(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp)) { onNewNote(it, Section.Notes) }
    }

    if (createDialog) {
        FolderDialog(title = "สร้างโฟลเดอร์ใหม่", initialName = "", initialColor = 0, onDismiss = { createDialog = false }) { name, color ->
            createDialog = false
            vm.createFolder(name, color)
        }
    }
    renameTarget?.let { f ->
        NameDialog(title = "เปลี่ยนชื่อโฟลเดอร์", initial = f.name, onDismiss = { renameTarget = null }, onConfirm = { vm.renameFolder(f, it); renameTarget = null })
    }
    colorTarget?.let { f ->
        FolderDialog(title = "สีของโฟลเดอร์", initialName = f.name, initialColor = f.color, nameEditable = false, onDismiss = { colorTarget = null }) { _, c ->
            vm.setFolderColor(f, c); colorTarget = null
        }
    }
    deleteTarget?.let { f ->
        ConfirmDialog(
            title = "ลบโฟลเดอร์",
            message = "ลบโฟลเดอร์ \"${f.name}\" ? โน้ตข้างในจะไม่ถูกลบ แต่จะย้ายออกจากโฟลเดอร์",
            confirmLabel = "ลบ",
            onDismiss = { deleteTarget = null },
            onConfirm = { vm.deleteFolder(f.id) }
        )
    }
    if (confirmEmptyTrash) {
        ConfirmDialog(title = "ล้างถังขยะ", message = "ลบโน้ตในถังขยะทั้งหมดถาวร กู้คืนไม่ได้", confirmLabel = "ล้างถังขยะ", onDismiss = { confirmEmptyTrash = false }, onConfirm = { vm.emptyTrash() })
    }
}

@Composable
private fun FolderCard(art: Int, name: String, count: String, modifier: Modifier, onClick: () -> Unit, menuItems: @Composable (close: () -> Unit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    GlassCard(modifier.height(118.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(Gold))
            Row(Modifier.weight(1f).fillMaxSize().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtImage(art, 68.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "เพิ่มเติม") }
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
            Text("สร้างโฟลเดอร์ใหม่", style = MaterialTheme.typography.titleSmall, color = if (dark) Gold else GoldDeep, fontWeight = FontWeight.Bold)
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
                        label = { Text("ชื่อโฟลเดอร์") },
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
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), color) }, enabled = !nameEditable || name.isNotBlank()) { Text("บันทึก") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}
