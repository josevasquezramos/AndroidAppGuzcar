package com.guzcar.app.data.api

import com.guzcar.app.data.api.TokenManager.token
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // private const val BASE_URL = "https://automotores-guzcar.com/api/"
    private const val BASE_URL = "http://192.168.0.9:8000/api/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        TokenManager.token?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        builder.addHeader("Accept", "application/json")
        builder.addHeader("Content-Type", "application/json")
        builder.addHeader("X-Requested-With", "XMLHttpRequest")

        chain.proceed(builder.build())
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}
