package com.sek789102.jotwai.ui.screens

import com.sek789102.jotwai.util.t
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sek789102.jotwai.BuildConfig
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.TextSize
import com.sek789102.jotwai.data.model.ThemeMode
import com.sek789102.jotwai.ui.design.ArtImage
import com.sek789102.jotwai.ui.design.GlassCard
import com.sek789102.jotwai.ui.design.Gold
import com.sek789102.jotwai.ui.design.GoldTile
import com.sek789102.jotwai.ui.design.TabHeader
import com.sek789102.jotwai.ui.design.isDarkUi
import com.sek789102.jotwai.ui.theme.isDarkTheme
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import com.sek789102.jotwai.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings tab (black-and-gold design). */
@Composable
fun SettingsTab(vm: AppViewModel, prefs: UserPreferences, onManageLabels: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var sizeDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var langDialog by remember { mutableStateOf(false) }
    val lang = remember { com.sek789102.jotwai.util.Lang.saved(context) }
    val dark = isDarkTheme(prefs.themeMode)

    fun io(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { block() } }
            busy = false
        }
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) io { context.contentResolver.openOutputStream(uri)?.use { vm.exportBackup(it) } }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) io {
            runCatching { context.contentResolver.openInputStream(uri)?.use { vm.importBackup(it) } }
                .onFailure { vm.notify(context.getString(R.string.msg_restore_failed)) }
        }
    }
    val exportText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) io { context.contentResolver.openOutputStream(uri)?.use { vm.exportText(it) } }
    }
    val importText = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) io {
            val files = uris.take(50).mapNotNull { u -> readTextFile(context, u) }
            vm.importTextNotes(files)
        }
    }

    var notifGranted by remember { mutableStateOf(true) }
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }
    LaunchedEffect(resumeTick) {
        notifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifGranted = ok
        if (ok) vm.setRemindersEnabled(true)
        else runCatching {
            // Asked before and refused: the phone no longer shows the question, so open the app's notification settings.
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        TabHeader(
            title = t("การตั้งค่า", "Settings"),
            subtitle = t("ปรับแต่งแอปให้เหมาะกับคุณ ✨", "Make the app yours ✨"),
            leading = { GoldTile(Icons.Filled.Settings, 68.dp, glass = true) },
            actions = { ThemeToggle(dark) { vm.setThemeMode(if (dark) ThemeMode.LIGHT else ThemeMode.DARK) } }
        )
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("settings_about_card"), onClick = { aboutDialog = true }) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtImage(R.drawable.art_app_circle, 84.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("จดไว้", "Jotwai"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(t("จดทุกไอเดีย ให้เป็นเรื่องง่าย", "Note every idea, the easy way"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ChevronRight, null)
            }
        }

        Group(t("การตั้งค่าทั่วไป", "General"), Icons.Filled.Settings) {
            Item(Icons.Filled.Palette, t("ธีมแอป", "App theme"), themeName(prefs.themeMode), tag = "set_theme", onClick = { themeDialog = true }) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Gold))
            }
            Divider()
            Item(Icons.Outlined.Language, "ภาษา / Language", langName(lang), tag = "set_language", onClick = { langDialog = true })
            Divider()
            Item(null, t("ขนาดตัวอักษร", "Text size"), sizeName(prefs.textSize), tag = "set_text_size", onClick = { sizeDialog = true }, leadingText = "Aa")
            Divider()
            val on = prefs.remindersEnabled && notifGranted
            Item(Icons.Filled.Notifications, t("การแจ้งเตือน", "Reminders"), if (on) t("เปิดใช้งาน", "On") else t("ปิดอยู่", "Off"), tag = "set_notifications", onClick = null, chevron = false) {
                Switch(
                    checked = on,
                    onCheckedChange = { want ->
                        if (want && !notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else vm.setRemindersEnabled(want)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Color.White),
                    modifier = Modifier.testTag("notif_switch")
                )
            }
        }

        Group(t("ข้อมูลและการสำรองข้อมูล", "Data and backup"), Icons.Filled.CloudUpload) {
            Item(
                Icons.Filled.CloudUpload, t("สำรองข้อมูลโน้ต", "Back up notes"),
                if (prefs.lastBackupAt > 0) t("ล่าสุด ${Dates.full(prefs.lastBackupAt)} · เก็บใน Google Drive ได้", "Last ${Dates.full(prefs.lastBackupAt)} · can be kept in Google Drive") else t("บันทึกเป็นไฟล์ เก็บใน Google Drive หรือที่อื่นได้", "Saved as a file you can keep in Google Drive or elsewhere"),
                tag = "backup_export", enabled = !busy,
                onClick = { backup.launch("jotwai_backup_${stamp()}.zip") }
            )
            Divider()
            Item(Icons.Outlined.History, t("กู้คืนข้อมูล", "Restore data"), t("นำข้อมูลกลับจากไฟล์สำรองข้อมูล", "Bring data back from a backup file"), tag = "backup_import", enabled = !busy,
                onClick = { restore.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "text/plain")) })
            Divider()
            Item(Icons.Outlined.IosShare, t("ส่งออกข้อมูล", "Export data"), t("ส่งออกโน้ตเป็นไฟล์ข้อความ (.txt)", "Export notes as a text file (.txt)"), tag = "export_text", enabled = !busy,
                onClick = { exportText.launch("jotwai_notes_${stamp()}.txt") })
            Divider()
            Item(Icons.Outlined.Download, t("นำเข้าข้อมูล", "Import data"), t("นำเข้าโน้ตจากไฟล์ข้อความ (.txt)", "Import notes from text files (.txt)"), tag = "import_text", enabled = !busy,
                onClick = { importText.launch(arrayOf("text/*")) })
        }

        Group(t("เพิ่มเติม", "More"), Icons.Outlined.CheckBox) {
            Item(Icons.AutoMirrored.Outlined.Label, t("ป้ายกำกับ", "Labels"), t("เพิ่ม เปลี่ยนชื่อ หรือลบป้ายกำกับ", "Add, rename or delete labels"), tag = "set_labels", onClick = onManageLabels)
            Divider()
            SwitchItem(stringResource(R.string.settings_add_bottom), stringResource(R.string.settings_add_bottom_desc), prefs.checklistAddToBottom) { vm.setChecklistAddToBottom(it) }
            Divider()
            SwitchItem(stringResource(R.string.settings_move_checked), stringResource(R.string.settings_move_checked_desc), prefs.checklistMoveCheckedDown) { vm.setChecklistMoveCheckedDown(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Divider()
                SwitchItem(stringResource(R.string.settings_dynamic_color), stringResource(R.string.settings_dynamic_color_desc), prefs.dynamicColor, Icons.Outlined.Wallpaper) { vm.setDynamicColor(it) }
            }
        }

        Group(t("เกี่ยวกับแอป", "About"), Icons.Filled.Info) {
            Item(Icons.Filled.Star, t("ให้คะแนนแอป", "Rate the app"), t("ขอบคุณที่ใช้แอปของเรา!", "Thank you for using our app!"), tag = "rate_app", onClick = { openStore(context) })
            Divider()
            Item(Icons.Filled.Info, t("เวอร์ชันแอป", "App version"), t("เวอร์ชัน ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"), tag = "set_version", onClick = { aboutDialog = true }, chevron = false) {
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (langDialog) {
        ChoiceDialog(
            "ภาษา / Language",
            listOf(com.sek789102.jotwai.util.Lang.SYSTEM to langName(com.sek789102.jotwai.util.Lang.SYSTEM), com.sek789102.jotwai.util.Lang.THAI to "ไทย", com.sek789102.jotwai.util.Lang.ENGLISH to "English"),
            lang,
            onDismiss = { langDialog = false }
        ) { v -> langDialog = false; if (v != lang) com.sek789102.jotwai.util.Lang.change(context, v) }
    }
    if (themeDialog) {
        ChoiceDialog(
            t("ธีมแอป", "App theme"),
            listOf(ThemeMode.DARK to t("ธีมมืด (ค่าเริ่มต้น)", "Dark theme (default)"), ThemeMode.LIGHT to t("ธีมสว่าง", "Light theme"), ThemeMode.SYSTEM to t("ตามระบบ", "Follow phone")),
            prefs.themeMode,
            onDismiss = { themeDialog = false }
        ) { vm.setThemeMode(it); themeDialog = false }
    }
    if (sizeDialog) {
        ChoiceDialog(
            t("ขนาดตัวอักษร", "Text size"),
            listOf(TextSize.SMALL to t("เล็ก", "Small"), TextSize.NORMAL to t("ปกติ", "Normal"), TextSize.LARGE to t("ใหญ่", "Large"), TextSize.XLARGE to t("ใหญ่มาก", "Extra large")),
            prefs.textSize,
            onDismiss = { sizeDialog = false }
        ) { vm.setTextSize(it); sizeDialog = false }
    }
    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            icon = { ArtImage(R.drawable.art_app_circle, 64.dp) },
            title = { Text(t("จดไว้ ${BuildConfig.VERSION_NAME}", "Jotwai ${BuildConfig.VERSION_NAME}")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.privacy_offline))
                    Text(stringResource(R.string.privacy_lock_note))
                    Text(stringResource(R.string.privacy_backup_note))
                }
            },
            confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text(t("ปิด", "Close")) } }
        )
    }
}

private fun langName(v: String) = when (v) {
    com.sek789102.jotwai.util.Lang.THAI -> "ไทย"
    com.sek789102.jotwai.util.Lang.ENGLISH -> "English"
    else -> t("ตามภาษาของโทรศัพท์", "Same as phone")
}

private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

private fun themeName(m: ThemeMode) = when (m) {
    ThemeMode.DARK -> t("ธีมมืด (ค่าเริ่มต้น)", "Dark theme (default)")
    ThemeMode.LIGHT -> t("ธีมสว่าง", "Light theme")
    ThemeMode.SYSTEM -> t("ตามระบบ", "Follow phone")
}

private fun sizeName(t: TextSize) = when (t) {
    TextSize.SMALL -> t("เล็ก", "Small")
    TextSize.NORMAL -> t("ปกติ", "Normal")
    TextSize.LARGE -> t("ใหญ่", "Large")
    TextSize.XLARGE -> t("ใหญ่มาก", "Extra large")
}

/** Reads a picked text file (max 1 MB) as (file name, text). */
private fun readTextFile(context: android.content.Context, uri: Uri): Pair<String, String>? = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: t("โน้ต", "Notes")
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readNBytesCompat(1_000_000) } ?: return null
    name to String(bytes, Charsets.UTF_8)
}.getOrNull()

private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

private fun openStore(context: android.content.Context) {
    val id = BuildConfig.APPLICATION_ID
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun ThemeToggle(dark: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 8.dp).testTag("theme_toggle"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.DarkMode, t("ธีมมืด", "Dark theme"), tint = if (dark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        if (dark) Box(Modifier.size(30.dp).clip(CircleShape).background(Gold))
        else Icon(Icons.Filled.LightMode, t("ธีมสว่าง", "Light theme"), tint = Gold, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun Group(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Row(Modifier.padding(start = 24.dp, end = 20.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Gold.copy(alpha = if (isDarkUi()) 0.18f else 0.3f))

@Composable
private fun Item(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    tag: String,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    chevron: Boolean = true,
    leadingText: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            if (leadingText != null) Text(leadingText, color = com.sek789102.jotwai.ui.design.goldText(), fontWeight = FontWeight.Bold, fontSize = 26.sp)
            else if (icon != null) Icon(icon, null, tint = Gold, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (chevron && onClick != null) Icon(Icons.Outlined.ChevronRight, null)
    }
}

@Composable
private fun SwitchItem(title: String, desc: String, checked: Boolean, icon: ImageVector = Icons.Outlined.CheckBox, onChange: (Boolean) -> Unit) {
    Item(icon, title, desc, tag = "switch_$title", onClick = { onChange(!checked) }, chevron = false) {
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Color.White))
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, current: T, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { i, (v, label) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(v) }.padding(vertical = 6.dp).testTag("choice_$i"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = v == current, onClick = { onPick(v) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(t("ปิด", "Close")) } }
    )
}
