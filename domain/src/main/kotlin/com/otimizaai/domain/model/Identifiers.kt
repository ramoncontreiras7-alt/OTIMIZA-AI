package com.otimizaai.domain.model

/*
 * ============================================================================
 *  IDENTIFICADORES DE DOMÍNIO — REGRA #1 (Imutabilidade Absoluta dos IDs)
 * ============================================================================
 *  Estes tipos embrulham o texto bruto do ID para que o compilador impeça
 *  trocas acidentais (ex.: passar um PlatformId onde se espera um NativeStopId).
 *
 *  Política de validação: REJEITAR, NUNCA CORRIGIR.
 *  O domínio jamais aplica trim(), uppercase(), hash, truncamento ou qualquer
 *  normalização. Se o valor chegar "sujo", a falha é de quem extraiu o dado
 *  (OCR, acessibilidade, parser) e deve ser corrigida lá — nunca aqui.
 * ============================================================================
 */

/** ID original da parada, exatamente como a plataforma de entrega o forneceu. */
@JvmInline
value class NativeStopId(val value: String) {
    init {
        require(value.isNotBlank()) { "NativeStopId não pode ser vazio." }
        require(value == value.trim()) {
            "NativeStopId com espaços nas bordas: [$value]. " +
                "O domínio não normaliza IDs — corrija a extração na origem."
        }
    }
}

/** Código estável e persistido de uma plataforma (independe do nome do enum). */
@JvmInline
value class PlatformId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlatformId não pode ser vazio." }
        require(value == value.trim()) { "PlatformId com espaços nas bordas: [$value]." }
    }
}

/** Identificador da sessão de trabalho (turno/jornada) à qual a parada pertence. */
@JvmInline
value class RouteSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "RouteSessionId não pode ser vazio." }
    }
}

/**
 * Identidade de negócio de uma parada = chave composta (native_stop_id, platform_id).
 * Espelha no domínio a PRIMARY KEY composta que o Room usará no Passo 2.
 * Útil como chave de Map/Set (deduplicação, reordenação VRP, seleção no mapa).
 */
data class StopKey(
    val nativeStopId: NativeStopId,
    val platformId: PlatformId,
)
