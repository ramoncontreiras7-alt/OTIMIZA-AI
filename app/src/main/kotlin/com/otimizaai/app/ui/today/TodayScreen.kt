package com.otimizaai.app.ui.today

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otimizaai.app.settings.ActiveRouteManager
import com.otimizaai.app.ui.common.BandBadge
import com.otimizaai.app.ui.common.EconomicsBlock
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.PlatformDot
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.app.ui.common.ValueRow
import com.otimizaai.app.ui.common.color
import com.otimizaai.app.ui.common.label
import com.otimizaai.domain.model.Route
import com.otimizaai.domain.model.RouteSummary
import com.otimizaai.domain.util.BrNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("EEE, dd/MM", Locale("pt", "BR"))

@Composable
fun TodayScreen(onGoToStops: () -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    val routes by vm.routes.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var kmText by rememberSaveable { mutableStateOf("") }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showRoutes by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    // Ao trocar de rota, carrega os km/horas já anotados nela.
    LaunchedEffect(s.route?.id) {
        s.route?.let { val (k, h) = vm.currentInputText(it); kmText = k; hoursText = h; error = null }
    }
    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Cabeçalho: rota aberta + troca de rota
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.route?.name?.replaceFirstChar { it.uppercase() } ?: "Abrindo rota...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                s.route?.let { Text(it.date.format(dateFmt), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            OutlinedButton(onClick = { showRoutes = true }) { Text("Rotas") }
        }

        SectionCard {
            val e = s.economics
            Text("Lucro previsto desta rota", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (e == null || s.rating == null) {
                Text("—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Informe os km da rota abaixo para calcular.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    BrNumber.formatCents(e.netProfitCents),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = s.rating!!.overall.color,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(BrNumber.formatCents(e.netCentsPerKm) + " por km", Modifier.weight(1f))
                    BandBadge(s.rating!!.overall)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Counter("Paradas", s.total, Modifier.weight(1f))
            Counter("Entregues", s.delivered, Modifier.weight(1f))
            Counter("Pendentes", s.pending, Modifier.weight(1f))
            Counter("Falhas", s.failed, Modifier.weight(1f))
        }

        if (s.total == 0) {
            SectionCard("Comece por aqui") {
                Text("Cadastre as paradas desta rota na aba Paradas. Depois abra a rota no mapa, veja os km e anote aqui.")
                Button(onClick = onGoToStops, modifier = Modifier.fillMaxWidth()) { Text("Adicionar paradas") }
            }
        }

        SectionCard("Ganhos da rota") {
            ValueRow("Receita das paradas", BrNumber.formatCents(s.revenueCents), bold = true)
            if (s.stopMinutesTotal > 0) {
                ValueRow("Tempo estimado nas paradas", "${s.stopMinutesTotal} min")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(kmText, { kmText = it; error = null }, "Distância", Modifier.weight(1f), suffix = "km")
                NumberField(hoursText, { hoursText = it; error = null }, "Tempo (opcional)", Modifier.weight(1f), suffix = "h")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { error = vm.saveRoute(kmText, hoursText) }, modifier = Modifier.fillMaxWidth()) {
                Text("Calcular lucro")
            }
            val e = s.economics
            val r = s.rating
            if (e != null && r != null) EconomicsBlock(e, r)
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showRename = true }, modifier = Modifier.weight(1f)) { Text("Renomear rota") }
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.weight(1f)) { Text("Apagar rota") }
        }
    }

    if (showRoutes) {
        RoutesDialog(
            routes = routes,
            activeId = s.route?.id?.value,
            onSelect = { vm.selectRoute(it); showRoutes = false },
            onCreate = { showRoutes = false; showCreate = true },
            onDismiss = { showRoutes = false },
        )
    }
    if (showCreate) {
        CreateRouteDialog(
            onConfirm = { name, date, bring -> vm.createRoute(name, date, bring); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }
    if (showRename) {
        var name by remember { mutableStateOf(s.route?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Renomear rota") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.renameRoute(name); showRename = false }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancelar") } },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Apagar esta rota?") },
            text = { Text("A rota \"${s.route?.name}\" e as ${s.total} parada(s) dela serão apagadas do celular. Isso não pode ser desfeito.") },
            confirmButton = { TextButton(onClick = { vm.deleteRoute(); showDelete = false }) { Text("Apagar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun RoutesDialog(
    routes: List<RouteSummary>,
    activeId: String?,
    onSelect: (Route) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minhas rotas") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(routes, key = { it.route.id.value }) { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(r.route) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.route.name.replaceFirstChar { it.uppercase() },
                                fontWeight = if (r.route.id.value == activeId) FontWeight.Bold else FontWeight.Normal,
                                color = if (r.route.id.value == activeId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(r.route.date.format(dateFmt), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${r.pendingStops}/${r.totalStops}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { Button(onClick = onCreate) { Text("+ Criar rota") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun CreateRouteDialog(
    onConfirm: (String, LocalDate, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var bring by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar rota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da rota (opcional)") },
                    placeholder = { Text(ActiveRouteManager.weekdayName(date)) },
                    singleLine = true,
                )
                Text("Data", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = date == today, onClick = { date = today }, label = { Text("Hoje") })
                    FilterChip(selected = date == today.plusDays(1), onClick = { date = today.plusDays(1) }, label = { Text("Amanhã") })
                    FilterChip(
                        selected = date != today && date != today.plusDays(1),
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                        },
                        label = { Text(if (date != today && date != today.plusDays(1)) date.format(dateFmt) else "Escolher") },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { bring = !bring }) {
                    Checkbox(checked = bring, onCheckedChange = { bring = it })
                    Text("Trazer as paradas pendentes das rotas anteriores")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, date, bring) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
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
