package com.example.theglobalcarbonfootprintproject.di

import com.example.theglobalcarbonfootprintproject.data.remote.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/api/") // 10.0.2.2 is localhost for Android Emulator
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

/*
    @Provides
    @Singleton
    @javax.inject.Named("AtlasRetrofit")
    fun provideAtlasRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://data.mongodb-api.com/app/${com.example.theglobalcarbonfootprintproject.BuildConfig.ATLAS_APP_ID}/endpoint/data/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideAtlasDataApi(@javax.inject.Named("AtlasRetrofit") retrofit: Retrofit): com.example.theglobalcarbonfootprintproject.data.remote.AtlasDataApi {
        return retrofit.create(com.example.theglobalcarbonfootprintproject.data.remote.AtlasDataApi::class.java)
    }
*/
}
