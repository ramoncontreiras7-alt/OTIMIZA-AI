package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.PlatformFinancialConfig
import com.otimizaai.domain.model.RouteOffer
import com.otimizaai.domain.model.RouteOfferEvaluation
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.util.BrNumber
import javax.inject.Inject

/**
 * Avalia uma oferta COM km conhecido: custo do veículo + regras da plataforma.
 *
 *  1. Conta de custo de sempre (combustível + custos fixos) com [CalculateRouteProfitUseCase].
 *  2. Regras ativas da plataforma via [PricingRuleEngine] (galpão, R$/km, R$/hora, bairros).
 *  3. Viável = nenhuma regra reprovou E o lucro depois dos custos não é negativo.
 *
 * Para ofertas lidas da tela (que podem não ter km), use [EvaluateOfferViabilityUseCase].
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
        val outcome = PricingRuleEngine.evaluate(
            rules = configs.filter { it.platform == offer.platform }.flatMap { it.activeRules },
            warehouse = offer.warehouse,
            neighborhoods = offer.deliveryNeighborhoods,
            freightCents = offer.revenueCents,
            distanceMeters = offer.distanceMeters,
            durationSeconds = offer.durationSeconds,
        )
        val reasons = buildList {
            outcome.failed.forEach { add(it.detail) }
            if (economics.netProfitCents < 0) {
                add("Dá prejuízo: depois de combustível e custos do veículo sobram ${BrNumber.formatCents(economics.netProfitCents)}.")
            }
        }
        return RouteOfferEvaluation(
            economics = economics,
            checks = outcome.checks,
            neighborhoodBonusCents = outcome.neighborhoodBonusCents,
            requiredMinimumCents = outcome.requiredMinimumCents,
            isViable = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    /**
     * Avalia uma rota já montada com paradas de UMA plataforma, usando o galpão e os
     * bairros gravados nas paradas. Paradas que falharam não contam.
     * Rotas com várias plataformas: avalie cada oferta antes de aceitar.
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
}
