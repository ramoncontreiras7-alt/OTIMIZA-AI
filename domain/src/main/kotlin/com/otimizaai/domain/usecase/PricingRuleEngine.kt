package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.PricingRule
import com.otimizaai.domain.model.RuleCheck
import com.otimizaai.domain.model.RuleStatus
import com.otimizaai.domain.util.BrNumber
import com.otimizaai.domain.util.TextMatch
import java.math.BigDecimal
import java.math.RoundingMode

/** Resultado das regras (sem a parte de custo do veículo). */
data class RulesOutcome(
    val checks: List<RuleCheck>,
    val neighborhoodBonusCents: Long,
    val neighborhoodMultiplier: Double,
    /** Maior mínimo exigido entre as regras de valor que se aplicaram (null se nenhuma). */
    val requiredMinimumCents: Long?,
) {
    val failed: List<RuleCheck> get() = checks.filter { it.status == RuleStatus.FALHOU }
}

/**
 * Aplica as regras de preço de uma plataforma a uma oferta. Funções puras, sem estado.
 *
 * Ordem:
 *  1. Bairros: soma os acréscimos fixos e acha o maior multiplicador.
 *  2. Mínimo de cada regra de valor = base × multiplicador + acréscimos.
 *       - Galpão:  base = mínimo do galpão (só se a coleta for nele)
 *       - R$/km:   base = R$/km × km          (só se houver km)
 *       - R$/hora: base = R$/hora × horas     (só se houver duração)
 *  3. Cada regra de valor PASSA se o frete >= mínimo dela.
 */
object PricingRuleEngine {

    fun evaluate(
        rules: List<PricingRule>,
        warehouse: String?,
        neighborhoods: List<String>,
        freightCents: Long,
        distanceMeters: Long?,
        durationSeconds: Long?,
    ): RulesOutcome {
        val active = rules.filter { it.enabled }

        val bonusChecks = active.filterIsInstance<PricingRule.NeighborhoodBonus>().map { rule ->
            val hits = neighborhoods.count { TextMatch.sameName(it, rule.neighborhoodName) }
            if (hits > 0) {
                RuleCheck(rule, RuleStatus.PASSOU, rule.bonusCents * hits,
                    "$hits entrega(s) em ${rule.neighborhoodName}: + ${BrNumber.formatCents(rule.bonusCents * hits)} no mínimo exigido.")
            } else {
                RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Nenhuma entrega em ${rule.neighborhoodName}.")
            }
        }
        val bonus = bonusChecks.sumOf { it.requiredCents ?: 0L }

        val multiplierChecks = active.filterIsInstance<PricingRule.NeighborhoodMultiplier>().map { rule ->
            if (neighborhoods.any { TextMatch.sameName(it, rule.neighborhoodName) }) {
                RuleCheck(rule, RuleStatus.PASSOU, null,
                    "Entrega em ${rule.neighborhoodName}: mínimos × ${BrNumber.formatDecimal(rule.multiplier, 2)}.")
            } else {
                RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Nenhuma entrega em ${rule.neighborhoodName}.")
            }
        }
        val multiplier = multiplierChecks
            .filter { it.status == RuleStatus.PASSOU }
            .maxOfOrNull { (it.rule as PricingRule.NeighborhoodMultiplier).multiplier } ?: 1.0

        fun required(base: Long): Long =
            BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.HALF_UP).toLong() + bonus

        val valueChecks = active.mapNotNull { rule ->
            when (rule) {
                is PricingRule.MinimumRouteValue -> {
                    if (warehouse == null || !TextMatch.containsWord(warehouse, rule.warehouseId)) {
                        RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Coleta não é no ${rule.warehouseId}.")
                    } else {
                        compare(rule, freightCents, required(rule.minValueCents), "Galpão ${rule.warehouseId}")
                    }
                }
                is PricingRule.ValuePerKm -> {
                    if (distanceMeters == null || distanceMeters <= 0) {
                        RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Sem km na oferta: R$/km não avaliado.")
                    } else {
                        val base = BigDecimal.valueOf(distanceMeters).multiply(BigDecimal.valueOf(rule.priceCentsPerKm.toLong()))
                            .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).toLong()
                        val km = BrNumber.formatDecimal(distanceMeters / 1000.0, 1)
                        compare(rule, freightCents, required(base), "$km km × ${BrNumber.formatCents(rule.priceCentsPerKm.toLong())}/km")
                    }
                }
                is PricingRule.MinimumValuePerHour -> {
                    if (durationSeconds == null || durationSeconds <= 0) {
                        RuleCheck(rule, RuleStatus.NAO_SE_APLICA, null, "Sem duração na oferta: R$/hora não avaliado.")
                    } else {
                        val base = BigDecimal.valueOf(durationSeconds).multiply(BigDecimal.valueOf(rule.priceCentsPerHour.toLong()))
                            .divide(BigDecimal.valueOf(3600), 0, RoundingMode.HALF_UP).toLong()
                        val h = BrNumber.formatDecimal(durationSeconds / 3600.0, 1)
                        compare(rule, freightCents, required(base), "$h h × ${BrNumber.formatCents(rule.priceCentsPerHour.toLong())}/h")
                    }
                }
                is PricingRule.NeighborhoodBonus, is PricingRule.NeighborhoodMultiplier -> null
            }
        }

        return RulesOutcome(
            checks = valueChecks + multiplierChecks + bonusChecks,
            neighborhoodBonusCents = bonus,
            neighborhoodMultiplier = multiplier,
            requiredMinimumCents = valueChecks.filter { it.status != RuleStatus.NAO_SE_APLICA }.mapNotNull { it.requiredCents }.maxOrNull(),
        )
    }

    private fun compare(rule: PricingRule, freight: Long, required: Long, label: String): RuleCheck =
        if (freight >= required) {
            RuleCheck(rule, RuleStatus.PASSOU, required,
                "$label: mínimo ${BrNumber.formatCents(required)}; paga ${BrNumber.formatCents(freight)}.")
        } else {
            RuleCheck(rule, RuleStatus.FALHOU, required,
                "$label exige ${BrNumber.formatCents(required)}, mas a oferta paga ${BrNumber.formatCents(freight)}.")
        }
}
