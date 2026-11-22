package com.sek789102.changkhamnuan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Gold = Color(0xFFFFC61F)
val GoldDeep = Color(0xFFFFA800)
val GoldBrush = Brush.verticalGradient(listOf(Color(0xFFFFD84A), Color(0xFFFFB300)))

@Composable
fun isDarkUi() = MaterialTheme.colorScheme.background.luminance() < 0.3f

/** Card background: soft top-lit dark panel (or warm white in light theme). */
@Composable
fun panelBrush(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.verticalGradient(listOf(c.surfaceContainerHigh, c.surfaceContainerLow))
}

@Composable
fun goldBorder(alpha: Float = 0.22f, width: Dp = 1.dp) = BorderStroke(width, Gold.copy(alpha = if (isDarkUi()) alpha else alpha + 0.25f))

/** Rounded dark panel with a thin gold rim — the base of every card in the app. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val inner: @Composable () -> Unit = { Box(Modifier.background(panelBrush()), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = Color.Transparent, border = goldBorder(), modifier = modifier, content = inner)
    } else {
        Surface(shape = shape, color = Color.Transparent, border = goldBorder(), modifier = modifier, content = inner)
    }
}

/** Yellow rounded square with a dark icon. */
@Composable
fun GoldBadge(icon: ImageVector, size: Dp = 56.dp, radius: Dp = 16.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(GoldBrush),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color(0xFF1C1400), modifier = Modifier.size(size * 0.54f)) }
}

/** "ช่าง" + yellow "คำนวณ". */
@Composable
fun TwoTone(first: String, second: String, size: Int = 30) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(first) }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append(second) }
        },
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = size.sp, lineHeight = (size + 10).sp, fontWeight = FontWeight.Bold)
    )
}

/** Section label with a short yellow bar in front. */
@Composable
fun GoldSection(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(Gold))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Photo banner for the main tabs: picture on the right fading into the background, two-tone
 * title on the left, and a glowing yellow swoosh along the bottom edge.
 */
@Composable
fun ArtHeader(
    @DrawableRes image: Int,
    first: String,
    second: String,
    subtitle: String,
    icon: ImageVector,
    height: Dp = 196.dp,
    extra: (@Composable () -> Unit)? = null
) {
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(height)) {
        Image(
            painterResource(image), null,
            contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd,
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(0.6f)
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to bg, 0.36f to bg, 0.58f to bg.copy(alpha = 0.55f), 0.75f to Color.Transparent)
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val curve = Path().apply {
                moveTo(0f, h * 0.70f)
                cubicTo(w * 0.18f, h * 0.93f, w * 0.55f, h * 0.90f, w, h * 0.80f)
            }
            val fill = Path().apply { addPath(curve); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(fill, bg)
            drawPath(curve, Gold.copy(alpha = 0.22f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(curve, Gold, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoldBadge(icon, 58.dp, 17.dp)
                Spacer(Modifier.width(14.dp))
                TwoTone(first, second)
            }
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f))
            if (extra != null) { Spacer(Modifier.height(10.dp)); extra() }
        }
    }
}

/** Dark pill with a gold rim, used for short highlights. */
@Composable
fun GoldPill(icon: ImageVector, text: String, highlight: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f), border = goldBorder(0.45f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    append(text); append("  ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)) { append(highlight) }
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/** Picture that fades into the card colour on its right edge. */
@Composable
fun FadedThumb(@DrawableRes image: Int, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier) {
        Image(painterResource(image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.55f to Color.Transparent, 1f to base)))
    }
}

/** Floating rounded bottom bar with a glowing yellow pill on the selected tab. */
@Composable
fun FloatingNavBar(items: List<Triple<String, ImageVector, String>>, selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, c.background))).navigationBarsPadding()) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = c.surfaceContainer,
            border = goldBorder(0.14f),
            shadowElevation = 10.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEachIndexed { i, (label, icon, tag) ->
                    val on = i == selected
                    Surface(
                        onClick = { onSelect(i) }, color = Color.Transparent, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).testTag(tag)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(
                                Modifier.width(64.dp).height(34.dp).clip(RoundedCornerShape(50))
                                    .then(if (on) Modifier.background(GoldBrush) else Modifier),
                                contentAlignment = Alignment.Center
                            ) { Icon(icon, null, tint = if (on) Color(0xFF1C1400) else c.onSurface, modifier = Modifier.size(24.dp)) }
                            Text(
                                label, style = MaterialTheme.typography.labelMedium,
                                color = if (on) c.secondary else c.onSurface,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            Box(
                                Modifier.padding(top = 3.dp).width(26.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                                    .background(if (on) Gold else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Round "go" button on calculator cards. */
@Composable
fun GoArrow(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(34.dp).clip(CircleShape).background(Gold.copy(alpha = 0.22f)).border(1.dp, Gold.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp).wrapContentSize()) }
}

/** Soft yellow glow curving up from the bottom-right corner of a card. */
@Composable
fun CornerGlow(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.radialGradient(listOf(Gold.copy(alpha = 0.28f), Color.Transparent), center = Offset(w, h), radius = w * 0.75f))
        val p = Path().apply { moveTo(w * 0.42f, h); cubicTo(w * 0.70f, h * 0.92f, w * 0.92f, h * 0.70f, w, h * 0.42f) }
        drawPath(p, Gold.copy(alpha = 0.55f), style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** Lets a child ignore its parent's horizontal content padding (full-width banners in padded lists). */
fun Modifier.bleed(horizontal: Dp) = this.then(
    androidx.compose.ui.Modifier.layout { measurable, constraints ->
        val extra = (horizontal * 2).roundToPx()
        val p = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra))
        layout(constraints.maxWidth, p.height) { p.place(-extra / 2, 0) }
    }
)
