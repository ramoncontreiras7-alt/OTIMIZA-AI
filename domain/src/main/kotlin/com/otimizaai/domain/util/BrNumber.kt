package com.otimizaai.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Leitura e escrita de números no formato brasileiro.
 *
 * Entrada aceita: "11,50", "R$ 11,50", "1.234,56", "11.50", "62", "62,5".
 * Saída: "R$ 159,05", "-R$ 14,65", "2,57".
 */
object BrNumber {

    /** Converte texto digitado em número decimal. Retorna null se não for um número. */
    fun parseDecimal(text: String): BigDecimal? {
        var t = text.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace(" ", "")
        if (t.isEmpty()) return null
        if (t.contains(',')) {
            // Formato brasileiro: ponto é separador de milhar, vírgula é decimal.
            t = t.replace(".", "").replace(',', '.')
        }
        return t.toBigDecimalOrNull()
    }

    /** "11,50" -> 1150 centavos. Retorna null se inválido. */
    fun parseCents(text: String): Long? {
        val value = parseDecimal(text) ?: return null
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    /** 15905 -> "R$ 159,05"; -1465 -> "-R$ 14,65". */
    fun formatCents(cents: Long): String {
        val absolute = abs(cents)
        val reais = (absolute / 100).toString()
            .reversed().chunked(3).joinToString(".").reversed()
        val centavos = (absolute % 100).toString().padStart(2, '0')
        val sign = if (cents < 0) "-" else ""
        return "${sign}R$ $reais,$centavos"
    }

    /** 2.565 -> "2,57" (com [decimals] casas). */
    fun formatDecimal(value: Double, decimals: Int = 2): String =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
}
