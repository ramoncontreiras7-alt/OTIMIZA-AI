package com.otimizaai.data.repository

import com.otimizaai.data.local.dao.DeliveryStopDao
import com.otimizaai.data.local.mapper.toDomain
import com.otimizaai.data.local.mapper.toEntity
import com.otimizaai.data.local.mapper.toStatusCounts
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.GeoCoordinate
import com.otimizaai.domain.model.RegisterStopResult
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.StopKey
import com.otimizaai.domain.repository.DeliveryStopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementação do contrato de domínio usando Room.
 * É a "ponte": recebe objetos de domínio, grava linhas; lê linhas, devolve domínio.
 */
class DeliveryStopRepositoryImpl @Inject constructor(
    private val dao: DeliveryStopDao,
) : DeliveryStopRepository {

    override fun observeBySession(sessionId: RouteSessionId): Flow<List<DeliveryStop>> =
        dao.observeBySession(sessionId.value).map { it.toDomain() }

    override suspend fun findBySession(sessionId: RouteSessionId): List<DeliveryStop> =
        dao.findBySession(sessionId.value).toDomain()

    override suspend fun findBySessionAndStatus(
        sessionId: RouteSessionId,
        status: DeliveryStatus,
    ): List<DeliveryStop> =
        dao.findBySessionAndStatus(sessionId.value, status.code).toDomain()

    override fun observeStatusCounts(sessionId: RouteSessionId): Flow<Map<DeliveryStatus, Int>> =
        dao.observeStatusCounts(sessionId.value).map { it.toStatusCounts() }

    override suspend fun findByKey(key: StopKey): DeliveryStop? =
        dao.findByKey(key.nativeStopId.value, key.platformId.value)?.toDomain()

    override suspend fun findPendingGeocoding(sessionId: RouteSessionId): List<DeliveryStop> =
        dao.findPendingGeocoding(sessionId.value).toDomain()

    override suspend fun register(stop: DeliveryStop): RegisterStopResult {
        val existing = dao.insertOrGetExisting(stop.toEntity())
            ?: return RegisterStopResult.Inserted

        return if (existing.sessionId == stop.sessionId.value) {
            RegisterStopResult.AlreadyInSession
        } else {
            // Bloqueio mantido: nada foi movido. A tela decide com o entregador.
            RegisterStopResult.ExistsInOtherSession(existing.toDomain())
        }
    }

    override suspend fun upsert(stop: DeliveryStop) {
        // Edição não pode mexer na posição da parada na rota.
        val currentOrder = dao.findByKey(stop.id.value, stop.platformId.value)?.routeOrder
            ?: dao.nextOrder(stop.sessionId.value)
        dao.upsert(stop.toEntity(routeOrder = currentOrder))
    }

    override suspend fun updateCoordinate(key: StopKey, coordinate: GeoCoordinate): Boolean =
        dao.updateCoordinate(
            nativeStopId = key.nativeStopId.value,
            platformId = key.platformId.value,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
        ) > 0

    override suspend fun updateStatus(key: StopKey, status: DeliveryStatus): Boolean =
        dao.updateStatus(
            nativeStopId = key.nativeStopId.value,
            platformId = key.platformId.value,
            status = status.code,
        ) > 0

    override suspend fun transferToSession(
        key: StopKey,
        targetSessionId: RouteSessionId,
    ): Boolean =
        dao.transferToSession(
            nativeStopId = key.nativeStopId.value,
            platformId = key.platformId.value,
            targetSessionId = targetSessionId.value,
            resetStatus = DeliveryStatus.PENDING.code,
        ) > 0

    override suspend fun deleteBySession(sessionId: RouteSessionId) {
        dao.deleteBySession(sessionId.value)
    }

    override suspend fun delete(key: StopKey) {
        dao.deleteByKey(key.nativeStopId.value, key.platformId.value)
    }

    override suspend fun reorder(sessionId: RouteSessionId, keysInOrder: List<StopKey>) {
        dao.applyOrder(keysInOrder.map { it.nativeStopId.value to it.platformId.value })
    }

    override suspend fun transferPendingFromOtherSessions(targetSessionId: RouteSessionId): Int {
        val pending = dao.findPendingOutside(targetSessionId.value)
        pending.forEach {
            dao.transferToSession(it.nativeStopId, it.platformId, targetSessionId.value, DeliveryStatus.PENDING.code)
        }
        return pending.size
    }
}
