package com.otimizaai.domain.messaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Dados fictícios (não usar dados reais de clientes em testes). */
class CustomerMessageTest {

    private val ctx = MessageContext(
        customerName = "MARIA SILVA SOUZA",
        driverName = "Ramon",
        platformName = "Mercado Livre",
        orderId = "100228996901",
        address = "Av. Exemplo, 100, apto 10",
        requiresCode = false,
    )

    @Test
    @DisplayName("Mensagem 'chegando' com nome, plataforma e pedido")
    fun arriving() {
        assertEquals(
            "Olá, Maria! Sou Ramon, entregador Mercado Livre. Estou chegando no seu endereço com a sua entrega (pedido 100228996901).",
            CustomerMessage.build(MessageMoment.CHEGANDO, ctx),
        )
    }

    @Test
    @DisplayName("Entrega com código: o lembrete é acrescentado obrigatoriamente")
    fun codeReminder() {
        val msg = CustomerMessage.build(MessageMoment.NO_ENDERECO, ctx.copy(requiresCode = true))
        assertTrue(msg.startsWith("Olá, Maria! Sou Ramon, entregador Mercado Livre. Já estou no seu endereço (Av. Exemplo, 100, apto 10)"))
        assertTrue(msg.endsWith("tenha o código em mãos para informar no momento da entrega."))
        assertFalse(CustomerMessage.build(MessageMoment.NO_ENDERECO, ctx).contains("código"))
    }

    @Test
    @DisplayName("Dados ausentes somem sem deixar buracos no texto")
    fun missingData() {
        val msg = CustomerMessage.build(MessageMoment.CHEGANDO, ctx.copy(customerName = null, platformName = null, orderId = null))
        assertEquals("Olá! Sou Ramon, entregador. Estou chegando no seu endereço com a sua entrega.", msg)
    }

    @Test
    @DisplayName("Texto personalizado pelo entregador")
    fun customTemplate() {
        val t = MessageTemplates(arriving = "Oi {cliente}, aqui é o {entregador}. Em 5 minutos chego aí!")
        assertEquals("Oi Maria, aqui é o Ramon. Em 5 minutos chego aí!", CustomerMessage.build(MessageMoment.CHEGANDO, ctx, t))
    }

    @Test
    @DisplayName("Link do WhatsApp com telefone brasileiro e texto codificado")
    fun link() {
        assertEquals("5585999990000", CustomerMessage.normalizeBrazilianPhone("85 99999-0000"))
        assertEquals("5585999990000", CustomerMessage.normalizeBrazilianPhone("+55 (85) 99999-0000"))
        assertNull(CustomerMessage.normalizeBrazilianPhone("1234"))
        val link = CustomerMessage.whatsAppLink("85 99999-0000", "Olá, Maria!")!!
        assertEquals("https://wa.me/5585999990000?text=Ol%C3%A1%2C%20Maria%21", link)
        assertNull(CustomerMessage.whatsAppLink("abc", "x"))
    }
}
