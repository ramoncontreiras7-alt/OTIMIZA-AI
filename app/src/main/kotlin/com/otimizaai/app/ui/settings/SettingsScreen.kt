package com.otimizaai.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otimizaai.app.settings.AppSettings
import com.otimizaai.app.settings.NavigationApp
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.ScreenTitle
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.app.ui.theme.BandBad
import com.otimizaai.app.ui.theme.BandGood
import com.otimizaai.domain.model.VehicleType
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings

    /** Retorna mensagem de erro, ou null se salvou. */
    fun save(form: SettingsForm): String? {
        val c = BrNumber.parseDecimal(form.consumption)?.toDouble()
        if (c == null || c <= 0) return "Consumo inválido (ex.: 11 para carro, 40 para moto)."
        val fuel = cents(form.fuel) ?: return "Preço do combustível inválido (ex.: 6,29)."
        if (fuel <= 0) return "Preço do combustível inválido (ex.: 6,29)."
        val fixed = cents(form.fixed) ?: return "Custo fixo por km inválido (ex.: 0,25)."
        val lowKm = cents(form.lowKm) ?: return "Faixa ruim por km inválida."
        val goodKm = cents(form.goodKm) ?: return "Faixa boa por km inválida."
        val lowH = cents(form.lowHour) ?: return "Faixa ruim por hora inválida."
        val goodH = cents(form.goodHour) ?: return "Faixa boa por hora inválida."
        if (lowKm > goodKm || lowH > goodH) return "O valor de 'ruim' precisa ser menor que o de 'bom'."
        val stopMin = form.stopMinutes.trim().toIntOrNull()
        if (stopMin == null || stopMin < 0 || stopMin > 120) return "Tempo por parada inválido (ex.: 3)."
        store.saveSettings(
            AppSettings(
                vehicleType = form.vehicle,
                consumptionKmPerLiter = c,
                fuelPriceCentsPerLiter = fuel,
                fixedCostCentsPerKm = fixed,
                lowNetCentsPerKm = lowKm,
                goodNetCentsPerKm = goodKm,
                lowNetCentsPerHour = lowH,
                goodNetCentsPerHour = goodH,
                navigationApp = form.navigation,
                stopMinutes = stopMin,
                voiceEnabled = form.voice,
            ),
        )
        return null
    }

    private fun cents(t: String): Int? {
        val v = BrNumber.parseCents(t) ?: return null
        return if (v < 0 || v > Int.MAX_VALUE) null else v.toInt()
    }
}

/** Valores do formulário, como texto digitado. */
data class SettingsForm(
    val vehicle: VehicleType,
    val consumption: String,
    val fuel: String,
    val fixed: String,
    val lowKm: String,
    val goodKm: String,
    val lowHour: String,
    val goodHour: String,
    val navigation: NavigationApp,
    val stopMinutes: String,
    val voice: Boolean,
)

private fun money(cents: Int): String = BrNumber.formatCents(cents.toLong()).removePrefix("R$ ")

@Composable
fun SettingsScreen(onOpenWizard: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    var vehicle by rememberSaveable { mutableStateOf(s.vehicleType) }
    var consumption by rememberSaveable { mutableStateOf("") }
    var fuel by rememberSaveable { mutableStateOf("") }
    var fixed by rememberSaveable { mutableStateOf("") }
    var lowKm by rememberSaveable { mutableStateOf("") }
    var goodKm by rememberSaveable { mutableStateOf("") }
    var lowHour by rememberSaveable { mutableStateOf("") }
    var goodHour by rememberSaveable { mutableStateOf("") }
    var navigation by rememberSaveable { mutableStateOf(s.navigationApp) }
    var stopMinutes by rememberSaveable { mutableStateOf("") }
    var voice by rememberSaveable { mutableStateOf(s.voiceEnabled) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // Preenche o formulário com o que está salvo (e de novo quando o assistente atualizar).
    LaunchedEffect(s) {
        vehicle = s.vehicleType
        consumption = BrNumber.formatDecimal(s.consumptionKmPerLiter, 1)
        fuel = money(s.fuelPriceCentsPerLiter)
        fixed = money(s.fixedCostCentsPerKm)
        lowKm = money(s.lowNetCentsPerKm)
        goodKm = money(s.goodNetCentsPerKm)
        lowHour = money(s.lowNetCentsPerHour)
        goodHour = money(s.goodNetCentsPerHour)
        navigation = s.navigationApp
        stopMinutes = s.stopMinutes.toString()
        voice = s.voiceEnabled
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Ajustes", "Usados em todas as contas de lucro")

        SectionCard("Assistente de custos") {
            Text("Responda algumas perguntas (dias, km, meta, manutenção, seguro, IPVA...) e o app calcula seu custo por km e as faixas de ruim/bom.")
            Button(onClick = onOpenWizard, modifier = Modifier.fillMaxWidth()) { Text("Calcular meu custo por km") }
        }

        SectionCard("Veículo e combustível") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vehicle == VehicleType.CAR, onClick = { vehicle = VehicleType.CAR }, label = { Text("Carro") })
                FilterChip(selected = vehicle == VehicleType.MOTORCYCLE, onClick = { vehicle = VehicleType.MOTORCYCLE }, label = { Text("Moto") })
            }
            NumberField(consumption, { consumption = it; error = null }, "Autonomia", suffix = "km/L")
            NumberField(fuel, { fuel = it; error = null }, "Preço do litro", suffix = "R$")
            NumberField(fixed, { fixed = it; error = null }, "Custos fixos por km (manutenção, seguro, IPVA...)", suffix = "R$/km")
        }

        SectionCard("Faixas de lucro líquido por km") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lowKm, { lowKm = it; error = null }, "Ruim abaixo de", Modifier.weight(1f), suffix = "R$")
                NumberField(goodKm, { goodKm = it; error = null }, "Boa a partir de", Modifier.weight(1f), suffix = "R$")
            }
            Text("Entre os dois valores, a oferta é MÉDIA.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard("Faixas de lucro líquido por hora") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lowHour, { lowHour = it; error = null }, "Ruim abaixo de", Modifier.weight(1f), suffix = "R$")
                NumberField(goodHour, { goodHour = it; error = null }, "Boa a partir de", Modifier.weight(1f), suffix = "R$")
            }
        }

        SectionCard("Rota e navegação") {
            Text("App de navegação", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = navigation == NavigationApp.GOOGLE_MAPS, onClick = { navigation = NavigationApp.GOOGLE_MAPS }, label = { Text("Google Maps") })
                FilterChip(selected = navigation == NavigationApp.WAZE, onClick = { navigation = NavigationApp.WAZE }, label = { Text("Waze") })
            }
            NumberField(stopMinutes, { stopMinutes = it; error = null }, "Tempo médio em cada parada", suffix = "min")
        }

        SectionCard("Voz") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ler o resultado da oferta em voz alta", Modifier.weight(1f))
                Switch(checked = voice, onCheckedChange = { voice = it })
            }
        }

        error?.let { Text(it, color = BandBad) }
        Button(
            onClick = {
                error = vm.save(SettingsForm(vehicle, consumption, fuel, fixed, lowKm, goodKm, lowHour, goodHour, navigation, stopMinutes, voice))
                if (error == null) Toast.makeText(context, "Ajustes salvos.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar ajustes") }
        OutlinedButton(onClick = onOpenWizard, modifier = Modifier.fillMaxWidth()) {
            Text("Refazer o assistente de custos", color = BandGood)
        }
    }
}
