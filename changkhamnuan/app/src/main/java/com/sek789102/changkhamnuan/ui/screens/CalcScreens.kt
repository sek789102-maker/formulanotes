package com.sek789102.changkhamnuan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.changkhamnuan.domain.CalcResult
import com.sek789102.changkhamnuan.domain.Calculator
import com.sek789102.changkhamnuan.domain.Calculators
import com.sek789102.changkhamnuan.domain.ChoiceField
import com.sek789102.changkhamnuan.domain.Fmt
import com.sek789102.changkhamnuan.domain.NumberField
import com.sek789102.changkhamnuan.domain.isVisible
import com.sek789102.changkhamnuan.ui.AppViewModel
import com.sek789102.changkhamnuan.ui.components.yellowBarColors
import com.sek789102.changkhamnuan.ui.components.ChoiceInput
import com.sek789102.changkhamnuan.ui.components.KeyValueRow
import com.sek789102.changkhamnuan.ui.components.NumberInput
import com.sek789102.changkhamnuan.ui.components.SectionTitle

fun calcIcon(id: String): ImageVector = when (id) {
    "bricks" -> Icons.Outlined.ViewModule
    "plaster" -> Icons.Outlined.Layers
    "concrete" -> Icons.Outlined.CropSquare
    "tiles" -> Icons.Outlined.GridOn
    "paint" -> Icons.Outlined.FormatPaint
    "rebar" -> Icons.Outlined.LinearScale
    else -> Icons.Outlined.SquareFoot
}

@Composable
fun CalcHomeScreen(padding: PaddingValues, onOpen: (String) -> Unit, onConverter: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().padding(padding).testTag("calc_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Calculators.all, key = { it.id }) { c ->
            CalcCard(calcIcon(c.id), c.title, c.subtitle, "calc_${c.id}") { onOpen(c.id) }
        }
        item { CalcCard(Icons.Outlined.SquareFoot, "แปลงหน่วย", "ตร.วา · ไร่ · ฟุต · คิว", "calc_converter", onConverter) }
    }
}

@Composable
private fun CalcCard(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).testTag(tag)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary) }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcScreen(calcId: String, vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit, onOpenProject: (Long) -> Unit) {
    val calc: Calculator = Calculators.byId(calcId) ?: return onBack()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val prices by vm.prices.collectAsStateWithLifecycle()
    val values = rememberSaveable(calcId, saver = mapSaver()) {
        mutableStateMapOf<String, String>().apply {
            calc.fields.filter { it.isVisible(values, calc) }.forEach { f ->
                when (f) {
                    is NumberField -> put(f.key, Fmt.n(f.default, 3).replace(",", ""))
                    is ChoiceField -> put(f.key, f.default)
                }
            }
        }
    }
    var labor by rememberSaveable(calcId) { mutableStateOf("") }
    var addDialog by remember { mutableStateOf(false) }
    val result: CalcResult = remember(values.toMap(), rates) { calc.run(values.toMap(), rates) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text(calc.title) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            calc.fields.filter { it.isVisible(values, calc) }.forEach { f ->
                when (f) {
                    is NumberField -> NumberInput(values[f.key].orEmpty(), { values[f.key] = it }, f.label, f.unit, help = f.help, tag = "in_${f.key}")
                    is ChoiceField -> ChoiceInput(f.label, f.options, values[f.key] ?: f.default, { values[f.key] = it }, tag = "in_${f.key}")
                }
            }
            ResultCard(result, prices)
            SectionTitle("ค่าแรง (ไม่บังคับ)")
            NumberInput(labor, { labor = it }, "ค่าแรง", "บาท/${result.laborUnit}", tag = "in_labor",
                help = if ((labor.toDoubleOrNull() ?: 0.0) > 0) "รวม ${Fmt.money((labor.toDoubleOrNull() ?: 0.0) * result.laborBase)} บาท (${Fmt.n(result.laborBase)} ${result.laborUnit})" else null)
            Button(
                onClick = { addDialog = true },
                enabled = result.materials.any { it.buyQty > 0 },
                modifier = Modifier.fillMaxWidth().testTag("add_to_project")
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("เพิ่มลงงาน / ใบเสนอราคา")
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    "ตัวเลขเป็นค่าประมาณจากอัตรามาตรฐาน ปรับอัตราได้ที่ “เพิ่มเติม → อัตราการใช้วัสดุ” ควรเผื่อตามหน้างานจริง",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addDialog) {
        AddToProjectDialog(
            vm = vm,
            defaultGroup = calc.title,
            onDismiss = { addDialog = false },
            onAdd = { projectId, projectName, group ->
                vm.addCalcResult(projectId, group, result, labor.toDoubleOrNull() ?: 0.0, projectName)
                addDialog = false
            },
            onOpenProject = onOpenProject
        )
    }
}

@Composable
fun ResultCard(result: CalcResult, prices: Map<String, Double>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth().testTag("result_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ผลการคำนวณ", style = MaterialTheme.typography.titleMedium)
            result.facts.forEach { (k, v) -> KeyValueRow(k, v) }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("ต้องซื้อ", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            var total = 0.0
            var priced = true
            result.materials.filter { it.buyQty > 0 }.forEach { m ->
                val p = prices[m.priceKey]
                if (p != null) total += p * m.buyQty else priced = false
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(m.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${Fmt.n(m.buyQty)} ${m.buyUnit}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("buy_${m.priceKey}"))
                    }
                    val sub = listOfNotNull(m.note, p?.let { "≈ ${Fmt.money(it * m.buyQty)} บาท (${Fmt.money(it)}/${m.buyUnit})" })
                    if (sub.isNotEmpty()) Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (total > 0) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                KeyValueRow(if (priced) "ค่าวัสดุโดยประมาณ" else "ค่าวัสดุ (เฉพาะที่ตั้งราคาไว้)", "${Fmt.money(total)} บาท", bold = true)
            }
            result.warnings.forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Warning, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(w, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddToProjectDialog(
    vm: AppViewModel,
    defaultGroup: String,
    onDismiss: () -> Unit,
    onAdd: (Long, String, String) -> Unit,
    onOpenProject: (Long) -> Unit
) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    var group by rememberSaveable { mutableStateOf(defaultGroup) }
    var newName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มลงงาน") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(group, { group = it.take(60) }, label = { Text("ชื่อรายการ เช่น ผนังห้องนอน") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("เลือกงาน", style = MaterialTheme.typography.labelLarge)
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(projects.orEmpty(), key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { if (p.customerName.isNotBlank()) Text(p.customerName, maxLines = 1) },
                            modifier = Modifier.clickable { onAdd(p.id, p.name, group.ifBlank { defaultGroup }) }.testTag("pick_project_${p.id}")
                        )
                    }
                }
                OutlinedTextField(newName, { newName = it.take(80) }, label = { Text("หรือสร้างงานใหม่") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name"))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = newName.ifBlank { "งานใหม่" }
                    vm.createProject(name) { id -> onAdd(id, name, group.ifBlank { defaultGroup }); onOpenProject(id) }
                },
                modifier = Modifier.testTag("create_and_add")
            ) { Text("สร้างงานใหม่แล้วเพิ่ม") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}

/** Saves a SnapshotStateMap<String,String> across rotation. */
fun mapSaver() = androidx.compose.runtime.saveable.Saver<androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>, ArrayList<String>>(
    save = { m -> ArrayList(m.flatMap { listOf(it.key, it.value) }) },
    restore = { l -> mutableStateMapOf<String, String>().apply { l.chunked(2).forEach { if (it.size == 2) put(it[0], it[1]) } } }
)
