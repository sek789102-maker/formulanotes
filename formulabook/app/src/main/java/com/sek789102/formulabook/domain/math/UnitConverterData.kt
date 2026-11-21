package com.sek789102.formulabook.domain.math

import androidx.annotation.StringRes
import com.sek789102.formulabook.R
import kotlin.math.PI

enum class UnitCategory(@param:StringRes val labelRes: Int) {
    LENGTH(R.string.unit_cat_length),
    MASS(R.string.unit_cat_mass),
    TEMPERATURE(R.string.unit_cat_temperature),
    SPEED(R.string.unit_cat_speed),
    ENERGY(R.string.unit_cat_energy),
    ANGLE(R.string.unit_cat_angle)
}

data class UnitOption(
    val id: String,
    @param:StringRes val labelRes: Int,
    val symbol: String,
    val toBaseFactor: Double = 1.0
)

data class ScienceConstant(
    val symbol: String,
    @param:StringRes val nameRes: Int,
    val valueText: String,
    val insertValue: String,
    val unit: String
)

object UnitConverterEngine {

    val unitsByCategory: Map<UnitCategory, List<UnitOption>> = mapOf(
        UnitCategory.LENGTH to listOf(
            UnitOption("mm", R.string.unit_mm, "mm", 0.001),
            UnitOption("cm", R.string.unit_cm, "cm", 0.01),
            UnitOption("m", R.string.unit_m, "m", 1.0),
            UnitOption("km", R.string.unit_km, "km", 1000.0),
            UnitOption("in", R.string.unit_in, "in", 0.0254),
            UnitOption("ft", R.string.unit_ft, "ft", 0.3048)
        ),
        UnitCategory.MASS to listOf(
            UnitOption("mg", R.string.unit_mg, "mg", 0.000001),
            UnitOption("g", R.string.unit_g, "g", 0.001),
            UnitOption("kg", R.string.unit_kg, "kg", 1.0),
            UnitOption("lb", R.string.unit_lb, "lb", 0.45359237)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitOption("C", R.string.unit_c, "°C"),
            UnitOption("F", R.string.unit_f, "°F"),
            UnitOption("K", R.string.unit_k, "K")
        ),
        UnitCategory.SPEED to listOf(
            UnitOption("mps", R.string.unit_mps, "m/s", 1.0),
            UnitOption("kmh", R.string.unit_kmh, "km/h", 1.0 / 3.6),
            UnitOption("mph", R.string.unit_mph, "mph", 0.44704)
        ),
        UnitCategory.ENERGY to listOf(
            UnitOption("J", R.string.unit_j, "J", 1.0),
            UnitOption("kJ", R.string.unit_kj, "kJ", 1000.0),
            UnitOption("cal", R.string.unit_cal, "cal", 4.184),
            UnitOption("kcal", R.string.unit_kcal, "kcal", 4184.0)
        ),
        UnitCategory.ANGLE to listOf(
            UnitOption("deg", R.string.unit_deg, "°", 1.0),
            UnitOption("rad", R.string.unit_rad, "rad", 180.0 / PI)
        )
    )

    val scienceConstants: List<ScienceConstant> = listOf(
        ScienceConstant("g", R.string.const_gravity, "9.81", "9.81", "m/s²"),
        ScienceConstant("c", R.string.const_light_speed, "2.998×10⁸", "299792458", "m/s"),
        ScienceConstant("Nₐ", R.string.const_avogadro, "6.022×10²³", "6.022*(10^23)", "mol⁻¹"),
        ScienceConstant("R", R.string.const_gas, "8.314", "8.314", "J/(mol·K)"),
        ScienceConstant("h", R.string.const_planck, "6.626×10⁻³⁴", "6.626*(10^-34)", "J·s"),
        ScienceConstant("G", R.string.const_gravitation, "6.674×10⁻¹¹", "6.674*(10^-11)", "N·m²/kg²")
    )

    fun convert(category: UnitCategory, value: Double, from: UnitOption, to: UnitOption): Double {
        if (category == UnitCategory.TEMPERATURE) {
            val celsius = when (from.id) {
                "C" -> value
                "F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (to.id) {
                "C" -> celsius
                "F" -> celsius * 9.0 / 5.0 + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }
        val base = value * from.toBaseFactor
        return base / to.toBaseFactor
    }
}
