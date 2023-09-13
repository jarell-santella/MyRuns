package com.example.myruns

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import kotlin.properties.Delegates

class ManualActivity : AppCompatActivity() {
    private lateinit var manualActivityViewModel: ManualActivityViewModel

    private lateinit var listView: ListView
    private lateinit var currentCalendar: Calendar
    private lateinit var calendar: Calendar

    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var timePickerDialog: TimePickerDialog

    private lateinit var activityInputAdapter: ArrayAdapter<String>

    private var durationSaved by Delegates.notNull<Boolean>()
    private var distanceSaved by Delegates.notNull<Boolean>()
    private var caloriesSaved by Delegates.notNull<Boolean>()
    private var heartRateSaved by Delegates.notNull<Boolean>()
    private var commentSaved by Delegates.notNull<Boolean>()

    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)

        manualActivityViewModel = ViewModelProvider(this).get(ManualActivityViewModel::class.java)
        manualActivityViewModel.default()

        currentCalendar = manualActivityViewModel.calendar.value!!
        calendar = Calendar.getInstance()

        // Get current date and time
        var currentYear = currentCalendar.get(Calendar.YEAR)
        var currentMonth = currentCalendar.get(Calendar.MONTH)
        var currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        var currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        var currentMinute = currentCalendar.get(Calendar.MINUTE)

        // Once ViewModel calendar changed, replace all of the current dates with the dates and times of the calendar stored in the ViewModel
        manualActivityViewModel.calendar.observe(this) {
            currentCalendar = it
            currentYear = currentCalendar.get(Calendar.YEAR)
            currentMonth = currentCalendar.get(Calendar.MONTH)
            currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
            currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
            currentMinute = currentCalendar.get(Calendar.MINUTE)
        }

        // Create DatePickerDialog and TimePickerDialog
        datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener {_, year, month, day ->
            calendar.set(year, month, day)
            manualActivityViewModel.calendar.value = calendar},
            currentYear, currentMonth, currentDay)
        timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener {_, hour, minute ->
            calendar.set(Calendar.HOUR, hour)
            calendar.set(Calendar.MINUTE, minute)
            manualActivityViewModel.calendar.value = calendar},
            currentHour, currentMinute, false)

        listView = findViewById(R.id.listView)
        activityInputAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.activity_inputs))
        listView.adapter = activityInputAdapter

        // Variables that specify whether to save user's last input (if they press "Ok" instead of "Cancel" on the AlertDialog)
        durationSaved = false
        distanceSaved = false
        caloriesSaved = false
        heartRateSaved = false
        commentSaved = false

        val numberInput: Int = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val stringInput: Int = InputType.TYPE_CLASS_TEXT

        // Specify attributes of ExerciseEntry manually with the given fields
        listView.setOnItemClickListener() { _, _, index: Int, _ ->
            when (index) {
                0 -> datePickerDialog.show()
                1 -> timePickerDialog.show()
                2 -> makeDialog(getString(R.string.duration), numberInput, durationSaved, getString(R.string.duration_hint))
                3 -> makeDialog(getString(R.string.distance), numberInput, distanceSaved, getString(R.string.distance_hint))
                4 -> makeDialog(getString(R.string.calories), numberInput, caloriesSaved, getString(R.string.calories_hint))
                5 -> makeDialog(getString(R.string.heart_rate), numberInput, heartRateSaved, getString(R.string.heart_rate_hint))
                6 -> makeDialog(getString(R.string.comment), stringInput, commentSaved, getString(R.string.comment_hint))
            }
        }

        // If "Ok" button is pressed, save date and time as a single string, and convert distance to meters
        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener() {
            val sharedPreferences = getSharedPreferences(getString(R.string.settings_key), Context.MODE_PRIVATE)
            val unitsPreference = sharedPreferences.getString(getString(R.string.units_key), getString(R.string.units_default))

            val activityType = intent.extras!!.getInt(getString(R.string.activity_key), -1)
            val dateTime = SimpleDateFormat("HH:mm:ss MM/dd/yyyy").format(manualActivityViewModel.calendar.value!!.time)
            var distance = 0.0
            when (unitsPreference) {
                "Kilometers" -> distance = manualActivityViewModel.distance.value!! * 1000
                "Miles" -> distance = manualActivityViewModel.distance.value!! * 1609.344
            }
            onClickSave(activityType, dateTime, distance)
        }
    }

    // Function to make AlertDialog where user can specify the values of fields of the ExerciseEntry
    private fun makeDialog (title: String, inputType: Int, saved: Boolean, hint: String = "") {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val input = EditText(this)
        input.setPadding(20,20,20,20)
        input.inputType = inputType
        input.hint = hint
        input.textSize = 15f

        // Give extra space for the EditText in AlertDialog
        val frameLayout = FrameLayout(this)
        val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(10,2, 10, 2)
        input.layoutParams = layoutParams
        frameLayout.addView(input)

        // If previous value saved, next time user opens the field, their last value is there
        if (saved) {
            when (title) {
                getString(R.string.duration) -> input.setText(manualActivityViewModel.duration.value.toString())
                getString(R.string.distance) -> input.setText(manualActivityViewModel.distance.value.toString())
                getString(R.string.calories) -> input.setText(manualActivityViewModel.calories.value.toString())
                getString(R.string.heart_rate) -> input.setText(manualActivityViewModel.heartRate.value.toString())
                getString(R.string.comment) -> input.setText(manualActivityViewModel.comment.value)
            }
        }
        builder.setView(frameLayout)
        // If "Ok" is pressed and there is input, mark that there is a saved value to show to the user if they open the dialog again and save input in respective ViewModel variable
        // If no input, mark that there is nothing saved and save default to respective ViewModel variable
        builder.setPositiveButton(getString(R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
            when (title) {
                getString(R.string.duration) -> {
                    if (input.text.toString().isEmpty()) {
                        manualActivityViewModel.duration.value = 0.0
                        durationSaved = false
                    }
                    else {
                        manualActivityViewModel.duration.value = input.text.toString().toDouble()
                        durationSaved = true
                    }
                }
                getString(R.string.distance) -> {
                    if (input.text.toString().isEmpty()) {
                        manualActivityViewModel.distance.value = 0.0
                        distanceSaved = false
                    }
                    else {
                        manualActivityViewModel.distance.value = input.text.toString().toDouble()
                        distanceSaved = true
                    }
                }
                getString(R.string.calories) -> {
                    if (input.text.toString().isEmpty()) {
                        manualActivityViewModel.calories.value = 0.0
                        caloriesSaved = false
                    }
                    else {
                        manualActivityViewModel.calories.value = input.text.toString().toDouble()
                        caloriesSaved = true
                    }
                }
                getString(R.string.heart_rate) -> {
                    if (input.text.toString().isEmpty()) {
                        manualActivityViewModel.heartRate.value = 0.0
                        heartRateSaved = false
                    }
                    else {
                        manualActivityViewModel.heartRate.value = input.text.toString().toDouble()
                        heartRateSaved = true
                    }
                }
                getString(R.string.comment) -> {
                    manualActivityViewModel.comment.value = input.text.toString()
                    commentSaved = true
                }
            }
            dialog.dismiss()})
        builder.setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, _ -> dialog.cancel()})
        builder.show()
    }

    // Tell StartFragment that the result of this activity is canceled (don't add ExerciseEntry to database and discard all information in the dialog fields)
    fun onClickCancel(view: View) {
        setResult(Activity.RESULT_CANCELED)

        manualActivityViewModel.default()
        finish()
    }

    // Tell StartFragment that the result of this activity is ok. Pass ExerciseEntry attributes through intent and add ExerciseEntry to database
    fun onClickSave(activityType: Int, dateTime: String, distance: Double) {
        var duration = manualActivityViewModel.duration.value!!
        var speed = distance
        if (duration != 0.0) {
            duration *= 60
            speed = distance / duration
        }

        val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type

        val bundle = Bundle()
        bundle.putInt(getString(R.string.input_key), 0)
        bundle.putInt(getString(R.string.activity_key), activityType)
        bundle.putString(getString(R.string.datetime_key), dateTime)
        bundle.putDouble(getString(R.string.duration_key), duration)
        bundle.putDouble(getString(R.string.distance_key), distance)
        bundle.putDouble(getString(R.string.avg_pace_key), 0.0)
        bundle.putDouble(getString(R.string.avg_speed_key), speed)
        bundle.putDouble(getString(R.string.calories_key), manualActivityViewModel.calories.value!!)
        bundle.putDouble(getString(R.string.climb_key), 0.0)
        bundle.putDouble(getString(R.string.heart_rate_key), manualActivityViewModel.heartRate.value!!)
        bundle.putString(getString(R.string.comment_key), manualActivityViewModel.comment.value!!)
        bundle.putString(getString(R.string.location_list_key), Gson().toJson(ArrayList<LatLng>(), type))

        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)

        manualActivityViewModel.default()
        finish()
    }

    override fun finish() {
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}