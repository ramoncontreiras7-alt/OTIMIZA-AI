package com.otimiza.delivery.domain.repository

import com.otimiza.delivery.domain.model.DeliveryStop
import kotlinx.coroutines.flow.Flow

interface DeliveryRepository {

    suspend fun findStopsBySession(sessionId: String): List<DeliveryStop>

    fun observeStopsBySession(sessionId: String): Flow<List<DeliveryStop>>

    suspend fun saveStop(stop: DeliveryStop)

    suspend fun optimizeSession(sessionId: String): Result<List<DeliveryStop>>
}
