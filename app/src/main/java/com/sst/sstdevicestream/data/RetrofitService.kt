package com.sst.sstdevicestream.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object RetrofitService {

    private fun okHttpClient(
        interceptors: List<Interceptor>,
    ): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .apply {
            interceptors.forEach { addInterceptor(it) }
        }
        .addNetworkInterceptor(HttpLoggingInterceptor { message ->
            println("SST Device Stream: $message")
        }.apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        })
        .build()

    private fun service(
        baseUrl: String,
        interceptors: List<Interceptor> = listOf()
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient(interceptors))
        .build()


    inline fun <reified T> getInstance(
        baseUrl: String,
        interceptors: List<Interceptor> = listOf()
    ): T = service(baseUrl, interceptors).create(T::class.java)
}