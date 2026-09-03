package com.otimiza.delivery.data.local.mapper

import com.otimiza.delivery.data.local.entity.DeliveryStopEntity
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.model.PlatformId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

internal class DeliveryStopMapperTest {

    @Test
    fun `round-trip dominio entidade dominio preserva IDs nativos e coordenadas exatas`() {
        val original = DeliveryStop(
            id = NativeStopId("IFOOD_ORDER_42"),
            platform = Platform.IFOOD,
            externalRef = PlatformId("IFOOD"),
            address = "Av. Paulista, 1000",
            latitude = -23.561684,
            longitude = -46.655981,
            sequence = 3,
            routeId = "session-9",
            createdAt = Instant.parse("2026-09-03T12:00:00Z"),
            surrogateKey = "local-only-abc"
        )

        val entity: DeliveryStopEntity = original.toEntity()
        val back: DeliveryStop = entity.toDomain()

        assertEquals("IFOOD_ORDER_42", entity.nativeStopId)
        assertEquals("IFOOD", entity.platformId)
        assertEquals("IFOOD_ORDER_42", back.id.value)
        assertEquals("IFOOD", back.externalRef.value)
        assertEquals(-23.561684, back.latitude, 0.0)
        assertEquals(-46.655981, back.longitude, 0.0)
        assertEquals(3, back.sequence)
        assertEquals("local-only-abc", back.surrogateKey)
        assertEquals(original, back)
    }
}
