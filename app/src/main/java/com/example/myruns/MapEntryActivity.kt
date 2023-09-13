package com.example.myruns

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.properties.Delegates

class MapEntryActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var toolbar: Toolbar

    private lateinit var gMap: GoogleMap
    private lateinit var markerOptions: MarkerOptions
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var polylines: ArrayList<Polyline>

    private lateinit var typeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var curSpeedTextView: TextView
    private lateinit var climbTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var distanceTextView: TextView

    private lateinit var database: ExerciseEntryDatabase
    private lateinit var databaseDao: ExerciseEntryDao
    private lateinit var repository: ExerciseEntryRepository
    private lateinit var viewModelFactory: ExerciseEntryViewModelFactory
    private lateinit var viewModel: ExerciseEntryViewModel

    private var id by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_entry)

        // Use custom ActionBar with "Back" and "Delete" buttons
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        database = ExerciseEntryDatabase.getInstance(this)
        databaseDao = database.exerciseEntryDao
        repository = ExerciseEntryRepository(databaseDao)
        viewModelFactory = ExerciseEntryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ExerciseEntryViewModel::class.java)

        typeTextView = findViewById(R.id.typeTextView)
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView)
        curSpeedTextView = findViewById(R.id.curSpeedTextView)
        climbTextView = findViewById(R.id.climbTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
        distanceTextView = findViewById(R.id.distanceTextView)

        id = intent.extras!!.getLong(getString(R.string.exercise_entry_id_key), -1)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        markerOptions = MarkerOptions()
        polylineOptions = PolylineOptions()
        polylineOptions.color(Color.RED)
        polylines = ArrayList()

        val sharedPreferences = getSharedPreferences(getString(R.string.settings_key), Context.MODE_PRIVATE)
        val unitsPreference = sharedPreferences.getString(getString(R.string.units_key), getString(R.string.units_default))

        viewModel.allExerciseEntries.observe(this) {
            // Get ExerciseEntry based off of id
            val exerciseEntry = it.find { entry -> entry.id == id }

            // Get attributes of ExerciseEntry
            if (exerciseEntry != null) {
                val locations = exerciseEntry.locationList
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

                    markerOptions = MarkerOptions()
                    markerOptions.position(locations.first())
                    markerOptions.title("Starting Position")
                    gMap.addMarker(markerOptions)

                    val mapCentered = gMap.projection.visibleRegion.latLngBounds.contains(lastLocation)
                    if (!mapCentered) {
                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocation, 17f)
                        gMap.animateCamera(cameraUpdate)
                    }

                    markerOptions = MarkerOptions()
                    markerOptions.position(lastLocation)
                    markerOptions.title("Last Position")
                    gMap.addMarker(markerOptions)
                }

                val activityType = when (exerciseEntry.activityType) {
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

                var avgSpeed = exerciseEntry.avgSpeed.toString()
                var climb = exerciseEntry.climb.toString()
                var distance = exerciseEntry.distance.toString()
                when (unitsPreference) {
                    "Kilometers" -> {
                        avgSpeed = String.format("Average speed: %.2f km/h", exerciseEntry.avgSpeed * 3.6)
                        climb = String.format("Climb: %.2f kilometers", exerciseEntry.climb / 1000)
                        distance = String.format("Distance: %.2f kilometers", exerciseEntry.distance / 1000)
                    }
                    "Miles" -> {
                        avgSpeed = String.format("Average speed: %.2f mi/h", exerciseEntry.avgSpeed * 2.2369362921)
                        climb = String.format("Climb: %.2f miles", exerciseEntry.climb / 1609.344)
                        distance = String.format("Distance: %.2f miles", exerciseEntry.distance / 1609.344)
                    }
                }

                val calories = exerciseEntry.calorie

                val curSpeed = getString(R.string.cur_speed_na)

                // Display attributes of ExerciseEntry
                typeTextView.text = String.format("Type: %s", activityType)
                avgSpeedTextView.text = avgSpeed
                curSpeedTextView.text = curSpeed
                climbTextView.text = climb
                caloriesTextView.text = String.format("Calories: %.2f cal(s)", calories)
                distanceTextView.text = distance
            }
        }
    }

    // Create ActionBar at the top with "Back" and "Delete" button
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.exercise_entry_activity_menu, menu)
        return true
    }

    // Listeners for the ActionBar Menu buttons "Back" and "Delete"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.back -> {
                finish()
            }
            R.id.delete -> {
                viewModel.delete(id)
                val toast = Toast.makeText(this, getString(R.string.toast_deleted, id), Toast.LENGTH_SHORT)
                toast.show()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}