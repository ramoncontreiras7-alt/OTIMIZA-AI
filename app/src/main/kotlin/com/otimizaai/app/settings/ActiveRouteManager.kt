package com.otimizaai.app.settings

import com.otimizaai.domain.model.Route
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.repository.DeliveryStopRepository
import com.otimizaai.domain.repository.RouteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sabe qual é a rota "aberta" no momento (a que as abas Rota e Paradas mostram).
 * Na primeira abertura do dia, cria automaticamente uma rota com o nome do dia da semana,
 * como o Spoke faz.
 */
@Singleton
class ActiveRouteManager @Inject constructor(
    private val routes: RouteRepository,
    private val stops: DeliveryStopRepository,
    private val settings: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _active = MutableStateFlow<Route?>(null)
    val active: StateFlow<Route?> = _active.asStateFlow()

    init {
        scope.launch { ensureActive() }
    }

    /** Garante que existe uma rota aberta (a salva, ou a de hoje, ou uma nova). */
    suspend fun ensureActive(): Route = mutex.withLock {
        _active.value?.let { return it }
        val saved = settings.activeRouteId.value?.let { routes.findById(RouteSessionId(it)) }
        val route = saved
            ?: routes.findLatestOn(LocalDate.now())
            ?: routes.create(weekdayName(LocalDate.now()), LocalDate.now())
        settings.setActiveRoute(route.id)
        _active.value = route
        route
    }

    fun select(route: Route) {
        settings.setActiveRoute(route.id)
        _active.value = route
    }

    /** Cria uma rota nova e já abre. Se [bringPending], traz as pendentes de rotas anteriores. */
    suspend fun createAndSelect(name: String, date: LocalDate, bringPending: Boolean): Pair<Route, Int> {
        val finalName = name.trim().ifEmpty { weekdayName(date) }
        val route = routes.create(finalName, date)
        val moved = if (bringPending) stops.transferPendingFromOtherSessions(route.id) else 0
        select(route)
        return route to moved
    }

    suspend fun rename(name: String) {
        val current = _active.value ?: return
        routes.rename(current.id, name)
        _active.value = current.copy(name = name.trim())
    }

    /** Apaga a rota aberta (e as paradas dela) e abre outra. */
    suspend fun deleteActive() {
        val current = _active.value ?: return
        routes.delete(current.id)
        mutex.withLock { _active.value = null }
        settings.clearActiveRoute()
        ensureActive()
    }

    companion object {
        fun weekdayName(date: LocalDate): String =
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    }
}
