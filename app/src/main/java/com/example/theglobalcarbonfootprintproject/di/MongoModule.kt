package com.example.theglobalcarbonfootprintproject.di

import android.util.Log
import com.example.theglobalcarbonfootprintproject.BuildConfig
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.codecs.configuration.CodecRegistry
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MongoModule {

    @Provides
    @Singleton
    fun provideMongoClient(): MongoClient {
        val connectionString = ConnectionString(BuildConfig.MONGODB_URI)
        
        val pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            // Removed .serverApi(serverApi) to reduce handshake overhead
            .codecRegistry(pojoCodecRegistry)
            .applyToSocketSettings { builder ->
                builder.connectTimeout(90, TimeUnit.SECONDS)
                builder.readTimeout(90, TimeUnit.SECONDS)
            }
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(90, TimeUnit.SECONDS)
            }
            .applyToSslSettings { builder ->
                builder.enabled(true)
                // Critical for some mobile networks
                builder.invalidHostNameAllowed(true)
            }
            .build()

        Log.d("MongoModule", "Creating MongoClient with URI: ${connectionString.toString().take(25)}...")
        Log.d("MongoModule", "SSL Enabled: ${settings.sslSettings.isEnabled}")
        return MongoClient.create(settings)
    }

    @Provides
    @Singleton
    fun provideMongoDatabase(mongoClient: MongoClient): MongoDatabase {
        // Changed database name to verify if a new one is created
        return mongoClient.getDatabase("debug_connection_db")
    }
}
