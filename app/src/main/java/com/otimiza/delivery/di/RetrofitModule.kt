package com.otimiza.delivery.di

import com.otimiza.delivery.data.remote.VrpApiService
import com.otimiza.delivery.data.remote.VrpEngineClient
import com.otimiza.delivery.data.remote.VrpEngineClientImpl
import com.otimiza.delivery.data.remote.interceptor.SessionHeaderInterceptor
import com.otimiza.delivery.data.remote.interceptor.TelemetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Singleton
    fun provideSessionIdProvider(): () -> String? = { null }

    @Provides
    @Singleton
    fun provideDriverIdProvider(): () -> String? = { null }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionIdProvider: () -> String?,
        driverIdProvider: () -> String?
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(SessionHeaderInterceptor(sessionIdProvider, driverIdProvider))
        .addInterceptor(TelemetryInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.otimizaai.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideVrpApiService(retrofit: Retrofit): VrpApiService =
        retrofit.create(VrpApiService::class.java)

    @Provides
    @Singleton
    fun provideVrpEngineClient(api: VrpApiService): VrpEngineClient =
        VrpEngineClientImpl(api, baseUrl = "https://api.otimizaai.com/v1")
}
