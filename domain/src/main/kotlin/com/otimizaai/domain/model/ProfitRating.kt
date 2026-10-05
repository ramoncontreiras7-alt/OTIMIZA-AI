package com.otimizaai.domain.model

/** As três faixas de avaliação (como no Gigu): ruim, média e boa. */
enum class ProfitBand { RUIM, MEDIA, BOA }

/**
 * Limites do entregador, em CENTAVOS de lucro líquido.
 * Abaixo de [low] é ruim; a partir de [good] é bom; entre os dois é médio.
 */
data class BandLimits(val low: Int, val good: Int) {
    init {
        require(low <= good) { "O limite de 'ruim' não pode ser maior que o de 'bom'." }
    }

    fun classify(value: Long): ProfitBand = when {
        value >= good -> ProfitBand.BOA
        value < low -> ProfitBand.RUIM
        else -> ProfitBand.MEDIA
    }
}

/** Avaliação de uma rota/oferta: faixa por km, faixa por hora (se houver) e a geral. */
data class ProfitRating(
    val perKm: ProfitBand,
    val perHour: ProfitBand?,
) {
    /** A faixa geral é a PIOR das duas: uma oferta só é boa se for boa nas duas medidas. */
    val overall: ProfitBand
        get() = listOfNotNull(perKm, perHour).minBy { it.ordinal }

    companion object {
        fun of(e: RouteEconomics, km: BandLimits, hour: BandLimits): ProfitRating = ProfitRating(
            perKm = km.classify(e.netCentsPerKm),
            perHour = e.netCentsPerHour?.let { hour.classify(it) },
        )
    }
}
