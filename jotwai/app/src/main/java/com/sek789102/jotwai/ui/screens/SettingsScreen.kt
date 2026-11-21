package com.sek789102.jotwai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sek789102.jotwai.BuildConfig
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.local.UserPreferences
import com.sek789102.jotwai.data.model.TextSize
import com.sek789102.jotwai.data.model.ThemeMode
import com.sek789102.jotwai.ui.components.ReminderReliabilityCard
import com.sek789102.jotwai.ui.viewmodel.AppViewModel
import com.sek789102.jotwai.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, prefs: UserPreferences, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }


    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { vm.exportBackup(it) } }
            }
            busy = false
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { vm.importBackup(it) } }
                    .onFailure { vm.notify(context.getString(R.string.msg_restore_failed)) }
            }
            busy = false
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
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
                title = { Text(stringResource(R.string.nav_settings)) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Group(stringResource(R.string.settings_display)) {
                Label(stringResource(R.string.settings_theme))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark).forEach { (m, l) ->
                        FilterChip(selected = prefs.themeMode == m, onClick = { vm.setThemeMode(m) }, label = { Text(stringResource(l)) })
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(stringResource(R.string.settings_dynamic_color), stringResource(R.string.settings_dynamic_color_desc), prefs.dynamicColor) { vm.setDynamicColor(it) }
                }
                Label(stringResource(R.string.settings_text_size))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TextSize.SMALL to R.string.text_small, TextSize.NORMAL to R.string.text_normal, TextSize.LARGE to R.string.text_large, TextSize.XLARGE to R.string.text_xlarge).forEach { (t, l) ->
                        FilterChip(selected = prefs.textSize == t, onClick = { vm.setTextSize(t) }, label = { Text(stringResource(l)) }, modifier = Modifier.testTag("text_${t.name}"))
                    }
                }
            }
            Group(stringResource(R.string.settings_checklists)) {
                SwitchRow(stringResource(R.string.settings_add_bottom), stringResource(R.string.settings_add_bottom_desc), prefs.checklistAddToBottom) { vm.setChecklistAddToBottom(it) }
                SwitchRow(stringResource(R.string.settings_move_checked), stringResource(R.string.settings_move_checked_desc), prefs.checklistMoveCheckedDown) { vm.setChecklistMoveCheckedDown(it) }
            }
            Group(stringResource(R.string.settings_reminders)) {
                if (!notifGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_notif_off), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) { Text(stringResource(R.string.reminder_reliability_allow)) }
                    }
                } else {
                    Text(stringResource(R.string.settings_notif_on), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ReminderReliabilityCard()
            }
            Group(stringResource(R.string.settings_backup)) {
                Text(stringResource(R.string.settings_backup_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (prefs.lastBackupAt > 0) stringResource(R.string.settings_last_backup, Dates.full(prefs.lastBackupAt))
                    else stringResource(R.string.settings_never_backed_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            exporter.launch("jotwai_backup_$stamp.zip")
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).testTag("backup_export")
                    ) {
                        Icon(Icons.Outlined.Backup, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_backup))
                    }
                    OutlinedButton(
                        onClick = { importer.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "text/plain")) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).testTag("backup_import")
                    ) {
                        Icon(Icons.Outlined.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_restore_backup))
                    }
                }
                Text(stringResource(R.string.settings_restore_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Group(stringResource(R.string.settings_privacy)) {
                Text(stringResource(R.string.privacy_offline), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.privacy_lock_note), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.privacy_backup_note), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
