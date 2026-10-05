package com.otimizaai.domain.parser

import com.otimizaai.domain.model.CapturedOffer
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.util.BrNumber
import kotlin.math.roundToInt

/**
 * O que o entregador escolheu capturar da tela (opções selecionáveis em Ajustes).
 * Desligar um item faz o processador ignorar aquele dado, mesmo que ele apareça.
 */
data class CaptureOptions(
    val captureFreight: Boolean = true,
    val captureWarehouse: Boolean = true,
    val captureNeighborhoods: Boolean = true,
    val captureDistance: Boolean = true,
    val captureDuration: Boolean = true,
    val captureIds: Boolean = true,
)

/**
 * Padrões de ID de pedido por plataforma. O texto encontrado é guardado EXATAMENTE
 * como aparece (regra #1): o regex só LOCALIZA, nunca transforma.
 * Ficam configuráveis porque cada plataforma pode mudar o formato.
 */
data class IdPattern(val platform: Platform, val regex: Regex)

/**
 * ============================================================================
 *  SCREEN CAPTURE PROCESSOR: da árvore de textos da tela para ofertas avaliáveis
 * ============================================================================
 *  Entrada: textos coletados pelo serviço de Acessibilidade (ver OfferScreenParser.kt
 *  para a forma de percorrer a árvore). Saída: lista de [CapturedOffer].
 *
 *  HEURÍSTICA (nesta ordem):
 *   1. Pacote -> plataforma (tabela configurável). Pacote desconhecido = ignora.
 *   2. Reconhece o TIPO de tela por marcadores de texto:
 *        "Valor da rota"           -> detalhe de rota (valor, km, coleta, bairros)
 *        "Rota logística"          -> blocos por hora do Mercado Livre
 *        "(DCE5)" / "Amazon.com.br" -> blocos do Amazon Flex
 *   3. Para cada campo:
 *        VALOR (freight_cents): primeiro "R$ 0,00" do bloco. Um segundo valor no
 *          mesmo bloco costuma ser o preço antigo riscado (ex.: R$ 270 / R$ 225):
 *          é ignorado.
 *        GALPÃO: texto após "Coleta" ou "Início em", ou o código entre parênteses
 *          no Flex ("Fortaleza (DCE5)").
 *        BAIRRO: texto logo após cada rótulo "Entrega".
 *        KM: "19Km", "Percurso 19 km", "Coleta · 6Km".
 *        DURAÇÃO: "(6 h)", "3 hora 30 minuto", "(3 hora)".
 *        IDs: padrões de [IdPattern], guardados sem alteração.
 *   4. Aplica [CaptureOptions] (o que o entregador escolheu capturar).
 *
 *  Quando a plataforma expõe viewIdResourceName estável, preferir o viewId ao regex
 *  (tabela por plataforma/versão do app, fora do código).
 * ============================================================================
 */
object ScreenCaptureProcessor {

    fun process(
        packageName: String,
        lines: List<ScreenText>,
        packageToPlatform: Map<String, Platform>,
        options: CaptureOptions = CaptureOptions(),
        idPatterns: List<IdPattern> = emptyList(),
    ): List<CapturedOffer> {
        val platform = packageToPlatform[packageName] ?: return emptyList()
        val texts = lines.map { it.text }

        val offers: List<CapturedOffer> = when {
            texts.any { it.startsWith("Valor da rota", ignoreCase = true) } -> {
                val p = RouteDetailScreenParser.parse(lines)
                val freight = p.revenueCents ?: return emptyList()
                listOf(
                    CapturedOffer(
                        platform = platform,
                        freightCents = freight,
                        warehouse = p.warehouse,
                        neighborhoods = p.neighborhoods,
                        estimatedKm = p.routeKm?.let { it + (p.pickupKm ?: 0.0) },
                    ),
                )
            }
            texts.any { it.equals("Rota logística", ignoreCase = true) } ->
                LogisticRouteBlockParser.parse(lines).map {
                    CapturedOffer(platform, it.valueCents, warehouse = it.startPlace, durationMinutes = it.durationMinutes)
                }
            texts.any { it.contains("Amazon.com.br", ignoreCase = true) } ->
                FlexOfferListParser.parse(lines).map {
                    CapturedOffer(platform, it.valueCents, warehouse = it.station, durationMinutes = it.durationMinutes)
                }
            else -> emptyList()
        }

        val ids = if (options.captureIds) {
            idPatterns.filter { it.platform == platform }
                .flatMap { p -> texts.flatMap { t -> p.regex.findAll(t).map { it.value }.toList() } }
                .distinct()
        } else {
            emptyList()
        }

        return offers.map { o ->
            o.copy(
                warehouse = o.warehouse.takeIf { options.captureWarehouse },
                neighborhoods = if (options.captureNeighborhoods) o.neighborhoods else emptyList(),
                estimatedKm = o.estimatedKm.takeIf { options.captureDistance },
                durationMinutes = o.durationMinutes.takeIf { options.captureDuration },
                nativeIds = ids,
            )
        }.filter { options.captureFreight }
    }
}

/** Um bloco "Rota logística" do Mercado Livre (pago por hora, sem km). */
data class LogisticRouteBlock(
    val valueCents: Long,
    val window: String?,
    val durationMinutes: Int?,
    val startPlace: String?,
    val points: Int?,
)

/**
 * Lê os blocos "Rota logística" (lista "Disponíveis" e tela de detalhe):
 *   Rota logística · R$ 270 · R$ 225 (riscado) · 11:00 a 17:00 h (6 h) ·
 *   Início em Itaitinga, Itaitinga · 120 PONTOS
 */
object LogisticRouteBlockParser {

    private val money = Regex("""^R\$\s*([\d.]+(?:,\d{2})?)$""")
    private val window = Regex("""(\d{1,2}:\d{2})\s*(?:a|às)\s*(\d{1,2}:\d{2})\s*h(?:\s*\((\d+(?:[.,]\d+)?)\s*h\))?""")
    private val points = Regex("""^(\d+)\s*PONTOS$""", RegexOption.IGNORE_CASE)

    fun parse(lines: List<ScreenText>): List<LogisticRouteBlock> {
        val blocks = mutableListOf<LogisticRouteBlock>()
        var inBlock = false
        var value: Long? = null
        var win: String? = null
        var minutes: Int? = null
        var start: String? = null
        var pts: Int? = null

        fun flush() {
            val v = value
            if (inBlock && v != null) blocks += LogisticRouteBlock(v, win, minutes, start, pts)
            inBlock = false; value = null; win = null; minutes = null; start = null; pts = null
        }

        for (raw in lines) {
            val s = raw.text.trim()
            when {
                s.equals("Rota logística", ignoreCase = true) -> { flush(); inBlock = true }
                !inBlock -> Unit
                money.matches(s) -> if (value == null) {
                    // O primeiro valor é o atual; o segundo (riscado) é ignorado.
                    val txt = money.find(s)!!.groupValues[1]
                    // "R$ 1.270" (sem centavos): o ponto é de milhar, não decimal.
                    value = BrNumber.parseCents(if (',' in txt) txt else txt.replace(".", ""))
                }
                window.containsMatchIn(s) -> {
                    val m = window.find(s)!!
                    win = "${m.groupValues[1]} - ${m.groupValues[2]}"
                    minutes = m.groupValues[3].takeIf { it.isNotEmpty() }
                        ?.let { BrNumber.parseDecimal(it)?.toDouble()?.times(60)?.roundToInt() }
                }
                s.startsWith("Início em", ignoreCase = true) -> start = s.removePrefix("Início em").trim()
                points.matches(s) -> pts = points.find(s)!!.groupValues[1].toInt()
            }
        }
        flush()
        return blocks
    }
}
