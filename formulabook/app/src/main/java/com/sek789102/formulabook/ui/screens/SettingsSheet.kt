package com.sek789102.formulabook.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.local.UserPreferences
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.ThemeMode
import com.sek789102.formulabook.data.model.accentStripeColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.repository.BackupImportSummary
import com.sek789102.formulabook.data.repository.BackupTooLargeException
import com.sek789102.formulabook.data.repository.InvalidBackupFormatException
import com.sek789102.formulabook.ui.components.CustomSubjectDialog
import com.sek789102.formulabook.ui.components.ReminderReliabilityCard
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    preferences: UserPreferences,
    notesCount: Int,
    formulasCount: Int,
    trashedCount: Int,
    subjects: List<SubjectEntity>,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleExamNotifications: (Boolean) -> Unit,
    onChangeDefaultPaper: (Int) -> Unit,
    onCreateSubject: (String, Long) -> Unit,
    onUpdateSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onExportBackup: suspend (java.io.OutputStream) -> Unit,
    onBackupExported: () -> Unit,
    onImportBackup: suspend (java.io.InputStream) -> Result<BackupImportSummary>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var deletingSubject by remember { mutableStateOf<SubjectEntity?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleExamNotifications(true)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_exam_notif_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val out = context.contentResolver.openOutputStream(uri)
                            ?: throw IllegalStateException("Unable to open output stream")
                        out.use { stream -> onExportBackup(stream) }
                    }
                    onBackupExported()
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

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Unable to open input stream")
                        stream.use { onImportBackup(it) }
                    }
                    result.onSuccess { summary ->
                        val msg = when {
                            summary.importedSubjects > 0 -> context.getString(
                                R.string.toast_import_with_subjects,
                                summary.importedNotes,
                                summary.importedSubjects,
                                summary.importedExams,
                                summary.totalSkipped
                            )
                            summary.importedExams > 0 || summary.skippedExams > 0 -> context.getString(
                                R.string.toast_import_result,
                                summary.importedNotes,
                                summary.importedExams,
                                summary.totalSkipped
                            )
                            else -> context.getString(
                                R.string.toast_import_notes_only,
                                summary.importedNotes,
                                summary.skippedNotes
                            )
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        val errMsgRes = when (err) {
                            is BackupTooLargeException -> R.string.toast_import_too_large
                            is InvalidBackupFormatException, is org.json.JSONException -> R.string.toast_import_invalid
                            else -> R.string.toast_import_read_error
                        }
                        Toast.makeText(context, context.getString(errMsgRes), Toast.LENGTH_SHORT).show()
                    }
                } catch (_: BackupTooLargeException) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_import_too_large),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: OutOfMemoryError) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_import_too_large),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_import_read_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Space24, vertical = Dimens.Space12),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudentMascotAvatar(
                    sizeDp = 54,
                    drawableRes = R.drawable.img_mascot_tags,
                    contentDescription = stringResource(R.string.mascot_tags_desc)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.settings_vault_summary, notesCount, formulasCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Theme Mode Selector (สว่าง / มืด / ตามระบบ)
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.settings_theme_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val themeOptions = listOf(
                    ThemeMode.LIGHT to R.string.theme_mode_light,
                    ThemeMode.DARK to R.string.theme_mode_dark,
                    ThemeMode.SYSTEM to R.string.theme_mode_system
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, (mode, labelRes) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = themeOptions.size
                            ),
                            selected = preferences.themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                inactiveBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Dynamic Color Toggle (Android 12+, default OFF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(Dimens.Space12))
                Switch(
                    checked = preferences.dynamicColor,
                    onCheckedChange = onToggleDynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    modifier = Modifier.testTag("dynamic_color_switch")
                )
            }

            // Exam Countdown Notifications Toggle (Feature 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_exam_notif_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_exam_notif_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(Dimens.Space12))
                Switch(
                    checked = preferences.examNotificationsEnabled,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Switch
                            }
                        }
                        onToggleExamNotifications(enable)
                    },
                    modifier = Modifier.testTag("exam_notifications_switch")
                )
            }

            if (preferences.examNotificationsEnabled) {
                ReminderReliabilityCard()
            }

            Column {
                Text(
                    text = stringResource(R.string.settings_default_paper_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(Dimens.Space8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                ) {
                    listOf(
                        0 to R.string.paper_style_lined,
                        1 to R.string.paper_style_grid,
                        2 to R.string.paper_style_plain
                    ).forEach { (id, labelRes) ->
                        FilterChip(
                            selected = preferences.defaultPaperStyle == id,
                            onClick = { onChangeDefaultPaper(id) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Custom Subjects Management (Feature 9)
            val customSubjects = subjects.filter { !it.isDefault }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_subjects_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_subjects_custom_count, customSubjects.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddSubjectDialog = true },
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier.testTag("settings_add_subject_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(Dimens.Space4))
                        Text(stringResource(R.string.subject_add_button))
                    }
                }

                if (customSubjects.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                        customSubjects.forEach { subj ->
                            Surface(
                                shape = RoundedCornerShape(Dimens.Radius12),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.Space12)
                                            .clip(CircleShape)
                                            .background(subj.accentStripeColor())
                                    )
                                    Spacer(Modifier.width(Dimens.Space12))
                                    Text(
                                        text = subj.displayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { editingSubject = subj }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { deletingSubject = subj }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Trash Box Row (moved from Home Hero into Settings)
            Surface(
                onClick = {
                    onDismiss()
                    onOpenTrash()
                },
                shape = RoundedCornerShape(Dimens.Radius16),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_trash_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius12),
                            color = if (trashedCount > 0) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.trash_open_desc),
                                tint = if (trashedCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier
                                    .padding(Dimens.Space8)
                                    .size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(Dimens.Space12))
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
                            Text(
                                text = stringResource(R.string.settings_trash_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (trashedCount > 0) {
                                    stringResource(R.string.settings_trash_subtitle_count, trashedCount)
                                } else {
                                    stringResource(R.string.settings_trash_subtitle_empty)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trashedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.Radius24),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = trashedCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = Dimens.Space8, vertical = Dimens.Space4)
                                )
                            }
                            Spacer(Modifier.width(Dimens.Space4))
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Backup & Restore Section
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Text(
                    text = stringResource(R.string.settings_backup_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)
                ) {
                    OutlinedButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            exportLauncher.launch("formulabook_backup_$stamp.zip")
                        },
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.Space8))
                        Text(
                            text = stringResource(R.string.settings_export),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/zip", "application/json", "text/plain", "application/octet-stream"))
                        },
                        shape = RoundedCornerShape(Dimens.Radius12),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.Space8))
                        Text(
                            text = stringResource(R.string.settings_import),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.Space16))
        }
    }

    if (showAddSubjectDialog) {
        CustomSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, colorHex ->
                onCreateSubject(name, colorHex)
                showAddSubjectDialog = false
            }
        )
    }

    editingSubject?.let { target ->
        CustomSubjectDialog(
            initialName = target.name,
            initialColorHex = target.colorHex,
            isEdit = true,
            onDismiss = { editingSubject = null },
            onConfirm = { name, colorHex ->
                onUpdateSubject(target.copy(name = name, colorHex = colorHex))
                editingSubject = null
            }
        )
    }

    deletingSubject?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSubject = null },
            title = { Text(stringResource(R.string.subject_delete_confirm_title)) },
            text = { Text(stringResource(R.string.subject_delete_confirm_msg, target.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSubject(target.id)
                        deletingSubject = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubject = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
