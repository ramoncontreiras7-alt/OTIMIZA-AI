package com.otimiza.delivery.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.Platform

@Composable
fun UnifiedMapScreen(
    viewModel: UnifiedMapViewModel,
    onStopClick: (DeliveryStop) -> Unit
) {
    val grouped by viewModel.stopsByPlatform.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibreMapContainer(
            stopsByPlatform = grouped,
            onStopSelected = { tag ->
                grouped.values.flatten()
                    .firstOrNull { buildMarkerTag(it) == tag }
                    ?.let(onStopClick)
            }
        )

        PlatformLegend(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            grouped = grouped
        )
    }
}

@Composable
private fun PlatformLegend(
    modifier: Modifier = Modifier,
    grouped: Map<Platform, List<DeliveryStop>>
) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Plataformas Ativas", style = MaterialTheme.typography.titleSmall)
            grouped.forEach { (platform, stops) ->
                val style = platform.toMarkerStyle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(style.color, shape = CircleShape)
                    )
                    androidx.compose.material3.Text(
                        "${style.label}: ${stops.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MapLibreMapContainer(
    stopsByPlatform: Map<Platform, List<DeliveryStop>>,
    onStopSelected: (tag: String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F0F2)),
        contentAlignment = Alignment.Center
    ) {
        Column {
            stopsByPlatform.forEach { (platform, stops) ->
                val style = platform.toMarkerStyle()
                Row(Modifier.padding(4.dp)) {
                    stops.forEach { stop ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp)
                                .background(style.color, shape = CircleShape)
                                .clickable { onStopSelected(buildMarkerTag(stop)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stop.externalRef.value.takeLast(4),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
