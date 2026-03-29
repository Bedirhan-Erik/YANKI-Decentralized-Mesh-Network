import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, MessageEntity::class, EmergencySignalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YankiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun emergencySignalDao(): EmergencySignalDao

}