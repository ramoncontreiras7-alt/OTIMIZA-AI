package com.otimizaai.app.settings

import android.content.Context
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.model.VehicleType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Jornada de trabalho de hoje (uma por dia). */
fun todaySession(): RouteSessionId = RouteSessionId(LocalDate.now().toString())

/** Preferências do entregador. Dinheiro sempre em centavos. */
data class AppSettings(
    val vehicleType: VehicleType = VehicleType.CAR,
    val consumptionKmPerLiter: Double = 11.0,
    val fuelPriceCentsPerLiter: Int = 629,
    val wearCostCentsPerKm: Int = 25,
    val minNetCentsPerKm: Int = 200,
) {
    fun vehicle(): VehicleProfile = VehicleProfile(vehicleType, consumptionKmPerLiter, wearCostCentsPerKm)
}

/** Km e horas da rota do dia, informados pelo entregador (ex.: copiados do Google Maps). */
data class RouteInput(val km: Double? = null, val hours: Double? = null)

/**
 * Guarda as preferências e os dados da rota no próprio celular (SharedPreferences).
 * Expõe tudo como "fluxos vivos": as telas se atualizam sozinhas quando algo muda.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("otimiza_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _routeInput = MutableStateFlow(loadRouteInput(todaySession()))
    val routeInput: StateFlow<RouteInput> = _routeInput.asStateFlow()

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putString(KEY_VEHICLE, s.vehicleType.name)
            .putFloat(KEY_CONSUMPTION, s.consumptionKmPerLiter.toFloat())
            .putInt(KEY_FUEL, s.fuelPriceCentsPerLiter)
            .putInt(KEY_WEAR, s.wearCostCentsPerKm)
            .putInt(KEY_MIN, s.minNetCentsPerKm)
            .apply()
        _settings.value = s
    }

    fun saveRouteInput(session: RouteSessionId, input: RouteInput) {
        prefs.edit()
            .putFloat("$KEY_KM${session.value}", (input.km ?: -1.0).toFloat())
            .putFloat("$KEY_HOURS${session.value}", (input.hours ?: -1.0).toFloat())
            .apply()
        if (session == todaySession()) _routeInput.value = input
    }

    private fun loadSettings(): AppSettings {
        val defaults = AppSettings()
        val type = runCatching {
            VehicleType.valueOf(prefs.getString(KEY_VEHICLE, defaults.vehicleType.name)!!)
        }.getOrDefault(defaults.vehicleType)
        return AppSettings(
            vehicleType = type,
            consumptionKmPerLiter = prefs.getFloat(KEY_CONSUMPTION, defaults.consumptionKmPerLiter.toFloat()).toDouble(),
            fuelPriceCentsPerLiter = prefs.getInt(KEY_FUEL, defaults.fuelPriceCentsPerLiter),
            wearCostCentsPerKm = prefs.getInt(KEY_WEAR, defaults.wearCostCentsPerKm),
            minNetCentsPerKm = prefs.getInt(KEY_MIN, defaults.minNetCentsPerKm),
        )
    }

    private fun loadRouteInput(session: RouteSessionId): RouteInput {
        val km = prefs.getFloat("$KEY_KM${session.value}", -1f)
        val hours = prefs.getFloat("$KEY_HOURS${session.value}", -1f)
        return RouteInput(
            km = km.takeIf { it > 0f }?.toDouble(),
            hours = hours.takeIf { it > 0f }?.toDouble(),
        )
    }

    private companion object {
        const val KEY_VEHICLE = "vehicle_type"
        const val KEY_CONSUMPTION = "consumption_km_l"
        const val KEY_FUEL = "fuel_cents_l"
        const val KEY_WEAR = "wear_cents_km"
        const val KEY_MIN = "min_net_cents_km"
        const val KEY_KM = "route_km_"
        const val KEY_HOURS = "route_hours_"
    }
}
