package com.bedir.yanki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.bedir.yanki.data.local.entity.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE msg_id = :msgId)")
    suspend fun isMessageExists(msgId: String): Boolean

    @Query("SELECT * FROM messages WHERE sender_id = :userId OR receiver_id = :userId ORDER BY timestamp ASC")
    fun getMessagesWithUser(userId: String): Flow<List<MessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE is_synced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Query("UPDATE messages SET is_synced = 1 WHERE msg_id = :msgId")
    suspend fun markAsSynced(msgId: String)

    @Query("SELECT * FROM messages WHERE (sender_id = :userId) OR (receiver_id = :userId) ORDER BY timestamp ASC")
    fun getChatHistory(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE is_synced = 0 ORDER BY timestamp DESC LIMIT 20")
    suspend fun getPendingMessages(): List<MessageEntity>
}

