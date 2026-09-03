package com.otimiza.delivery.data.remote

import com.otimiza.delivery.domain.model.DeliveryStop

interface VrpEngineClient {
    suspend fun optimizeRoute(stops: List<DeliveryStop>): List<DeliveryStop>
}

class VrpEngineClientImpl(
    private val api: VrpApiService,
    private val baseUrl: String
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

        val response = api.optimizeRoute(endpoint = "$baseUrl/optimize", request = payload)
        if (!response.isSuccessful) {
            throw VrpEngineException("VRP ${response.code()}: ${response.errorBody()?.string()}")
        }
        val body = response.body() ?: throw VrpEngineException("Response vazio do motor VRP")

        // O motor devolve índices; reordenamos a lista ORIGINAL preservando os IDs nativos
        return body.optimizedSequence.mapNotNull { idx ->
            stops.getOrNull(idx)
        }
    }
}

class VrpEngineException(message: String) : RuntimeException(message)
