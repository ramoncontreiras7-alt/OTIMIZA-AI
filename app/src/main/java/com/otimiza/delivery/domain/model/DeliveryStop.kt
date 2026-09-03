package com.otimiza.delivery.domain.model

import java.time.Instant

@JvmInline
value class NativeStopId(val value: String)

@JvmInline
value class PlatformId(val value: String)

data class DeliveryStop(
    val id: NativeStopId,
    val platform: Platform,
    val externalRef: PlatformId,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int,
    val routeId: String?,
    val createdAt: Instant,
    val surrogateKey: String? = null
) {
    val compositeKey: Pair<String, String> get() = id.value to externalRef.value
}
