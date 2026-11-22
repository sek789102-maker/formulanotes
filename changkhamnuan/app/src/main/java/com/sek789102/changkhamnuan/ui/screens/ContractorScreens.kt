package com.sek789102.changkhamnuan.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.changkhamnuan.data.CostEntity
import com.sek789102.changkhamnuan.data.InstallmentEntity
import com.sek789102.changkhamnuan.domain.Contractor
import com.sek789102.changkhamnuan.domain.DocType
import com.sek789102.changkhamnuan.domain.Fmt
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.OrderLine
import com.sek789102.changkhamnuan.domain.QuoteLine
import com.sek789102.changkhamnuan.domain.QuoteMath
import com.sek789102.changkhamnuan.domain.QuoteOptions
import com.sek789102.changkhamnuan.ui.AppViewModel
import com.sek789102.changkhamnuan.ui.components.ChoiceInput
import com.sek789102.changkhamnuan.ui.components.ConfirmDialog
import com.sek789102.changkhamnuan.ui.components.GoldSection
import com.sek789102.changkhamnuan.ui.components.KeyValueRow
import com.sek789102.changkhamnuan.ui.components.NumberInput
import com.sek789102.changkhamnuan.ui.components.Panel
import com.sek789102.changkhamnuan.ui.components.yellowBarColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val thaiDate = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("th-TH"))

/** Starts a share sheet for an intent built off the main thread. */
fun startShare(context: Context, vm: AppViewModel, intent: Intent?, title: String) {
    if (intent == null) return
    try {
        context.startActivity(Intent.createChooser(intent, title))
    } catch (_: ActivityNotFoundException) {
        vm.say("ไม่พบแอปที่ใช้ส่งไฟล์")
    }
}

/** Picks image / PDF (and CSV for BOQ) for a document, then shares it. */
@Composable
fun DocFormatDialog(
    vm: AppViewModel,
    projectId: Long,
    type: DocType,
    stageId: Long? = null,
    titleSuffix: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun go(asImage: Boolean) {
        onDismiss()
        scope.launch { startShare(context, vm, vm.docShareIntent(projectId, type, asImage, stageId), "ส่ง${type.th}") }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(type.th + titleSuffix) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { go(true) }, modifier = Modifier.fillMaxWidth().testTag("doc_image")) {
                    Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(8.dp)); Text("ส่งเป็นรูป (LINE)")
                }
                OutlinedButton(onClick = { go(false) }, modifier = Modifier.fillMaxWidth().testTag("doc_pdf")) {
                    Icon(Icons.Outlined.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("ส่งเป็น PDF")
                }
                if (type == DocType.BOQ) {
                    OutlinedButton(onClick = {
                        onDismiss()
                        scope.launch { startShare(context, vm, vm.boqCsvIntent(projectId), "ส่งไฟล์ BOQ") }
                    }, modifier = Modifier.fillMaxWidth().testTag("doc_csv")) {
                        Icon(Icons.Outlined.TableChart, null); Spacer(Modifier.width(8.dp)); Text("ไฟล์ Excel (CSV)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("ปิด") } }
    )
}

// ------------------------------------------------------------------ payment stages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagesScreen(projectId: Long, vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val project by remember(projectId) { vm.observeProject(projectId) }.collectAsState(initial = null)
    val items by remember(projectId) { vm.observeItems(projectId) }.collectAsState(initial = emptyList())
    val stages by remember(projectId) { vm.observeInstallments(projectId) }.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InstallmentEntity?>(null) }
    var doc by remember { mutableStateOf<Pair<DocType, InstallmentEntity>?>(null) }
    var presetAsk by remember { mutableStateOf<Pair<String, List<Pair<String, Double>>>?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("งวดงาน · ${project?.name.orEmpty()}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    ) { padding ->
        val p = project ?: return@Scaffold
        val total = vm.contractTotal(p, items)
        val sum = Contractor.payments(total, stages.map { Triple(it.percent, it.amount, it.paidAt > 0) })
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth().testTag("stage_summary")) {
                    Column(Modifier.padding(16.dp)) {
                        KeyValueRow("มูลค่างาน (ตามใบเสนอราคา)", "${Fmt.money(sum.contract)} บาท", bold = true)
                        KeyValueRow("แบ่งงวดแล้ว", "${Fmt.money(sum.scheduled)} บาท")
                        KeyValueRow("รับเงินแล้ว", "${Fmt.money(sum.paid)} บาท")
                        KeyValueRow("ค้างรับ", "${Fmt.money(sum.outstanding)} บาท", bold = true)
                        if (stages.isNotEmpty() && kotlin.math.abs(sum.unscheduled) > 0.5) {
                            Text(
                                if (sum.unscheduled > 0) "ยังไม่ได้แบ่งงวดอีก ${Fmt.money(sum.unscheduled)} บาท" else "แบ่งงวดเกินมูลค่างาน ${Fmt.money(-sum.unscheduled)} บาท",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (total <= 0) Text("ใส่รายการและราคาในงานก่อน จึงจะคิดเงินแต่ละงวดได้", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                GoldSection("แบ่งงวดอัตโนมัติ")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Contractor.presets.forEachIndexed { idx, preset ->
                        AssistChip(onClick = { presetAsk = preset }, label = { Text(preset.first, maxLines = 1) }, modifier = Modifier.testTag("preset_$idx"))
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldSection("งวดงาน", Modifier.weight(1f))
                    TextButton(onClick = { adding = true }, modifier = Modifier.testTag("add_stage")) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("เพิ่มงวด") }
                }
            }
            if (stages.isEmpty()) item {
                Text("ยังไม่มีงวดงาน กดแบ่งงวดอัตโนมัติด้านบน หรือ “เพิ่มงวด”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(stages, key = { it.id }) { st ->
                val idx = stages.indexOf(st) + 1
                val amount = Contractor.stageAmount(st.percent, st.amount, total)
                Panel(Modifier.fillMaxWidth().testTag("stage_$idx"), radius = 18.dp, onClick = { editing = st }) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("งวดที่ $idx · ${st.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    (if (st.percent > 0) "${Fmt.n(st.percent)}% · " else "") +
                                        if (st.paidAt > 0) "รับเงินแล้ว ${thaiDate.format(Date(st.paidAt))}" else "ยังไม่ได้รับเงิน",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (st.paidAt > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Fmt.money(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Text("รับเงินแล้ว", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = st.paidAt > 0,
                                onCheckedChange = { vm.updateInstallment(st.copy(paidAt = if (it) System.currentTimeMillis() else 0L)) },
                                modifier = Modifier.testTag("stage_paid_$idx")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { doc = DocType.INVOICE to st }, modifier = Modifier.weight(1f).testTag("stage_invoice_$idx")) { Text("ใบแจ้งหนี้") }
                            Button(onClick = { doc = DocType.RECEIPT to st }, enabled = st.paidAt > 0, modifier = Modifier.weight(1f).testTag("stage_receipt_$idx")) { Text("ใบเสร็จ") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.size(32.dp)) }
        }
    }

    if (adding) StageDialog(null, onDismiss = { adding = false }) { title, pct, amt -> vm.addInstallment(projectId, title, pct, amt); adding = false }
    editing?.let { st ->
        StageDialog(st, onDismiss = { editing = null }, onDelete = { vm.deleteInstallment(st); editing = null }) { title, pct, amt ->
            vm.updateInstallment(st.copy(title = title.ifBlank { st.title }, percent = pct, amount = amt)); editing = null
        }
    }
    doc?.let { (type, st) ->
        DocFormatDialog(vm, projectId, type, st.id, " · งวดที่ ${stages.indexOf(st) + 1}") { doc = null }
    }
    presetAsk?.let { preset ->
        ConfirmDialog(
            "แบ่งงวดแบบ ${preset.first}?", "งวดที่ยังไม่ได้รับเงินจะถูกแทนที่ งวดที่รับเงินแล้วยังอยู่", "แบ่งงวด",
            onDismiss = { presetAsk = null }
        ) { vm.applyPreset(projectId, preset.second) }
    }
}

@Composable
private fun StageDialog(st: InstallmentEntity?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (String, Double, Double) -> Unit) {
    var title by rememberSaveable { mutableStateOf(st?.title ?: "") }
    var mode by rememberSaveable { mutableStateOf(if (st != null && st.percent <= 0) "amount" else "percent") }
    var value by rememberSaveable { mutableStateOf(st?.let { Fmt.n(if (it.percent > 0) it.percent else it.amount).replace(",", "") } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (st == null) "เพิ่มงวดงาน" else "แก้ไขงวดงาน") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("ชื่องวด เช่น มัดจำ / งานโครงสร้างเสร็จ") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stage_title"))
                ChoiceInput("คิดเป็น", listOf("percent" to "% ของมูลค่างาน", "amount" to "จำนวนเงิน (บาท)"), mode, { mode = it })
                NumberInput(value, { value = it }, if (mode == "percent") "เปอร์เซ็นต์" else "จำนวนเงิน", if (mode == "percent") "%" else "บาท", tag = "stage_value")
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error); Text("ลบงวดนี้", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = value.toDoubleOrNull() ?: 0.0
                onSave(title.ifBlank { "งวดงาน" }, if (mode == "percent") v.coerceIn(0.0, 100.0) else 0.0, if (mode == "amount") v else 0.0)
            }, enabled = (value.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.testTag("stage_save")) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}

// ------------------------------------------------------------------ costs & profit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostsScreen(projectId: Long, vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val project by remember(projectId) { vm.observeProject(projectId) }.collectAsState(initial = null)
    val items by remember(projectId) { vm.observeItems(projectId) }.collectAsState(initial = emptyList())
    val costs by remember(projectId) { vm.observeCosts(projectId) }.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf<ItemKind?>(null) }
    var editing by remember { mutableStateOf<CostEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("ต้นทุน & กำไร · ${project?.name.orEmpty()}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    ) { padding ->
        val p = project ?: return@Scaffold
        val quote = QuoteMath.totals(items.map { QuoteLine(it.kind, it.qty, it.unitPrice) }, QuoteOptions(p.overheadPercent, p.discount, p.vat, p.withholding))
        val profit = Contractor.profit(quote.beforeVat, costs.map { it.kind to it.amount })
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth().testTag("profit_card")) {
                    Column(Modifier.padding(16.dp)) {
                        Text("กำไรงานนี้", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        KeyValueRow("รายรับ (มูลค่างานก่อนภาษี)", Fmt.money(profit.revenue))
                        KeyValueRow("ค่าแรงจ่ายจริง", "-" + Fmt.money(profit.labor))
                        KeyValueRow("ค่าวัสดุจ่ายจริง", "-" + Fmt.money(profit.materials))
                        KeyValueRow("ค่าใช้จ่ายอื่น", "-" + Fmt.money(profit.other))
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        KeyValueRow(if (profit.profit >= 0) "กำไร" else "ขาดทุน", "${Fmt.money(profit.profit)} บาท (${Fmt.n(profit.marginPercent, 1)}%)", bold = true)
                        Text(
                            "ในใบเสนอราคาตั้งค่าแรงไว้ ${Fmt.money(quote.labor)} · ค่าวัสดุ ${Fmt.money(quote.materials)} บาท",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            listOf(
                ItemKind.LABOR to "ค่าแรงรายวัน",
                ItemKind.MATERIAL to "ค่าวัสดุที่ซื้อจริง",
                ItemKind.OTHER to "ค่าใช้จ่ายหน้างานอื่น ๆ"
            ).forEach { (kind, label) ->
                val rows = costs.filter { it.kind == kind }
                item(key = "h_$kind") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoldSection("$label · ${Fmt.money(rows.sumOf { it.amount })}", Modifier.weight(1f))
                        TextButton(onClick = { adding = kind }, modifier = Modifier.testTag("add_cost_${kind.name}")) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("เพิ่ม") }
                    }
                }
                items(rows, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { editing = c }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                thaiDate.format(Date(c.date)) + (if (kind == ItemKind.LABOR) " · ${Fmt.n(c.qty)} แรง × ${Fmt.money(c.unitCost)}" else "") +
                                    (if (c.note.isNotBlank()) " · ${c.note}" else ""),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(Fmt.money(c.amount), style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
            item { Spacer(Modifier.size(32.dp)) }
        }
    }

    adding?.let { kind ->
        CostDialog(kind, null, onDismiss = { adding = null }) { c -> vm.addCost(c.copy(projectId = projectId)); adding = null }
    }
    editing?.let { c ->
        CostDialog(c.kind, c, onDismiss = { editing = null }, onDelete = { vm.deleteCost(c); editing = null }) { n -> vm.updateCost(n.copy(id = c.id, projectId = c.projectId)); editing = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CostDialog(kind: ItemKind, c: CostEntity?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (CostEntity) -> Unit) {
    val labor = kind == ItemKind.LABOR
    var title by rememberSaveable { mutableStateOf(c?.title ?: "") }
    var qty by rememberSaveable { mutableStateOf(c?.let { Fmt.n(it.qty).replace(",", "") } ?: "1") }
    var cost by rememberSaveable { mutableStateOf(c?.unitCost?.takeIf { it > 0 }?.let { Fmt.n(it).replace(",", "") } ?: "") }
    var note by rememberSaveable { mutableStateOf(c?.note ?: "") }
    var date by rememberSaveable { mutableStateOf(c?.date ?: System.currentTimeMillis()) }
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (labor) "ค่าแรงรายวัน" else if (kind == ItemKind.MATERIAL) "ค่าวัสดุที่ซื้อจริง" else "ค่าใช้จ่ายอื่น") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("วันที่ ${thaiDate.format(Date(date))}") }
                OutlinedTextField(title, { title = it.take(80) }, label = { Text(if (labor) "ชื่อช่าง / ทีม เช่น ช่างปูน 2 คน" else "รายการ เช่น ปูน 20 ถุง ร้าน ก.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("cost_title"))
                if (labor) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInput(qty, { qty = it }, "จำนวนแรง (คน × วัน)", "แรง", Modifier.weight(1f), tag = "cost_qty")
                        NumberInput(cost, { cost = it }, "ค่าแรงต่อแรง", "บาท", Modifier.weight(1f), tag = "cost_value")
                    }
                } else {
                    NumberInput(cost, { cost = it }, "จำนวนเงิน", "บาท", tag = "cost_value")
                }
                OutlinedTextField(note, { note = it.take(200) }, label = { Text("หมายเหตุ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val total = (if (labor) qty.toDoubleOrNull() ?: 0.0 else 1.0) * (cost.toDoubleOrNull() ?: 0.0)
                Text("รวม ${Fmt.money(total)} บาท", style = MaterialTheme.typography.titleSmall)
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error); Text("ลบรายการ", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(CostEntity(projectId = 0, date = date, kind = kind, title = title.ifBlank { if (labor) "ค่าแรง" else "รายการ" },
                    qty = if (labor) qty.toDoubleOrNull() ?: 0.0 else 1.0, unitCost = cost.toDoubleOrNull() ?: 0.0, note = note))
            }, enabled = (cost.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.testTag("cost_save")) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { date = it }; picking = false }) { Text("ตกลง") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("ยกเลิก") } }
        ) { DatePicker(state) }
    }
}

// ------------------------------------------------------------------ order list

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(projectId: Long, vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val project by remember(projectId) { vm.observeProject(projectId) }.collectAsState(initial = null)
    val items by remember(projectId) { vm.observeItems(projectId) }.collectAsState(initial = emptyList())
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var picked by remember { mutableStateOf<List<String>?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("รายการสั่งของ", maxLines = 1) }
            )
        }
    ) { padding ->
        val p = project ?: return@Scaffold
        val lines = Contractor.orderList(items.filter { it.kind == ItemKind.MATERIAL }.map { OrderLine(it.name, it.unit, it.qty, it.unitPrice) })
        val keys = lines.map { it.name + "|" + it.unit }
        val selected = picked ?: keys
        val chosen = lines.filterIndexed { i, _ -> keys[i] in selected }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("รวมวัสดุจากทุกการคำนวณในงาน “${p.name}” (ของชนิดเดียวกันรวมเป็นบรรทัดเดียว) เลือกรายการที่จะสั่ง แล้วส่งให้ร้านวัสดุ",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (lines.isEmpty()) item { Text("ยังไม่มีวัสดุในงานนี้", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp)) }
            items(lines.indices.toList(), key = { keys[it] }) { idx ->
                val l = lines[idx]
                val on = keys[idx] in selected
                Row(
                    Modifier.fillMaxWidth().clickable { picked = if (on) selected - keys[idx] else selected + keys[idx] }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(selected = on, onClick = { picked = if (on) selected - keys[idx] else selected + keys[idx] }, label = { Text(if (on) "สั่ง" else "ไม่สั่ง") })
                    Spacer(Modifier.width(10.dp))
                    Text(l.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("${Fmt.n(l.qty)} ${l.unit}", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            item {
                val est = chosen.sumOf { it.qty * it.unitPrice }
                if (est > 0) KeyValueRow("ค่าวัสดุโดยประมาณ (ตามราคาในงาน)", "${Fmt.money(est)} บาท", bold = true)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val text = vm.orderText(p, chosen, settings?.profile?.shopName.orEmpty())
                    Button(onClick = {
                        startShare(context, vm, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "ส่งรายการสั่งของ")
                    }, enabled = chosen.isNotEmpty(), modifier = Modifier.weight(1f).testTag("order_share")) {
                        Icon(Icons.AutoMirrored.Outlined.Send, null); Spacer(Modifier.width(6.dp)); Text("ส่ง LINE")
                    }
                    OutlinedButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("order", text))
                        vm.say("คัดลอกรายการแล้ว")
                    }, enabled = chosen.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("คัดลอก")
                    }
                }
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}
