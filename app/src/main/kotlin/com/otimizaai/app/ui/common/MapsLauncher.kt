package com.otimizaai.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.otimizaai.app.settings.NavigationApp
import java.net.URLEncoder

/**
 * Abre o app de navegação escolhido (Google Maps ou Waze) já com o destino.
 * O Google Maps também aceita a rota inteira (até 10 paradas); o Waze navega
 * uma parada por vez.
 */
object MapsLauncher {

    /** O Google Maps aceita no máximo 9 paradas intermediárias pelo link. */
    const val MAX_WAYPOINTS = 9

    fun navigateTo(context: Context, address: String, app: NavigationApp) {
        val url = when (app) {
            NavigationApp.WAZE -> "https://waze.com/ul?q=${enc(address)}&navigate=yes"
            NavigationApp.GOOGLE_MAPS -> "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=${enc(address)}"
        }
        open(context, url)
    }

    /**
     * Monta a rota na ordem da lista. No Waze, abre só a primeira parada.
     * Retorna quantas paradas entraram.
     */
    fun openRoute(context: Context, addresses: List<String>, app: NavigationApp): Int {
        if (addresses.isEmpty()) return 0
        if (app == NavigationApp.WAZE) {
            navigateTo(context, addresses.first(), app)
            return 1
        }
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
