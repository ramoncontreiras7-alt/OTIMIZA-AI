package com.otimizaai.domain.usecase

import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.RouteEconomics
import com.otimizaai.domain.model.VehicleProfile
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Calcula se uma rota vale a pena (o "CPK" do escopo).
 *
 * FÓRMULAS
 *   combustível = km ÷ consumo (km/L) × preço do litro
 *   desgaste    = km × custo de desgaste por km
 *   lucro       = receita − combustível − desgaste
 *   R$/km       = lucro ÷ km
 *   R$/hora     = lucro ÷ horas
 *
 * PRECISÃO
 * Dinheiro entra e sai em centavos inteiros. As contas intermediárias usam
 * BigDecimal (decimal exato) e só arredondam no final, para o centavo mais
 * próximo (meio centavo arredonda para cima). Isso evita os erros de
 * arredondamento que o Double acumula.
 */
class CalculateRouteProfitUseCase @Inject constructor() {

    /**
     * @param distanceMeters distância total da rota, em metros (precisa ser > 0).
     * @param revenueCents soma do que o entregador vai receber, em centavos.
     * @param fuelPriceCentsPerLiter preço do litro em centavos (R$ 6,29 = 629).
     * @param minNetCentsPerKm meta mínima de lucro por km, em centavos (R$ 2,00 = 200).
     * @param estimatedDurationSeconds duração estimada; null se desconhecida.
     */
    operator fun invoke(
        distanceMeters: Long,
        revenueCents: Long,
        fuelPriceCentsPerLiter: Int,
        vehicle: VehicleProfile,
        minNetCentsPerKm: Int,
        estimatedDurationSeconds: Long? = null,
    ): RouteEconomics {
        require(distanceMeters > 0) { "A distância precisa ser maior que zero: $distanceMeters m" }
        require(revenueCents >= 0) { "A receita não pode ser negativa: $revenueCents centavos" }
        require(fuelPriceCentsPerLiter >= 0) {
            "O preço do combustível não pode ser negativo: $fuelPriceCentsPerLiter centavos/L"
        }
        require(estimatedDurationSeconds == null || estimatedDurationSeconds > 0) {
            "A duração, quando informada, precisa ser maior que zero: $estimatedDurationSeconds s"
        }

        val km = BigDecimal.valueOf(distanceMeters).divide(METERS_PER_KM, SCALE, RoundingMode.HALF_UP)

        val liters = km.divide(BigDecimal.valueOf(vehicle.consumptionKmPerLiter), SCALE, RoundingMode.HALF_UP)
        val fuelCostCents = liters.multiply(BigDecimal.valueOf(fuelPriceCentsPerLiter.toLong())).toCents()
        val wearCostCents = km.multiply(BigDecimal.valueOf(vehicle.wearCostCentsPerKm.toLong())).toCents()

        val netProfitCents = revenueCents - fuelCostCents - wearCostCents
        val grossCentsPerKm = BigDecimal.valueOf(revenueCents).divide(km, SCALE, RoundingMode.HALF_UP).toCents()
        val netCentsPerKm = BigDecimal.valueOf(netProfitCents).divide(km, SCALE, RoundingMode.HALF_UP).toCents()

        val netCentsPerHour = estimatedDurationSeconds?.let { seconds ->
            val hours = BigDecimal.valueOf(seconds).divide(SECONDS_PER_HOUR, SCALE, RoundingMode.HALF_UP)
            BigDecimal.valueOf(netProfitCents).divide(hours, SCALE, RoundingMode.HALF_UP).toCents()
        }

        return RouteEconomics(
            distanceMeters = distanceMeters,
            revenueCents = revenueCents,
            fuelCostCents = fuelCostCents,
            wearCostCents = wearCostCents,
            netProfitCents = netProfitCents,
            grossCentsPerKm = grossCentsPerKm,
            netCentsPerKm = netCentsPerKm,
            netCentsPerHour = netCentsPerHour,
            minNetCentsPerKm = minNetCentsPerKm,
            isViable = netCentsPerKm >= minNetCentsPerKm,
        )
    }

    /**
     * Atalho para uma rota montada com paradas: a receita é a soma dos fretes.
     * As paradas não são alteradas (a regra do ID original continua intacta).
     */
    fun forStops(
        stops: List<DeliveryStop>,
        distanceMeters: Long,
        fuelPriceCentsPerLiter: Int,
        vehicle: VehicleProfile,
        minNetCentsPerKm: Int,
        estimatedDurationSeconds: Long? = null,
    ): RouteEconomics = invoke(
        distanceMeters = distanceMeters,
        revenueCents = stops.sumOf { it.freightCents.toLong() },
        fuelPriceCentsPerLiter = fuelPriceCentsPerLiter,
        vehicle = vehicle,
        minNetCentsPerKm = minNetCentsPerKm,
        estimatedDurationSeconds = estimatedDurationSeconds,
    )

    private fun BigDecimal.toCents(): Long = setScale(0, RoundingMode.HALF_UP).longValueExact()

    private companion object {
        const val SCALE = 10
        val METERS_PER_KM: BigDecimal = BigDecimal.valueOf(1000)
        val SECONDS_PER_HOUR: BigDecimal = BigDecimal.valueOf(3600)
    }
}
