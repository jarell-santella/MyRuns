package com.example.myruns

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEntryDao {
    @Query("SELECT * FROM exercise_entry_table")
    fun selectAll(): Flow<List<ExerciseEntry>>

    @Insert
    suspend fun insert(exerciseEntry: ExerciseEntry)

    @Query("DELETE FROM exercise_entry_table WHERE id = :id")
    suspend fun delete(id: Long)
}