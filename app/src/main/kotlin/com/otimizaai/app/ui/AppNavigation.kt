package com.otimizaai.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.otimizaai.app.ui.offer.OfferScreen
import com.otimizaai.app.ui.settings.SettingsScreen
import com.otimizaai.app.ui.stops.StopsScreen
import com.otimizaai.app.ui.today.TodayScreen
import com.otimizaai.app.ui.wizard.CostWizardScreen

private const val WIZARD_ROUTE = "assistente-custos"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("hoje", "Rota", Icons.Filled.Home),
    STOPS("paradas", "Paradas", Icons.Filled.LocationOn),
    OFFER("oferta", "Avaliar", Icons.Filled.ThumbUp),
    SETTINGS("ajustes", "Ajustes", Icons.Filled.Settings),
}

/** Barra de abas no rodapé + troca de telas. */
@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.TODAY.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.TODAY.route) { TodayScreen(onGoToStops = { nav.navigate(Tab.STOPS.route) { launchSingleTop = true } }) }
            composable(Tab.STOPS.route) { StopsScreen() }
            composable(Tab.OFFER.route) { OfferScreen() }
            composable(Tab.SETTINGS.route) { SettingsScreen(onOpenWizard = { nav.navigate(WIZARD_ROUTE) }) }
            composable(WIZARD_ROUTE) { CostWizardScreen(onClose = { nav.popBackStack() }) }
        }
    }
}
