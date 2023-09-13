package com.example.myruns

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import kotlin.math.floor
import kotlin.properties.Delegates

class ExerciseEntryActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar

    private lateinit var inputTypeText: EditText
    private lateinit var activityTypeText: EditText
    private lateinit var datetimeText: EditText
    private lateinit var durationText: EditText
    private lateinit var distanceText: EditText
    private lateinit var caloriesText: EditText
    private lateinit var heartRateText: EditText

    private lateinit var database: ExerciseEntryDatabase
    private lateinit var databaseDao: ExerciseEntryDao
    private lateinit var repository: ExerciseEntryRepository
    private lateinit var viewModelFactory: ExerciseEntryViewModelFactory
    private lateinit var viewModel: ExerciseEntryViewModel

    private var id by Delegates.notNull<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_entry)

        // Use custom ActionBar with "Back" and "Delete" buttons
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        inputTypeText = findViewById(R.id.inputTypeText)
        activityTypeText = findViewById(R.id.activityTypeText)
        datetimeText = findViewById(R.id.datetimeText)
        durationText = findViewById(R.id.durationText)
        distanceText = findViewById(R.id.distanceText)
        caloriesText = findViewById(R.id.caloriesText)
        heartRateText = findViewById(R.id.heartRateText)

        database = ExerciseEntryDatabase.getInstance(this)
        databaseDao = database.exerciseEntryDao
        repository = ExerciseEntryRepository(databaseDao)
        viewModelFactory = ExerciseEntryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ExerciseEntryViewModel::class.java)

        val sharedPreferences = getSharedPreferences(getString(R.string.settings_key), Context.MODE_PRIVATE)
        val unitsPreference = sharedPreferences.getString(getString(R.string.units_key), getString(R.string.units_default))

        id = intent.extras!!.getLong(getString(R.string.exercise_entry_id_key), -1)

        viewModel.allExerciseEntries.observe(this) {
            // Get ExerciseEntry based off of id
            val exerciseEntry = it.find { entry -> entry.id == id }

            // Get attributes of ExerciseEntry
            if (exerciseEntry != null) {
                val inputType = when (exerciseEntry.inputType) {
                    0 -> "Manual Entry"
                    1 -> "GPS"
                    else -> "Automatic"
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

                var distance = exerciseEntry.distance.toString()
                when (unitsPreference) {
                    "Kilometers" -> distance = String.format("%.2f kilometers", exerciseEntry.distance / 1000)
                    "Miles" -> distance = String.format("%.2f miles", exerciseEntry.distance / 1609.344)
                }

                val minutes = floor(exerciseEntry.duration).toInt() / 60
                val seconds = exerciseEntry.duration % 60
                val duration = if (minutes == 0) {
                    String.format("%.3f second(s)", seconds)
                }
                else {
                    String.format("%d minute(s) %.3f second(s)", minutes, seconds)
                }

                val calories = exerciseEntry.calorie

                val heartRate = exerciseEntry.heartRate

                // Display attributes of ExerciseEntry
                inputTypeText.setText(inputType)
                activityTypeText.setText(activityType)
                datetimeText.setText(exerciseEntry.dateTime)
                durationText.setText(duration)
                distanceText.setText(distance)
                caloriesText.setText(String.format("%.2f cal(s)", calories))
                heartRateText.setText(String.format("%.2f bpm", heartRate))
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