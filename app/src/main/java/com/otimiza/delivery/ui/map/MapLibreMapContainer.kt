package com.otimiza.delivery.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Marker
import com.mapbox.mapboxsdk.plugins.annotation.MarkerManager
import com.mapbox.mapboxsdk.plugins.annotation.MarkerOptions
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.Platform

private const val MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"

@Composable
fun MapLibreMapContainer(
    modifier: Modifier = Modifier,
    stopsByPlatform: Map<Platform, List<DeliveryStop>>,
    onMarkerClick: (tag: String) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
    val markerManagers = remember(context) { mutableMapOf<MapboxMap, MarkerManager>() }
    val currentMarkers = remember { mutableListOf<Marker>() }

    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                if (map.style == null) {
                    map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) {
                        attachMarkers(map, markerManagers, currentMarkers, stopsByPlatform, onMarkerClick)
                        centerCamera(map, stopsByPlatform)
                    }
                } else {
                    attachMarkers(map, markerManagers, currentMarkers, stopsByPlatform, onMarkerClick)
                    centerCamera(map, stopsByPlatform)
                }
            }
        }
    )
}

private fun attachMarkers(
    map: MapboxMap,
    managers: MutableMap<MapboxMap, MarkerManager>,
    currentMarkers: MutableList<Marker>,
    stopsByPlatform: Map<Platform, List<DeliveryStop>>,
    onMarkerClick: (String) -> Unit
) {
    currentMarkers.forEach { it.remove() }
    currentMarkers.clear()

    val manager = managers.getOrPut(map) { MarkerManager(map) }
    val iconCache = managers.getOrPut(map) { MarkerManager(map) }
        .let { mutableMapOf<Platform, com.mapbox.mapboxsdk.plugins.annotation.Icon>() }

    stopsByPlatform.values.flatten().forEach { stop ->
        val icon = iconCache.getOrPut(stop.platform) {
            val factory = com.mapbox.mapboxsdk.plugins.annotation.IconFactory.getInstance(map)
            factory.defaultMarker(com.mapbox.mapboxsdk.plugins.annotation.IconFactory.IconColor.RED)
        }
        val marker = manager.create(
            MarkerOptions()
                .position(LatLng(stop.latitude, stop.longitude))
                .title(stop.address)
                .snippet(buildMarkerTag(stop))
                .icon(icon)
        )
        currentMarkers += marker
        marker.setOnMarkerClickListener {
            onMarkerClick(buildMarkerTag(stop))
            true
        }
    }
}

private fun centerCamera(
    map: MapboxMap,
    stopsByPlatform: Map<Platform, List<DeliveryStop>>
) {
    val all = stopsByPlatform.values.flatten()
    if (all.isEmpty()) return
    val avgLat = all.map { it.latitude }.average()
    val avgLng = all.map { it.longitude }.average()
    map.animateCamera(
        CameraUpdateFactory.newLatLngZoom(LatLng(avgLat, avgLng), 12.0)
    )
}

private fun buildMarkerTag(stop: DeliveryStop): String =
    "native:${stop.id.value}|platform:${stop.externalRef.value}"