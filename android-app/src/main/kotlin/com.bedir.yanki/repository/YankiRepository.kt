import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YankiRepository @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val emergencySignalDao: EmergencySignalDao
) {
    // --- KULLANICI İŞLEMLERİ ---
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    suspend fun getAllUsers() = userDao.getAllUsers()

    // --- MESAJ İŞLEMLERİ ---
    suspend fun saveMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun getUnsyncedMessages() = messageDao.getUnsyncedMessages()
    suspend fun markMessageAsSynced(msgId: String) = messageDao.markAsSynced(msgId)
    suspend fun getChatHistory(userId: String) = messageDao.getChatHistory(userId)

    // --- ACİL DURUM (SOS) İŞLEMLERİ ---
    suspend fun saveEmergencySignal(signal: EmergencySignalEntity) = emergencySignalDao.insertSignal(signal)
    suspend fun getAllSignals() = emergencySignalDao.getAllSignals()
}