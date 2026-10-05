package com.otimizaai.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otimizaai.app.ui.common.EconomicsBlock
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.PlatformDot
import com.otimizaai.app.ui.common.ScreenTitle
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.app.ui.common.ValueRow
import com.otimizaai.app.ui.common.label
import com.otimizaai.app.ui.theme.ProfitBad
import com.otimizaai.app.ui.theme.ProfitGood
import com.otimizaai.domain.util.BrNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(onGoToStops: () -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var kmText by rememberSaveable { mutableStateOf(vm.initialKm) }
    var hoursText by rememberSaveable { mutableStateOf(vm.initialHours) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale("pt", "BR")))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Hoje", date.replaceFirstChar { it.uppercase() })

        // Lucro em destaque
        SectionCard {
            val e = s.economics
            Text("Lucro previsto da jornada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (e == null) {
                Text("—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Informe os km da rota abaixo para calcular.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    BrNumber.formatCents(e.netProfitCents),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (e.isViable) ProfitGood else ProfitBad,
                )
                Text(BrNumber.formatCents(e.netCentsPerKm) + " por km")
            }
        }

        // Contadores
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Counter("Paradas", s.total, Modifier.weight(1f))
            Counter("Entregues", s.delivered, Modifier.weight(1f))
            Counter("Pendentes", s.pending, Modifier.weight(1f))
            Counter("Falhas", s.failed, Modifier.weight(1f))
        }

        if (s.total == 0) {
            SectionCard("Comece por aqui") {
                Text("Cadastre as paradas do dia na aba Paradas. Depois abra a rota no Google Maps, veja os km e anote aqui.")
                Button(onClick = onGoToStops, modifier = Modifier.fillMaxWidth()) { Text("Adicionar paradas") }
            }
        }

        SectionCard("Rota de hoje") {
            ValueRow("Receita das paradas", BrNumber.formatCents(s.revenueCents), bold = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(kmText, { kmText = it; error = null }, "Distância", Modifier.weight(1f), suffix = "km")
                NumberField(hoursText, { hoursText = it; error = null }, "Tempo (opcional)", Modifier.weight(1f), suffix = "h")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { error = vm.saveRoute(kmText, hoursText) }, modifier = Modifier.fillMaxWidth()) {
                Text("Calcular lucro")
            }
            s.economics?.let { EconomicsBlock(it) }
        }

        if (s.byPlatform.isNotEmpty()) {
            SectionCard("Por plataforma") {
                s.byPlatform.forEach { (platform, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlatformDot(platform)
                        Spacer(Modifier.width(8.dp))
                        Text(platform.label, Modifier.weight(1f))
                        Text("$count", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        OutlinedButton(onClick = onGoToStops, modifier = Modifier.fillMaxWidth()) { Text("Ver paradas") }
    }
}

@Composable
private fun Counter(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(vertical = 10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
