package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.PlatformFinancialConfig
import com.otimizaai.domain.model.PricingRule
import com.otimizaai.domain.model.RouteOffer
import com.otimizaai.domain.model.RouteOfferEvaluation
import com.otimizaai.domain.model.RuleCheck
import com.otimizaai.domain.model.RuleStatus
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.util.BrNumber
import com.otimizaai.domain.util.TextMatch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Motor financeiro com regras por plataforma (evolução do CPK).
 *
 * Passo a passo:
 *  1. Faz a conta de custo de sempre (combustível + custos fixos) com [CalculateRouteProfitUseCase].
 *  2. Pega as regras da plataforma da oferta.
 *  3. Soma os acréscimos de bairro (NeighborhoodBonus) das entregas.
 *  4. Confere cada regra:
 *       - MinimumRouteValue: só vale se a coleta for naquele galpão.
 *         Exige: valor >= mínimo do galpão + acréscimos de bairro.
 *       - ValuePerKm: exige valor >= (R$/km × km) + acréscimos de bairro.
 *  5. Viável = nenhuma regra reprovou E o lucro depois dos custos não é negativo.
 *
 * As faixas ruim/média/boa (ProfitRating) continuam valendo à parte, para a cor do cartão.
 */
class EvaluateRouteOfferUseCase @Inject constructor(
    private val calculateProfit: CalculateRouteProfitUseCase,
) {

    operator fun invoke(
        offer: RouteOffer,
        configs: List<PlatformFinancialConfig>,
        vehicle: VehicleProfile,
        fuelPriceCentsPerLiter: Int,
        minNetCentsPerKm: Int,
    ): RouteOfferEvaluation {
        val economics = calculateProfit(
            distanceMeters = offer.distanceMeters,
            revenueCents = offer.revenueCents,
            fuelPriceCentsPerLiter = fuelPriceCentsPerLiter,
            vehicle = vehicle,
            minNetCentsPerKm = minNetCentsPerKm,
            estimatedDurationSeconds = offer.durationSeconds,
        )

        val rules = configs.filter { it.enabled && it.platform == offer.platform }.flatMap { it.rules }
        val bonusRules = rules.filterIsInstance<PricingRule.NeighborhoodBonus>()

        // Acréscimos de bairro: cada entrega no bairro configurado soma o bônus.
        val bonusChecks = bonusRules.map { rule ->
            val hits = offer.deliveryNeighborhoods.count { TextMatch.sameName(it, rule.neighborhoodName) }
            if (hits > 0) {
                RuleCheck(rule, RuleStatus.PASSOU, rule.bonusCents * hits,
                    "$hits entrega(s) em ${rule.neighborhoodName}: + ${BrNumber.formatCents(rule.bonusCents * hits)} no mínimo exigido.")
            } else {
                RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Nenhuma entrega em ${rule.neighborhoodName}.")
            }
        }
        val bonusTotal = bonusChecks.sumOf { it.requiredCents ?: 0L }

        val valueChecks = rules.mapNotNull { rule ->
            when (rule) {
                is PricingRule.MinimumRouteValue -> checkWarehouse(rule, offer, bonusTotal)
                is PricingRule.ValuePerKm -> checkPerKm(rule, offer, bonusTotal)
                is PricingRule.NeighborhoodBonus -> null
            }
        }

        val checks = valueChecks + bonusChecks
        val failed = checks.filter { it.status == RuleStatus.FALHOU }
        val reasons = buildList {
            failed.forEach { add(it.detail) }
            if (economics.netProfitCents < 0) {
                add("Dá prejuízo: depois de combustível e custos do veículo sobram ${BrNumber.formatCents(economics.netProfitCents)}.")
            }
        }
        return RouteOfferEvaluation(
            economics = economics,
            checks = checks,
            neighborhoodBonusCents = bonusTotal,
            requiredMinimumCents = valueChecks.filter { it.status != RuleStatus.NAO_SE_APLICA }.mapNotNull { it.requiredCents }.maxOrNull(),
            isViable = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    /**
     * Avalia uma rota já montada com paradas de UMA plataforma (ex.: a rota do dia do ML).
     * Usa o galpão e os bairros gravados nas próprias paradas. Paradas que falharam não contam.
     * Rotas com várias plataformas: avalie cada oferta antes de aceitar (o km por plataforma
     * não é conhecido depois que tudo vira uma rota só).
     */
    fun forStops(
        stops: List<DeliveryStop>,
        distanceMeters: Long,
        configs: List<PlatformFinancialConfig>,
        vehicle: VehicleProfile,
        fuelPriceCentsPerLiter: Int,
        minNetCentsPerKm: Int,
        durationSeconds: Long? = null,
    ): RouteOfferEvaluation {
        val counted = stops.filter { it.status != DeliveryStatus.FAILED }
        val platforms = counted.map { it.platform }.distinct()
        require(platforms.size == 1) {
            "Avaliação por regras precisa de paradas de uma única plataforma (encontradas: ${platforms.size})."
        }
        val offer = RouteOffer(
            platform = platforms.single(),
            warehouse = counted.firstNotNullOfOrNull { it.warehouseName },
            revenueCents = counted.sumOf { it.freightCents.toLong() },
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            deliveryNeighborhoods = counted.mapNotNull { it.neighborhood },
        )
        return invoke(offer, configs, vehicle, fuelPriceCentsPerLiter, minNetCentsPerKm)
    }

    private fun checkWarehouse(rule: PricingRule.MinimumRouteValue, offer: RouteOffer, bonus: Long): RuleCheck {
        val warehouse = offer.warehouse
        if (warehouse == null || !TextMatch.containsWord(warehouse, rule.warehouseId)) {
            return RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Coleta não é no ${rule.warehouseId}.")
        }
        val required = rule.minValueCents + bonus
        return if (offer.revenueCents >= required) {
            RuleCheck(rule, RuleStatus.PASSOU, required,
                "Galpão ${rule.warehouseId}: paga ${BrNumber.formatCents(offer.revenueCents)}, mínimo ${BrNumber.formatCents(required)}.")
        } else {
            RuleCheck(rule, RuleStatus.FALHOU, required,
                "Galpão ${rule.warehouseId}: paga ${BrNumber.formatCents(offer.revenueCents)}, abaixo do mínimo de ${BrNumber.formatCents(required)}.")
        }
    }

    private fun checkPerKm(rule: PricingRule.ValuePerKm, offer: RouteOffer, bonus: Long): RuleCheck {
        val base = BigDecimal.valueOf(offer.distanceMeters)
            .multiply(BigDecimal.valueOf(rule.priceCentsPerKm.toLong()))
            .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
            .toLong()
        val required = base + bonus
        val km = BrNumber.formatDecimal(offer.distanceMeters / 1000.0, 1)
        val perKm = BrNumber.formatCents(rule.priceCentsPerKm.toLong())
        return if (offer.revenueCents >= required) {
            RuleCheck(rule, RuleStatus.PASSOU, required,
                "$km km × $perKm = mínimo ${BrNumber.formatCents(required)}; paga ${BrNumber.formatCents(offer.revenueCents)}.")
        } else {
            RuleCheck(rule, RuleStatus.FALHOU, required,
                "$km km × $perKm exige ${BrNumber.formatCents(required)}, mas a oferta paga ${BrNumber.formatCents(offer.revenueCents)}.")
        }
    }
}
