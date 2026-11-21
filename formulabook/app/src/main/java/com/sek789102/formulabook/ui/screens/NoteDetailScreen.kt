package com.sek789102.formulabook.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectEntity
import com.sek789102.formulabook.data.model.badgeBgColor
import com.sek789102.formulabook.data.model.badgeFgColor
import com.sek789102.formulabook.data.model.displayName
import com.sek789102.formulabook.data.model.mascotDrawableRes
import com.sek789102.formulabook.data.model.resolveDisplayName
import com.sek789102.formulabook.domain.markdown.SimpleMarkdown
import com.sek789102.formulabook.ui.components.FormulaDisplayCard
import com.sek789102.formulabook.ui.components.FormulaEvaluatorDialog
import com.sek789102.formulabook.ui.components.MarkdownContentView
import com.sek789102.formulabook.ui.components.NoteImageThumbnail
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.components.ZoomableImageDialog
import com.sek789102.formulabook.ui.components.notebookPaperBackground
import com.sek789102.formulabook.ui.components.paperStrokeColors
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.util.NotePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    noteWithTags: NoteWithTags?,
    subject: SubjectEntity?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onToggleNeedsReview: (Long, Boolean) -> Unit,
    onToggleFormulaMastered: (Long, String) -> Unit,
    onToggleChecklistLine: (Long, Int) -> Unit,
    onPlotFormula: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var recallMode by rememberSaveable { mutableStateOf(false) }
    var activeEvalFormula by remember { mutableStateOf<FormulaBlock?>(null) }
    var zoomedImageFileName by rememberSaveable { mutableStateOf<String?>(null) }

    if (noteWithTags == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.detail_note_not_found))
        }
        return
    }

    val note = noteWithTags.note
    val dateLabel = SimpleDateFormat("d MMMM yyyy · HH:mm", Locale.forLanguageTag("th-TH")).format(Date(note.updatedAt))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subject.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onTogglePin(note.id, note.isPinned) }) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(R.string.pin_action_pin),
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onToggleNeedsReview(note.id, note.needsReview) }) {
                        Icon(
                            imageVector = if (note.needsReview) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.badge_needs_review),
                            tint = if (note.needsReview) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val pdfUri = withContext(Dispatchers.IO) {
                                    NotePdfExporter.exportNoteToPdfUri(context, noteWithTags, subject)
                                }
                                if (pdfUri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                                        putExtra(Intent.EXTRA_SUBJECT, note.title)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(intent, context.getString(R.string.detail_export_pdf))
                                        )
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_no_app_to_handle),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_pdf_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("detail_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = stringResource(R.string.detail_export_pdf)
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareText = buildShareText(context, noteWithTags, subject)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { context.getString(R.string.note_untitled_paren) })
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.detail_share_note)))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_no_app_to_handle),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.detail_share_note))
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("detail_delete_button")
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.detail_delete_note),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEdit(note.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("detail_edit_fab")
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.detail_edit_note))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.Space8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = recallMode,
                        onClick = { recallMode = !recallMode },
                        label = { Text(stringResource(R.string.detail_recall_mode)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (recallMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.testTag("detail_recall_toggle")
                    )
                    if (recallMode) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.Radius12),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = stringResource(R.string.detail_recall_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                val (paperLineColor, paperMarginColor) = paperStrokeColors()
                // Main Paper Sheet
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.Radius24),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = Dimens.ElevationNone
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .notebookPaperBackground(
                                paperStyle = note.paperStyle,
                                lineColor = paperLineColor,
                                marginColor = paperMarginColor
                            )
                            .padding(
                                start = if (note.paperStyle == 0) Dimens.Space32 else Dimens.Space24,
                                end = Dimens.Space24,
                                top = Dimens.Space24,
                                bottom = Dimens.Space24
                            )
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StudentMascotAvatar(
                                sizeDp = 34,
                                drawableRes = subject.mascotDrawableRes(),
                                contentDescription = subject.displayName(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = subject.badgeBgColor(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = subject.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = subject.badgeFgColor(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = note.title.ifBlank { stringResource(R.string.note_untitled_paren) },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (note.content.isNotBlank()) {
                            Spacer(Modifier.height(18.dp))
                            MarkdownContentView(
                                content = note.content,
                                recallMode = recallMode,
                                onToggleChecklistLine = { lineIdx ->
                                    onToggleChecklistLine(note.id, lineIdx)
                                }
                            )
                        }
                    }
                }

                // Attached Images Section (Feature 1)
                if (note.imagePaths.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.detail_images_section, note.imagePaths.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        note.imagePaths.forEach { imgFile ->
                            NoteImageThumbnail(
                                imageFileName = imgFile,
                                size = 118.dp,
                                onClick = { zoomedImageFileName = imgFile }
                            )
                        }
                    }
                }

                if (note.formulas.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.detail_formulas_section, note.formulas.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        note.formulas.forEach { formula ->
                            FormulaDisplayCard(
                                formula = formula,
                                recallMode = recallMode,
                                onCopy = {
                                    clipboard.setText(AnnotatedString(formula.expression))
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_formula_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onEvaluate = { activeEvalFormula = formula },
                                onPlotGraph = { onPlotFormula(formula.expression) },
                                onToggleMastered = { onToggleFormulaMastered(note.id, formula.id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(88.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_note_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_to_trash_message,
                        note.title.ifBlank { stringResource(R.string.note_untitled_paren) }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(note.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_note_button")
                ) {
                    Text(
                        text = stringResource(R.string.detail_delete_note),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    activeEvalFormula?.let { formula ->
        FormulaEvaluatorDialog(
            formula = formula,
            onDismiss = { activeEvalFormula = null }
        )
    }

    zoomedImageFileName?.let { fileName ->
        ZoomableImageDialog(
            imageFileName = fileName,
            onDismiss = { zoomedImageFileName = null }
        )
    }
}

private fun buildShareText(
    context: Context,
    noteWithTags: NoteWithTags,
    subject: SubjectEntity?
): String {
    val n = noteWithTags.note
    val subjectName = subject.resolveDisplayName(context)
    val safeTitle = n.title.ifBlank { context.getString(R.string.note_untitled_paren) }
    return buildString {
        appendLine("📒 $safeTitle [$subjectName]")
        appendLine()
        if (n.content.isNotBlank()) {
            appendLine(SimpleMarkdown.toReadablePlainLines(n.content).joinToString("\n"))
            appendLine()
        }
        if (n.formulas.isNotEmpty()) {
            appendLine(context.getString(R.string.share_formulas_header))
            n.formulas.forEachIndexed { idx, f ->
                val title = f.title.ifBlank { context.getString(R.string.share_formula_fallback) }
                appendLine("${idx + 1}. $title: ${f.expression}")
                if (f.variablesNote.isNotBlank()) {
                    appendLine("   (${f.variablesNote})")
                }
            }
        }
    }.trim()
}
