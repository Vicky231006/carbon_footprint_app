package com.example.theglobalcarbonfootprintproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.EnergyLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.FoodLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.TransportSegmentDao
import com.example.theglobalcarbonfootprintproject.data.local.entities.*

@Database(
    entities = [
        UserProfile::class,
        CarbonLog::class,
        RewardRecord::class,
        TransportSegment::class,
        FoodLog::class,
        EnergyLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CarbonDatabase : RoomDatabase() {
    abstract fun carbonDao(): CarbonDao
    abstract fun transportDao(): TransportSegmentDao
    abstract fun foodDao(): FoodLogDao
    abstract fun energyDao(): EnergyLogDao

    companion object {
        @Volatile
        private var INSTANCE: CarbonDatabase? = null

        fun getDatabase(context: Context): CarbonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonDatabase::class.java,
                    "carbon_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
