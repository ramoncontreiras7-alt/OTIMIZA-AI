package com.otimizaai.domain.model

/**
 * Situação de uma parada dentro da jornada.
 *
 * Assim como em [Platform], o que vai para o banco é o [code] estável, e não o
 * `name` do enum: renomear uma constante no código nunca corrompe dados gravados.
 */
enum class DeliveryStatus(val code: String) {
    PENDING("PENDING"),
    DELIVERED("DELIVERED"),
    FAILED("FAILED");

    companion object {
        private val byCode: Map<String, DeliveryStatus> = entries.associateBy { it.code }

        fun fromCode(code: String): DeliveryStatus =
            byCode[code] ?: throw IllegalArgumentException("Status desconhecido: [$code]")
    }
}
