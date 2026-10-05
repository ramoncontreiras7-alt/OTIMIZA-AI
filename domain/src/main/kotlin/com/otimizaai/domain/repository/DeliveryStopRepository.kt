package com.otimizaai.domain.repository

import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.GeoCoordinate
import com.otimizaai.domain.model.RegisterStopResult
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.StopKey
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso às paradas. O domínio define O QUE precisa;
 * o módulo `data` (Room) decide COMO entregar.
 *
 * Nenhuma operação deste contrato permite alterar a chave (ID nativo + plataforma).
 */
interface DeliveryStopRepository {

    /** Lista viva: a tela atualiza sozinha quando o banco muda. */
    fun observeBySession(sessionId: RouteSessionId): Flow<List<DeliveryStop>>

    /** Retrato único da sessão, em ordem de captura. */
    suspend fun findBySession(sessionId: RouteSessionId): List<DeliveryStop>

    /** Paradas da sessão filtradas por status (ex.: só as pendentes). */
    suspend fun findBySessionAndStatus(
        sessionId: RouteSessionId,
        status: DeliveryStatus,
    ): List<DeliveryStop>

    /**
     * Contadores vivos por status para a legenda do mapa.
     * Todo status aparece no mapa, mesmo com zero.
     */
    fun observeStatusCounts(sessionId: RouteSessionId): Flow<Map<DeliveryStatus, Int>>

    suspend fun findByKey(key: StopKey): DeliveryStop?

    /** Paradas ainda sem coordenada — fila de trabalho da geocodificação. */
    suspend fun findPendingGeocoding(sessionId: RouteSessionId): List<DeliveryStop>

    /**
     * Usado pela ENTRADA de dados (OCR, acessibilidade, romaneio).
     * Nunca sobrescreve nem move uma parada existente — só insere se for nova.
     */
    suspend fun register(stop: DeliveryStop): RegisterStopResult

    /**
     * Substitui os dados editáveis de uma parada. Usado por EDIÇÕES explícitas.
     * A chave é o critério de busca, nunca é reescrita.
     */
    suspend fun upsert(stop: DeliveryStop)

    /** Grava só a coordenada. Retorna false se a parada não existir. */
    suspend fun updateCoordinate(key: StopKey, coordinate: GeoCoordinate): Boolean

    /** Grava só o status. Retorna false se a parada não existir. */
    suspend fun updateStatus(key: StopKey, status: DeliveryStatus): Boolean

    /**
     * REENTREGA: traz uma parada de outra jornada para [targetSessionId] e volta
     * o status para PENDING. Só deve ser chamada após confirmação do entregador.
     * Retorna false se a parada não existir.
     */
    suspend fun transferToSession(key: StopKey, targetSessionId: RouteSessionId): Boolean

    suspend fun deleteBySession(sessionId: RouteSessionId)

    /** Remove UMA parada (decisão explícita do entregador). */
    suspend fun delete(key: StopKey)

    /**
     * Grava a nova ordem da rota. [keysInOrder] é a lista completa na ordem desejada.
     * Só a posição muda; ID e plataforma ficam intocados.
     */
    suspend fun reorder(sessionId: RouteSessionId, keysInOrder: List<StopKey>)

    /**
     * Traz para [targetSessionId] todas as paradas PENDENTES de outras rotas
     * (como "Reutilizar paradas anteriores" do Spoke). Retorna quantas vieram.
     */
    suspend fun transferPendingFromOtherSessions(targetSessionId: RouteSessionId): Int
}
