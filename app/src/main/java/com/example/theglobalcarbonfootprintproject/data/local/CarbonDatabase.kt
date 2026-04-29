package com.example.theglobalcarbonfootprintproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.theglobalcarbonfootprintproject.data.local.dao.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.*

@Database(
    entities = [
        UserProfile::class,
        InstitutionProfile::class,
        CarbonLog::class,
        RewardRecord::class,
        TransportSegment::class,
        FoodLog::class,
        EnergyLog::class,
        DigitalLog::class
    ],
    version = 11,

    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CarbonDatabase : RoomDatabase() {
    abstract fun carbonDao(): CarbonDao
    abstract fun transportDao(): TransportSegmentDao
    abstract fun foodDao(): FoodLogDao
    abstract fun energyDao(): EnergyLogDao
    abstract fun digitalDao(): DigitalLogDao

    companion object {
        @Volatile
        private var INSTANCE: CarbonDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
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
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                CREATE TABLE IF NOT EXISTS food_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date INTEGER NOT NULL,
                    mealType TEXT NOT NULL,
                    co2Kg REAL NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
                database.execSQL("""
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
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `institution_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `city` TEXT NOT NULL, `state` TEXT NOT NULL, `gridFactor` REAL NOT NULL, `studentCount` INTEGER NOT NULL, `staffCount` INTEGER NOT NULL, `buildingFloors` INTEGER NOT NULL, `classroomCount` INTEGER NOT NULL, `classroomAC` TEXT NOT NULL, `labCount` INTEGER NOT NULL, `pcsPerLab` INTEGER NOT NULL, `labHoursPerDay` REAL NOT NULL, `hasServerRoom` INTEGER NOT NULL, `serverRoomSize` TEXT, `monthlyEnergyKwh` REAL NOT NULL, `solarCapacityKw` REAL NOT NULL, `generatorDieselLitresMonth` REAL NOT NULL, `studentCommuteSplitJson` TEXT NOT NULL, `avgCommuteKm` REAL NOT NULL, `institutionBusCount` INTEGER NOT NULL, `busFuelType` TEXT NOT NULL, `hasCanteen` INTEGER NOT NULL, `canteenFuel` TEXT NOT NULL, `lpgCylindersMonth` INTEGER NOT NULL, `dailyMealsServed` INTEGER NOT NULL, `paperReavesMonth` INTEGER NOT NULL, `annualEventsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `carbon_logs` ADD COLUMN `wasteKg` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE `carbon_logs` ADD COLUMN `eventKg` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `fuelType` TEXT NOT NULL DEFAULT 'PETROL'")
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `kmPerDay` REAL NOT NULL DEFAULT 10.0")
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `mealsPerDay` INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `deviceCount` INTEGER NOT NULL DEFAULT 2")
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `streamingHeavy` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): CarbonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonDatabase::class.java,
                    "carbon_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
