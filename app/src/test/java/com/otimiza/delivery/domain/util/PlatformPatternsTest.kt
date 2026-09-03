package com.otimiza.delivery.domain.util

import com.otimiza.delivery.domain.model.Platform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlatformPatternsTest {

    @BeforeEach
    fun reset() {
        PlatformPatterns.clearCache()
    }

    @Test
    fun `deve identificar corretamente o ID nativo e a plataforma iFood`() {
        val result = PlatformPatterns.identifyPlatformAndId("Pedido IFOOD-123456 entregue")
        assertNotNull(result)
        assertEquals(Platform.IFOOD, result?.first)
        assertEquals("IFOOD-123456", result?.second)
    }

    @Test
    fun `deve identificar Mercado Livre com prefixo MLB`() {
        val result = PlatformPatterns.identifyPlatformAndId("MLB123456789")
        assertNotNull(result)
        assertEquals(Platform.MERCADO_LIVRE, result?.first)
        assertEquals("MLB123456789", result?.second)
    }

    @Test
    fun `deve identificar Lalamove`() {
        val result = PlatformPatterns.identifyPlatformAndId("LALA-999888")
        assertNotNull(result)
        assertEquals(Platform.LALAMOVE, result?.first)
        assertEquals("LALA-999888", result?.second)
    }

    @Test
    fun `deve retornar null para texto sem ID nativo`() {
        val result = PlatformPatterns.identifyPlatformAndId("Rua das Flores, 123 - sem pedido")
        assertNull(result)
    }

    @Test
    fun `deve aplicar deduplicacao temporal corretamente`() {
        val id = "LALA-999888"
        val now = 1_000L

        assertFalse(PlatformPatterns.isDuplicate(id, now))
        assertTrue(PlatformPatterns.isDuplicate(id, now + 10_000L))
        assertFalse(PlatformPatterns.isDuplicate(id, now + 35_000L))
    }

    @Test
    fun `IDs distintos nao devem ser deduplicados`() {
        val now = 1_000L
        assertFalse(PlatformPatterns.isDuplicate("IFOOD-1", now))
        assertFalse(PlatformPatterns.isDuplicate("IFOOD-2", now + 5_000L))
        assertFalse(PlatformPatterns.isDuplicate("MLB123456789", now + 10_000L))
    }
}