package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey
    val packageName: String,
    val gameName: String,
    val isFavorite: Boolean = false,
    val targetBrightness: Int = -1, // -1 means don't alter system brightness, 0..100
    val enableDnd: Boolean = false,
    val targetRefreshRate: Float = -1f, // -1 means system auto, 60f, 90f
    val targetAnimationScale: Float = -1f, // -1 means don't touch, 0.5f, 0.0f
    val launchOverlay: Boolean = true,
    val lastPlayedTimestamp: Long = 0L,
    val customNotes: String = ""
)
