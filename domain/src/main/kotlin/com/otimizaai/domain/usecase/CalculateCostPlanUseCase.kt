package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.CostPlan
import com.otimizaai.domain.model.CostProfile
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Transforma as respostas do assistente em números de decisão.
 *
 *   km por semana   = km/dia × dias/semana
 *   km por mês      = km/semana × 52 ÷ 12           (≈ 4,33 semanas)
 *   custo fixo/mês  = manutenção + seguro + parcela/aluguel + outros + IPVA ÷ 12
 *   custo fixo/km   = custo fixo/mês ÷ km/mês
 *   combustível/km  = preço do litro ÷ autonomia
 *   meta líquida/km = meta semanal ÷ km/semana
 *   meta líquida/h  = meta semanal ÷ (horas/dia × dias/semana)
 *   faixa "ruim"    = abaixo de 70% da meta
 *   bruto mínimo/km = meta líquida/km + custo total/km
 */
class CalculateCostPlanUseCase @Inject constructor() {

    operator fun invoke(p: CostProfile): CostPlan {
        val kmWeek = bd(p.kmPerDay).multiply(BigDecimal.valueOf(p.daysPerWeek.toLong()))
        val kmMonth = kmWeek.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP)
        val hoursWeek = bd(p.hoursPerDay).multiply(BigDecimal.valueOf(p.daysPerWeek.toLong()))

        val fixedMonth = BigDecimal.valueOf(
            p.maintenanceMonthCents + p.insuranceMonthCents + p.financingMonthCents + p.otherMonthCents,
        ).add(BigDecimal.valueOf(p.ipvaYearCents).divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP))

        val fixedPerKm = fixedMonth.divide(kmMonth, SCALE, RoundingMode.HALF_UP)
        val fuelPerKm = BigDecimal.valueOf(p.fuelPriceCentsPerLiter.toLong()).divide(bd(p.consumptionKmPerLiter), SCALE, RoundingMode.HALF_UP)
        val goalPerKm = BigDecimal.valueOf(p.weeklyGoalCents).divide(kmWeek, SCALE, RoundingMode.HALF_UP)
        val goalPerHour = BigDecimal.valueOf(p.weeklyGoalCents).divide(hoursWeek, SCALE, RoundingMode.HALF_UP)

        return CostPlan(
            kmPerWeek = kmWeek.toDouble(),
            kmPerMonth = kmMonth.toDouble(),
            fixedMonthCents = fixedMonth.round0().toLong(),
            fixedCostCentsPerKm = fixedPerKm.round0(),
            fuelCostCentsPerKm = fuelPerKm.round0(),
            totalCostCentsPerKm = fixedPerKm.add(fuelPerKm).round0(),
            goalNetCentsPerKm = goalPerKm.round0(),
            goalNetCentsPerHour = goalPerHour.round0(),
            lowNetCentsPerKm = goalPerKm.multiply(LOW_FACTOR).round0(),
            lowNetCentsPerHour = goalPerHour.multiply(LOW_FACTOR).round0(),
            grossNeededCentsPerKm = goalPerKm.add(fixedPerKm).add(fuelPerKm).round0(),
        )
    }

    private fun bd(d: Double): BigDecimal = BigDecimal.valueOf(d)
    private fun BigDecimal.round0(): Int = setScale(0, RoundingMode.HALF_UP).intValueExact()

    private companion object {
        const val SCALE = 10
        val LOW_FACTOR: BigDecimal = BigDecimal("0.7")
    }
}
