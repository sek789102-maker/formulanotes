package com.sek789102.formulabook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.domain.math.MathEvaluator
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaDisplayCard(
    formula: FormulaBlock,
    modifier: Modifier = Modifier,
    recallMode: Boolean = false,
    onCopy: () -> Unit,
    onEvaluate: () -> Unit,
    onPlotGraph: (() -> Unit)? = null,
    onToggleMastered: (() -> Unit)? = null
) {
    var answerRevealed by remember(formula.id, recallMode) { mutableStateOf(false) }
    val showAnswer = !recallMode || answerRevealed

    val isFlashcard = formula.isFlashcard
    val canPlotAsFunction = remember(formula.expression, isFlashcard) {
        if (isFlashcard || formula.expression.isBlank()) false
        else {
            val parsed = MathEvaluator.inspectFormula(formula.expression)
            parsed.variables.size == 1 && parsed.variables.first().equals("x", ignoreCase = true)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.Radius16),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(Dimens.Space16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.Radius8),
                        color = if (isFlashcard) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashcard) Icons.Rounded.Style else Icons.Rounded.Functions,
                            contentDescription = null,
                            tint = if (isFlashcard) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .padding(Dimens.Space8)
                                .size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(Dimens.Space8))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = formula.title.ifBlank { stringResource(R.string.formula_default_title) },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isFlashcard) {
                            Text(
                                text = stringResource(R.string.formula_flashcard_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleMastered != null) {
                        IconButton(onClick = onToggleMastered) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(
                                    if (formula.mastered) R.string.formula_mastered else R.string.formula_mark_mastered
                                ),
                                tint = if (formula.mastered) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.formula_copy_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.Space12))

            if (showAnswer) {
                Surface(
                    onClick = if (recallMode) { { answerRevealed = false } } else ({}),
                    enabled = recallMode,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = formula.expression,
                        fontFamily = if (isFlashcard) FontFamily.Default else JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isFlashcard) 16.sp else 18.sp,
                        lineHeight = if (isFlashcard) 24.sp else 26.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Dimens.Space16, vertical = Dimens.Space12)
                    )
                }

                if (formula.variablesNote.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.Space8))
                    Text(
                        text = formula.variablesNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isFlashcard && formula.expression.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.Space12))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space4)
                    ) {
                        if (canPlotAsFunction && onPlotGraph != null) {
                            AssistChip(
                                onClick = onPlotGraph,
                                label = {
                                    Text(
                                        text = stringResource(R.string.formula_view_graph),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ShowChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null
                            )
                        }

                        AssistChip(
                            onClick = onEvaluate,
                            modifier = Modifier.testTag("evaluate_formula_button"),
                            label = {
                                Text(
                                    text = stringResource(R.string.formula_evaluate_btn),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null
                        )
                    }
                }
            } else {
                Surface(
                    onClick = { answerRevealed = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.Radius12),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.Space8))
                        Text(
                            text = stringResource(R.string.detail_recall_tap_reveal),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
