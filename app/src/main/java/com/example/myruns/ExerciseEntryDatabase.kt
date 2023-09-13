package com.example.myruns

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ExerciseEntry::class], version = 1)
@TypeConverters(ArrayListJsonStringConverters::class)
abstract class ExerciseEntryDatabase : RoomDatabase() {
    abstract val exerciseEntryDao: ExerciseEntryDao

    companion object{
        //The Volatile keyword guarantees visibility of changes to the INSTANCE variable across threads
        @Volatile
        private var INSTANCE: ExerciseEntryDatabase? = null

        fun getInstance(context: Context) : ExerciseEntryDatabase{
            synchronized(this){
                var instance = INSTANCE
                if(instance == null){
                    instance = Room.databaseBuilder(context.applicationContext, ExerciseEntryDatabase::class.java, "comment_table").build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}