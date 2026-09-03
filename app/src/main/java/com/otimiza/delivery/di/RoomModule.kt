package com.otimiza.delivery.di

import android.content.Context
import androidx.room.Room
import com.otimiza.delivery.data.local.AppDatabase
import com.otimiza.delivery.data.local.dao.DeliveryStopDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "otimizaai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDeliveryStopDao(db: AppDatabase): DeliveryStopDao = db.deliveryStopDao()
}
