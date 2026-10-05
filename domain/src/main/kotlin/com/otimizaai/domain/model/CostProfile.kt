package com.otimizaai.domain.model

/** Combustível do veículo (só informativo; o que entra na conta é o preço e a autonomia). */
enum class FuelType { GASOLINA, ETANOL, GNV, ELETRICO }

/**
 * Respostas do assistente de custos (inspirado no "Calcular Lucro e Custo por Km").
 * Dinheiro em centavos. Valores mensais e anuais são os que o entregador paga de verdade.
 */
data class CostProfile(
    val daysPerWeek: Int,
    val hoursPerDay: Double,
    val kmPerDay: Double,
    val weeklyGoalCents: Long,
    val maintenanceMonthCents: Long = 0,
    val insuranceMonthCents: Long = 0,
    val financingMonthCents: Long = 0,
    val otherMonthCents: Long = 0,
    val ipvaYearCents: Long = 0,
    val fuelType: FuelType = FuelType.GASOLINA,
    val consumptionKmPerLiter: Double,
    val fuelPriceCentsPerLiter: Int,
) {
    init {
        require(daysPerWeek in 1..7) { "Dias por semana deve ser de 1 a 7." }
        require(hoursPerDay > 0 && hoursPerDay <= 24) { "Horas por dia deve ser entre 0 e 24." }
        require(kmPerDay > 0) { "Km por dia deve ser maior que zero." }
        require(weeklyGoalCents >= 0) { "Meta semanal não pode ser negativa." }
        require(listOf(maintenanceMonthCents, insuranceMonthCents, financingMonthCents, otherMonthCents, ipvaYearCents).all { it >= 0 }) {
            "Custos não podem ser negativos."
        }
        require(consumptionKmPerLiter > 0) { "Autonomia deve ser maior que zero." }
        require(fuelPriceCentsPerLiter > 0) { "Preço do combustível deve ser maior que zero." }
    }
}

/**
 * Resultado do assistente: quanto custa cada km e quanto o entregador precisa
 * ganhar (líquido) por km e por hora para bater a meta.
 */
data class CostPlan(
    val kmPerWeek: Double,
    val kmPerMonth: Double,
    val fixedMonthCents: Long,
    val fixedCostCentsPerKm: Int,
    val fuelCostCentsPerKm: Int,
    val totalCostCentsPerKm: Int,
    val goalNetCentsPerKm: Int,
    val goalNetCentsPerHour: Int,
    val lowNetCentsPerKm: Int,
    val lowNetCentsPerHour: Int,
    /** Valor BRUTO mínimo por km que a oferta precisa pagar para bater a meta. */
    val grossNeededCentsPerKm: Int,
)
