package com.example.theglobalcarbonfootprintproject

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.Test
import java.util.concurrent.TimeUnit

class MongoDbConnectionTest {

    @Test
    fun testMongoConnection() = runBlocking {
        // The URI from your local.properties
        val uri = "mongodb://bickyboi09_db_user:Fd2XtYecyYKrzj0c@ac-oachdew-shard-00-00.3cm3wfw.mongodb.net:27017,ac-oachdew-shard-00-01.3cm3wfw.mongodb.net:27017,ac-oachdew-shard-00-02.3cm3wfw.mongodb.net:27017/?ssl=true&replicaSet=atlas-27l6y6-shard-0&authSource=admin&retryWrites=true&w=majority&appName=Cluster0"
        
        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri))
            .serverApi(serverApi)
            // Timeout settings to fail fast if it can't connect
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS)
            }
            .build()

        var client: MongoClient? = null
        try {
            println("Attempting to connect to MongoDB Atlas...")
            client = MongoClient.create(settings)
            
            // Try to access the specific database from your screenshot
            val database = client.getDatabase("global_carbon_footprint_project")
            val collection = database.getCollection<Document>("test_connection")
            
            println("Successfully connected to database!")
            println("Attempting to insert a test document...")
            
            val doc = Document("test", "Connection successful")
                .append("timestamp", System.currentTimeMillis())
            
            val result = collection.insertOne(doc)
            println("Insert successful! Inserted ID: ${result.insertedId}")
            
            println("Attempting to find the document...")
            val found = collection.find().firstOrNull()
            println("Found document: $found")
            
        } catch (e: Exception) {
            System.err.println("Failed to connect or perform operations on MongoDB!")
            e.printStackTrace()
            throw e
        } finally {
            client?.close()
        }
    }
}
