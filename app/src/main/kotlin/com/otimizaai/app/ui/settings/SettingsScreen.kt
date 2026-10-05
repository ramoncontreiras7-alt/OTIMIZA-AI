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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.otimizaai.app.settings.AppSettings
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.ScreenTitle
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.domain.model.VehicleType
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
) : ViewModel() {

    val current: AppSettings get() = store.settings.value

    /** Retorna mensagem de erro, ou null se salvou. */
    fun save(type: VehicleType, consumption: String, fuel: String, wear: String, min: String): String? {
        val c = BrNumber.parseDecimal(consumption)?.toDouble()
        if (c == null || c <= 0) return "Consumo inválido (ex.: 11 para carro, 40 para moto)."
        val f = BrNumber.parseCents(fuel)
        if (f == null || f <= 0 || f > Int.MAX_VALUE) return "Preço do combustível inválido (ex.: 6,29)."
        val w = BrNumber.parseCents(wear)
        if (w == null || w < 0 || w > Int.MAX_VALUE) return "Desgaste inválido (ex.: 0,25)."
        val m = BrNumber.parseCents(min)
        if (m == null || m < 0 || m > Int.MAX_VALUE) return "Meta inválida (ex.: 2,00)."
        store.saveSettings(AppSettings(type, c, f.toInt(), w.toInt(), m.toInt()))
        return null
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s = vm.current
    var type by rememberSaveable { mutableStateOf(s.vehicleType) }
    var consumption by rememberSaveable { mutableStateOf(BrNumber.formatDecimal(s.consumptionKmPerLiter, 1)) }
    var fuel by rememberSaveable { mutableStateOf(BrNumber.formatCents(s.fuelPriceCentsPerLiter.toLong()).removePrefix("R$ ")) }
    var wear by rememberSaveable { mutableStateOf(BrNumber.formatCents(s.wearCostCentsPerKm.toLong()).removePrefix("R$ ")) }
    var min by rememberSaveable { mutableStateOf(BrNumber.formatCents(s.minNetCentsPerKm.toLong()).removePrefix("R$ ")) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Ajustes", "Usados em todas as contas de lucro")

        SectionCard("Veículo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == VehicleType.CAR,
                    onClick = { type = VehicleType.CAR; if (consumption.isBlank()) consumption = "11,0" },
                    label = { Text("Carro") },
                )
                FilterChip(
                    selected = type == VehicleType.MOTORCYCLE,
                    onClick = { type = VehicleType.MOTORCYCLE; if (consumption.isBlank()) consumption = "40,0" },
                    label = { Text("Moto") },
                )
            }
            NumberField(consumption, { consumption = it; error = null }, "Consumo médio", suffix = "km/L")
            NumberField(wear, { wear = it; error = null }, "Desgaste (pneu, óleo, manutenção)", suffix = "R$/km")
        }

        SectionCard("Combustível e meta") {
            NumberField(fuel, { fuel = it; error = null }, "Preço do litro", suffix = "R$")
            NumberField(min, { min = it; error = null }, "Lucro mínimo aceitável", suffix = "R$/km")
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                error = vm.save(type, consumption, fuel, wear, min)
                if (error == null) Toast.makeText(context, "Ajustes salvos.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar ajustes") }

        Text(
            "Dica: para moto, um consumo comum fica entre 35 e 45 km/L e o desgaste perto de R$ 0,12/km.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
