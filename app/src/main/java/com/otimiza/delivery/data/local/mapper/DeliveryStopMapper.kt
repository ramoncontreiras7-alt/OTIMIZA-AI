package com.otimiza.delivery.data.local.mapper

import com.otimiza.delivery.data.local.entity.DeliveryStopEntity
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.PlatformId
import java.time.Instant

fun DeliveryStop.toEntity(): DeliveryStopEntity = DeliveryStopEntity(
    nativeStopId = id.value,
    platformId = externalRef.value,
    platform = platform,
    address = address,
    latitude = latitude,
    longitude = longitude,
    sequence = sequence,
    routeId = routeId,
    createdAt = createdAt.epochSecond,
    surrogateKey = surrogateKey
)

fun DeliveryStopEntity.toDomain(): DeliveryStop = DeliveryStop(
    id = NativeStopId(nativeStopId),
    platform = platform,
    externalRef = PlatformId(platformId),
    address = address,
    latitude = latitude,
    longitude = longitude,
    sequence = sequence,
    routeId = routeId,
    createdAt = Instant.ofEpochSecond(createdAt),
    surrogateKey = surrogateKey
)
