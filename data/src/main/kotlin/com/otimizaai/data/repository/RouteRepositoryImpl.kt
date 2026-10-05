package com.otimizaai.data.repository

import com.otimizaai.data.local.dao.RouteDao
import com.otimizaai.data.local.entity.RouteEntity
import com.otimizaai.domain.model.Route
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.RouteSummary
import com.otimizaai.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val dao: RouteDao,
) : RouteRepository {

    override fun observeAll(): Flow<List<RouteSummary>> =
        dao.observeSummaries().map { rows ->
            rows.map { RouteSummary(it.route.toDomain(), it.total, it.pending) }
        }

    override suspend fun findById(id: RouteSessionId): Route? = dao.findById(id.value)?.toDomain()

    override suspend fun findLatestOn(date: LocalDate): Route? = dao.findLatestOn(date.toString())?.toDomain()

    override suspend fun create(name: String, date: LocalDate): Route {
        val now = System.currentTimeMillis()
        val route = Route(RouteSessionId("${date}_$now"), name.trim(), date)
        dao.insert(RouteEntity(route.id.value, route.name, date.toString(), now))
        return route
    }

    override suspend fun rename(id: RouteSessionId, name: String) {
        require(name.isNotBlank()) { "O nome da rota não pode ser vazio." }
        dao.rename(id.value, name.trim())
    }

    override suspend fun delete(id: RouteSessionId) = dao.deleteWithStops(id.value)

    private fun RouteEntity.toDomain(): Route = Route(
        id = RouteSessionId(id),
        name = name,
        date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
    )
}
