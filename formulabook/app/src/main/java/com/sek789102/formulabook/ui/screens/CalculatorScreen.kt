package com.sek789102.formulabook.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.domain.math.EvalResult
import com.sek789102.formulabook.domain.math.MathEvaluator
import com.sek789102.formulabook.domain.math.UnitCategory
import com.sek789102.formulabook.domain.math.UnitConverterEngine
import com.sek789102.formulabook.domain.study.GpaCalculator
import com.sek789102.formulabook.domain.study.GpaCourseEntry
import com.sek789102.formulabook.ui.components.StudentMascotAvatar
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily
import com.sek789102.formulabook.ui.viewmodel.CalcHistoryEntry
import com.sek789102.formulabook.ui.viewmodel.MathLabTool
import java.util.Locale
import kotlin.math.abs

@Composable
fun CalculatorScreen(
    activeTool: MathLabTool,
    graphExpression: String,
    expression: String,
    useDegrees: Boolean,
    errorText: String?,
    lastAnswer: String?,
    history: List<CalcHistoryEntry>,
    gpaCourses: List<GpaCourseEntry>,
    onSelectTool: (MathLabTool) -> Unit,
    onGraphExpressionChange: (String) -> Unit,
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEvaluate: () -> Unit,
    onToggleDegrees: () -> Unit,
    onSelectHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onAddGpaCourse: (String, Double, Double) -> Unit,
    onRemoveGpaCourse: (String) -> Unit,
    onClearGpaCourses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolMascotRes = when (activeTool) {
        MathLabTool.CALCULATOR, MathLabTool.GRAPH -> R.drawable.img_mascot_math
        MathLabTool.CONVERTER -> R.drawable.img_mascot_science
        MathLabTool.GPA -> R.drawable.img_mascot_trophy
    }
    val toolMascotDescRes = when (activeTool) {
        MathLabTool.CALCULATOR, MathLabTool.GRAPH -> R.string.mascot_math_desc
        MathLabTool.CONVERTER -> R.string.mascot_science_desc
        MathLabTool.GPA -> R.string.mascot_trophy_desc
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.SheetMaxWidth)
                .fillMaxSize()
                .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    StudentMascotAvatar(
                        sizeDp = 46,
                        drawableRes = toolMascotRes,
                        contentDescription = stringResource(toolMascotDescRes)
                    )
                    Spacer(Modifier.width(Dimens.Space12))
                    Text(
                        text = stringResource(R.string.math_lab_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activeTool == MathLabTool.CALCULATOR || activeTool == MathLabTool.GRAPH) {
                    Spacer(Modifier.width(Dimens.Space8))
                    FilterChip(
                        selected = useDegrees,
                        onClick = onToggleDegrees,
                        label = { Text(if (useDegrees) "DEG" else "RAD") }
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space8))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTool == MathLabTool.CALCULATOR,
                    onClick = { onSelectTool(MathLabTool.CALCULATOR) },
                    label = { Text(stringResource(MathLabTool.CALCULATOR.labelRes)) },
                    leadingIcon = { Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = activeTool == MathLabTool.GPA,
                    onClick = { onSelectTool(MathLabTool.GPA) },
                    label = { Text(stringResource(MathLabTool.GPA.labelRes)) },
                    leadingIcon = { Icon(Icons.Rounded.Grade, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tool_gpa_tab")
                )
                FilterChip(
                    selected = activeTool == MathLabTool.GRAPH,
                    onClick = { onSelectTool(MathLabTool.GRAPH) },
                    label = { Text(stringResource(MathLabTool.GRAPH.labelRes)) },
                    leadingIcon = { Icon(Icons.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
                FilterChip(
                    selected = activeTool == MathLabTool.CONVERTER,
                    onClick = { onSelectTool(MathLabTool.CONVERTER) },
                    label = { Text(stringResource(MathLabTool.CONVERTER.labelRes)) },
                    leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            when (activeTool) {
                MathLabTool.CALCULATOR -> ScientificCalcPanel(
                    expression = expression,
                    useDegrees = useDegrees,
                    errorText = errorText,
                    lastAnswer = lastAnswer,
                    history = history,
                    onAppend = onAppend,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onEvaluate = onEvaluate,
                    onSelectHistory = onSelectHistory,
                    onClearHistory = onClearHistory,
                    modifier = Modifier.weight(1f)
                )
                MathLabTool.GRAPH -> GraphPlotterPanel(
                    funcExpr = graphExpression,
                    onFuncExprChange = onGraphExpressionChange,
                    useDegrees = useDegrees,
                    modifier = Modifier.weight(1f)
                )
                MathLabTool.CONVERTER -> UnitConverterPanel(
                    onSendConstantToCalc = { valStr ->
                        onAppend(valStr)
                        onSelectTool(MathLabTool.CALCULATOR)
                    },
                    modifier = Modifier.weight(1f)
                )
                MathLabTool.GPA -> GpaCalculatorPanel(
                    courses = gpaCourses,
                    onAddCourse = onAddGpaCourse,
                    onRemoveCourse = onRemoveGpaCourse,
                    onClearCourses = onClearGpaCourses,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScientificCalcPanel(
    expression: String,
    useDegrees: Boolean,
    errorText: String?,
    lastAnswer: String?,
    history: List<CalcHistoryEntry>,
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEvaluate: () -> Unit,
    onSelectHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val livePreview = remember(expression, useDegrees) {
        if (expression.isNotBlank()) {
            (MathEvaluator.evaluate(expression, useDegrees = useDegrees) as? EvalResult.Success)?.formatted
        } else null
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.Radius16),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = expression.ifEmpty { "0" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lastAnswer != null) {
                            Surface(
                                onClick = { onAppend(lastAnswer) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Ans",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (errorText != null) {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (livePreview != null && livePreview != expression) {
                            Text(
                                text = "= $livePreview",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "sin, cos, tan, log, ln, √, π, e",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calc_history_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.calc_clear_history))
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    history.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHistory(entry.expression) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.expression,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "= ${entry.result}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sciRows = listOf(
                listOf("sin(" to "sin", "cos(" to "cos", "tan(" to "tan", "√(" to "√", "∛(" to "∛", "π" to "π"),
                listOf("ln(" to "ln", "log(" to "log", "(" to "(", ")" to ")", "^" to "^", "!" to "!")
            )
            sciRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (token, label) ->
                        CalcKey(
                            label = label,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            onClick = { onAppend(token) }
                        )
                    }
                }
            }

            val mainRows = listOf(
                listOf("AC", "DEL", "²", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "e", "=")
            )
            mainRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isPrimaryAction = key == "="
                        val isOperator = key in setOf("÷", "×", "−", "+", "²", "AC", "DEL")
                        val bg = when {
                            isPrimaryAction -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = when {
                            isPrimaryAction -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        if (key == "DEL") {
                            Surface(
                                onClick = onBackspace,
                                shape = RoundedCornerShape(14.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = fg
                                    )
                                }
                            }
                        } else {
                            CalcKey(
                                label = key,
                                containerColor = bg,
                                contentColor = fg,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (key == "=") Modifier.testTag("calc_equals_button") else Modifier
                                    ),
                                heightDp = 50,
                                onClick = {
                                    when (key) {
                                        "AC" -> onClear()
                                        "=" -> onEvaluate()
                                        else -> onAppend(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GraphPlotterPanel(
    funcExpr: String,
    onFuncExprChange: (String) -> Unit,
    useDegrees: Boolean,
    modifier: Modifier = Modifier
) {
    var rangeBound by rememberSaveable { mutableFloatStateOf(10f) }
    var inspectX by rememberSaveable { mutableFloatStateOf(0f) }

    val samples = remember(funcExpr, rangeBound, useDegrees) {
        if (funcExpr.isBlank()) {
            emptyList()
        } else {
            val count = 180
            val minX = -rangeBound.toDouble()
            val maxX = rangeBound.toDouble()
            val step = (maxX - minX) / count
            (0..count).map { idx ->
                val x = minX + idx * step
                val res = MathEvaluator.evaluate(funcExpr, mapOf("x" to x), useDegrees = useDegrees)
                val y = (res as? EvalResult.Success)?.value
                x to y
            }
        }
    }

    val hasValidSamples = remember(samples) {
        samples.any { it.second != null && !it.second!!.isNaN() && !it.second!!.isInfinite() }
    }
    val hasExprError = funcExpr.isNotBlank() && samples.isNotEmpty() && !hasValidSamples

    val inspectSuccess = remember(funcExpr, inspectX, useDegrees) {
        if (funcExpr.isBlank()) null
        else MathEvaluator.evaluate(funcExpr, mapOf("x" to inspectX.toDouble()), useDegrees = useDegrees) as? EvalResult.Success
    }
    val inspectY = inspectSuccess?.formatted
    val inspectValue = inspectSuccess?.value

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = funcExpr,
            onValueChange = onFuncExprChange,
            label = { Text(stringResource(R.string.graph_func_label)) },
            placeholder = { Text(stringResource(R.string.graph_func_placeholder)) },
            isError = hasExprError,
            supportingText = if (hasExprError) {
                { Text(stringResource(R.string.graph_invalid_expr)) }
            } else null,
            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 16.sp),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("x", "x²", "√(x)", "sin(x)", "cos(x)", "2×x+1", "1/x").forEach { snippet ->
                Surface(
                    onClick = {
                        onFuncExprChange(if (funcExpr.isBlank()) snippet else "$funcExpr$snippet")
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = snippet,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.graph_axis_range, rangeBound.toInt()),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                listOf(5f, 10f, 20f).forEach { bound ->
                    FilterChip(
                        selected = rangeBound == bound,
                        onClick = {
                            rangeBound = bound
                            inspectX = inspectX.coerceIn(-bound, bound)
                        },
                        label = { Text("±${bound.toInt()}") }
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(Dimens.Radius16),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val bound = rangeBound

                    fun toScreenX(x: Float): Float = ((x + bound) / (2f * bound)) * w
                    fun toScreenY(y: Float): Float = h - ((y + bound) / (2f * bound)) * h

                    val gridSteps = 8
                    for (i in 0..gridSteps) {
                        val gx = (w / gridSteps) * i
                        val gy = (h / gridSteps) * i
                        drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
                    }

                    val zeroX = toScreenX(0f)
                    val zeroY = toScreenY(0f)
                    drawLine(axisColor.copy(alpha = 0.6f), Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1.5.dp.toPx())
                    drawLine(axisColor.copy(alpha = 0.6f), Offset(zeroX, 0f), Offset(zeroX, h), strokeWidth = 1.5.dp.toPx())

                    if (samples.isNotEmpty()) {
                        val path = Path()
                        var penDown = false
                        var prevY: Double? = null
                        for ((x, y) in samples) {
                            if (y == null || y.isNaN() || y.isInfinite() || abs(y) > bound * 4.0) {
                                penDown = false
                                prevY = null
                                continue
                            }
                            if (prevY != null && abs(y - prevY) > bound * 1.5) {
                                penDown = false
                            }
                            val sx = toScreenX(x.toFloat())
                            val sy = toScreenY(y.toFloat())
                            if (!penDown) {
                                path.moveTo(sx, sy)
                                penDown = true
                            } else {
                                path.lineTo(sx, sy)
                            }
                            prevY = y
                        }
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    val inspRes = inspectValue
                    if (inspRes != null && !inspRes.isNaN() && !inspRes.isInfinite()) {
                        val px = toScreenX(inspectX)
                        val py = toScreenY(inspRes.toFloat()).coerceIn(0f, h)
                        drawLine(
                            color = secondaryColor.copy(alpha = 0.45f),
                            start = Offset(px, 0f),
                            end = Offset(px, h),
                            strokeWidth = 1.2.dp.toPx()
                        )
                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(px, py))
                        drawCircle(color = secondaryColor, radius = 4.dp.toPx(), center = Offset(px, py))
                    }
                }

                if (funcExpr.isBlank()) {
                    Text(
                        text = stringResource(R.string.graph_empty_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.graph_inspect_x, MathEvaluator.formatNumber(inspectX.toDouble())),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (inspectY != null) "f(x) = $inspectY" else "f(x) = -",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = inspectX,
                    onValueChange = { inspectX = (kotlin.math.round(it * 10f) / 10f) },
                    valueRange = -rangeBound..rangeBound
                )
            }
        }
    }
}

@Composable
private fun UnitConverterPanel(
    onSendConstantToCalc: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var categoryName by rememberSaveable { mutableStateOf(UnitCategory.LENGTH.name) }
    val category = UnitCategory.entries.firstOrNull { it.name == categoryName } ?: UnitCategory.LENGTH
    val units = UnitConverterEngine.unitsByCategory[category].orEmpty()
    var fromIndex by rememberSaveable(categoryName) { mutableIntStateOf(0) }
    var toIndex by rememberSaveable(categoryName) {
        mutableIntStateOf(if (units.size > 2) 2 else 1.coerceAtMost(units.lastIndex))
    }
    var inputText by rememberSaveable { mutableStateOf("") }

    val fromUnit = units[fromIndex.coerceIn(0, units.lastIndex)]
    val toUnit = units[toIndex.coerceIn(0, units.lastIndex)]
    val numericInput = inputText.trim().replace(',', '.').replace('−', '-').toDoubleOrNull()
    val convertedText = remember(category, numericInput, fromUnit, toUnit) {
        if (numericInput == null) {
            "—"
        } else {
            MathEvaluator.formatNumber(UnitConverterEngine.convert(category, numericInput, fromUnit, toUnit))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { categoryName = cat.name },
                    label = { Text(stringResource(cat.labelRes)) }
                )
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text(stringResource(R.string.converter_value_label, fromUnit.symbol)) },
            placeholder = { Text(stringResource(R.string.converter_value_placeholder)) },
            trailingIcon = {
                TextButton(
                    onClick = {
                        val trimmed = inputText.trim()
                        inputText = if (trimmed.startsWith("-")) {
                            trimmed.removePrefix("-")
                        } else {
                            "-$trimmed"
                        }
                    }
                ) {
                    Text(
                        text = "+/-",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.converter_from_label), style = MaterialTheme.typography.labelLarge)
            units.forEachIndexed { idx, u ->
                FilterChip(
                    selected = idx == fromIndex,
                    onClick = { fromIndex = idx },
                    label = { Text("${stringResource(u.labelRes)} (${u.symbol})") }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    val tmp = fromIndex
                    fromIndex = toIndex
                    toIndex = tmp
                }
            ) {
                Icon(
                    Icons.Rounded.SwapVert,
                    contentDescription = stringResource(R.string.converter_swap_desc),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.converter_to_label), style = MaterialTheme.typography.labelLarge)
            units.forEachIndexed { idx, u ->
                FilterChip(
                    selected = idx == toIndex,
                    onClick = { toIndex = idx },
                    label = { Text("${stringResource(u.labelRes)} (${u.symbol})") }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.converter_result_label, stringResource(toUnit.labelRes)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "$convertedText ${toUnit.symbol}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.converter_constants_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        UnitConverterEngine.scienceConstants.forEach { constant ->
            Surface(
                onClick = { onSendConstantToCalc(constant.insertValue) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${constant.symbol} — ${stringResource(constant.nameRes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${constant.valueText} ${constant.unit}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(constant.insertValue))
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_constant_copied, constant.symbol),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.converter_copy_constant_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GpaCalculatorPanel(
    courses: List<GpaCourseEntry>,
    onAddCourse: (String, Double, Double) -> Unit,
    onRemoveCourse: (String) -> Unit,
    onClearCourses: () -> Unit,
    modifier: Modifier = Modifier
) {
    var courseName by rememberSaveable { mutableStateOf("") }
    var selectedCredits by rememberSaveable { mutableDoubleStateOf(1.5) }
    var selectedGrade by rememberSaveable { mutableDoubleStateOf(4.0) }

    val summary = remember(courses) { GpaCalculator.computeGpa(courses) }
    val gpaFormatted = String.format(Locale.US, "%.2f", summary.gpa)
    val creditsFormatted = MathEvaluator.formatNumber(summary.totalCredits)

    val reactionMascotRes = when {
        courses.isEmpty() -> R.drawable.img_student_mascot
        summary.gpa >= 3.5 -> R.drawable.img_mascot_trophy
        summary.gpa >= 2.5 -> R.drawable.img_mascot_study
        else -> R.drawable.img_mascot_focus
    }
    val reactionText = when {
        courses.isEmpty() -> stringResource(R.string.gpa_empty_hint)
        summary.gpa >= 3.5 -> stringResource(R.string.gpa_cheer_top)
        summary.gpa >= 2.5 -> stringResource(R.string.gpa_cheer_good)
        else -> stringResource(R.string.gpa_cheer_keep_going)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.Radius24),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Dimens.Space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StudentMascotAvatar(
                    sizeDp = 58,
                    drawableRes = reactionMascotRes,
                    contentDescription = stringResource(R.string.mascot_trophy_desc)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_header_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Text(
                        text = gpaFormatted,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.gpa_total_credits, creditsFormatted, summary.courseCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = reactionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(Dimens.Radius16),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Dimens.Space16),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text(stringResource(R.string.gpa_course_name_label)) },
                    placeholder = { Text(stringResource(R.string.gpa_course_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gpa_course_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_credits_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GpaCalculator.creditPresets.forEach { credit ->
                            FilterChip(
                                selected = selectedCredits == credit,
                                onClick = { selectedCredits = credit },
                                label = { Text(MathEvaluator.formatNumber(credit)) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.gpa_grade_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GpaCalculator.thaiGradeScale.forEach { (point, label) ->
                            FilterChip(
                                selected = selectedGrade == point,
                                onClick = { selectedGrade = point },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onAddCourse(courseName, selectedCredits, selectedGrade)
                        courseName = ""
                    },
                    enabled = courseName.trim().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_gpa_course_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.gpa_add_course))
                }
            }
        }

        if (courses.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.gpa_total_credits, creditsFormatted, summary.courseCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onClearCourses) {
                    Text(
                        text = stringResource(R.string.gpa_clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            courses.forEach { entry ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.gpa_course_meta,
                                    MathEvaluator.formatNumber(entry.credits),
                                    String.format(Locale.US, "%.1f", entry.gradePoint)
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onRemoveCourse(entry.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CalcKey(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    heightDp: Int = 42
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.Radius12),
        color = containerColor,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.heightIn(min = heightDp.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (heightDp >= 48) 19.sp else 14.sp,
                maxLines = 1,
                color = contentColor
            )
        }
    }
}
