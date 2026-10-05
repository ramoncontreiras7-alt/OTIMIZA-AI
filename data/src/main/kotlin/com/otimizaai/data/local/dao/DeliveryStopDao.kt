package com.otimizaai.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.otimizaai.data.local.entity.DeliveryStopEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acesso à tabela `delivery_stops`.
 *
 * GARANTIA DA REGRA #1 NAS ESCRITAS
 * - Nenhum comando UPDATE deste DAO coloca native_stop_id ou platform_id no SET.
 *   As duas colunas aparecem apenas no WHERE, como critério de busca.
 * - @Upsert usa a chave primária para decidir entre inserir ou atualizar;
 *   a chave é o que ENCONTRA a linha, portanto nunca é sobrescrita.
 *
 * REENTREGA
 * - A inserção da entrada de dados usa IGNORE: se a chave já existe (mesmo em
 *   outra sessão), NADA é alterado. O bloqueio natural da chave primária é
 *   mantido e a linha existente é devolvida para a camada superior decidir.
 * - A transferência de sessão só acontece por [transferToSession], chamada
 *   explicitamente após a confirmação do entregador.
 *
 * A ordem "de captura" usa o `rowid` interno do SQLite (cresce a cada inserção
 * e não muda em updates), sem precisar de uma coluna de ID artificial.
 */
@Dao
abstract class DeliveryStopDao {

    // ---------------------------------------------------------------- Leituras

    @Query(
        "SELECT * FROM delivery_stops WHERE session_id = :sessionId ORDER BY route_order ASC, rowid ASC"
    )
    abstract fun observeBySession(sessionId: String): Flow<List<DeliveryStopEntity>>

    @Query(
        "SELECT * FROM delivery_stops WHERE session_id = :sessionId ORDER BY route_order ASC, rowid ASC"
    )
    abstract suspend fun findBySession(sessionId: String): List<DeliveryStopEntity>

    @Query(
        "SELECT * FROM delivery_stops " +
            "WHERE session_id = :sessionId AND status = :status " +
            "ORDER BY route_order ASC, rowid ASC"
    )
    abstract suspend fun findBySessionAndStatus(
        sessionId: String,
        status: String,
    ): List<DeliveryStopEntity>

    /**
     * Contadores para a legenda do mapa, calculados pelo próprio banco
     * (sem carregar todas as paradas na memória). Atualiza sozinho.
     */
    @Query(
        "SELECT status, COUNT(*) AS total FROM delivery_stops " +
            "WHERE session_id = :sessionId GROUP BY status"
    )
    abstract fun observeStatusCounts(sessionId: String): Flow<List<StatusCountRow>>

    @Query(
        "SELECT * FROM delivery_stops " +
            "WHERE native_stop_id = :nativeStopId AND platform_id = :platformId LIMIT 1"
    )
    abstract suspend fun findByKey(nativeStopId: String, platformId: String): DeliveryStopEntity?

    @Query(
        "SELECT * FROM delivery_stops " +
            "WHERE session_id = :sessionId AND (latitude IS NULL OR longitude IS NULL) " +
            "ORDER BY route_order ASC, rowid ASC"
    )
    abstract suspend fun findPendingGeocoding(sessionId: String): List<DeliveryStopEntity>

    // ---------------------------------------------------------------- Escritas

    /**
     * Insere só se a chave não existir. Retorna o rowid, ou -1 se já existia.
     * Uma releitura da mesma etiqueta NUNCA apaga dados já gravados.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(entity: DeliveryStopEntity): Long

    /** Inserção ou substituição dos campos editáveis, para edições explícitas. */
    @Upsert
    abstract suspend fun upsert(entity: DeliveryStopEntity)

    /** Atualiza SOMENTE a coordenada. Retorna quantas linhas mudaram (0 ou 1). */
    @Query(
        "UPDATE delivery_stops SET latitude = :latitude, longitude = :longitude " +
            "WHERE native_stop_id = :nativeStopId AND platform_id = :platformId"
    )
    abstract suspend fun updateCoordinate(
        nativeStopId: String,
        platformId: String,
        latitude: Double,
        longitude: Double,
    ): Int

    /** Atualiza SOMENTE o status. Retorna quantas linhas mudaram (0 ou 1). */
    @Query(
        "UPDATE delivery_stops SET status = :status " +
            "WHERE native_stop_id = :nativeStopId AND platform_id = :platformId"
    )
    abstract suspend fun updateStatus(
        nativeStopId: String,
        platformId: String,
        status: String,
    ): Int

    /**
     * REENTREGA: troca SOMENTE a sessão e o status. Chamada apenas após o
     * entregador confirmar. Retorna quantas linhas mudaram (0 ou 1).
     */
    @Query(
        "UPDATE delivery_stops SET session_id = :targetSessionId, status = :resetStatus, " +
            "route_order = (SELECT COALESCE(MAX(route_order), 0) + 1 FROM delivery_stops WHERE session_id = :targetSessionId) " +
            "WHERE native_stop_id = :nativeStopId AND platform_id = :platformId"
    )
    abstract suspend fun transferToSession(
        nativeStopId: String,
        platformId: String,
        targetSessionId: String,
        resetStatus: String,
    ): Int

    @Query("DELETE FROM delivery_stops WHERE session_id = :sessionId")
    abstract suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM delivery_stops WHERE native_stop_id = :nativeStopId AND platform_id = :platformId")
    abstract suspend fun deleteByKey(nativeStopId: String, platformId: String): Int

    /** Próxima posição livre no fim da rota. */
    @Query("SELECT COALESCE(MAX(route_order), 0) + 1 FROM delivery_stops WHERE session_id = :sessionId")
    abstract suspend fun nextOrder(sessionId: String): Int

    /** Atualiza SOMENTE a posição na rota. */
    @Query(
        "UPDATE delivery_stops SET route_order = :order " +
            "WHERE native_stop_id = :nativeStopId AND platform_id = :platformId"
    )
    abstract suspend fun updateOrder(nativeStopId: String, platformId: String, order: Int): Int

    /** Paradas pendentes que estão em OUTRAS rotas. */
    @Query(
        "SELECT * FROM delivery_stops WHERE session_id != :sessionId AND status = 'PENDING' " +
            "ORDER BY session_id ASC, route_order ASC, rowid ASC"
    )
    abstract suspend fun findPendingOutside(sessionId: String): List<DeliveryStopEntity>

    // --------------------------------------------------------- Operação composta

    /**
     * Tenta inserir e, se a chave já existir, devolve a linha existente sem
     * alterá-la. Tudo numa única transação, para que duas leituras simultâneas
     * (câmera + acessibilidade) não se atropelem.
     *
     * @return null se inseriu; a linha já existente caso contrário.
     */
    @Transaction
    open suspend fun insertOrGetExisting(entity: DeliveryStopEntity): DeliveryStopEntity? {
        // A parada nova entra no FIM da rota.
        val rowId = insertIfAbsent(entity.copy(routeOrder = nextOrder(entity.sessionId)))
        if (rowId != -1L) return null
        return findByKey(entity.nativeStopId, entity.platformId)
    }

    /** Grava a ordem completa de uma rota numa única transação. */
    @Transaction
    open suspend fun applyOrder(keys: List<Pair<String, String>>) {
        keys.forEachIndexed { index, (nativeStopId, platformId) ->
            updateOrder(nativeStopId, platformId, index + 1)
        }
    }
}

/** Resultado de uma linha do agrupamento por status. */
data class StatusCountRow(
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "total") val total: Int,
)
