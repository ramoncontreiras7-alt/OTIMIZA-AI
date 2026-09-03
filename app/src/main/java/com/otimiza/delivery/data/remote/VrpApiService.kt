package com.otimizaai.delivery.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class VrpOptimizationRequest(
    val sessionId: String,
    val stops: List<VrpStopPayload>,
    val platformIds: List<String>
)

data class VrpStopPayload(
    val nativeStopId: String,
    val platformId: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int
)

data class VrpOptimizationResponse(
    val routeId: String,
    val optimizedSequence: List<Int>,
    val totalDistanceMeters: Double
)

interface VrpApiService {
    @POST
    suspend fun optimizeRoute(
        @Url endpoint: String,
        @Body request: VrpOptimizationRequest
    ): Response<VrpOptimizationResponse>
}
