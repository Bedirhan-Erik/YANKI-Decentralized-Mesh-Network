package com.bedir.yanki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_signals")
data class EmergencySignalEntity(
    @PrimaryKey val signal_id: String,
    val user_id: String,
    val user_name: String? = null, // Yeni alan: Kullanıcı ismi
    val latitude: Double,
    val longitude: Double,
    val emergency_type: String,
    val battery_level: Int,
    val timestamp: Long,
    val is_synced: Boolean = false,
    val hop_count: Int = 0,
    val blood_type: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val emergency_contact: String? = null
)