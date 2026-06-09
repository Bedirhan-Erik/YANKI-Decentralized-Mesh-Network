package com.bedir.yanki.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index("sender_id"),
        Index("receiver_id"),
        Index("is_synced"),
        Index("ack_status")
    ]
)
data class MessageEntity(
    @PrimaryKey val msg_id: String,
    val sender_id: String,
    val sender_name: String? = null,
    val receiver_id: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val content_blob: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val signature: ByteArray? = null,
    val timestamp: Long,
    val status: Int = 0,
    val is_synced: Boolean = false,
    val ttl: Int = 7,
    val ack_status: Int = 0,   // 0=bekliyor, 1=iletildi, 2=başarısız
    val retry_count: Int = 0
)
