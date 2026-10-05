package com.otimizaai.domain.model

/**
 * Plataformas de entrega suportadas.
 *
 * Cada plataforma carrega um [PlatformId] estável — é ESSE código que vai para o
 * banco, e não o `name` do enum. Assim, renomear uma constante no código nunca
 * corrompe dados já gravados.
 *
 * Observação de arquitetura: cores, ícones e rótulos de tela NÃO ficam aqui.
 * Eles pertencem à camada de apresentação (Passo 5), mantendo o domínio puro.
 */
enum class Platform(val id: PlatformId) {
    IFOOD(PlatformId("IFOOD")),
    MERCADO_LIVRE(PlatformId("MERCADO_LIVRE")),
    LALAMOVE(PlatformId("LALAMOVE")),
    AMAZON_FLEX(PlatformId("AMAZON_FLEX")),
    MAGALU(PlatformId("MAGALU"));

    companion object {
        private val byId: Map<PlatformId, Platform> = entries.associateBy { it.id }

        /** Converte um código persistido de volta para o enum. Falha alto se desconhecido. */
        fun fromId(id: PlatformId): Platform =
            byId[id] ?: throw UnknownPlatformException(id)

        /** Versão tolerante, para fluxos de entrada onde o código pode não ser reconhecido. */
        fun fromIdOrNull(id: PlatformId): Platform? = byId[id]
    }
}

class UnknownPlatformException(id: PlatformId) :
    IllegalArgumentException("Plataforma desconhecida: [${id.value}]")
