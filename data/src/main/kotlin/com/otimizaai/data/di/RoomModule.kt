package com.otimizaai.data.di

import android.content.Context
import androidx.room.Room
import com.otimizaai.data.local.OtimizaDatabase
import com.otimizaai.data.local.dao.DeliveryStopDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Ensina o Hilt a construir o banco e o DAO.
 * O banco é @Singleton: uma única instância durante toda a vida do app.
 */
@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OtimizaDatabase =
        Room.databaseBuilder(context, OtimizaDatabase::class.java, OtimizaDatabase.NAME)
            .build()

    @Provides
    fun provideDeliveryStopDao(database: OtimizaDatabase): DeliveryStopDao =
        database.deliveryStopDao()
}
