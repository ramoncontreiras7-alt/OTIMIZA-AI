package com.otimiza.delivery.data.remote.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class TelemetryInterceptor : Interceptor {

    companion object {
        private const val TAG = "NetworkTelemetry"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            Log.d(TAG, "${request.method} ${request.url} -> ${response.code} (${ms}ms)")
            return response
        } catch (e: Exception) {
            val ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            Log.w(TAG, "${request.method} ${request.url} falhou em ${ms}ms: ${e.message}")
            throw e
        }
    }
}
