package com.example.myruns

import android.icu.util.Calendar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ManualActivityViewModel() : ViewModel() {

    var calendar = MutableLiveData<Calendar>().apply {
        value = Calendar.getInstance()
    }
    var duration = MutableLiveData<Double>().apply {
        value = 0.0
    }
    var distance = MutableLiveData<Double>().apply {
        value = 0.0
    }
    var calories = MutableLiveData<Double>().apply {
        value = 0.0
    }
    var heartRate = MutableLiveData<Double>().apply {
        value = 0.0
    }
    var comment = MutableLiveData<String>().apply {
        value = ""
    }

    init {
        default()
    }

    fun default() {
        defaultCalendar()
        defaultDuration()
        defaultDistance()
        defaultCalories()
        defaultHeartRate()
        defaultComment()
    }

    fun defaultCalendar() {
        calendar.value = Calendar.getInstance()
    }

    fun defaultDuration() {
        duration.value = 0.0
    }

    fun defaultDistance() {
        distance.value = 0.0
    }

    fun defaultCalories() {
        calories.value = 0.0
    }

    fun defaultHeartRate() {
        heartRate.value = 0.0
    }

    fun defaultComment() {
        comment.value = ""
    }
}