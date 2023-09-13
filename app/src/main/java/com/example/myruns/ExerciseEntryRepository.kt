package com.example.myruns

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExerciseEntryRepository(private val exerciseEntryDao: ExerciseEntryDao) {
    val exerciseEntries: Flow<List<ExerciseEntry>> = exerciseEntryDao.selectAll()

    // Use coroutines for IO with database
    fun insert(exerciseEntry: ExerciseEntry) {
        CoroutineScope(IO).launch {
            exerciseEntryDao.insert(exerciseEntry)
        }
    }

    // Use coroutines for IO with database
    fun delete(id: Long) {
        CoroutineScope(IO).launch {
            exerciseEntryDao.delete(id)
        }
    }
}