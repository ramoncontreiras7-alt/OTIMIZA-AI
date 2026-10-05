package com.otimizaai.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.Platform

/** Nome que aparece na tela para cada plataforma. */
val Platform.label: String
    get() = when (this) {
        Platform.IFOOD -> "iFood"
        Platform.MERCADO_LIVRE -> "Mercado Livre"
        Platform.LALAMOVE -> "Lalamove"
        Platform.AMAZON_FLEX -> "Amazon Flex"
    }

/** Cor de identificação de cada plataforma (bolinha nos cards e no mapa). */
val Platform.color: Color
    get() = when (this) {
        Platform.IFOOD -> Color(0xFFEA1D2C)
        Platform.MERCADO_LIVRE -> Color(0xFFFFD400)
        Platform.LALAMOVE -> Color(0xFFF16622)
        Platform.AMAZON_FLEX -> Color(0xFF2D6CDF)
    }

val DeliveryStatus.label: String
    get() = when (this) {
        DeliveryStatus.PENDING -> "Pendente"
        DeliveryStatus.DELIVERED -> "Entregue"
        DeliveryStatus.FAILED -> "Falhou"
    }

@Composable
fun PlatformDot(platform: Platform, size: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .background(platform.color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}
