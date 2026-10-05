package com.otimizaai.data.local.mapper

import com.otimizaai.data.local.dao.StatusCountRow
import com.otimizaai.data.local.entity.DeliveryStopEntity
import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.GeoCoordinate
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.PlatformId
import com.otimizaai.domain.model.RouteSessionId

/**
 * Tradução entre a linha do banco e o objeto de domínio.
 *
 * REGRA #1: os IDs são copiados byte a byte, sem trim, sem uppercase e sem
 * qualquer transformação. `.value` sai do domínio e volta embrulhado igual.
 */

fun DeliveryStop.toEntity(routeOrder: Int = 0): DeliveryStopEntity = DeliveryStopEntity(
    nativeStopId = id.value,
    platformId = platformId.value,
    sessionId = sessionId.value,
    latitude = latitude,
    longitude = longitude,
    addressFormatted = address.formatted,
    addressComplement = address.complement,
    addressNeighborhood = address.neighborhood,
    addressCity = address.city,
    addressPostalCode = address.postalCode,
    status = status.code,
    freightCents = freightCents,
    routeOrder = routeOrder,
)

fun DeliveryStopEntity.toDomain(): DeliveryStop = DeliveryStop.create(
    id = NativeStopId(nativeStopId),
    platform = Platform.fromId(PlatformId(platformId)),
    sessionId = RouteSessionId(sessionId),
    address = DeliveryAddress(
        formatted = addressFormatted,
        complement = addressComplement,
        neighborhood = addressNeighborhood,
        city = addressCity,
        postalCode = addressPostalCode,
    ),
    coordinate = toCoordinateOrNull(),
    status = DeliveryStatus.fromCode(status),
    freightCents = freightCents,
)

fun List<DeliveryStopEntity>.toDomain(): List<DeliveryStop> = map { it.toDomain() }

/**
 * Converte o agrupamento do banco num mapa completo: todo status aparece,
 * mesmo os que têm zero paradas (o banco só devolve os que existem).
 */
fun List<StatusCountRow>.toStatusCounts(): Map<DeliveryStatus, Int> {
    val found = associate { DeliveryStatus.fromCode(it.status) to it.total }
    return DeliveryStatus.entries.associateWith { found[it] ?: 0 }
}

/**
 * As duas colunas precisam estar ambas preenchidas ou ambas vazias.
 * Uma linha com só uma delas é dado corrompido: falhamos alto em vez de
 * inventar um valor.
 */
private fun DeliveryStopEntity.toCoordinateOrNull(): GeoCoordinate? {
    val lat = latitude
    val lng = longitude
    return when {
        lat == null && lng == null -> null
        lat != null && lng != null -> GeoCoordinate(lat, lng)
        else -> throw IllegalStateException(
            "Coordenada incompleta no banco para a parada " +
                "[$nativeStopId]/[$platformId]: lat=$lat, lng=$lng"
        )
    }
}
