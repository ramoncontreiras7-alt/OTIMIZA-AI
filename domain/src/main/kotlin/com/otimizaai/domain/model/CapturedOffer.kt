package com.otimizaai.domain.model

/**
 * Dados BRUTOS de uma oferta lida na tela da plataforma, antes de aceitar.
 * Tudo que pode faltar na tela é opcional:
 *  - Amazon Flex e "Rota logística" do ML mostram valor + horas, mas NÃO km;
 *  - telas de detalhe de rota mostram valor + km + bairros.
 */
data class CapturedOffer(
    val platform: Platform,
    val freightCents: Long,
    /** Galpão/ponto de coleta como aparece na tela (ex.: "SCE3 - Itaitinga, Itaitinga"). */
    val warehouse: String? = null,
    /** Bairro de cada entrega, na ordem. */
    val neighborhoods: List<String> = emptyList(),
    /** Km da oferta (da tela) ou estimado pelo entregador. */
    val estimatedKm: Double? = null,
    val durationMinutes: Int? = null,
    /** IDs de pedido lidos na tela, EXATAMENTE como apareceram (regra #1). */
    val nativeIds: List<String> = emptyList(),
) {
    init {
        require(freightCents >= 0) { "Frete não pode ser negativo." }
        require(estimatedKm == null || estimatedKm > 0) { "Km estimado precisa ser maior que zero." }
        require(durationMinutes == null || durationMinutes > 0) { "Duração precisa ser maior que zero." }
    }
}

/**
 * Captura aguardando a decisão do entregador (cartão flutuante na tela).
 * O app mostra a avaliação e o entregador decide na própria plataforma.
 */
data class PendingCapture(
    val offer: CapturedOffer,
    val evaluation: OfferEvaluation,
    val sourcePackage: String,
    val capturedAtMillis: Long,
)

/** Veredito claro para o cartão e para a voz. */
sealed interface OfferEvaluation {

    /** Detalhes comuns: contas e regras. [economics] é null quando a oferta não tem km. */
    val economics: RouteEconomics?
    val checks: List<RuleCheck>
    val requiredMinimumCents: Long?
    /** Avisos que não reprovam (ex.: "sem km, combustível não calculado"). */
    val notes: List<String>

    data class Viable(
        override val economics: RouteEconomics?,
        override val checks: List<RuleCheck>,
        override val requiredMinimumCents: Long?,
        override val notes: List<String>,
    ) : OfferEvaluation

    data class Rejected(
        /** Motivos em português, prontos para tela e voz. Nunca vazio. */
        val reasons: List<String>,
        override val economics: RouteEconomics?,
        override val checks: List<RuleCheck>,
        override val requiredMinimumCents: Long?,
        override val notes: List<String>,
    ) : OfferEvaluation {
        init {
            require(reasons.isNotEmpty()) { "Uma oferta reprovada precisa de pelo menos um motivo." }
        }
    }
}
