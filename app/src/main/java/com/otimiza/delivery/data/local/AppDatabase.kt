package com.otimiza.delivery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.otimiza.delivery.data.local.dao.DeliveryStopDao
import com.otimiza.delivery.data.local.entity.DeliveryStopEntity

@Database(entities = [DeliveryStopEntity::class], version = 1, exportSchema = true)
@TypeConverters(PlatformConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryStopDao(): DeliveryStopDao
}
