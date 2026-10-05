package com.otimizaai.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BrNumberTest {

    @Test
    @DisplayName("Lê valores em reais nos formatos que o entregador digita")
    fun parsesCents() {
        assertEquals(1150L, BrNumber.parseCents("11,50"))
        assertEquals(1150L, BrNumber.parseCents("R$ 11,50"))
        assertEquals(1150L, BrNumber.parseCents("11.50"))
        assertEquals(1100L, BrNumber.parseCents("11"))
        assertEquals(123456L, BrNumber.parseCents("1.234,56"))
        assertEquals(629L, BrNumber.parseCents(" 6,29 "))
        assertEquals(5L, BrNumber.parseCents("0,05"))
        assertEquals(1L, BrNumber.parseCents("0,005"))
    }

    @Test
    @DisplayName("Texto que não é número vira null")
    fun rejectsGarbage() {
        assertNull(BrNumber.parseCents(""))
        assertNull(BrNumber.parseCents("abc"))
        assertNull(BrNumber.parseCents("R$"))
        assertNull(BrNumber.parseDecimal("1,2,3"))
    }

    @Test
    @DisplayName("Lê quilômetros com vírgula ou ponto")
    fun parsesKm() {
        assertEquals("62.5", BrNumber.parseDecimal("62,5")?.toPlainString())
        assertEquals("62.5", BrNumber.parseDecimal("62.5")?.toPlainString())
        assertEquals("45", BrNumber.parseDecimal("45")?.toPlainString())
    }

    @Test
    @DisplayName("Escreve valores no formato R$")
    fun formatsCents() {
        assertEquals("R$ 159,05", BrNumber.formatCents(15905))
        assertEquals("R$ 0,05", BrNumber.formatCents(5))
        assertEquals("R$ 0,00", BrNumber.formatCents(0))
        assertEquals("R$ 1.234,56", BrNumber.formatCents(123456))
        assertEquals("R$ 1.000.000,00", BrNumber.formatCents(100_000_000))
        assertEquals("-R$ 14,65", BrNumber.formatCents(-1465))
    }

    @Test
    @DisplayName("Escreve decimais com vírgula")
    fun formatsDecimal() {
        assertEquals("2,57", BrNumber.formatDecimal(2.565))
        assertEquals("62,0", BrNumber.formatDecimal(62.0, 1))
    }
}
