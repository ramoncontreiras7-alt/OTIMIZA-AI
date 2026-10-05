package com.otimizaai.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.otimizaai.data.local.OtimizaDatabase
import com.otimizaai.data.local.entity.DeliveryStopEntity
import com.otimizaai.data.repository.DeliveryStopRepositoryImpl
import com.otimizaai.data.repository.RouteRepositoryImpl
import com.otimizaai.domain.model.RouteSessionId
import com.otimizaai.domain.model.StopKey
import com.otimizaai.domain.model.NativeStopId
import com.otimizaai.domain.model.PlatformId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Simula o celular do entregador: banco da versão 1 (APK anterior) com paradas,
 * depois abre com o app novo (versão 2). Nada pode se perder e o ID não pode mudar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath("migracao-teste.db")
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
    }

    @After
    fun tearDown() {
        dbFile.delete()
    }

    /** Cria o banco exatamente como a versão 1 do app criava. */
    private fun createVersion1WithStops() {
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `delivery_stops` (`native_stop_id` TEXT NOT NULL, `platform_id` TEXT NOT NULL, " +
                "`session_id` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, `address_formatted` TEXT NOT NULL, " +
                "`address_complement` TEXT, `address_neighborhood` TEXT, `address_city` TEXT, `address_postal_code` TEXT, " +
                "`status` TEXT NOT NULL, `freight_cents` INTEGER NOT NULL, PRIMARY KEY(`native_stop_id`, `platform_id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_delivery_stops_session_id_status` ON `delivery_stops` (`session_id`, `status`)")
        db.execSQL(
            "INSERT INTO delivery_stops VALUES ('mlb-0004419027731/Ç#a', 'MERCADO_LIVRE', '2026-10-05', NULL, NULL, " +
                "'R. Barão de Aracati, 210', NULL, NULL, NULL, NULL, 'PENDING', 1150)"
        )
        db.execSQL(
            "INSERT INTO delivery_stops VALUES ('IFD-2', 'IFOOD', '2026-10-05', -3.73, -38.52, " +
                "'Av. Santos Dumont, 1500', NULL, NULL, NULL, NULL, 'DELIVERED', 850)"
        )
        db.execSQL(
            "INSERT INTO delivery_stops VALUES ('TBA-3', 'AMAZON_FLEX', '2026-10-04', NULL, NULL, " +
                "'R. Tibúrcio Cavalcante, 77', NULL, NULL, NULL, NULL, 'FAILED', 0)"
        )
        db.version = 1
        db.close()
    }

    private fun openVersion2(): OtimizaDatabase =
        Room.databaseBuilder(context, OtimizaDatabase::class.java, dbFile.absolutePath)
            .addMigrations(OtimizaDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

    @Test
    fun migration_keepsAllStopsAndIdsAndCreatesRoutes() = runBlocking {
        createVersion1WithStops()
        val db = openVersion2()
        try {
            val stops = db.deliveryStopDao().findBySession("2026-10-05")
            assertEquals(2, stops.size)
            // ID preservado byte a byte
            assertEquals("mlb-0004419027731/Ç#a", stops[0].nativeStopId)
            assertEquals("IFD-2", stops[1].nativeStopId)
            // ordem de cadastro mantida
            assertEquals(listOf(1, 2), stops.map { it.routeOrder })

            // uma rota criada para cada jornada antiga
            val routes = RouteRepositoryImpl(db.routeDao()).observeAll().first()
            assertEquals(setOf("2026-10-05", "2026-10-04"), routes.map { it.route.id.value }.toSet())
            val today = routes.first { it.route.id.value == "2026-10-05" }
            assertEquals(2, today.totalStops)
            assertEquals(1, today.pendingStops)
        } finally {
            db.close()
        }
    }

    @Test
    fun newFeatures_reorderDeleteAndBringPending() = runBlocking {
        createVersion1WithStops()
        val db = openVersion2()
        try {
            val repo = DeliveryStopRepositoryImpl(db.deliveryStopDao())
            val routes = RouteRepositoryImpl(db.routeDao())
            val today = RouteSessionId("2026-10-05")

            // Reordenar: a segunda parada passa a ser a primeira
            val keys = repo.findBySession(today).map { it.key }
            repo.reorder(today, keys.reversed())
            assertEquals(listOf("IFD-2", "mlb-0004419027731/Ç#a"), repo.findBySession(today).map { it.id.value })

            // Nova rota trazendo as pendentes de outras rotas (só a do Mercado Livre está pendente)
            val nova = routes.create("Rota 2", java.time.LocalDate.of(2026, 10, 5))
            val moved = repo.transferPendingFromOtherSessions(nova.id)
            assertEquals(1, moved)
            val inNew = repo.findBySession(nova.id)
            assertEquals("mlb-0004419027731/Ç#a", inNew.single().id.value)

            // Remover uma parada
            repo.delete(StopKey(NativeStopId("IFD-2"), PlatformId("IFOOD")))
            assertEquals(0, repo.findBySession(today).size)

            // Apagar rota apaga as paradas dela
            routes.delete(nova.id)
            assertEquals(0, repo.findBySession(nova.id).size)
            assertEquals(null, routes.findById(nova.id))
        } finally {
            db.close()
        }
    }

    @Test
    fun freshInstall_opensVersion2Directly() = runBlocking {
        val db = Room.databaseBuilder(context, OtimizaDatabase::class.java, dbFile.absolutePath)
            .addMigrations(OtimizaDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val route = RouteRepositoryImpl(db.routeDao()).create("segunda-feira", java.time.LocalDate.of(2026, 10, 5))
            db.deliveryStopDao().insertOrGetExisting(
                DeliveryStopEntity("X-1", "IFOOD", route.id.value, null, null, "Rua A, 1", null, null, null, null, "PENDING", 500),
            )
            assertEquals(1, db.deliveryStopDao().findBySession(route.id.value).single().routeOrder)
        } finally {
            db.close()
        }
    }
}
