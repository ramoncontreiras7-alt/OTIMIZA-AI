package com.otimizaai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * REGRA #1: o ID original da plataforma nunca é alterado, cortado ou "limpo".
 */
class IdentityRuleTest {

    private val session = RouteSessionId("2026-10-05")
    private val address = DeliveryAddress("R. Barão de Aracati, 210")

    @Test
    @DisplayName("ID é guardado exatamente como veio (maiúsculas, zeros, símbolos)")
    fun idIsKeptVerbatim() {
        val raw = "mlb-0004419027731/Ç#a"
        assertEquals(raw, NativeStopId(raw).value)
    }

    @Test
    @DisplayName("ID com espaço nas bordas é recusado, nunca 'corrigido'")
    fun paddedIdIsRejected() {
        assertThrows<IllegalArgumentException> { NativeStopId(" MLB-1") }
        assertThrows<IllegalArgumentException> { NativeStopId("MLB-1 ") }
        assertThrows<IllegalArgumentException> { NativeStopId("") }
        assertThrows<IllegalArgumentException> { NativeStopId("   ") }
        assertThrows<IllegalArgumentException> { PlatformId(" IFOOD") }
    }

    @Test
    @DisplayName("Mesmo ID em plataformas diferentes são paradas diferentes")
    fun sameIdDifferentPlatformsAreDifferentKeys() {
        val a = DeliveryStop.create(NativeStopId("12345"), Platform.IFOOD, session, address)
        val b = DeliveryStop.create(NativeStopId("12345"), Platform.MERCADO_LIVRE, session, address)
        assertNotEquals(a.key, b.key)
    }

    @Test
    @DisplayName("Toda alteração permitida preserva ID e plataforma")
    fun allChangesPreserveKey() {
        val original = DeliveryStop.create(
            NativeStopId("TBA318845120009"), Platform.AMAZON_FLEX, session, address,
            coordinate = GeoCoordinate(-3.7319, -38.5267), freightCents = 1_200,
        )
        val changed = original
            .withStatus(DeliveryStatus.FAILED)
            .withFreightCents(1_500)
            .withCoordinate(GeoCoordinate(-3.74, -38.53))
            .withAddress(DeliveryAddress("R. Tibúrcio Cavalcante, 77"))
            .transferTo(RouteSessionId("2026-10-06"))

        assertEquals(original.key, changed.key)
        assertEquals("TBA318845120009", changed.id.value)
        assertEquals(Platform.AMAZON_FLEX, changed.platform)
    }

    @Test
    @DisplayName("Reentrega: troca a jornada e volta para PENDENTE")
    fun transferResetsStatus() {
        val failed = DeliveryStop.create(NativeStopId("MLB-9"), Platform.MERCADO_LIVRE, session, address, status = DeliveryStatus.FAILED)
        val moved = failed.transferTo(RouteSessionId("2026-10-06"))
        assertEquals("2026-10-06", moved.sessionId.value)
        assertEquals(DeliveryStatus.PENDING, moved.status)
    }

    @Test
    @DisplayName("Trocar o endereço apaga a coordenada antiga")
    fun newAddressClearsCoordinate() {
        val stop = DeliveryStop.create(NativeStopId("IFD-1"), Platform.IFOOD, session, address, coordinate = GeoCoordinate(-3.7, -38.5))
        val edited = stop.withAddress(DeliveryAddress("Outra rua, 10"))
        assertNull(edited.coordinate)
        assertEquals(false, edited.isRoutable)
    }

    @Test
    @DisplayName("Frete negativo e coordenada impossível são recusados")
    fun invalidValuesAreRejected() {
        assertThrows<IllegalArgumentException> {
            DeliveryStop.create(NativeStopId("X"), Platform.IFOOD, session, address, freightCents = -1)
        }
        assertThrows<IllegalArgumentException> { GeoCoordinate(-91.0, 0.0) }
        assertThrows<IllegalArgumentException> { GeoCoordinate(0.0, 181.0) }
        assertThrows<IllegalArgumentException> { DeliveryAddress("  ") }
    }

    @Test
    @DisplayName("Plataformas são lidas pelo código gravado; código desconhecido falha alto")
    fun platformCodes() {
        Platform.entries.forEach { assertEquals(it, Platform.fromId(it.id)) }
        assertEquals(Platform.AMAZON_FLEX, Platform.fromId(PlatformId("AMAZON_FLEX")))
        assertNull(Platform.fromIdOrNull(PlatformId("SHOPEE")))
        assertThrows<UnknownPlatformException> { Platform.fromId(PlatformId("SHOPEE")) }
        assertEquals(DeliveryStatus.DELIVERED, DeliveryStatus.fromCode("DELIVERED"))
        assertThrows<IllegalArgumentException> { DeliveryStatus.fromCode("delivered") }
    }
}
