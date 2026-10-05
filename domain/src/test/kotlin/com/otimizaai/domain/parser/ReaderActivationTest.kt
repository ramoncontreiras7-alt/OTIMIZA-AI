package com.otimizaai.domain.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ReaderActivationTest {

    private val ready = ReaderSetup(accessibilityConsent = true, costsConfigured = true, monitoredPackages = setOf("app.ml"), platformConfigs = emptyList())
    private val offerScreen = listOf("Rota logística", "R$ 150", "Início em SCE3 - Itaitinga, Itaitinga", "Agendar").map { ScreenText(it) }

    @Test
    @DisplayName("Leitor só liga com as configurações feitas; lista o que falta")
    fun readiness() {
        assertEquals(ReaderReadiness.Ready, ReaderActivation.check(ready))
        val missing = ReaderActivation.check(ready.copy(costsConfigured = false, accessibilityConsent = false))
        assertTrue(missing is ReaderReadiness.Missing)
        assertEquals(2, (missing as ReaderReadiness.Missing).items.size)
    }

    @Test
    @DisplayName("Gatilho automático: lê só na tela de oferta do app monitorado, com tudo configurado")
    fun trigger() {
        assertTrue(ReaderActivation.shouldRead(ready, "app.ml", offerScreen))
        assertFalse(ReaderActivation.shouldRead(ready, "app.outro", offerScreen))
        assertFalse(ReaderActivation.shouldRead(ready, "app.ml", listOf(ScreenText("Início"), ScreenText("Agendados"))))
        assertFalse(ReaderActivation.shouldRead(ready.copy(costsConfigured = false), "app.ml", offerScreen))
    }
}
