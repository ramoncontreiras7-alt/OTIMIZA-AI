package com.otimiza.delivery.data.repository

import com.otimiza.delivery.data.local.dao.DeliveryStopDao
import com.otimiza.delivery.data.local.entity.DeliveryStopEntity
import com.otimiza.delivery.data.remote.VrpEngineClient
import com.otimiza.delivery.data.remote.VrpEngineException
import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.model.PlatformId
import io.mockk.MockKExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
@MockKExtension.ConfirmVerification
internal class DeliveryRepositoryImplTest {

    @RelaxedMockK
    lateinit var dao: DeliveryStopDao

    @RelaxedMockK
    lateinit var vrpEngineClient: VrpEngineClient

    @InjectMockKs
    lateinit var repository: DeliveryRepositoryImpl

    private val entity = DeliveryStopEntity(
        nativeStopId = "ML-12345",
        platformId = "MERCADO_LIVRE",
        platform = Platform.MERCADO_LIVRE,
        address = "Rua X, 100",
        latitude = -22.9068,
        longitude = -47.0469,
        sequence = 1,
        routeId = "session-1",
        createdAt = Instant.now().epochSecond,
        surrogateKey = null
    )

    @Test
    fun `findStopsBySession preserva native_stop_id e platform_id no dominio`() = runTest {
        coEvery { dao.findBySession("session-1") } returns listOf(entity)

        val result = repository.findStopsBySession("session-1")

        assertEquals(1, result.size)
        assertEquals("ML-12345", result.first().id.value)
        assertEquals("MERCADO_LIVRE", result.first().externalRef.value)
        assertEquals(Platform.MERCADO_LIVRE, result.first().platform)
        coVerify { dao.findBySession("session-1") }
    }

    @Test
    fun `saveStop mapeia e persiste entity preservando native id e platform id inalterados`() = runTest {
        val stop = DeliveryStop(
            id = NativeStopId("IFOOD_ORDER_777"),
            platform = Platform.IFOOD,
            externalRef = PlatformId("IFOOD"),
            address = "Av. Paulista, 1000",
            latitude = -23.561684,
            longitude = -46.655981,
            sequence = 1,
            routeId = "session-1",
            createdAt = Instant.now()
        )
        val slot = slot<DeliveryStopEntity>()
        coEvery { dao.upsert(capture(slot)) } returns Unit

        repository.saveStop(stop)

        val saved = slot.captured
        assertEquals("IFOOD_ORDER_777", saved.nativeStopId)
        assertEquals("IFOOD", saved.platformId)
        coVerify(exactly = 1) { dao.upsert(any()) }
    }

    @Test
    fun `optimizeSession retorna failure quando VrpEngineClient lanca excecao`() = runTest {
        coEvery { dao.findBySession("session-err") } returns listOf(entity)
        coEvery { vrpEngineClient.optimizeRoute(any()) } throws VrpEngineException("VRP down")

        val result = repository.optimizeSession("session-err")

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is VrpEngineException)
        assertEquals("VRP down", ex?.message)
        coVerify { dao.findBySession("session-err") }
        coVerify { vrpEngineClient.optimizeRoute(any()) }
    }
}
