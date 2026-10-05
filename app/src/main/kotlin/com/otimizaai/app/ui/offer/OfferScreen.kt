package com.otimizaai.app.ui.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.app.ui.common.EconomicsBlock
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.ScreenTitle
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.app.ui.common.spokenSummary
import com.otimizaai.app.voice.Speaker
import com.otimizaai.domain.model.ProfitRating
import com.otimizaai.domain.model.RouteEconomics
import com.otimizaai.domain.usecase.CalculateRouteProfitUseCase
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong

/** Resultado da avaliação: ou a conta, ou uma mensagem de erro. */
data class OfferResult(val economics: RouteEconomics? = null, val rating: ProfitRating? = null, val error: String? = null)

@HiltViewModel
class OfferViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val calculate: CalculateRouteProfitUseCase,
    private val speaker: Speaker,
) : ViewModel() {

    fun evaluate(valueText: String, kmText: String, hoursText: String): OfferResult {
        val cents = BrNumber.parseCents(valueText)
        if (cents == null || cents < 0) return OfferResult(error = "Informe o valor da oferta (ex.: 168,00).")
        val km = BrNumber.parseDecimal(kmText)?.toDouble()
        if (km == null || km <= 0) return OfferResult(error = "Informe os km estimados (ex.: 45).")
        val hours = if (hoursText.isBlank()) null else BrNumber.parseDecimal(hoursText)?.toDouble()
        if (hoursText.isNotBlank() && (hours == null || hours <= 0)) return OfferResult(error = "Horas inválidas (ex.: 3).")
        val s = settingsStore.settings.value
        val economics = calculate(
            distanceMeters = (km * 1000).roundToLong(),
            revenueCents = cents,
            fuelPriceCentsPerLiter = s.fuelPriceCentsPerLiter,
            vehicle = s.vehicle(),
            minNetCentsPerKm = s.goodNetCentsPerKm,
            estimatedDurationSeconds = hours?.let { (it * 3600).roundToLong() }?.takeIf { it > 0 },
        )
        val rating = ProfitRating.of(economics, s.kmLimits(), s.hourLimits())
        // Leitura em voz alta, como no Gigu (pode desligar em Ajustes).
        if (s.voiceEnabled) speaker.speak(spokenSummary(economics, rating))
        return OfferResult(economics = economics, rating = rating)
    }
}

/**
 * Calculadora rápida: "essa oferta vale a pena?".
 * O entregador digita o que a plataforma mostra; quem decide aceitar é ele.
 */
@Composable
fun OfferScreen(vm: OfferViewModel = hiltViewModel()) {
    var value by rememberSaveable { mutableStateOf("") }
    var km by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf(OfferResult()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Avaliar oferta", "Digite o que a plataforma oferece e veja se compensa")

        SectionCard {
            NumberField(value, { value = it }, "Valor da oferta", suffix = "R$")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(km, { km = it }, "Distância", Modifier.weight(1f), suffix = "km")
                NumberField(hours, { hours = it }, "Tempo (opcional)", Modifier.weight(1f), suffix = "h")
            }
            Button(onClick = { result = vm.evaluate(value, km, hours) }, modifier = Modifier.fillMaxWidth()) {
                Text("Avaliar")
            }
            result.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        val e = result.economics
        val r = result.rating
        if (e != null && r != null) {
            SectionCard("Resultado") { EconomicsBlock(e, r) }
        }

        Text(
            "A conta usa o consumo, o preço do combustível, os custos fixos e as faixas da aba Ajustes. Verde = boa, amarelo = média, vermelho = ruim.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
