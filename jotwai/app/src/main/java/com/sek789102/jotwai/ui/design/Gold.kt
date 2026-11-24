package com.sek789102.jotwai.ui.design

import com.sek789102.jotwai.util.t
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Black-and-gold look: dark page with soft gold waves, glassy cards with a thin gold edge. */

val Gold = Color(0xFFFFC21A)
val GoldDeep = Color(0xFFE59A00)
val GoldBrush = Brush.linearGradient(listOf(Color(0xFFFFD84D), Gold, GoldDeep))

/** Gold for text: deeper on the light theme so it stays readable. */
@Composable
fun goldText(): Color = if (isDarkUi()) Gold else Color(0xFFA86F00)

@Composable
fun isDarkUi(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.2f

/** Page background with flowing gold waves at the top and the bottom. */
@Composable
fun GoldBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = isDarkUi()
    val bg = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(bg)) {
      androidx.compose.runtime.CompositionLocalProvider(
          androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground
      ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val a = if (dark) 1f else 0.55f
            drawCircle(
                Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f * a), Color.Transparent), center = Offset(w * 0.2f, -h * 0.02f), radius = w * 0.9f),
                radius = w * 0.9f,
                center = Offset(w * 0.2f, -h * 0.02f)
            )
            fun wave(y0: Float, amp: Float, up: Boolean): Path = Path().apply {
                moveTo(0f, h * y0)
                cubicTo(w * 0.3f, h * (y0 - amp), w * 0.6f, h * (y0 + amp), w, h * (y0 - amp * 0.4f))
                if (up) { lineTo(w, 0f); lineTo(0f, 0f) } else { lineTo(w, h); lineTo(0f, h) }
                close()
            }
            drawPath(wave(0.13f, 0.06f, true), Brush.verticalGradient(listOf(GoldDeep.copy(alpha = 0.10f * a), Gold.copy(alpha = 0.04f * a))))
            drawPath(wave(0.10f, 0.05f, true), color = Gold.copy(alpha = 0.55f * a), style = Stroke(width = 2.5f))
            drawPath(wave(0.88f, 0.05f, false), Brush.verticalGradient(listOf(Gold.copy(alpha = 0.10f * a), GoldDeep.copy(alpha = 0.18f * a))))
            drawPath(wave(0.90f, 0.04f, false), color = Gold.copy(alpha = 0.45f * a), style = Stroke(width = 2f))
        }
        content()
      }
    }
}

/** Card with a translucent warm fill and a thin gold edge. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    accentLeft: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isDarkUi()
    val shape = RoundedCornerShape(radius)
    val fill = if (dark) Brush.linearGradient(listOf(Color(0xFF2E2210), Color(0xFF1E150A)))
    else Brush.linearGradient(listOf(Color.White, Color(0xFFFFF3D6)))
    Column(
        modifier
            .shadow(if (dark) 0.dp else 3.dp, shape, ambientColor = GoldDeep, spotColor = GoldDeep)
            .clip(shape)
            .background(fill)
            .border(1.dp, Brush.linearGradient(listOf(Gold.copy(alpha = if (dark) 0.55f else 0.7f), Gold.copy(alpha = 0.12f))), shape)
            .then(if (accentLeft) Modifier.drawLeftAccent() else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) { content() }
    }
}

private fun Modifier.drawLeftAccent(): Modifier = this.then(
    Modifier.background(Brush.horizontalGradient(0f to Gold, 0.018f to Gold, 0.019f to Color.Transparent))
)

/** Gold rounded square with a dark icon (screen headers, settings rows). */
@Composable
fun GoldTile(icon: ImageVector, size: Dp = 56.dp, glass: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier.size(size)
            .shadow(8.dp, shape, ambientColor = Gold, spotColor = Gold)
            .clip(shape)
            .background(
                when {
                    !glass -> GoldBrush
                    isDarkUi() -> Brush.linearGradient(listOf(Color(0xFF5A3F08), Color(0xFF2A1D06)))
                    else -> Brush.linearGradient(listOf(Color(0xFFFFF6DA), Color(0xFFFFE39A)))
                }
            )
            .border(1.dp, Gold.copy(alpha = 0.7f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (glass) (if (isDarkUi()) Gold else GoldDeep) else Color(0xFF1A1206), modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun ArtImage(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(res), null, modifier.size(size), contentScale = ContentScale.Fit)
}

/** Big header used at the top of each tab. */
@Composable
fun TabHeader(
    title: String,
    subtitle: String? = null,
    leading: @Composable () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

/** Search field look-alike with a gold edge. */
@Composable
fun GoldSearchBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = isDarkUi()
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier.fillMaxWidth().height(58.dp).clip(shape)
            .background(if (dark) Color(0xFF231A0D).copy(alpha = 0.85f) else Color.White)
            .border(1.dp, Gold.copy(alpha = 0.45f), shape)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

data class TabItem(val icon: ImageVector, val selectedIcon: ImageVector, val label: String, val tag: String)

/** Floating bottom bar with a gold outline; the selected tab is gold with an underline. */
@Composable
fun GoldBottomBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit) {
    val dark = isDarkUi()
    val shape = RoundedCornerShape(34.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(78.dp).clip(shape)
                .background(if (dark) Brush.verticalGradient(listOf(Color(0xFF2A1F0E), Color(0xFF140E06))) else Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF2D2))))
                .border(1.5.dp, Brush.horizontalGradient(listOf(Gold, GoldDeep.copy(alpha = 0.5f), Gold)), shape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, it ->
                val on = i == selected
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable { onSelect(i) }.testTag(it.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(if (on) it.selectedIcon else it.icon, null, tint = if (on) Gold else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                    Text(
                        it.label,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) (if (dark) Gold else GoldDeep) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(if (on) 64.dp else 0.dp).height(4.dp).clip(CircleShape).background(Gold))
                }
            }
        }
    }
}

/** Round gold "+" button. */
@Composable
fun GoldFab(onClick: () -> Unit, modifier: Modifier = Modifier, description: String = t("เพิ่ม", "Add")) {
    Box(
        modifier.size(68.dp)
            .shadow(14.dp, CircleShape, ambientColor = Gold, spotColor = Gold)
            .clip(CircleShape)
            .background(GoldBrush)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Add, description, tint = Color(0xFF1A1206), modifier = Modifier.size(38.dp))
    }
}

/** Gold pill chip used for filters. */
@Composable
fun GoldChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = isDarkUi()
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier.height(50.dp).clip(shape)
            .background(if (selected) GoldBrush else Brush.linearGradient(listOf(if (dark) Color(0xFF2A2012) else Color.White, if (dark) Color(0xFF1C150B) else Color(0xFFFFF4DA))))
            .border(1.dp, Gold.copy(alpha = if (selected) 0.9f else 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFF1A1206) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (selected) Color(0xFF1A1206) else MaterialTheme.colorScheme.onSurface)
    }
}

/** Section title with a small gold icon. */
@Composable
fun SectionTitle(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        trailing()
    }
}
