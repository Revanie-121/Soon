package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JobEntity::class], version = 1, exportSchema = false)
abstract class SonoraDatabase : RoomDatabase() {

    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: SonoraDatabase? = null

        fun getInstance(context: Context): SonoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SonoraDatabase::class.java,
                    "sonora_database.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
