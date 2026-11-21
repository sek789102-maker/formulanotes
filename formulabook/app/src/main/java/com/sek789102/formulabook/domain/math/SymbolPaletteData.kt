package com.sek789102.formulabook.domain.math

import androidx.annotation.StringRes
import com.sek789102.formulabook.R

data class SymbolGroup(
    val id: String,
    @param:StringRes val labelRes: Int,
    val symbols: List<String>
)

data class FormulaStructureHelper(
    @param:StringRes val labelRes: Int,
    val snippet: String
)

object SymbolPaletteData {

    val groups: List<SymbolGroup> = listOf(
        SymbolGroup(
            id = "basic",
            labelRes = R.string.sym_group_basic,
            symbols = listOf("+", "−", "×", "÷", "=", "≠", "≈", "±", "²", "³", "ⁿ", "√", "∛", "≤", "≥", "∞", "∝", "()")
        ),
        SymbolGroup(
            id = "calculus",
            labelRes = R.string.sym_group_calculus,
            symbols = listOf("∫", "∬", "∑", "∏", "∂", "∇", "Δ", "→", "lim ", "sin(", "cos(", "tan(", "log(", "ln(", "eˣ", "|x|")
        ),
        SymbolGroup(
            id = "greek_sci",
            labelRes = R.string.sym_group_physics_chem,
            symbols = listOf("π", "θ", "α", "β", "γ", "λ", "μ", "σ", "ω", "Ω", "ρ", "η", "⇌", "↑", "↓", "°C", "Å", "ΔH")
        ),
        SymbolGroup(
            id = "logic_set",
            labelRes = R.string.sym_group_set_geom,
            symbols = listOf("∈", "∉", "⊂", "⊄", "∪", "∩", "∅", "∀", "∃", "∧", "∨", "⇒", "⇔", "∠", "⊥", "∥", "△", "°")
        ),
        SymbolGroup(
            id = "super_sub",
            labelRes = R.string.sym_group_sup_sub,
            symbols = listOf("⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹", "⁺", "⁻", "₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉", "₊", "₋", "ₙ")
        )
    )

    val structureHelpers: List<FormulaStructureHelper> = listOf(
        FormulaStructureHelper(R.string.struct_fraction, "(a) / (b)"),
        FormulaStructureHelper(R.string.struct_sqrt, "√(x)"),
        FormulaStructureHelper(R.string.struct_power, "x²"),
        FormulaStructureHelper(R.string.struct_quadratic, "ax² + bx + c = 0"),
        FormulaStructureHelper(R.string.struct_integral, "∫ f(x) dx"),
        FormulaStructureHelper(R.string.struct_sigma, "∑ xᵢ"),
        FormulaStructureHelper(R.string.struct_limit, "lim(x→0)"),
        FormulaStructureHelper(R.string.struct_chem_eq, "A + B ⇌ C + D")
    )
}
