package com.otimizaai.domain.model

import java.time.LocalDate

/**
 * Uma rota (jornada) do entregador. Pode haver várias no mesmo dia
 * (ex.: "Amazon" de manhã e "Rota 2" à tarde), como no Spoke.
 * O [id] é o mesmo RouteSessionId gravado em cada parada.
 */
data class Route(
    val id: RouteSessionId,
    val name: String,
    val date: LocalDate,
) {
    init {
        require(name.isNotBlank()) { "O nome da rota não pode ser vazio." }
    }
}

/** Rota com contadores, para a lista de rotas. */
data class RouteSummary(
    val route: Route,
    val totalStops: Int,
    val pendingStops: Int,
)
