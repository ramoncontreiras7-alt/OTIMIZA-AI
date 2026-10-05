package com.otimizaai.domain.parser

import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RouteOffer
import com.otimizaai.domain.util.BrNumber
import kotlin.math.roundToLong

/*
 * ============================================================================
 *  LEITURA DA TELA DAS PLATAFORMAS  (contrato para o ScreenCaptureProcessor)
 * ============================================================================
 *  COMO O SERVIÇO DE ACESSIBILIDADE (app) DEVE ALIMENTAR ESTES LEITORES:
 *
 *  1. Só depois do consentimento explícito do entregador (opt-in), e só para os
 *     pacotes das plataformas que ele ativou. Nunca clicar/aceitar nada: só LER.
 *
 *  2. Ao receber TYPE_WINDOW_CONTENT_CHANGED / TYPE_WINDOW_STATE_CHANGED, percorrer
 *     a árvore a partir de rootInActiveWindow em PRÉ-ORDEM (pai antes dos filhos,
 *     filhos da esquerda para a direita), o que reproduz a ordem de leitura da tela.
 *
 *  3. Para cada nó com texto, criar um ScreenText com:
 *       - text            = node.text ?: node.contentDescription
 *       - viewId          = node.viewIdResourceName (ex.: "com.app:id/route_value"), se houver
 *     Ignorar nós invisíveis (!isVisibleToUser) e textos vazios.
 *
 *  4. ESTRATÉGIA DE EXTRAÇÃO (nesta ordem):
 *       a) viewId conhecido -> valor direto. É o mais confiável, mas muda quando a
 *          plataforma atualiza o app; por isso fica numa tabela configurável por
 *          plataforma/versão (fora do código), NUNCA espalhado pelo app.
 *       b) rótulo + valor (regex) -> o que estes parsers fazem: acham o rótulo
 *          ("Valor da rota", "Percurso", "Coleta", "Entrega") e leem o texto
 *          seguinte (ou o restante do mesmo nó).
 *
 *  5. GALPÃO e BAIRRO:
 *       - Galpão: texto logo após "Coleta" (ex.: "PETZ *** LOJA 246 ... MARAPONGA ...");
 *         no Mercado Livre o código do centro aparece no início ("BRNCE20 - ...", "SCE3 - ...");
 *         no Amazon Flex o código da estação vem entre parênteses ("Fortaleza (DCE5)").
 *       - Bairro: texto logo após cada rótulo "Entrega" (ex.: "Maraponga", "Jóquei Clube").
 *
 *  6. Fazer debounce (~300 ms) e deduplicar por "assinatura" da oferta (valor + km +
 *     coleta), porque a mesma tela dispara vários eventos.
 * ============================================================================
 */

/** Um pedaço de texto lido da tela, na ordem de leitura. */
data class ScreenText(val text: String, val viewId: String? = null)

/** Oferta extraída da tela. Campos ficam null quando não aparecem. */
data class ParsedRouteOffer(
    val revenueCents: Long?,
    val routeKm: Double?,
    val pickupKm: Double?,
    val deliveries: Int?,
    val warehouse: String?,
    val neighborhoods: List<String>,
    val returnToStore: Boolean?,
) {
    /** Tem o mínimo para avaliar? (valor e distância) */
    val isComplete: Boolean get() = revenueCents != null && routeKm != null

    /**
     * Converte em [RouteOffer] para o motor financeiro.
     * [includePickup] = true soma o deslocamento até a coleta, que também gasta combustível.
     */
    fun toRouteOffer(platform: Platform, includePickup: Boolean = true): RouteOffer? {
        if (!isComplete) return null
        val km = routeKm!! + if (includePickup) (pickupKm ?: 0.0) else 0.0
        return RouteOffer(
            platform = platform,
            warehouse = warehouse,
            revenueCents = revenueCents!!,
            distanceMeters = (km * 1000).roundToLong(),
            deliveryNeighborhoods = neighborhoods,
        )
    }
}

/**
 * Lê a tela de detalhe de rota no formato:
 *   Valor da rota / R$ 40,59 · Percurso / 19Km · Entregas / 04 · Retorno à Loja / Não ·
 *   Coleta · 6Km · <endereço da coleta> · Entrega / <bairro> · Entrega / <bairro> ...
 */
object RouteDetailScreenParser {

    private val money = Regex("""R\$\s*([\d.]+,\d{2})""")
    private val km = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE)
    private val number = Regex("""^\s*(\d+)\s*$""")

    fun parse(lines: List<ScreenText>): ParsedRouteOffer {
        val t = lines.map { it.text.trim() }.filter { it.isNotEmpty() }
        var revenue: Long? = null
        var routeKm: Double? = null
        var pickupKm: Double? = null
        var deliveries: Int? = null
        var warehouse: String? = null
        var returnToStore: Boolean? = null
        val neighborhoods = mutableListOf<String>()

        var i = 0
        while (i < t.size) {
            val line = t[i]
            val next = t.getOrNull(i + 1)
            when {
                line.startsWith("Valor da rota", ignoreCase = true) -> {
                    revenue = findMoney(line) ?: next?.let { findMoney(it) }
                }
                line.startsWith("Percurso", ignoreCase = true) -> {
                    routeKm = findKm(line) ?: next?.let { findKm(it) }
                }
                line.startsWith("Entregas", ignoreCase = true) -> {
                    deliveries = Regex("""\d+""").find(line.removePrefix("Entregas"))?.value?.toInt()
                        ?: next?.let { number.find(it)?.groupValues?.get(1)?.toInt() }
                }
                line.startsWith("Retorno", ignoreCase = true) -> {
                    val answer = if (line.contains("sim", true) || line.endsWith("não", true)) line else next.orEmpty()
                    returnToStore = when {
                        answer.endsWith("sim", ignoreCase = true) -> true
                        answer.endsWith("não", ignoreCase = true) || answer.endsWith("nao", ignoreCase = true) -> false
                        else -> null
                    }
                }
                line.startsWith("Coleta", ignoreCase = true) -> {
                    pickupKm = findKm(line)
                    warehouse = next
                    i++ // o próximo texto é o endereço da coleta
                }
                line.equals("Entrega", ignoreCase = true) -> {
                    next?.takeUnless { it.equals("Entrega", true) || it.startsWith("Aceitar", true) }?.let {
                        neighborhoods += it
                        i++
                    }
                }
            }
            i++
        }
        return ParsedRouteOffer(revenue, routeKm, pickupKm, deliveries, warehouse, neighborhoods, returnToStore)
    }

    private fun findMoney(s: String): Long? = money.find(s)?.groupValues?.get(1)?.let { BrNumber.parseCents(it) }
    private fun findKm(s: String): Double? = km.find(s)?.groupValues?.get(1)?.let { BrNumber.parseDecimal(it)?.toDouble() }
}

/** Um bloco de ofertas do Amazon Flex. */
data class FlexBlockOffer(
    val station: String,
    val stationCode: String?,
    val window: String?,
    val durationMinutes: Int?,
    val valueCents: Long,
)

/**
 * Lê a lista "OFERTAS" do Amazon Flex no formato:
 *   Fortaleza (DCE5) - Amazon.com.br · 19:15 - 22:45 · 3 hora 30 minuto · R$ 222,50
 * O Flex não mostra km: a avaliação usa R$/hora e a regra do galpão (estação).
 */
object FlexOfferListParser {

    private val stationLine = Regex("""^(.+\(([A-Z]{2,4}\d{1,2})\).*)$""")
    private val window = Regex("""^\d{1,2}(:\d{2})?\s*-\s*\d{1,2}(:\d{2})?$""")
    private val duration = Regex("""(\d+)\s*hora[s]?(?:\s*(\d+)\s*minuto[s]?)?""", RegexOption.IGNORE_CASE)
    private val money = Regex("""R\$\s*([\d.]+,\d{2})""")

    fun parse(lines: List<ScreenText>): List<FlexBlockOffer> {
        val result = mutableListOf<FlexBlockOffer>()
        var station: String? = null
        var code: String? = null
        var win: String? = null
        var minutes: Int? = null
        for (raw in lines) {
            val s = raw.text.trim()
            val st = stationLine.find(s)
            when {
                st != null -> {
                    station = st.groupValues[1].trim(); code = st.groupValues[2]; win = null; minutes = null
                }
                station != null && window.matches(s) -> win = s
                station != null && duration.containsMatchIn(s) && !s.contains("R$") -> {
                    val m = duration.find(s)!!
                    minutes = m.groupValues[1].toInt() * 60 + (m.groupValues[2].toIntOrNull() ?: 0)
                }
                station != null && money.containsMatchIn(s) -> {
                    val cents = BrNumber.parseCents(money.find(s)!!.groupValues[1])
                    if (cents != null) result += FlexBlockOffer(station, code, win, minutes, cents)
                    station = null
                }
            }
        }
        return result
    }
}
