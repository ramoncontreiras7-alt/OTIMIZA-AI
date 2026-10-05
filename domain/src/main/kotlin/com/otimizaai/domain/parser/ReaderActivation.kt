package com.otimizaai.domain.parser

import com.otimizaai.domain.model.PlatformFinancialConfig

/**
 * ============================================================================
 *  QUANDO O LEITOR DE TELA PODE LIGAR E QUANDO ELE LÊ
 * ============================================================================
 *  Regra do Ramon (05/10/2026):
 *   1. O leitor SÓ pode ser ligado depois que as configurações estiverem prontas.
 *   2. O gatilho é automático: basta a tela de ofertas/aceite da plataforma estar
 *      aberta. Não há botão para "capturar". O app lê e avisa; quem aceita é o entregador.
 * ============================================================================
 */

/** O que o entregador já configurou. */
data class ReaderSetup(
    /** Autorizou o serviço de Acessibilidade (opt-in explícito, exigência do Google Play). */
    val accessibilityConsent: Boolean,
    /** Fez o assistente de custos (consumo, preço do combustível, custos fixos). */
    val costsConfigured: Boolean,
    /** Plataformas que o entregador escolheu monitorar. */
    val monitoredPackages: Set<String>,
    /** Regras financeiras por plataforma (podem estar vazias: aí vale só o custo). */
    val platformConfigs: List<PlatformFinancialConfig>,
)

/** Resultado da checagem: pronto, ou a lista do que falta (em português, para a tela). */
sealed interface ReaderReadiness {
    data object Ready : ReaderReadiness
    data class Missing(val items: List<String>) : ReaderReadiness
}

object ReaderActivation {

    /** Confere se o leitor pode ser ligado. */
    fun check(setup: ReaderSetup): ReaderReadiness {
        val missing = buildList {
            if (!setup.costsConfigured) add("Fazer o assistente de custos (Ajustes > Calcular meu custo por km).")
            if (setup.monitoredPackages.isEmpty()) add("Escolher pelo menos uma plataforma para monitorar.")
            if (!setup.accessibilityConsent) add("Autorizar a leitura de tela nas configurações de Acessibilidade do Android.")
        }
        return if (missing.isEmpty()) ReaderReadiness.Ready else ReaderReadiness.Missing(missing)
    }

    /**
     * Marcadores de tela de ofertas/aceite, vistos nos prints:
     *  - "Aceitar corrida"            (detalhe de rota com coleta e entregas)
     *  - "Agendar" / "Disponíveis"    (Rota logística do Mercado Livre)
     *  - "OFERTAS" / "DETALHES DA OFERTA" / "Programar" (Amazon Flex)
     * A lista é configurável porque os apps mudam os textos.
     */
    val DEFAULT_OFFER_MARKERS: List<String> = listOf(
        "Aceitar corrida", "Agendar", "Disponíveis", "OFERTAS", "DETALHES DA OFERTA", "Programar",
    )

    /**
     * Gatilho automático: o serviço de Acessibilidade chama isto a cada mudança de tela.
     * Só lê quando: o leitor está pronto, o app em primeiro plano é monitorado e a tela
     * tem um marcador de oferta. Fora disso, ignora (economiza bateria e respeita a privacidade).
     */
    fun shouldRead(
        setup: ReaderSetup,
        foregroundPackage: String,
        lines: List<ScreenText>,
        markers: List<String> = DEFAULT_OFFER_MARKERS,
    ): Boolean {
        if (check(setup) != ReaderReadiness.Ready) return false
        if (foregroundPackage !in setup.monitoredPackages) return false
        return lines.any { line -> markers.any { line.text.trim().equals(it, ignoreCase = true) } }
    }
}
