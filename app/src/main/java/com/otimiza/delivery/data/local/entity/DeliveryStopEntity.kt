package com.otimiza.delivery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.otimiza.delivery.domain.model.Platform

@Entity(
    tableName = "delivery_stops",
    primaryKeys = ["native_stop_id", "platform_id"]
)
data class DeliveryStopEntity(
    @ColumnInfo(name = "native_stop_id") val nativeStopId: String,
    @ColumnInfo(name = "platform_id") val platformId: String,
    @ColumnInfo(name = "platform") val platform: Platform,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "sequence") val sequence: Int,
    @ColumnInfo(name = "route_id") val routeId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "surrogate_key") val surrogateKey: String?
)
