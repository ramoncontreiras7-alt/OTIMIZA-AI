package com.otimiza.delivery.domain.usecase

import com.otimiza.delivery.domain.model.RouteFinancialMetrics

class CalculateRouteCpKUseCase {

    operator fun invoke(
        totalDistanceKm: Double,
        totalRevenue: Double,
        fuelPricePerLiter: Double,
        vehicleConsumptionKmPerLiter: Double,
        minimumAcceptableYieldPerKm: Double = 2.0
    ): RouteFinancialMetrics {

        if (totalDistanceKm <= 0.0 || vehicleConsumptionKmPerLiter <= 0.0) {
            return RouteFinancialMetrics(
                totalDistanceKm = totalDistanceKm,
                totalRevenue = totalRevenue,
                fuelPricePerLiter = fuelPricePerLiter,
                vehicleConsumptionKmPerLiter = vehicleConsumptionKmPerLiter,
                totalFuelCost = 0.0,
                netProfit = totalRevenue,
                netYieldPerKm = 0.0,
                isViable = false
            )
        }

        val litersConsumed = totalDistanceKm / vehicleConsumptionKmPerLiter
        val totalFuelCost = litersConsumed * fuelPricePerLiter
        val netProfit = totalRevenue - totalFuelCost
        val netYieldPerKm = netProfit / totalDistanceKm

        return RouteFinancialMetrics(
            totalDistanceKm = totalDistanceKm,
            totalRevenue = totalRevenue,
            fuelPricePerLiter = fuelPricePerLiter,
            vehicleConsumptionKmPerLiter = vehicleConsumptionKmPerLiter,
            totalFuelCost = totalFuelCost,
            netProfit = netProfit,
            netYieldPerKm = netYieldPerKm,
            isViable = netYieldPerKm >= minimumAcceptableYieldPerKm
        )
    }
}
