package com.otimizaai.app.settings

import android.content.Context
import com.otimizaai.domain.model.BandLimits
import com.otimizaai.domain.model.CostProfile
import com.otimizaai.domain.model.FuelType
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.VehicleProfile
import com.otimizaai.domain.model.VehicleType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** App usado para navegar até as paradas (como no Spoke). */
enum class NavigationApp { GOOGLE_MAPS, WAZE }

/** Preferências do entregador. Dinheiro sempre em centavos. */
data class AppSettings(
    val vehicleType: VehicleType = VehicleType.CAR,
    val consumptionKmPerLiter: Double = 11.0,
    val fuelPriceCentsPerLiter: Int = 629,
    /** Custos fixos por km (manutenção, seguro, IPVA...). O assistente calcula. */
    val fixedCostCentsPerKm: Int = 25,
    /** Faixas de lucro líquido por km: abaixo de low = ruim; a partir de good = bom. */
    val lowNetCentsPerKm: Int = 140,
    val goodNetCentsPerKm: Int = 200,
    /** Faixas de lucro líquido por hora. */
    val lowNetCentsPerHour: Int = 2_500,
    val goodNetCentsPerHour: Int = 3_500,
    val navigationApp: NavigationApp = NavigationApp.GOOGLE_MAPS,
    val stopMinutes: Int = 3,
    val voiceEnabled: Boolean = true,
) {
    fun vehicle(): VehicleProfile = VehicleProfile(vehicleType, consumptionKmPerLiter, fixedCostCentsPerKm)
    fun kmLimits(): BandLimits = BandLimits(minOf(lowNetCentsPerKm, goodNetCentsPerKm), goodNetCentsPerKm)
    fun hourLimits(): BandLimits = BandLimits(minOf(lowNetCentsPerHour, goodNetCentsPerHour), goodNetCentsPerHour)
}

/** Km e horas da rota, informados pelo entregador (ex.: copiados do Google Maps). */
data class RouteInput(val km: Double? = null, val hours: Double? = null)

/**
 * Guarda as preferências no próprio celular (SharedPreferences).
 * Expõe tudo como "fluxos vivos": as telas se atualizam sozinhas quando algo muda.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("otimiza_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _activeRouteId = MutableStateFlow(prefs.getString(KEY_ACTIVE_ROUTE, null))
    val activeRouteId: StateFlow<String?> = _activeRouteId.asStateFlow()

    /** Muda a cada gravação de km/horas, para as telas recalcularem. */
    private val _routeInputVersion = MutableStateFlow(0)
    val routeInputVersion: StateFlow<Int> = _routeInputVersion.asStateFlow()

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putString(KEY_VEHICLE, s.vehicleType.name)
            .putFloat(KEY_CONSUMPTION, s.consumptionKmPerLiter.toFloat())
            .putInt(KEY_FUEL, s.fuelPriceCentsPerLiter)
            .putInt(KEY_WEAR, s.fixedCostCentsPerKm)
            .putInt(KEY_LOW_KM, s.lowNetCentsPerKm)
            .putInt(KEY_MIN, s.goodNetCentsPerKm)
            .putInt(KEY_LOW_HOUR, s.lowNetCentsPerHour)
            .putInt(KEY_GOOD_HOUR, s.goodNetCentsPerHour)
            .putString(KEY_NAV, s.navigationApp.name)
            .putInt(KEY_STOP_MIN, s.stopMinutes)
            .putBoolean(KEY_VOICE, s.voiceEnabled)
            .apply()
        _settings.value = s
    }

    fun setActiveRoute(id: RouteSessionId) {
        prefs.edit().putString(KEY_ACTIVE_ROUTE, id.value).apply()
        _activeRouteId.value = id.value
    }

    fun clearActiveRoute() {
        prefs.edit().remove(KEY_ACTIVE_ROUTE).apply()
        _activeRouteId.value = null
    }

    fun routeInput(session: RouteSessionId): RouteInput {
        val km = prefs.getFloat("$KEY_KM${session.value}", -1f)
        val hours = prefs.getFloat("$KEY_HOURS${session.value}", -1f)
        return RouteInput(km.takeIf { it > 0f }?.toDouble(), hours.takeIf { it > 0f }?.toDouble())
    }

    fun saveRouteInput(session: RouteSessionId, input: RouteInput) {
        prefs.edit()
            .putFloat("$KEY_KM${session.value}", (input.km ?: -1.0).toFloat())
            .putFloat("$KEY_HOURS${session.value}", (input.hours ?: -1.0).toFloat())
            .apply()
        _routeInputVersion.value += 1
    }

    /** Últimas respostas do assistente de custos (para reabrir já preenchido). */
    fun loadCostProfile(): CostProfile? = runCatching {
        if (!prefs.contains(CP_DAYS)) return null
        CostProfile(
            daysPerWeek = prefs.getInt(CP_DAYS, 6),
            hoursPerDay = prefs.getFloat(CP_HOURS, 8f).toDouble(),
            kmPerDay = prefs.getFloat(CP_KM, 100f).toDouble(),
            weeklyGoalCents = prefs.getLong(CP_GOAL, 0),
            maintenanceMonthCents = prefs.getLong(CP_MAINT, 0),
            insuranceMonthCents = prefs.getLong(CP_INS, 0),
            financingMonthCents = prefs.getLong(CP_FIN, 0),
            otherMonthCents = prefs.getLong(CP_OTHER, 0),
            ipvaYearCents = prefs.getLong(CP_IPVA, 0),
            fuelType = runCatching { FuelType.valueOf(prefs.getString(CP_FUEL_TYPE, "GASOLINA")!!) }.getOrDefault(FuelType.GASOLINA),
            consumptionKmPerLiter = prefs.getFloat(CP_CONS, 11f).toDouble(),
            fuelPriceCentsPerLiter = prefs.getInt(CP_PRICE, 629),
        )
    }.getOrNull()

    fun saveCostProfile(p: CostProfile) {
        prefs.edit()
            .putInt(CP_DAYS, p.daysPerWeek)
            .putFloat(CP_HOURS, p.hoursPerDay.toFloat())
            .putFloat(CP_KM, p.kmPerDay.toFloat())
            .putLong(CP_GOAL, p.weeklyGoalCents)
            .putLong(CP_MAINT, p.maintenanceMonthCents)
            .putLong(CP_INS, p.insuranceMonthCents)
            .putLong(CP_FIN, p.financingMonthCents)
            .putLong(CP_OTHER, p.otherMonthCents)
            .putLong(CP_IPVA, p.ipvaYearCents)
            .putString(CP_FUEL_TYPE, p.fuelType.name)
            .putFloat(CP_CONS, p.consumptionKmPerLiter.toFloat())
            .putInt(CP_PRICE, p.fuelPriceCentsPerLiter)
            .apply()
    }

    private fun loadSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            vehicleType = runCatching { VehicleType.valueOf(prefs.getString(KEY_VEHICLE, d.vehicleType.name)!!) }.getOrDefault(d.vehicleType),
            consumptionKmPerLiter = prefs.getFloat(KEY_CONSUMPTION, d.consumptionKmPerLiter.toFloat()).toDouble(),
            fuelPriceCentsPerLiter = prefs.getInt(KEY_FUEL, d.fuelPriceCentsPerLiter),
            fixedCostCentsPerKm = prefs.getInt(KEY_WEAR, d.fixedCostCentsPerKm),
            lowNetCentsPerKm = prefs.getInt(KEY_LOW_KM, d.lowNetCentsPerKm),
            goodNetCentsPerKm = prefs.getInt(KEY_MIN, d.goodNetCentsPerKm),
            lowNetCentsPerHour = prefs.getInt(KEY_LOW_HOUR, d.lowNetCentsPerHour),
            goodNetCentsPerHour = prefs.getInt(KEY_GOOD_HOUR, d.goodNetCentsPerHour),
            navigationApp = runCatching { NavigationApp.valueOf(prefs.getString(KEY_NAV, d.navigationApp.name)!!) }.getOrDefault(d.navigationApp),
            stopMinutes = prefs.getInt(KEY_STOP_MIN, d.stopMinutes),
            voiceEnabled = prefs.getBoolean(KEY_VOICE, d.voiceEnabled),
        )
    }

    private companion object {
        const val KEY_VEHICLE = "vehicle_type"
        const val KEY_CONSUMPTION = "consumption_km_l"
        const val KEY_FUEL = "fuel_cents_l"
        const val KEY_WEAR = "wear_cents_km"
        const val KEY_MIN = "min_net_cents_km"
        const val KEY_LOW_KM = "low_net_cents_km"
        const val KEY_LOW_HOUR = "low_net_cents_h"
        const val KEY_GOOD_HOUR = "good_net_cents_h"
        const val KEY_NAV = "navigation_app"
        const val KEY_STOP_MIN = "stop_minutes"
        const val KEY_VOICE = "voice_enabled"
        const val KEY_ACTIVE_ROUTE = "active_route_id"
        const val KEY_KM = "route_km_"
        const val KEY_HOURS = "route_hours_"
        const val CP_DAYS = "cp_days"
        const val CP_HOURS = "cp_hours"
        const val CP_KM = "cp_km"
        const val CP_GOAL = "cp_goal"
        const val CP_MAINT = "cp_maint"
        const val CP_INS = "cp_ins"
        const val CP_FIN = "cp_fin"
        const val CP_OTHER = "cp_other"
        const val CP_IPVA = "cp_ipva"
        const val CP_FUEL_TYPE = "cp_fuel_type"
        const val CP_CONS = "cp_cons"
        const val CP_PRICE = "cp_price"
    }
}
