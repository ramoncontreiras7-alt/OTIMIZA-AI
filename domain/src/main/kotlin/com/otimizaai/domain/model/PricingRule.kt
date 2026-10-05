package com.otimizaai.domain.model

/*
 * ============================================================================
 *  REGRAS FINANCEIRAS POR PLATAFORMA
 * ============================================================================
 *  Cada plataforma (Mercado Livre, Amazon Flex, Magalu...) paga de um jeito.
 *  O entregador escolhe quais regras usar (cada uma pode ser ligada/desligada
 *  em [PricingRule.enabled]) e o app avalia cada oferta lida na tela contra elas,
 *  ALÉM do custo de combustível e dos custos fixos do veículo.
 *
 *  O app só LÊ e AVISA. Nunca aceita ofertas sozinho (sem autoclique).
 *  Todos os valores em CENTAVOS (R$ 80,00 = 8000).
 * ============================================================================
 */

/** Uma regra de preço configurada pelo entregador para uma plataforma. */
sealed interface PricingRule {

    /** O entregador pode desligar a regra sem apagá-la (opção selecionável). */
    val enabled: Boolean

    /**
     * Galpão / ponto de coleta: rotas que saem de [warehouseId] precisam pagar
     * pelo menos [minValueCents]. Ex.: Mercado Livre, coleta no "SCE3" -> mínimo R$ 160,00.
     *
     * [warehouseId] é comparado sem diferenciar maiúsculas/acentos e como palavra
     * inteira: "BRNCE20" casa com "BRNCE20 - Agencia Mercado Livre", mas NÃO com "BRNCE203".
     */
    data class MinimumRouteValue(
        val warehouseId: String,
        val minValueCents: Long,
        override val enabled: Boolean = true,
    ) : PricingRule {
        init {
            require(warehouseId.isNotBlank()) { "Informe o galpão/ponto de coleta." }
            require(minValueCents > 0) { "O valor mínimo da rota precisa ser maior que zero." }
        }
    }

    /**
     * Valor por km rodado: a oferta precisa pagar pelo menos [priceCentsPerKm] por km.
     * Ex.: Magalu Ultra, R$ 2,50/km -> rota de 19 km precisa pagar R$ 47,50.
     * Só é avaliada quando a oferta informa (ou o entregador estima) os km.
     */
    data class ValuePerKm(
        val priceCentsPerKm: Int,
        override val enabled: Boolean = true,
    ) : PricingRule {
        init {
            require(priceCentsPerKm > 0) { "O valor por km precisa ser maior que zero." }
        }
    }

    /**
     * Valor por hora: para blocos pagos por tempo (Amazon Flex, "Rota logística" do ML).
     * Ex.: mínimo R$ 40,00/h -> bloco de 4 h precisa pagar R$ 160,00.
     */
    data class MinimumValuePerHour(
        val priceCentsPerHour: Int,
        override val enabled: Boolean = true,
    ) : PricingRule {
        init {
            require(priceCentsPerHour > 0) { "O valor por hora precisa ser maior que zero." }
        }
    }

    /**
     * Bairro/zona com MULTIPLICADOR: se a rota tem entrega em [neighborhoodName], os mínimos
     * de galpão, km e hora são multiplicados por [multiplier]. Ex.: 1,2 = exigir 20% a mais.
     * Se vários bairros com multiplicador aparecem, vale o MAIOR (não multiplica em cascata).
     */
    data class NeighborhoodMultiplier(
        val neighborhoodName: String,
        val multiplier: Double,
        override val enabled: Boolean = true,
    ) : PricingRule {
        init {
            require(neighborhoodName.isNotBlank()) { "Informe o bairro." }
            require(multiplier.isFinite() && multiplier >= 1.0 && multiplier <= 5.0) {
                "O multiplicador deve ficar entre 1,0 e 5,0."
            }
        }
    }

    /**
     * Bairro/zona com ACRÉSCIMO FIXO: cada entrega em [neighborhoodName] soma [bonusCents]
     * ao mínimo exigido (ex.: área de risco, morro, trânsito ruim).
     */
    data class NeighborhoodBonus(
        val neighborhoodName: String,
        val bonusCents: Long,
        override val enabled: Boolean = true,
    ) : PricingRule {
        init {
            require(neighborhoodName.isNotBlank()) { "Informe o bairro." }
            require(bonusCents > 0) { "O acréscimo do bairro precisa ser maior que zero." }
        }
    }
}

/** Conjunto de regras de UMA plataforma. [enabled] liga/desliga todas de uma vez. */
data class PlatformFinancialConfig(
    val platform: Platform,
    val rules: List<PricingRule>,
    val enabled: Boolean = true,
) {
    /** Regras que realmente valem agora. */
    val activeRules: List<PricingRule>
        get() = if (enabled) rules.filter { it.enabled } else emptyList()
}
