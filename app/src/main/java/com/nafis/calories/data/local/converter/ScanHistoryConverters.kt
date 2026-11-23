package com.nafis.calories.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nafis.calories.data.local.entity.IngredientSnapshot

/**
 * Room TypeConverters for ScanHistoryEntity.
 * Handles JSON serialization/deserialization of complex types.
 */
class ScanHistoryConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromIngredientsList(ingredients: List<IngredientSnapshot>): String {
        return gson.toJson(ingredients)
    }

    @TypeConverter
    fun toIngredientsList(json: String): List<IngredientSnapshot> {
        val type = object : TypeToken<List<IngredientSnapshot>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}