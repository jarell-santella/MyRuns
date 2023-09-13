package com.example.myruns

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import kotlin.properties.Delegates

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var gMap: GoogleMap
    private lateinit var markerOptions: MarkerOptions
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var polylines: ArrayList<Polyline>
    private val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type
    private val defaultLocations = Gson().toJson(ArrayList<LatLng>(), type)
    private lateinit var locationsJson: String
    private var locations = ArrayList<LatLng>()

    private var PERMISSION_REQUEST_CODE by Delegates.notNull<Int>()

    private lateinit var mapActivityViewModel: MapActivityViewModel

    private lateinit var serviceIntent: Intent

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var unitsPreference: String

    private lateinit var typeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var curSpeedTextView: TextView
    private lateinit var climbTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var distanceTextView: TextView

    private lateinit var loadingTextView: TextView

    private lateinit var saveButton: Button

    private var inputType by Delegates.notNull<Int>()
    private var activityType by Delegates.notNull<Int>()
    private var avgSpeed by Delegates.notNull<Double>()
    private var curSpeed by Delegates.notNull<Double>()
    private var climb by Delegates.notNull<Double>()
    private var calories by Delegates.notNull<Double>()
    private var distance by Delegates.notNull<Double>()
    private var duration by Delegates.notNull<Double>()

    private lateinit var calendar: Calendar
    private lateinit var dateTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapActivityViewModel = ViewModelProvider(this).get(MapActivityViewModel::class.java)
        mapActivityViewModel.default()

        locationsJson = defaultLocations

        serviceIntent = Intent(this, RecordingService::class.java)

        calendar = Calendar.getInstance()

        PERMISSION_REQUEST_CODE = resources.getInteger(R.integer.permission_request_code)

        sharedPreferences = getSharedPreferences(getString(R.string.settings_key), Context.MODE_PRIVATE)
        unitsPreference = sharedPreferences.getString(getString(R.string.units_key), getString(R.string.units_default))!!

        typeTextView = findViewById(R.id.typeTextView)
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView)
        curSpeedTextView = findViewById(R.id.curSpeedTextView)
        climbTextView = findViewById(R.id.climbTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        loadingTextView = findViewById(R.id.loadingTextView)

        inputType = intent.extras!!.getInt(getString(R.string.input_key), -1)
        activityType = intent.extras!!.getInt(getString(R.string.activity_key), -1)

        if (inputType == 2) {

            // Update activity with the activity type data that the classifier provides
            mapActivityViewModel.activityType.observe(this) {
                activityType = it

                // Set activity type as the classified activity type specified by the classifier
                val activityName = when (it) {
                    0 -> "Running"
                    1 -> "Walking"
                    2 -> "Standing"
                    else -> {
                        if (mapActivityViewModel.mapInit.value!!) {
                            "Other"
                        }
                        else {
                            "loading"
                        }
                    }
                }
                typeTextView.text = String.format("Type: %s", activityName)
            }
        }
        else {
            // Set activity type as the actual activity type specified by the user
            val activityName = when (activityType) {
                0 -> "Running"
                1 -> "Walking"
                2 -> "Standing"
                3 -> "Cycling"
                4 -> "Hiking"
                5 -> "Downhill Skiing"
                6 -> "Cross-Country Skiing"
                7 -> "Snowboarding"
                8 -> "Skating"
                9 -> "Swimming"
                10 -> "Mountain Biking"
                11 -> "Wheelchair"
                12 -> "Elliptical"
                else -> "Other"
            }
            typeTextView.text = String.format("Type: %s", activityName)
        }
        avgSpeedTextView.text = getString(R.string.avg_speed_loading)
        curSpeedTextView.text = getString(R.string.cur_speed_loading)
        climbTextView.text = getString(R.string.climb_loading)
        caloriesTextView.text = getString(R.string.calories_loading)
        distanceTextView.text = getString(R.string.distance_loading)

        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener() {
            // Save button only actually saves once the map has finished loading
            if (mapActivityViewModel.mapInit.value!!) {
                // Get starting time and save it in string format
                dateTime = SimpleDateFormat("HH:mm:ss MM/dd/yyyy").format(calendar.time)
                onClickSave(inputType, activityType, dateTime)
            }
            else {
                val toast = Toast.makeText(this, getString(R.string.map_not_finished_loading), Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        markerOptions = MarkerOptions()
        polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)
        polylines = ArrayList()

        checkPermission()
        mapActivityViewModel.bundle.observe(this) {
            updateMap(it)
        }
    }

    // Update map every time the bundle with exercise entry data in the ViewModel is updated
    fun updateMap(bundle: Bundle) {
        locationsJson = bundle.getString(getString(R.string.location_list_key), defaultLocations)
        locations = if (locationsJson != defaultLocations) {
            Gson().fromJson(locationsJson, type)
        }
        else {
            ArrayList()
        }

        if (locations.isEmpty()) {
            markerOptions = MarkerOptions()
            markerOptions.position(LatLng(0.0, 0.0))
            markerOptions.title("Placeholder Position")
            gMap.addMarker(markerOptions)
        }
        else {
            // Draw a line between all "adjacent" locations recorded. This traces where the user has been in this exercise entry
            polylineOptions = PolylineOptions()
            polylineOptions.addAll(locations)
            gMap.addPolyline(polylineOptions)

            val lastLocation = locations.last()

            // Find if map contains the last location
            val mapCentered = if (mapActivityViewModel.isBind.value!!) {
                gMap.projection.visibleRegion.latLngBounds.contains(lastLocation)
            }
            else {
                false
            }
            if (!mapCentered) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocation, 17f)
                gMap.animateCamera(cameraUpdate)
            }

            // Put marker in first position and remove the "LOADING" text from screen
            if (!mapActivityViewModel.mapInit.value!!) {
                loadingTextView.visibility = View.GONE

                if (inputType == 2) {
                    typeTextView.text = resources.getString(R.string.type_unknown)
                }

                markerOptions = MarkerOptions()
                markerOptions.position(locations.first())
                markerOptions.title("Starting Position")
                gMap.addMarker(markerOptions)

                calendar = Calendar.getInstance()

                mapActivityViewModel.mapInit.value = true
            }

            // Get statistics for this exercise entry every time the bundle with exercise entry data in the ViewModel is updated
            avgSpeed = bundle.getDouble(getString(R.string.avg_speed_key), 0.0)
            curSpeed = bundle.getDouble(getString(R.string.cur_speed_key), 0.0)
            climb = bundle.getDouble(getString(R.string.climb_key), 0.0)
            calories = bundle.getDouble(getString(R.string.calories_key), 0.0)
            distance = bundle.getDouble(getString(R.string.distance_key), 0.0)
            duration = bundle.getDouble(getString(R.string.duration_key), 0.0)
            updateStats(avgSpeed, curSpeed, climb, calories, distance)

            // If it exists, remove the last marker put down
            if (mapActivityViewModel.lastMarker.value != null) {
                mapActivityViewModel.lastMarker.value!!.remove()
            }

            // Put marker in last position (marker follows the user)
            markerOptions = MarkerOptions()
            markerOptions.position(lastLocation)
            markerOptions.title("Last Position")
            mapActivityViewModel.lastMarker.value = gMap.addMarker(markerOptions)
        }
    }

    // Update the TextViews displaying statistics for this exercise entry on screen
    fun updateStats(avgSpeed: Double, curSpeed: Double, climb: Double, calories: Double, distance: Double) {
        when (unitsPreference) {
            "Kilometers" -> {
                avgSpeedTextView.text = String.format("Average speed: %.2f km/h", avgSpeed * 3.6)
                curSpeedTextView.text = String.format("Current speed: %.2f km/h", curSpeed * 3.6)
                climbTextView.text = String.format("Climb: %.2f kilometers", climb / 1000)
                distanceTextView.text = String.format("Distance: %.2f kilometers", distance / 1000)
            }
            "Miles" -> {
                avgSpeedTextView.text = String.format("Average speed: %.2f mi/h", avgSpeed * 2.2369362921)
                curSpeedTextView.text = String.format("Current speed: %.2f mi/h", curSpeed * 2.2369362921)
                climbTextView.text = String.format("Climb: %.2f miles", climb / 1609.344)
                distanceTextView.text = String.format("Distance: %.2f miles", distance / 1609.344)
            }
        }
        caloriesTextView.text = String.format("Calories: %.2f cal(s)", calories)
    }

    // Start service in foreground
    fun startRecordingService() {
        try {
            // Give the service which kind of input type it is to determine whether the service needs to perform activity type classification
            serviceIntent.putExtra(getString(R.string.input_key), inputType)
            applicationContext.startForegroundService(serviceIntent)
            if (!mapActivityViewModel.isBind.value!!) {
                applicationContext.bindService(serviceIntent, mapActivityViewModel, Context.BIND_AUTO_CREATE)
                mapActivityViewModel.isBind.value = true
            }
        }
        catch (e: SecurityException) {
        }
    }

    // Tell StartFragment that the result of this activity is canceled (don't add ExerciseEntry to database and discard all information in the dialog fields)
    fun onClickCancel(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    // Tell StartFragment that the result of this activity is ok. Pass ExerciseEntry attributes through intent and add ExerciseEntry to database
    fun onClickSave(inputType: Int, activityType: Int, dateTime: String) {
        val bundle = Bundle()
        bundle.putInt(getString(R.string.input_key), inputType)
        bundle.putInt(getString(R.string.activity_key), activityType)
        bundle.putString(getString(R.string.datetime_key), dateTime)
        bundle.putDouble(getString(R.string.duration_key), duration)
        bundle.putDouble(getString(R.string.distance_key), distance)
        bundle.putDouble(getString(R.string.avg_pace_key), 0.0)
        bundle.putDouble(getString(R.string.avg_speed_key), avgSpeed)
        bundle.putDouble(getString(R.string.calories_key), calories)
        bundle.putDouble(getString(R.string.climb_key), climb)
        bundle.putDouble(getString(R.string.heart_rate_key), 0.0)
        bundle.putString(getString(R.string.comment_key), "")
        bundle.putString(getString(R.string.location_list_key), locationsJson)

        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)

        finish()
    }

    // Make sure user allows location permission (important: this permission can be very invasive to user's privacy)
    fun checkPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }
        else {
            // If user already gives permission, start tracking location
            startRecordingService()
        }
    }

    // Check if user gives per mission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If user gives permission, start tracking permission
                startRecordingService()
            }
            else {
                // If user doesn't give permission, ask for permission again
                val toast = Toast.makeText(this, getString(R.string.toast_permissions_declined), Toast.LENGTH_SHORT)
                toast.show()
                checkPermission()
            }
        }
    }

    // Handles edge case for when service is killed on swipe up rather than with the save or cancel buttons
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && mapActivityViewModel.isBind.value!!) {
            applicationContext.unbindService(mapActivityViewModel)
            applicationContext.stopService(serviceIntent)
            mapActivityViewModel.default()
        }
    }
}