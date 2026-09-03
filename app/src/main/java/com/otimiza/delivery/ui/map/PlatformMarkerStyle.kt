package com.otimiza.delivery.ui.map

import androidx.compose.ui.graphics.Color
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.Platform

data class MarkerStyle(
    val color: Color,
    val label: String
)

fun Platform.toMarkerStyle(): MarkerStyle = MarkerStyle(
    color = Color(colorHex.toInt()),
    label = displayName
)

fun buildMarkerTag(stop: DeliveryStop): String =
    "native:${stop.id.value}|ref:${stop.externalRef.value}"
