package com.example.myruns

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ArrayListJsonStringConverters {
    // Convert from JSON string to ArrayList<LatLng>
    @TypeConverter
    fun jsonStringToArrayList(jsonString: String): ArrayList<LatLng> {
        val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type
        return Gson().fromJson(jsonString, type)
    }

    // Convert from ArrayList<LatLng> to JSON string
    @TypeConverter
    fun arrayListToJsonString(arrayList: ArrayList<LatLng>): String {
        val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type
        return Gson().toJson(arrayList, type)
    }
}