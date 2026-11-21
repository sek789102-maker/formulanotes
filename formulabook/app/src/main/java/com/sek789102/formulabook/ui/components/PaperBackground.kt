package com.sek789102.formulabook.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.sek789102.formulabook.ui.theme.PaperLineDark
import com.sek789102.formulabook.ui.theme.PaperLineLight
import com.sek789102.formulabook.ui.theme.PaperMarginDark
import com.sek789102.formulabook.ui.theme.PaperMarginLight

@Composable
fun paperStrokeColors(): Pair<Color, Color> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        PaperLineDark to PaperMarginDark
    } else {
        PaperLineLight to PaperMarginLight
    }
}

/**
 * Draws authentic stationery paper textures (0 = Ruled Notebook, 1 = Dot Grid, 2 = Plain Paper).
 */
fun Modifier.notebookPaperBackground(
    paperStyle: Int,
    lineColor: Color,
    marginColor: Color
): Modifier = this.drawBehind {
    when (paperStyle) {
        0 -> {
            // Classic Ruled Notebook Lines + Left Margin Accent
            val lineSpacing = 28.dp.toPx()
            var y = lineSpacing
            while (y < size.height) {
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += lineSpacing
            }
            val marginX = 24.dp.toPx()
            drawLine(
                color = marginColor.copy(alpha = 0.35f),
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = 1.2.dp.toPx()
            )
        }
        1 -> {
            // Study Dot Grid
            val spacing = 22.dp.toPx()
            val radius = 1.3.dp.toPx()
            var y = spacing
            while (y < size.height) {
                var x = spacing
                while (x < size.width) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.85f),
                        radius = radius,
                        center = Offset(x, y)
                    )
                    x += spacing
                }
                y += spacing
            }
        }
        else -> {
            // Plain Cream Paper - no overlay lines
        }
    }
}
