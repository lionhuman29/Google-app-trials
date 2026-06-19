package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_people")
data class DiscoveredPerson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val instagramId: String,
    val displayName: String,
    val tagline: String,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val rssi: Int = -70,
    val estimatedDistance: Double = 5.0,
    val avatarColorIndex: Int = 0,
    val notes: String = "",
    val isSaved: Boolean = false
)
