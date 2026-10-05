package com.otimizaai.domain.util

import java.text.Normalizer

/**
 * Comparação de NOMES (bairros, galpões) lidos da tela ou digitados.
 * Ignora maiúsculas, acentos e espaços extras: "Jóquei  Clube" == "joquei clube".
 *
 * ATENÇÃO: isto é só para nomes de lugares. IDs de pedido NUNCA passam por aqui
 * (regra #1: o ID original é intocável).
 */
object TextMatch {

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    fun sameName(a: String, b: String): Boolean = normalize(a) == normalize(b)

    /** [token] aparece como palavra inteira em [text]: "brnce20" em "BRNCE20 - Agência", mas não em "BRNCE203". */
    fun containsWord(text: String, token: String): Boolean {
        val t = normalize(token)
        if (t.isEmpty()) return false
        return Regex("(^|[^a-z0-9])" + Regex.escape(t) + "($|[^a-z0-9])").containsMatchIn(normalize(text))
    }
}
