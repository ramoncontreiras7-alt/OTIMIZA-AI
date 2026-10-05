package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.model.VehicleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Os números destes testes são os mesmos exemplos da "Planta do OTIMIZA AI".
 * Carro: 11 km/L, desgaste R$ 0,25/km. Gasolina: R$ 6,29. Meta: R$ 2,00/km.
 */
class CalculateRouteProfitUseCaseTest {

    private val calculate = CalculateRouteProfitUseCase()
    private val car = VehicleProfile(VehicleType.CAR, consumptionKmPerLiter = 11.0, wearCostCentsPerKm = 25)
    private val motorcycle = VehicleProfile(VehicleType.MOTORCYCLE, consumptionKmPerLiter = 40.0, wearCostCentsPerKm = 12)

    @Test
    @DisplayName("Rota do dia: 62 km, R$ 210 de receita -> lucro R$ 159,05 e R$ 2,57/km")
    fun dailyRouteExample() {
        val r = calculate(
            distanceMeters = 62_000,
            revenueCents = 21_000,
            fuelPriceCentsPerLiter = 629,
            vehicle = car,
            minNetCentsPerKm = 200,
        )
        assertEquals(3_545, r.fuelCostCents)      // 62 / 11 x 6,29 = 35,4527...
        assertEquals(1_550, r.wearCostCents)      // 62 x 0,25
        assertEquals(5_095, r.totalCostCents)
        assertEquals(15_905, r.netProfitCents)    // 210,00 - 35,45 - 15,50
        assertEquals(339, r.grossCentsPerKm)      // 210 / 62 = 3,387...
        assertEquals(257, r.netCentsPerKm)        // 159,05 / 62 = 2,565...
        assertNull(r.netCentsPerHour)
        assertTrue(r.isViable)
    }

    @Test
    @DisplayName("Oferta de 3h, 45 km, R$ 168 -> lucro R$ 131,02, R$ 2,91/km e R$ 43,67/h")
    fun offerExampleWithDuration() {
        val r = calculate(
            distanceMeters = 45_000,
            revenueCents = 16_800,
            fuelPriceCentsPerLiter = 629,
            vehicle = car,
            minNetCentsPerKm = 200,
            estimatedDurationSeconds = 3 * 3_600,
        )
        assertEquals(2_573, r.fuelCostCents)
        assertEquals(1_125, r.wearCostCents)
        assertEquals(13_102, r.netProfitCents)
        assertEquals(291, r.netCentsPerKm)
        assertEquals(4_367L, r.netCentsPerHour)   // L: o campo é Long? (pode ser nulo)
        assertTrue(r.isViable)
    }

    @Test
    @DisplayName("Rota abaixo da meta é marcada como não viável")
    fun belowTargetIsNotViable() {
        val r = calculate(30_000, 5_000, 629, car, minNetCentsPerKm = 200)
        assertEquals(1_715, r.fuelCostCents)   // 1715,45 -> 1715
        assertEquals(750, r.wearCostCents)
        assertEquals(2_535, r.netProfitCents)
        assertEquals(85, r.netCentsPerKm)      // 84,5 -> arredonda para cima
        assertFalse(r.isViable)
    }

    @Test
    @DisplayName("Rota que dá prejuízo mostra lucro negativo")
    fun lossIsNegative() {
        val r = calculate(30_000, 1_000, 629, car, minNetCentsPerKm = 0)
        assertEquals(-1_465, r.netProfitCents)
        assertEquals(-49, r.netCentsPerKm)
        assertFalse(r.isViable)
    }

    @Test
    @DisplayName("Moto gasta bem menos combustível que carro no mesmo percurso")
    fun motorcycleUsesLessFuel() {
        val r = calculate(30_000, 5_000, 629, motorcycle, minNetCentsPerKm = 100)
        assertEquals(472, r.fuelCostCents)     // 0,75 L x 6,29 = 4,7175
        assertEquals(360, r.wearCostCents)
        assertEquals(4_168, r.netProfitCents)
        assertEquals(139, r.netCentsPerKm)
        assertTrue(r.isViable)
    }

    @Test
    @DisplayName("Distância fracionada (metros) é respeitada")
    fun fractionalDistance() {
        val r = calculate(1_500, 1_000, 600, car, minNetCentsPerKm = 0)
        assertEquals(82, r.fuelCostCents)      // 1,5 / 11 x 6,00 = 0,818...
        assertEquals(38, r.wearCostCents)      // 1,5 x 0,25 = 0,375
        assertEquals(880, r.netProfitCents)
    }

    @Test
    @DisplayName("Receita das paradas = soma dos fretes")
    fun revenueFromStops() {
        val session = RouteSessionId("2026-10-05")
        val address = DeliveryAddress("Av. Santos Dumont, 1500")
        val stops = listOf(
            DeliveryStop.create(NativeStopId("MLB-1"), Platform.MERCADO_LIVRE, session, address, freightCents = 1_150),
            DeliveryStop.create(NativeStopId("IFD-2"), Platform.IFOOD, session, address, freightCents = 850),
            DeliveryStop.create(NativeStopId("TBA-3"), Platform.AMAZON_FLEX, session, address, freightCents = 0),
        )
        val r = calculate.forStops(stops, distanceMeters = 10_000, fuelPriceCentsPerLiter = 629, vehicle = car, minNetCentsPerKm = 0)
        assertEquals(2_000, r.revenueCents)
    }

    @Test
    @DisplayName("Entradas inválidas são recusadas")
    fun invalidInputsAreRejected() {
        assertThrows<IllegalArgumentException> { calculate(0, 1_000, 629, car, 200) }
        assertThrows<IllegalArgumentException> { calculate(-5, 1_000, 629, car, 200) }
        assertThrows<IllegalArgumentException> { calculate(1_000, -1, 629, car, 200) }
        assertThrows<IllegalArgumentException> { calculate(1_000, 1_000, -1, car, 200) }
        assertThrows<IllegalArgumentException> { calculate(1_000, 1_000, 629, car, 200, estimatedDurationSeconds = 0) }
        assertThrows<IllegalArgumentException> { VehicleProfile(VehicleType.CAR, 0.0, 25) }
        assertThrows<IllegalArgumentException> { VehicleProfile(VehicleType.CAR, Double.NaN, 25) }
        assertThrows<IllegalArgumentException> { VehicleProfile(VehicleType.CAR, 11.0, -1) }
    }
}
