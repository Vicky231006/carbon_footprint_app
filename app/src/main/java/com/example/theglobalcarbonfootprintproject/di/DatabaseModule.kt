package com.example.theglobalcarbonfootprintproject.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.theglobalcarbonfootprintproject.data.local.CarbonDatabase
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.EnergyLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.FoodLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.TransportSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS transport_segments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    activityType INTEGER NOT NULL,
                    durationMinutes REAL NOT NULL,
                    estimatedKm REAL NOT NULL,
                    transportMode TEXT NOT NULL,
                    co2Kg REAL NOT NULL,
                    userVerified INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS food_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    mealType TEXT NOT NULL,
                    co2Kg REAL NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS energy_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    energyType TEXT NOT NULL,
                    value REAL NOT NULL,
                    co2Kg REAL NOT NULL
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CarbonDatabase {
        return Room.databaseBuilder(
            context,
            CarbonDatabase::class.java,
            "carbon_db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    fun provideCarbonDao(database: CarbonDatabase): CarbonDao {
        return database.carbonDao()
    }

    @Provides
    fun provideTransportDao(database: CarbonDatabase): TransportSegmentDao {
        return database.transportDao()
    }

    @Provides
    fun provideFoodDao(database: CarbonDatabase): FoodLogDao {
        return database.foodDao()
    }

    @Provides
    fun provideEnergyDao(database: CarbonDatabase): EnergyLogDao {
        return database.energyDao()
    }
}
