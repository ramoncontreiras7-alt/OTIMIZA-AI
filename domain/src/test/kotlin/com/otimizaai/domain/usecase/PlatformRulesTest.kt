package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.PlatformFinancialConfig
import com.otimizaai.domain.model.PricingRule
import com.otimizaai.domain.model.RouteOffer
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.RuleStatus
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.model.VehicleType
import com.otimizaai.domain.parser.FlexOfferListParser
import com.otimizaai.domain.parser.RouteDetailScreenParser
import com.otimizaai.domain.parser.ScreenText
import com.otimizaai.domain.util.TextMatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Regras por plataforma, testadas com os textos reais dos prints do Ramon (05/10/2026). */
class PlatformRulesTest {

    private val evaluate = EvaluateRouteOfferUseCase(CalculateRouteProfitUseCase())
    private val car = VehicleProfile(VehicleType.CAR, consumptionKmPerLiter = 11.0, wearCostCentsPerKm = 25)

    /** Tela "Valor da rota R$ 40,59 · Percurso 19Km · 4 entregas", coleta na Petz Maraponga. */
    private val petzScreen = listOf(
        "PETZ *** LOJA 246 MRPG-CE ...", "Valor da rota", "R$ 40,59", "Percurso", "19Km",
        "Entregas", "04", "Retorno à Loja", "Não", "Coleta · 6Km ·",
        "PETZ *** LOJA 246 MRPG-CE MARAPONGA AV GODOFREDO MACIEL, 2560 - Maraponga, Fortaleza, CE - CEP 60710-684",
        "Entrega", "Maraponga", "Entrega", "Jóquei Clube", "Entrega", "Montese", "Entrega", "Autran Nunes", "Aceitar corrida",
    ).map { ScreenText(it) }

    @Test
    @DisplayName("Leitor de tela: extrai valor, km, coleta, entregas e bairros do print real")
    fun parsesRouteDetail() {
        val p = RouteDetailScreenParser.parse(petzScreen)
        assertEquals(4059L, p.revenueCents)
        assertEquals(19.0, p.routeKm)
        assertEquals(6.0, p.pickupKm)
        assertEquals(4, p.deliveries)
        assertEquals(false, p.returnToStore)
        assertTrue(p.warehouse!!.startsWith("PETZ *** LOJA 246 MRPG-CE MARAPONGA"))
        assertEquals(listOf("Maraponga", "Jóquei Clube", "Montese", "Autran Nunes"), p.neighborhoods)

        val offer = p.toRouteOffer(Platform.LALAMOVE)!!
        assertEquals(25_000L, offer.distanceMeters) // 19 km de rota + 6 km até a coleta
        assertEquals(19_000L, p.toRouteOffer(Platform.LALAMOVE, includePickup = false)!!.distanceMeters)
    }

    @Test
    @DisplayName("Leitor do Amazon Flex: estação, janela, duração e valor de cada bloco")
    fun parsesFlexList() {
        val lines = listOf(
            "OFERTAS", "Filtro", "15 de 15 ofertas", "segunda-feira, 05/10",
            "Fortaleza (DCE5) - Amazon.com.br", "19:15 - 22:45", "3 hora 30 minuto", "R$ 222,50",
            "Fortaleza (DCE5) - Amazon.com.br", "20 - 23", "3 hora", "R$ 159,00",
            "terça-feira, 06/10",
            "Fortaleza (DCE5) - Amazon.com.br", "19 - 22:30", "3 hora 30 minuto", "R$ 122,50",
        ).map { ScreenText(it) }
        val blocks = FlexOfferListParser.parse(lines)
        assertEquals(3, blocks.size)
        assertEquals("DCE5", blocks[0].stationCode)
        assertEquals("19:15 - 22:45", blocks[0].window)
        assertEquals(210, blocks[0].durationMinutes)
        assertEquals(22_250L, blocks[0].valueCents)
        assertEquals(180, blocks[1].durationMinutes)
        assertEquals(12_250L, blocks[2].valueCents)
    }

    @Test
    @DisplayName("Galpão: rota do BRNCE20 abaixo de R$ 80 é reprovada; outro galpão não é afetado")
    fun warehouseMinimum() {
        val configs = listOf(PlatformFinancialConfig(Platform.MERCADO_LIVRE, listOf(PricingRule.MinimumRouteValue("BRNCE20", 8_000))))
        val fromBrnce20 = RouteOffer(Platform.MERCADO_LIVRE, "BRNCE20 - Agencia Mercado Livre - ECOPRINT", 7_500, 30_000)
        val r1 = evaluate(fromBrnce20, configs, car, 629, 200)
        assertEquals(RuleStatus.FALHOU, r1.checks.single().status)
        assertEquals(8_000L, r1.requiredMinimumCents)
        assertFalse(r1.isViable)

        val r2 = evaluate(fromBrnce20.copy(revenueCents = 9_000), configs, car, 629, 200)
        assertEquals(RuleStatus.PASSOU, r2.checks.single().status)
        assertTrue(r2.isViable)

        // "BRNCE203" NÃO é o galpão "BRNCE20"
        val other = evaluate(fromBrnce20.copy(warehouse = "BRNCE203 - Outra agência"), configs, car, 629, 200)
        assertEquals(RuleStatus.NAO_SE_APLICA, other.checks.single().status)
        assertNull(other.requiredMinimumCents)
    }

    @Test
    @DisplayName("Valor por km (Magalu): 19 km × R$ 2,50 exige R$ 47,50")
    fun valuePerKm() {
        val configs = listOf(PlatformFinancialConfig(Platform.MAGALU_ULTRA, listOf(PricingRule.ValuePerKm(250))))
        val offer = RouteOffer(Platform.MAGALU_ULTRA, null, 4_059, 19_000)
        val r = evaluate(offer, configs, car, 629, 200)
        assertEquals(4_750L, r.checks.single().requiredCents)
        assertEquals(RuleStatus.FALHOU, r.checks.single().status)
        assertFalse(r.isViable)
        assertTrue(r.reasons.single().contains("R$ 47,50"))
    }

    @Test
    @DisplayName("Bairro: cada entrega no bairro configurado soma no mínimo exigido (sem acento/maiúscula)")
    fun neighborhoodBonus() {
        val configs = listOf(
            PlatformFinancialConfig(
                Platform.LALAMOVE,
                listOf(
                    PricingRule.ValuePerKm(150),
                    PricingRule.NeighborhoodBonus("joquei clube", 500),
                    PricingRule.NeighborhoodBonus("Aldeota", 300),
                ),
            ),
        )
        val offer = RouteDetailScreenParser.parse(petzScreen).toRouteOffer(Platform.LALAMOVE)!!
        val r = evaluate(offer, configs, car, 629, 200)
        assertEquals(500L, r.neighborhoodBonusCents)
        // 25 km × R$ 1,50 = R$ 37,50 + R$ 5,00 do Jóquei Clube = R$ 42,50 > R$ 40,59
        assertEquals(4_250L, r.requiredMinimumCents)
        assertEquals(RuleStatus.FALHOU, r.checks.first { it.rule is PricingRule.ValuePerKm }.status)
        assertEquals(RuleStatus.NAO_SE_APLICA, r.checks.first { (it.rule as? PricingRule.NeighborhoodBonus)?.neighborhoodName == "Aldeota" }.status)
        assertFalse(r.isViable)
    }

    @Test
    @DisplayName("Sem regras, a viabilidade depende só de não dar prejuízo após combustível e custos")
    fun noRulesUsesCostOnly() {
        val lucro = evaluate(RouteOffer(Platform.IFOOD, null, 4_059, 25_000), emptyList(), car, 629, 200)
        assertTrue(lucro.isViable) // 25 km: combustível ≈ R$ 14,30 + custos R$ 6,25 < R$ 40,59
        val prejuizo = evaluate(RouteOffer(Platform.IFOOD, null, 1_000, 25_000), emptyList(), car, 629, 200)
        assertFalse(prejuizo.isViable)
        assertTrue(prejuizo.reasons.single().startsWith("Dá prejuízo"))
    }

    @Test
    @DisplayName("Regras de outra plataforma ou desligadas não interferem")
    fun otherPlatformIgnored() {
        val configs = listOf(
            PlatformFinancialConfig(Platform.MAGALU_ULTRA, listOf(PricingRule.ValuePerKm(9_999))),
            PlatformFinancialConfig(Platform.IFOOD, listOf(PricingRule.ValuePerKm(9_999)), enabled = false),
        )
        val r = evaluate(RouteOffer(Platform.IFOOD, null, 4_059, 19_000), configs, car, 629, 200)
        assertTrue(r.checks.isEmpty())
        assertTrue(r.isViable)
    }

    @Test
    @DisplayName("Rota montada com paradas: usa galpão e bairro das paradas; falhas não contam")
    fun forStops() {
        val session = RouteSessionId("2026-10-05")
        fun stop(id: String, bairro: String, frete: Int, status: DeliveryStatus = DeliveryStatus.PENDING) = DeliveryStop.create(
            NativeStopId(id), Platform.MERCADO_LIVRE, session,
            DeliveryAddress("Rua X, 1", neighborhood = bairro), status = status, freightCents = frete,
            warehouseName = "BRNCE20 - Agencia Mercado Livre - ECOPRINT",
        )
        val stops = listOf(stop("MLB-1", "Maraponga", 3_000), stop("MLB-2", "Montese", 3_000), stop("MLB-3", "Pici", 5_000, DeliveryStatus.FAILED))
        val configs = listOf(PlatformFinancialConfig(Platform.MERCADO_LIVRE, listOf(PricingRule.MinimumRouteValue("BRNCE20", 8_000))))
        val r = evaluate.forStops(stops, 20_000, configs, car, 629, 200)
        assertEquals(6_000L, r.economics.revenueCents) // a parada que falhou não conta
        assertEquals(RuleStatus.FALHOU, r.checks.single().status)

        val mixed = stops + DeliveryStop.create(NativeStopId("IFD-9"), Platform.IFOOD, session, DeliveryAddress("Rua Y, 2"))
        assertThrows<IllegalArgumentException> { evaluate.forStops(mixed, 20_000, configs, car, 629, 200) }
    }

    @Test
    @DisplayName("Comparação de nomes ignora acento e maiúsculas, e galpão casa como palavra inteira")
    fun textMatch() {
        assertTrue(TextMatch.sameName("Jóquei  Clube", "joquei clube"))
        assertTrue(TextMatch.containsWord("SCE3 - SCE3 - Itaitinga", "sce3"))
        assertTrue(TextMatch.containsWord("Fortaleza (DCE5) - Amazon.com.br", "DCE5"))
        assertFalse(TextMatch.containsWord("BRNCE203 - X", "BRNCE20"))
    }

    @Test
    @DisplayName("Regras com valores impossíveis são recusadas")
    fun invalidRules() {
        assertThrows<IllegalArgumentException> { PricingRule.MinimumRouteValue(" ", 8_000) }
        assertThrows<IllegalArgumentException> { PricingRule.MinimumRouteValue("BRNCE20", 0) }
        assertThrows<IllegalArgumentException> { PricingRule.ValuePerKm(0) }
        assertThrows<IllegalArgumentException> { PricingRule.NeighborhoodBonus("Pici", -1) }
    }
}
