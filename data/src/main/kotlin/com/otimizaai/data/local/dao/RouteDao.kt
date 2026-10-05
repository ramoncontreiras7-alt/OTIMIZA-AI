package com.otimizaai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.otimizaai.data.local.entity.RouteEntity
import com.otimizaai.data.local.entity.RouteSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RouteDao {

    @Query(
        "SELECT r.*, " +
            "(SELECT COUNT(*) FROM delivery_stops s WHERE s.session_id = r.id) AS total, " +
            "(SELECT COUNT(*) FROM delivery_stops s WHERE s.session_id = r.id AND s.status = 'PENDING') AS pending " +
            "FROM routes r ORDER BY r.date DESC, r.created_at DESC"
    )
    abstract fun observeSummaries(): Flow<List<RouteSummaryRow>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE date = :date ORDER BY created_at DESC LIMIT 1")
    abstract suspend fun findLatestOn(date: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(route: RouteEntity)

    @Query("UPDATE routes SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: String, name: String)

    @Query("DELETE FROM routes WHERE id = :id")
    abstract suspend fun deleteRoute(id: String)

    @Query("DELETE FROM delivery_stops WHERE session_id = :id")
    abstract suspend fun deleteStopsOf(id: String)

    /** Apaga a rota e as paradas dela, juntas. */
    @Transaction
    open suspend fun deleteWithStops(id: String) {
        deleteStopsOf(id)
        deleteRoute(id)
    }
}
