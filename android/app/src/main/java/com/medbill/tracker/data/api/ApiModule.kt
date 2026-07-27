package com.medbill.tracker.data.api

import android.os.Build
import com.medbill.tracker.BuildConfig
import com.medbill.tracker.auth.SessionStore
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiModule {
    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        return fingerprint.contains("generic") || model.contains("emulator") || model.contains("sdk")
    }

    private fun resolvedBaseUrl(): String {
        val base = BuildConfig.BASE_URL
        return if (!isEmulator() && base.startsWith("http://10.0.2.2:")) {
            // Physical devices cannot reach emulator loopback. Use adb reverse target instead.
            base.replace("http://10.0.2.2:", "http://127.0.0.1:")
        } else {
            base
        }
    }

    fun createApi(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val token = SessionStore.accessToken.value
            val updated = if (token.isNullOrBlank()) {
                request
            } else {
                request.newBuilder().addHeader("Authorization", "Bearer $token").build()
            }
            chain.proceed(updated)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(resolvedBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
