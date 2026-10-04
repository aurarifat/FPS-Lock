package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GameProfile

@Database(entities = [GameProfile::class], version = 2, exportSchema = false)
abstract class GameBoostDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao

    companion object {
        @Volatile
        private var INSTANCE: GameBoostDatabase? = null

        fun getDatabase(context: Context): GameBoostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameBoostDatabase::class.java,
                    "gameboost_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
