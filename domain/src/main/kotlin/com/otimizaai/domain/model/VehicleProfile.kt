package com.otimizaai.domain.model

/** Tipo de veículo do entregador. O MVP atende carro e moto. */
enum class VehicleType { CAR, MOTORCYCLE }

/**
 * Dados do veículo usados no cálculo de custo da rota.
 *
 * - [consumptionKmPerLiter]: quantos km o veículo roda com 1 litro (ex.: carro 11.0, moto 40.0).
 * - [wearCostCentsPerKm]: desgaste estimado por km, em CENTAVOS (pneu, óleo, manutenção).
 *   Ex.: R$ 0,25 por km = 25.
 */
data class VehicleProfile(
    val type: VehicleType,
    val consumptionKmPerLiter: Double,
    val wearCostCentsPerKm: Int,
) {
    init {
        require(consumptionKmPerLiter.isFinite() && consumptionKmPerLiter > 0.0) {
            "Consumo precisa ser maior que zero: $consumptionKmPerLiter km/L"
        }
        require(wearCostCentsPerKm >= 0) {
            "Custo de desgaste não pode ser negativo: $wearCostCentsPerKm centavos/km"
        }
    }
}
