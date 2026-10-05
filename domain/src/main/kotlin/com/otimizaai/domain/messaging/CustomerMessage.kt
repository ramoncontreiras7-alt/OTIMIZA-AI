package com.otimizaai.domain.messaging

import java.net.URLEncoder

/** Momento da mensagem ao cliente. */
enum class MessageMoment { CHEGANDO, NO_ENDERECO }

/**
 * Textos das mensagens de WhatsApp. O entregador pode trocar qualquer um nos Ajustes.
 *
 * Marcadores aceitos (substituídos automaticamente; se faltar o dado, somem do texto):
 *   {cliente}     nome do cliente
 *   {entregador}  nome do entregador (configurado uma vez)
 *   {plataforma}  ex.: "Mercado Livre"
 *   {pedido}      número do pedido, EXATAMENTE como na plataforma
 *   {endereco}    endereço da entrega
 */
data class MessageTemplates(
    val arriving: String = DEFAULT_ARRIVING,
    val atAddress: String = DEFAULT_AT_ADDRESS,
    /** Acrescentado ao final quando a entrega exige código/palavra-chave no recebimento. */
    val codeReminder: String = DEFAULT_CODE_REMINDER,
) {
    companion object {
        const val DEFAULT_ARRIVING =
            "Olá, {cliente}! Sou {entregador}, entregador {plataforma}. Estou chegando no seu endereço com a sua entrega (pedido {pedido})."
        const val DEFAULT_AT_ADDRESS =
            "Olá, {cliente}! Sou {entregador}, entregador {plataforma}. Já estou no seu endereço ({endereco}) com a sua entrega (pedido {pedido})."
        const val DEFAULT_CODE_REMINDER =
            "Esta entrega exige o código de recebimento. Por favor, tenha o código em mãos para informar no momento da entrega."
    }
}

/** Dados de uma entrega para montar a mensagem. */
data class MessageContext(
    val customerName: String?,
    val driverName: String?,
    val platformName: String?,
    val orderId: String?,
    val address: String?,
    /** A plataforma exige código ou palavra-chave na entrega (ex.: código de 4 dígitos). */
    val requiresCode: Boolean,
)

object CustomerMessage {

    /** Monta o texto final, já com o lembrete de código quando a entrega exige. */
    fun build(moment: MessageMoment, ctx: MessageContext, templates: MessageTemplates = MessageTemplates()): String {
        val base = when (moment) {
            MessageMoment.CHEGANDO -> templates.arriving
            MessageMoment.NO_ENDERECO -> templates.atAddress
        }
        val text = if (ctx.requiresCode && templates.codeReminder.isNotBlank()) "$base ${templates.codeReminder}" else base
        return render(
            text,
            mapOf(
                "cliente" to firstName(ctx.customerName),
                "entregador" to ctx.driverName,
                "plataforma" to ctx.platformName,
                "pedido" to ctx.orderId,
                "endereco" to ctx.address,
            ),
        )
    }

    /** Substitui os marcadores e limpa o que sobrou de dados ausentes. */
    fun render(template: String, values: Map<String, String?>): String {
        var s = template
        values.forEach { (k, v) -> s = s.replace("{$k}", v?.trim().orEmpty()) }
        return s
            .replace(Regex("""\{[a-z]+}"""), "")                 // marcador desconhecido
            .replace(Regex("""\s*\(\s*(pedido\s*)?\)"""), "")    // "(pedido )" ou "()" vazios
            .replace(Regex(""",\s*!"""), "!")                   // "Olá, !" -> "Olá!"
            .replace(Regex("""entregador\s+([.,!])"""), "entregador$1")
            .replace(Regex("""Sou\s*,"""), "Sou o entregador,")
            .replace(Regex("""\s+([.,!])"""), "$1")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    /**
     * Link do WhatsApp com a mensagem pronta. O app abre e o ENTREGADOR toca em enviar
     * (nada é enviado sozinho). Retorna null se o telefone não for válido.
     */
    fun whatsAppLink(phone: String, message: String): String? {
        val number = normalizeBrazilianPhone(phone) ?: return null
        val text = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        return "https://wa.me/$number?text=$text"
    }

    /** "85 99131-6471" -> "5585991316471". Aceita com ou sem +55. */
    fun normalizeBrazilianPhone(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("55") && digits.length in 12..13 -> digits
            digits.length in 10..11 -> "55$digits"
            else -> null
        }
    }

    private fun firstName(name: String?): String? =
        name?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?.lowercase()?.replaceFirstChar { it.uppercase() }
}
