package com.otimizaai.domain.repository

import com.otimizaai.domain.model.Route
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.RouteSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Contrato das rotas (jornadas). */
interface RouteRepository {

    /** Todas as rotas, da mais recente para a mais antiga, com contadores vivos. */
    fun observeAll(): Flow<List<RouteSummary>>

    suspend fun findById(id: RouteSessionId): Route?

    /** A rota mais recente com a data informada, se houver. */
    suspend fun findLatestOn(date: LocalDate): Route?

    suspend fun create(name: String, date: LocalDate): Route

    suspend fun rename(id: RouteSessionId, name: String)

    /** Apaga a rota e as paradas dela. */
    suspend fun delete(id: RouteSessionId)
}
