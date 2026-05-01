package com.example.theglobalcarbonfootprintproject.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.theglobalcarbonfootprintproject.data.local.CarbonDatabase
import com.example.theglobalcarbonfootprintproject.data.local.dao.*
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

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `institution_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `city` TEXT NOT NULL, `state` TEXT NOT NULL, `gridFactor` REAL NOT NULL, `studentCount` INTEGER NOT NULL, `staffCount` INTEGER NOT NULL, `buildingFloors` INTEGER NOT NULL, `classroomCount` INTEGER NOT NULL, `classroomAC` TEXT NOT NULL, `labCount` INTEGER NOT NULL, `pcsPerLab` INTEGER NOT NULL, `labHoursPerDay` REAL NOT NULL, `hasServerRoom` INTEGER NOT NULL, `serverRoomSize` TEXT, `monthlyEnergyKwh` REAL NOT NULL, `solarCapacityKw` REAL NOT NULL, `generatorDieselLitresMonth` REAL NOT NULL, `studentCommuteSplitJson` TEXT NOT NULL, `avgCommuteKm` REAL NOT NULL, `institutionBusCount` INTEGER NOT NULL, `busFuelType` TEXT NOT NULL, `hasCanteen` INTEGER NOT NULL, `canteenFuel` TEXT NOT NULL, `lpgCylindersMonth` INTEGER NOT NULL, `dailyMealsServed` INTEGER NOT NULL, `paperReamsMonth` INTEGER NOT NULL, `annualEventsJson` TEXT NOT NULL, `departmentBreakdownJson` TEXT NOT NULL DEFAULT '[]', PRIMARY KEY(`id`))")
        }
    }


    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `carbon_logs` ADD COLUMN `wasteKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `carbon_logs` ADD COLUMN `eventKg` REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `fuelType` TEXT NOT NULL DEFAULT 'PETROL'")
            db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `kmPerDay` REAL NOT NULL DEFAULT 10.0")
            db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `mealsPerDay` INTEGER NOT NULL DEFAULT 3")
            db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `deviceCount` INTEGER NOT NULL DEFAULT 2")
            db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `streamingHeavy` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE `institution_profile` RENAME COLUMN `paperReavesMonth` TO `paperReamsMonth`")
            } catch (e: Exception) {}
            try {
                db.execSQL("ALTER TABLE `institution_profile` ADD COLUMN `departmentBreakdownJson` TEXT NOT NULL DEFAULT '[]'")
            } catch (e: Exception) {}
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_14_15)

        .fallbackToDestructiveMigration()
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

    @Provides
    fun provideDigitalDao(database: CarbonDatabase): DigitalLogDao {
        return database.digitalDao()
    }
}
