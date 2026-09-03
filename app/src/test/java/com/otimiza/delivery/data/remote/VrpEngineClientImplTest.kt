package com.otimiza.delivery.data.remote

import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.model.PlatformId
import io.mockk.MockKExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.Response
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
@MockKExtension.ConfirmVerification
internal class VrpEngineClientImplTest {

    @MockK
    lateinit var api: VrpApiService

    private lateinit var client: VrpEngineClientImpl

    @BeforeEach
    fun setUp() {
        client = VrpEngineClientImpl(api, baseUrl = "https://vrp.test")
    }

    @Test
    fun `reordena lista original preservando IDs nativos pelos indices retornados`() = runTest {
        val stops = listOf(stop("IFOOD_A"), stop("IFOOD_B"), stop("IFOOD_C"))
        coEvery { api.optimizeRoute(any(), any()) } returns Response.success(
            VrpOptimizationResponse(routeId = "r1", optimizedSequence = listOf(2, 0, 1), totalDistanceMeters = 3000.0)
        )

        val result = client.optimizeRoute(stops)

        assertEquals(3, result.size)
        assertEquals("IFOOD_C", result[0].id.value)
        assertEquals("IFOOD_A", result[1].id.value)
        assertEquals("IFOOD_B", result[2].id.value)
        coVerify(exactly = 1) { api.optimizeRoute(any(), any()) }
    }

    @Test
    fun `lança VrpEngineException quando a resposta nao e bem sucedida`() = runTest {
        coEvery { api.optimizeRoute(any(), any()) } returns Response.error(500, mockk<ResponseBody>())

        val ex = org.junit.jupiter.api.Assertions.assertThrows(VrpEngineException::class.java) {
            client.optimizeRoute(listOf(stop("IFOOD_A")))
        }
        assertTrue(ex.message?.contains("500") == true)
        coVerify { api.optimizeRoute(any(), any()) }
    }

    @Test
    fun `retorna lista vazia sem chamar a API quando nao ha paradas`() = runTest {
        val result = client.optimizeRoute(emptyList())

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { api.optimizeRoute(any(), any()) }
    }

    @Test
    fun `payload enviado ao motor carrega os native ids originais intactos`() = runTest {
        val stops = listOf(stop("LALA_9"), stop("LALA_8"))
        val requestSlot = slot<VrpOptimizationRequest>()
        coEvery { api.optimizeRoute(capture(requestSlot), any()) } returns Response.success(
            VrpOptimizationResponse(routeId = "r2", optimizedSequence = listOf(0, 1), totalDistanceMeters = 1000.0)
        )

        client.optimizeRoute(stops)

        val payload = requestSlot.captured
        assertEquals("LALA_9", payload.stops[0].nativeStopId)
        assertEquals("LALA_8", payload.stops[1].nativeStopId)
        coVerify(exactly = 1) { api.optimizeRoute(any(), any()) }
    }

    private fun stop(id: String): DeliveryStop = DeliveryStop(
        id = NativeStopId(id),
        platform = Platform.fromNativeId(id),
        externalRef = PlatformId(id.substringBefore("_")),
        address = "addr-$id",
        latitude = -23.5,
        longitude = -46.6,
        sequence = 0,
        routeId = "session-x",
        createdAt = Instant.now()
    )
}
