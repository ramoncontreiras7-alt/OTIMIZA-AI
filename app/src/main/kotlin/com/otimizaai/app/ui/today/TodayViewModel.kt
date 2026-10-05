package com.otimizaai.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otimizaai.app.settings.AppSettings
import com.otimizaai.app.settings.RouteInput
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.app.settings.todaySession
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RouteEconomics
import com.otimizaai.domain.repository.DeliveryStopRepository
import com.otimizaai.domain.usecase.CalculateRouteProfitUseCase
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToLong

data class TodayState(
    val total: Int = 0,
    val delivered: Int = 0,
    val pending: Int = 0,
    val failed: Int = 0,
    val revenueCents: Long = 0,
    val byPlatform: List<Pair<Platform, Int>> = emptyList(),
    val routeInput: RouteInput = RouteInput(),
    val economics: RouteEconomics? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: DeliveryStopRepository,
    private val settingsStore: SettingsStore,
    private val calculate: CalculateRouteProfitUseCase,
) : ViewModel() {

    private val session = todaySession()

    val state: StateFlow<TodayState> = combine(
        repository.observeBySession(session),
        settingsStore.settings,
        settingsStore.routeInput,
    ) { stops, settings, input -> build(stops, settings, input) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    /** Texto inicial dos campos, para não aparecerem vazios ao reabrir o app. */
    val initialKm: String = settingsStore.routeInput.value.km?.let { BrNumber.formatDecimal(it, 1) } ?: ""
    val initialHours: String = settingsStore.routeInput.value.hours?.let { BrNumber.formatDecimal(it, 1) } ?: ""

    /** Retorna uma mensagem de erro, ou null se deu certo. */
    fun saveRoute(kmText: String, hoursText: String): String? {
        val km = BrNumber.parseDecimal(kmText)?.toDouble()
        if (km == null || km <= 0.0) return "Informe os km da rota (ex.: 62,5)."
        val hours = if (hoursText.isBlank()) null else BrNumber.parseDecimal(hoursText)?.toDouble()
        if (hoursText.isNotBlank() && (hours == null || hours <= 0.0)) return "Horas inválidas (ex.: 4,5)."
        settingsStore.saveRouteInput(session, RouteInput(km, hours))
        return null
    }

    private fun build(stops: List<DeliveryStop>, settings: AppSettings, input: RouteInput): TodayState {
        // Paradas que falharam normalmente não são pagas: ficam fora da receita.
        val paid = stops.filter { it.status != DeliveryStatus.FAILED }
        val economics = input.km?.takeIf { it > 0 }?.let { km ->
            runCatching {
                calculate.forStops(
                    stops = paid,
                    distanceMeters = (km * 1000).roundToLong(),
                    fuelPriceCentsPerLiter = settings.fuelPriceCentsPerLiter,
                    vehicle = settings.vehicle(),
                    minNetCentsPerKm = settings.minNetCentsPerKm,
                    estimatedDurationSeconds = input.hours?.let { (it * 3600).roundToLong() }?.takeIf { it > 0 },
                )
            }.getOrNull()
        }
        return TodayState(
            total = stops.size,
            delivered = stops.count { it.status == DeliveryStatus.DELIVERED },
            pending = stops.count { it.status == DeliveryStatus.PENDING },
            failed = stops.count { it.status == DeliveryStatus.FAILED },
            revenueCents = paid.sumOf { it.freightCents.toLong() },
            byPlatform = stops.groupingBy { it.platform }.eachCount().toList().sortedByDescending { it.second },
            routeInput = input,
            economics = economics,
        )
    }
}
