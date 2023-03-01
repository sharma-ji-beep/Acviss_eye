package com.example.myapplication.remote_db

import com.example.android.camerax.video.remote_db.RemoteApiInterface
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class RemoteApiClient {
    companion object {
        private var retrofit: Retrofit? = null

        private var okHttpClient: OkHttpClient? = null

        fun getClient(): RemoteApiInterface {
            if (okHttpClient == null) {
                initOkHttp()
            }
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient =  OkHttpClient().newBuilder().addInterceptor(interceptor).build()

            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(RemoteApiInterface.BASEURL)
                    .client(okHttpClient!!)
//                    .addConverterFactory(GsonConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create())) //to allow nulls
                    .build()
            }
            return retrofit!!.create(RemoteApiInterface::class.java)

        }

        private fun initOkHttp() {
            val httpClient = OkHttpClient().newBuilder()
//                .addNetworkInterceptor(StethoInterceptor())
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
            val interceptor = HttpLoggingInterceptor()
//            interceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(interceptor)
//            httpClient.addInterceptor(ConnectivityInterceptor(context))
            httpClient.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            okHttpClient = httpClient.build()
        }
    }
}