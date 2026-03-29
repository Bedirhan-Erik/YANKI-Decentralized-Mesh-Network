import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // İnternet geldiğinde WorkManager'ın buluta basacağı gönderilmemiş mesajları getirir
    @Query("SELECT * FROM messages WHERE is_synced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    // Mesaj buluta başarıyla gittikten sonra tetiklenecek
    @Query("UPDATE messages SET is_synced = 1 WHERE msg_id = :msgId")
    suspend fun markAsSynced(msgId: String)

    // Belli bir kullanıcıya ait mesaj geçmişini getirir
    @Query("SELECT * FROM messages WHERE sender_id = :userId OR receiver_id = :userId ORDER BY timestamp ASC")
    suspend fun getChatHistory(userId: String): List<MessageEntity>
}