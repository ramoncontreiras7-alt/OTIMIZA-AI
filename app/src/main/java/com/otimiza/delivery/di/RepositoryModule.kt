package com.otimiza.delivery.di

import com.otimiza.delivery.data.local.dao.DeliveryStopDao
import com.otimiza.delivery.data.remote.VrpEngineClient
import com.otimiza.delivery.data.repository.DeliveryRepositoryImpl
import com.otimiza.delivery.domain.repository.DeliveryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDeliveryRepository(
        dao: DeliveryStopDao,
        vrpEngineClient: VrpEngineClient
    ): DeliveryRepository = DeliveryRepositoryImpl(dao, vrpEngineClient)
}
