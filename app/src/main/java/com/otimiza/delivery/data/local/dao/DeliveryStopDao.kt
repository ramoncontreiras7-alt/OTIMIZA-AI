package com.otimiza.delivery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.otimiza.delivery.data.local.entity.DeliveryStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryStopDao {

    @Query("SELECT * FROM delivery_stops WHERE route_id = :sessionId")
    suspend fun findBySession(sessionId: String): List<DeliveryStopEntity>

    @Query("SELECT * FROM delivery_stops WHERE route_id = :sessionId")
    fun observeBySession(sessionId: String): Flow<List<DeliveryStopEntity>>

    @Query("SELECT * FROM delivery_stops WHERE route_id = :routeId ORDER BY sequence ASC")
    suspend fun findByRoute(routeId: String): List<DeliveryStopEntity>

    @Query("SELECT * FROM delivery_stops WHERE native_stop_id = :nativeStopId AND platform_id = :platformId")
    suspend fun findById(nativeStopId: String, platformId: String): DeliveryStopEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DeliveryStopEntity)

    @Transaction
    suspend fun upsert(entity: DeliveryStopEntity) {
        insert(entity)
    }

    @Query("DELETE FROM delivery_stops WHERE native_stop_id = :nativeStopId AND platform_id = :platformId")
    suspend fun delete(nativeStopId: String, platformId: String)

    @Query("UPDATE delivery_stops SET sequence = :newSequence WHERE route_id = :sessionId AND native_stop_id = :nativeId AND platform_id = :platformId")
    suspend fun updateSequence(sessionId: String, nativeId: String, platformId: String, newSequence: Int)
}
