package app.velora.launcher.core.data.search

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.pow

/**
 * Calculadora del buscador: "12*3", "(4+6)/2", "2,5 x 4". Kotlin puro, sin red ni librerías.
 * Solo responde si hay al menos una operación: escribir "12" sigue buscando apps.
 */
object Calculator {

    /** El resultado ya formateado ("36", "2.5"), o null si no es una cuenta válida. */
    fun evaluate(input: String): String? {
        val text = input.replace(" ", "").replace(',', '.')
            .replace('x', '*').replace('X', '*').replace('×', '*').replace('÷', '/')
        val valid = text.isNotEmpty() && text.any { it in OPERATORS } && text.all { it in ALLOWED }
        val value = if (valid) runCatching { Parser(text).parse() }.getOrNull() else null
        return value?.takeUnless { it.isNaN() || it.isInfinite() }?.let(::format)
    }

    private fun format(value: Double): String {
        val rounded = BigDecimal(value).round(MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
        return rounded.stripTrailingZeros().toPlainString()
    }

    /** Descenso recursivo: suma/resta < producto/división < potencia < signo < número o paréntesis. */
    private class Parser(private val text: String) {
        private var index = 0

        fun parse(): Double = expression().also { require(index == text.length) }

        private fun peek(): Char? = text.getOrNull(index)

        private fun expression(): Double {
            var value = term()
            while (peek() == '+' || peek() == '-') {
                val op = text[index++]
                val right = term()
                value = if (op == '+') value + right else value - right
            }
            return value
        }

        private fun term(): Double {
            var value = power()
            while (peek() == '*' || peek() == '/' || peek() == '%') {
                val op = text[index++]
                val right = power()
                value = when (op) {
                    '*' -> value * right
                    '/' -> value / right
                    else -> value % right
                }
            }
            return value
        }

        private fun power(): Double {
            val base = unary()
            if (peek() != '^') return base
            index++
            return base.pow(power())
        }

        private fun unary(): Double {
            if (peek() != '-') return primary()
            index++
            return -unary()
        }

        private fun primary(): Double {
            if (peek() == '(') {
                index++
                val value = expression()
                require(peek() == ')')
                index++
                return value
            }
            val start = index
            while (peek()?.let { it.isDigit() || it == '.' } == true) index++
            return text.substring(start, index).toDouble()
        }
    }

    private const val OPERATORS = "+-*/%^"
    private const val ALLOWED = "0123456789.()+-*/%^"
    private const val SIGNIFICANT_DIGITS = 10
}
