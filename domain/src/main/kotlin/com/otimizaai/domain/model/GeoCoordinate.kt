package com.otimizaai.domain.model

/**
 * Coordenada geográfica (WGS84 — o mesmo padrão do GPS, OSRM e MapLibre).
 * Valida as faixas físicas possíveis para impedir que um par lat/lng invertido
 * ou corrompido chegue ao motor de roteamento.
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude fora da faixa: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude fora da faixa: $longitude" }
    }
}
