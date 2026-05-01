package com.example.theglobalcarbonfootprintproject.di

import com.example.theglobalcarbonfootprintproject.data.remote.MongoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MongoModule {
    
    // Point to the local backend for testing phase
    private const val BASE_URL = "http://192.168.1.107:3000/"




    @Provides
    @Singleton
    @Named("MongoOkHttpClient")
    fun provideMongoOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()


    }

    @Provides
    @Singleton
    @Named("MongoRetrofit")
    fun provideMongoRetrofit(@Named("MongoOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMongoApiService(@Named("MongoRetrofit") retrofit: Retrofit): MongoApiService {
        return retrofit.create(MongoApiService::class.java)
    }
}
