package com.otimiza.delivery.data.repository

import com.otimiza.delivery.data.local.dao.DeliveryStopDao
import com.otimiza.delivery.data.local.mapper.toDomain
import com.otimiza.delivery.data.local.mapper.toEntity
import com.otimiza.delivery.data.remote.VrpEngineClient
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeliveryRepositoryImpl @Inject constructor(
    private val dao: DeliveryStopDao,
    private val vrpEngineClient: VrpEngineClient
) : DeliveryRepository {

    override suspend fun findStopsBySession(sessionId: String): List<DeliveryStop> =
        dao.findBySession(sessionId).map { it.toDomain() }

    override fun observeStopsBySession(sessionId: String): Flow<List<DeliveryStop>> =
        dao.observeBySession(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveStop(stop: DeliveryStop) {
        dao.upsert(stop.toEntity())
    }

    override suspend fun optimizeSession(sessionId: String): Result<List<DeliveryStop>> =
        runCatching {
            val stops = findStopsBySession(sessionId)
            vrpEngineClient.optimizeRoute(stops)
        }
}
