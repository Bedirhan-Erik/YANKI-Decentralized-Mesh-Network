package com.bedir.yanki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.io.encoding.Base64

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val user_id: String,
    val username: String,
    val public_key: ByteArray,
    val last_seen: Long,
    val last_mac: String? = null,
    val last_rssi: Int = -100,
    val is_trusted: Boolean = false
)
