package com.sek789102.jotwai.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sek789102.jotwai.R
import com.sek789102.jotwai.util.NoteImageStorage

private class Stroke2(val points: List<Offset>, val color: Color, val width: Float)

private val penColors = listOf(
    Color(0xFF1F1F1F), Color(0xFFD93025), Color(0xFFE8710A), Color(0xFF188038), Color(0xFF1967D2), Color(0xFF9334E6)
)
private val penWidths = listOf(6f, 12f, 24f)

/** Simple sketch pad. The drawing is saved as an image and added to the note. */
@Composable
fun DrawingDialog(onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var color by remember { mutableStateOf(penColors[0]) }
    var widthIndex by remember { mutableIntStateOf(1) }
    var eraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val strokeWidth = penWidths[widthIndex] * if (eraser) 2.5f else 1f
    val strokeColor = if (eraser) Color.White else color

    fun save() {
        if (strokes.isEmpty() || canvasSize.width <= 0) {
            onDismiss()
            return
        }
        val bmp = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        strokes.forEach { s ->
            paint.color = s.color.toArgb()
            paint.strokeWidth = s.width
            if (s.points.size == 1) {
                paint.style = android.graphics.Paint.Style.FILL
                c.drawCircle(s.points[0].x, s.points[0].y, s.width / 2, paint)
                paint.style = android.graphics.Paint.Style.STROKE
            } else {
                val p = android.graphics.Path()
                s.points.forEachIndexed { i, o -> if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
                c.drawPath(p, paint)
            }
        }
        val name = NoteImageStorage.saveDrawing(context, bmp)
        bmp.recycle()
        if (name != null) onSaved(name)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, stringResource(R.string.action_cancel)) }
                    Text(stringResource(R.string.drawing_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = strokes.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.action_undo))
                    }
                    IconButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.drawing_clear))
                    }
                    TextButton(onClick = { save() }, modifier = Modifier.testTag("drawing_save")) { Text(stringResource(R.string.action_save)) }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .testTag("drawing_canvas")
                            .pointerInput(strokeColor, strokeWidth) {
                                detectTapGestures { o -> strokes.add(Stroke2(listOf(o), strokeColor, strokeWidth)) }
                            }
                            .pointerInput(strokeColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { current = listOf(it) },
                                    onDragEnd = {
                                        if (current.isNotEmpty()) strokes.add(Stroke2(current, strokeColor, strokeWidth))
                                        current = emptyList()
                                    },
                                    onDragCancel = { current = emptyList() },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        current = current + change.position
                                    }
                                )
                            }
                    ) {
                        fun draw(points: List<Offset>, c: Color, w: Float) {
                            if (points.size == 1) {
                                drawCircle(c, w / 2, points[0])
                                return
                            }
                            val path = Path().apply {
                                points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                            }
                            drawPath(path, c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        strokes.forEach { draw(it.points, it.color, it.width) }
                        if (current.isNotEmpty()) draw(current, strokeColor, strokeWidth)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    penColors.forEach { c ->
                        val sel = !eraser && c == color
                        Box(
                            Modifier.size(if (sel) 34.dp else 28.dp).clip(CircleShape).background(c)
                                .border(if (sel) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { color = c; eraser = false }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    penWidths.forEachIndexed { i, w ->
                        val sel = i == widthIndex
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { widthIndex = i },
                            contentAlignment = Alignment.Center
                        ) { Box(Modifier.size((w / 2 + 4).dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { eraser = !eraser }) {
                        Text(stringResource(if (eraser) R.string.drawing_pen else R.string.drawing_eraser))
                    }
                }
            }
        }
    }
}
