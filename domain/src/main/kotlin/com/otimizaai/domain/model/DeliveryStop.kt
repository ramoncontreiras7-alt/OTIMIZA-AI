package com.otimizaai.domain.model

/**
 * Uma parada de entrega — a entidade central do Otimiza AI.
 *
 * BLINDAGEM DA REGRA #1
 * ---------------------
 * Um `data class` comum gera automaticamente um método `copy()` público, o que
 * permitiria `stop.copy(id = NativeStopId("OUTRO"))` em qualquer parte do app.
 * Para fechar essa brecha:
 *
 *  1. O construtor é `private`.
 *  2. `@ConsistentCopyVisibility` torna o `copy()` tão privado quanto o construtor
 *     (requer Kotlin 2.0.20+).
 *  3. As únicas alterações permitidas são as funções `with...`/`transferTo`
 *     abaixo, que por construção preservam `id` e `platform`.
 *
 * PARADAS PENDENTES DE GEOCODIFICAÇÃO
 * -----------------------------------
 * [latitude] e [longitude] são `Double?`, derivadas de um único [coordinate].
 * Isso impede o estado "meia coordenada". Só [isRoutable] = true vai ao VRP.
 *
 * VALORES FINANCEIROS
 * -------------------
 * [freightCents] é o frete em CENTAVOS (Int). R$ 12,50 = 1250.
 * Somas em ponto flutuante (Double) acumulam erros de centavos; inteiros não.
 */
@ConsistentCopyVisibility
data class DeliveryStop private constructor(
    val id: NativeStopId,
    val platform: Platform,
    val sessionId: RouteSessionId,
    val coordinate: GeoCoordinate?,
    val address: DeliveryAddress,
    val status: DeliveryStatus,
    val freightCents: Int,
) {

    init {
        require(freightCents >= 0) { "Frete não pode ser negativo: $freightCents centavos" }
    }

    /** Código persistido da plataforma (segunda metade da chave composta). */
    val platformId: PlatformId
        get() = platform.id

    /** Identidade de negócio: (native_stop_id, platform_id). */
    val key: StopKey
        get() = StopKey(nativeStopId = id, platformId = platformId)

    val latitude: Double?
        get() = coordinate?.latitude

    val longitude: Double?
        get() = coordinate?.longitude

    /** true quando a parada já tem coordenada e pode entrar na otimização de rota. */
    val isRoutable: Boolean
        get() = latitude != null && longitude != null

    /** Resultado da geocodificação ou ajuste no mapa. O ID permanece intocado. */
    fun withCoordinate(newCoordinate: GeoCoordinate): DeliveryStop =
        copy(coordinate = newCoordinate)

    /**
     * Correção do endereço pelo entregador. A coordenada antiga deixa de ser
     * confiável e a parada volta a ficar pendente de geocodificação.
     */
    fun withAddress(newAddress: DeliveryAddress): DeliveryStop =
        copy(address = newAddress, coordinate = null)

    /** Marca entregue / falhou / pendente. O ID permanece intocado. */
    fun withStatus(newStatus: DeliveryStatus): DeliveryStop =
        copy(status = newStatus)

    /** Ajuste do valor do frete (em centavos). O ID permanece intocado. */
    fun withFreightCents(newFreightCents: Int): DeliveryStop =
        copy(freightCents = newFreightCents)

    /**
     * REENTREGA — só deve ser chamada DEPOIS de o entregador confirmar que quer
     * trazer a parada de outra jornada para a atual. Mantém ID, plataforma,
     * endereço, coordenada e frete; troca a sessão e volta o status para PENDING.
     */
    fun transferTo(newSessionId: RouteSessionId): DeliveryStop =
        copy(sessionId = newSessionId, status = DeliveryStatus.PENDING)

    companion object {
        /** Único ponto de criação de uma parada. */
        fun create(
            id: NativeStopId,
            platform: Platform,
            sessionId: RouteSessionId,
            address: DeliveryAddress,
            coordinate: GeoCoordinate? = null,
            status: DeliveryStatus = DeliveryStatus.PENDING,
            freightCents: Int = 0,
        ): DeliveryStop = DeliveryStop(
            id = id,
            platform = platform,
            sessionId = sessionId,
            coordinate = coordinate,
            address = address,
            status = status,
            freightCents = freightCents,
        )
    }
}
