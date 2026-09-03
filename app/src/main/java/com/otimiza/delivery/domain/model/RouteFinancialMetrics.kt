package com.otimiza.delivery.domain.model

data class RouteFinancialMetrics(
    val totalDistanceKm: Double,
    val totalRevenue: Double,
    val fuelPricePerLiter: Double,
    val vehicleConsumptionKmPerLiter: Double,
    val totalFuelCost: Double,
    val netProfit: Double,
    val netYieldPerKm: Double,
    val isViable: Boolean
)
