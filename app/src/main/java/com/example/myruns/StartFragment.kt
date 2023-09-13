package com.example.myruns

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class StartFragment : Fragment() {
    private lateinit var inputTypeSpinner: Spinner
    private lateinit var inputTypeAdapter: ArrayAdapter<String>

    private lateinit var activityTypeSpinner: Spinner
    private lateinit var activityTypeAdapter: ArrayAdapter<String>

    private lateinit var startButton: Button

    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        inputTypeSpinner = view.findViewById(R.id.inputTypeSpinner)
        inputTypeAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.input_types))
        inputTypeSpinner.adapter = inputTypeAdapter

        activityTypeSpinner = view.findViewById(R.id.activityTypeSpinner)
        activityTypeAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.activity_types))
        activityTypeSpinner.adapter = activityTypeAdapter

        startButton = view.findViewById(R.id.startButton)
        startButton.setOnClickListener(View.OnClickListener {
            val inputType = inputTypeSpinner.selectedItemPosition
            val activityType = activityTypeSpinner.selectedItemPosition

            // Depending on InputType, open certain activity and pass the InputType and ActivityType
            var intent = Intent()
            when (inputType) {
                0 -> intent = Intent(context, ManualActivity::class.java)
                1 -> intent = Intent(context, MapActivity::class.java)
                2 -> intent = Intent(context, MapActivity::class.java)
            }

            intent.putExtra(getString(R.string.input_key), inputType)
            intent.putExtra(getString(R.string.activity_key), activityType)
            activityResultLauncher.launch(intent)
        })

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ExerciseEntryDatabase.getInstance(requireActivity())
        val databaseDao = database.exerciseEntryDao
        val repository = ExerciseEntryRepository(databaseDao)
        val viewModelFactory = ExerciseEntryViewModelFactory(repository)
        val viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ExerciseEntryViewModel::class.java)

        // Wait for ManualActivity to respond with "Ok" or "Cancelled".
        // If "Ok", add a new ExerciseEntry to database using the default values and values of the fields given through Bundle/Intent from ManualActivity
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val sharedPreferences = requireActivity().getSharedPreferences(getString(R.string.database_key), Context.MODE_PRIVATE)
            val exerciseEntryID = sharedPreferences.getLong(getString(R.string.id_key), 0).plus(1)

            if (result.resultCode == Activity.RESULT_OK) {
                val intent: Intent = result.data!!

                val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type
                val defaultLocations = Gson().toJson(ArrayList<LatLng>(), type)
                val locations = intent.extras!!.getString(getString(R.string.location_list_key), defaultLocations)

                val exerciseEntry = ExerciseEntry()
                exerciseEntry.inputType = intent.extras!!.getInt(getString(R.string.input_key), -1)
                exerciseEntry.activityType = intent.extras!!.getInt(getString(R.string.activity_key), -1)
                exerciseEntry.dateTime = intent.extras!!.getString(getString(R.string.datetime_key), "")
                exerciseEntry.duration = intent.extras!!.getDouble(getString(R.string.duration_key), 0.0)
                exerciseEntry.distance = intent.extras!!.getDouble(getString(R.string.distance_key), 0.0)
                exerciseEntry.avgPace = intent.extras!!.getDouble(getString(R.string.avg_pace_key), 0.0)
                exerciseEntry.avgSpeed = intent.extras!!.getDouble(getString(R.string.avg_speed_key), 0.0)
                exerciseEntry.calorie = intent.extras!!.getDouble(getString(R.string.calories_key), 0.0)
                exerciseEntry.climb = intent.extras!!.getDouble(getString(R.string.climb_key), 0.0)
                exerciseEntry.heartRate = intent.extras!!.getDouble(getString(R.string.heart_rate_key), 0.0)
                exerciseEntry.comment = intent.extras!!.getString(getString(R.string.comment_key), "")
                exerciseEntry.locationList = if (locations != defaultLocations) {
                    Gson().fromJson(locations, type)
                }
                else {
                    ArrayList()
                }

                viewModel.insert(exerciseEntry)

                val toast = Toast.makeText(context, getString(R.string.toast_added, exerciseEntryID), Toast.LENGTH_SHORT)
                toast.show()
                sharedPreferences.edit().putLong(getString(R.string.id_key), exerciseEntryID).apply()
            }
            else {
                val toast = Toast.makeText(context, getString(R.string.toast_discarded), Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }
}