package com.otimiza.delivery.data.remote

import android.util.Log
import com.otimiza.delivery.domain.model.DeliveryStop
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException

interface VrpEngineClient {
    suspend fun optimizeRoute(stops: List<DeliveryStop>): List<DeliveryStop>
}

class VrpEngineClientImpl(
    private val api: VrpApiService,
    private val baseUrl: String,
    private val maxRetries: Int = 3
) : VrpEngineClient {

    override suspend fun optimizeRoute(stops: List<DeliveryStop>): List<DeliveryStop> {
        if (stops.isEmpty()) return emptyList()

        val payload = VrpOptimizationRequest(
            sessionId = stops.first().routeId ?: error("routeId obrigatório"),
            stops = stops.mapIndexed { index, s ->
                VrpStopPayload(
                    nativeStopId = s.id.value,
                    platformId = s.externalRef.value,
                    latitude = s.latitude,
                    longitude = s.longitude,
                    sequence = index
                )
            },
            platformIds = stops.map { it.externalRef.value }
        )

        var attempt = 0
        var lastError: Throwable? = null

        while (attempt < maxRetries) {
            try {
                val response: Response<VrpOptimizationResponse> = api.optimizeRoute(
                    endpoint = "$baseUrl/optimize",
                    request = payload
                )
                if (response.isSuccessful) {
                    val body = response.body()
                        ?: throw VrpEngineException("Response vazio do motor VRP")
                    return body.optimizedSequence.mapNotNull { idx -> stops.getOrNull(idx) }
                }
                throw VrpEngineException("VRP HTTP ${response.code()}: ${response.errorBody()?.string()}")
            } catch (e: IOException) {
                lastError = e
                attempt++
                Log.w(TAG, "VRP offline tentativa $attempt/$maxRetries: ${e.message}")
                if (attempt < maxRetries) delay(1_000L * attempt)
            } catch (e: VrpEngineException) {
                lastError = e
                attempt++
                Log.w(TAG, "VRP erro $attempt/$maxRetries: ${e.message}")
                if (attempt < maxRetries) delay(1_000L * attempt)
            }
        }

        throw VrpEngineException(
            "Falha após $maxRetries tentativas: ${lastError?.message ?: "sem detalhes"}"
        )
    }

    companion object {
        private const val TAG = "VrpEngineClient"
    }
}

class VrpEngineException(message: String) : RuntimeException(message)