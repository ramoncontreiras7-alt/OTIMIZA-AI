package com.otimiza.delivery.domain.usecase

import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.repository.DeliveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UnifiedRoutingUseCase @Inject constructor(
    private val repository: DeliveryRepository
) {

    suspend operator fun invoke(sessionId: String): Result<List<DeliveryStop>> =
        withContext(Dispatchers.IO) {
            repository.optimizeSession(sessionId)
        }
}
