package com.otimizaai.app.di

import com.otimizaai.domain.usecase.CalculateCostPlanUseCase
import com.otimizaai.domain.usecase.CalculateRouteProfitUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Ensina o Hilt a criar as regras de negócio que vêm do módulo domain (Kotlin puro). */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCalculateRouteProfit(): CalculateRouteProfitUseCase = CalculateRouteProfitUseCase()

    @Provides
    fun provideCalculateCostPlan(): CalculateCostPlanUseCase = CalculateCostPlanUseCase()
}
