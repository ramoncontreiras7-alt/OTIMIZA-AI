package com.otimizaai.domain.parser

/** Uma entrega dentro da visita. IDs guardados EXATAMENTE como aparecem (regra #1). */
data class VisitDelivery(
    /** "ENTREGA 307535809 (21177067)" -> "307535809" */
    val nativeId: String,
    /** O número entre parênteses, quando houver -> "21177067" */
    val secondaryId: String?,
    val volumes: Int?,
    val invoice: String?,
    val orderId: String?,
    val load: String?,
)

/** Visita lida na tela "Detalhes da visita" (Magalu Entregas leves e pesadas). */
data class ParsedVisit(
    val shipper: String?,
    val sequence: Int?,
    val customerName: String?,
    val phone: String?,
    val postalCode: String?,
    val neighborhood: String?,
    val street: String?,
    val city: String?,
    val deliveries: List<VisitDelivery>,
    /** A tela menciona código/palavra-chave de recebimento -> a mensagem do WhatsApp avisa o cliente. */
    val requiresCode: Boolean,
)

/**
 * Lê a tela "Detalhes da visita" no formato:
 *   OMS CENTAURO · 13° - NOME DO CLIENTE · 85 9XXXX-XXXX ·
 *   60000-000 - BAIRRO · RUA, NÚMERO, COMPLEMENTO · Cidade/UF ·
 *   ENTREGA 307535809 (21177067) · Volumes: 1 · NF: ... · PEDIDO: ... · Carga: ...
 */
object VisitDetailsParser {

    private val sequenceName = Regex("""^(\d+)\s*[°º]\s*-\s*(.+)$""")
    private val phone = Regex("""^\+?(?:55\s*)?\(?\d{2}\)?\s*9?\s*\d{4}[-\s]?\d{4}$""")
    private val cepBairro = Regex("""^(\d{5}-?\d{3})\s*-\s*(.+)$""")
    private val cityUf = Regex("""^[\p{L} .'-]+/[A-Z]{2}$""")
    private val delivery = Regex("""^ENTREGA\s+([A-Za-z0-9-]+)(?:\s*\(([A-Za-z0-9-]+)\))?""", RegexOption.IGNORE_CASE)
    private val field = Regex("""^(Volumes|NF|PEDIDO|Carga)\s*:\s*(\S+)""", RegexOption.IGNORE_CASE)
    private val codeHints = listOf("código de entrega", "codigo de entrega", "código de recebimento", "palavra-chave", "palavra chave", "token de entrega")

    fun parse(lines: List<ScreenText>): ParsedVisit {
        val t = lines.map { it.text.trim() }.filter { it.isNotEmpty() }
        var shipper: String? = null
        var sequence: Int? = null
        var customer: String? = null
        var phoneNumber: String? = null
        var cep: String? = null
        var bairro: String? = null
        var street: String? = null
        var city: String? = null
        val deliveries = mutableListOf<VisitDelivery>()
        var current: VisitDelivery? = null

        t.forEachIndexed { i, s ->
            sequenceName.find(s)?.let { m ->
                sequence = m.groupValues[1].toInt()
                customer = m.groupValues[2].trim()
                shipper = t.getOrNull(i - 1)?.takeUnless { it.equals("FOTOS", true) || it.equals("PRINCIPAL", true) }
                return@forEachIndexed
            }
            if (phoneNumber == null && phone.matches(s)) { phoneNumber = s; return@forEachIndexed }
            cepBairro.find(s)?.let { m ->
                cep = m.groupValues[1]; bairro = m.groupValues[2].trim()
                street = t.getOrNull(i + 1)
                city = t.getOrNull(i + 2)?.takeIf { cityUf.matches(it) }
                return@forEachIndexed
            }
            delivery.find(s)?.let { m ->
                current?.let { deliveries += it }
                current = VisitDelivery(m.groupValues[1], m.groupValues[2].ifEmpty { null }, null, null, null, null)
                return@forEachIndexed
            }
            field.find(s)?.let { m ->
                val c = current ?: return@forEachIndexed
                val v = m.groupValues[2]
                current = when (m.groupValues[1].lowercase()) {
                    "volumes" -> c.copy(volumes = v.toIntOrNull())
                    "nf" -> c.copy(invoice = v)
                    "pedido" -> c.copy(orderId = v)
                    else -> c.copy(load = v)
                }
            }
        }
        current?.let { deliveries += it }

        val all = t.joinToString(" ").lowercase()
        return ParsedVisit(
            shipper = shipper,
            sequence = sequence,
            customerName = customer,
            phone = phoneNumber,
            postalCode = cep,
            neighborhood = bairro,
            street = street,
            city = city,
            deliveries = deliveries,
            requiresCode = codeHints.any { it in all },
        )
    }
}
