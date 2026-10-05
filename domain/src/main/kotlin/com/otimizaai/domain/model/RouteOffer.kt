package com.otimizaai.domain.model

/**
 * Uma oferta de rota como aparece na tela da plataforma, ANTES de aceitar.
 * Ex. (print do Ramon): coleta "PETZ *** LOJA 246 MRPG-CE MARAPONGA", valor R$ 40,59,
 * percurso 19 km, 4 entregas em Maraponga, Jóquei Clube, Montese e Autran Nunes.
 */
data class RouteOffer(
    val platform: Platform,
    /** Texto do galpão/ponto de coleta, como lido na tela (pode ser null). */
    val warehouse: String?,
    val revenueCents: Long,
    val distanceMeters: Long,
    val durationSeconds: Long? = null,
    /** Bairro de cada entrega, na ordem (repetições contam: 2 entregas no mesmo bairro = 2). */
    val deliveryNeighborhoods: List<String> = emptyList(),
) {
    init {
        require(revenueCents >= 0) { "O valor da oferta não pode ser negativo." }
        require(distanceMeters > 0) { "A distância precisa ser maior que zero." }
    }
}

/** Resultado de uma regra na avaliação. */
enum class RuleStatus { PASSOU, FALHOU, NAO_SE_APLICA }

data class RuleCheck(
    val rule: PricingRule,
    val status: RuleStatus,
    /** Valor mínimo exigido por esta regra (já com acréscimos de bairro), quando houver. */
    val requiredCents: Long?,
    /** Explicação em português para mostrar ao entregador. */
    val detail: String,
)

/** Avaliação completa: contas de custo + regras da plataforma. */
data class RouteOfferEvaluation(
    val economics: RouteEconomics,
    val checks: List<RuleCheck>,
    /** Soma dos acréscimos de bairro aplicados. */
    val neighborhoodBonusCents: Long,
    /** Maior valor mínimo exigido entre as regras que se aplicam (null se nenhuma). */
    val requiredMinimumCents: Long?,
    /**
     * Viável = nenhuma regra FALHOU **e** a rota dá lucro depois de combustível e custos fixos.
     */
    val isViable: Boolean,
    /** Motivos de reprovação, em português, prontos para tela e voz. */
    val reasons: List<String>,
)
