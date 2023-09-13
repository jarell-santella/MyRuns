package com.example.myruns

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData

class ExerciseEntryViewModel(private val exerciseEntryRepository: ExerciseEntryRepository) : ViewModel() {
    val allExerciseEntries: LiveData<List<ExerciseEntry>> = exerciseEntryRepository.exerciseEntries.asLiveData()

    // Insert entire ExerciseEntry object into database (creating a new record)
    fun insert(exerciseEntry: ExerciseEntry) {
        exerciseEntryRepository.insert(exerciseEntry)
    }

    // Delete ExerciseEntry from database by id
    fun delete(id: Long) {
        exerciseEntryRepository.delete(id)
    }
}

// ViewModelFactory class for this ViewModel
class ExerciseEntryViewModelFactory(private val exerciseEntryRepository: ExerciseEntryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseEntryViewModel::class.java)) {
            return ExerciseEntryViewModel(exerciseEntryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}