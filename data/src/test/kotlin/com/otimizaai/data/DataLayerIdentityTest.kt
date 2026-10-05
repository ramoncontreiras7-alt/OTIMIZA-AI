package com.otimizaai.data

import com.otimizaai.data.local.dao.DeliveryStopDao
import com.otimizaai.data.local.dao.StatusCountRow
import com.otimizaai.data.local.mapper.toDomain
import com.otimizaai.data.local.mapper.toEntity
import com.otimizaai.data.local.mapper.toStatusCounts
import com.otimizaai.data.repository.DeliveryStopRepositoryImpl
import com.otimizaai.domain.model.DeliveryAddress
import com.otimizaai.domain.model.DeliveryStatus
import com.otimizaai.domain.model.DeliveryStop
import com.otimizaai.domain.model.GeoCoordinate
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.Platform
import com.otimizaai.domain.model.RegisterStopResult
import com.otimizaai.domain.model.RouteSessionId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Garante a REGRA #1 na passagem domínio -> banco -> domínio.
 */
class DataLayerIdentityTest {

    private val today = RouteSessionId("2026-10-05")
    private val yesterday = RouteSessionId("2026-10-04")

    private fun stop(id: String, session: RouteSessionId = today) = DeliveryStop.create(
        id = NativeStopId(id),
        platform = Platform.MERCADO_LIVRE,
        sessionId = session,
        address = DeliveryAddress("R. Barão de Aracati, 210", city = "Fortaleza"),
        coordinate = GeoCoordinate(-3.7319, -38.5267),
        freightCents = 1_150,
    )

    @Test
    @DisplayName("Ida e volta pelo banco mantém o ID byte a byte")
    fun mapperRoundTripKeepsIdVerbatim() {
        val original = stop("mlb-0004419027731/Ç#a")
        val entity = original.toEntity()
        assertEquals("mlb-0004419027731/Ç#a", entity.nativeStopId)
        assertEquals("MERCADO_LIVRE", entity.platformId)
        assertEquals(original, entity.toDomain())
    }

    @Test
    @DisplayName("Linha com meia coordenada no banco falha alto")
    fun halfCoordinateFails() {
        val corrupted = stop("MLB-1").toEntity().copy(longitude = null)
        assertThrows<IllegalStateException> { corrupted.toDomain() }
    }

    @Test
    @DisplayName("Contadores mostram todos os status, inclusive os zerados")
    fun statusCountsAreComplete() {
        val counts = listOf(StatusCountRow("DELIVERED", 17), StatusCountRow("FAILED", 1)).toStatusCounts()
        assertEquals(mapOf(DeliveryStatus.PENDING to 0, DeliveryStatus.DELIVERED to 17, DeliveryStatus.FAILED to 1), counts)
    }

    @Test
    @DisplayName("Registrar parada nova -> Inserted")
    fun registerNew() = runTest {
        val dao = mockk<DeliveryStopDao>()
        coEvery { dao.insertOrGetExisting(any()) } returns null
        val result = DeliveryStopRepositoryImpl(dao).register(stop("MLB-1"))
        assertEquals(RegisterStopResult.Inserted, result)
    }

    @Test
    @DisplayName("Mesma parada lida duas vezes na jornada -> AlreadyInSession, nada muda")
    fun registerDuplicateSameSession() = runTest {
        val dao = mockk<DeliveryStopDao>()
        coEvery { dao.insertOrGetExisting(any()) } returns stop("MLB-1").toEntity()
        val result = DeliveryStopRepositoryImpl(dao).register(stop("MLB-1"))
        assertEquals(RegisterStopResult.AlreadyInSession, result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { dao.transferToSession(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("Parada de ontem lida hoje -> pede confirmação, não move sozinha")
    fun registerExistingFromOtherSession() = runTest {
        val dao = mockk<DeliveryStopDao>()
        coEvery { dao.insertOrGetExisting(any()) } returns stop("MLB-1", yesterday).toEntity()
        val result = DeliveryStopRepositoryImpl(dao).register(stop("MLB-1", today))
        val conflict = assertInstanceOf(RegisterStopResult.ExistsInOtherSession::class.java, result)
        assertEquals(yesterday, conflict.existing.sessionId)
        coVerify(exactly = 0) { dao.transferToSession(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("Transferência confirmada envia o ID original e status PENDING")
    fun confirmedTransfer() = runTest {
        val dao = mockk<DeliveryStopDao>()
        coEvery { dao.transferToSession(any(), any(), any(), any()) } returns 1
        val moved = DeliveryStopRepositoryImpl(dao).transferToSession(stop("MLB-1", yesterday).key, today)
        assertEquals(true, moved)
        coVerify { dao.transferToSession("MLB-1", "MERCADO_LIVRE", "2026-10-05", "PENDING") }
    }
}
