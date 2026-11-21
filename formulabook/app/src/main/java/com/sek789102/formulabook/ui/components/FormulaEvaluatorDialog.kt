package com.sek789102.formulabook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.domain.math.EvalResult
import com.sek789102.formulabook.domain.math.MathEvaluator
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaEvaluatorDialog(
    formula: FormulaBlock,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val parsed = remember(formula.expression) {
        MathEvaluator.inspectFormula(formula.expression)
    }
    val variableInputs = remember(formula.expression) {
        mutableStateMapOf<String, String>().apply {
            parsed.variables.forEach { put(it, "") }
        }
    }
    var useDegrees by remember { mutableStateOf(true) }

    val numericMap: Map<String, Double> = buildMap {
        parsed.variables.forEach { varName ->
            val v = variableInputs[varName]
                ?.trim()
                ?.replace(',', '.')
                ?.replace('−', '-')
                ?.toDoubleOrNull()
            if (v != null) put(varName, v)
        }
    }
    val allFilled = parsed.variables.all { numericMap.containsKey(it) }
    val evalResult = remember(formula.expression, numericMap, useDegrees, allFilled) {
        if (!allFilled && parsed.variables.isNotEmpty()) {
            null
        } else {
            MathEvaluator.evaluate(formula.expression, numericMap, useDegrees)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = formula.title.ifBlank { stringResource(R.string.eval_dialog_title_fallback) },
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(Dimens.Space4))
                Surface(
                    shape = RoundedCornerShape(Dimens.Radius8),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = formula.expression,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (parsed.variables.isEmpty()) {
                            stringResource(R.string.eval_constant_expr)
                        } else {
                            stringResource(R.string.eval_fill_variables, parsed.variables.size)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    FilterChip(
                        selected = useDegrees,
                        onClick = { useDegrees = !useDegrees },
                        label = {
                            Text(
                                stringResource(if (useDegrees) R.string.eval_mode_deg else R.string.eval_mode_rad)
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                parsed.variables.forEach { varName ->
                    val currentVal = variableInputs[varName].orEmpty()
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { variableInputs[varName] = it },
                        label = { Text(stringResource(R.string.eval_var_label, varName)) },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val trimmed = currentVal.trim()
                                    variableInputs[varName] = if (trimmed.startsWith("-")) {
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(Dimens.Space4))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(Dimens.Space16)) {
                        val prefix = parsed.targetLabel?.let { "$it = " }
                            ?: stringResource(R.string.eval_result_prefix)
                        when (evalResult) {
                            null -> {
                                Text(
                                    text = stringResource(R.string.eval_fill_all_prompt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            is EvalResult.Success -> {
                                Text(
                                    text = stringResource(R.string.eval_answer_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$prefix${evalResult.formatted}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            is EvalResult.Error -> {
                                Text(
                                    text = evalResult.localizedMessage(context),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
