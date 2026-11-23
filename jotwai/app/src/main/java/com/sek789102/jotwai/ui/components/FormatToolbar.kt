package com.sek789102.jotwai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Subscript
import androidx.compose.material.icons.outlined.Superscript
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.jotwai.domain.RichText
import com.sek789102.jotwai.domain.RichText.Kind
import com.sek789102.jotwai.domain.TextEditing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Text colours offered first (the custom picker allows any colour). */
private val textColors = listOf(
    "000000", "424242", "757575", "BDBDBD", "FFFFFF",
    "D32F2F", "E53935", "F44336", "FF7043", "FB8C00",
    "FFB300", "FDD835", "C0CA33", "7CB342", "43A047",
    "00897B", "00ACC1", "039BE5", "1E88E5", "3949AB",
    "5E35B1", "8E24AA", "D81B60", "EC407A", "6D4C41"
)
private val highlightColors = listOf(
    "FFF176", "FFE082", "FFCC80", "FFAB91", "EF9A9A",
    "F48FB1", "CE93D8", "B39DDB", "9FA8DA", "90CAF9",
    "81D4FA", "80DEEA", "80CBC4", "A5D6A7", "C5E1A5",
    "E6EE9C", "BCAAA4", "EEEEEE", "B0BEC5", "FFEB3B"
)
private val sizePresets = listOf(10, 12, 14, 16, 18, 20, 24, 28, 32, 40, 48, 64, 80, 96)
private const val BASE_PT = 16f

private enum class Panel { NONE, TEXT_COLOR, HIGHLIGHT, SIZE, FONT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatToolbar(value: TextFieldValue, apply: ((TextFieldValue) -> TextFieldValue) -> Unit) {
    val active = remember(value.text, value.selection) { RichText.activeKinds(value) }
    val align = remember(value.text, value.selection) { RichText.alignAt(value) }
    val headingLevel = remember(value.text, value.selection) { headingOf(value) }
    val curEm = active[Kind.SIZE]?.toFloatOrNull() ?: 1f
    var panel by remember { mutableStateOf(Panel.NONE) }
    var headingMenu by remember { mutableStateOf(false) }
    var alignMenu by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    @Composable
    fun b(icon: ImageVector, label: String, tag: String, on: Boolean = false, tint: Color? = null, t: () -> Unit) {
        Box(
            Modifier
                .padding(horizontal = 1.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (on) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                .clickable(onClickLabel = label) { t() }
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = tint ?: if (on) scheme.primary else scheme.onSurface)
        }
    }

    @Composable
    fun sep() = VerticalDivider(Modifier.height(28.dp).padding(horizontal = 4.dp), color = scheme.outlineVariant)

    Column {
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            b(Icons.Outlined.FormatBold, "ตัวหนา", "fmt_bold", Kind.BOLD in active) { apply { RichText.toggleMarker(it, "**") } }
            b(Icons.Outlined.FormatItalic, "ตัวเอียง", "fmt_italic", Kind.ITALIC in active) { apply { RichText.toggleMarker(it, "*") } }
            b(Icons.Outlined.FormatUnderlined, "ขีดเส้นใต้", "fmt_underline", Kind.UNDERLINE in active) { apply { RichText.applyTag(it, "u", "") } }
            b(Icons.Outlined.FormatStrikethrough, "ขีดฆ่า", "fmt_strike", Kind.STRIKE in active) { apply { RichText.toggleMarker(it, "~~") } }
            sep()
            val tc = active[Kind.COLOR]?.let { RichText.parseColor(it) }
            b(Icons.Outlined.FormatColorText, "สีตัวอักษร", "fmt_color", tc != null, tint = tc) { panel = if (panel == Panel.TEXT_COLOR) Panel.NONE else Panel.TEXT_COLOR }
            val hc = active[Kind.BG]?.let { RichText.parseColor(it) }
            b(Icons.Outlined.BorderColor, "สีไฮไลต์", "fmt_mark", Kind.MARK in active || hc != null, tint = hc) { panel = if (panel == Panel.HIGHLIGHT) Panel.NONE else Panel.HIGHLIGHT }
            sep()
            b(Icons.Outlined.TextDecrease, "ลดขนาด", "fmt_smaller") { apply { RichText.applyTag(it, "z", stepSize(curEm, -1)) } }
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (panel == Panel.SIZE || Kind.SIZE in active) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { panel = if (panel == Panel.SIZE) Panel.NONE else Panel.SIZE }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("fmt_size"),
                contentAlignment = Alignment.Center
            ) { Text("${(curEm * BASE_PT).toInt()}", fontWeight = FontWeight.Bold, color = scheme.onSurface) }
            b(Icons.Outlined.TextIncrease, "เพิ่มขนาด", "fmt_bigger") { apply { RichText.applyTag(it, "z", stepSize(curEm, +1)) } }
            b(Icons.Outlined.FontDownload, "ฟอนต์", "fmt_font", Kind.FONT in active) { panel = if (panel == Panel.FONT) Panel.NONE else Panel.FONT }
            sep()
            Box {
                b(Icons.Outlined.Title, "หัวข้อ", "fmt_heading", headingLevel > 0) { headingMenu = true }
                DropdownMenu(expanded = headingMenu, onDismissRequest = { headingMenu = false }) {
                    listOf(0 to "ข้อความปกติ", 1 to "หัวข้อใหญ่", 2 to "หัวข้อกลาง", 3 to "หัวข้อเล็ก").forEach { (lv, name) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    name,
                                    fontWeight = if (lv > 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = when (lv) { 1 -> 22.sp; 2 -> 19.sp; 3 -> 17.sp; else -> 15.sp }
                                )
                            },
                            trailingIcon = { if (lv == headingLevel) Icon(Icons.Outlined.Check, null) },
                            onClick = { headingMenu = false; apply { setHeading(it, lv) } }
                        )
                    }
                }
            }
            b(Icons.AutoMirrored.Outlined.FormatListBulleted, "รายการแบบจุด", "fmt_bullets") { apply { TextEditing.toggleLinePrefix(it, "- ") } }
            b(Icons.Outlined.FormatListNumbered, "รายการแบบตัวเลข", "fmt_numbers") { apply { TextEditing.toggleLinePrefix(it, "1. ") } }
            b(Icons.Outlined.CheckBoxOutlineBlank, "ช่องติ๊ก", "fmt_box") { apply { TextEditing.toggleLinePrefix(it, "[ ] ") } }
            b(Icons.Outlined.FormatQuote, "ข้อความอ้างอิง", "fmt_quote", Kind.QUOTE in active) { apply { TextEditing.toggleLinePrefix(it, "> ") } }
            sep()
            Box {
                b(alignIcon(align), "จัดแนว", "fmt_align", align != TextAlign.Start) { alignMenu = true }
                DropdownMenu(expanded = alignMenu, onDismissRequest = { alignMenu = false }) {
                    listOf(
                        TextAlign.Start to "ชิดซ้าย", TextAlign.Center to "กึ่งกลาง",
                        TextAlign.End to "ชิดขวา", TextAlign.Justify to "เต็มบรรทัด"
                    ).forEach { (a, name) ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(alignIcon(a), null) },
                            text = { Text(name) },
                            trailingIcon = { if (a == align) Icon(Icons.Outlined.Check, null) },
                            onClick = { alignMenu = false; apply { RichText.setAlign(it, a) } }
                        )
                    }
                }
            }
            b(Icons.AutoMirrored.Outlined.FormatIndentDecrease, "ลดย่อหน้า", "fmt_outdent") { apply { RichText.indent(it, -1) } }
            b(Icons.AutoMirrored.Outlined.FormatIndentIncrease, "เพิ่มย่อหน้า", "fmt_indent") { apply { RichText.indent(it, 1) } }
            sep()
            b(Icons.Outlined.Superscript, "ตัวยก", "fmt_sup", Kind.SUP in active) { apply { RichText.applyTag(it, "sup", "") } }
            b(Icons.Outlined.Subscript, "ตัวห้อย", "fmt_sub", Kind.SUB in active) { apply { RichText.applyTag(it, "sub", "") } }
            b(Icons.Outlined.Code, "โค้ด", "fmt_code", Kind.CODE in active) { apply { RichText.toggleMarker(it, "`") } }
            b(Icons.Outlined.HorizontalRule, "เส้นคั่น", "fmt_rule") { apply { RichText.insertDivider(it) } }
            b(Icons.Outlined.DateRange, "แทรกวันเวลา", "fmt_date") { apply { insertText(it, nowText()) } }
            b(Icons.Outlined.FormatClear, "ล้างรูปแบบ", "fmt_clear") { apply { RichText.clearFormatting(it) } }
        }
        when (panel) {
            Panel.TEXT_COLOR -> ColorPanel(
                colors = textColors,
                current = active[Kind.COLOR],
                defaultLabel = "สีปกติ",
                onPick = { hex -> apply { RichText.applyTag(it, "c", hex) } },
                onClose = { panel = Panel.NONE }
            )
            Panel.HIGHLIGHT -> ColorPanel(
                colors = highlightColors,
                current = active[Kind.BG],
                defaultLabel = "ไม่มีไฮไลต์",
                onPick = { hex ->
                    apply {
                        val cleared = RichText.removeKinds(it, setOf(Kind.MARK))
                        if (hex == null) RichText.applyTag(cleared, "h", null) else RichText.applyTag(cleared, "h", hex)
                    }
                },
                onClose = { panel = Panel.NONE }
            )
            Panel.SIZE -> SizePanel(
                currentEm = curEm,
                onPick = { em -> apply { RichText.applyTag(it, "z", em?.let(::emArg)) } },
                onClose = { panel = Panel.NONE }
            )
            Panel.FONT -> FontPanel(
                current = active[Kind.FONT],
                onPick = { f -> apply { RichText.applyTag(it, "f", f) } },
                onClose = { panel = Panel.NONE }
            )
            Panel.NONE -> Unit
        }
    }
}

/** A−/A+ move to the previous/next preset size (16 = normal, which removes the size code). */
private fun stepSize(curEm: Float, dir: Int): String? {
    val pt = curEm * BASE_PT
    val next = if (dir > 0) sizePresets.firstOrNull { it > pt + 0.5f } ?: sizePresets.last()
    else sizePresets.lastOrNull { it < pt - 0.5f } ?: sizePresets.first()
    return if (next == BASE_PT.toInt()) null else emArg(next / BASE_PT)
}

private fun emArg(em: Float): String = String.format(Locale.US, "%.3f", em).trimEnd('0').trimEnd('.')

private fun alignIcon(a: TextAlign): ImageVector = when (a) {
    TextAlign.Center -> Icons.Outlined.FormatAlignCenter
    TextAlign.End, TextAlign.Right -> Icons.Outlined.FormatAlignRight
    TextAlign.Justify -> Icons.Outlined.FormatAlignJustify
    else -> Icons.Outlined.FormatAlignLeft
}

private val headingLine = Regex("""^(?:⟦al:[a-z]⟧)?(#{1,3}) """)

private fun headingOf(v: TextFieldValue): Int {
    val t = v.text
    val i = v.selection.min
    val ls = if (i <= 0) 0 else t.lastIndexOf('\n', i - 1) + 1
    val le = t.indexOf('\n', ls).let { if (it < 0) t.length else it }
    return headingLine.find(t.substring(ls, le))?.groupValues?.get(1)?.length ?: 0
}

private fun setHeading(v: TextFieldValue, level: Int): TextFieldValue {
    val current = headingOf(v)
    var r = v
    if (current > 0) r = TextEditing.toggleLinePrefix(r, "#".repeat(current) + " ")
    if (level > 0 && level != current) r = TextEditing.toggleLinePrefix(r, "#".repeat(level) + " ")
    return r
}

private fun insertText(v: TextFieldValue, s: String): TextFieldValue {
    val t = v.text
    val text = t.substring(0, v.selection.min) + s + t.substring(v.selection.max)
    return TextFieldValue(text, androidx.compose.ui.text.TextRange(v.selection.min + s.length))
}

private fun nowText(): String {
    val th = Locale.Builder().setLanguage("th").setRegion("TH").build()
    val d = Date()
    val y = SimpleDateFormat("yyyy", Locale.US).format(d).toInt() + 543
    return SimpleDateFormat("d MMM", th).format(d) + " $y " + SimpleDateFormat("HH:mm", Locale.US).format(d)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPanel(colors: List<String>, current: String?, defaultLabel: String, onPick: (String?) -> Unit, onClose: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Default / none
            Box(
                Modifier.size(34.dp).clip(CircleShape).border(2.dp, scheme.outline, CircleShape).clickable { onPick(null); onClose() },
                contentAlignment = Alignment.Center
            ) { Text("✕", color = scheme.onSurfaceVariant) }
            colors.forEach { hex ->
                val c = RichText.parseColor(hex)!!
                val selected = current.equals(hex, ignoreCase = true)
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, CircleShape)
                        .clickable { onPick(hex) }
                        .testTag("color_$hex"),
                    contentAlignment = Alignment.Center
                ) { if (selected) Icon(Icons.Outlined.Check, null, tint = if (c.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp)) }
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    .clickable { custom = true }
                    .testTag("color_custom"),
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "✕ = $defaultLabel   ·   วงสีรุ้ง = เลือกสีเอง",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text("ปิด") }
        }
    }
    if (custom) {
        CustomColorDialog(
            initial = current?.let { RichText.parseColor(it) } ?: Color(0xFF1E88E5),
            onDismiss = { custom = false },
            onPick = { hex -> custom = false; onPick(hex) }
        )
    }
}

@Composable
private fun CustomColorDialog(initial: Color, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val hsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    val hex = String.format(Locale.US, "%06X", color.toArgb() and 0xFFFFFF)
    var hexField by remember(hex) { mutableStateOf(hex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เลือกสีเอง") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(color))
                Spacer(Modifier.height(12.dp))
                Text("เฉดสี", style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(
                        Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) })
                    )
                )
                Slider(value = h, onValueChange = { h = it }, valueRange = 0f..360f, modifier = Modifier.testTag("hue"))
                Text("ความสด", style = MaterialTheme.typography.labelMedium)
                Slider(value = s, onValueChange = { s = it })
                Text("ความสว่าง", style = MaterialTheme.typography.labelMedium)
                Slider(value = v, onValueChange = { v = it })
                OutlinedTextField(
                    value = hexField,
                    onValueChange = { raw ->
                        val clean = raw.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                        hexField = clean
                        RichText.parseColor(clean)?.let { c ->
                            val out = FloatArray(3)
                            android.graphics.Color.colorToHSV(c.toArgb(), out)
                            h = out[0]; s = out[1]; v = out[2]
                        }
                    },
                    prefix = { Text("#") },
                    singleLine = true,
                    label = { Text("รหัสสี") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(hex) }) { Text("ใช้สีนี้") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizePanel(currentEm: Float, onPick: (Float?) -> Unit, onClose: () -> Unit) {
    var pt by remember(currentEm) { mutableFloatStateOf(currentEm * BASE_PT) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sizePresets.forEach { p ->
                FilterChip(
                    selected = (currentEm * BASE_PT).toInt() == p,
                    onClick = { onPick(if (p == BASE_PT.toInt()) null else p / BASE_PT) },
                    label = { Text(if (p == BASE_PT.toInt()) "$p (ปกติ)" else "$p") },
                    modifier = Modifier.testTag("size_$p")
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ขนาด ${pt.toInt()}", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = pt,
                onValueChange = { pt = it },
                onValueChangeFinished = { onPick(if (pt.toInt() == BASE_PT.toInt()) null else pt.toInt() / BASE_PT) },
                valueRange = 6f..160f,
                modifier = Modifier.weight(1f).testTag("size_slider")
            )
            TextButton(onClick = onClose) { Text("ปิด") }
        }
    }
}

@Composable
private fun FontPanel(current: String?, onPick: (String?) -> Unit, onClose: () -> Unit) {
    val fonts = listOf(null to "ค่าเริ่มต้น", "sans" to "ไม่มีหัว", "serif" to "มีหัว", "mono" to "พิมพ์ดีด", "hand" to "ลายมือ")
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fonts.forEach { (key, name) ->
                val selected = current == key || (key == null && current == null)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { onPick(key) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("font_${key ?: "default"}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("กขค Aa", fontFamily = key?.let { RichText.fontOf(it) }, fontSize = 18.sp)
                    Text(name, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text("ปิด") } }
    }
}
