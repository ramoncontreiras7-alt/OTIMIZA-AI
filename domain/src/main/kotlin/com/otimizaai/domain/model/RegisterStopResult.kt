package com.otimizaai.domain.model

/**
 * O que aconteceu ao tentar registrar uma parada capturada (OCR, tela, arquivo).
 *
 * A chave (native_stop_id, platform_id) é única no banco inteiro, não por sessão.
 * O registro NUNCA move nem altera uma parada existente: o bloqueio da chave
 * primária é mantido e a decisão sobe para a camada de apresentação.
 */
sealed interface RegisterStopResult {

    /** Parada nova, gravada com sucesso. */
    data object Inserted : RegisterStopResult

    /** Mesma parada já registrada nesta sessão. Nada foi alterado. */
    data object AlreadyInSession : RegisterStopResult

    /**
     * A parada existe em OUTRA sessão (ex.: falhou ontem, voltou hoje).
     * Nada foi alterado. [existing] traz a parada como está no banco — status,
     * sessão de origem, frete — para a tela perguntar ao entregador se quer
     * trazê-la para a jornada atual (ver DeliveryStopRepository.transferToSession).
     */
    data class ExistsInOtherSession(val existing: DeliveryStop) : RegisterStopResult
}
