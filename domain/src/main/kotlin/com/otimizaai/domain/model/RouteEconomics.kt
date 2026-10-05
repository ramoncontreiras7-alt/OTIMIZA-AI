package com.otimizaai.domain.model

/**
 * Resultado financeiro de uma rota (ou de uma oferta avaliada).
 * Todos os valores em dinheiro estão em CENTAVOS. Ex.: 15905 = R$ 159,05.
 *
 * - [netCentsPerKm]: lucro líquido por km (o principal indicador do entregador).
 * - [netCentsPerHour]: lucro por hora; null quando não se sabe a duração.
 * - [isViable]: true quando [netCentsPerKm] alcança a meta mínima do entregador.
 */
data class RouteEconomics(
    val distanceMeters: Long,
    val revenueCents: Long,
    val fuelCostCents: Long,
    val wearCostCents: Long,
    val netProfitCents: Long,
    val grossCentsPerKm: Long,
    val netCentsPerKm: Long,
    val netCentsPerHour: Long?,
    val minNetCentsPerKm: Int,
    val isViable: Boolean,
) {
    /** Custo total da rota (combustível + desgaste). */
    val totalCostCents: Long
        get() = fuelCostCents + wearCostCents
}
