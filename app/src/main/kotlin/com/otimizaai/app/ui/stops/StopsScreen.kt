package com.otimizaai.app.ui.stops

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otimizaai.app.ui.common.MapsLauncher
import com.otimizaai.app.ui.common.PlatformDot
import com.otimizaai.app.ui.common.ScreenTitle
import com.otimizaai.app.ui.common.label
import com.otimizaai.app.ui.theme.ProfitBad
import com.otimizaai.app.ui.theme.ProfitGood
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.util.BrNumber

@Composable
fun StopsScreen(vm: StopsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val stops by vm.stops.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val conflict by vm.conflict.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenTitle("Paradas de hoje", "${stops.size} parada(s) · ordem de cadastro")
            }
            if (stops.isNotEmpty()) {
                item {
                    val pending = stops.filter { it.status == DeliveryStatus.PENDING }
                    OutlinedButton(
                        onClick = {
                            val used = MapsLauncher.openRoute(context, pending.map { it.address.formatted })
                            if (pending.size > used) {
                                Toast.makeText(context, "O Maps aceita $used paradas por vez. Abri as primeiras $used.", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = pending.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Abrir rota das pendentes no Google Maps") }
                }
            } else {
                item {
                    Text(
                        "Nenhuma parada ainda. Toque em \"Nova parada\" e cadastre na ordem em que pretende entregar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(stops, key = { _, s -> s.platformId.value + "|" + s.id.value }) { index, stop ->
                StopCard(
                    number = index + 1,
                    stop = stop,
                    onNavigate = { MapsLauncher.navigateTo(context, stop.address.formatted) },
                    onStatus = { vm.setStatus(stop, it) },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nova parada") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (showAdd) {
        AddStopDialog(
            onDismiss = { showAdd = false },
            onConfirm = { platform, id, address, freight ->
                val error = vm.addStop(platform, id, address, freight)
                if (error == null) showAdd = false
                error
            },
        )
    }

    conflict?.let { existing ->
        AlertDialog(
            onDismissRequest = vm::dismissConflict,
            title = { Text("Pedido já cadastrado") },
            text = {
                Text(
                    "O pedido ${existing.id.value} (${existing.platform.label}) está na jornada de " +
                        "${existing.sessionId.value} com status \"${existing.status.label}\".\n\n" +
                        "Quer trazer para a jornada de hoje? Ele volta como Pendente.",
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmTransfer) { Text("Trazer para hoje") } },
            dismissButton = { TextButton(onClick = vm::dismissConflict) { Text("Deixar como está") } },
        )
    }
}

@Composable
private fun StopCard(
    number: Int,
    stop: DeliveryStop,
    onNavigate: () -> Unit,
    onStatus: (DeliveryStatus) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$number", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                PlatformDot(stop.platform)
                Spacer(Modifier.width(6.dp))
                Text(stop.platform.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val statusColor = when (stop.status) {
                    DeliveryStatus.DELIVERED -> ProfitGood
                    DeliveryStatus.FAILED -> ProfitBad
                    DeliveryStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(stop.status.label, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
            // O ID aparece exatamente como a plataforma forneceu.
            Text(stop.id.value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text(stop.address.formatted)
            Text("Frete: " + BrNumber.formatCents(stop.freightCents.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onNavigate) { Text("Navegar") }
                if (stop.status != DeliveryStatus.DELIVERED) {
                    TextButton(onClick = { onStatus(DeliveryStatus.DELIVERED) }) { Text("Entregue", color = ProfitGood) }
                }
                if (stop.status != DeliveryStatus.FAILED) {
                    TextButton(onClick = { onStatus(DeliveryStatus.FAILED) }) { Text("Falhou", color = ProfitBad) }
                }
                if (stop.status != DeliveryStatus.PENDING) {
                    TextButton(onClick = { onStatus(DeliveryStatus.PENDING) }) { Text("Desfazer") }
                }
            }
        }
    }
}

@Composable
private fun AddStopDialog(
    onDismiss: () -> Unit,
    onConfirm: (Platform?, String, String, String) -> String?,
) {
    var platform by rememberSaveable { mutableStateOf<Platform?>(null) }
    var id by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var freight by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova parada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Plataforma", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Platform.entries.forEach { p ->
                        FilterChip(
                            selected = platform == p,
                            onClick = { platform = p; error = null },
                            label = { Text(p.label) },
                            leadingIcon = { PlatformDot(p, 10.dp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it; error = null },
                    label = { Text("ID do pedido") },
                    supportingText = { Text("Igual ao da plataforma, sem alterar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; error = null },
                    label = { Text("Endereço completo") },
                    placeholder = { Text("Rua, número, bairro, cidade") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = freight,
                    onValueChange = { freight = it; error = null },
                    label = { Text("Valor que você recebe") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onConfirm(platform, id, address, freight) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
