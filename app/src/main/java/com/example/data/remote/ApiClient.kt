package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    initialBaseUrl: String = "http://10.0.2.2:8000/",
    private var apiToken: String? = null
) {
    var baseUrl: String = sanitizeUrl(initialBaseUrl)
        set(value) {
            field = sanitizeUrl(value)
            rebuildRetrofit()
        }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        val token = apiToken
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private var retrofit: Retrofit = buildRetrofit()
    var apiService: SonoraApiService = retrofit.create(SonoraApiService::class.java)
        private set

    fun updateToken(token: String?) {
        this.apiToken = token
    }

    private fun rebuildRetrofit() {
        retrofit = buildRetrofit()
        apiService = retrofit.create(SonoraApiService::class.java)
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    companion object {
        fun sanitizeUrl(url: String): String {
            var trimmed = url.trim()
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "https://$trimmed"
            }
            if (!trimmed.endsWith("/")) {
                trimmed = "$trimmed/"
            }
            return trimmed
        }
    }
}
