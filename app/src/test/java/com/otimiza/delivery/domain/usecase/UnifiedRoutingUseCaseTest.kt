package com.otimiza.delivery.domain.usecase

import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.model.PlatformId
import com.otimiza.delivery.domain.repository.DeliveryRepository
import io.mockk.MockKExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
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
internal class UnifiedRoutingUseCaseTest {

    @RelaxedMockK
    lateinit var repository: DeliveryRepository

    @InjectMockKs
    lateinit var useCase: UnifiedRoutingUseCase

    @Test
    fun `deve preservar o ID nativo original apos round-trip VRP`() = runTest {
        val stop = DeliveryStop(
            id = NativeStopId("IFOOD_ORDER_999"),
            platform = Platform.IFOOD,
            externalRef = PlatformId("IFOOD"),
            address = "Av. Paulista, 1000",
            latitude = -23.561684,
            longitude = -46.655981,
            sequence = 1,
            routeId = "session-123",
            createdAt = Instant.now()
        )
        coEvery { repository.optimizeSession("session-123") } returns Result.success(listOf(stop))

        val result = useCase("session-123")

        assertTrue(result.isSuccess)
        val actual = result.getOrThrow()
        assertEquals(1, actual.size)
        assertEquals("IFOOD_ORDER_999", actual.first().id.value)
        coVerify(exactly = 1) { repository.optimizeSession("session-123") }
    }

    @Test
    fun `deve propagar falha quando o repositorio falha`() = runTest {
        coEvery { repository.optimizeSession("bad-session") } returns
            Result.failure(RuntimeException("VRP indisponível"))

        val result = useCase("bad-session")

        assertTrue(result.isFailure)
        coVerify { repository.optimizeSession("bad-session") }
    }

    @Test
    fun `nao deve mascarar IDs nativos quando a rota esta vazia`() = runTest {
        coEvery { repository.optimizeSession("empty") } returns Result.success(emptyList())

        val result = useCase("empty")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
        coVerify { repository.optimizeSession("empty") }
    }
}
