package com.otimizaai.app.ui.wizard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.otimizaai.app.settings.SettingsStore
import com.otimizaai.app.ui.common.NumberField
import com.otimizaai.app.ui.common.SectionCard
import com.otimizaai.app.ui.common.ValueRow
import com.otimizaai.domain.model.CostPlan
import com.otimizaai.domain.model.CostProfile
import com.otimizaai.domain.model.FuelType
import com.otimizaai.domain.model.VehicleType
import com.otimizaai.domain.usecase.CalculateCostPlanUseCase
import com.otimizaai.domain.util.BrNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Um passo do assistente: uma pergunta por tela (como no Gigu). */
private enum class Step(val question: String, val hint: String? = null, val suffix: String? = null) {
    VEHICLE("Que tipo de veículo você usa?"),
    DAYS("Quantos dias você trabalha por semana?"),
    HOURS("Quantas horas você dirige por dia?", "Conte só o tempo rodando. Se trabalha 10 h e para 1 h, informe 9.", "h"),
    KM("Quantos km você roda por dia?", "Uma média dos seus dias de trabalho.", "km"),
    GOAL("Quanto você quer LUCRAR por semana?", "O que sobra no bolso depois de combustível e custos do veículo.", "R$"),
    FINANCING("Paga parcela ou aluguel do veículo?", "Valor por mês. Deixe 0 se o veículo é quitado.", "R$/mês"),
    MAINTENANCE("Quanto gasta por mês com manutenção?", "Óleo, pneus, revisões, peças. Uma média.", "R$/mês"),
    INSURANCE("Quanto custa o seguro por mês?", "Deixe 0 se não tem seguro.", "R$/mês"),
    OTHER("Outros custos mensais do trabalho?", "Lava-jato, alimentação, pedágio, internet do celular...", "R$/mês"),
    IPVA("Quanto paga por ano de IPVA e licenciamento?", null, "R$/ano"),
    FUEL_TYPE("Que combustível o veículo usa?"),
    CONSUMPTION("Quantos km o veículo faz com 1 litro?", "Carro costuma fazer de 9 a 14; moto de 30 a 45.", "km/L"),
    PRICE("Quanto você paga, em média, no litro?", null, "R$/L"),
}

@HiltViewModel
class CostWizardViewModel @Inject constructor(
    private val store: SettingsStore,
    private val calculate: CalculateCostPlanUseCase,
) : ViewModel() {

    /** Respostas anteriores, para o assistente abrir já preenchido. */
    val previous: CostProfile? = store.loadCostProfile()
    val previousVehicle: VehicleType = store.settings.value.vehicleType

    fun plan(profile: CostProfile): CostPlan = calculate(profile)

    /** Aplica o resultado nos Ajustes (custo fixo por km e faixas). */
    fun apply(vehicle: VehicleType, profile: CostProfile, plan: CostPlan) {
        store.saveCostProfile(profile)
        val s = store.settings.value
        store.saveSettings(
            s.copy(
                vehicleType = vehicle,
                consumptionKmPerLiter = profile.consumptionKmPerLiter,
                fuelPriceCentsPerLiter = profile.fuelPriceCentsPerLiter,
                fixedCostCentsPerKm = plan.fixedCostCentsPerKm,
                lowNetCentsPerKm = plan.lowNetCentsPerKm,
                goodNetCentsPerKm = plan.goalNetCentsPerKm,
                lowNetCentsPerHour = plan.lowNetCentsPerHour,
                goodNetCentsPerHour = plan.goalNetCentsPerHour,
            ),
        )
    }
}

@Composable
fun CostWizardScreen(onClose: () -> Unit, vm: CostWizardViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val steps = Step.entries
    var index by rememberSaveable { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var vehicle by rememberSaveable { mutableStateOf(vm.previousVehicle) }
    var fuelType by rememberSaveable { mutableStateOf(vm.previous?.fuelType ?: FuelType.GASOLINA) }
    var days by rememberSaveable { mutableStateOf(vm.previous?.daysPerWeek ?: 0) }
    val text = remember {
        val p = vm.previous
        mutableStateMapOf<Step, String>().apply {
            if (p != null) {
                put(Step.HOURS, BrNumber.formatDecimal(p.hoursPerDay, 1))
                put(Step.KM, BrNumber.formatDecimal(p.kmPerDay, 0))
                put(Step.GOAL, cents(p.weeklyGoalCents))
                put(Step.FINANCING, cents(p.financingMonthCents))
                put(Step.MAINTENANCE, cents(p.maintenanceMonthCents))
                put(Step.INSURANCE, cents(p.insuranceMonthCents))
                put(Step.OTHER, cents(p.otherMonthCents))
                put(Step.IPVA, cents(p.ipvaYearCents))
                put(Step.CONSUMPTION, BrNumber.formatDecimal(p.consumptionKmPerLiter, 1))
                put(Step.PRICE, cents(p.fuelPriceCentsPerLiter.toLong()))
            }
        }
    }
    var result by remember { mutableStateOf<Pair<CostProfile, CostPlan>?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Calcular custo por km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { if (result != null) 1f else (index + 1f) / (steps.size + 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        val done = result
        if (done != null) {
            ResultCard(done.second)
            Button(
                onClick = {
                    vm.apply(vehicle, done.first, done.second)
                    Toast.makeText(context, "Custos e faixas atualizados.", Toast.LENGTH_SHORT).show()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Usar esses valores no app") }
            OutlinedButton(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) { Text("Voltar e corrigir") }
            return@Column
        }

        val step = steps[index]
        Text(step.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        when (step) {
            Step.VEHICLE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vehicle == VehicleType.CAR, onClick = { vehicle = VehicleType.CAR }, label = { Text("Carro") })
                FilterChip(selected = vehicle == VehicleType.MOTORCYCLE, onClick = { vehicle = VehicleType.MOTORCYCLE }, label = { Text("Moto") })
            }
            Step.DAYS -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..7).forEach { d -> FilterChip(selected = days == d, onClick = { days = d; error = null }, label = { Text("$d") }) }
            }
            Step.FUEL_TYPE -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(FuelType.GASOLINA to "Gasolina", FuelType.ETANOL to "Etanol", FuelType.GNV to "GNV").forEach { (f, l) ->
                    FilterChip(selected = fuelType == f, onClick = { fuelType = f }, label = { Text(l) })
                }
            }
            else -> NumberField(
                value = text[step] ?: "",
                onValueChange = { text[step] = it; error = null },
                label = step.suffix ?: "",
                suffix = step.suffix,
                placeholder = "0",
            )
        }
        step.hint?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.padding(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (index == 0) onClose() else { index--; error = null } },
                modifier = Modifier.weight(1f),
            ) { Text(if (index == 0) "Cancelar" else "Voltar") }
            Button(
                onClick = {
                    error = validate(step, days, text)
                    if (error == null) {
                        if (index < steps.lastIndex) {
                            index++
                        } else {
                            val profile = buildProfile(days, fuelType, text)
                            if (profile == null) error = "Confira os valores informados." else result = profile to vm.plan(profile)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (index < steps.lastIndex) "Próximo" else "Calcular") }
        }
    }
}

@Composable
private fun ResultCard(p: CostPlan) {
    SectionCard("Seu resultado") {
        ValueRow("Km por semana", BrNumber.formatDecimal(p.kmPerWeek, 0) + " km")
        ValueRow("Custos fixos por mês", BrNumber.formatCents(p.fixedMonthCents))
        ValueRow("Custo fixo por km", BrNumber.formatCents(p.fixedCostCentsPerKm.toLong()))
        ValueRow("Combustível por km", BrNumber.formatCents(p.fuelCostCentsPerKm.toLong()))
        ValueRow("Custo total por km", BrNumber.formatCents(p.totalCostCentsPerKm.toLong()), bold = true)
    }
    SectionCard("Para bater a meta") {
        ValueRow("Lucro por km (bom a partir de)", BrNumber.formatCents(p.goalNetCentsPerKm.toLong()), bold = true)
        ValueRow("Ruim abaixo de", BrNumber.formatCents(p.lowNetCentsPerKm.toLong()))
        ValueRow("Lucro por hora (bom a partir de)", BrNumber.formatCents(p.goalNetCentsPerHour.toLong()), bold = true)
        ValueRow("Ruim abaixo de", BrNumber.formatCents(p.lowNetCentsPerHour.toLong()))
        Text(
            "Na prática: aceite ofertas que paguem a partir de ${BrNumber.formatCents(p.grossNeededCentsPerKm.toLong())} por km (valor bruto).",
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun cents(v: Long): String = BrNumber.formatCents(v).removePrefix("R$ ")

private fun validate(step: Step, days: Int, text: Map<Step, String>): String? {
    val t = text[step].orEmpty()
    return when (step) {
        Step.VEHICLE, Step.FUEL_TYPE -> null
        Step.DAYS -> if (days in 1..7) null else "Escolha quantos dias."
        Step.HOURS -> {
            val h = BrNumber.parseDecimal(t)?.toDouble()
            if (h != null && h > 0 && h <= 24) null else "Informe as horas (ex.: 9)."
        }
        Step.KM, Step.CONSUMPTION -> {
            val v = BrNumber.parseDecimal(t)?.toDouble()
            if (v != null && v > 0) null else "Informe um número maior que zero."
        }
        Step.PRICE -> {
            val c = BrNumber.parseCents(t)
            if (c != null && c > 0) null else "Informe o preço (ex.: 6,29)."
        }
        else -> if (t.isBlank() || (BrNumber.parseCents(t) ?: -1) >= 0) null else "Valor inválido (ex.: 150,00)."
    }
}

private fun buildProfile(days: Int, fuelType: FuelType, text: Map<Step, String>): CostProfile? = runCatching {
    fun money(s: Step) = text[s].orEmpty().let { if (it.isBlank()) 0L else BrNumber.parseCents(it)!! }
    fun num(s: Step) = BrNumber.parseDecimal(text[s].orEmpty())!!.toDouble()
    CostProfile(
        daysPerWeek = days,
        hoursPerDay = num(Step.HOURS),
        kmPerDay = num(Step.KM),
        weeklyGoalCents = money(Step.GOAL),
        maintenanceMonthCents = money(Step.MAINTENANCE),
        insuranceMonthCents = money(Step.INSURANCE),
        financingMonthCents = money(Step.FINANCING),
        otherMonthCents = money(Step.OTHER),
        ipvaYearCents = money(Step.IPVA),
        fuelType = fuelType,
        consumptionKmPerLiter = num(Step.CONSUMPTION),
        fuelPriceCentsPerLiter = money(Step.PRICE).toInt(),
    )
}.getOrNull()
