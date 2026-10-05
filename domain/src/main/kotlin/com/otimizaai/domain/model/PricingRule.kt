package com.otimizaai.domain.model

/*
 * ============================================================================
 *  REGRAS FINANCEIRAS POR PLATAFORMA
 * ============================================================================
 *  Cada plataforma (Mercado Livre, Amazon Flex, Magalu...) tem a sua própria
 *  forma de pagar. O entregador configura regras e o app avalia cada oferta/rota
 *  contra elas, ALÉM do custo de combustível e dos custos fixos do veículo.
 *
 *  Todos os valores em CENTAVOS (R$ 80,00 = 8000).
 * ============================================================================
 */

/** Uma regra de preço configurada pelo entregador para uma plataforma. */
sealed interface PricingRule {

    /**
     * Galpão / ponto de coleta: rotas que saem de [warehouseId] precisam pagar
     * pelo menos [minValueCents]. Ex.: Mercado Livre, coleta no "BRNCE20" -> mínimo R$ 80,00.
     *
     * [warehouseId] é comparado sem diferenciar maiúsculas/acentos e como palavra
     * inteira dentro do texto da coleta: "BRNCE20" casa com
     * "BRNCE20 - Agencia Mercado Livre - ECOPRINT", mas NÃO com "BRNCE203".
     */
    data class MinimumRouteValue(
        val warehouseId: String,
        val minValueCents: Long,
    ) : PricingRule {
        init {
            require(warehouseId.isNotBlank()) { "Informe o galpão/ponto de coleta." }
            require(minValueCents > 0) { "O valor mínimo da rota precisa ser maior que zero." }
        }
    }

    /**
     * Valor por km rodado: a oferta precisa pagar pelo menos [priceCentsPerKm] por km.
     * Ex.: Magalu Ultra, mínimo R$ 2,50/km -> uma rota de 19 km precisa pagar R$ 47,50.
     */
    data class ValuePerKm(
        val priceCentsPerKm: Int,
    ) : PricingRule {
        init {
            require(priceCentsPerKm > 0) { "O valor por km precisa ser maior que zero." }
        }
    }

    /**
     * Bairro/zona: cada entrega em [neighborhoodName] exige [bonusCents] a mais
     * (ex.: área de risco, trânsito ruim, morro). O acréscimo soma no mínimo
     * exigido pelas regras de galpão e de km.
     */
    data class NeighborhoodBonus(
        val neighborhoodName: String,
        val bonusCents: Long,
    ) : PricingRule {
        init {
            require(neighborhoodName.isNotBlank()) { "Informe o bairro." }
            require(bonusCents > 0) { "O acréscimo do bairro precisa ser maior que zero." }
        }
    }
}

/** Conjunto de regras de UMA plataforma. */
data class PlatformFinancialConfig(
    val platform: Platform,
    val rules: List<PricingRule>,
    val enabled: Boolean = true,
)
