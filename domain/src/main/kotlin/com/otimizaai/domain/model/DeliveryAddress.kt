package com.otimizaai.domain.model

/**
 * Endereço de entrega.
 *
 * [formatted] guarda o endereço como veio da origem (etiqueta, tela, planilha).
 * Os campos opcionais são preenchidos quando a origem os entrega separados.
 */
data class DeliveryAddress(
    val formatted: String,
    val complement: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
) {
    init {
        require(formatted.isNotBlank()) { "Endereço formatado não pode ser vazio." }
    }
}
