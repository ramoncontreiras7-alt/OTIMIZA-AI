package com.otimizaai.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/**
 * Abre o Google Maps (ou o navegador) já com o destino/rota preenchidos.
 * O Maps mostra os km e o tempo da rota, que o entregador anota na tela "Hoje".
 */
object MapsLauncher {

    /** O Google Maps aceita no máximo 9 paradas intermediárias pelo link. */
    const val MAX_WAYPOINTS = 9

    fun navigateTo(context: Context, address: String) {
        open(context, "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=${enc(address)}")
    }

    /** Monta a rota na ordem da lista. Retorna quantas paradas entraram no link. */
    fun openRoute(context: Context, addresses: List<String>): Int {
        if (addresses.isEmpty()) return 0
        val used = addresses.take(MAX_WAYPOINTS + 1)
        val destination = used.last()
        val waypoints = used.dropLast(1)
        val url = buildString {
            append("https://www.google.com/maps/dir/?api=1&travelmode=driving")
            append("&destination=").append(enc(destination))
            if (waypoints.isNotEmpty()) append("&waypoints=").append(enc(waypoints.joinToString("|")))
        }
        open(context, url)
        return used.size
    }

    private fun open(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app de mapa encontrado.", Toast.LENGTH_LONG).show()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
