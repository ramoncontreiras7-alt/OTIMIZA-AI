package com.otimiza.delivery.domain.model

enum class Platform(
    val platformId: String,
    val displayName: String,
    val colorHex: Long
) {
    IFOOD("IFOOD", "iFood", 0xFFEA1D2CL),
    MERCADO_LIVRE("MERCADO_LIVRE", "Mercado Livre", 0xFFFFE600L),
    LALAMOVE("LALAMOVE", "Lalamove", 0xFFF96302L);

    companion object {
        fun fromNativeId(nativeId: String): Platform = when {
            nativeId.startsWith("IFOOD", ignoreCase = true) -> IFOOD
            nativeId.startsWith("ML", ignoreCase = true) -> MERCADO_LIVRE
            nativeId.startsWith("LALA", ignoreCase = true) -> LALAMOVE
            else -> throw IllegalArgumentException("Plataforma desconhecida para ID nativo: $nativeId")
        }
    }
}
