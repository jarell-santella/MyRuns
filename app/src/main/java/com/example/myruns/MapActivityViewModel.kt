package com.example.myruns

import android.content.ComponentName
import android.content.ServiceConnection
import android.icu.text.AlphabeticIndex.Record
import android.os.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class MapActivityViewModel : ViewModel(), ServiceConnection {
    private var serviceMessageHandler: ServiceMessageHandler
    private val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type

    // Bundle storing all of the information of the user's exercise
    private val _bundle = MutableLiveData<Bundle>().apply {
        value = getDefaultBundle()
    }
    val bundle: LiveData<Bundle> = _bundle

    // Bundle storing all of the information of the user's exercise
    private val _activityType = MutableLiveData<Int>().apply {
        value = -1
    }
    val activityType: LiveData<Int> = _activityType

    val isBind = MutableLiveData<Boolean>().apply {
        value = false
    }

    val mapInit = MutableLiveData<Boolean>().apply {
        value = false
    }

    val lastMarker = MutableLiveData<Marker?>().apply {
        value = null
    }

    init {
        // Set all values to the default values when MapActivity is made
        default()
        serviceMessageHandler = ServiceMessageHandler(Looper.getMainLooper())
    }

    fun default() {
        _bundle.value = getDefaultBundle()
        _activityType.value = -1

        isBind.value = false
        mapInit.value = false

        lastMarker.value = null
    }

    fun getDefaultBundle(): Bundle {
        val bundle = Bundle()
        bundle.putDouble(RecordingService.DURATION_KEY, 0.0)
        bundle.putDouble(RecordingService.DISTANCE_KEY, 0.0)
        bundle.putDouble(RecordingService.AVG_SPEED_KEY, 0.0)
        bundle.putDouble(RecordingService.CUR_SPEED_KEY, 0.0)
        bundle.putDouble(RecordingService.CLIMB_KEY, 0.0)
        bundle.putDouble(RecordingService.CALORIES_KEY, 0.0)
        bundle.putString(RecordingService.LOCATION_LIST_KEY, Gson().toJson(ArrayList<LatLng>(), type))
        return bundle
    }

    override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
        val tempBinder = iBinder as RecordingService.ServiceBinder
        tempBinder.setMsgHandler(serviceMessageHandler)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    inner class ServiceMessageHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == RecordingService.TRACKING_MSG_INT_VALUE) {
                // Get message related to tracking the user in their current exercise
                // Update user's location as well as several other statistics of the user's exercise
                _bundle.value = msg.data
            }
            else if (msg.what == RecordingService.CLASSIFYING_MSG_INT_VALUE) {
                // Get message related to classifying the user's current exercise
                _activityType.value = msg.data.getInt(RecordingService.AUTOMATIC_ACTIVITY_KEY, -1)
            }
        }
    }
}