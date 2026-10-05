package com.otimizaai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `routes`: uma rota/jornada.
 * [id] é o mesmo valor gravado em `delivery_stops.session_id`.
 * [date] no formato AAAA-MM-DD (ordena corretamente como texto).
 */
@Entity(tableName = RouteEntity.TABLE_NAME)
data class RouteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    companion object {
        const val TABLE_NAME = "routes"
    }
}

/** Rota + contadores calculados pelo banco. */
data class RouteSummaryRow(
    @Embedded val route: RouteEntity,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "pending") val pending: Int,
)
