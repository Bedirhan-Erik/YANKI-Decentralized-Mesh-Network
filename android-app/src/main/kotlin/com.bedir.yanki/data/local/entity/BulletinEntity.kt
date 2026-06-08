package com.bedir.yanki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bulletins")
data class BulletinEntity(
    @PrimaryKey
    val post_id: String,
    val sender_id: String,
    val sender_name: String,
    val content: String,
    val timestamp: Long,
    val type: String, // INFO, NEED, ALERT
    val latitude: Double,
    val longitude: Double,
    val ttl: Int,
    val is_synced: Boolean = false
)
