package com.otimizaai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.otimizaai.data.local.dao.DeliveryStopDao
import com.otimizaai.data.local.dao.RouteDao
import com.otimizaai.data.local.entity.DeliveryStopEntity
import com.otimizaai.data.local.entity.RouteEntity

/**
 * O banco local do app. Cada nova tabela entra na lista `entities`.
 *
 * Versão 2: tabela `routes` (várias rotas por dia) e coluna `route_order`
 * (ordem das paradas). A migração abaixo preserva tudo o que já foi cadastrado.
 */
@Database(
    entities = [DeliveryStopEntity::class, RouteEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class OtimizaDatabase : RoomDatabase() {

    abstract fun deliveryStopDao(): DeliveryStopDao
    abstract fun routeDao(): RouteDao

    companion object {
        const val NAME = "otimiza_ai.db"

        /**
         * 1 -> 2:
         *  1. cria a tabela de rotas;
         *  2. cria uma rota para cada jornada que já existia (o id antigo era a data);
         *  3. adiciona a coluna de ordem e mantém a ordem de cadastro.
         * Nenhuma parada é apagada e nenhum ID é alterado.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`date` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO routes (id, name, date, created_at) " +
                        "SELECT DISTINCT session_id, 'Rota de ' || session_id, substr(session_id, 1, 10), 0 FROM delivery_stops"
                )
                db.execSQL("ALTER TABLE delivery_stops ADD COLUMN `route_order` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE delivery_stops SET route_order = rowid")
            }
        }
    }
}
