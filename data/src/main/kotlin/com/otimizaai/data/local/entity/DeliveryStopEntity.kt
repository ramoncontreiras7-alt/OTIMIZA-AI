package com.otimizaai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Linha da tabela `delivery_stops`.
 *
 * REGRA #1 — CHAVE PRIMÁRIA COMPOSTA
 * A identidade da parada é o par (native_stop_id, platform_id). Não existe
 * coluna de ID autogerado: o banco guarda e busca pelo ID original da plataforma.
 *
 * Os campos aqui são tipos simples (String/Double/Int) porque o Room não trabalha
 * bem com value classes. A proteção dos tipos é reaplicada no mapper.
 *
 * - latitude/longitude anuláveis: NULL = pendente de geocodificação.
 *   Nunca usar 0.0 como "sem coordenada" — (0,0) é um ponto real no oceano.
 * - status: código estável do DeliveryStatus (PENDING / DELIVERED / FAILED).
 * - freight_cents: frete em centavos, inteiro. R$ 12,50 = 1250.
 *
 * Índice (session_id, status): acelera tanto "todas as paradas da sessão"
 * quanto "paradas da sessão com status X" — o SQLite usa a primeira coluna
 * do índice sozinha quando a consulta filtra só por sessão.
 */
@Entity(
    tableName = DeliveryStopEntity.TABLE_NAME,
    primaryKeys = [DeliveryStopEntity.COL_NATIVE_STOP_ID, DeliveryStopEntity.COL_PLATFORM_ID],
    indices = [Index(value = [DeliveryStopEntity.COL_SESSION_ID, DeliveryStopEntity.COL_STATUS])],
)
data class DeliveryStopEntity(
    @ColumnInfo(name = COL_NATIVE_STOP_ID)
    val nativeStopId: String,

    @ColumnInfo(name = COL_PLATFORM_ID)
    val platformId: String,

    @ColumnInfo(name = COL_SESSION_ID)
    val sessionId: String,

    @ColumnInfo(name = COL_LATITUDE)
    val latitude: Double?,

    @ColumnInfo(name = COL_LONGITUDE)
    val longitude: Double?,

    @ColumnInfo(name = "address_formatted")
    val addressFormatted: String,

    @ColumnInfo(name = "address_complement")
    val addressComplement: String?,

    @ColumnInfo(name = "address_neighborhood")
    val addressNeighborhood: String?,

    @ColumnInfo(name = "address_city")
    val addressCity: String?,

    @ColumnInfo(name = "address_postal_code")
    val addressPostalCode: String?,

    @ColumnInfo(name = COL_STATUS)
    val status: String,

    @ColumnInfo(name = COL_FREIGHT_CENTS)
    val freightCents: Int,

    /** Posição na rota (1, 2, 3...). Só define a ORDEM; não tem relação com o ID. */
    @ColumnInfo(name = COL_ROUTE_ORDER, defaultValue = "0")
    val routeOrder: Int = 0,
) {
    companion object {
        const val TABLE_NAME = "delivery_stops"
        const val COL_NATIVE_STOP_ID = "native_stop_id"
        const val COL_PLATFORM_ID = "platform_id"
        const val COL_SESSION_ID = "session_id"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_STATUS = "status"
        const val COL_FREIGHT_CENTS = "freight_cents"
        const val COL_ROUTE_ORDER = "route_order"
    }
}
