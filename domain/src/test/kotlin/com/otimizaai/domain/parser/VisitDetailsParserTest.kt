package com.otimizaai.domain.parser

import com.otimizaai.domain.messaging.CustomerMessage
import com.otimizaai.domain.messaging.MessageContext
import com.otimizaai.domain.messaging.MessageMoment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Formato da tela "Detalhes da visita" (Magalu Entregas). Dados de cliente FICTÍCIOS. */
class VisitDetailsParserTest {

    private val screen = listOf(
        "Detalhes da visita", "PRINCIPAL", "FOTOS", "OMS CENTAURO", "13° - MARIA EXEMPLO SOUZA", "85 99999-0000",
        "60000-000 - BAIRRO TESTE", "AV EXEMPLO 100 APT 10", "Fortaleza/CE",
        "ENTREGA 307535809 (21177067)", "Volumes: 1", "NF: 17814106", "PEDIDO: 100228996901", "Carga: 6600372",
        "Entregue", "VISITA FINALIZADA",
    ).map { ScreenText(it) }

    @Test
    @DisplayName("Lê cliente, telefone, endereço e os IDs da entrega sem alterar")
    fun parse() {
        val v = VisitDetailsParser.parse(screen)
        assertEquals("OMS CENTAURO", v.shipper)
        assertEquals(13, v.sequence)
        assertEquals("MARIA EXEMPLO SOUZA", v.customerName)
        assertEquals("85 99999-0000", v.phone)
        assertEquals("60000-000", v.postalCode)
        assertEquals("BAIRRO TESTE", v.neighborhood)
        assertEquals("AV EXEMPLO 100 APT 10", v.street)
        assertEquals("Fortaleza/CE", v.city)
        val d = v.deliveries.single()
        assertEquals("307535809", d.nativeId)
        assertEquals("21177067", d.secondaryId)
        assertEquals(1, d.volumes)
        assertEquals("17814106", d.invoice)
        assertEquals("100228996901", d.orderId)
        assertEquals("6600372", d.load)
        assertFalse(v.requiresCode)
    }

    @Test
    @DisplayName("Tela que pede código de entrega liga o lembrete no WhatsApp")
    fun codeAndWhatsApp() {
        val v = VisitDetailsParser.parse(screen + ScreenText("Informe o código de entrega ao concluir"))
        assertTrue(v.requiresCode)
        val msg = CustomerMessage.build(
            MessageMoment.NO_ENDERECO,
            MessageContext(v.customerName, "Ramon", "Magalu", v.deliveries.single().orderId, "${v.street}, ${v.neighborhood}", v.requiresCode),
        )
        assertTrue(msg.startsWith("Olá, Maria! Sou Ramon, entregador Magalu."))
        assertTrue(msg.contains("código em mãos"))
        assertTrue(CustomerMessage.whatsAppLink(v.phone!!, msg)!!.startsWith("https://wa.me/5585999990000?text="))
    }

    @Test
    @DisplayName("Várias entregas na mesma visita")
    fun multipleDeliveries() {
        val extra = listOf("ENTREGA 307535810", "Volumes: 2", "PEDIDO: 100228996902").map { ScreenText(it) }
        val v = VisitDetailsParser.parse(screen + extra)
        assertEquals(listOf("307535809", "307535810"), v.deliveries.map { it.nativeId })
        assertEquals(2, v.deliveries[1].volumes)
    }
}
