package com.otimizaai.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otimizaai.app.settings.ActiveRouteManager
import com.otimizaai.app.settings.AppSettings
import com.otimizaai.app.settings.RouteInput
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.ProfitRating
import com.otimizaai.domain.model.Route
import com.otimizaai.domain.model.RouteEconomics
import com.otimizaai.domain.model.RouteSummary
import com.otimizaai.domain.repository.DeliveryStopRepository
import com.otimizaai.domain.repository.RouteRepository
import com.otimizaai.domain.usecase.CalculateRouteProfitUseCase
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToLong

data class TodayState(
    val route: Route? = null,
    val total: Int = 0,
    val delivered: Int = 0,
    val pending: Int = 0,
    val failed: Int = 0,
    val revenueCents: Long = 0,
    val byPlatform: List<Pair<Platform, Int>> = emptyList(),
    val routeInput: RouteInput = RouteInput(),
    val stopMinutesTotal: Int = 0,
    val economics: RouteEconomics? = null,
    val rating: ProfitRating? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val stopRepository: DeliveryStopRepository,
    routeRepository: RouteRepository,
    private val settingsStore: SettingsStore,
    private val activeRoutes: ActiveRouteManager,
    private val calculate: CalculateRouteProfitUseCase,
) : ViewModel() {

    private val activeRoute = activeRoutes.active.filterNotNull()

    val state: StateFlow<TodayState> = activeRoute
        .flatMapLatest { route ->
            combine(
                stopRepository.observeBySession(route.id),
                settingsStore.settings,
                settingsStore.routeInputVersion.map { settingsStore.routeInput(route.id) },
            ) { stops, settings, input -> build(route, stops, settings, input) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    /** Lista de rotas para o seletor (como o menu lateral do Spoke). */
    val routes: StateFlow<List<RouteSummary>> = routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    /** Retorna uma mensagem de erro, ou null se deu certo. */
    fun saveRoute(kmText: String, hoursText: String): String? {
        val route = activeRoutes.active.value ?: return "Aguarde, abrindo a rota..."
        val km = BrNumber.parseDecimal(kmText)?.toDouble()
        if (km == null || km <= 0.0) return "Informe os km da rota (ex.: 62,5)."
        val hours = if (hoursText.isBlank()) null else BrNumber.parseDecimal(hoursText)?.toDouble()
        if (hoursText.isNotBlank() && (hours == null || hours <= 0.0)) return "Horas inválidas (ex.: 4,5)."
        settingsStore.saveRouteInput(route.id, RouteInput(km, hours))
        return null
    }

    fun currentInputText(route: Route): Pair<String, String> {
        val input = settingsStore.routeInput(route.id)
        return (input.km?.let { BrNumber.formatDecimal(it, 1) } ?: "") to (input.hours?.let { BrNumber.formatDecimal(it, 1) } ?: "")
    }

    fun selectRoute(route: Route) = activeRoutes.select(route)

    fun createRoute(name: String, date: LocalDate, bringPending: Boolean) {
        viewModelScope.launch {
            val (route, moved) = activeRoutes.createAndSelect(name, date, bringPending)
            _message.value = if (moved > 0) "Rota \"${route.name}\" criada com $moved parada(s) pendente(s)." else "Rota \"${route.name}\" criada."
        }
    }

    fun renameRoute(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { activeRoutes.rename(name) }
    }

    fun deleteRoute() {
        viewModelScope.launch {
            activeRoutes.deleteActive()
            _message.value = "Rota apagada."
        }
    }

    private fun build(route: Route, stops: List<DeliveryStop>, settings: AppSettings, input: RouteInput): TodayState {
        // Paradas que falharam normalmente não são pagas: ficam fora da receita.
        val paid = stops.filter { it.status != DeliveryStatus.FAILED }
        val stopMinutes = stops.count { it.status == DeliveryStatus.PENDING } * settings.stopMinutes
        val economics = input.km?.takeIf { it > 0 }?.let { km ->
            runCatching {
                calculate.forStops(
                    stops = paid,
                    distanceMeters = (km * 1000).roundToLong(),
                    fuelPriceCentsPerLiter = settings.fuelPriceCentsPerLiter,
                    vehicle = settings.vehicle(),
                    minNetCentsPerKm = settings.goodNetCentsPerKm,
                    estimatedDurationSeconds = input.hours?.let { (it * 3600).roundToLong() }?.takeIf { it > 0 },
                )
            }.getOrNull()
        }
        return TodayState(
            route = route,
            total = stops.size,
            delivered = stops.count { it.status == DeliveryStatus.DELIVERED },
            pending = stops.count { it.status == DeliveryStatus.PENDING },
            failed = stops.count { it.status == DeliveryStatus.FAILED },
            revenueCents = paid.sumOf { it.freightCents.toLong() },
            byPlatform = stops.groupingBy { it.platform }.eachCount().toList().sortedByDescending { it.second },
            routeInput = input,
            stopMinutesTotal = stopMinutes,
            economics = economics,
            rating = economics?.let { ProfitRating.of(it, settings.kmLimits(), settings.hourLimits()) },
        )
    }
}
