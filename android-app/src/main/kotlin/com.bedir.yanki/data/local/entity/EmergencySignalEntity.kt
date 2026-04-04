package com.bedir.yanki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_signals")
data class EmergencySignalEntity(
    @PrimaryKey val signal_id: String,
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val emergency_type: String,
    val battery_level: Int,
    val timestamp: Long,
    val is_synced: Boolean = false,
    val hop_count: Int = 0
)