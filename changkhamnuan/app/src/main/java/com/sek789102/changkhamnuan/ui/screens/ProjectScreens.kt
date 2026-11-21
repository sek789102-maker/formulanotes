package com.sek789102.changkhamnuan.ui.screens

import android.content.ActivityNotFoundException
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.changkhamnuan.data.ItemEntity
import com.sek789102.changkhamnuan.data.ProjectEntity
import com.sek789102.changkhamnuan.domain.Fmt
import com.sek789102.changkhamnuan.domain.ItemKind
import com.sek789102.changkhamnuan.domain.QuoteLine
import com.sek789102.changkhamnuan.domain.QuoteMath
import com.sek789102.changkhamnuan.domain.QuoteOptions
import com.sek789102.changkhamnuan.ui.AppViewModel
import com.sek789102.changkhamnuan.ui.components.ConfirmDialog
import com.sek789102.changkhamnuan.ui.components.KeyValueRow
import com.sek789102.changkhamnuan.ui.components.NumberInput
import com.sek789102.changkhamnuan.ui.components.SectionTitle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(padding: PaddingValues, vm: AppViewModel, onOpen: (Long) -> Unit) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val summaries by vm.summaries.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("th-TH")) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            Modifier.weight(1f).testTag("project_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("งาน / ใบเสนอราคา", style = MaterialTheme.typography.headlineSmall)
                Text("รวมรายการวัสดุและค่าแรงของแต่ละงาน แล้วส่งใบเสนอราคาให้ลูกค้า", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val list = projects
            if (list != null && list.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.WorkOutline, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("ยังไม่มีงาน", style = MaterialTheme.typography.titleMedium)
                        Text("คำนวณวัสดุแล้วกด “เพิ่มลงงาน” หรือสร้างงานใหม่", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(list.orEmpty(), key = { it.id }) { p ->
                val s = summaries[p.id]
                Card(
                    onClick = { onOpen(p.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().testTag("project_${p.id}")
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.customerName.isNotBlank()) Text(p.customerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text("${s?.count ?: 0} รายการ · แก้ไข ${fmt.format(Date(p.updatedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text("${Fmt.money(s?.total ?: 0.0)} บาท", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("งานใหม่") }, modifier = Modifier.testTag("new_project"))
        }
    }
    if (creating) {
        ProjectInfoDialog(null, onDismiss = { creating = false }) { name, customer, _, _ ->
            creating = false
            vm.createProject(name, customer) { onOpen(it) }
        }
    }
}

@Composable
fun ProjectInfoDialog(p: ProjectEntity?, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(p?.name ?: "") }
    var customer by rememberSaveable { mutableStateOf(p?.customerName ?: "") }
    var phone by rememberSaveable { mutableStateOf(p?.customerPhone ?: "") }
    var site by rememberSaveable { mutableStateOf(p?.siteAddress ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (p == null) "งานใหม่" else "ข้อมูลงาน") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("ชื่องาน เช่น ต่อเติมครัว") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("project_name"))
                OutlinedTextField(customer, { customer = it.take(80) }, label = { Text("ชื่อลูกค้า") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it.take(30) }, label = { Text("เบอร์ลูกค้า") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(site, { site = it.take(160) }, label = { Text("สถานที่ทำงาน") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.ifBlank { "งานใหม่" }, customer, phone, site) }, modifier = Modifier.testTag("project_save")) { Text("บันทึก") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(projectId: Long, vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit, onOpenProject: (Long) -> Unit) {
    val project by remember(projectId) { vm.observeProject(projectId) }.collectAsState(initial = null)
    val items by remember(projectId) { vm.observeItems(projectId) }.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var editInfo by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<ItemEntity?>(null) }
    var newItemKind by remember { mutableStateOf<ItemKind?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val p = project
    fun share(asImage: Boolean) {
        busy = true
        scope.launch {
            vm.quoteShareIntent(projectId, asImage)?.let { intent ->
                try {
                    context.startActivity(Intent.createChooser(intent, "ส่งใบเสนอราคา"))
                } catch (_: ActivityNotFoundException) {
                    vm.say("ไม่พบแอปที่ใช้ส่งไฟล์")
                }
            }
            busy = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text(p?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { editInfo = true }, modifier = Modifier.testTag("edit_project")) { Icon(Icons.Outlined.Edit, "แก้ข้อมูลงาน") }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "เพิ่มเติม") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("ใส่ราคาที่บันทึกไว้") }, leadingIcon = { Icon(Icons.Outlined.PriceCheck, null) }, onClick = { menu = false; vm.applySavedPrices(projectId) })
                        DropdownMenuItem(text = { Text("ทำสำเนางาน") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; vm.duplicateProject(projectId) { onOpenProject(it) } })
                        DropdownMenuItem(text = { Text("ลบงาน") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; confirmDelete = true })
                    }
                }
            )
        }
    ) { padding ->
        if (p == null) return@Scaffold
        val totals = QuoteMath.totals(items.map { QuoteLine(it.kind, it.qty, it.unitPrice) }, QuoteOptions(p.overheadPercent, p.discount, p.vat, p.withholding))
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                if (p.customerName.isNotBlank() || p.siteAddress.isNotBlank()) {
                    Text(listOf(p.customerName, p.customerPhone, p.siteAddress).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            listOf(ItemKind.MATERIAL to "ค่าวัสดุ", ItemKind.LABOR to "ค่าแรง", ItemKind.OTHER to "ค่าใช้จ่ายอื่น").forEach { (kind, label) ->
                val rows = items.filter { it.kind == kind }
                item(key = "h_$kind") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(label, Modifier.weight(1f))
                        TextButton(onClick = { newItemKind = kind }, modifier = Modifier.testTag("add_${kind.name}")) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text("เพิ่ม")
                        }
                    }
                }
                items(rows, key = { it.id }) { it2 ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editItem = it2 }.padding(vertical = 6.dp).testTag("item_${it2.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(it2.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${Fmt.n(it2.qty)} ${it2.unit} × ${if (it2.unitPrice > 0) Fmt.money(it2.unitPrice) else "ยังไม่ใส่ราคา"}" +
                                    (if (it2.group.isNotBlank()) " · ${it2.group}" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it2.unitPrice > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Text(Fmt.money(it2.qty * it2.unitPrice), style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
            item {
                SectionTitle("ตั้งค่าใบเสนอราคา")
                QuoteOptionsEditor(p) { vm.updateProject(it) }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("totals")) {
                    Column(Modifier.padding(16.dp)) {
                        KeyValueRow("ค่าวัสดุ", Fmt.money(totals.materials))
                        KeyValueRow("ค่าแรง", Fmt.money(totals.labor))
                        if (totals.other > 0) KeyValueRow("ค่าใช้จ่ายอื่น", Fmt.money(totals.other))
                        if (totals.overhead > 0) KeyValueRow("ค่าดำเนินการ ${Fmt.n(p.overheadPercent)}%", Fmt.money(totals.overhead))
                        if (totals.discount > 0) KeyValueRow("ส่วนลด", "-" + Fmt.money(totals.discount))
                        if (p.vat) KeyValueRow("ภาษีมูลค่าเพิ่ม 7%", Fmt.money(totals.vat))
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        KeyValueRow("ยอดรวมทั้งสิ้น (บาท)", Fmt.money(totals.total), bold = true)
                        Text(QuoteMath.bahtText(totals.total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (p.withholding) KeyValueRow("หัก ณ ที่จ่าย 3% → ยอดรับจริง", Fmt.money(totals.toPay))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { share(asImage = true) }, enabled = !busy && items.isNotEmpty(), modifier = Modifier.weight(1f).testTag("share_image")) {
                        Icon(Icons.Outlined.Image, null); Spacer(Modifier.width(6.dp)); Text("ส่งเป็นรูป")
                    }
                    OutlinedButton(onClick = { share(asImage = false) }, enabled = !busy && items.isNotEmpty(), modifier = Modifier.weight(1f).testTag("share_pdf")) {
                        Icon(Icons.Outlined.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("ส่ง PDF")
                    }
                }
                Text("ใบเสนอราคาแสดงชื่อร้านของคุณ (ตั้งที่ “เพิ่มเติม → ข้อมูลร้าน”)", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))
            }
        }
    }

    if (editInfo && p != null) {
        ProjectInfoDialog(p, onDismiss = { editInfo = false }) { name, customer, phone, site ->
            editInfo = false
            vm.updateProject(p.copy(name = name, customerName = customer, customerPhone = phone, siteAddress = site))
        }
    }
    editItem?.let { item ->
        ItemDialog(item.kind, item, onDismiss = { editItem = null }, onDelete = { vm.deleteItem(item); editItem = null }) { name, qty, unit, price ->
            vm.updateItem(item.copy(name = name, qty = qty, unit = unit, unitPrice = price))
            editItem = null
        }
    }
    newItemKind?.let { kind ->
        ItemDialog(kind, null, onDismiss = { newItemKind = null }, onDelete = null) { name, qty, unit, price ->
            vm.addItem(projectId, kind, name, qty, unit, price)
            newItemKind = null
        }
    }
    if (confirmDelete && p != null) {
        ConfirmDialog("ลบงานนี้?", "รายการทั้งหมดในงาน “${p.name}” จะถูกลบ กู้คืนไม่ได้", "ลบ", onDismiss = { confirmDelete = false }) {
            vm.deleteProject(p.id)
            onBack()
        }
    }
}

@Composable
private fun QuoteOptionsEditor(p: ProjectEntity, onChange: (ProjectEntity) -> Unit) {
    var overhead by remember(p.id) { mutableStateOf(if (p.overheadPercent == 0.0) "" else Fmt.n(p.overheadPercent).replace(",", "")) }
    var discount by remember(p.id) { mutableStateOf(if (p.discount == 0.0) "" else Fmt.n(p.discount).replace(",", "")) }
    var valid by remember(p.id) { mutableStateOf(p.validDays.toString()) }
    var note by remember(p.id) { mutableStateOf(p.note) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberInput(overhead, { overhead = it; onChange(p.copy(overheadPercent = it.toDoubleOrNull() ?: 0.0)) }, "ค่าดำเนินการ/กำไร", "%", Modifier.weight(1f), tag = "overhead")
            NumberInput(discount, { discount = it; onChange(p.copy(discount = it.toDoubleOrNull() ?: 0.0)) }, "ส่วนลด", "บาท", Modifier.weight(1f), tag = "discount")
        }
        NumberInput(valid, { valid = it; onChange(p.copy(validDays = it.toDoubleOrNull()?.toInt()?.coerceIn(1, 365) ?: 30)) }, "ยืนราคา", "วัน")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("คิดภาษีมูลค่าเพิ่ม 7%", Modifier.weight(1f))
            Switch(checked = p.vat, onCheckedChange = { onChange(p.copy(vat = it)) }, modifier = Modifier.testTag("vat_switch"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("แสดงหัก ณ ที่จ่าย 3%")
                Text("สำหรับลูกค้านิติบุคคล", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = p.withholding, onCheckedChange = { onChange(p.copy(withholding = it)) })
        }
        OutlinedTextField(note, { note = it.take(500); onChange(p.copy(note = note)) }, label = { Text("หมายเหตุบนใบเสนอราคา") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ItemDialog(
    kind: ItemKind,
    item: ItemEntity?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, Double, String, Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(item?.name ?: "") }
    var qty by rememberSaveable { mutableStateOf(item?.qty?.let { Fmt.n(it).replace(",", "") } ?: "1") }
    var unit by rememberSaveable { mutableStateOf(item?.unit ?: if (kind == ItemKind.LABOR) "งาน" else "") }
    var price by rememberSaveable { mutableStateOf(item?.unitPrice?.takeIf { it > 0 }?.let { Fmt.n(it).replace(",", "") } ?: "") }
    val title = when (kind) { ItemKind.MATERIAL -> "วัสดุ"; ItemKind.LABOR -> "ค่าแรง"; ItemKind.OTHER -> "ค่าใช้จ่ายอื่น" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "เพิ่ม$title" else "แก้ไข$title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(120) }, label = { Text("รายการ") }, modifier = Modifier.fillMaxWidth().testTag("item_name"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInput(qty, { qty = it }, "จำนวน", "", Modifier.weight(1f), tag = "item_qty")
                    OutlinedTextField(unit, { unit = it.take(20) }, label = { Text("หน่วย") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                NumberInput(price, { price = it }, "ราคาต่อหน่วย", "บาท", tag = "item_price")
                val amount = (qty.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)
                Text("รวม ${Fmt.money(amount)} บาท", style = MaterialTheme.typography.titleSmall)
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error); Text("ลบรายการ", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.ifBlank { title }, qty.toDoubleOrNull() ?: 0.0, unit, price.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.testTag("item_save")
            ) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}

