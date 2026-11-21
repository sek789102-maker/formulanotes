package com.sek789102.changkhamnuan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.changkhamnuan.BuildConfig
import com.sek789102.changkhamnuan.data.BusinessProfile
import com.sek789102.changkhamnuan.data.ThemeMode
import com.sek789102.changkhamnuan.domain.Catalog
import com.sek789102.changkhamnuan.domain.CatalogItem
import com.sek789102.changkhamnuan.domain.Fmt
import com.sek789102.changkhamnuan.domain.RateDef
import com.sek789102.changkhamnuan.domain.RateDefs
import com.sek789102.changkhamnuan.domain.Units
import com.sek789102.changkhamnuan.ui.AppViewModel
import com.sek789102.changkhamnuan.ui.components.ChoiceInput
import com.sek789102.changkhamnuan.ui.components.ConfirmDialog
import com.sek789102.changkhamnuan.ui.components.NumberInput
import com.sek789102.changkhamnuan.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- prices

@Composable
fun PricesScreen(padding: PaddingValues, vm: AppViewModel) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("price_list"), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("ราคาวัสดุ", style = MaterialTheme.typography.headlineSmall)
            Text(
                "ใส่ราคาจากร้านที่คุณซื้อจริง แอปจะใช้คำนวณค่าวัสดุและใส่ในใบเสนอราคาให้อัตโนมัติ",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(Catalog.items, key = { it.key }) { c ->
            val p = prices[c.key]
            ListItem(
                headlineContent = { Text(c.name) },
                supportingContent = { Text("ต่อ${c.unit}") },
                trailingContent = {
                    Text(
                        if (p != null) "${Fmt.money(p)} บาท" else "ใส่ราคา",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (p != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { editing = c }.testTag("price_${c.key}")
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
    editing?.let { c ->
        var v by rememberSaveable(c.key) { mutableStateOf(prices[c.key]?.let { Fmt.n(it).replace(",", "") } ?: "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(c.name) },
            text = { NumberInput(v, { v = it }, "ราคา", "บาท/${c.unit}", tag = "price_input") },
            confirmButton = { TextButton(onClick = { vm.setPrice(c.key, v.toDoubleOrNull()); editing = null }, modifier = Modifier.testTag("price_save")) { Text("บันทึก") } },
            dismissButton = {
                Row {
                    if (prices[c.key] != null) TextButton(onClick = { vm.setPrice(c.key, null); editing = null }) { Text("ลบราคา", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editing = null }) { Text("ยกเลิก") }
                }
            }
        )
    }
}

// ---------------------------------------------------------------- more

@Composable
fun MoreScreen(padding: PaddingValues, vm: AppViewModel, onProfile: () -> Unit, onRates: () -> Unit, onConverter: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { vm.exportBackup(it) } }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { vm.importBackup(it) } }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("เพิ่มเติม", style = MaterialTheme.typography.headlineSmall)
        NavRow(Icons.Outlined.Storefront, "ข้อมูลร้าน / ช่าง", if (s.profile.isEmpty) "ยังไม่ได้ตั้ง — แสดงบนใบเสนอราคา" else s.profile.shopName.ifBlank { s.profile.ownerName }, "nav_profile", onProfile)
        NavRow(Icons.Outlined.Tune, "อัตราการใช้วัสดุ", "ปรับสูตร เช่น อิฐต่อ ตร.ม. ปูนต่อถุง สีต่อแกลลอน", "nav_rates", onRates)
        NavRow(Icons.Outlined.SquareFoot, "แปลงหน่วย", "ตร.ม. ↔ ตร.วา · ไร่ · ฟุต · คิว", "nav_converter", onConverter)

        SectionTitle("ธีม")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM to "ตามระบบ", ThemeMode.LIGHT to "สว่าง", ThemeMode.DARK to "มืด").forEach { (m, l) ->
                FilterChip(selected = s.theme == m, onClick = { vm.setTheme(m) }, label = { Text(l) })
            }
        }

        SectionTitle("สำรองและกู้คืน")
        Text(
            "บันทึกงาน ราคา อัตรา และข้อมูลร้านทั้งหมดเป็นไฟล์เดียว เก็บไว้ใน Google Drive หรือส่งเข้า LINE ตัวเอง ใช้ย้ายเครื่องได้",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (s.lastBackupAt > 0) "สำรองล่าสุด " + SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("th-TH")).format(Date(s.lastBackupAt)) else "ยังไม่เคยสำรองข้อมูล",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            FilledTonalButton(onClick = {
                exporter.launch("changkhamnuan_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".json")
            }, modifier = Modifier.weight(1f).testTag("backup_export")) {
                Icon(Icons.Outlined.Backup, null); Spacer(Modifier.width(6.dp)); Text("สำรองข้อมูล")
            }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.weight(1f).testTag("backup_import")) {
                Icon(Icons.Outlined.Restore, null); Spacer(Modifier.width(6.dp)); Text("กู้คืน")
            }
        }

        SectionTitle("เกี่ยวกับ")
        Text("• ใช้งานออฟไลน์ทั้งหมด ไม่มีโฆษณา ไม่ต้องสมัครบัญชี ไม่ขอสิทธิ์ใดๆ ในเครื่อง", style = MaterialTheme.typography.bodyMedium)
        Text("• ปริมาณวัสดุเป็นค่าประมาณจากอัตรามาตรฐาน (เช่น ตารางราคากลางงานก่อสร้าง) ควรตรวจสอบและเผื่อตามหน้างาน งานโครงสร้างให้ใช้ตามแบบวิศวกร", style = MaterialTheme.typography.bodyMedium)
        Text("• ไฟล์สำรองข้อมูลไม่ได้เข้ารหัส ควรเก็บไว้ในที่ปลอดภัย", style = MaterialTheme.typography.bodyMedium)
        Text("เวอร์ชัน ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, sub: String, tag: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(sub) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick).testTag(tag)
    )
}

// ---------------------------------------------------------------- profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val start = settings?.profile ?: return
    var shop by rememberSaveable { mutableStateOf(start.shopName) }
    var owner by rememberSaveable { mutableStateOf(start.ownerName) }
    var phone by rememberSaveable { mutableStateOf(start.phone) }
    var line by rememberSaveable { mutableStateOf(start.lineId) }
    var address by rememberSaveable { mutableStateOf(start.address) }
    var tax by rememberSaveable { mutableStateOf(start.taxId) }
    var terms by rememberSaveable { mutableStateOf(start.terms) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("ข้อมูลร้าน / ช่าง") },
                actions = {
                    TextButton(onClick = {
                        vm.setProfile(BusinessProfile(shop.trim(), owner.trim(), phone.trim(), line.trim(), address.trim(), tax.trim(), terms.trim()))
                        onBack()
                    }, modifier = Modifier.testTag("profile_save")) { Text("บันทึก") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ข้อมูลนี้แสดงบนหัวใบเสนอราคา (ไม่แสดงชื่อแอป)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(shop, { shop = it.take(80) }, label = { Text("ชื่อร้าน / บริษัท") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_shop"))
            OutlinedTextField(owner, { owner = it.take(80) }, label = { Text("ชื่อผู้เสนอราคา") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it.take(30) }, label = { Text("เบอร์โทร") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(line, { line = it.take(40) }, label = { Text("LINE ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it.take(200) }, label = { Text("ที่อยู่") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tax, { tax = it.filter { c -> c.isDigit() }.take(13) }, label = { Text("เลขประจำตัวผู้เสียภาษี (ถ้ามี)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(terms, { terms = it.take(500) }, label = { Text("เงื่อนไขการชำระเงิน") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------- rates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val rates by vm.rates.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RateDef?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("อัตราการใช้วัสดุ") },
                actions = { TextButton(onClick = { confirmReset = true }) { Text("คืนค่าทั้งหมด") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                Text("ค่าเริ่มต้นมาจากตารางมาตรฐาน แตะเพื่อแก้ตามยี่ห้อวัสดุหรือหน้างานของคุณ", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RateDefs.groups.forEach { g ->
                item(key = "g_$g") { SectionTitle(g, Modifier.padding(top = 8.dp)) }
                items(RateDefs.all.filter { it.group == g }, key = { it.key }) { d ->
                    val custom = rates.isCustom(d.key)
                    ListItem(
                        headlineContent = { Text(d.label) },
                        supportingContent = { Text(if (custom) "ปรับเอง (ค่ามาตรฐาน ${Fmt.n(d.default, 3)})" else "ค่ามาตรฐาน") },
                        trailingContent = {
                            Text("${Fmt.n(rates[d.key], 3)} ${d.unit}", style = MaterialTheme.typography.titleSmall,
                                color = if (custom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                        },
                        modifier = Modifier.clickable { editing = d }.testTag("rate_${d.key}")
                    )
                }
            }
        }
    }
    editing?.let { d ->
        var v by rememberSaveable(d.key) { mutableStateOf(Fmt.n(rates[d.key], 4).replace(",", "")) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("${d.group}: ${d.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInput(v, { v = it }, d.label, d.unit, tag = "rate_input")
                    Text("ค่ามาตรฐาน: ${Fmt.n(d.default, 4)} ${d.unit}", style = MaterialTheme.typography.bodySmall)
                    Text("ที่มา: ${d.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val x = v.toDoubleOrNull()
                    vm.setRate(d.key, x?.takeIf { it > 0 && it != d.default })
                    editing = null
                }, modifier = Modifier.testTag("rate_save")) { Text("บันทึก") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.setRate(d.key, null); editing = null }) { Text("ใช้ค่ามาตรฐาน") }
                    TextButton(onClick = { editing = null }) { Text("ยกเลิก") }
                }
            }
        )
    }
    if (confirmReset) {
        ConfirmDialog("คืนค่ามาตรฐานทั้งหมด?", "อัตราที่ปรับเองทั้งหมดจะกลับเป็นค่ามาตรฐาน", "คืนค่า", onDismiss = { confirmReset = false }) { vm.resetRates() }
    }
}

// ---------------------------------------------------------------- converter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(onBack: () -> Unit) {
    var groupId by rememberSaveable { mutableStateOf("area") }
    val group = Units.groups.first { it.id == groupId }
    var fromId by rememberSaveable(groupId) { mutableStateOf(group.units[0].id) }
    var value by rememberSaveable { mutableStateOf("1") }
    val from = group.units.firstOrNull { it.id == fromId } ?: group.units[0]
    val v = value.toDoubleOrNull() ?: 0.0
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("แปลงหน่วย") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Units.groups.forEach { g -> FilterChip(selected = g.id == groupId, onClick = { groupId = g.id }, label = { Text(g.label) }) }
            }
            NumberInput(value, { value = it }, "จำนวน", "", tag = "conv_value")
            ChoiceInput("จากหน่วย", group.units.map { it.id to it.label }, from.id, { fromId = it }, tag = "conv_from")
            HorizontalDivider()
            group.units.filter { it.id != from.id }.forEach { u ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(u.label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.n(Units.convert(v, from, u), 4), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("conv_${u.id}"))
                }
            }
            if (groupId == "area") {
                HorizontalDivider()
                Text("= " + Units.thaiLand(Units.convert(v, from, group.units[0])), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
