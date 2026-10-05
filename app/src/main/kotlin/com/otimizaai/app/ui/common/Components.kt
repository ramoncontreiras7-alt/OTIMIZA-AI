package com.otimizaai.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.otimizaai.app.ui.theme.ProfitBad
import com.otimizaai.app.ui.theme.ProfitGood
import com.otimizaai.domain.model.RouteEconomics
import com.otimizaai.domain.util.BrNumber

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

/** Linha "rótulo .......... valor" com números alinhados. */
@Composable
fun ValueRow(label: String, value: String, bold: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
        )
    }
}

/** Campo para números (abre o teclado numérico). */
@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, suffix: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Selo verde "VALE A PENA" ou vermelho "ABAIXO DA META". */
@Composable
fun VerdictBadge(viable: Boolean) {
    Surface(color = if (viable) ProfitGood else ProfitBad, shape = MaterialTheme.shapes.small) {
        Text(
            if (viable) "VALE A PENA" else "ABAIXO DA META",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Bloco com o resultado completo de uma conta de lucro. */
@Composable
fun EconomicsBlock(e: RouteEconomics) {
    VerdictBadge(e.isViable)
    ValueRow("Receita", BrNumber.formatCents(e.revenueCents))
    ValueRow("Combustível", "- " + BrNumber.formatCents(e.fuelCostCents))
    ValueRow("Desgaste do veículo", "- " + BrNumber.formatCents(e.wearCostCents))
    ValueRow("Lucro líquido", BrNumber.formatCents(e.netProfitCents), bold = true, valueColor = if (e.netProfitCents >= 0) ProfitGood else ProfitBad)
    ValueRow("Lucro por km", BrNumber.formatCents(e.netCentsPerKm) + "/km", bold = true)
    e.netCentsPerHour?.let { ValueRow("Lucro por hora", BrNumber.formatCents(it) + "/h") }
    ValueRow("Sua meta", BrNumber.formatCents(e.minNetCentsPerKm.toLong()) + "/km")
}
