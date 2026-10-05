package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.BandLimits
import com.otimizaai.domain.model.CostProfile
import com.otimizaai.domain.model.ProfitBand
import com.otimizaai.domain.model.ProfitRating
import com.otimizaai.domain.model.RouteEconomics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CostPlanAndRatingTest {

    private val calc = CalculateCostPlanUseCase()

    /** 6 dias, 10 h/dia, 150 km/dia, meta R$ 1.500/semana, 10 km/L a R$ 5,50. */
    private val profile = CostProfile(
        daysPerWeek = 6,
        hoursPerDay = 10.0,
        kmPerDay = 150.0,
        weeklyGoalCents = 150_000,
        maintenanceMonthCents = 50_000,
        insuranceMonthCents = 15_000,
        financingMonthCents = 0,
        otherMonthCents = 20_000,
        ipvaYearCents = 120_000,
        consumptionKmPerLiter = 10.0,
        fuelPriceCentsPerLiter = 550,
    )

    @Test
    @DisplayName("Assistente: custo por km e metas a partir dos custos do mês")
    fun costPlanExample() {
        val p = calc(profile)
        assertEquals(900.0, p.kmPerWeek)
        assertEquals(3900.0, p.kmPerMonth)
        assertEquals(95_000L, p.fixedMonthCents)       // 500 + 150 + 200 + 1200/12
        assertEquals(24, p.fixedCostCentsPerKm)         // 950 / 3900 = 0,2436
        assertEquals(55, p.fuelCostCentsPerKm)          // 5,50 / 10
        assertEquals(79, p.totalCostCentsPerKm)
        assertEquals(167, p.goalNetCentsPerKm)          // 1500 / 900 = 1,667
        assertEquals(2_500, p.goalNetCentsPerHour)      // 1500 / 60 h
        assertEquals(117, p.lowNetCentsPerKm)           // 70% da meta
        assertEquals(1_750, p.lowNetCentsPerHour)
        assertEquals(246, p.grossNeededCentsPerKm)      // 1,667 + 0,244 + 0,55
    }

    @Test
    @DisplayName("Assistente: respostas impossíveis são recusadas")
    fun invalidProfile() {
        assertThrows<IllegalArgumentException> { profile.copy(daysPerWeek = 0) }
        assertThrows<IllegalArgumentException> { profile.copy(daysPerWeek = 8) }
        assertThrows<IllegalArgumentException> { profile.copy(kmPerDay = 0.0) }
        assertThrows<IllegalArgumentException> { profile.copy(hoursPerDay = 25.0) }
        assertThrows<IllegalArgumentException> { profile.copy(consumptionKmPerLiter = 0.0) }
        assertThrows<IllegalArgumentException> { profile.copy(insuranceMonthCents = -1) }
    }

    private fun economics(perKm: Long, perHour: Long?) = RouteEconomics(
        distanceMeters = 10_000, revenueCents = 0, fuelCostCents = 0, wearCostCents = 0,
        netProfitCents = 0, grossCentsPerKm = 0, netCentsPerKm = perKm, netCentsPerHour = perHour,
        minNetCentsPerKm = 0, isViable = true,
    )

    private val kmLimits = BandLimits(low = 117, good = 167)
    private val hourLimits = BandLimits(low = 1_750, good = 2_500)

    @Test
    @DisplayName("Três faixas: ruim, média e boa")
    fun bands() {
        assertEquals(ProfitBand.RUIM, kmLimits.classify(116))
        assertEquals(ProfitBand.MEDIA, kmLimits.classify(117))
        assertEquals(ProfitBand.MEDIA, kmLimits.classify(166))
        assertEquals(ProfitBand.BOA, kmLimits.classify(167))
        assertThrows<IllegalArgumentException> { BandLimits(low = 300, good = 200) }
    }

    @Test
    @DisplayName("A faixa geral é a pior entre R$/km e R$/hora")
    fun overallIsWorst() {
        val good = ProfitRating.of(economics(291, 4_367), kmLimits, hourLimits)
        assertEquals(ProfitBand.BOA, good.overall)

        val mixed = ProfitRating.of(economics(291, 1_000), kmLimits, hourLimits)
        assertEquals(ProfitBand.BOA, mixed.perKm)
        assertEquals(ProfitBand.RUIM, mixed.perHour)
        assertEquals(ProfitBand.RUIM, mixed.overall)

        val noHour = ProfitRating.of(economics(150, null), kmLimits, hourLimits)
        assertNull(noHour.perHour)
        assertEquals(ProfitBand.MEDIA, noHour.overall)
    }
}
