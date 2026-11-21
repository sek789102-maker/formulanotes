package com.sek789102.formulabook.domain.math

import android.content.Context
import androidx.annotation.StringRes
import com.sek789102.formulabook.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class MathErrorCode(@param:StringRes val messageRes: Int) {
    EMPTY(R.string.math_err_empty),
    UNSUPPORTED_CHARS(R.string.math_err_unsupported_chars),
    NO_EXPRESSION(R.string.math_err_no_expr),
    INCOMPLETE(R.string.math_err_incomplete),
    UNDEFINED_RESULT(R.string.math_err_undefined),
    DIVISION_BY_ZERO(R.string.math_err_div_zero),
    MISSING_CLOSE_PAREN(R.string.math_err_missing_paren),
    MISSING_FUNC_PAREN(R.string.math_err_missing_fn_paren),
    MISSING_VARIABLE(R.string.math_err_missing_var),
    INVALID_SYNTAX(R.string.math_err_invalid_syntax),
    NEGATIVE_SQRT(R.string.math_err_negative_sqrt),
    LOG_DOMAIN(R.string.math_err_log_domain),
    LN_DOMAIN(R.string.math_err_ln_domain),
    FACTORIAL_DOMAIN(R.string.math_err_factorial_domain),
    OVERFLOW(R.string.math_err_overflow),
    UNKNOWN_FUNCTION(R.string.math_err_unknown_fn),
    GENERIC(R.string.math_err_generic);

    fun format(context: Context, arg: String? = null): String {
        return if (arg != null) {
            context.getString(messageRes, arg)
        } else {
            context.getString(messageRes)
        }
    }
}

sealed class EvalResult {
    data class Success(val value: Double, val formatted: String) : EvalResult()
    data class Error(val code: MathErrorCode, val arg: String? = null) : EvalResult() {
        fun localizedMessage(context: Context): String = code.format(context, arg)
    }
}

private class MathEvalException(val code: MathErrorCode, val arg: String? = null) : RuntimeException()

data class ParsedFormula(
    val targetLabel: String?,
    val evaluableExpression: String,
    val variables: List<String>
)

object MathEvaluator {

    private const val MAX_RECURSION_DEPTH = 256

    private val functions = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sqrt", "cbrt", "abs", "log", "ln", "exp", "floor", "ceil", "round"
    )
    private val constants = setOf("pi", "e", "π")

    private val superscriptMap = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9'
    )

    private val subscriptChars = setOf('₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉', '₊', '₋', 'ₙ')

    fun normalizeExpression(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            when {
                ch == '×' || ch == '·' || ch == '⋅' -> sb.append('*')
                ch == '÷' -> sb.append('/')
                ch == '−' || ch == '–' -> sb.append('-')
                ch == 'π' -> {
                    if (sb.isNotEmpty() && isIdentContinue(sb.last())) sb.append(' ')
                    sb.append("pi")
                    val next = raw.getOrNull(i + 1)
                    if (next != null && isIdentContinue(next)) sb.append(' ')
                }
                ch == '√' -> {
                    if (sb.isNotEmpty() && isIdentContinue(sb.last())) sb.append(' ')
                    sb.append("sqrt")
                    val next = raw.getOrNull(i + 1)
                    if (next != null && !next.isWhitespace() && next != '(' && next != '[' && next != '{') {
                        sb.append(' ')
                    }
                }
                ch == '∛' -> {
                    if (sb.isNotEmpty() && isIdentContinue(sb.last())) sb.append(' ')
                    sb.append("cbrt")
                    val next = raw.getOrNull(i + 1)
                    if (next != null && !next.isWhitespace() && next != '(' && next != '[' && next != '{') {
                        sb.append(' ')
                    }
                }
                ch in superscriptMap -> {
                    sb.append("^(")
                    while (i < raw.length && raw[i] in superscriptMap) {
                        sb.append(superscriptMap[raw[i]])
                        i++
                    }
                    sb.append(')')
                    continue
                }
                ch == 'ⁿ' -> sb.append("^n")
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    fun inspectFormula(rawFormula: String): ParsedFormula {
        val trimmed = rawFormula.trim()
        val parts = trimmed.split("=", limit = 2)
        val (targetLabel, rhs) = if (parts.size == 2 && parts[1].isNotBlank()) {
            parts[0].trim() to parts[1].trim()
        } else {
            null to trimmed
        }
        val normalized = normalizeExpression(rhs)
        val vars = extractVariables(normalized)
        return ParsedFormula(
            targetLabel = targetLabel,
            evaluableExpression = rhs,
            variables = vars
        )
    }

    fun extractVariables(normalizedExpr: String): List<String> {
        val tokens = tokenize(normalizedExpr) ?: return emptyList()
        val found = LinkedHashSet<String>()
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token is Token.Ident) {
                val lower = token.name.lowercase()
                val nextIsParen = (i + 1 < tokens.size && tokens[i + 1] == Token.LParen)
                if (!nextIsParen && lower !in functions && lower !in constants) {
                    found.add(token.name)
                }
            }
        }
        return found.toList()
    }

    fun evaluate(
        rawExpression: String,
        variables: Map<String, Double> = emptyMap(),
        useDegrees: Boolean = true
    ): EvalResult {
        val trimmed = rawExpression.trim()
        if (trimmed.isEmpty()) return EvalResult.Error(MathErrorCode.EMPTY)

        val rhs = if (trimmed.contains("=")) {
            val parts = trimmed.split("=", limit = 2)
            if (parts[1].isNotBlank()) parts[1].trim() else parts[0].trim()
        } else {
            trimmed
        }

        val normalized = normalizeExpression(rhs)
        return try {
            val rawTokens = tokenizeOrThrow(normalized)
            if (rawTokens.isEmpty()) return EvalResult.Error(MathErrorCode.NO_EXPRESSION)

            val tokens = insertImplicitMultiplication(rawTokens)
            val parser = Parser(tokens, variables, useDegrees)
            val value = parser.parseExpression()
            if (!parser.isAtEnd()) {
                EvalResult.Error(MathErrorCode.INCOMPLETE)
            } else if (value.isInfinite()) {
                EvalResult.Error(MathErrorCode.OVERFLOW)
            } else if (value.isNaN()) {
                EvalResult.Error(MathErrorCode.UNDEFINED_RESULT)
            } else {
                EvalResult.Success(value = value, formatted = formatNumber(value))
            }
        } catch (e: MathEvalException) {
            EvalResult.Error(e.code, e.arg)
        } catch (_: Exception) {
            EvalResult.Error(MathErrorCode.GENERIC)
        }
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        val absVal = abs(value)
        if (absVal < 1e-14) return "0"

        if (absVal >= 1e12 || absVal < 1e-6) {
            val rawSci = String.format(java.util.Locale.US, "%.8e", value)
            val eIdx = rawSci.indexOf('e')
            if (eIdx > 0) {
                val mantissa = rawSci.substring(0, eIdx).trimEnd('0').trimEnd('.')
                val expSign = rawSci[eIdx + 1]
                val expDigits = rawSci.substring(eIdx + 2).trimStart('0').ifEmpty { "0" }
                return "${mantissa}e${expSign}${expDigits}"
            }
        }

        val longVal = value.toLong()
        if (absVal < 1e12 && abs(value - longVal.toDouble()) < 1e-10) {
            return longVal.toString()
        }
        val formatted = String.format(java.util.Locale.US, "%.8f", value)
            .trimEnd('0')
            .trimEnd('.')
        return if (formatted == "-0") "0" else formatted
    }

    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Ident(val name: String) : Token
        data class Op(val symbol: Char) : Token
        data object Factorial : Token
        data object LParen : Token
        data object RParen : Token
        data object Comma : Token
    }

    private fun isIdentStart(ch: Char): Boolean {
        return ch.isLetter() || ch == '_' || ch == 'Δ' || ch == 'θ' || ch == 'α' ||
            ch == 'β' || ch == 'γ' || ch == 'λ' || ch == 'μ' || ch == 'σ' || ch == 'ω' ||
            ch == 'Ω' || ch == 'ρ' || ch == 'η'
    }

    private fun isIdentContinue(ch: Char): Boolean {
        return isIdentStart(ch) || ch.isDigit() || ch in subscriptChars || ch == '\''
    }

    private fun tokenize(input: String): List<Token>? {
        return try {
            tokenizeOrThrow(input)
        } catch (_: MathEvalException) {
            null
        }
    }

    private fun tokenizeOrThrow(input: String): List<Token> {
        val list = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotCount = 0
                    var digitCount = 0
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i] == '.') dotCount++ else digitCount++
                        i++
                    }
                    if (dotCount > 1 || digitCount == 0) {
                        throw MathEvalException(MathErrorCode.INVALID_SYNTAX)
                    }
                    // Support scientific notation literals such as 1.5e-10 or 6.022E23
                    if (i < input.length && (input[i] == 'e' || input[i] == 'E')) {
                        var expIdx = i + 1
                        if (expIdx < input.length && (input[expIdx] == '+' || input[expIdx] == '-')) {
                            expIdx++
                        }
                        var expDigits = 0
                        while (expIdx < input.length && input[expIdx].isDigit()) {
                            expDigits++
                            expIdx++
                        }
                        if (expDigits > 0) {
                            i = expIdx
                        }
                    }
                    val numStr = input.substring(start, i)
                    val num = numStr.toDoubleOrNull()
                        ?: throw MathEvalException(MathErrorCode.INVALID_SYNTAX)
                    if (num.isInfinite()) {
                        throw MathEvalException(MathErrorCode.OVERFLOW)
                    }
                    list.add(Token.Num(num))
                }
                isIdentStart(ch) -> {
                    val start = i
                    i++
                    while (i < input.length && isIdentContinue(input[i])) {
                        i++
                    }
                    val word = input.substring(start, i)
                    list.add(Token.Ident(word))
                }
                ch == '(' || ch == '[' || ch == '{' -> {
                    list.add(Token.LParen)
                    i++
                }
                ch == ')' || ch == ']' || ch == '}' -> {
                    list.add(Token.RParen)
                    i++
                }
                ch == ',' -> {
                    list.add(Token.Comma)
                    i++
                }
                ch == '!' -> {
                    list.add(Token.Factorial)
                    i++
                }
                ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' || ch == '%' -> {
                    list.add(Token.Op(ch))
                    i++
                }
                else -> throw MathEvalException(MathErrorCode.UNSUPPORTED_CHARS)
            }
        }
        return list
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) return tokens
        val out = mutableListOf<Token>()
        for (i in tokens.indices) {
            val cur = tokens[i]
            out.add(cur)
            if (i + 1 < tokens.size) {
                val next = tokens[i + 1]
                val curEndsValue = cur is Token.Num ||
                    cur == Token.RParen ||
                    cur == Token.Factorial ||
                    (cur is Token.Ident && cur.name.lowercase() !in functions)
                val nextStartsValue = next is Token.Num || next == Token.LParen || next is Token.Ident
                if (curEndsValue && nextStartsValue) {
                    out.add(Token.Op('*'))
                }
            }
        }
        return out
    }

    private class Parser(
        private val tokens: List<Token>,
        private val variables: Map<String, Double>,
        private val useDegrees: Boolean
    ) {
        private var pos = 0
        private var depth = 0

        fun isAtEnd(): Boolean = pos >= tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        private inline fun <T> withDepthGuard(block: () -> T): T {
            if (++depth > MAX_RECURSION_DEPTH) {
                throw MathEvalException(MathErrorCode.INVALID_SYNTAX)
            }
            return try {
                block()
            } finally {
                depth--
            }
        }

        fun parseExpression(): Double = withDepthGuard {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (t.symbol == '+') left + right else left - right
                } else {
                    break
                }
            }
            left
        }

        private fun parseTerm(): Double {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/' || t.symbol == '%')) {
                    consume()
                    val right = parseUnary()
                    left = when (t.symbol) {
                        '*' -> left * right
                        '/' -> {
                            if (right == 0.0) throw MathEvalException(MathErrorCode.DIVISION_BY_ZERO)
                            left / right
                        }
                        else -> {
                            if (right == 0.0) throw MathEvalException(MathErrorCode.DIVISION_BY_ZERO)
                            left % right
                        }
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): Double = withDepthGuard {
            val t = peek()
            if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                consume()
                val operand = parseUnary()
                if (t.symbol == '-') -operand else operand
            } else {
                parsePower()
            }
        }

        private fun parsePower(): Double = withDepthGuard {
            val base = parsePostfix()
            val t = peek()
            if (t is Token.Op && t.symbol == '^') {
                consume()
                val exponent = parseUnary()
                val res = base.pow(exponent)
                if (res.isInfinite()) throw MathEvalException(MathErrorCode.OVERFLOW)
                res
            } else {
                base
            }
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (peek() == Token.Factorial) {
                consume()
                value = computeFactorial(value)
            }
            return value
        }

        private fun computeFactorial(n: Double): Double {
            if (n.isNaN() || n < 0.0 || abs(n - round(n)) > 1e-9) {
                throw MathEvalException(MathErrorCode.FACTORIAL_DOMAIN)
            }
            val intN = round(n).toInt()
            if (intN > 170) {
                throw MathEvalException(MathErrorCode.OVERFLOW)
            }
            var result = 1.0
            for (k in 2..intN) {
                result *= k.toDouble()
            }
            return result
        }

        private fun parsePrimary(): Double = withDepthGuard {
            val t = peek() ?: throw MathEvalException(MathErrorCode.INCOMPLETE)
            when (t) {
                is Token.Num -> {
                    consume()
                    t.value
                }
                Token.LParen -> {
                    consume()
                    val value = parseExpression()
                    if (peek() != Token.RParen) throw MathEvalException(MathErrorCode.MISSING_CLOSE_PAREN)
                    consume()
                    value
                }
                is Token.Ident -> {
                    consume()
                    val lower = t.name.lowercase()
                    if (lower in functions) {
                        if (peek() == Token.LParen) {
                            consume()
                            val arg = parseExpression()
                            if (peek() != Token.RParen) throw MathEvalException(MathErrorCode.MISSING_FUNC_PAREN)
                            consume()
                            applyFunction(lower, arg)
                        } else {
                            val next = peek()
                            if (next == null || next == Token.RParen || next == Token.Comma) {
                                throw MathEvalException(MathErrorCode.INCOMPLETE)
                            }
                            val arg = parseUnary()
                            applyFunction(lower, arg)
                        }
                    } else {
                        when (lower) {
                            "pi", "π" -> PI
                            "e" -> variables["e"] ?: kotlin.math.E
                            else -> variables[t.name]
                                ?: variables.entries.firstOrNull { it.key.equals(t.name, ignoreCase = true) }?.value
                                ?: throw MathEvalException(MathErrorCode.MISSING_VARIABLE, t.name)
                        }
                    }
                }
                else -> throw MathEvalException(MathErrorCode.INVALID_SYNTAX)
            }
        }

        private fun applyFunction(fn: String, arg: Double): Double {
            val rad = if (useDegrees) Math.toRadians(arg) else arg
            return when (fn) {
                "sin" -> sin(rad).let { if (abs(it) < 1e-14) 0.0 else it }
                "cos" -> cos(rad).let { if (abs(it) < 1e-14) 0.0 else it }
                "tan" -> {
                    val c = cos(rad)
                    if (abs(c) < 1e-12) throw MathEvalException(MathErrorCode.UNDEFINED_RESULT)
                    tan(rad).let { if (abs(it) < 1e-14) 0.0 else it }
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw MathEvalException(MathErrorCode.UNDEFINED_RESULT)
                    if (useDegrees) Math.toDegrees(asin(arg)) else asin(arg)
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw MathEvalException(MathErrorCode.UNDEFINED_RESULT)
                    if (useDegrees) Math.toDegrees(acos(arg)) else acos(arg)
                }
                "atan" -> if (useDegrees) Math.toDegrees(atan(arg)) else atan(arg)
                "sqrt" -> {
                    if (arg < 0) throw MathEvalException(MathErrorCode.NEGATIVE_SQRT)
                    sqrt(arg)
                }
                "cbrt" -> cbrt(arg)
                "abs" -> abs(arg)
                "log" -> {
                    if (arg <= 0) throw MathEvalException(MathErrorCode.LOG_DOMAIN)
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0) throw MathEvalException(MathErrorCode.LN_DOMAIN)
                    ln(arg)
                }
                "exp" -> exp(arg)
                "floor" -> floor(arg)
                "ceil" -> ceil(arg)
                "round" -> round(arg)
                else -> throw MathEvalException(MathErrorCode.UNKNOWN_FUNCTION, fn)
            }
        }
    }
}
