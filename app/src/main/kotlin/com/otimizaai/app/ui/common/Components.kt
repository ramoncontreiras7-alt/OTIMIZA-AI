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
import com.otimizaai.app.ui.theme.BandBad
import com.otimizaai.app.ui.theme.BandGood
import com.otimizaai.app.ui.theme.BandMid
import com.otimizaai.domain.model.ProfitBand
import com.otimizaai.domain.model.ProfitRating
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
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
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

val ProfitBand.color: Color
    get() = when (this) {
        ProfitBand.BOA -> BandGood
        ProfitBand.MEDIA -> BandMid
        ProfitBand.RUIM -> BandBad
    }

val ProfitBand.label: String
    get() = when (this) {
        ProfitBand.BOA -> "BOA"
        ProfitBand.MEDIA -> "MÉDIA"
        ProfitBand.RUIM -> "RUIM"
    }

/** Selo colorido da faixa (verde = boa, amarelo = média, vermelho = ruim). */
@Composable
fun BandBadge(band: ProfitBand, prefix: String = "") {
    Surface(color = band.color, shape = MaterialTheme.shapes.small) {
        Text(
            prefix + band.label,
            color = if (band == ProfitBand.MEDIA) Color.Black else Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Bloco com o resultado completo de uma conta de lucro, colorido pelas faixas. */
@Composable
fun EconomicsBlock(e: RouteEconomics, rating: ProfitRating) {
    BandBadge(rating.overall, prefix = "OFERTA ")
    ValueRow("Receita", BrNumber.formatCents(e.revenueCents))
    ValueRow("Combustível", "- " + BrNumber.formatCents(e.fuelCostCents))
    ValueRow("Custos fixos do veículo", "- " + BrNumber.formatCents(e.wearCostCents))
    ValueRow("Lucro líquido", BrNumber.formatCents(e.netProfitCents), bold = true, valueColor = if (e.netProfitCents >= 0) BandGood else BandBad)
    ValueRow("Lucro por km", BrNumber.formatCents(e.netCentsPerKm) + "/km", bold = true, valueColor = rating.perKm.color)
    e.netCentsPerHour?.let { perHour ->
        ValueRow("Lucro por hora", BrNumber.formatCents(perHour) + "/h", bold = true, valueColor = rating.perHour?.color ?: Color.Unspecified)
    }
    ValueRow("Ganho bruto por km", BrNumber.formatCents(e.grossCentsPerKm) + "/km")
}

/** Texto para leitura em voz alta do resultado. */
fun spokenSummary(e: RouteEconomics, rating: ProfitRating): String {
    val faixa = when (rating.overall) {
        ProfitBand.BOA -> "Oferta boa."
        ProfitBand.MEDIA -> "Oferta média."
        ProfitBand.RUIM -> "Oferta ruim."
    }
    val km = "Lucro de ${BrNumber.formatCents(e.netCentsPerKm).replace("R$ ", "")} reais por quilômetro."
    val hora = e.netCentsPerHour?.let { " ${BrNumber.formatCents(it).replace("R$ ", "")} reais por hora." } ?: ""
    return "$faixa $km$hora"
}
