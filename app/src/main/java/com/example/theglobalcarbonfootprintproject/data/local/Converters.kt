package com.example.theglobalcarbonfootprintproject.data.local

import androidx.room.TypeConverter
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.AnnualEvent
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromCommuteSplitMap(value: Map<TransportMode, Int>): String {
        val mapStringKeys = value.mapKeys { it.key.name }
        return gson.toJson(mapStringKeys)
    }

    @TypeConverter
    fun toCommuteSplitMap(value: String): Map<TransportMode, Int> {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val mapStringKeys: Map<String, Int> = gson.fromJson(value, type)
        return mapStringKeys.mapKeys { TransportMode.valueOf(it.key) }
    }

    @TypeConverter
    fun fromAnnualEventList(value: List<AnnualEvent>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAnnualEventList(value: String): List<AnnualEvent> {
        val type = object : TypeToken<List<AnnualEvent>>() {}.type
        return gson.fromJson(value, type)
    }
}
