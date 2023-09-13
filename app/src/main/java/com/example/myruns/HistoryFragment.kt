package com.example.myruns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class HistoryFragment : Fragment() {
    private lateinit var listView: ListView

    private lateinit var database: ExerciseEntryDatabase
    private lateinit var databaseDao: ExerciseEntryDao
    private lateinit var repository: ExerciseEntryRepository
    private lateinit var viewModelFactory: ExerciseEntryViewModelFactory
    private lateinit var viewModel: ExerciseEntryViewModel

    private lateinit var exerciseEntryArrayList: ArrayList<ExerciseEntry>
    private lateinit var exerciseEntryAdapter: ExerciseEntryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        listView = view.findViewById(R.id.listView)
        exerciseEntryArrayList = ArrayList()
        exerciseEntryAdapter = ExerciseEntryAdapter(requireActivity(), exerciseEntryArrayList)
        listView.adapter = exerciseEntryAdapter

        database = ExerciseEntryDatabase.getInstance(requireActivity())
        databaseDao = database.exerciseEntryDao
        repository = ExerciseEntryRepository(databaseDao)
        viewModelFactory = ExerciseEntryViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ExerciseEntryViewModel::class.java)

        // Update/refresh ExerciseEntries in list if there is change
        viewModel.allExerciseEntries.observe(requireActivity(), Observer { it ->
            exerciseEntryAdapter.replace(it)
            exerciseEntryAdapter.notifyDataSetChanged()
        })

        return view
    }

    // Update/refresh list of ExerciseEntries after pressing "Back" or "Delete" from the ExerciseEntryActivity as well as refresh when you switch to History tab
    override fun onResume() {
        super.onResume()
        exerciseEntryAdapter.notifyDataSetChanged()
    }
}