package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.CapturedOffer
import com.otimizaai.domain.model.OfferEvaluation
import com.otimizaai.domain.model.PlatformFinancialConfig
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.util.BrNumber
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Avalia uma oferta CAPTURADA NA TELA e devolve [OfferEvaluation.Viable] ou
 * [OfferEvaluation.Rejected] com os motivos.
 *
 * Diferente de [EvaluateRouteOfferUseCase], aceita ofertas sem km (blocos por hora):
 *  - com km: calcula combustível + custos fixos e reprova se der prejuízo;
 *  - sem km: avalia só as regras (galpão, R$/hora, bairros) e avisa que o
 *    combustível não entrou na conta.
 */
class EvaluateOfferViabilityUseCase @Inject constructor(
    private val calculateProfit: CalculateRouteProfitUseCase,
) {

    operator fun invoke(
        offer: CapturedOffer,
        configs: List<PlatformFinancialConfig>,
        vehicle: VehicleProfile,
        fuelPriceCentsPerLiter: Int,
        minNetCentsPerKm: Int,
    ): OfferEvaluation {
        val distanceMeters = offer.estimatedKm?.let { (it * 1000).roundToLong() }
        val durationSeconds = offer.durationMinutes?.let { it * 60L }

        val economics = distanceMeters?.let {
            calculateProfit(
                distanceMeters = it,
                revenueCents = offer.freightCents,
                fuelPriceCentsPerLiter = fuelPriceCentsPerLiter,
                vehicle = vehicle,
                minNetCentsPerKm = minNetCentsPerKm,
                estimatedDurationSeconds = durationSeconds,
            )
        }

        val outcome = PricingRuleEngine.evaluate(
            rules = configs.filter { it.platform == offer.platform }.flatMap { it.activeRules },
            warehouse = offer.warehouse,
            neighborhoods = offer.neighborhoods,
            freightCents = offer.freightCents,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
        )

        val notes = buildList {
            if (economics == null) add("Oferta sem km: combustível e custos do veículo não entraram na conta. Informe um km estimado para avaliar o lucro.")
            if (outcome.neighborhoodMultiplier > 1.0) add("Multiplicador de bairro aplicado: × ${BrNumber.formatDecimal(outcome.neighborhoodMultiplier, 2)}.")
        }
        val reasons = buildList {
            outcome.failed.forEach { add(it.detail) }
            if (economics != null && economics.netProfitCents < 0) {
                add("Dá prejuízo: depois de combustível e custos do veículo sobram ${BrNumber.formatCents(economics.netProfitCents)}.")
            }
        }

        return if (reasons.isEmpty()) {
            OfferEvaluation.Viable(economics, outcome.checks, outcome.requiredMinimumCents, notes)
        } else {
            OfferEvaluation.Rejected(reasons, economics, outcome.checks, outcome.requiredMinimumCents, notes)
        }
    }
}
