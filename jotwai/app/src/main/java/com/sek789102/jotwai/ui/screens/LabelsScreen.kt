package com.sek789102.jotwai.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.jotwai.R
import com.sek789102.jotwai.data.model.LabelEntity
import com.sek789102.jotwai.ui.components.ConfirmDialog
import com.sek789102.jotwai.ui.components.EmptyState
import com.sek789102.jotwai.ui.components.NameDialog
import com.sek789102.jotwai.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val labels by vm.labels.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<LabelEntity?>(null) }
    var deleting by remember { mutableStateOf<LabelEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
                title = { Text(stringResource(R.string.labels_manage)) },
                actions = {
                    IconButton(onClick = { creating = true }, modifier = Modifier.testTag("label_new")) {
                        Icon(Icons.Outlined.NewLabel, stringResource(R.string.label_new))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (labels.isEmpty()) {
                item { EmptyState(Icons.AutoMirrored.Outlined.Label, stringResource(R.string.label_none_yet), stringResource(R.string.labels_empty_message)) }
            }
            items(labels, key = { it.id }) { l ->
                ListItem(
                    headlineContent = { Text(l.name) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Label, null) },
                    trailingContent = {
                        androidx.compose.foundation.layout.Row {
                            IconButton(onClick = { renaming = l }) { Icon(Icons.Outlined.Edit, stringResource(R.string.action_rename)) }
                            IconButton(onClick = { deleting = l }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
                        }
                    }
                )
            }
        }
    }

    if (creating) {
        NameDialog(
            title = stringResource(R.string.label_new),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { creating = false },
            onConfirm = { vm.createLabel(it); creating = false }
        )
    }
    renaming?.let { l ->
        NameDialog(
            title = stringResource(R.string.label_rename),
            initial = l.name,
            onDismiss = { renaming = null },
            onConfirm = { vm.renameLabel(l.id, it); renaming = null }
        )
    }
    deleting?.let { l ->
        ConfirmDialog(
            title = stringResource(R.string.label_delete_title),
            message = stringResource(R.string.label_delete_message, l.name),
            confirmLabel = stringResource(R.string.action_delete),
            onDismiss = { deleting = null },
            onConfirm = { vm.deleteLabel(l.id) }
        )
    }
}
