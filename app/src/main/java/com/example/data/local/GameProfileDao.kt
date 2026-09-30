package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GameProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY isFavorite DESC, lastPlayedTimestamp DESC, gameName ASC")
    fun getAllProfiles(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfile(packageName: String): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: GameProfile)

    @Update
    suspend fun updateProfile(profile: GameProfile)

    @Delete
    suspend fun deleteProfile(profile: GameProfile)

    @Query("UPDATE game_profiles SET lastPlayedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastPlayed(packageName: String, timestamp: Long)

    @Query("UPDATE game_profiles SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavorite(packageName: String, isFavorite: Boolean)
}
