package com.sek789102.formulabook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sek789102.formulabook.R
import com.sek789102.formulabook.domain.math.SymbolPaletteData
import com.sek789102.formulabook.ui.theme.Dimens
import com.sek789102.formulabook.ui.theme.JetBrainsMonoFontFamily

@Composable
fun MathSymbolToolbar(
    onInsertSymbol: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGroupId by rememberSaveable { mutableStateOf(SymbolPaletteData.groups.first().id) }
    val currentGroup = SymbolPaletteData.groups.firstOrNull { it.id == selectedGroupId }
        ?: SymbolPaletteData.groups.first()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = Dimens.ElevationNone
    ) {
        Column(modifier = Modifier.padding(vertical = Dimens.Space8)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.Space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SymbolPaletteData.groups.forEach { group ->
                    FilterChip(
                        selected = group.id == selectedGroupId,
                        onClick = { selectedGroupId = group.id },
                        label = {
                            Text(
                                text = stringResource(group.labelRes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                Spacer(Modifier.width(Dimens.Space4))
                TextButton(
                    onClick = onOpenTemplates,
                    contentPadding = PaddingValues(horizontal = Dimens.Space12, vertical = Dimens.Space4)
                ) {
                    Text(
                        text = stringResource(R.string.toolbar_structures_btn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space8))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.Space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
            ) {
                currentGroup.symbols.forEach { sym ->
                    Surface(
                        onClick = { onInsertSymbol(sym) },
                        shape = RoundedCornerShape(Dimens.Radius8),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = sym,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = Dimens.Space12, vertical = Dimens.Space8)
                        )
                    }
                }
            }
        }
    }
}
