package com.otimiza.delivery.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class SessionHeaderInterceptor(
    private val sessionIdProvider: () -> String?,
    private val driverIdProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Client-App", "OtimizaAI-Android")

        sessionIdProvider()?.let { builder.addHeader("X-Session-ID", it) }
        driverIdProvider()?.let { builder.addHeader("X-Driver-ID", it) }

        return chain.proceed(builder.build())
    }
}
