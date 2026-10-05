package com.otimizaai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Cores das três faixas de lucro (boa / média / ruim). */
val BandGood = Color(0xFF2E9E5B)
val BandMid = Color(0xFFF2C230)
val BandBad = Color(0xFFD9483B)
val ProfitGood = BandGood
val ProfitBad = BandBad

private val Light = lightColorScheme(
    primary = Color(0xFF1D5D7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E8F2),
    onPrimaryContainer = Color(0xFF0B2A38),
    secondary = Color(0xFFB47C10),
    background = Color(0xFFF4F5F3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6EAE8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7CC0DE),
    onPrimary = Color(0xFF0B2A38),
    primaryContainer = Color(0xFF1F4556),
    onPrimaryContainer = Color(0xFFD3E8F2),
    secondary = Color(0xFFE3A83A),
    background = Color(0xFF13181B),
    surface = Color(0xFF1B2226),
    surfaceVariant = Color(0xFF2A3338),
)

@Composable
fun OtimizaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
