package com.sek789102.jotwai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sek789102.jotwai.JotwaiApp
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.NoteType
import com.sek789102.jotwai.data.model.NoteVersionEntity
import com.sek789102.jotwai.domain.TextEditing
import com.sek789102.jotwai.ui.components.EmptyState
import com.sek789102.jotwai.util.Dates
import kotlinx.coroutines.launch

private fun NoteVersionEntity.previewText(): String = if (type == NoteType.CHECKLIST) {
    checklist.joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
} else TextEditing.toPlainText(content)

/** Earlier versions of a note, saved automatically each time it is edited. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(noteId: Long, onBack: () -> Unit, onRestored: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as JotwaiApp).repository
    val versions by remember(noteId) { repo.observeVersions(noteId) }.collectAsState(initial = null)
    var preview by remember { mutableStateOf<NoteVersionEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
                title = { Text(stringResource(R.string.history_title)) }
            )
        }
    ) { padding ->
        val list = versions
        if (list != null && list.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(Icons.Outlined.History, stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_message))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Text(
                        stringResource(R.string.history_info, com.sek789102.jotwai.data.repository.NotesRepository.MAX_VERSIONS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(list.orEmpty(), key = { it.id }) { v ->
                    ListItem(
                        headlineContent = { Text(Dates.full(v.savedAt)) },
                        supportingContent = {
                            Text(
                                listOf(v.title, v.previewText()).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.History, null) },
                        modifier = Modifier.clickable { preview = v }.testTag("version_${v.id}")
                    )
                }
            }
        }
    }

    preview?.let { v ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(Dates.full(v.savedAt)) },
            text = {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (v.title.isNotBlank()) Text(v.title, style = MaterialTheme.typography.titleMedium)
                        Text(v.previewText())
                        if (v.imagePaths.isNotEmpty()) {
                            Text(stringResource(R.string.history_images, v.imagePaths.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preview = null
                    scope.launch {
                        repo.restoreVersion(v.id)
                        onRestored()
                    }
                }, modifier = Modifier.testTag("version_restore")) { Text(stringResource(R.string.history_restore)) }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.action_close)) } }
        )
    }
}
