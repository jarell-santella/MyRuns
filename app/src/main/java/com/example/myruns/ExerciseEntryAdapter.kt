package com.example.myruns

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlin.math.floor

class ExerciseEntryAdapter(private val context: Context, private var exerciseEntryList: List<ExerciseEntry>) : BaseAdapter() {

    override fun getCount(): Int {
        return exerciseEntryList.size
    }

    override fun getItem(position: Int): Any {
        return exerciseEntryList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = View.inflate(context, R.layout.exercise_entry_list_item, null)

        val textViewTitle = view.findViewById(R.id.textViewTitle) as TextView
        val textViewDetails = view.findViewById(R.id.textViewDetails) as TextView

        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.settings_key), Context.MODE_PRIVATE)
        val unitsPreference = sharedPreferences.getString(context.getString(R.string.units_key), context.getString(R.string.units_default))

        // Get ExerciseEntry
        val exerciseEntry = exerciseEntryList[position]

        // Get attributes of ExerciseEntry
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

        textViewTitle.text = context.getString(R.string.exercise_entry_title, inputType, activityType, exerciseEntry.dateTime)
        textViewDetails.text = if (minutes == 0) {
            context.getString(R.string.exercise_entry_details_secs, distance, seconds)
        }
        else {
            context.getString(R.string.exercise_entry_details_mins_secs, distance, minutes, seconds)
        }

        // Launch activity of detailed version of respective ExerciseEntry if an ExerciseEntry is tapped
        // The screen launched depends on the input type of the ExerciseEntry
        view.setOnClickListener(View.OnClickListener {
            var intent = Intent()
            when (inputType) {
                "Manual Entry" -> intent = Intent(context, ExerciseEntryActivity::class.java)
                "GPS" -> intent = Intent(context, MapEntryActivity::class.java)
                "Automatic" -> intent = Intent(context, MapEntryActivity::class.java)
            }

            intent.putExtra(context.getString(R.string.exercise_entry_id_key), exerciseEntry.id)
            context.startActivity(intent)
        })

        return view
    }

    // Refresh/replace list of ExerciseEntries
    fun replace(newExerciseEntryList: List<ExerciseEntry>) {
        exerciseEntryList = newExerciseEntryList
    }
}