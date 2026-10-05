package com.otimizaai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.otimizaai.data.local.dao.DeliveryStopDao
import com.otimizaai.data.local.entity.DeliveryStopEntity

/**
 * O banco local do app. Cada nova tabela entra na lista `entities`.
 *
 * exportSchema = true grava a "planta" do banco em data/schemas/. Quando o
 * banco mudar numa versão futura, essa planta permite migrar os dados dos
 * entregadores sem apagar nada.
 */
@Database(
    entities = [DeliveryStopEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class OtimizaDatabase : RoomDatabase() {

    abstract fun deliveryStopDao(): DeliveryStopDao

    companion object {
        const val NAME = "otimiza_ai.db"
    }
}
