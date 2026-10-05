package com.otimizaai.data.di

import com.otimizaai.data.repository.DeliveryStopRepositoryImpl
import com.otimizaai.domain.repository.DeliveryStopRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Liga o contrato do domínio (interface) à implementação com Room.
 * Quem pede um DeliveryStopRepository recebe um DeliveryStopRepositoryImpl
 * sem nunca saber que o Room existe.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeliveryStopRepository(
        impl: DeliveryStopRepositoryImpl,
    ): DeliveryStopRepository
}
