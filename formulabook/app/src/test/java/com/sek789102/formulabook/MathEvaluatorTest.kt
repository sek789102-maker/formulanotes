package com.sek789102.formulabook

import com.sek789102.formulabook.domain.math.EvalResult
import com.sek789102.formulabook.domain.math.MathErrorCode
import com.sek789102.formulabook.domain.math.MathEvaluator
import com.sek789102.formulabook.domain.math.UnitCategory
import com.sek789102.formulabook.domain.math.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEvaluatorTest {

    @Test
    fun `evaluates unicode operators, powers, and trig in degrees`() {
        val res1 = MathEvaluator.evaluate("2 × (3 + 4)² − √(16)") as EvalResult.Success
        assertEquals("94", res1.formatted)

        val res2 = MathEvaluator.evaluate("sin(30) + cos(60)", useDegrees = true) as EvalResult.Success
        assertEquals("1", res2.formatted)
    }

    @Test
    fun `inspects formula variables including subscripts and evaluates with values`() {
        val parsed = MathEvaluator.inspectFormula("E = 0.5 × m × v²")
        assertEquals("E", parsed.targetLabel)
        assertEquals(listOf("m", "v"), parsed.variables)

        val res = MathEvaluator.evaluate("E = 0.5 × m × v²", mapOf("m" to 4.0, "v" to 5.0))
        assertTrue(res is EvalResult.Success)
        assertEquals("50", (res as EvalResult.Success).formatted)
    }

    @Test
    fun `handles division by zero gracefully`() {
        val res = MathEvaluator.evaluate("10 ÷ 0")
        assertTrue(res is EvalResult.Error)
        assertEquals(MathErrorCode.DIVISION_BY_ZERO, (res as EvalResult.Error).code)
    }

    @Test
    fun `converts student science units accurately`() {
        val tempUnits = UnitConverterEngine.unitsByCategory[UnitCategory.TEMPERATURE]!!
        val celsius = tempUnits.first { it.id == "C" }
        val fahrenheit = tempUnits.first { it.id == "F" }
        val kelvin = tempUnits.first { it.id == "K" }

        assertEquals(212.0, UnitConverterEngine.convert(UnitCategory.TEMPERATURE, 100.0, celsius, fahrenheit), 1e-6)
        assertEquals(273.15, UnitConverterEngine.convert(UnitCategory.TEMPERATURE, 0.0, celsius, kelvin), 1e-6)
    }

    @Test
    fun `evaluates basic arithmetic, order of operations, decimals, and negatives`() {
        assertEquals("2", (MathEvaluator.evaluate("1+1") as EvalResult.Success).formatted)
        assertEquals("6", (MathEvaluator.evaluate("2×3") as EvalResult.Success).formatted)
        assertEquals("5", (MathEvaluator.evaluate("10÷2") as EvalResult.Success).formatted)
        assertEquals("20", (MathEvaluator.evaluate("(2+3)×4") as EvalResult.Success).formatted)
        assertEquals("0.3", (MathEvaluator.evaluate("0.1+0.2") as EvalResult.Success).formatted)
        assertEquals("-12.5", (MathEvaluator.evaluate("-5 × 2.5") as EvalResult.Success).formatted)
    }

    @Test
    fun `evaluates exponent right-associativity and unary minus precedence`() {
        assertEquals("8", (MathEvaluator.evaluate("2^3") as EvalResult.Success).formatted)
        assertEquals("512", (MathEvaluator.evaluate("2^3^2") as EvalResult.Success).formatted)
        assertEquals("-4", (MathEvaluator.evaluate("-2^2") as EvalResult.Success).formatted)
        assertEquals("4", (MathEvaluator.evaluate("(-2)^2") as EvalResult.Success).formatted)
        assertEquals("0.25", (MathEvaluator.evaluate("2^-2") as EvalResult.Success).formatted)
    }

    @Test
    fun `evaluates roots and factorials including domain and overflow checks`() {
        assertEquals("3", (MathEvaluator.evaluate("√9") as EvalResult.Success).formatted)
        val sqrt2 = MathEvaluator.evaluate("√2") as EvalResult.Success
        assertEquals(1.414213562, sqrt2.value, 1e-7)
        assertEquals("5", (MathEvaluator.evaluate("√(25)") as EvalResult.Success).formatted)
        assertEquals("6", (MathEvaluator.evaluate("2√9") as EvalResult.Success).formatted)
        assertEquals("2", (MathEvaluator.evaluate("∛8") as EvalResult.Success).formatted)
        val circ = MathEvaluator.evaluate("2πr", mapOf("r" to 5.0)) as EvalResult.Success
        assertEquals(10.0 * kotlin.math.PI, circ.value, 1e-9)

        assertEquals("120", (MathEvaluator.evaluate("5!") as EvalResult.Success).formatted)
        assertEquals("1", (MathEvaluator.evaluate("0!") as EvalResult.Success).formatted)
        assertEquals("-6", (MathEvaluator.evaluate("-3!") as EvalResult.Success).formatted)
        assertEquals("12", (MathEvaluator.evaluate("3!2") as EvalResult.Success).formatted)

        val negFact = MathEvaluator.evaluate("(-1)!") as EvalResult.Error
        assertEquals(MathErrorCode.FACTORIAL_DOMAIN, negFact.code)

        val fracFact = MathEvaluator.evaluate("2.5!") as EvalResult.Error
        assertEquals(MathErrorCode.FACTORIAL_DOMAIN, fracFact.code)

        val hugeFact = MathEvaluator.evaluate("171!") as EvalResult.Error
        assertEquals(MathErrorCode.OVERFLOW, hugeFact.code)
    }

    @Test
    fun `handles scientific notation, trig asymptotes, syntax errors, and overflow`() {
        val electronCharge = MathEvaluator.evaluate("1.6e-19 × 2") as EvalResult.Success
        assertEquals(3.2e-19, electronCharge.value, 1e-28)

        val euler = MathEvaluator.evaluate("2e") as EvalResult.Success
        assertEquals(2 * kotlin.math.E, euler.value, 1e-9)

        assertEquals(
            MathErrorCode.UNDEFINED_RESULT,
            (MathEvaluator.evaluate("tan(90)", useDegrees = true) as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.UNDEFINED_RESULT,
            (MathEvaluator.evaluate("asin(2)") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.MISSING_CLOSE_PAREN,
            (MathEvaluator.evaluate("(2+3") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.INCOMPLETE,
            (MathEvaluator.evaluate("2+3)") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.INCOMPLETE,
            (MathEvaluator.evaluate("√") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.INVALID_SYNTAX,
            (MathEvaluator.evaluate("1.2.3") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.INVALID_SYNTAX,
            (MathEvaluator.evaluate(".") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.MISSING_VARIABLE,
            (MathEvaluator.evaluate("foo + 1") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.OVERFLOW,
            (MathEvaluator.evaluate("2^1024") as EvalResult.Error).code
        )
        assertEquals(
            MathErrorCode.NEGATIVE_SQRT,
            (MathEvaluator.evaluate("√(-4)") as EvalResult.Error).code
        )
        val deepParens = "(".repeat(300) + "1" + ")".repeat(300)
        assertEquals(
            MathErrorCode.INVALID_SYNTAX,
            (MathEvaluator.evaluate(deepParens) as EvalResult.Error).code
        )
    }
}
