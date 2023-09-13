package com.example.myruns

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "exercise_entry_table")
data class ExerciseEntry (
    @PrimaryKey(autoGenerate = true) var id: Long = 0L,
    @ColumnInfo(name = "input_type") var inputType: Int = -1,
    @ColumnInfo(name = "activity_type") var activityType: Int = -1,
    @ColumnInfo(name = "date_time") var dateTime: String = "",
    @ColumnInfo(name = "duration") var duration: Double = 0.0, // seconds
    @ColumnInfo(name = "distance") var distance: Double = 0.0, // stored in meters
    @ColumnInfo(name = "avg_pace") var avgPace: Double = 0.0,
    @ColumnInfo(name = "avg_speed") var avgSpeed: Double = 0.0, // stored in meters per second
    @ColumnInfo(name = "calorie") var calorie: Double = 0.0,
    @ColumnInfo(name = "climb") var climb: Double = 0.0, // stored in meters
    @ColumnInfo(name = "heart_rate") var heartRate: Double = 0.0, // beats per minute
    @ColumnInfo(name = "comment") var comment: String = "",
    @ColumnInfo(name = "location_list") var locationList: ArrayList<LatLng> = ArrayList()
)