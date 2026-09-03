package com.otimiza.delivery.domain.usecase

import com.otimiza.delivery.domain.model.RouteFinancialMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CalculateRouteCpKUseCaseTest {

    private val useCase = CalculateRouteCpKUseCase()

    @Test
    fun `deve calcular CPK, custo de combustivel e rendimento liquido por km`() {
        val metrics = useCase(
            totalDistanceKm = 100.0,
            totalRevenue = 500.0,
            fuelPricePerLiter = 5.0,
            vehicleConsumptionKmPerLiter = 10.0
        )

        assertEquals(10.0, metrics.totalFuelCost, 0.001)
        assertEquals(490.0, metrics.netProfit, 0.001)
        assertEquals(4.9, metrics.netYieldPerKm, 0.001)
        assertEquals(100.0, metrics.totalDistanceKm, 0.001)
        assertTrue(metrics.isViable)
    }

    @Test
    fun `isViable deve ser false quando rendimento abaixo do minimo aceitavel`() {
        val metrics = useCase(
            totalDistanceKm = 100.0,
            totalRevenue = 200.0,
            fuelPricePerLiter = 5.0,
            vehicleConsumptionKmPerLiter = 10.0,
            minimumAcceptableYieldPerKm = 5.0
        )

        assertEquals(50.0, metrics.totalFuelCost, 0.001)
        assertEquals(150.0, metrics.netProfit, 0.001)
        assertEquals(1.5, metrics.netYieldPerKm, 0.001)
        assertFalse(metrics.isViable)
    }

    @Test
    fun `deve retornar nao-viavel e custo zero para entrada invalida`() {
        val zeroDistance = useCase(
            totalDistanceKm = 0.0,
            totalRevenue = 500.0,
            fuelPricePerLiter = 5.0,
            vehicleConsumptionKmPerLiter = 10.0
        )
        assertFalse(zeroDistance.isViable)
        assertEquals(0.0, zeroDistance.totalFuelCost, 0.001)

        val zeroConsumption = useCase(
            totalDistanceKm = 100.0,
            totalRevenue = 500.0,
            fuelPricePerLiter = 5.0,
            vehicleConsumptionKmPerLiter = 0.0
        )
        assertFalse(zeroConsumption.isViable)
        assertEquals(0.0, zeroConsumption.totalFuelCost, 0.001)
    }
}
