package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.CapturedOffer
import com.otimizaai.domain.model.OfferEvaluation
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.PlatformFinancialConfig
import com.otimizaai.domain.model.PricingRule
import com.otimizaai.domain.model.RuleStatus
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.model.VehicleType
import com.otimizaai.domain.parser.CaptureOptions
import com.otimizaai.domain.parser.IdPattern
import com.otimizaai.domain.parser.LogisticRouteBlockParser
import com.otimizaai.domain.parser.ScreenCaptureProcessor
import com.otimizaai.domain.parser.ScreenText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Viabilidade de ofertas capturadas, com os textos dos prints do Ramon (05/10/2026). */
class OfferViabilityTest {

    private val evaluate = EvaluateOfferViabilityUseCase(CalculateRouteProfitUseCase())
    private val car = VehicleProfile(VehicleType.CAR, 11.0, 25)
    private val packages = mapOf("app.ml" to Platform.MERCADO_LIVRE, "app.flex" to Platform.AMAZON_FLEX, "app.loja" to Platform.LALAMOVE)

    /** Lista "Disponíveis" do ML: R$ 270 (antes R$ 225) e R$ 155. */
    private val meliList = listOf(
        "Hoje", "Segunda-feira", "2 disponíveis", "Melhor tarifa por hora",
        "Rota logística", "11:00 a 17:00 h (6 h)", "Início em Itaitinga, Itaitinga", "R$ 270", "R$ 225", "120 PONTOS",
        "Rota logística", "R$ 155", "12:00 a 16:00 h (4 h)", "Início em SELECT LAVA JATO, Fortaleza", "Resta 1 vaga", "100 PONTOS",
    ).map { ScreenText(it) }

    @Test
    @DisplayName("ML Rota logística: lê valor atual (ignora o riscado), horas, início e pontos")
    fun parsesMeliBlocks() {
        val b = LogisticRouteBlockParser.parse(meliList)
        assertEquals(2, b.size)
        assertEquals(27_000L, b[0].valueCents)
        assertEquals(360, b[0].durationMinutes)
        assertEquals("Itaitinga, Itaitinga", b[0].startPlace)
        assertEquals(120, b[0].points)
        assertEquals(15_500L, b[1].valueCents)
        assertEquals("12:00 - 16:00", b[1].window)
        assertEquals("SELECT LAVA JATO, Fortaleza", b[1].startPlace)
    }

    @Test
    @DisplayName("ML detalhe: SCE3, 0:00 às 4:00 h (4 h), R$ 150")
    fun parsesMeliDetail() {
        val detail = listOf(
            "Rota logística", "R$ 150", "100 PONTOS", "Domingo 4 de outubro", "0:00 às 4:00 h (4 h)",
            "Início em SCE3 - Itaitinga, Itaitinga", "Rodovia Quarto Anel Viário 3955", "Agendar",
        ).map { ScreenText(it) }
        val b = LogisticRouteBlockParser.parse(detail).single()
        assertEquals(15_000L, b.valueCents)
        assertEquals(240, b.durationMinutes)
        assertEquals("SCE3 - Itaitinga, Itaitinga", b.startPlace)
    }

    @Test
    @DisplayName("Galpão SCE3 com mínimo R$ 160: bloco de R$ 150 é REJEITADO com motivo")
    fun rejectsBelowWarehouseMinimum() {
        val configs = listOf(PlatformFinancialConfig(Platform.MERCADO_LIVRE, listOf(PricingRule.MinimumRouteValue("SCE3", 16_000))))
        val offer = CapturedOffer(Platform.MERCADO_LIVRE, 15_000, warehouse = "SCE3 - Itaitinga, Itaitinga", durationMinutes = 240)
        val r = evaluate(offer, configs, car, 629, 200)
        assertTrue(r is OfferEvaluation.Rejected)
        assertTrue((r as OfferEvaluation.Rejected).reasons.single().contains("R$ 160,00"))
        assertNull(r.economics) // sem km: sem conta de combustível
        assertTrue(r.notes.first().startsWith("Oferta sem km"))
    }

    @Test
    @DisplayName("R$/hora: R$ 270 em 6 h passa com mínimo R$ 40/h; R$ 155 em 4 h não passa com R$ 40/h")
    fun perHour() {
        val configs = listOf(PlatformFinancialConfig(Platform.MERCADO_LIVRE, listOf(PricingRule.MinimumValuePerHour(4_000))))
        val good = evaluate(CapturedOffer(Platform.MERCADO_LIVRE, 27_000, durationMinutes = 360), configs, car, 629, 200)
        assertTrue(good is OfferEvaluation.Viable)
        val bad = evaluate(CapturedOffer(Platform.MERCADO_LIVRE, 15_500, durationMinutes = 240), configs, car, 629, 200)
        assertTrue(bad is OfferEvaluation.Rejected)
        assertEquals(16_000L, bad.requiredMinimumCents)
    }

    @Test
    @DisplayName("Multiplicador de bairro: vale o maior, aplicado sobre os mínimos, e soma com acréscimo fixo")
    fun multiplier() {
        val configs = listOf(
            PlatformFinancialConfig(
                Platform.LALAMOVE,
                listOf(
                    PricingRule.ValuePerKm(150),
                    PricingRule.NeighborhoodMultiplier("Montese", 1.2),
                    PricingRule.NeighborhoodMultiplier("Jóquei Clube", 1.5),
                    PricingRule.NeighborhoodBonus("Autran Nunes", 200),
                ),
            ),
        )
        val offer = CapturedOffer(
            Platform.LALAMOVE, 4_059, warehouse = "PETZ *** LOJA 246",
            neighborhoods = listOf("Maraponga", "Jóquei Clube", "Montese", "Autran Nunes"), estimatedKm = 25.0,
        )
        val r = evaluate(offer, configs, car, 629, 200)
        // 25 km × R$ 1,50 = R$ 37,50 × 1,5 = R$ 56,25 + R$ 2,00 = R$ 58,25
        assertEquals(5_825L, r.requiredMinimumCents)
        assertTrue(r is OfferEvaluation.Rejected)
        assertTrue(r.notes.any { it.contains("× 1,50") })
        assertTrue(r.economics != null)
    }

    @Test
    @DisplayName("Regra desligada pelo entregador não conta")
    fun disabledRule() {
        val configs = listOf(PlatformFinancialConfig(Platform.MERCADO_LIVRE, listOf(PricingRule.MinimumRouteValue("SCE3", 99_999, enabled = false))))
        val r = evaluate(CapturedOffer(Platform.MERCADO_LIVRE, 15_000, warehouse = "SCE3 - Itaitinga"), configs, car, 629, 200)
        assertTrue(r is OfferEvaluation.Viable)
        assertTrue(r.checks.isEmpty())
    }

    @Test
    @DisplayName("Com km, dar prejuízo reprova mesmo sem regras")
    fun lossWithKm() {
        val r = evaluate(CapturedOffer(Platform.IFOOD, 1_000, estimatedKm = 25.0), emptyList(), car, 629, 200)
        assertTrue(r is OfferEvaluation.Rejected)
        assertTrue((r as OfferEvaluation.Rejected).reasons.single().startsWith("Dá prejuízo"))
    }

    @Test
    @DisplayName("Processador: reconhece a tela, aplica as opções e guarda IDs sem alterar")
    fun processorWithOptions() {
        val ml = ScreenCaptureProcessor.process("app.ml", meliList, packages)
        assertEquals(2, ml.size)
        assertEquals(Platform.MERCADO_LIVRE, ml[0].platform)
        assertEquals("Itaitinga, Itaitinga", ml[0].warehouse)

        val noWarehouse = ScreenCaptureProcessor.process("app.ml", meliList, packages, CaptureOptions(captureWarehouse = false, captureDuration = false))
        assertNull(noWarehouse[0].warehouse)
        assertNull(noWarehouse[0].durationMinutes)

        val flex = ScreenCaptureProcessor.process(
            "app.flex",
            listOf("Fortaleza (DCE5) - Amazon.com.br", "ROD SANTOS-DUMONT, S/N", "segunda-feira, 05/10", "20 - 23 • (3 hora)", "R$ 159,00",
                "O pagamento da Amazon é de R$ 159,00 para a entrega desse bloco. Pedido TBA318845120009").map { ScreenText(it) },
            packages,
            idPatterns = listOf(IdPattern(Platform.AMAZON_FLEX, Regex("""TBA\d{12}"""))),
        ).single()
        assertEquals(15_900L, flex.freightCents)
        assertEquals(180, flex.durationMinutes)
        assertTrue(flex.warehouse!!.contains("DCE5"))
        assertEquals(listOf("TBA318845120009"), flex.nativeIds)

        val loja = ScreenCaptureProcessor.process(
            "app.loja",
            listOf("Valor da rota", "R$ 40,59", "Percurso", "19Km", "Coleta · 6Km ·", "PETZ *** LOJA 246", "Entrega", "Maraponga").map { ScreenText(it) },
            packages,
        ).single()
        assertEquals(25.0, loja.estimatedKm)
        assertEquals(listOf("Maraponga"), loja.neighborhoods)

        assertTrue(ScreenCaptureProcessor.process("app.desconhecido", meliList, packages).isEmpty())
    }

    @Test
    @DisplayName("Valores impossíveis são recusados")
    fun invalid() {
        assertThrows<IllegalArgumentException> { PricingRule.NeighborhoodMultiplier("Pici", 0.5) }
        assertThrows<IllegalArgumentException> { PricingRule.MinimumValuePerHour(0) }
        assertThrows<IllegalArgumentException> { CapturedOffer(Platform.IFOOD, -1) }
        assertThrows<IllegalArgumentException> { CapturedOffer(Platform.IFOOD, 100, estimatedKm = 0.0) }
        assertEquals(RuleStatus.FALHOU, RuleStatus.valueOf("FALHOU"))
    }
}
