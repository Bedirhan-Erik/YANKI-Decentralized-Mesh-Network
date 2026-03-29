import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideYankiDatabase(@ApplicationContext context: Context): YankiDatabase {
        return Room.databaseBuilder(
            context,
            YankiDatabase::class.java,
            "yanki_local_db"
        )
            // Şimdilik veritabanı şeması değiştiğinde eskisini silip yenisini açması için fallback ekleyebiliriz
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: YankiDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: YankiDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideEmergencySignalDao(database: YankiDatabase): EmergencySignalDao {
        return database.emergencySignalDao()
    }
}