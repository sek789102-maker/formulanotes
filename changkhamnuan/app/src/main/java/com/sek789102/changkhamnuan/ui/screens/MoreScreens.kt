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
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sek789102.changkhamnuan.ui.components.yellowBarColors
import com.sek789102.changkhamnuan.ui.components.ChoiceInput
import com.sek789102.changkhamnuan.ui.components.ConfirmDialog
import com.sek789102.changkhamnuan.ui.components.NumberInput
import com.sek789102.changkhamnuan.ui.components.SectionTitle
import com.sek789102.changkhamnuan.R
import com.sek789102.changkhamnuan.ui.components.ArtHeader
import com.sek789102.changkhamnuan.ui.components.FadedThumb
import com.sek789102.changkhamnuan.ui.components.Gold
import com.sek789102.changkhamnuan.ui.components.GoldBadge
import com.sek789102.changkhamnuan.ui.components.GoldBrush
import com.sek789102.changkhamnuan.ui.components.GoldSection
import com.sek789102.changkhamnuan.ui.components.Panel
import com.sek789102.changkhamnuan.ui.components.bleed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- prices

fun materialArt(key: String): Int = when {
    key == "brick_mon" -> R.drawable.art_m_brick
    key == "brick_aac" -> R.drawable.art_m_aac
    key == "brick_block" -> R.drawable.art_m_block
    key == "cement_bag" -> R.drawable.art_m_cement_bag
    key == "plaster_bag" -> R.drawable.art_m_powder
    key == "mortar_aac" || key == "tile_adhesive" || key == "grout" -> R.drawable.art_m_mortar
    key == "sand" || key == "fill_sand" -> R.drawable.art_m_sand
    key == "stone" -> R.drawable.art_m_gravel
    key == "ready_mix" || key == "lean_concrete" -> R.drawable.art_c_concrete
    key == "plywood" -> R.drawable.art_c_footing
    key.startsWith("rebar") -> R.drawable.art_m_rebar
    key == "tile_box" -> R.drawable.art_c_tiles
    key.startsWith("paint") || key.startsWith("primer") -> R.drawable.art_c_paint
    key.startsWith("floor_") || key == "skirting" -> R.drawable.art_c_flooring
    key.startsWith("metal_") -> R.drawable.art_m_metal_sheet
    key.startsWith("roof_") -> R.drawable.art_c_roof
    key == "purlin" -> R.drawable.art_m_steel
    key == "c_stud" || key == "u_track" -> R.drawable.art_c_drywall
    key in setOf("gypsum_board", "c_line", "wall_angle", "hanger", "ceiling_tile", "main_tee", "cross_tee_120", "cross_tee_60") -> R.drawable.art_c_ceiling
    key.startsWith("pvc_") || key.startsWith("elbow_") || key.startsWith("tee_") || key.startsWith("socket_") || key == "pipe_clip" -> R.drawable.art_c_pipes
    key == "fill_soil" || key == "laterite" -> R.drawable.art_c_fill
    key == "haul_soil" -> R.drawable.art_c_excavation
    key == "wire_mesh" || key.startsWith("paver_") -> R.drawable.art_c_yard
    key.startsWith("fence_") -> R.drawable.art_m_fence
    else -> R.drawable.art_m_cement_bag
}

@Composable
fun PricesScreen(padding: PaddingValues, vm: AppViewModel) {
    val prices by vm.prices.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    var confirmDeletePrice by remember { mutableStateOf<CatalogItem?>(null) }
    confirmDeletePrice?.let { c ->
        ConfirmDialog("ลบราคา ${c.name}?", "งานที่เพิ่มไปแล้วไม่เปลี่ยน แต่การคำนวณครั้งต่อไปจะไม่มีราคานี้", "ลบราคา",
            onDismiss = { confirmDeletePrice = null }) { vm.setPrice(c.key, null) }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).testTag("price_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(Modifier.bleed(16.dp)) {
                ArtHeader(R.drawable.art_header_prices, "ราคา", "วัสดุ", "ราคาจากร้านของคุณ ใช้กับทุกงาน", Icons.Outlined.Layers)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), radius = 18.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
                        Text("i", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1C1400), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "ใส่ราคาวัสดุที่คุณซื้อจริงครั้งเดียว แล้วแอปจะคิดค่าวัสดุและเติมราคาในใบเสนอราคาให้ทุกงาน",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Catalog.items.groupBy { it.group }.forEach { (group, list) ->
        if (group.isNotBlank()) item(key = "g_$group") { GoldSection(group) }
        items(list, key = { it.key }) { c ->
            val p = prices[c.key]
            Panel(Modifier.fillMaxWidth().height(86.dp).testTag("price_${c.key}"), radius = 18.dp, onClick = { editing = c }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FadedThumb(materialArt(c.key), Modifier.width(108.dp).fillMaxHeight())
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("ต่อ${c.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(50), color = Color.Transparent,
                        border = BorderStroke(1.5.dp, if (p != null) Gold else Gold.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (p != null) "${Fmt.money(p)} ฿" else "ใส่ราคา",
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        }
    }
    editing?.let { c ->
        var v by rememberSaveable(c.key) { mutableStateOf(prices[c.key]?.let { Fmt.n(it).replace(",", "") } ?: "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(c.name) },
            text = { NumberInput(v, { v = it }, "ราคา", "บาท/${c.unit}", tag = "price_input") },
            confirmButton = {
                TextButton(
                    onClick = { v.toDoubleOrNull()?.takeIf { it > 0 }?.let { vm.setPrice(c.key, it) }; editing = null },
                    enabled = (v.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.testTag("price_save")
                ) { Text("บันทึก") }
            },
            dismissButton = {
                Row {
                    if (prices[c.key] != null) TextButton(onClick = { confirmDeletePrice = c; editing = null }) { Text("ลบราคา", color = MaterialTheme.colorScheme.error) }
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
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        ArtHeader(R.drawable.art_header_more, "เพิ่ม", "เติม", "ข้อมูลร้าน · สูตร · สำรองข้อมูล", Icons.Outlined.Settings)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NavCard(Icons.Outlined.Person, "ข้อมูลร้าน / ช่าง", if (s.profile.isEmpty) "ยังไม่ได้ตั้งค่า — แสดงบนใบเสนอราคา" else s.profile.shopName.ifBlank { s.profile.ownerName }, "nav_profile", onProfile)
            NavCard(Icons.Outlined.Tune, "อัตราการใช้วัสดุ", "ปรับสูตร เช่น อิฐต่อ ตร.ม. ปูนต่อถุง สีต่อแกลลอน", "nav_rates", onRates)
            NavCard(Icons.Outlined.SquareFoot, "แปลงหน่วย", "ตร.ม. ↔ ตร.วา · ไร่ · ฟุต · คิว", "nav_converter", onConverter)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            GoldSection("ธีม")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(ThemeMode.SYSTEM, "ตามระบบ", Icons.Outlined.BrightnessMedium),
                    Triple(ThemeMode.LIGHT, "สว่าง", Icons.Outlined.LightMode),
                    Triple(ThemeMode.DARK, "มืด", Icons.Outlined.DarkMode)
                ).forEach { (m, l, ic) -> ThemeChip(l, ic, s.theme == m, Modifier.weight(1f).testTag("theme_${m.name.lowercase()}")) { vm.setTheme(m) } }
            }

            GoldSection("สำรองและกู้คืน")
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudUpload, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "บันทึกงาน ราคา อัตรา และข้อมูลร้านทั้งหมดเป็นไฟล์เดียว เก็บไว้ใน Google Drive หรือส่งเข้า LINE ตัวเอง ใช้ย้ายเครื่องได้",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (s.lastBackupAt > 0) "สำรองล่าสุด " + SimpleDateFormat("d MMM yyyy HH:mm", Locale.forLanguageTag("th-TH")).format(Date(s.lastBackupAt)) else "ยังไม่เคยสำรองข้อมูล",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                        Button(
                            onClick = { exporter.launch("changkhamnuan_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".json") },
                            shape = RoundedCornerShape(50), modifier = Modifier.weight(1f).height(50.dp).testTag("backup_export")
                        ) { Icon(Icons.Outlined.Backup, null); Spacer(Modifier.width(6.dp)); Text("สำรองข้อมูล", fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            shape = RoundedCornerShape(50), border = BorderStroke(1.5.dp, Gold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("backup_import")
                        ) { Icon(Icons.Outlined.History, null); Spacer(Modifier.width(6.dp)); Text("กู้คืน", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            GoldSection("เกี่ยวกับ")
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(GoldBrush), contentAlignment = Alignment.Center) {
                        Text("i", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1C1400), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• ใช้งานออฟไลน์ทั้งหมด ไม่มีโฆษณา ไม่ต้องสมัครบัญชี ไม่ขอสิทธิ์ใดๆ ในเครื่อง", style = MaterialTheme.typography.bodyMedium)
                        Text("• ปริมาณวัสดุเป็นค่าประมาณจากอัตรามาตรฐาน (เช่น ตารางราคากลางงานก่อสร้าง) ควรตรวจสอบและเผื่อตามหน้างาน งานโครงสร้างให้ใช้ตามแบบวิศวกร", style = MaterialTheme.typography.bodyMedium)
                        Text("• ไฟล์สำรองข้อมูลไม่ได้เข้ารหัส ควรเก็บไว้ในที่ปลอดภัย", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("เวอร์ชัน ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
        }
    }
}

@Composable
private fun NavCard(icon: ImageVector, title: String, sub: String, tag: String, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth().testTag(tag), onClick = onClick) {
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.10f), Color.Transparent))))
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GoldBadge(icon, 56.dp, 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ThemeChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        Surface(
            onClick = onClick, shape = RoundedCornerShape(50),
            color = if (selected) Gold.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) BorderStroke(2.dp, Gold) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 6.dp).height(50.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (selected || icon == Icons.Outlined.LightMode) Gold else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(Gold),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Check, null, tint = Color(0xFF1C1400), modifier = Modifier.size(15.dp)) }
        }
    }
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
    // Leaving the screen (back arrow, system back or the save button) always keeps what was typed.
    fun saveAndClose() {
        vm.setProfile(BusinessProfile(shop.trim(), owner.trim(), phone.trim(), line.trim(), address.trim(), tax.trim(), terms.trim()))
        onBack()
    }
    androidx.activity.compose.BackHandler { saveAndClose() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = { saveAndClose() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "บันทึกและกลับ") } },
                title = { Text("ข้อมูลร้าน / ช่าง") },
                actions = {
                    TextButton(
                        onClick = { saveAndClose() },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.testTag("profile_save")
                    ) { Text("บันทึก") }
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
            androidx.compose.material3.Button(onClick = { saveAndClose() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("profile_save_bottom")) {
                Text("บันทึกข้อมูลร้าน")
            }
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
                colors = yellowBarColors(),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "กลับ") } },
                title = { Text("อัตราการใช้วัสดุ") },
                actions = {
                    TextButton(
                        onClick = { confirmReset = true },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("คืนค่าทั้งหมด") }
                }
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
                colors = yellowBarColors(),
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
                Text("= " + Units.thaiLand(Units.convert(v, from, group.units[0])), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
