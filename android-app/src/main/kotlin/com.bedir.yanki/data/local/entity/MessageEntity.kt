package com.bedir.yanki.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["sender_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sender_id"), Index("receiver_id")]
)
data class MessageEntity(
    @PrimaryKey val msg_id: String,
    val sender_id: String,
    val receiver_id: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val content_blob: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val signature: ByteArray? = null, // Mesaj doğrulaması için imza alanı eklendi
    val timestamp: Long,
    val status: Int = 0,
    val is_synced: Boolean = false,
    val ttl: Int = 7
)
